package com.proyectonami.app

import com.proyectonami.app.feature.contacts.domain.Contact
import com.proyectonami.app.feature.contacts.domain.ContactCreateResult
import com.proyectonami.app.feature.contacts.domain.ContactRepository

/**
 * Repositorio de contactos en memoria para las pruebas instrumentadas (T-13).
 *
 * Reproduce el comportamiento que importa de `FirebaseContactRepository` (T-12):
 * el identificador deriva del teléfono, así que el mismo teléfono no se guarda
 * dos veces y el intento repetido devuelve el contacto existente.
 */
class FakeContactRepository : ContactRepository {
    private val contactsByPhone = linkedMapOf<String, Contact>()

    /** Intentos de escritura, incluidos los que fallan y los que encuentran duplicado. */
    var createAttempts: Int = 0
        private set

    /** Número de escrituras que fallarán antes de completarse correctamente. */
    var failuresBeforeSuccess: Int = 0

    val stored: List<Contact>
        get() = contactsByPhone.values.toList()

    override suspend fun createContact(
        userId: String,
        contact: Contact,
    ): ContactCreateResult {
        createAttempts++
        if (failuresBeforeSuccess > 0) {
            failuresBeforeSuccess--
            return ContactCreateResult.Failed
        }
        val existing = contactsByPhone[contact.phoneE164]
        if (existing != null) {
            return ContactCreateResult.AlreadyExists(existing)
        }
        contactsByPhone[contact.phoneE164] = contact
        return ContactCreateResult.Created
    }

    /** Simula que ese teléfono ya estaba guardado antes de la prueba. */
    fun seed(contact: Contact) {
        contactsByPhone[contact.phoneE164] = contact
    }
}
