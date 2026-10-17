package com.proyectonami.app.core.domain.media

import kotlinx.coroutines.flow.StateFlow

sealed interface AudioPlaybackState {
    data object Idle : AudioPlaybackState

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

enum class AudioPlaybackFailure { MISSING_RESOURCE, NOT_DECODABLE, UNKNOWN }

interface AudioPlayer {
    fun setOnCompletionListener(listener: () -> Unit) = Unit

    fun play(resourceId: String)

    fun pause()

    fun stop()
}

interface AudioPlaybackController {
    val state: StateFlow<AudioPlaybackState>

    fun play(resourceId: String)

    fun pause()

    fun stop()
}
