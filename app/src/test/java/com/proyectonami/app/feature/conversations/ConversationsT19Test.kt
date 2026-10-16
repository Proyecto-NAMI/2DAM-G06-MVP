package com.proyectonami.app.feature.conversations

import com.proyectonami.app.core.domain.conversations.ConversationSummary
import com.proyectonami.app.core.domain.conversations.ConversationsRemoteState
import com.proyectonami.app.core.domain.conversations.ConversationsRepository
import com.proyectonami.app.core.domain.conversations.ObserveConversationsUseCase
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class ConversationsT19Test {
    @Test
    fun homeOrdersConversationsByRecentActivityAndPreservesEmptyState() =
        runBlocking {
            val old = summary("old", 1_000L)
            val recent = summary("recent", 2_000L)
            val repository =
                object : ConversationsRepository {
                    override fun observeConversations() =
                        flowOf(ConversationsRemoteState.Loaded(listOf(old, recent)), ConversationsRemoteState.Loaded(emptyList()))
                }

            val states = mutableListOf<ConversationsRemoteState>()
            ObserveConversationsUseCase(repository)().collect(states::add)

            assertEquals(listOf(recent, old), (states[0] as ConversationsRemoteState.Loaded).conversations)
            assertEquals(emptyList<ConversationSummary>(), (states[1] as ConversationsRemoteState.Loaded).conversations)
        }

    @Test
    fun homePropagatesFailureForAccessibleRetryState() =
        runBlocking {
            val repository =
                object : ConversationsRepository {
                    override fun observeConversations() = flowOf(ConversationsRemoteState.Failed)
                }

            var state: ConversationsRemoteState? = null
            ObserveConversationsUseCase(repository)().collect { state = it }

            assertEquals(ConversationsRemoteState.Failed, state)
        }

    private fun summary(
        id: String,
        activity: Long,
    ) = ConversationSummary(id, "other-$id", "Persona", id, null, 0, activity)
}
