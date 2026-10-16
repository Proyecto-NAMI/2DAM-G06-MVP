package com.proyectonami.app.feature.contacts.domain

/** Estado de vinculación de un contacto de confianza (RF-03). */
enum class ContactStatus {
    /** El contacto ya está unido a una cuenta NAMI. */
    LINKED,

    /** El teléfono todavía no está unido a ninguna cuenta NAMI. */
    PENDING,
}

/**
 * Contacto de confianza de una persona usuaria (T-12, RF-03).
 *
 * [status] se deduce de [linkedUserId] para que no pueda existir un contacto
 * «vinculado» sin cuenta ni uno «pendiente» con cuenta.
 */
data class Contact(
    val id: String,
    val displayName: String,
    val surname: String,
    val phoneE164: String,
    val linkedUserId: String? = null,
) {
    val status: ContactStatus
        get() = if (linkedUserId == null) ContactStatus.PENDING else ContactStatus.LINKED
}
