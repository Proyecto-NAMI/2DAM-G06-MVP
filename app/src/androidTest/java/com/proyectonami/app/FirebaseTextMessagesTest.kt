package com.proyectonami.app

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Source
import com.proyectonami.app.core.data.firebase.FirebaseEnvironment
import com.proyectonami.app.core.data.firebase.FirebasePaths
import com.proyectonami.app.core.data.firebase.FirebaseServicesFactory
import com.proyectonami.app.core.data.firebase.FirebaseTextMessagesRepository
import com.proyectonami.app.core.domain.messages.OutgoingTextMessage
import com.proyectonami.app.core.domain.messages.SendTextMessageUseCase
import com.proyectonami.app.core.domain.messages.TextMessageFailure
import com.proyectonami.app.core.domain.messages.TextMessageSendResult
import com.proyectonami.app.core.domain.messages.TextMessageSendState
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.net.InetSocketAddress
import java.net.Socket
import java.util.UUID
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class FirebaseTextMessagesTest {
    private val environment = FirebaseEnvironment.fromBuildConfig()
    private val context = ApplicationProvider.getApplicationContext<NamiApplication>()
    private val services = FirebaseServicesFactory.create(context, environment)
    private val repository = FirebaseTextMessagesRepository(services.firestore, services.auth)
    private lateinit var senderId: String
    private lateinit var conversationId: String

    @Before
    fun setUp() {
        assumeTrue(environment.useEmulators)
        for (port in listOf(environment.authEmulatorPort, environment.firestoreEmulatorPort)) {
            assumeTrue(
                "Firebase Auth/Firestore Emulator no está en ejecución",
                runCatching {
                    Socket().use { it.connect(InetSocketAddress(environment.emulatorHost, port), 1_000) }
                }.isSuccess,
            )
        }
        services.auth.signOut()
        senderId = requireNotNull(await(services.auth.signInAnonymously()).user?.uid)
        conversationId = "text-test-${UUID.randomUUID()}"
    }

    private fun seedConversation() {
        await(
            conversation().set(
                mapOf(
                    "participantIds" to listOf(senderId, RECIPIENT_ID),
                    "unreadCountByUser" to mapOf(senderId to 2L, RECIPIENT_ID to 3L),
                ),
            ),
        )
    }

    @After
    fun tearDown() {
        await(services.firestore.enableNetwork())
        services.auth.signOut()
    }

    @Test
    fun retriesAndNewRepositoryKeepOneCopyAndOneUnreadIncrement() =
        runBlocking {
            seedConversation()
            val message = message()
            val sender = SendTextMessageUseCase(repository)
            assertEquals(
                listOf(TextMessageSendState.Pending(message), TextMessageSendState.Sent(message)),
                sendStates(sender, message),
            )
            val original = await(document(message).get(Source.SERVER))
            val activity = await(conversation().get(Source.SERVER)).getTimestamp("lastActivityAt")
            assertNotNull(original.getTimestamp("createdAt"))
            assertNotNull(activity)
            assertEquals(message.clientMessageId, original.id)
            assertEquals(message.clientMessageId, original.getString("clientMessageId"))
            assertEquals(message.text, original.getString("text"))
            assertEquals(senderId, original.getString("senderId"))
            assertEquals("sent", original.getString("status"))

            assertEquals(TextMessageSendResult.Sent, repository.send(message))
            val recreated = FirebaseTextMessagesRepository(services.firestore, services.auth)
            assertEquals(TextMessageSendResult.Sent, recreated.send(message))

            val messages = await(conversation().collection("messages").get(Source.SERVER))
            assertEquals(1, messages.size())
            assertEquals(original.getTimestamp("createdAt"), messages.documents.single().getTimestamp("createdAt"))
            val updated = await(conversation().get(Source.SERVER))
            assertEquals(activity, updated.getTimestamp("lastActivityAt"))
            assertEquals(4L, updated.getLong("unreadCountByUser.$RECIPIENT_ID"))
            assertEquals(2L, updated.getLong("unreadCountByUser.$senderId"))
        }

    @Test
    fun concurrentAttemptsStoreOneCopyAndDistinctMessagesRemainDistinct() =
        runBlocking {
            seedConversation()
            val message = message()
            val results = List(4) { async { repository.send(message) } }.awaitAll()
            assertTrue(results.all { it == TextMessageSendResult.Sent })
            assertEquals(1, await(conversation().collection("messages").get(Source.SERVER)).size())
            assertEquals(4L, await(conversation().get(Source.SERVER)).getLong("unreadCountByUser.$RECIPIENT_ID"))

            assertEquals(TextMessageSendResult.Sent, repository.send(message()))
            assertEquals(2, await(conversation().collection("messages").get(Source.SERVER)).size())
            assertEquals(5L, await(conversation().get(Source.SERVER)).getLong("unreadCountByUser.$RECIPIENT_ID"))
        }

    @Test
    fun changedPayloadWithSameUuidFailsWithoutOverwriting() =
        runBlocking {
            seedConversation()
            val message = message()
            assertEquals(TextMessageSendResult.Sent, repository.send(message))

            assertEquals(
                TextMessageSendResult.Failed(TextMessageFailure.ID_CONFLICT),
                repository.send(message.copy(text = "Otro texto ficticio")),
            )
            assertEquals(message.text, await(document(message).get(Source.SERVER)).getString("text"))
            assertEquals(1, await(conversation().collection("messages").get(Source.SERVER)).size())
        }

    @Test
    fun unavailableFirestoreRetainsUuidAndRetriesSuccessfully() =
        runBlocking {
            seedConversation()
            val message = message()
            val unavailableApp =
                FirebaseApp.initializeApp(
                    context,
                    environment.toFirebaseOptions(),
                    "nami-unavailable-${UUID.randomUUID()}",
                )
            val unavailableFirestore = FirebaseFirestore.getInstance(unavailableApp)
            unavailableFirestore.useEmulator("127.0.0.1", 1)
            val sender = SendTextMessageUseCase(repository)
            try {
                val failingSender =
                    SendTextMessageUseCase(
                        FirebaseTextMessagesRepository(unavailableFirestore, services.auth),
                    )
                assertEquals(
                    listOf(
                        TextMessageSendState.Pending(message),
                        TextMessageSendState.Failed(message, TextMessageFailure.NETWORK),
                    ),
                    sendStates(failingSender, message),
                )
            } finally {
                await(unavailableFirestore.terminate())
                unavailableApp.delete()
            }

            assertEquals(TextMessageSendState.Sent(message), sendStates(sender, message).last())
            assertEquals(1, await(conversation().collection("messages").get(Source.SERVER)).size())
        }

    @Test
    fun unauthenticatedOrSpoofedSenderCannotSend() =
        runBlocking {
            val message = message()
            assertEquals(
                TextMessageSendResult.Failed(TextMessageFailure.NOT_AUTHORIZED),
                repository.send(message.copy(senderId = RECIPIENT_ID)),
            )
            services.auth.signOut()
            assertEquals(
                TextMessageSendResult.Failed(TextMessageFailure.UNAUTHENTICATED),
                repository.send(message),
            )
        }

    @Test
    fun thirdPartyCannotReadOrWriteMessagesEvenBypassingRepository() =
        runBlocking {
            seedConversation()
            val message = message()
            assertEquals(TextMessageSendResult.Sent, repository.send(message))
            services.auth.signOut()
            val thirdPartyId = requireNotNull(await(services.auth.signInAnonymously()).user?.uid)

            assertEquals(
                TextMessageSendResult.Failed(TextMessageFailure.NOT_AUTHORIZED),
                repository.send(message.copy(senderId = thirdPartyId)),
            )
            assertPermissionDenied { await(document(message).get(Source.SERVER)) }
            val forged = message().copy(senderId = thirdPartyId)
            assertPermissionDenied {
                await(document(forged).set(payload(forged)))
            }
        }

    @Test
    fun rulesRejectInvalidPayloadAndPreventEditingCommittedMessages() =
        runBlocking {
            seedConversation()
            val message = message()
            val valid = payload(message)
            val invalid =
                listOf(
                    valid + ("clientMessageId" to UUID.randomUUID().toString()),
                    valid + ("senderId" to RECIPIENT_ID),
                    valid + ("text" to " \n\t "),
                    valid + ("status" to "pending"),
                    valid + (
                        "createdAt" to
                            com.google.firebase.Timestamp
                                .now()
                    ),
                    valid + ("unexpectedField" to "fictitious"),
                )
            for (data in invalid) {
                assertPermissionDenied { await(document(message).set(data)) }
            }
            assertPermissionDenied {
                await(conversation().collection("messages").document("not-a-uuid").set(valid))
            }
            assertEquals(TextMessageSendResult.Sent, repository.send(message))
            assertPermissionDenied { await(document(message).update("text", "Cambio ficticio")) }
            assertPermissionDenied { await(document(message).delete()) }
        }

    private fun message(): OutgoingTextMessage = OutgoingTextMessage.create(conversationId, senderId, "Hola, persona ficticia")

    private fun conversation() = services.firestore.document(FirebasePaths.conversation(conversationId))

    private fun document(message: OutgoingTextMessage) =
        services.firestore.document(FirebasePaths.message(message.conversationId, message.clientMessageId))

    private fun payload(message: OutgoingTextMessage): Map<String, Any> =
        mapOf(
            "clientMessageId" to message.clientMessageId,
            "senderId" to message.senderId,
            "type" to "text",
            "text" to message.text,
            "createdAt" to FieldValue.serverTimestamp(),
            "status" to "sent",
        )

    private suspend fun sendStates(
        sender: SendTextMessageUseCase,
        message: OutgoingTextMessage,
    ): List<TextMessageSendState> = withTimeout(60_000L) { sender(message).toList() }

    /**
     * La primera operación contra Firestore después de arrancar el proceso puede
     * tardar mientras se abre el canal con el emulador, y en CI comparten la
     * máquina cuatro emuladores y el emulador de Android. El límite anterior de
     * 20 s se agotaba en ese primer envío y hacía fallar la preparación de la
     * prueba sin que hubiera ningún error de datos.
     */
    private fun <T> await(task: Task<T>): T = Tasks.await(task, 60, TimeUnit.SECONDS)

    private fun assertPermissionDenied(action: () -> Unit) {
        val error = assertThrows(ExecutionException::class.java, action)
        assertEquals(
            FirebaseFirestoreException.Code.PERMISSION_DENIED,
            (error.cause as? FirebaseFirestoreException)?.code,
        )
    }

    companion object {
        private const val RECIPIENT_ID = "uid-text-recipient-fictitious"
    }
}
