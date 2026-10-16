package com.proyectonami.app.core.data.firebase

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.functions.FirebaseFunctions
import com.proyectonami.app.feature.contacts.domain.Contact
import com.proyectonami.app.feature.contacts.domain.ContactCreateResult
import com.proyectonami.app.feature.contacts.domain.ContactRepository
import com.proyectonami.app.feature.contacts.domain.ContactStatus
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

private const val FIELD_DISPLAY_NAME = "displayName"
private const val FIELD_SURNAME = "surname"
private const val FIELD_PHONE_E164 = "phoneE164"
private const val FIELD_STATUS = "status"
private const val FIELD_LINKED_USER_ID = "linkedUserId"
private const val STATUS_LINKED = "linked"
private const val STATUS_PENDING = "pending"

/**
 * Repositorio de contactos sobre el callable `addContact` (T-12, RF-03).
 *
 * Las reglas de Firestore no permiten escrituras de contactos desde el cliente:
 * la función comprueba la sesión, el teléfono verificado y la idempotencia en
 * una única frontera de autorización.
 */
class FirebaseContactRepository(
    private val auth: FirebaseAuth,
    private val functions: FirebaseFunctions,
) : ContactRepository {
    override suspend fun createContact(
        userId: String,
        contact: Contact,
    ): ContactCreateResult {
        if (auth.currentUser?.uid != userId) return ContactCreateResult.Failed
        return suspendCancellableCoroutine { continuation ->
            functions
                .getHttpsCallable("addContact")
                .call(
                    mapOf(
                        FIELD_DISPLAY_NAME to contact.displayName,
                        FIELD_SURNAME to contact.surname,
                        FIELD_PHONE_E164 to contact.phoneE164,
                    ),
                ).addOnCompleteListener { task ->
                    if (continuation.isActive) {
                        continuation.resume(
                            if (task.isSuccessful) {
                                task.result?.data.toContactCreateResult()
                            } else {
                                ContactCreateResult.Failed
                            },
                        )
                    }
                }
        }
    }
}

private fun Any?.toContactCreateResult(): ContactCreateResult {
    val response = this as? Map<*, *> ?: return ContactCreateResult.Failed
    val data = response["contact"] as? Map<*, *> ?: return ContactCreateResult.Failed
    val contact =
        Contact(
            id = data["id"] as? String ?: return ContactCreateResult.Failed,
            displayName = data[FIELD_DISPLAY_NAME] as? String ?: return ContactCreateResult.Failed,
            surname = data[FIELD_SURNAME] as? String ?: return ContactCreateResult.Failed,
            phoneE164 = data[FIELD_PHONE_E164] as? String ?: return ContactCreateResult.Failed,
            linkedUserId = data[FIELD_LINKED_USER_ID] as? String,
        )
    return when (response["outcome"] as? String) {
        "added" -> ContactCreateResult.Created
        "duplicate" -> ContactCreateResult.AlreadyExists(contact)
        else -> ContactCreateResult.Failed
    }
}

/** Campos históricos del documento, conservados para validar el contrato de lectura. */
internal fun contactFields(contact: Contact): Map<String, Any?> =
    mapOf(
        FIELD_DISPLAY_NAME to contact.displayName,
        FIELD_SURNAME to contact.surname,
        FIELD_PHONE_E164 to contact.phoneE164,
        FIELD_STATUS to contact.status.toWireValue(),
        FIELD_LINKED_USER_ID to contact.linkedUserId,
    )

internal fun contactFromDocument(
    id: String,
    data: Map<String, Any?>,
): Contact? {
    val displayName = data[FIELD_DISPLAY_NAME] as? String ?: return null
    val surname = data[FIELD_SURNAME] as? String ?: return null
    val phoneE164 = data[FIELD_PHONE_E164] as? String ?: return null
    val linkedUserId = (data[FIELD_LINKED_USER_ID] as? String)?.takeIf { it.isNotBlank() }
    return when (data[FIELD_STATUS] as? String) {
        STATUS_LINKED ->
            linkedUserId?.let { userId ->
                Contact(id, displayName, surname, phoneE164, userId)
            }
        STATUS_PENDING ->
            if (linkedUserId == null) Contact(id, displayName, surname, phoneE164) else null
        else -> null
    }
}

private fun ContactStatus.toWireValue(): String =
    when (this) {
        ContactStatus.LINKED -> STATUS_LINKED
        ContactStatus.PENDING -> STATUS_PENDING
    }
