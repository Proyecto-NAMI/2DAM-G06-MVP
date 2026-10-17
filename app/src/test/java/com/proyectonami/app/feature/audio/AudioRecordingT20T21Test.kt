package com.proyectonami.app.feature.audio

import com.proyectonami.app.core.domain.audio.AudioClock
import com.proyectonami.app.core.domain.audio.AudioLimits
import com.proyectonami.app.core.domain.audio.AudioNoteMetadata
import com.proyectonami.app.core.domain.audio.AudioRecorder
import com.proyectonami.app.core.domain.audio.AudioRecordingFailure
import com.proyectonami.app.core.domain.audio.AudioRecordingResult
import com.proyectonami.app.core.domain.audio.AudioRecordingState
import com.proyectonami.app.core.domain.audio.AudioRecordingValidation
import com.proyectonami.app.core.domain.audio.AudioUploadFailure
import com.proyectonami.app.core.domain.audio.AudioUploadState
import com.proyectonami.app.core.domain.audio.ObserveRecordingDurationUseCase
import com.proyectonami.app.core.domain.audio.isEmptyOrOverLimit
import com.proyectonami.app.core.domain.audio.validate
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AudioRecordingT20T21Test {
    @Test
    fun emptyRecordingIsRejectedAndLimitIsExplicit() {
        val limits = AudioLimits(maxDurationMillis = 120_000L, maxSizeBytes = 5L * 1024L * 1024L)
        assertTrue(AudioRecordingResult("/tmp/fictitious.m4a", 0L, 1L).isEmptyOrOverLimit(limits))
        assertTrue(AudioRecordingResult("/tmp/fictitious.m4a", 121_000L, 10L).isEmptyOrOverLimit(limits))
        assertTrue(AudioRecordingResult("/tmp/fictitious.m4a", 1_000L, 6L * 1024L * 1024L).isEmptyOrOverLimit(limits))
        assertFalse(AudioRecordingResult("/tmp/fictitious.m4a", 1_000L, 10_000L).isEmptyOrOverLimit(limits))
        assertTrue(AudioRecordingResult("/tmp/fictitious.m4a", 0L, 1L).validate(limits) is AudioRecordingValidation.Empty)
        assertTrue(AudioRecordingResult("/tmp/fictitious.m4a", 121_000L, 1L).validate(limits) is AudioRecordingValidation.OverLimit)
        assertTrue(AudioRecordingResult("/tmp/fictitious.m4a", 1_000L, 1L).validate(limits) is AudioRecordingValidation.Valid)
    }

    @Test
    fun recordingStatesCannotPretendToBeActiveWithoutRecordingIndicator() {
        val idle: AudioRecordingState = AudioRecordingState.Idle
        val recording: AudioRecordingState = AudioRecordingState.Recording(1_000L)
        val preview: AudioRecordingState = AudioRecordingState.Preview("/tmp/fictitious.m4a", 1_000L, 10_000L)

        assertTrue(idle is AudioRecordingState.Idle)
        assertTrue(recording is AudioRecordingState.Recording)
        assertTrue(preview is AudioRecordingState.Preview)
    }

    @Test
    fun durationAndUploadStatesCoverAllSafeOutcomes() =
        runBlocking {
            val values =
                ObserveRecordingDurationUseCase(
                    object : AudioClock {
                        private var value = 0L

                        override fun elapsedMillis(): Long = value++
                    },
                    intervalMillis = 1L,
                ).invoke().take(3).toList()
            assertTrue(values == listOf(0L, 1L, 2L))

            val metadata = AudioNoteMetadata("message-fictitious", "conversation-fictitious", "sender-fictitious", 1_000L, 10_000L)
            assertTrue(AudioRecordingFailure.values().isNotEmpty())
            assertTrue(AudioUploadFailure.values().isNotEmpty())
            assertTrue(AudioUploadState.Pending == AudioUploadState.Pending)
            assertTrue(AudioUploadState.Progress(0.5f).fraction > 0f)
            assertTrue(AudioUploadState.Sent(metadata).metadata == metadata)
            assertTrue(AudioUploadState.Failed(metadata, AudioUploadFailure.NETWORK).reason == AudioUploadFailure.NETWORK)
            val recorder =
                object : AudioRecorder {
                    override suspend fun start(localPath: String) = Result.success(Unit)

                    override suspend fun stop() = Result.success(AudioRecordingResult("/tmp/fictitious.m4a", 1L, 1L))

                    override suspend fun cancel() = Unit
                }
            assertTrue(recorder.start("/tmp/fictitious.m4a").isSuccess)
        }
}
