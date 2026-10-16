package com.proyectonami.app.core.domain.audio

import kotlinx.coroutines.flow.Flow

data class AudioNoteMetadata(
    val clientMessageId: String,
    val conversationId: String,
    val senderId: String,
    val durationMillis: Long,
    val sizeBytes: Long,
    val contentType: String = "audio/mp4",
)

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

enum class AudioUploadFailure { NETWORK, NOT_AUTHORIZED, INVALID_NOTE, UNKNOWN }

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
