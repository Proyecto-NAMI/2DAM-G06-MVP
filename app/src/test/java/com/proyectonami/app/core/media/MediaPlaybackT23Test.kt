package com.proyectonami.app.core.media

import com.proyectonami.app.core.domain.media.AudioPlaybackFailure
import com.proyectonami.app.core.domain.media.AudioPlaybackSourceResult
import com.proyectonami.app.core.domain.media.AudioPlaybackState
import com.proyectonami.app.core.domain.media.AudioPlayer
import com.proyectonami.app.core.domain.media.AudioPlayerException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MediaPlaybackT23Test {
    @Test
    fun onlyOneResourcePlaysAndPauseIsAvailable() {
        val player = FakePlayer()
        val coordinator = MediaPlaybackCoordinator(player)
        coordinator.play("audio-one")
        coordinator.play("audio-two")
        coordinator.pause()
        coordinator.resume()

        assertEquals(listOf("stop", "play:audio-one", "stop", "play:audio-two", "pause", "resume"), player.events)
        assertEquals(AudioPlaybackState.Playing("audio-two"), coordinator.state.value)
    }

    @Test
    fun missingOrUndecodableResourcesBecomeRecoverableErrors() {
        val coordinator = MediaPlaybackCoordinator(FakePlayer(throwOnPlay = true))
        coordinator.play("")
        assertEquals(AudioPlaybackState.Failed("", AudioPlaybackFailure.MISSING_RESOURCE), coordinator.state.value)
        coordinator.play("bad-resource")
        assertEquals(AudioPlaybackState.Failed("bad-resource", AudioPlaybackFailure.NOT_DECODABLE), coordinator.state.value)
        coordinator.stop()
        assertTrue(coordinator.state.value is AudioPlaybackState.Idle)
    }

    @Test
    fun completionReturnsPreviewPlaybackToIdle() {
        val player = FakePlayer()
        val coordinator = MediaPlaybackCoordinator(player)
        coordinator.play("audio-preview")

        player.complete()

        assertTrue(coordinator.state.value is AudioPlaybackState.Idle)
    }

    @Test
    fun preparationAndDownloadResultsKeepTheLogicalResourceId() {
        val coordinator = MediaPlaybackCoordinator(FakePlayer())

        coordinator.prepare("audio-pending")
        assertEquals(AudioPlaybackState.Preparing("audio-pending"), coordinator.state.value)

        coordinator.fail("audio-pending", AudioPlaybackFailure.NOT_AUTHORIZED)
        assertEquals(
            AudioPlaybackState.Failed("audio-pending", AudioPlaybackFailure.NOT_AUTHORIZED),
            coordinator.state.value,
        )
        assertEquals("private-cache.m4a", AudioPlaybackSourceResult.Ready("private-cache.m4a").localPath)
        assertEquals(
            AudioPlaybackFailure.NETWORK,
            AudioPlaybackSourceResult.Failed(AudioPlaybackFailure.NETWORK).reason,
        )
    }

    private class FakePlayer(
        private val throwOnPlay: Boolean = false,
    ) : AudioPlayer {
        val events = mutableListOf<String>()
        private var completionListener: () -> Unit = {}

        override fun setOnCompletionListener(listener: () -> Unit) {
            completionListener = listener
        }

        override fun play(localPath: String) {
            events += "play:$localPath"
            if (throwOnPlay) throw AudioPlayerException(AudioPlaybackFailure.NOT_DECODABLE)
        }

        override fun pause() {
            events += "pause"
        }

        override fun resume() {
            events += "resume"
        }

        override fun stop() {
            events += "stop"
        }

        fun complete() = completionListener()
    }
}
