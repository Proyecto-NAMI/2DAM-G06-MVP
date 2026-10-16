package com.proyectonami.app.core.domain.messages

import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TextConversationT17Test {
    @Test
    fun blankDraftsAreNotSendable() {
        assertNull(normalizeTextDraft("   \n  \t"))
        assertEquals("Hola", normalizeTextDraft("  Hola  "))
    }

    @Test
    fun retryReusesTheOriginalClientMessageId() {
        val first = prepareOutgoingTextMessage("conversation-fictitious", "sender-fictitious", "Hola")
        val retry =
            prepareOutgoingTextMessage(
                "conversation-fictitious",
                "sender-fictitious",
                " Hola ",
                retryMessage = first,
            )

        assertEquals(first, retry)
        assertNull(prepareOutgoingTextMessage("conversation-fictitious", "sender-fictitious", "   "))
    }

    @Test
    fun historyIsOrderedOldestFirstAndEmptyStateIsPreserved() =
        runBlocking {
            val newest = TextMessageRecord("new", "sender", "dos", 2_000L, "sent")
            val oldest = TextMessageRecord("old", "sender", "uno", 1_000L, "sent")
            val repository =
                object : TextMessagesRepository {
                    override suspend fun send(message: OutgoingTextMessage): TextMessageSendResult = TextMessageSendResult.Sent

                    override fun observe(conversationId: String) = flowOf(TextMessagesRemoteState.Loaded(listOf(newest, oldest)))
                }

            val state =
                ObserveTextMessagesUseCase(repository)("conversation-fictitious").let { flow ->
                    var result: TextMessagesRemoteState? = null
                    flow.collect { result = it }
                    result
                }
            assertEquals(
                listOf(oldest, newest),
                (state as TextMessagesRemoteState.Loaded).messages,
            )
        }
}
