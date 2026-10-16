package com.proyectonami.app.core.domain.messages

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class SendTextMessageUseCaseTest {
    @Test
    fun createsUuidBeforeSendingAndPreservesText() {
        val message = message("  Hola, persona ficticia\nSegunda línea  ")

        assertEquals(message.clientMessageId, UUID.fromString(message.clientMessageId).toString())
        assertEquals(4, UUID.fromString(message.clientMessageId).version())
        assertEquals("  Hola, persona ficticia\nSegunda línea  ", message.text)
        assertNotEquals(message.clientMessageId, message(message.text).clientMessageId)
    }

    @Test
    fun rejectsBlankTextAndInvalidIdentifiers() {
        for (text in listOf("", " ", "\n\t", "\u00a0")) {
            assertThrows(IllegalArgumentException::class.java) { message(text) }
        }
        val valid = message()
        for (id in listOf("", "message-1", "1-1-1-1-1")) {
            assertThrows(IllegalArgumentException::class.java) { valid.copy(clientMessageId = id) }
        }
        for (id in listOf("", "  ", "conversation/other")) {
            assertThrows(IllegalArgumentException::class.java) { valid.copy(conversationId = id) }
            assertThrows(IllegalArgumentException::class.java) { valid.copy(senderId = id) }
        }
    }

    @Test
    fun emitsPendingThenSentWithTheSameImmutableMessage() =
        runBlocking {
            val message = message()
            val received = mutableListOf<OutgoingTextMessage>()
            val sender =
                SendTextMessageUseCase(
                    object : TextMessagesRepository {
                        override suspend fun send(message: OutgoingTextMessage): TextMessageSendResult {
                            received += message
                            return TextMessageSendResult.Sent
                        }
                    },
                )

            val states = sender(message).toList()

            assertEquals(listOf(TextMessageSendState.Pending(message), TextMessageSendState.Sent(message)), states)
            assertSame(message, received.single())
            assertTrue(states.all { it.message === message })
        }

    @Test
    fun remainsPendingUntilServerAcknowledges() =
        runBlocking {
            val acknowledgement = CompletableDeferred<TextMessageSendResult>()
            val sender =
                SendTextMessageUseCase(
                    object : TextMessagesRepository {
                        override suspend fun send(message: OutgoingTextMessage): TextMessageSendResult = acknowledgement.await()
                    },
                )
            val message = message()
            val states = mutableListOf<TextMessageSendState>()
            val sending = launch(start = CoroutineStart.UNDISPATCHED) { sender(message).toList(states) }

            assertEquals(listOf(TextMessageSendState.Pending(message)), states)
            acknowledgement.complete(TextMessageSendResult.Sent)
            sending.join()
            assertEquals(TextMessageSendState.Sent(message), states.last())
        }

    @Test
    fun exposesEachFailureAndKeepsMessageForRetry() =
        runBlocking {
            val message = message()
            for (reason in TextMessageFailure.entries) {
                val sender =
                    SendTextMessageUseCase(
                        object : TextMessagesRepository {
                            override suspend fun send(message: OutgoingTextMessage): TextMessageSendResult =
                                TextMessageSendResult.Failed(reason)
                        },
                    )
                assertEquals(
                    listOf(TextMessageSendState.Pending(message), TextMessageSendState.Failed(message, reason)),
                    sender(message).toList(),
                )
            }
        }

    @Test
    fun retriesAfterLostAcknowledgementWithoutCreatingAnotherMessage() =
        runBlocking {
            val stored = mutableMapOf<String, OutgoingTextMessage>()
            val attempts = mutableListOf<OutgoingTextMessage>()
            val sender =
                SendTextMessageUseCase(
                    object : TextMessagesRepository {
                        override suspend fun send(message: OutgoingTextMessage): TextMessageSendResult {
                            attempts += message
                            stored.putIfAbsent(message.clientMessageId, message)
                            return if (attempts.size == 1) {
                                TextMessageSendResult.Failed(TextMessageFailure.NETWORK)
                            } else {
                                TextMessageSendResult.Sent
                            }
                        }
                    },
                )
            val message = message()

            val first = sender(message).toList()
            val retry = sender(first.last().message).toList()

            assertEquals(TextMessageSendState.Failed(message, TextMessageFailure.NETWORK), first.last())
            assertEquals(TextMessageSendState.Sent(message), retry.last())
            assertEquals(1, stored.size)
            assertEquals(listOf(message.clientMessageId, message.clientMessageId), attempts.map { it.clientMessageId })
        }

    @Test
    fun cancellationDoesNotReportSentOrFailed() =
        runBlocking {
            val waiting = CompletableDeferred<Unit>()
            val sender =
                SendTextMessageUseCase(
                    object : TextMessagesRepository {
                        override suspend fun send(message: OutgoingTextMessage): TextMessageSendResult {
                            waiting.await()
                            return TextMessageSendResult.Sent
                        }
                    },
                )
            val message = message()
            val states = mutableListOf<TextMessageSendState>()
            val sending = launch(start = CoroutineStart.UNDISPATCHED) { sender(message).toList(states) }

            sending.cancelAndJoin()

            assertEquals(listOf(TextMessageSendState.Pending(message)), states)
            assertTrue(sending.isCancelled)
        }

    @Test
    fun propagatesRepositoryCancellation() =
        runBlocking {
            val sender =
                SendTextMessageUseCase(
                    object : TextMessagesRepository {
                        override suspend fun send(message: OutgoingTextMessage): TextMessageSendResult =
                            throw CancellationException("Cancelled test send")
                    },
                )
            val sending = async { sender(message()).toList() }

            try {
                sending.await()
                throw AssertionError("Cancellation must propagate")
            } catch (_: CancellationException) {
                assertTrue(sending.isCancelled)
            }
        }

    private fun message(text: String = "Mensaje ficticio"): OutgoingTextMessage =
        OutgoingTextMessage.create("conversation-fictitious", "uid-fictitious", text)
}
