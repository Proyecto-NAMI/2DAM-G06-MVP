package com.proyectonami.app.feature.chat

import androidx.compose.ui.test.DeviceConfigurationOverride
import androidx.compose.ui.test.FontScale
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.lifecycle.SavedStateHandle
import com.proyectonami.app.core.designsystem.theme.NamiTheme
import com.proyectonami.app.core.domain.messages.ObserveTextMessagesUseCase
import com.proyectonami.app.core.domain.messages.OutgoingTextMessage
import com.proyectonami.app.core.domain.messages.SendTextMessageUseCase
import com.proyectonami.app.core.domain.messages.TextMessageFailure
import com.proyectonami.app.core.domain.messages.TextMessageSendResult
import com.proyectonami.app.core.domain.messages.TextMessagesRemoteState
import com.proyectonami.app.core.domain.messages.TextMessagesRepository
import com.proyectonami.app.feature.chat.presentation.TextConversationRoute
import com.proyectonami.app.feature.chat.presentation.TextConversationViewModel
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
}
