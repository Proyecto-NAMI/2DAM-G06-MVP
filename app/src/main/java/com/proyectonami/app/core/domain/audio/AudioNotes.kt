package com.proyectonami.app.core.domain.audio

import kotlinx.coroutines.flow.Flow
import java.util.UUID

const val AUDIO_NOTE_CONTENT_TYPE = "audio/mp4"

fun audioNoteStoragePath(
    conversationId: String,
    clientMessageId: String,
): String = "conversations/$conversationId/audio/$clientMessageId/note.m4a"

data class AudioNoteMetadata(
    val clientMessageId: String,
    val conversationId: String,
    val senderId: String,
    val durationMillis: Long,
    val sizeBytes: Long,
    val contentType: String = AUDIO_NOTE_CONTENT_TYPE,
    val storagePath: String = audioNoteStoragePath(conversationId, clientMessageId),
) {
    init {
        require(UUID.fromString(clientMessageId).toString() == clientMessageId) { "Invalid message UUID" }
        require(conversationId.isNotBlank() && '/' !in conversationId) { "Invalid conversation ID" }
        require(senderId.isNotBlank() && '/' !in senderId) { "Invalid sender ID" }
        require(durationMillis in 1..DEFAULT_AUDIO_LIMIT_MILLIS) { "Invalid audio duration" }
        require(sizeBytes in 1..DEFAULT_AUDIO_LIMIT_BYTES) { "Invalid audio size" }
        require(contentType == AUDIO_NOTE_CONTENT_TYPE) { "Invalid audio content type" }
        require(storagePath == audioNoteStoragePath(conversationId, clientMessageId)) { "Invalid audio storage path" }
    }
}

sealed interface AudioUploadState {
    data object Pending : AudioUploadState

    data class Progress(
        val fraction: Float,
    ) : AudioUploadState

    data class Sent(
        val metadata: AudioNoteMetadata,
    ) : AudioUploadState

    data class Failed(
        val metadata: AudioNoteMetadata,
        val reason: AudioUploadFailure,
    ) : AudioUploadState
}

enum class AudioUploadFailure {
    UNAUTHENTICATED,
    NETWORK,
    NOT_AUTHORIZED,
    INVALID_NOTE,
    ID_CONFLICT,
    UNKNOWN,
}

interface AudioNotesRepository {
    fun upload(
        metadata: AudioNoteMetadata,
        localPath: String,
    ): Flow<AudioUploadState>

    suspend fun deleteLocal(localPath: String): Result<Unit>
}

class UploadAudioNoteUseCase(
    private val repository: AudioNotesRepository,
) {
    operator fun invoke(
        metadata: AudioNoteMetadata,
        localPath: String,
    ): Flow<AudioUploadState> = repository.upload(metadata, localPath)
}
