package com.proyectonami.app.feature.chat

import android.Manifest
import androidx.compose.ui.test.DeviceConfigurationOverride
import androidx.compose.ui.test.FontScale
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.lifecycle.SavedStateHandle
import androidx.test.platform.app.InstrumentationRegistry
import com.proyectonami.app.core.designsystem.theme.NamiTheme
import com.proyectonami.app.core.domain.audio.AudioClock
import com.proyectonami.app.core.domain.audio.AudioNoteMetadata
import com.proyectonami.app.core.domain.audio.AudioNotesRepository
import com.proyectonami.app.core.domain.audio.AudioRecorder
import com.proyectonami.app.core.domain.audio.AudioRecordingResult
import com.proyectonami.app.core.domain.audio.AudioRecordingState
import com.proyectonami.app.core.domain.audio.AudioUploadFailure
import com.proyectonami.app.core.domain.audio.AudioUploadState
import com.proyectonami.app.core.domain.audio.UploadAudioNoteUseCase
import com.proyectonami.app.core.domain.messages.ObserveTextMessagesUseCase
import com.proyectonami.app.core.domain.messages.OutgoingTextMessage
import com.proyectonami.app.core.domain.messages.SendTextMessageUseCase
import com.proyectonami.app.core.domain.messages.TextMessageFailure
import com.proyectonami.app.core.domain.messages.TextMessageSendResult
import com.proyectonami.app.core.domain.messages.TextMessagesRemoteState
import com.proyectonami.app.core.domain.messages.TextMessagesRepository
import com.proyectonami.app.feature.audio.presentation.AudioRecordingViewModel
import com.proyectonami.app.feature.chat.presentation.TextConversationRoute
import com.proyectonami.app.feature.chat.presentation.TextConversationViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class TextConversationScreenTest {
    @get:Rule val composeRule = createComposeRule()

    @Test
    fun emptyFieldIsDisabledAndLabelsRemainVisibleAt200Percent() {
        val viewModel = viewModel(FakeTextMessagesRepository())
        composeRule.setContent {
            DeviceConfigurationOverride(DeviceConfigurationOverride.FontScale(2f)) {
                NamiTheme { TextConversationRoute(viewModel, onBack = {}) }
            }
        }

        composeRule.onNodeWithText("Escribe un mensaje").assertIsDisplayed()
        composeRule.onNodeWithText("Enviar").assertIsNotEnabled()
        composeRule.onNodeWithText("Volver").assertIsDisplayed()
    }

    @Test
    fun sendShowsConfirmationAndRetryUsesTheSameClientId() {
        val repository =
            FakeTextMessagesRepository(listOf(TextMessageSendResult.Failed(TextMessageFailure.NETWORK), TextMessageSendResult.Sent))
        val viewModel = viewModel(repository)
        composeRule.setContent { NamiTheme { TextConversationRoute(viewModel, onBack = {}) } }

        composeRule.onNodeWithText("Escribe un mensaje").performTextInput("Hola desde inicio")
        composeRule.onNodeWithText("Enviar").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("No se pudo enviar el mensaje. El borrador se ha conservado para reintentarlo.").assertIsDisplayed()
        composeRule.onNodeWithText("Enviar").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Mensaje enviado").assertIsDisplayed()
        assertEquals(2, repository.sentMessages.size)
        assertEquals(repository.sentMessages[0].clientMessageId, repository.sentMessages[1].clientMessageId)
    }

    @Test
    fun microphoneActionOpensTheRecordingFlowInsideTheChat() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.uiAutomation.grantRuntimePermission(
            instrumentation.targetContext.packageName,
            Manifest.permission.RECORD_AUDIO,
        )
        val recorder = FakeAudioRecorder()
        val audioViewModel =
            AudioRecordingViewModel(
                recorder = recorder,
                clock = AudioClock { 0L },
                localPath = { "/tmp/audio-fictitious.m4a" },
            )
        composeRule.setContent {
            NamiTheme {
                TextConversationRoute(
                    viewModel = viewModel(FakeTextMessagesRepository()),
                    onBack = {},
                    audioRecordingViewModel = audioViewModel,
                )
            }
        }

        composeRule.onNodeWithText("Grabar nota de voz").performClick()
        composeRule.onNodeWithText("Nota de voz").assertIsDisplayed()
        composeRule.waitUntil { recorder.startCount == 1 }
        composeRule.onNodeWithText("Grabando: 0 s").assertIsDisplayed()
        composeRule.onNodeWithText("Detener grabación").assertIsDisplayed()
        composeRule.onNodeWithText("Cerrar").performClick()
        composeRule.onNodeWithText("Escribe un mensaje").assertIsDisplayed()
    }

    @Test
    fun failedAudioUploadCanRetryWithTheSameUuid() {
        val recorder = FakeAudioRecorder()
        val audioRepository = FakeAudioNotesRepository()
        val audioViewModel =
            AudioRecordingViewModel(
                recorder = recorder,
                clock = AudioClock { 1_000L },
                localPath = { "/tmp/audio-fictitious.m4a" },
                conversationId = "conversation-fictitious",
                senderId = { "sender-fictitious" },
                uploadAudioNote = UploadAudioNoteUseCase(audioRepository),
            )
        audioViewModel.start()
        composeRule.waitUntil { audioViewModel.state.value is AudioRecordingState.Recording }
        audioViewModel.stop()
        composeRule.waitUntil { audioViewModel.state.value is AudioRecordingState.Preview }
        composeRule.setContent {
            NamiTheme {
                TextConversationRoute(
                    viewModel = viewModel(FakeTextMessagesRepository()),
                    onBack = {},
                    audioRecordingViewModel = audioViewModel,
                )
            }
        }

        composeRule.onNodeWithText("Grabar nota de voz").performClick()
        composeRule.onAllNodesWithText("Enviar")[1].performClick()
        composeRule
            .onNodeWithText("No se pudo enviar la nota de voz. El archivo se conserva para reintentarlo o borrarlo.")
            .assertIsDisplayed()
        composeRule.onNodeWithText("Borrar").assertIsDisplayed()
        composeRule.onNodeWithText("Reintentar").assertIsDisplayed().performClick()
        composeRule.onNodeWithText("Nota de voz enviada").assertIsDisplayed()
        assertEquals(2, audioRepository.uploads.size)
        assertEquals(audioRepository.uploads[0].clientMessageId, audioRepository.uploads[1].clientMessageId)
    }

    private fun viewModel(repository: TextMessagesRepository) =
        TextConversationViewModel(
            conversationId = "conversation-fictitious",
            senderId = { "sender-fictitious" },
            observeTextMessages = ObserveTextMessagesUseCase(repository),
            sendTextMessage = SendTextMessageUseCase(repository),
            savedStateHandle = SavedStateHandle(),
        )

    private class FakeTextMessagesRepository(
        private val outcomes: List<TextMessageSendResult> = listOf(TextMessageSendResult.Sent),
    ) : TextMessagesRepository {
        val sentMessages = mutableListOf<OutgoingTextMessage>()
        private var index = 0

        override suspend fun send(message: OutgoingTextMessage): TextMessageSendResult {
            sentMessages += message
            return outcomes.getOrElse(index++) { TextMessageSendResult.Sent }
        }

        override fun observe(conversationId: String) = flowOf(TextMessagesRemoteState.Loaded(emptyList()))
    }

    private class FakeAudioRecorder : AudioRecorder {
        var startCount = 0

        override suspend fun start(localPath: String): Result<Unit> {
            startCount++
            return Result.success(Unit)
        }

        override suspend fun stop() = Result.success(AudioRecordingResult("/tmp/audio-fictitious.m4a", 1_000L, 1_000L))

        override suspend fun cancel() = Unit
    }

    private class FakeAudioNotesRepository : AudioNotesRepository {
        val uploads = mutableListOf<AudioNoteMetadata>()

        override fun upload(
            metadata: AudioNoteMetadata,
            localPath: String,
        ): Flow<AudioUploadState> =
            kotlinx.coroutines.flow.flow {
                uploads += metadata
                emit(AudioUploadState.Pending)
                emit(AudioUploadState.Progress(0.5f))
                if (uploads.size == 1) {
                    emit(AudioUploadState.Failed(metadata, AudioUploadFailure.NETWORK))
                } else {
                    emit(AudioUploadState.Sent(metadata))
                }
            }

        override suspend fun deleteLocal(localPath: String) = Result.success(Unit)
    }
}
