package com.proyectonami.app

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Source
import com.google.firebase.storage.StorageException
import com.google.firebase.storage.StorageMetadata
import com.proyectonami.app.core.data.firebase.FirebaseAudioNotesRepository
import com.proyectonami.app.core.data.firebase.FirebaseEnvironment
import com.proyectonami.app.core.data.firebase.FirebasePaths
import com.proyectonami.app.core.data.firebase.FirebaseServicesFactory
import com.proyectonami.app.core.domain.audio.AUDIO_NOTE_CONTENT_TYPE
import com.proyectonami.app.core.domain.audio.AudioNoteMetadata
import com.proyectonami.app.core.domain.audio.AudioUploadFailure
import com.proyectonami.app.core.domain.audio.AudioUploadState
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.last
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.net.InetSocketAddress
import java.net.Socket
import java.util.UUID
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class FirebaseAudioNotesTest {
    private val environment = FirebaseEnvironment.fromBuildConfig()
    private val context = ApplicationProvider.getApplicationContext<NamiApplication>()
    private val services = FirebaseServicesFactory.create(context, environment)
    private val repository = FirebaseAudioNotesRepository(services.storage, services.firestore, services.auth)

    @Before
    fun requireEmulators() {
        assumeTrue(environment.useEmulators)
        for (
        port in
        listOf(
            environment.authEmulatorPort,
            environment.firestoreEmulatorPort,
            environment.functionsEmulatorPort,
            environment.storageEmulatorPort,
        )
        ) {
            assumeTrue(
                "Firebase Emulator Suite no está en ejecución",
                runCatching { Socket().use { it.connect(InetSocketAddress(environment.emulatorHost, port), 1_000) } }.isSuccess,
            )
        }
    }

    @After
    fun signOut() {
        services.auth.signOut()
    }

    @Test
    fun successfulUploadWritesClosedSchemaUpdatesUnreadAndDeletesLocal() =
        runBlocking {
            val senderId = signInAnonymously()
            val conversationId = "audio-success-${UUID.randomUUID()}"
            seedConversation(conversationId)
            val local = audioFile("success", AUDIO_BYTES)
            val metadata = metadata(conversationId, senderId, local)

            val states = repository.upload(metadata, local.absolutePath).toList()

            assertEquals(AudioUploadState.Pending, states.first())
            assertTrue(states.any { it is AudioUploadState.Progress })
            assertEquals(AudioUploadState.Sent(metadata), states.last())
            assertFalse(local.exists())

            val message =
                Tasks.await(
                    services.firestore.document(FirebasePaths.message(conversationId, metadata.clientMessageId)).get(Source.SERVER),
                    TIMEOUT_SECONDS,
                    TimeUnit.SECONDS,
                )
            assertEquals(
                setOf(
                    "clientMessageId",
                    "senderId",
                    "type",
                    "durationMillis",
                    "sizeBytes",
                    "contentType",
                    "storagePath",
                    "createdAt",
                    "status",
                ),
                message.data?.keys,
            )
            assertEquals(metadata.clientMessageId, message.getString("clientMessageId"))
            assertEquals(metadata.storagePath, message.getString("storagePath"))
            assertEquals("audio", message.getString("type"))
            assertEquals("sent", message.getString("status"))
            assertEquals(
                AUDIO_NOTE_CONTENT_TYPE,
                Tasks
                    .await(
                        services.storage.reference
                            .child(metadata.storagePath)
                            .metadata,
                        TIMEOUT_SECONDS,
                        TimeUnit.SECONDS,
                    ).contentType,
            )
            val conversation =
                Tasks.await(
                    services.firestore.document(FirebasePaths.conversation(conversationId)).get(Source.SERVER),
                    TIMEOUT_SECONDS,
                    TimeUnit.SECONDS,
                )
            assertEquals(1L, conversation.getLong("unreadCountByUser.$RECIPIENT_ID"))
        }

    @Test
    fun concurrentRetryUsesOneUuidOneMessageAndOneUnreadIncrement() =
        runBlocking {
            val senderId = signInAnonymously()
            val conversationId = "audio-concurrent-${UUID.randomUUID()}"
            seedConversation(conversationId)
            val firstLocal = audioFile("concurrent-a", AUDIO_BYTES)
            val secondLocal = audioFile("concurrent-b", AUDIO_BYTES)
            val metadata = metadata(conversationId, senderId, firstLocal)

            val results =
                listOf(firstLocal, secondLocal)
                    .map { local ->
                        async { repository.upload(metadata, local.absolutePath).last() }
                    }.awaitAll()

            assertTrue(results.all { it == AudioUploadState.Sent(metadata) })
            val messages =
                Tasks.await(
                    services.firestore.collection("${FirebasePaths.conversation(conversationId)}/messages").get(Source.SERVER),
                    TIMEOUT_SECONDS,
                    TimeUnit.SECONDS,
                )
            assertEquals(1, messages.size())
            val conversation =
                Tasks.await(
                    services.firestore.document(FirebasePaths.conversation(conversationId)).get(Source.SERVER),
                    TIMEOUT_SECONDS,
                    TimeUnit.SECONDS,
                )
            assertEquals(1L, conversation.getLong("unreadCountByUser.$RECIPIENT_ID"))
        }

    @Test
    fun authorizationFailureKeepsTheTemporaryFileUntilExplicitDeletion() =
        runBlocking {
            signInAnonymously()
            val conversationId = "audio-private-${UUID.randomUUID()}"
            seedConversation(conversationId)
            services.auth.signOut()
            val outsiderId = signInAnonymously()
            val local = audioFile("failed", AUDIO_BYTES)
            val metadata = metadata(conversationId, outsiderId, local)

            val state = repository.upload(metadata, local.absolutePath).last()

            assertTrue(state is AudioUploadState.Failed)
            assertEquals(AudioUploadFailure.NOT_AUTHORIZED, (state as AudioUploadState.Failed).reason)
            assertTrue(local.exists())
            assertTrue(repository.deleteLocal(local.absolutePath).isSuccess)
            assertFalse(local.exists())
        }

    @Test
    fun invalidResourcesAndThirdPartyAccessAreDenied() =
        runBlocking {
            val ownerId = signInAnonymously()
            val conversationId = "audio-rules-${UUID.randomUUID()}"
            seedConversation(conversationId)
            val local = audioFile("private", AUDIO_BYTES)
            val metadata = metadata(conversationId, ownerId, local)
            assertEquals(AudioUploadState.Sent(metadata), repository.upload(metadata, local.absolutePath).last())

            val invalidMimeReference =
                services.storage.reference.child(FirebasePaths.audio(conversationId, UUID.randomUUID().toString()))
            assertPermissionDenied {
                Tasks.await(
                    invalidMimeReference.putBytes(
                        AUDIO_BYTES,
                        StorageMetadata.Builder().setContentType("audio/mpeg").build(),
                    ),
                    TIMEOUT_SECONDS,
                    TimeUnit.SECONDS,
                )
            }
            assertPermissionDenied {
                Tasks.await(
                    services.storage.reference
                        .child(FirebasePaths.audio(conversationId, "not-a-uuid"))
                        .putBytes(AUDIO_BYTES, StorageMetadata.Builder().setContentType(AUDIO_NOTE_CONTENT_TYPE).build()),
                    TIMEOUT_SECONDS,
                    TimeUnit.SECONDS,
                )
            }
            assertPermissionDenied {
                Tasks.await(
                    services.firestore.document(FirebasePaths.message(conversationId, UUID.randomUUID().toString())).set(
                        mapOf(
                            "clientMessageId" to UUID.randomUUID().toString(),
                            "senderId" to ownerId,
                            "type" to "audio",
                            "durationMillis" to 1_000L,
                            "sizeBytes" to 5L * 1024L * 1024L + 1L,
                            "contentType" to AUDIO_NOTE_CONTENT_TYPE,
                            "storagePath" to "invalid/path",
                            "createdAt" to FieldValue.serverTimestamp(),
                            "status" to "sent",
                        ),
                    ),
                    TIMEOUT_SECONDS,
                    TimeUnit.SECONDS,
                )
            }

            services.auth.signOut()
            val outsiderId = signInAnonymously()
            assertPermissionDenied {
                Tasks.await(
                    services.firestore.document(FirebasePaths.message(conversationId, metadata.clientMessageId)).get(Source.SERVER),
                    TIMEOUT_SECONDS,
                    TimeUnit.SECONDS,
                )
            }
            assertPermissionDenied {
                Tasks.await(
                    services.storage.reference
                        .child(metadata.storagePath)
                        .getBytes(1_024L),
                    TIMEOUT_SECONDS,
                    TimeUnit.SECONDS,
                )
            }
            assertPermissionDenied {
                val forgedId = UUID.randomUUID().toString()
                Tasks.await(
                    services.firestore.document(FirebasePaths.message(conversationId, forgedId)).set(
                        mapOf(
                            "clientMessageId" to forgedId,
                            "senderId" to outsiderId,
                            "type" to "audio",
                            "durationMillis" to 1_000L,
                            "sizeBytes" to AUDIO_BYTES.size.toLong(),
                            "contentType" to AUDIO_NOTE_CONTENT_TYPE,
                            "storagePath" to FirebasePaths.audio(conversationId, forgedId),
                            "createdAt" to FieldValue.serverTimestamp(),
                            "status" to "sent",
                        ),
                    ),
                    TIMEOUT_SECONDS,
                    TimeUnit.SECONDS,
                )
            }
            assertPermissionDenied {
                Tasks.await(
                    services.storage.reference
                        .child(FirebasePaths.audio(conversationId, UUID.randomUUID().toString()))
                        .putBytes(AUDIO_BYTES, StorageMetadata.Builder().setContentType(AUDIO_NOTE_CONTENT_TYPE).build()),
                    TIMEOUT_SECONDS,
                    TimeUnit.SECONDS,
                )
            }
        }

    private fun signInAnonymously(): String =
        requireNotNull(
            Tasks.await(services.auth.signInAnonymously(), TIMEOUT_SECONDS, TimeUnit.SECONDS).user?.uid,
        )

    private fun seedConversation(conversationId: String) {
        Tasks.await(
            services.functions.getHttpsCallable("seedConversationForTests").call(
                mapOf(
                    "conversationId" to conversationId,
                    "otherUserId" to RECIPIENT_ID,
                    "currentUnreadCount" to 0,
                    "otherUnreadCount" to 0,
                    "activityMillis" to System.currentTimeMillis(),
                ),
            ),
            TIMEOUT_SECONDS,
            TimeUnit.SECONDS,
        )
    }

    private fun audioFile(
        label: String,
        bytes: ByteArray,
    ): File = File(context.cacheDir, "audio-$label-${UUID.randomUUID()}.m4a").apply { writeBytes(bytes) }

    private fun metadata(
        conversationId: String,
        senderId: String,
        local: File,
    ) = AudioNoteMetadata(
        clientMessageId = UUID.randomUUID().toString(),
        conversationId = conversationId,
        senderId = senderId,
        durationMillis = 1_000L,
        sizeBytes = local.length(),
    )

    private fun assertPermissionDenied(action: () -> Unit) {
        val error = assertThrows(Exception::class.java) { action() }
        val cause = if (error is ExecutionException) error.cause ?: error else error
        val denied =
            when (cause) {
                is FirebaseFirestoreException -> cause.code == FirebaseFirestoreException.Code.PERMISSION_DENIED
                is StorageException -> cause.errorCode == StorageException.ERROR_NOT_AUTHORIZED
                else -> false
            }
        assertTrue("Se esperaba PERMISSION_DENIED y se recibió $cause", denied)
    }

    private companion object {
        const val TIMEOUT_SECONDS = 60L
        const val RECIPIENT_ID = "audio-recipient-fictitious"
        val AUDIO_BYTES = "audio-fictitious-content".encodeToByteArray()
    }
}
