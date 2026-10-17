package com.proyectonami.app.core.media

import com.proyectonami.app.core.domain.media.AudioPlaybackController
import com.proyectonami.app.core.domain.media.AudioPlaybackFailure
import com.proyectonami.app.core.domain.media.AudioPlaybackState
import com.proyectonami.app.core.domain.media.AudioPlayer
import com.proyectonami.app.core.domain.media.AudioPlayerException
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
        player.setOnErrorListener { reason ->
            val resourceId = mutableState.value.resourceIdOrNull() ?: return@setOnErrorListener
            mutableState.value = AudioPlaybackState.Failed(resourceId, reason)
        }
    }

    @Synchronized
    override fun prepare(resourceId: String) {
        if (resourceId.isBlank()) {
            mutableState.value = AudioPlaybackState.Failed(resourceId, AudioPlaybackFailure.MISSING_RESOURCE)
            return
        }
        player.stop()
        mutableState.value = AudioPlaybackState.Preparing(resourceId)
    }

    @Synchronized
    override fun play(
        resourceId: String,
        localPath: String,
    ) {
        if (resourceId.isBlank() || localPath.isBlank()) {
            mutableState.value = AudioPlaybackState.Failed(resourceId, AudioPlaybackFailure.MISSING_RESOURCE)
            return
        }
        if (mutableState.value == AudioPlaybackState.Paused(resourceId)) {
            resume()
            return
        }
        try {
            player.stop()
            player.play(localPath)
            mutableState.value = AudioPlaybackState.Playing(resourceId)
        } catch (error: AudioPlayerException) {
            mutableState.value = AudioPlaybackState.Failed(resourceId, error.reason)
        } catch (_: Exception) {
            mutableState.value = AudioPlaybackState.Failed(resourceId, AudioPlaybackFailure.UNKNOWN)
        }
    }

    @Synchronized
    override fun pause() {
        val current = mutableState.value as? AudioPlaybackState.Playing ?: return
        try {
            player.pause()
            mutableState.value = AudioPlaybackState.Paused(current.resourceId)
        } catch (_: Exception) {
            mutableState.value = AudioPlaybackState.Failed(current.resourceId, AudioPlaybackFailure.UNKNOWN)
        }
    }

    @Synchronized
    override fun resume() {
        val current = mutableState.value as? AudioPlaybackState.Paused ?: return
        try {
            player.resume()
            mutableState.value = AudioPlaybackState.Playing(current.resourceId)
        } catch (error: AudioPlayerException) {
            mutableState.value = AudioPlaybackState.Failed(current.resourceId, error.reason)
        } catch (_: Exception) {
            mutableState.value = AudioPlaybackState.Failed(current.resourceId, AudioPlaybackFailure.UNKNOWN)
        }
    }

    @Synchronized
    override fun fail(
        resourceId: String,
        reason: AudioPlaybackFailure,
    ) {
        player.stop()
        mutableState.value = AudioPlaybackState.Failed(resourceId, reason)
    }

    @Synchronized
    override fun stop() {
        player.stop()
        mutableState.value = AudioPlaybackState.Idle
    }
}

private fun AudioPlaybackState.resourceIdOrNull(): String? =
    when (this) {
        AudioPlaybackState.Idle -> null
        is AudioPlaybackState.Preparing -> resourceId
        is AudioPlaybackState.Playing -> resourceId
        is AudioPlaybackState.Paused -> resourceId
        is AudioPlaybackState.Failed -> resourceId
    }
