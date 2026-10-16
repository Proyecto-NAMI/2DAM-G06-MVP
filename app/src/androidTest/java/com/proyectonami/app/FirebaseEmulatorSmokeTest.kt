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
import com.proyectonami.app.core.domain.auth.AccountProfile
import com.proyectonami.app.core.domain.auth.AccountProfileSaveResult
import com.proyectonami.app.core.domain.auth.AuthSession
import com.proyectonami.app.core.domain.auth.PhoneAuthFailure
import com.proyectonami.app.core.domain.contacts.ContactDraft
import com.proyectonami.app.core.domain.contacts.ContactSaveResult
import com.proyectonami.app.core.domain.conversations.ConversationsRemoteState
import com.proyectonami.app.core.media.MediaSessionCoordinator
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
import java.util.Date
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import com.proyectonami.app.core.domain.contacts.ContactStatus as CoreContactStatus

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
            val phoneNumber = fictitiousEmulatorPhoneNumber()
            val activityReference = AtomicReference<Activity>()
            val viewModel =
                PhoneAuthViewModel(
                    authRepository = repositories.authRepository,
                    accountProfileRepository = repositories.accountProfileRepository,
                    mediaSessionController = MediaSessionCoordinator(),
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

                assertEquals(
                    AccountProfileSaveResult.Saved,
                    repositories.accountProfileRepository.saveProfile(
                        AccountProfile(
                            userId = userId,
                            displayName = "Persona",
                            surname = "Ficticia",
                            phoneNumber = phoneNumber,
                        ),
                    ),
                )
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
                assertEquals(phoneNumber, profile.getString("phoneE164"))
                assertEquals(null, profile.getString("phoneNumber"))

                val contactPhoneNumber = nextFictitiousPhoneNumber(phoneNumber)
                val firstContact =
                    repositories.contactsRepository.addContact(
                        ContactDraft("Contacto", "Ficticio", contactPhoneNumber),
                    )
                val duplicateContact =
                    repositories.contactsRepository.addContact(
                        ContactDraft("Otro", "Nombre", contactPhoneNumber),
                    )
                assertTrue(firstContact is ContactSaveResult.Added)
                assertEquals(
                    CoreContactStatus.PENDING,
                    (firstContact as ContactSaveResult.Added).contact.status,
                )
                assertTrue(duplicateContact is ContactSaveResult.Duplicate)
            }
        }

    @Test
    fun profileReadsAreOwnerOnlyAndPhoneDirectoryQueriesAreDenied() {
        assumeEmulatorsAreRunning()

        val ownerId = signInAnonymously()
        val ownerProfile = repositories.firestoreRepository.userDocument(ownerId)
        Tasks.await(
            ownerProfile.set(mapOf("displayName" to "Persona Ficticia")),
            TIMEOUT_SECONDS,
            TimeUnit.SECONDS,
        )
        assertEquals(
            "Persona Ficticia",
            Tasks
                .await(ownerProfile.get(Source.SERVER), TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .getString("displayName"),
        )

        repositories.authRepository.signOut()
        signInAnonymously()

        assertPermissionDenied {
            Tasks.await(ownerProfile.get(Source.SERVER), TIMEOUT_SECONDS, TimeUnit.SECONDS)
        }
        assertPermissionDenied {
            Tasks.await(
                services.firestore
                    .collection("users")
                    .whereEqualTo("phoneE164", "+15555550100")
                    .limit(1)
                    .get(Source.SERVER),
                TIMEOUT_SECONDS,
                TimeUnit.SECONDS,
            )
        }
    }

    @Test
    fun conversationsAreScopedToCurrentUserAndOrderedWithUnreadCounts() =
        runBlocking {
            assumeEmulatorsAreRunning()

            val unrelatedUserId = signInAnonymously()
            val unrelatedConversationId = "conversation-unrelated-$unrelatedUserId"
            Tasks.await(
                repositories.firestoreRepository.conversationDocument(unrelatedConversationId).set(
                    conversationData(unrelatedUserId, "uid-ficticio-2", unreadCount = 7, activityMillis = 300L),
                ),
                TIMEOUT_SECONDS,
                TimeUnit.SECONDS,
            )

            repositories.authRepository.signOut()
            val currentUserId = signInAnonymously()
            val olderConversationId = "conversation-older-$currentUserId"
            val newerConversationId = "conversation-newer-$currentUserId"
            Tasks.await(
                repositories.firestoreRepository.conversationDocument(olderConversationId).set(
                    conversationData(currentUserId, "uid-contact-old", unreadCount = 2, activityMillis = 100L),
                ),
                TIMEOUT_SECONDS,
                TimeUnit.SECONDS,
            )
            Tasks.await(
                repositories.firestoreRepository.conversationDocument(newerConversationId).set(
                    conversationData(currentUserId, "uid-contact-new", unreadCount = 4, activityMillis = 300L),
                ),
                TIMEOUT_SECONDS,
                TimeUnit.SECONDS,
            )

            val state =
                withTimeout(TIMEOUT_SECONDS * 1_000) {
                    repositories.conversationsRepository
                        .observeConversations()
                        .first { it is ConversationsRemoteState.Loaded }
                } as ConversationsRemoteState.Loaded

            assertEquals(listOf(newerConversationId, olderConversationId), state.conversations.map { it.id })
            assertEquals(listOf(4, 2), state.conversations.map { it.unreadCount })
            assertEquals(
                listOf("Persona uid-contact-new", "Persona uid-contact-old"),
                state.conversations.map { it.displayName },
            )
        }

    @Test
    fun clientsCannotWriteContactsDirectly() {
        assumeEmulatorsAreRunning()

        val userId = signInAnonymously()
        assertPermissionDenied {
            Tasks.await(
                repositories.firestoreRepository.contactDocument(userId, "contact-forged").set(
                    mapOf(
                        "displayName" to "Persona",
                        "surname" to "Ficticia",
                        "phoneE164" to "+15555550101",
                        "linkedUserId" to "uid-ajeno-ficticio",
                        "status" to "linked",
                    ),
                ),
                TIMEOUT_SECONDS,
                TimeUnit.SECONDS,
            )
        }
    }

    @Test
    fun unverifiedUserCannotRegisterPhoneForDirectoryLookup() {
        assumeEmulatorsAreRunning()

        val userId = signInAnonymously()
        assertPermissionDenied {
            Tasks.await(
                repositories.firestoreRepository.userDocument(userId).set(
                    mapOf(
                        "displayName" to "Persona Ficticia",
                        "phoneE164" to "+15555550100",
                    ),
                ),
                TIMEOUT_SECONDS,
                TimeUnit.SECONDS,
            )
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
                environment.functionsEmulatorPort,
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

    private fun fictitiousEmulatorPhoneNumber(): String {
        val suffix = Math.floorMod(System.nanoTime(), 10_000_000).toString().padStart(7, '0')
        return "+1555$suffix"
    }

    private fun nextFictitiousPhoneNumber(phoneNumber: String): String {
        val nextDigit = ((phoneNumber.last().digitToInt() + 1) % 10).toString()
        return phoneNumber.dropLast(1) + nextDigit
    }

    private fun conversationData(
        currentUserId: String,
        otherUserId: String,
        unreadCount: Long,
        activityMillis: Long,
    ): Map<String, Any> =
        mapOf(
            "participantIds" to listOf(currentUserId, otherUserId),
            "participantDetails" to
                mapOf(
                    currentUserId to mapOf("displayName" to "Usuario", "surname" to "Ficticio"),
                    otherUserId to
                        mapOf(
                            "displayName" to "Persona $otherUserId",
                            "surname" to otherUserId,
                            "avatarPath" to "avatars/ficticio.png",
                        ),
                ),
            "unreadCountByUser" to mapOf(currentUserId to unreadCount),
            "lastActivityAt" to com.google.firebase.Timestamp(Date(activityMillis)),
        )

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
