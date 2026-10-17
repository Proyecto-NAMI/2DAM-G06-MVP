package com.proyectonami.app.core.data.firebase

import android.app.Activity
import android.content.Context
import android.net.Uri
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Source
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageException
import com.google.firebase.storage.StorageMetadata
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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
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
    private val auth: FirebaseAuth,
) : AudioNotesRepository {
    override fun upload(
        metadata: AudioNoteMetadata,
        localPath: String,
    ): Flow<AudioUploadState> =
        flow {
            emit(AudioUploadState.Pending)
            val userId = auth.currentUser?.uid
            if (userId == null) {
                emit(AudioUploadState.Failed(metadata, AudioUploadFailure.UNAUTHENTICATED))
                return@flow
            }
            if (userId != metadata.senderId) {
                emit(AudioUploadState.Failed(metadata, AudioUploadFailure.NOT_AUTHORIZED))
                return@flow
            }

            try {
                when (existingAudioMessage(metadata)) {
                    ExistingAudioMessage.MATCHING -> {
                        deleteLocal(localPath).getOrThrow()
                        emit(AudioUploadState.Sent(metadata))
                        return@flow
                    }
                    ExistingAudioMessage.CONFLICT -> {
                        emit(AudioUploadState.Failed(metadata, AudioUploadFailure.ID_CONFLICT))
                        return@flow
                    }
                    ExistingAudioMessage.MISSING -> Unit
                }

                val localFile = File(localPath)
                if (!localFile.isFile || localFile.length() != metadata.sizeBytes) {
                    emit(AudioUploadState.Failed(metadata, AudioUploadFailure.INVALID_NOTE))
                    return@flow
                }

                val reference = storage.reference.child(metadata.storagePath)
                emit(AudioUploadState.Progress(0f))
                try {
                    uploadProgress(reference, localFile, metadata.contentType).collect { fraction ->
                        emit(AudioUploadState.Progress(fraction))
                    }
                } catch (error: Exception) {
                    // Otra petición concurrente pudo terminar mientras esta subida
                    // recibía la denegación de sobrescritura del objeto ya enviado.
                    if (runCatching { existingAudioMessage(metadata) }.getOrNull() == ExistingAudioMessage.MATCHING) {
                        deleteLocal(localPath).getOrThrow()
                        emit(AudioUploadState.Sent(metadata))
                    } else {
                        emit(AudioUploadState.Failed(metadata, error.toAudioUploadFailure()))
                    }
                    return@flow
                }

                val persistenceFailure = persistMetadata(metadata, userId)
                if (persistenceFailure != null) {
                    emit(AudioUploadState.Failed(metadata, persistenceFailure))
                    return@flow
                }

                deleteLocal(localPath).getOrThrow()
                emit(AudioUploadState.Sent(metadata))
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                emit(AudioUploadState.Failed(metadata, error.toAudioUploadFailure()))
            }
        }

    override suspend fun deleteLocal(localPath: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                val file = File(localPath)
                if (file.exists() && !file.delete()) error("Unable to delete temporary audio")
            }
        }

    private fun uploadProgress(
        reference: StorageReference,
        localFile: File,
        contentType: String,
    ): Flow<Float> =
        callbackFlow {
            val storageMetadata = StorageMetadata.Builder().setContentType(contentType).build()
            val task = reference.putFile(Uri.fromFile(localFile), storageMetadata)
            task.addOnProgressListener { snapshot ->
                val total = snapshot.totalByteCount
                if (total > 0L) trySend((snapshot.bytesTransferred.toFloat() / total).coerceIn(0f, 1f))
            }
            task.addOnSuccessListener {
                trySend(1f)
                close()
            }
            task.addOnFailureListener(::close)
            task.addOnCanceledListener { close(CancellationException("Audio upload cancelled")) }
            awaitClose {
                if (!task.isComplete) task.cancel()
            }
        }

    private suspend fun existingAudioMessage(metadata: AudioNoteMetadata): ExistingAudioMessage =
        suspendCancellableCoroutine { continuation ->
            firestore
                .document(FirebasePaths.message(metadata.conversationId, metadata.clientMessageId))
                .get(Source.SERVER)
                .addOnCompleteListener { task ->
                    if (!continuation.isActive) return@addOnCompleteListener
                    if (!task.isSuccessful) {
                        continuation.resumeWith(Result.failure(task.exception ?: IllegalStateException("Audio message lookup failed")))
                        return@addOnCompleteListener
                    }
                    val snapshot = task.result
                    if (snapshot == null) {
                        continuation.resumeWith(Result.failure(IllegalStateException("Audio message lookup failed")))
                    } else if (!snapshot.exists()) {
                        continuation.resume(ExistingAudioMessage.MISSING)
                    } else {
                        continuation.resume(
                            if (snapshot.matches(metadata)) ExistingAudioMessage.MATCHING else ExistingAudioMessage.CONFLICT,
                        )
                    }
                }
        }

    private suspend fun persistMetadata(
        metadata: AudioNoteMetadata,
        userId: String,
    ): AudioUploadFailure? {
        val conversationReference = firestore.document(FirebasePaths.conversation(metadata.conversationId))
        val messageReference = firestore.document(FirebasePaths.message(metadata.conversationId, metadata.clientMessageId))
        return suspendCancellableCoroutine { continuation ->
            firestore
                .runTransaction { transaction ->
                    val conversation = transaction.get(conversationReference)
                    val participantIds = conversation.get("participantIds") as? List<*>
                    if (participantIds == null ||
                        participantIds.size != 2 ||
                        participantIds.any { it !is String || it.isBlank() } ||
                        participantIds.distinct().size != 2
                    ) {
                        return@runTransaction AudioUploadFailure.INVALID_NOTE
                    }
                    if (userId !in participantIds) return@runTransaction AudioUploadFailure.NOT_AUTHORIZED

                    val existing = transaction.get(messageReference)
                    if (existing.exists()) {
                        return@runTransaction if (existing.matches(metadata)) null else AudioUploadFailure.ID_CONFLICT
                    }

                    val recipientId = participantIds.filterIsInstance<String>().single { it != userId }
                    transaction.set(
                        messageReference,
                        mapOf(
                            "clientMessageId" to metadata.clientMessageId,
                            "senderId" to metadata.senderId,
                            "type" to "audio",
                            "durationMillis" to metadata.durationMillis,
                            "sizeBytes" to metadata.sizeBytes,
                            "contentType" to metadata.contentType,
                            "storagePath" to metadata.storagePath,
                            "createdAt" to FieldValue.serverTimestamp(),
                            "status" to "sent",
                        ),
                    )
                    transaction.update(
                        conversationReference,
                        FieldPath.of("lastActivityAt"),
                        FieldValue.serverTimestamp(),
                        FieldPath.of("unreadCountByUser", recipientId),
                        FieldValue.increment(1),
                    )
                    null
                }.addOnCompleteListener { task ->
                    if (!continuation.isActive) return@addOnCompleteListener
                    if (task.isSuccessful) {
                        continuation.resume(task.result)
                    } else {
                        continuation.resume(task.exception.toAudioUploadFailure())
                    }
                }
        }
    }
}

private enum class ExistingAudioMessage { MISSING, MATCHING, CONFLICT }

private fun com.google.firebase.firestore.DocumentSnapshot.matches(metadata: AudioNoteMetadata): Boolean =
    getString("clientMessageId") == metadata.clientMessageId &&
        getString("senderId") == metadata.senderId &&
        getString("type") == "audio" &&
        getLong("durationMillis") == metadata.durationMillis &&
        getLong("sizeBytes") == metadata.sizeBytes &&
        getString("contentType") == metadata.contentType &&
        getString("storagePath") == metadata.storagePath &&
        getString("status") == "sent"

private fun Exception?.toAudioUploadFailure(): AudioUploadFailure =
    when (this) {
        is FirebaseFirestoreException ->
            when (code) {
                FirebaseFirestoreException.Code.UNAUTHENTICATED -> AudioUploadFailure.UNAUTHENTICATED
                FirebaseFirestoreException.Code.PERMISSION_DENIED -> AudioUploadFailure.NOT_AUTHORIZED
                FirebaseFirestoreException.Code.UNAVAILABLE,
                FirebaseFirestoreException.Code.DEADLINE_EXCEEDED,
                -> AudioUploadFailure.NETWORK
                else -> AudioUploadFailure.UNKNOWN
            }
        is StorageException ->
            when (errorCode) {
                StorageException.ERROR_NOT_AUTHENTICATED -> AudioUploadFailure.UNAUTHENTICATED
                StorageException.ERROR_NOT_AUTHORIZED -> AudioUploadFailure.NOT_AUTHORIZED
                StorageException.ERROR_RETRY_LIMIT_EXCEEDED -> AudioUploadFailure.NETWORK
                else -> AudioUploadFailure.UNKNOWN
            }
        else -> AudioUploadFailure.UNKNOWN
    }

class FirebaseRepositoryContainer(
    val authRepository: AuthRepository,
    val accountProfileRepository: AccountProfileRepository,
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
                audioNotesRepository = FirebaseAudioNotesRepository(services.storage, services.firestore, services.auth),
            )
        }
    }
}
