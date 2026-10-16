package com.proyectonami.app.core.data.firebase

import android.app.Activity
import android.content.Context
import android.net.Uri
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.proyectonami.app.core.domain.audio.AudioNoteMetadata
import com.proyectonami.app.core.domain.audio.AudioNotesRepository
import com.proyectonami.app.core.domain.audio.AudioUploadFailure
import com.proyectonami.app.core.domain.audio.AudioUploadState
import com.proyectonami.app.core.domain.auth.AccountProfile
import com.proyectonami.app.core.domain.auth.AccountProfileLoadResult
import com.proyectonami.app.core.domain.auth.AccountProfileRepository
import com.proyectonami.app.core.domain.auth.AccountProfileSaveResult
import com.proyectonami.app.core.domain.auth.AuthSession
import com.proyectonami.app.core.domain.auth.PhoneCodeVerificationResult
import com.proyectonami.app.core.domain.auth.PhoneVerificationChallenge
import com.proyectonami.app.core.domain.auth.PhoneVerificationRequestResult
import com.proyectonami.app.core.domain.contacts.ContactsRepository
import com.proyectonami.app.core.domain.conversations.ConversationsRepository
import com.proyectonami.app.core.domain.messages.TextMessagesRepository
import com.proyectonami.app.core.domain.validation.PhoneNumberValidation
import com.proyectonami.app.core.domain.validation.PhoneNumberValidator
import com.proyectonami.app.feature.contacts.domain.ContactRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

interface AuthRepository {
    val currentUserId: String?

    fun observeSession(): Flow<AuthSession>

    suspend fun requestPhoneCode(
        activity: Activity,
        phoneNumber: String,
        previousChallenge: PhoneVerificationChallenge? = null,
    ): PhoneVerificationRequestResult

    suspend fun verifyPhoneCode(
        challenge: PhoneVerificationChallenge,
        code: String,
    ): PhoneCodeVerificationResult

    suspend fun verifyPhoneNumberChange(
        challenge: PhoneVerificationChallenge,
        code: String,
    ): PhoneCodeVerificationResult = verifyPhoneCode(challenge, code)

    fun signOut()
}

interface FirestoreRepository {
    fun userDocument(userId: String): DocumentReference

    fun contactDocument(
        userId: String,
        contactId: String,
    ): DocumentReference

    fun conversationDocument(conversationId: String): DocumentReference

    fun messageDocument(
        conversationId: String,
        messageId: String,
    ): DocumentReference
}

interface AudioStorageRepository {
    fun audioReference(
        conversationId: String,
        messageId: String,
    ): StorageReference
}

class FirebaseFirestoreRepository(
    private val firestore: FirebaseFirestore,
) : FirestoreRepository {
    override fun userDocument(userId: String): DocumentReference = firestore.document(FirebasePaths.user(userId))

    override fun contactDocument(
        userId: String,
        contactId: String,
    ): DocumentReference = firestore.document(FirebasePaths.contact(userId, contactId))

    override fun conversationDocument(conversationId: String): DocumentReference =
        firestore.document(FirebasePaths.conversation(conversationId))

    override fun messageDocument(
        conversationId: String,
        messageId: String,
    ): DocumentReference = firestore.document(FirebasePaths.message(conversationId, messageId))
}

class FirebaseAccountProfileRepository(
    private val firestore: FirebaseFirestore,
) : AccountProfileRepository {
    override suspend fun saveProfile(profile: AccountProfile): AccountProfileSaveResult {
        val phoneE164 =
            when (val validation = PhoneNumberValidator.validate(profile.phoneNumber)) {
                is PhoneNumberValidation.Valid -> validation.e164
                is PhoneNumberValidation.Invalid -> return AccountProfileSaveResult.Failed
            }

        return suspendCancellableCoroutine { continuation ->
            firestore
                .document(FirebasePaths.user(profile.userId))
                .set(
                    mapOf(
                        "displayName" to profile.displayName,
                        "surname" to profile.surname,
                        "phoneE164" to phoneE164,
                        "avatarPath" to profile.avatarPath,
                    ),
                ).addOnCompleteListener { task ->
                    if (continuation.isActive) {
                        continuation.resume(
                            if (task.isSuccessful) {
                                AccountProfileSaveResult.Saved
                            } else {
                                AccountProfileSaveResult.Failed
                            },
                        )
                    }
                }
        }
    }

    override suspend fun loadProfile(userId: String): AccountProfileLoadResult =
        suspendCancellableCoroutine { continuation ->
            firestore.document(FirebasePaths.user(userId)).get().addOnCompleteListener { task ->
                if (!continuation.isActive) return@addOnCompleteListener
                val snapshot = task.result
                val profile =
                    if (task.isSuccessful && snapshot != null && snapshot.exists()) {
                        val displayName = snapshot.getString("displayName")
                        val surname = snapshot.getString("surname")
                        val phoneE164 = snapshot.getString("phoneE164")
                        if (displayName != null && surname != null && phoneE164 != null) {
                            AccountProfile(
                                userId = userId,
                                displayName = displayName,
                                surname = surname,
                                phoneNumber = phoneE164,
                                avatarPath = snapshot.getString("avatarPath"),
                            )
                        } else {
                            null
                        }
                    } else {
                        null
                    }
                continuation.resume(
                    when {
                        profile != null -> AccountProfileLoadResult.Loaded(profile)
                        task.isSuccessful -> AccountProfileLoadResult.Missing
                        else -> AccountProfileLoadResult.Failed
                    },
                )
            }
        }
}

class FirebaseAudioStorageRepository(
    private val storage: FirebaseStorage,
) : AudioStorageRepository {
    override fun audioReference(
        conversationId: String,
        messageId: String,
    ): StorageReference = storage.reference.child(FirebasePaths.audio(conversationId, messageId))
}

class FirebaseAudioNotesRepository(
    private val storage: FirebaseStorage,
    private val firestore: FirebaseFirestore,
) : AudioNotesRepository {
    override fun upload(
        metadata: AudioNoteMetadata,
        localPath: String,
    ): Flow<AudioUploadState> =
        flow {
            emit(AudioUploadState.Pending)
            if (metadata.durationMillis <= 0L || metadata.sizeBytes <= 0L) {
                emit(AudioUploadState.Failed(metadata, AudioUploadFailure.INVALID_NOTE))
                return@flow
            }
            try {
                val reference = storage.reference.child(FirebasePaths.audio(metadata.conversationId, metadata.clientMessageId))
                val uri = Uri.fromFile(java.io.File(localPath))
                emit(AudioUploadState.Progress(0f))
                awaitUpload(reference, uri)
                emit(AudioUploadState.Progress(1f))
                val values =
                    mapOf(
                        "type" to "audio",
                        "senderId" to metadata.senderId,
                        "createdAt" to
                            com.google.firebase.firestore.FieldValue
                                .serverTimestamp(),
                        "durationMillis" to metadata.durationMillis,
                        "sizeBytes" to metadata.sizeBytes,
                        "contentType" to metadata.contentType,
                    )
                awaitSet(firestore.document(FirebasePaths.message(metadata.conversationId, metadata.clientMessageId)), values)
                emit(AudioUploadState.Sent(metadata))
            } catch (_: Exception) {
                emit(AudioUploadState.Failed(metadata, AudioUploadFailure.NETWORK))
            }
        }

    override suspend fun deleteLocal(localPath: String): Result<Unit> =
        runCatching {
            val file = java.io.File(localPath)
            if (file.exists() && !file.delete()) error("Unable to delete temporary audio")
        }

    private suspend fun awaitUpload(
        reference: StorageReference,
        uri: Uri,
    ) = suspendCancellableCoroutine<Unit> { continuation ->
        val task = reference.putFile(uri)
        task.addOnSuccessListener { continuation.resume(Unit) }
        task.addOnFailureListener { continuation.resumeWith(Result.failure(it)) }
    }

    private suspend fun awaitSet(
        reference: com.google.firebase.firestore.DocumentReference,
        values: Map<String, Any>,
    ) = suspendCancellableCoroutine<Unit> { continuation ->
        reference
            .set(values)
            .addOnSuccessListener {
                continuation.resume(Unit)
            }.addOnFailureListener { continuation.resumeWith(Result.failure(it)) }
    }
}

class FirebaseRepositoryContainer(
    val authRepository: AuthRepository,
    val accountProfileRepository: AccountProfileRepository,
    val contactRepository: ContactRepository,
    val firestoreRepository: FirestoreRepository,
    val contactsRepository: ContactsRepository,
    val conversationsRepository: ConversationsRepository,
    val textMessagesRepository: TextMessagesRepository,
    val audioStorageRepository: AudioStorageRepository,
    val audioNotesRepository: AudioNotesRepository,
) {
    companion object {
        fun create(
            context: Context,
            environment: FirebaseEnvironment,
        ): FirebaseRepositoryContainer {
            val services = FirebaseServicesFactory.create(context, environment)
            return FirebaseRepositoryContainer(
                authRepository = FirebasePhoneAuthRepository(services.auth),
                accountProfileRepository = FirebaseAccountProfileRepository(services.firestore),
                contactRepository = FirebaseContactRepository(services.firestore),
                firestoreRepository = FirebaseFirestoreRepository(services.firestore),
                contactsRepository =
                    FirebaseContactsRepository(
                        firestore = services.firestore,
                        auth = services.auth,
                        functions = services.functions,
                    ),
                conversationsRepository = FirebaseConversationsRepository(services.firestore, services.auth),
                textMessagesRepository = FirebaseTextMessagesRepository(services.firestore, services.auth),
                audioStorageRepository = FirebaseAudioStorageRepository(services.storage),
                audioNotesRepository = FirebaseAudioNotesRepository(services.storage, services.firestore),
            )
        }
    }
}
