package com.proyectonami.app.core.data.firebase

import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
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
private const val FIELD_CREATED_AT = "createdAt"
private const val STATUS_LINKED = "linked"
private const val STATUS_PENDING = "pending"

/**
 * Repositorio de contactos sobre Firestore (T-12, RF-03).
 *
 * Escribe en `users/{userId}/contacts/{contactId}`, la única ubicación que las
 * reglas permiten a la persona propietaria. Como el identificador deriva del
 * teléfono normalizado (T-12), la creación es idempotente: si el documento ya
 * existe se devuelve el contacto guardado en lugar de crear otro.
 *
 * Sin conexión, Firestore resuelve la comprobación con su copia local y guarda la
 * escritura en cola, así que el alta puede completarse más tarde sin duplicar.
 */
class FirebaseContactRepository(
    private val firestore: FirebaseFirestore,
) : ContactRepository {
    override suspend fun createContact(
        userId: String,
        contact: Contact,
    ): ContactCreateResult {
        val document = firestore.document(FirebasePaths.contact(userId, contact.id))
        val snapshot = loadDocument(document) ?: return ContactCreateResult.Failed
        if (snapshot.exists()) {
            val existing = contactFromDocument(contact.id, snapshot.data.orEmpty())
            return ContactCreateResult.AlreadyExists(existing ?: contact)
        }
        return writeContact(document, contact)
    }

    private suspend fun loadDocument(document: DocumentReference): DocumentSnapshot? =
        suspendCancellableCoroutine { continuation ->
            document.get().addOnCompleteListener { task ->
                if (continuation.isActive) {
                    continuation.resume(if (task.isSuccessful) task.result else null)
                }
            }
        }

    private suspend fun writeContact(
        document: DocumentReference,
        contact: Contact,
    ): ContactCreateResult =
        suspendCancellableCoroutine { continuation ->
            document
                .set(contactFields(contact) + (FIELD_CREATED_AT to FieldValue.serverTimestamp()))
                .addOnCompleteListener { task ->
                    if (continuation.isActive) {
                        continuation.resume(
                            if (task.isSuccessful) ContactCreateResult.Created else ContactCreateResult.Failed,
                        )
                    }
                }
        }
}

/** Campos del documento de contacto acordados en `plan.md`. */
internal fun contactFields(contact: Contact): Map<String, Any?> =
    mapOf(
        FIELD_DISPLAY_NAME to contact.displayName,
        FIELD_SURNAME to contact.surname,
        FIELD_PHONE_E164 to contact.phoneE164,
        FIELD_STATUS to contact.status.toWireValue(),
        FIELD_LINKED_USER_ID to contact.linkedUserId,
    )

/**
 * Reconstruye un contacto desde el documento.
 *
 * Devuelve `null` si falta un campo obligatorio o si `status` y `linkedUserId` no
 * concuerdan, para no inventar un contacto a partir de datos incoherentes.
 */
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
                Contact(
                    id = id,
                    displayName = displayName,
                    surname = surname,
                    phoneE164 = phoneE164,
                    linkedUserId = userId,
                )
            }

        STATUS_PENDING ->
            if (linkedUserId == null) {
                Contact(
                    id = id,
                    displayName = displayName,
                    surname = surname,
                    phoneE164 = phoneE164,
                )
            } else {
                null
            }

        else -> null
    }
}

private fun ContactStatus.toWireValue(): String =
    when (this) {
        ContactStatus.LINKED -> STATUS_LINKED
        ContactStatus.PENDING -> STATUS_PENDING
    }
