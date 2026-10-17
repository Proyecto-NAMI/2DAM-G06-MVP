package com.proyectonami.app.core.media

import com.proyectonami.app.core.domain.media.AudioPlaybackController
import com.proyectonami.app.core.domain.media.AudioPlaybackFailure
import com.proyectonami.app.core.domain.media.AudioPlaybackState
import com.proyectonami.app.core.domain.media.AudioPlayer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class MediaPlaybackCoordinator(
    private val player: AudioPlayer,
) : AudioPlaybackController {
    private val mutableState = MutableStateFlow<AudioPlaybackState>(AudioPlaybackState.Idle)
    override val state: StateFlow<AudioPlaybackState> = mutableState.asStateFlow()

    init {
        player.setOnCompletionListener { mutableState.value = AudioPlaybackState.Idle }
    }

    @Synchronized
    override fun play(resourceId: String) {
        if (resourceId.isBlank()) {
            mutableState.value = AudioPlaybackState.Failed(resourceId, AudioPlaybackFailure.MISSING_RESOURCE)
            return
        }
        try {
            player.stop()
            player.play(resourceId)
            mutableState.value = AudioPlaybackState.Playing(resourceId)
        } catch (_: IllegalArgumentException) {
            mutableState.value = AudioPlaybackState.Failed(resourceId, AudioPlaybackFailure.NOT_DECODABLE)
        } catch (_: Exception) {
            mutableState.value = AudioPlaybackState.Failed(resourceId, AudioPlaybackFailure.UNKNOWN)
        }
    }

    override fun pause() {
        val current = mutableState.value as? AudioPlaybackState.Playing ?: return
        player.pause()
        mutableState.value = AudioPlaybackState.Paused(current.resourceId)
    }

    override fun stop() {
        player.stop()
        mutableState.value = AudioPlaybackState.Idle
    }
}
