package com.proyectonami.app.feature.conversations

import androidx.compose.ui.test.DeviceConfigurationOverride
import androidx.compose.ui.test.FontScale
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.proyectonami.app.core.designsystem.theme.NamiTheme
import com.proyectonami.app.core.domain.conversations.ConversationSummary
import com.proyectonami.app.core.domain.conversations.ConversationsRemoteState
import com.proyectonami.app.core.domain.conversations.ConversationsRepository
import com.proyectonami.app.core.domain.conversations.ObserveConversationsUseCase
import com.proyectonami.app.feature.conversations.presentation.ConversationsRoute
import com.proyectonami.app.feature.conversations.presentation.ConversationsViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class ConversationsScreenTest {
    @get:Rule val composeRule = createComposeRule()

    @Test
    fun contentShowsAvatarUnreadCountAndNavigatesToConversation() {
        var openedConversation: String? = null
        val viewModel = viewModel(ConversationsRemoteState.Loaded(listOf(summary(avatarPath = "avatars/fictitious.png"))))

        composeRule.setContent {
            NamiTheme {
                ConversationsRoute(
                    viewModel = viewModel,
                    onOpenConversation = { openedConversation = it },
                    onAddContact = {},
                    onProfile = {},
                    onSignOut = {},
                )
            }
        }

        composeRule.onNodeWithContentDescription("Avatar de Persona Ficticia").assertIsDisplayed()
        composeRule.onNodeWithText("Persona Ficticia, 3 mensajes sin leer").assertIsDisplayed().performClick()
        assertEquals("conversation-fictitious", openedConversation)
    }

    @Test
    fun loadingEmptyAndErrorStatesAreAnnouncedAndRemainVisibleAt200Percent() {
        val states =
            listOf(
                ConversationsRemoteState.Loading to "Cargando conversaciones…",
                ConversationsRemoteState.Loaded(emptyList()) to "Aún no tienes conversaciones.",
                ConversationsRemoteState.Failed to "No se pudieron cargar las conversaciones. Comprueba la conexión.",
            )

        states.forEach { (state, expectedText) ->
            val viewModel = viewModel(state)
            composeRule.setContent {
                DeviceConfigurationOverride(DeviceConfigurationOverride.FontScale(2f)) {
                    NamiTheme {
                        ConversationsRoute(
                            viewModel = viewModel,
                            onOpenConversation = {},
                            onAddContact = {},
                            onProfile = {},
                            onSignOut = {},
                        )
                    }
                }
            }
            composeRule.onNodeWithText(expectedText).assertIsDisplayed()
        }
    }

    private fun viewModel(state: ConversationsRemoteState) =
        ConversationsViewModel(ObserveConversationsUseCase(FakeConversationsRepository(state)))

    private fun summary(avatarPath: String?) =
        ConversationSummary(
            id = "conversation-fictitious",
            otherParticipantId = "participant-fictitious",
            displayName = "Persona",
            surname = "Ficticia",
            avatarPath = avatarPath,
            unreadCount = 3,
            lastActivityAtEpochMillis = 1_000L,
        )

    private class FakeConversationsRepository(
        private val state: ConversationsRemoteState,
    ) : ConversationsRepository {
        override fun observeConversations(): Flow<ConversationsRemoteState> = flowOf(state)
    }
}
