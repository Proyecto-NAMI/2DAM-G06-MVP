package com.proyectonami.app

import android.app.Activity
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Source
import com.google.firebase.storage.StorageException
import com.google.firebase.storage.StorageMetadata
import com.proyectonami.app.core.data.firebase.FirebaseEnvironment
import com.proyectonami.app.core.data.firebase.FirebasePaths
import com.proyectonami.app.core.data.firebase.FirebasePhoneAuthRepository
import com.proyectonami.app.core.data.firebase.FirebaseRepositoryContainer
import com.proyectonami.app.core.data.firebase.FirebaseServicesFactory
import com.proyectonami.app.core.domain.auth.AccountProfile
import com.proyectonami.app.core.domain.auth.AccountProfileSaveResult
import com.proyectonami.app.core.domain.auth.AuthSession
import com.proyectonami.app.core.domain.auth.PhoneAuthFailure
import com.proyectonami.app.core.domain.contacts.ContactDraft
import com.proyectonami.app.core.domain.contacts.ContactSaveResult
import com.proyectonami.app.core.domain.contacts.ContactStatus
import com.proyectonami.app.core.domain.contacts.ConversationOpenResult
import com.proyectonami.app.core.domain.conversations.ConversationsRemoteState
import com.proyectonami.app.core.domain.messages.OutgoingTextMessage
import com.proyectonami.app.core.domain.messages.TextMessageSendResult
import com.proyectonami.app.core.media.MediaSessionCoordinator
import com.proyectonami.app.feature.auth.presentation.PhoneAuthViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.net.HttpURLConnection
import java.net.InetSocketAddress
import java.net.Socket
import java.net.URL
import java.util.UUID
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
        val messageId = UUID.randomUUID().toString()

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
        seedConversation(conversationId, "usuario-ficticio-2")

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
                    ContactStatus.PENDING,
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
            seedConversation(unrelatedConversationId, "uid-ficticio-2", currentUnreadCount = 7, activityMillis = 300L)

            repositories.authRepository.signOut()
            val currentUserId = signInAnonymously()
            val olderConversationId = "conversation-older-$currentUserId"
            val newerConversationId = "conversation-newer-$currentUserId"
            seedConversation(olderConversationId, "uid-contact-old", currentUnreadCount = 2, activityMillis = 100L)
            seedConversation(newerConversationId, "uid-contact-new", currentUnreadCount = 4, activityMillis = 300L)

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
    fun addContactLinksKnownPhonesAndStoresUnknownOnesAsPending() =
        runBlocking {
            assumeEmulatorsAreRunning()

            // Primero, la cuenta que ya usa NAMI: otro teléfono ficticio verificado.
            val knownPhoneNumber = fictitiousEmulatorPhoneNumber()
            signInWithFictitiousPhone(knownPhoneNumber)
            val knownUserId = requireNotNull(repositories.authRepository.currentUserId)

            // Después, quien añade el contacto.
            val ownerPhoneNumber = nextFictitiousPhoneNumber(knownPhoneNumber)
            signInWithFictitiousPhone(ownerPhoneNumber)
            val ownerUserId = requireNotNull(repositories.authRepository.currentUserId)
            assertTrue("Cada alta debe partir de una cuenta distinta", ownerUserId != knownUserId)

            val linked =
                repositories.contactsRepository.addContact(
                    ContactDraft("Carlos", "Ficticio", knownPhoneNumber),
                )
            assertTrue("Se esperaba un contacto vinculado: $linked", linked is ContactSaveResult.Added)
            val linkedContact = (linked as ContactSaveResult.Added).contact
            assertEquals(ContactStatus.LINKED, linkedContact.status)
            assertEquals(knownUserId, linkedContact.linkedUserId)

            val opened = repositories.contactsRepository.openConversation(linkedContact.id)
            assertTrue("Se esperaba abrir la conversación: $opened", opened is ConversationOpenResult.Opened)
            val conversationId = (opened as ConversationOpenResult.Opened).conversationId
            assertEquals(opened, repositories.contactsRepository.openConversation(linkedContact.id))

            val conversation =
                Tasks.await(
                    repositories.firestoreRepository.conversationDocument(conversationId).get(Source.SERVER),
                    TIMEOUT_SECONDS,
                    TimeUnit.SECONDS,
                )
            assertEquals(setOf(ownerUserId, knownUserId), (conversation.get("participantIds") as List<*>).toSet())
            assertEquals(0L, conversation.getLong("unreadCountByUser.$ownerUserId"))
            assertEquals(0L, conversation.getLong("unreadCountByUser.$knownUserId"))

            val outgoing = OutgoingTextMessage.create(conversationId, ownerUserId, "Hola, persona ficticia")
            assertEquals(TextMessageSendResult.Sent, repositories.textMessagesRepository.send(outgoing))
            assertTrue(
                Tasks
                    .await(
                        repositories.firestoreRepository.messageDocument(conversationId, outgoing.clientMessageId).get(Source.SERVER),
                        TIMEOUT_SECONDS,
                        TimeUnit.SECONDS,
                    ).exists(),
            )

            val duplicate =
                repositories.contactsRepository.addContact(
                    ContactDraft("Otro", "Nombre", knownPhoneNumber),
                )
            assertTrue("El segundo alta debe informar del duplicado: $duplicate", duplicate is ContactSaveResult.Duplicate)
            assertEquals("Carlos", (duplicate as ContactSaveResult.Duplicate).existing.displayName)

            val pending =
                repositories.contactsRepository.addContact(
                    ContactDraft("Sin", "Cuenta", nextFictitiousPhoneNumber(knownPhoneNumber)),
                )
            assertTrue("Se esperaba un contacto pendiente: $pending", pending is ContactSaveResult.Added)
            assertEquals(ContactStatus.PENDING, (pending as ContactSaveResult.Added).contact.status)
            assertNull(pending.contact.linkedUserId)
            assertEquals(
                ConversationOpenResult.ContactPending,
                repositories.contactsRepository.openConversation(pending.contact.id),
            )

            val storedContacts =
                Tasks.await(
                    services.firestore
                        .collection("${FirebasePaths.user(ownerUserId)}/contacts")
                        .get(Source.SERVER),
                    TIMEOUT_SECONDS,
                    TimeUnit.SECONDS,
                )
            assertEquals("Cada teléfono debe guardarse una sola vez", 2, storedContacts.size())

            repositories.authRepository.signOut()
            signInAnonymously()

            assertEquals(
                ConversationOpenResult.ContactPending,
                repositories.contactsRepository.openConversation(linkedContact.id),
            )
            assertPermissionDenied {
                Tasks.await(
                    repositories.firestoreRepository.conversationDocument("forged-${UUID.randomUUID()}").set(
                        mapOf("participantIds" to listOf(repositories.authRepository.currentUserId, knownUserId)),
                    ),
                    TIMEOUT_SECONDS,
                    TimeUnit.SECONDS,
                )
            }

            assertPermissionDenied {
                Tasks.await(
                    repositories.firestoreRepository
                        .contactDocument(ownerUserId, linkedContact.id)
                        .get(Source.SERVER),
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
        val audioMessageId = UUID.randomUUID().toString()
        val audioReference =
            repositories.audioStorageRepository.audioReference(
                conversationId = conversationId,
                messageId = audioMessageId,
            )

        seedConversation(conversationId, "usuario-ficticio-2")
        val messageId = UUID.randomUUID().toString()
        Tasks.await(
            repositories.firestoreRepository.messageDocument(conversationId, messageId).set(
                mapOf(
                    "clientMessageId" to messageId,
                    "senderId" to ownerId,
                    "type" to "text",
                    "text" to "Mensaje ficticio privado",
                    "createdAt" to FieldValue.serverTimestamp(),
                    "status" to "sent",
                ),
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
                repositories.firestoreRepository
                    .conversationDocument(conversationId)
                    .update("lastActivityAt", FieldValue.serverTimestamp()),
                TIMEOUT_SECONDS,
                TimeUnit.SECONDS,
            )
        }
        assertPermissionDenied {
            Tasks.await(
                repositories.firestoreRepository
                    .messageDocument(conversationId, messageId)
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

    /**
     * Crea o recupera la sesión de un teléfono ficticio verificado, que es lo que
     * exige la función `addContact` (T-11) para poder consultar un número.
     */
    private suspend fun signInWithFictitiousPhone(phoneNumber: String) {
        repositories.authRepository.signOut()
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
                    viewModel.uiState.first { !it.isRequestingCode && (it.challenge != null || it.failure != null) }
                }
            assertNull("No se pudo solicitar el código: ${requestedState.failure}", requestedState.failure)
            viewModel.updateVerificationCode(getVerificationCode(phoneNumber))
            viewModel.verifyCode()
            val verifiedState =
                withTimeout(TIMEOUT_SECONDS * 1_000) {
                    viewModel.uiState.first { it.isAccountReady || it.profileSaveFailed }
                }
            assertTrue("No se pudo verificar el teléfono ficticio: ${verifiedState.failure}", verifiedState.isAccountReady)
        }
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

    private fun seedConversation(
        conversationId: String,
        otherUserId: String,
        currentUnreadCount: Long = 0,
        otherUnreadCount: Long = 0,
        activityMillis: Long = System.currentTimeMillis(),
    ) {
        Tasks.await(
            services.functions.getHttpsCallable("seedConversationForTests").call(
                mapOf(
                    "conversationId" to conversationId,
                    "otherUserId" to otherUserId,
                    "currentUnreadCount" to currentUnreadCount,
                    "otherUnreadCount" to otherUnreadCount,
                    "activityMillis" to activityMillis,
                ),
            ),
            TIMEOUT_SECONDS,
            TimeUnit.SECONDS,
        )
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
        // Holgado a propósito: en CI comparten máquina el emulador de Android y
        // cuatro emuladores de Firebase, y la primera conexión puede tardar.
        private const val TIMEOUT_SECONDS = 60L
        private const val CONNECT_TIMEOUT_MILLIS = 1_000
        private const val MAX_AUDIO_TEST_BYTES = 1_024L
    }
}
