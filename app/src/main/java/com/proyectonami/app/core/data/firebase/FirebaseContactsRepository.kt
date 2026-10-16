package com.proyectonami.app.core.data.firebase

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.FirebaseFunctionsException
import com.proyectonami.app.core.domain.contacts.Contact
import com.proyectonami.app.core.domain.contacts.ContactDraft
import com.proyectonami.app.core.domain.contacts.ContactSaveResult
import com.proyectonami.app.core.domain.contacts.ContactStatus
import com.proyectonami.app.core.domain.contacts.ContactsRemoteState
import com.proyectonami.app.core.domain.contacts.ContactsRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class FirebaseContactsRepository(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val functions: FirebaseFunctions,
) : ContactsRepository {
    override fun observeContacts(): Flow<ContactsRemoteState> =
        flow {
            emit(ContactsRemoteState.Loading)
            emitAll(
                callbackFlow {
                    val userId = auth.currentUser?.uid
                    if (userId == null) {
                        trySend(ContactsRemoteState.Failed)
                        close()
                        return@callbackFlow
                    }

                    val registration =
                        firestore
                            .collection(FirebasePaths.contacts(userId))
                            .orderBy("displayName")
                            .addSnapshotListener { snapshot, error ->
                                if (error != null || snapshot == null) {
                                    trySend(ContactsRemoteState.Failed)
                                } else {
                                    val contacts = snapshot.documents.mapNotNull { it.toContact() }
                                    if (contacts.size == snapshot.documents.size) {
                                        trySend(ContactsRemoteState.Loaded(contacts))
                                    } else {
                                        trySend(ContactsRemoteState.Failed)
                                    }
                                }
                            }
                    awaitClose { registration.remove() }
                },
            )
        }

    override suspend fun addContact(contact: ContactDraft): ContactSaveResult {
        if (auth.currentUser == null) return ContactSaveResult.Failed
        return suspendCancellableCoroutine { continuation ->
            functions
                .getHttpsCallable("addContact")
                .call(
                    mapOf(
                        "displayName" to contact.displayName,
                        "surname" to contact.surname,
                        "phoneE164" to contact.phoneE164,
                    ),
                ).addOnCompleteListener { task ->
                    if (continuation.isActive) {
                        val result =
                            if (task.isSuccessful) {
                                task.result?.data.toContactSaveResult() ?: ContactSaveResult.Failed
                            } else {
                                task.exception.toContactSaveFailure()
                            }
                        continuation.resume(result)
                    }
                }
        }
    }
}

private fun DocumentSnapshot.toContact(): Contact? {
    val status =
        when (getString("status")) {
            "linked" -> ContactStatus.LINKED
            "pending" -> ContactStatus.PENDING
            else -> return null
        }
    return Contact(
        id = id,
        displayName = getString("displayName") ?: return null,
        surname = getString("surname") ?: return null,
        phoneE164 = getString("phoneE164") ?: return null,
        linkedUserId = getString("linkedUserId"),
        status = status,
    )
}

private fun Any?.toContactSaveResult(): ContactSaveResult? {
    val response = this as? Map<*, *> ?: return null
    val contact = (response["contact"] as? Map<*, *>)?.toContact() ?: return null
    return when (response["outcome"]) {
        "added" -> ContactSaveResult.Added(contact)
        "duplicate" -> ContactSaveResult.Duplicate(contact)
        else -> null
    }
}

private fun Map<*, *>.toContact(): Contact? {
    val status =
        when (this["status"] as? String) {
            "linked" -> ContactStatus.LINKED
            "pending" -> ContactStatus.PENDING
            else -> return null
        }
    return Contact(
        id = this["id"] as? String ?: return null,
        displayName = this["displayName"] as? String ?: return null,
        surname = this["surname"] as? String ?: return null,
        phoneE164 = this["phoneE164"] as? String ?: return null,
        linkedUserId = this["linkedUserId"] as? String,
        status = status,
    )
}

private fun Throwable?.toContactSaveFailure(): ContactSaveResult =
    when ((this as? FirebaseFunctionsException)?.code) {
        FirebaseFunctionsException.Code.RESOURCE_EXHAUSTED,
        FirebaseFunctionsException.Code.UNAVAILABLE,
        FirebaseFunctionsException.Code.DEADLINE_EXCEEDED,
        -> ContactSaveResult.LookupUnavailable

        else -> ContactSaveResult.Failed
    }
