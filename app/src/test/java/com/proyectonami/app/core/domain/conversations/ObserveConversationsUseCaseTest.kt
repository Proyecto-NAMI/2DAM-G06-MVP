package com.proyectonami.app.core.domain.conversations

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ObserveConversationsUseCaseTest {
    @Test
    fun returnsOnlyAuthorizedRecordsWithContactDetailsAndUnreadCount() {
        val summary =
            ConversationMapper.toSummary(
                record = conversationRecord("conversation-1", listOf("uid-owner", "uid-contact"), 4),
                currentUserId = "uid-owner",
            )

        assertEquals("Persona", summary?.displayName)
        assertEquals("Ficticia", summary?.surname)
        assertEquals("avatars/ficticio.png", summary?.avatarPath)
        assertEquals(4, summary?.unreadCount)
        assertEquals("uid-contact", summary?.otherParticipantId)
        assertNull(
            ConversationMapper.toSummary(
                conversationRecord("conversation-private", listOf("uid-other", "uid-contact"), 9),
                currentUserId = "uid-owner",
            ),
        )
    }

    @Test
    fun sortsConversationsByMostRecentActivity() =
        runBlocking {
            val older = summary("older", 100L)
            val newer = summary("newer", 300L)
            val middle = summary("middle", 200L)
            val useCase =
                ObserveConversationsUseCase(FakeConversationsRepository(ConversationsRemoteState.Loaded(listOf(older, newer, middle))))

            val state = useCase().first() as ConversationsRemoteState.Loaded

            assertEquals(listOf("newer", "middle", "older"), state.conversations.map { it.id })
        }

    @Test
    fun emitsEmptyStateWithoutInventingConversations() =
        runBlocking {
            val useCase = ObserveConversationsUseCase(FakeConversationsRepository(ConversationsRemoteState.Loaded(emptyList())))

            val state = useCase().first() as ConversationsRemoteState.Loaded

            assertTrue(state.conversations.isEmpty())
        }

    @Test
    fun preservesRecoverableRemoteError() =
        runBlocking {
            val useCase = ObserveConversationsUseCase(FakeConversationsRepository(ConversationsRemoteState.Failed))

            assertEquals(ConversationsRemoteState.Failed, useCase().first())
        }

    @Test
    fun defaultsMissingUnreadCountAndAllowsMissingAvatar() {
        val record =
            conversationRecord("conversation-no-avatar", listOf("uid-owner", "uid-contact"), unreadCount = null)
                .copy(participantDetails = mapOf("uid-contact" to ConversationParticipant("Persona", "Ficticia", null)))

        val summary = ConversationMapper.toSummary(record, "uid-owner")

        assertEquals(0, summary?.unreadCount)
        assertNull(summary?.avatarPath)
    }

    private fun summary(
        id: String,
        lastActivity: Long,
    ) = ConversationSummary(
        id = id,
        otherParticipantId = "uid-contact",
        displayName = "Persona",
        surname = "Ficticia",
        avatarPath = null,
        unreadCount = 0,
        lastActivityAtEpochMillis = lastActivity,
    )

    private fun conversationRecord(
        id: String,
        participantIds: List<String>,
        unreadCount: Long?,
    ) = ConversationRecord(
        id = id,
        participantIds = participantIds,
        participantDetails =
            mapOf(
                "uid-contact" to ConversationParticipant("Persona", "Ficticia", "avatars/ficticio.png"),
            ),
        unreadCountByUser = unreadCount?.let { mapOf("uid-owner" to it) } ?: emptyMap(),
        lastActivityAtEpochMillis = 100,
    )

    private class FakeConversationsRepository(
        private val state: ConversationsRemoteState,
    ) : ConversationsRepository {
        override fun observeConversations(): Flow<ConversationsRemoteState> = flowOf(state)
    }
}
