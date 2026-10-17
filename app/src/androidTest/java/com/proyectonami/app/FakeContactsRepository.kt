package com.proyectonami.app

import com.proyectonami.app.core.domain.contacts.Contact
import com.proyectonami.app.core.domain.contacts.ContactDraft
import com.proyectonami.app.core.domain.contacts.ContactSaveResult
import com.proyectonami.app.core.domain.contacts.ContactStatus
import com.proyectonami.app.core.domain.contacts.ContactsRemoteState
import com.proyectonami.app.core.domain.contacts.ContactsRepository
import com.proyectonami.app.core.domain.contacts.ConversationOpenResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/**
 * Repositorio de contactos en memoria para las pruebas instrumentadas (T-13).
 *
 * Reproduce lo que importa del repositorio real, que llama a la función
 * `addContact` (T-11): el mismo teléfono no se guarda dos veces, el servidor
 * decide si el contacto queda `LINKED` o `PENDING`, y la comprobación puede
 * agotar la cuota o no estar disponible sin guardar nada.
 */
class FakeContactsRepository : ContactsRepository {
    private val contactsByPhone = linkedMapOf<String, Contact>()

    /** Intentos de alta, incluidos los que fallan, los que no están disponibles y los duplicados. */
    var createAttempts: Int = 0
        private set

    /** Altas que fallarán antes de completarse correctamente. */
    var failuresBeforeSuccess: Int = 0

    /** Comprobaciones que no estarán disponibles antes de completarse correctamente. */
    var lookupsUnavailableBeforeSuccess: Int = 0

    /** Estado con el que el servidor guarda el contacto; `LINKED` simula que ya usa NAMI. */
    var savedStatus: ContactStatus = ContactStatus.PENDING

    val stored: List<Contact>
        get() = contactsByPhone.values.toList()

    var openAttempts: Int = 0
        private set

    override fun observeContacts(): Flow<ContactsRemoteState> = flowOf(ContactsRemoteState.Loaded(stored))

    override suspend fun addContact(contact: ContactDraft): ContactSaveResult {
        createAttempts++
        if (lookupsUnavailableBeforeSuccess > 0) {
            lookupsUnavailableBeforeSuccess--
            return ContactSaveResult.LookupUnavailable
        }
        if (failuresBeforeSuccess > 0) {
            failuresBeforeSuccess--
            return ContactSaveResult.Failed
        }
        val existing = contactsByPhone[contact.phoneE164]
        if (existing != null) {
            return ContactSaveResult.Duplicate(existing)
        }
        val saved =
            Contact(
                id = "contact_${contactsByPhone.size + 1}",
                displayName = contact.displayName,
                surname = contact.surname,
                phoneE164 = contact.phoneE164,
                linkedUserId = if (savedStatus == ContactStatus.LINKED) LINKED_USER_ID else null,
                status = savedStatus,
            )
        contactsByPhone[contact.phoneE164] = saved
        return ContactSaveResult.Added(saved)
    }

    override suspend fun openConversation(contactId: String): ConversationOpenResult {
        openAttempts++
        val contact = stored.firstOrNull { it.id == contactId } ?: return ConversationOpenResult.Failed
        return if (contact.status == ContactStatus.LINKED && contact.linkedUserId != null) {
            ConversationOpenResult.Opened("conversation_${contact.id}")
        } else {
            ConversationOpenResult.ContactPending
        }
    }

    /** Simula que ese teléfono ya estaba guardado antes de la prueba. */
    fun seed(contact: Contact) {
        contactsByPhone[contact.phoneE164] = contact
    }

    companion object {
        const val LINKED_USER_ID: String = "uid-ficticio-vinculado"
    }
}
