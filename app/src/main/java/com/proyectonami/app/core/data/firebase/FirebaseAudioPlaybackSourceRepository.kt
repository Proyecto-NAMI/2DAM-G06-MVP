package com.proyectonami.app.core.data.firebase

import android.content.Context
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageException
import com.proyectonami.app.core.domain.audio.AUDIO_NOTE_CONTENT_TYPE
import com.proyectonami.app.core.domain.audio.DEFAULT_AUDIO_LIMIT_BYTES
import com.proyectonami.app.core.domain.audio.DEFAULT_AUDIO_LIMIT_MILLIS
import com.proyectonami.app.core.domain.audio.audioNoteStoragePath
import com.proyectonami.app.core.domain.media.AudioPlaybackFailure
import com.proyectonami.app.core.domain.media.AudioPlaybackSourceRepository
import com.proyectonami.app.core.domain.media.AudioPlaybackSourceResult
import com.proyectonami.app.core.domain.messages.AudioMessageRecord
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID
import kotlin.coroutines.resume

class FirebaseAudioPlaybackSourceRepository(
    context: Context,
    private val storage: FirebaseStorage,
) : AudioPlaybackSourceRepository {
    private val cacheDirectory = File(context.cacheDir, CACHE_DIRECTORY)

    override suspend fun prepare(message: AudioMessageRecord): AudioPlaybackSourceResult =
        withContext(Dispatchers.IO) {
            if (!message.isValidAudioResource()) {
                return@withContext AudioPlaybackSourceResult.Failed(AudioPlaybackFailure.MISSING_RESOURCE)
            }
            val destination = File(cacheDirectory, "${message.clientMessageId}.m4a")
            if (destination.isFile && destination.length() == message.sizeBytes) {
                return@withContext AudioPlaybackSourceResult.Ready(destination.absolutePath)
            }
            runCatching {
                check(cacheDirectory.exists() || cacheDirectory.mkdirs())
                if (destination.exists()) destination.delete()
                awaitDownload(message.storagePath, destination)
                check(destination.isFile && destination.length() == message.sizeBytes)
                AudioPlaybackSourceResult.Ready(destination.absolutePath)
            }.getOrElse { error ->
                destination.delete()
                if (error is CancellationException) throw error
                AudioPlaybackSourceResult.Failed(error.toPlaybackFailure())
            }
        }

    override fun clear() {
        cacheDirectory.listFiles()?.forEach { file ->
            if (file.isFile) file.delete()
        }
        cacheDirectory.delete()
    }

    private suspend fun awaitDownload(
        storagePath: String,
        destination: File,
    ) = suspendCancellableCoroutine<Unit> { continuation ->
        val task = storage.reference.child(storagePath).getFile(destination)
        task.addOnSuccessListener {
            if (continuation.isActive) continuation.resume(Unit)
        }
        task.addOnFailureListener { error ->
            if (continuation.isActive) continuation.resumeWith(Result.failure(error))
        }
        continuation.invokeOnCancellation { task.cancel() }
    }

    private fun AudioMessageRecord.isValidAudioResource(): Boolean =
        runCatching { UUID.fromString(clientMessageId).toString() == clientMessageId }.getOrDefault(false) &&
            conversationId.isNotBlank() &&
            '/' !in conversationId &&
            durationMillis in 1..DEFAULT_AUDIO_LIMIT_MILLIS &&
            sizeBytes in 1..DEFAULT_AUDIO_LIMIT_BYTES &&
            contentType == AUDIO_NOTE_CONTENT_TYPE &&
            storagePath == audioNoteStoragePath(conversationId, clientMessageId)

    private fun Throwable.toPlaybackFailure(): AudioPlaybackFailure =
        when (this) {
            is StorageException ->
                when (errorCode) {
                    StorageException.ERROR_OBJECT_NOT_FOUND -> AudioPlaybackFailure.MISSING_RESOURCE
                    StorageException.ERROR_NOT_AUTHENTICATED,
                    StorageException.ERROR_NOT_AUTHORIZED,
                    -> AudioPlaybackFailure.NOT_AUTHORIZED
                    StorageException.ERROR_RETRY_LIMIT_EXCEEDED -> AudioPlaybackFailure.NETWORK
                    else -> AudioPlaybackFailure.UNKNOWN
                }
            else -> AudioPlaybackFailure.UNKNOWN
        }

    private companion object {
        const val CACHE_DIRECTORY = "nami-audio-playback"
    }
}
