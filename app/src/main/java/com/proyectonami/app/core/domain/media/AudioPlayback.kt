package com.proyectonami.app.core.domain.media

import com.proyectonami.app.core.domain.messages.AudioMessageRecord
import kotlinx.coroutines.flow.StateFlow

sealed interface AudioPlaybackState {
    data object Idle : AudioPlaybackState

    data class Preparing(
        val resourceId: String,
    ) : AudioPlaybackState

    data class Playing(
        val resourceId: String,
    ) : AudioPlaybackState

    data class Paused(
        val resourceId: String,
    ) : AudioPlaybackState

    data class Failed(
        val resourceId: String,
        val reason: AudioPlaybackFailure,
    ) : AudioPlaybackState
}

enum class AudioPlaybackFailure {
    MISSING_RESOURCE,
    NOT_AUTHORIZED,
    NETWORK,
    NOT_DECODABLE,
    UNKNOWN,
}

class AudioPlayerException(
    val reason: AudioPlaybackFailure,
    cause: Throwable? = null,
) : IllegalStateException(reason.name, cause)

interface AudioPlayer {
    fun setOnCompletionListener(listener: () -> Unit) = Unit

    fun setOnErrorListener(listener: (AudioPlaybackFailure) -> Unit) = Unit

    fun play(localPath: String)

    fun pause()

    fun resume()

    fun stop()
}

interface AudioPlaybackController {
    val state: StateFlow<AudioPlaybackState>

    fun prepare(resourceId: String)

    fun play(
        resourceId: String,
        localPath: String = resourceId,
    )

    fun pause()

    fun resume()

    fun fail(
        resourceId: String,
        reason: AudioPlaybackFailure,
    )

    fun stop()
}

sealed interface AudioPlaybackSourceResult {
    data class Ready(
        val localPath: String,
    ) : AudioPlaybackSourceResult

    data class Failed(
        val reason: AudioPlaybackFailure,
    ) : AudioPlaybackSourceResult
}

interface AudioPlaybackSourceRepository {
    suspend fun prepare(message: AudioMessageRecord): AudioPlaybackSourceResult

    fun clear()
}
