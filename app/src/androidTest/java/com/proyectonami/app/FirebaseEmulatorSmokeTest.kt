package com.proyectonami.app

import android.app.Activity
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Source
import com.google.firebase.storage.StorageException
import com.google.firebase.storage.StorageMetadata
import com.proyectonami.app.core.data.firebase.FirebaseEnvironment
import com.proyectonami.app.core.data.firebase.FirebasePhoneAuthRepository
import com.proyectonami.app.core.data.firebase.FirebaseRepositoryContainer
import com.proyectonami.app.core.data.firebase.FirebaseServicesFactory
import com.proyectonami.app.core.domain.auth.AuthSession
import com.proyectonami.app.core.domain.auth.PhoneAuthFailure
import com.proyectonami.app.feature.auth.presentation.PhoneAuthViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.net.HttpURLConnection
import java.net.InetSocketAddress
import java.net.Socket
import java.net.URL
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

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
    fun phoneVerificationCanRetryAndSessionIsRestoredByANewRepository() =
        runBlocking {
            assumeEmulatorsAreRunning()
            repositories.authRepository.signOut()
            val phoneNumber = "+15555550100"
            val activityReference = AtomicReference<Activity>()
            val viewModel =
                PhoneAuthViewModel(
                    authRepository = repositories.authRepository,
                    accountProfileRepository = repositories.accountProfileRepository,
                )

            ActivityScenario.launch(MainActivity::class.java).use { scenario ->
                scenario.onActivity(activityReference::set)
                viewModel.updateName("Persona")
                viewModel.updateSurname("Ficticia")
                viewModel.updatePhoneNumber(phoneNumber)
                viewModel.requestCode(activityReference.get())
                val requestedState =
                    withTimeout(TIMEOUT_SECONDS * 1_000) {
                        viewModel.uiState.first {
                            !it.isRequestingCode && (it.challenge != null || it.failure != null)
                        }
                    }
                assertEquals(null, requestedState.failure)
                val challenge = requireNotNull(requestedState.challenge)

                val initialCode = getVerificationCode(phoneNumber)
                val invalidCode = initialCode.dropLast(1) + if (initialCode.last() == '9') "8" else "9"
                viewModel.updateVerificationCode(invalidCode)
                viewModel.verifyCode()
                val failedVerificationState =
                    withTimeout(TIMEOUT_SECONDS * 1_000) {
                        viewModel.uiState.first { !it.isVerifyingCode && it.failure != null }
                    }
                assertEquals(PhoneAuthFailure.INVALID_CODE, failedVerificationState.failure)
                assertEquals("Persona", failedVerificationState.name)
                assertEquals("Ficticia", failedVerificationState.surname)
                assertEquals(phoneNumber, failedVerificationState.phoneNumber)
                assertEquals(challenge, failedVerificationState.challenge)

                viewModel.requestCode(activityReference.get())
                val resentState =
                    withTimeout(TIMEOUT_SECONDS * 1_000) {
                        viewModel.uiState.first { !it.isRequestingCode && it.failure == null }
                    }
                assertEquals("Persona", resentState.name)
                assertEquals("Ficticia", resentState.surname)
                assertEquals(phoneNumber, resentState.phoneNumber)
                val resentChallenge = requireNotNull(resentState.challenge)
                val resentCode = getVerificationCode(phoneNumber)
                viewModel.updateVerificationCode(resentCode)
                viewModel.verifyCode()
                val verifiedState =
                    withTimeout(TIMEOUT_SECONDS * 1_000) {
                        viewModel.uiState.first { it.isAccountReady }
                    }
                val userId = (verifiedState.session as AuthSession.SignedIn).userId

                val recreatedRepository = FirebasePhoneAuthRepository(services.auth)
                assertEquals(userId, recreatedRepository.currentUserId)
                assertEquals(
                    AuthSession.SignedIn(userId),
                    withTimeout(5_000) { recreatedRepository.observeSession().first() },
                )
                assertEquals(phoneNumber, services.auth.currentUser?.phoneNumber)
                val profile =
                    Tasks.await(
                        repositories.firestoreRepository.userDocument(userId).get(),
                        TIMEOUT_SECONDS,
                        TimeUnit.SECONDS,
                    )
                assertEquals("Persona", profile.getString("displayName"))
                assertEquals("Ficticia", profile.getString("surname"))
                assertEquals(phoneNumber, profile.getString("phoneNumber"))
            }
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

    private fun getVerificationCode(phoneNumber: String): String {
        val url =
            URL(
                "http://${environment.emulatorHost}:${environment.authEmulatorPort}" +
                    "/emulator/v1/projects/${environment.projectId}/verificationCodes",
            )
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.connectTimeout = CONNECT_TIMEOUT_MILLIS
        connection.readTimeout = TIMEOUT_SECONDS.toInt() * 1_000
        return try {
            val response = JSONObject(connection.inputStream.bufferedReader().use { reader -> reader.readText() })
            val codes = response.getJSONArray("verificationCodes")
            (codes.length() - 1 downTo 0)
                .map { index -> codes.getJSONObject(index) }
                .firstOrNull { entry -> entry.getString("phoneNumber") == phoneNumber }
                ?.getString("code")
                ?: throw AssertionError("El emulador no devolvió un código para el teléfono ficticio")
        } finally {
            connection.disconnect()
        }
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
