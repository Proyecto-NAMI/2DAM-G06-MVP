package com.proyectonami.app.feature.audio

import androidx.compose.ui.test.DeviceConfigurationOverride
import androidx.compose.ui.test.FontScale
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import com.proyectonami.app.core.designsystem.theme.NamiTheme
import com.proyectonami.app.core.domain.audio.AudioClock
import com.proyectonami.app.core.domain.audio.AudioLimits
import com.proyectonami.app.core.domain.audio.AudioRecorder
import com.proyectonami.app.core.domain.audio.AudioRecordingFailure
import com.proyectonami.app.core.domain.audio.AudioRecordingResult
import com.proyectonami.app.core.domain.audio.AudioRecordingState
import com.proyectonami.app.core.domain.media.AudioPlaybackState
import com.proyectonami.app.feature.audio.presentation.AudioRecordingContent
import com.proyectonami.app.feature.audio.presentation.AudioRecordingViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class AudioRecordingScreenTest {
    @get:Rule val composeRule = createComposeRule()

    @Test
    fun previewShowsDurationAndListenDeleteSendActions() {
        val events = mutableListOf<String>()
        composeRule.setContent {
            NamiTheme {
                AudioRecordingContent(
                    state = AudioRecordingState.Preview("audio-fictitious.m4a", 4_000L, 2_000L),
                    playbackState = AudioPlaybackState.Idle,
                    onRecord = {},
                    onStop = {},
                    onListen = { events += "listen" },
                    onDelete = { events += "delete" },
                    onSend = { events += "send" },
                    onOpenSettings = {},
                    onContinueWithText = {},
                )
            }
        }

        composeRule.onNodeWithText("Nota preparada: 4 s. Escúchala, bórrala o envíala.").assertIsDisplayed()
        composeRule.onNodeWithText("Escuchar").assertHeightIsAtLeast(48.dp).performClick()
        composeRule.onNodeWithText("Borrar").assertHeightIsAtLeast(48.dp).performClick()
        composeRule.onNodeWithText("Enviar").assertHeightIsAtLeast(48.dp).performClick()
        assertEquals(listOf("listen", "delete", "send"), events)
    }

    @Test
    fun permanentPermissionDenialExplainsSettingsAndKeepsTextAvailable() {
        var settingsRequests = 0
        var continueRequests = 0
        composeRule.setContent {
            NamiTheme {
                AudioRecordingContent(
                    state = AudioRecordingState.PermissionDeniedPermanent,
                    playbackState = AudioPlaybackState.Idle,
                    onRecord = {},
                    onStop = {},
                    onListen = {},
                    onDelete = {},
                    onSend = {},
                    onOpenSettings = { settingsRequests++ },
                    onContinueWithText = { continueRequests++ },
                )
            }
        }

        composeRule
            .onNodeWithText(
                "El permiso del micrófono está desactivado. Puedes habilitarlo en Ajustes o continuar escribiendo.",
            ).assertIsDisplayed()
        composeRule.onNodeWithText("Abrir Ajustes").performClick()
        composeRule.onNodeWithText("Continuar con texto").performClick()
        assertEquals(1, settingsRequests)
        assertEquals(1, continueRequests)
    }

    @Test
    fun recordingControlsRemainVisibleAt200Percent() {
        composeRule.setContent {
            DeviceConfigurationOverride(DeviceConfigurationOverride.FontScale(2f)) {
                NamiTheme {
                    AudioRecordingContent(
                        state = AudioRecordingState.Recording(12_000L),
                        playbackState = AudioPlaybackState.Idle,
                        onRecord = {},
                        onStop = {},
                        onListen = {},
                        onDelete = {},
                        onSend = {},
                        onOpenSettings = {},
                        onContinueWithText = {},
                    )
                }
            }
        }

        composeRule.onNodeWithText("Grabando: 12 s").assertIsDisplayed()
        composeRule.onNodeWithText("Detener grabación").assertIsDisplayed().assertHeightIsAtLeast(48.dp)
    }

    @Test
    fun emptyAndOversizedRecordingsAreDeletedIntoRecoverableStates() {
        val emptyRecorder = FakeAudioRecorder(AudioRecordingResult("audio-empty.m4a", 0L, 10L))
        val emptyViewModel = viewModel(emptyRecorder)
        emptyViewModel.start()
        composeRule.waitUntil { emptyViewModel.state.value is AudioRecordingState.Recording }
        emptyViewModel.stop()
        composeRule.waitUntil { emptyViewModel.state.value is AudioRecordingState.Failed }
        assertEquals(AudioRecordingFailure.EMPTY_RECORDING, (emptyViewModel.state.value as AudioRecordingState.Failed).reason)
        assertEquals(1, emptyRecorder.cancelCount)

        val oversizedRecorder = FakeAudioRecorder(AudioRecordingResult("audio-large.m4a", 1_000L, 6L * 1024L * 1024L))
        val oversizedViewModel = viewModel(oversizedRecorder)
        oversizedViewModel.start()
        composeRule.waitUntil { oversizedViewModel.state.value is AudioRecordingState.Recording }
        oversizedViewModel.stop()
        composeRule.waitUntil { oversizedViewModel.state.value is AudioRecordingState.LimitReached }
        assertEquals(1, oversizedRecorder.cancelCount)
    }

    @Test
    fun leavingTheScreenCancelsAnActiveRecording() {
        val recorder = FakeAudioRecorder(AudioRecordingResult("audio-fictitious.m4a", 1_000L, 1_000L))
        val viewModel = viewModel(recorder)
        viewModel.start()
        composeRule.waitUntil { viewModel.state.value is AudioRecordingState.Recording }

        viewModel.onScreenExit()

        composeRule.waitUntil { viewModel.state.value is AudioRecordingState.Interrupted }
        assertTrue(recorder.cancelCount >= 1)
    }

    private fun viewModel(recorder: AudioRecorder) =
        AudioRecordingViewModel(
            recorder = recorder,
            clock = AudioClock { 0L },
            localPath = { "audio-fictitious.m4a" },
            limits = AudioLimits(),
        )

    private class FakeAudioRecorder(
        private val result: AudioRecordingResult,
    ) : AudioRecorder {
        var cancelCount = 0

        override suspend fun start(localPath: String) = Result.success(Unit)

        override suspend fun stop() = Result.success(result)

        override suspend fun cancel() {
            cancelCount++
        }
    }
}
