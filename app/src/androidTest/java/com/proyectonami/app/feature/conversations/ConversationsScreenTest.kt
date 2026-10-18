package com.proyectonami.app.feature.conversations

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.DeviceConfigurationOverride
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.FontScale
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.accessibility.enableAccessibilityChecks
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.tryPerformAccessibilityChecks
import androidx.compose.ui.unit.dp
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
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalTestApi::class)
class ConversationsScreenTest {
    @get:Rule val composeRule = createComposeRule()

    @Before
    fun enableAccessibilityValidation() {
        composeRule.enableAccessibilityChecks()
    }

    @Test
    fun contentShowsAvatarUnreadCountAndNavigatesToConversation() {
        var openedConversation: String? = null
        var signOutRequests = 0
        var settingsRequests = 0
        val viewModel = viewModel(ConversationsRemoteState.Loaded(listOf(summary(avatarPath = "avatars/fictitious.png"))))

        composeRule.setContent {
            NamiTheme {
                ConversationsRoute(
                    viewModel = viewModel,
                    onOpenConversation = { openedConversation = it },
                    onAddContact = {},
                    onProfile = {},
                    onSettings = { settingsRequests++ },
                    onSignOut = { signOutRequests++ },
                )
            }
        }

        composeRule.onNodeWithText("Mis conversaciones").assertIsDisplayed()
        composeRule.onNode(SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading)).assertExists()
        composeRule.onNodeWithContentDescription("Avatar de Persona Ficticia").assertIsDisplayed()
        composeRule
            .onNodeWithText("Persona Ficticia, 3 mensajes sin leer")
            .assertIsDisplayed()
            .assertHeightIsAtLeast(48.dp)
            .performClick()
        assertEquals("conversation-fictitious", openedConversation)
        composeRule.onNodeWithText("Ajustes").performScrollTo().performClick()
        assertEquals(1, settingsRequests)
        composeRule.onNodeWithText("Cerrar sesión").performScrollTo().performClick()
        assertEquals(1, signOutRequests)
    }

    @Test
    fun loadingStateIsAnnouncedAndRemainsVisibleAt320DpAnd200Percent() {
        assertStateAt200Percent(320, ConversationsRemoteState.Loading, "Cargando conversaciones…")
    }

    @Test
    fun emptyStateIsAnnouncedAndRemainsVisibleAt411DpAnd200Percent() {
        assertStateAt200Percent(411, ConversationsRemoteState.Loaded(emptyList()), "Aún no tienes conversaciones.")
    }

    @Test
    fun errorStateIsAnnouncedAndRemainsVisibleAt320DpAnd200Percent() {
        assertStateAt200Percent(
            320,
            ConversationsRemoteState.Failed,
            "No se pudieron cargar las conversaciones. Comprueba la conexión.",
        )
    }

    private fun assertStateAt200Percent(
        widthDp: Int,
        state: ConversationsRemoteState,
        expectedText: String,
    ) {
        val viewModel = viewModel(state)
        composeRule.setContent {
            Box(Modifier.size(widthDp.dp, 720.dp)) {
                DeviceConfigurationOverride(DeviceConfigurationOverride.FontScale(2f)) {
                    NamiTheme {
                        ConversationsRoute(
                            viewModel = viewModel,
                            onOpenConversation = {},
                            onAddContact = {},
                            onProfile = {},
                            onSettings = {},
                            onSignOut = {},
                        )
                    }
                }
            }
        }
        composeRule.onNodeWithText(expectedText).assertIsDisplayed()
        composeRule.onRoot().tryPerformAccessibilityChecks()
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
