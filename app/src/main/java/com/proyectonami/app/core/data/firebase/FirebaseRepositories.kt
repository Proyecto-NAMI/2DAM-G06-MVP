package com.proyectonami.app.core.data.firebase

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference

interface AuthRepository {
    val currentUserId: String?

    fun signOut()
}

interface FirestoreRepository {
    fun userDocument(userId: String): DocumentReference

    fun contactDocument(userId: String, contactId: String): DocumentReference

    fun conversationDocument(conversationId: String): DocumentReference

    fun messageDocument(conversationId: String, messageId: String): DocumentReference
}

interface AudioStorageRepository {
    fun audioReference(conversationId: String, messageId: String): StorageReference
}

class FirebaseAuthRepository(
    private val auth: FirebaseAuth,
) : AuthRepository {
    override val currentUserId: String?
        get() = auth.currentUser?.uid

    override fun signOut() = auth.signOut()
}

class FirebaseFirestoreRepository(
    private val firestore: FirebaseFirestore,
) : FirestoreRepository {
    override fun userDocument(userId: String): DocumentReference =
        firestore.document(FirebasePaths.user(userId))

    override fun contactDocument(userId: String, contactId: String): DocumentReference =
        firestore.document(FirebasePaths.contact(userId, contactId))

    override fun conversationDocument(conversationId: String): DocumentReference =
        firestore.document(FirebasePaths.conversation(conversationId))

    override fun messageDocument(conversationId: String, messageId: String): DocumentReference =
        firestore.document(FirebasePaths.message(conversationId, messageId))
}

class FirebaseAudioStorageRepository(
    private val storage: FirebaseStorage,
) : AudioStorageRepository {
    override fun audioReference(conversationId: String, messageId: String): StorageReference =
        storage.reference.child(FirebasePaths.audio(conversationId, messageId))
}

class FirebaseRepositoryContainer(
    val authRepository: AuthRepository,
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
                authRepository = FirebaseAuthRepository(services.auth),
                firestoreRepository = FirebaseFirestoreRepository(services.firestore),
                audioStorageRepository = FirebaseAudioStorageRepository(services.storage),
            )
        }
    }
}

