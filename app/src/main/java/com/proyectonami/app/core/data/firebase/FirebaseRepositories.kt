package com.proyectonami.app.core.data.firebase

import android.app.Activity
import android.content.Context
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.proyectonami.app.core.domain.auth.AccountProfile
import com.proyectonami.app.core.domain.auth.AccountProfileRepository
import com.proyectonami.app.core.domain.auth.AccountProfileSaveResult
import com.proyectonami.app.core.domain.auth.AuthSession
import com.proyectonami.app.core.domain.auth.PhoneCodeVerificationResult
import com.proyectonami.app.core.domain.auth.PhoneVerificationChallenge
import com.proyectonami.app.core.domain.auth.PhoneVerificationRequestResult
import com.proyectonami.app.core.domain.validation.PhoneNumberValidation
import com.proyectonami.app.core.domain.validation.PhoneNumberValidator
import com.proyectonami.app.feature.contacts.domain.ContactRepository
import kotlinx.coroutines.flow.Flow
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
}

class FirebaseAudioStorageRepository(
    private val storage: FirebaseStorage,
) : AudioStorageRepository {
    override fun audioReference(
        conversationId: String,
        messageId: String,
    ): StorageReference = storage.reference.child(FirebasePaths.audio(conversationId, messageId))
}

class FirebaseRepositoryContainer(
    val authRepository: AuthRepository,
    val accountProfileRepository: AccountProfileRepository,
    val contactRepository: ContactRepository,
    val firestoreRepository: FirestoreRepository,
    val audioStorageRepository: AudioStorageRepository,
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
                audioStorageRepository = FirebaseAudioStorageRepository(services.storage),
            )
        }
    }
}
