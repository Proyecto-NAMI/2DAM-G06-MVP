package com.proyectonami.app

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Source
import com.google.firebase.storage.StorageException
import com.google.firebase.storage.StorageMetadata
import com.proyectonami.app.core.data.firebase.FirebaseEnvironment
import com.proyectonami.app.core.data.firebase.FirebaseRepositoryContainer
import com.proyectonami.app.core.data.firebase.FirebaseServicesFactory
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.net.InetSocketAddress
import java.net.Socket
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class FirebaseEmulatorSmokeTest {
    private val environment = FirebaseEnvironment.fromBuildConfig()
    private val context = ApplicationProvider.getApplicationContext<NamiApplication>()
    private val services = FirebaseServicesFactory.create(context, environment)
    private val repositories = FirebaseRepositoryContainer.create(context, environment)

    @After
    fun signOut() {
        repositories.authRepository.signOut()
    }

    @Test
    fun authFirestoreAndStorageWorkWithFictitiousData() {
        assumeEmulatorsAreRunning()

        val authResult =
            Tasks.await(
                services.auth.signInAnonymously(),
                TIMEOUT_SECONDS,
                TimeUnit.SECONDS,
            )
        val userId = requireNotNull(authResult.user?.uid)
        val conversationId = "conversation-$userId"
        val messageId = "message-1"

        Tasks.await(
            repositories.firestoreRepository.userDocument(userId).set(
                mapOf(
                    "displayName" to "Persona Ficticia",
                    "surname" to "Prueba",
                ),
            ),
            TIMEOUT_SECONDS,
            TimeUnit.SECONDS,
        )
        Tasks.await(
            repositories.firestoreRepository.conversationDocument(conversationId).set(
                mapOf("participantIds" to listOf(userId, "usuario-ficticio-2")),
            ),
            TIMEOUT_SECONDS,
            TimeUnit.SECONDS,
        )

        val metadata =
            StorageMetadata
                .Builder()
                .setContentType("audio/mp4")
                .build()
        val audioReference =
            repositories.audioStorageRepository.audioReference(
                conversationId = conversationId,
                messageId = messageId,
            )
        Tasks.await(
            audioReference.putBytes("audio-ficticio".encodeToByteArray(), metadata),
            TIMEOUT_SECONDS,
            TimeUnit.SECONDS,
        )

        assertEquals(
            "audio/mp4",
            Tasks.await(audioReference.metadata, TIMEOUT_SECONDS, TimeUnit.SECONDS).contentType,
        )
        assertEquals(userId, repositories.authRepository.currentUserId)
    }

    @Test
    fun unauthenticatedUserCannotWriteAProfile() {
        assumeEmulatorsAreRunning()
        repositories.authRepository.signOut()

        assertPermissionDenied {
            Tasks.await(
                repositories.firestoreRepository.userDocument("usuario-no-autenticado").set(
                    mapOf("displayName" to "Persona Ficticia"),
                ),
                TIMEOUT_SECONDS,
                TimeUnit.SECONDS,
            )
        }
    }

    @Test
    fun thirdPartyCannotReadConversationOrAudio() {
        assumeEmulatorsAreRunning()

        val ownerId = signInAnonymously()
        val conversationId = "conversation-private-$ownerId"
        val audioReference =
            repositories.audioStorageRepository.audioReference(
                conversationId = conversationId,
                messageId = "message-private",
            )

        Tasks.await(
            repositories.firestoreRepository.conversationDocument(conversationId).set(
                mapOf("participantIds" to listOf(ownerId, "usuario-ficticio-2")),
            ),
            TIMEOUT_SECONDS,
            TimeUnit.SECONDS,
        )
        Tasks.await(
            audioReference.putBytes(
                "audio-ficticio-privado".encodeToByteArray(),
                StorageMetadata.Builder().setContentType("audio/mp4").build(),
            ),
            TIMEOUT_SECONDS,
            TimeUnit.SECONDS,
        )

        repositories.authRepository.signOut()
        signInAnonymously()

        assertPermissionDenied {
            Tasks.await(
                repositories.firestoreRepository
                    .conversationDocument(conversationId)
                    .get(Source.SERVER),
                TIMEOUT_SECONDS,
                TimeUnit.SECONDS,
            )
        }
        assertPermissionDenied {
            Tasks.await(
                audioReference.getBytes(MAX_AUDIO_TEST_BYTES),
                TIMEOUT_SECONDS,
                TimeUnit.SECONDS,
            )
        }
    }

    private fun assumeEmulatorsAreRunning() {
        assumeTrue(environment.useEmulators)
        // Sin Emulator Suite activo se omite en vez de fallar, para no bloquear otras pruebas UI.
        assumeTrue(
            "Firebase Emulator Suite no está en ejecución",
            listOf(
                environment.authEmulatorPort,
                environment.firestoreEmulatorPort,
                environment.storageEmulatorPort,
            ).all { port -> isReachable(environment.emulatorHost, port) },
        )
    }

    private fun signInAnonymously(): String {
        val authResult =
            Tasks.await(
                services.auth.signInAnonymously(),
                TIMEOUT_SECONDS,
                TimeUnit.SECONDS,
            )
        return requireNotNull(authResult.user?.uid)
    }

    private fun assertPermissionDenied(action: () -> Unit) {
        val error = assertThrows(Exception::class.java) { action() }
        val cause = if (error is ExecutionException) error.cause ?: error else error
        val isPermissionDenied =
            when (cause) {
                is FirebaseFirestoreException ->
                    cause.code == FirebaseFirestoreException.Code.PERMISSION_DENIED
                is StorageException ->
                    cause.errorCode == StorageException.ERROR_NOT_AUTHORIZED
                else -> false
            }
        assertTrue("Se esperaba PERMISSION_DENIED y se recibió $cause", isPermissionDenied)
    }

    private fun isReachable(
        host: String,
        port: Int,
    ): Boolean =
        runCatching {
            Socket().use { it.connect(InetSocketAddress(host, port), CONNECT_TIMEOUT_MILLIS) }
        }.isSuccess

    companion object {
        private const val TIMEOUT_SECONDS = 20L
        private const val CONNECT_TIMEOUT_MILLIS = 1_000
        private const val MAX_AUDIO_TEST_BYTES = 1_024L
    }
}
