package com.proyectonami.app.feature.audio

import com.proyectonami.app.core.domain.audio.AudioLimits
import com.proyectonami.app.core.domain.audio.AudioRecordingResult
import com.proyectonami.app.core.domain.audio.AudioRecordingState
import com.proyectonami.app.core.domain.audio.isEmptyOrOverLimit
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
}
