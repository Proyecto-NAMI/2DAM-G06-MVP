package com.proyectonami.app.core.domain.contacts

import com.proyectonami.app.core.domain.validation.PersonNameError
import com.proyectonami.app.core.domain.validation.PersonNameValidation
import com.proyectonami.app.core.domain.validation.PersonNameValidator
import com.proyectonami.app.core.domain.validation.PhoneNumberError
import com.proyectonami.app.core.domain.validation.PhoneNumberValidation
import com.proyectonami.app.core.domain.validation.PhoneNumberValidator
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow

data class ContactDraft(
    val displayName: String,
    val surname: String,
    val phoneE164: String,
)

data class Contact(
    val id: String,
    val displayName: String,
    val surname: String,
    val phoneE164: String,
    val linkedUserId: String?,
    val status: ContactStatus,
)

enum class ContactStatus {
    LINKED,
    PENDING,
}

data class NewContact(
    val displayName: String,
    val surname: String,
    val phoneNumber: String,
)

sealed interface ContactsRemoteState {
    data object Loading : ContactsRemoteState

    data class Loaded(
        val contacts: List<Contact>,
    ) : ContactsRemoteState

    data object Failed : ContactsRemoteState
}

sealed interface ContactSaveResult {
    data class Added(
        val contact: Contact,
    ) : ContactSaveResult

    data class Duplicate(
        val existing: Contact,
    ) : ContactSaveResult

    data object Failed : ContactSaveResult

    data object LookupUnavailable : ContactSaveResult
}

sealed interface ConversationOpenResult {
    data class Opened(
        val conversationId: String,
    ) : ConversationOpenResult

    data object ContactPending : ConversationOpenResult

    data object NotAuthorized : ConversationOpenResult

    data object Failed : ConversationOpenResult
}

sealed interface AddContactResult {
    data class Added(
        val contact: Contact,
    ) : AddContactResult

    data class Duplicate(
        val existing: Contact,
    ) : AddContactResult

    data class InvalidName(
        val error: PersonNameError,
    ) : AddContactResult

    data class InvalidPhone(
        val error: PhoneNumberError,
    ) : AddContactResult

    data object LookupUnavailable : AddContactResult

    data object Failed : AddContactResult
}

interface ContactsRepository {
    fun observeContacts(): Flow<ContactsRemoteState>

    suspend fun addContact(contact: ContactDraft): ContactSaveResult

    suspend fun openConversation(contactId: String): ConversationOpenResult = ConversationOpenResult.Failed
}

class AddContactUseCase(
    private val contactsRepository: ContactsRepository,
    private val defaultCallingCode: String? = null,
) {
    suspend operator fun invoke(contact: NewContact): AddContactResult {
        val displayName =
            when (val validation = PersonNameValidator.validate(contact.displayName)) {
                is PersonNameValidation.Valid -> validation.value
                is PersonNameValidation.Invalid -> return AddContactResult.InvalidName(validation.error)
            }
        val surname =
            when (val validation = PersonNameValidator.validate(contact.surname)) {
                is PersonNameValidation.Valid -> validation.value
                is PersonNameValidation.Invalid -> return AddContactResult.InvalidName(validation.error)
            }
        val phoneE164 =
            when (val validation = PhoneNumberValidator.validate(contact.phoneNumber, defaultCallingCode)) {
                is PhoneNumberValidation.Valid -> validation.e164
                is PhoneNumberValidation.Invalid -> return AddContactResult.InvalidPhone(validation.error)
            }

        val draft = ContactDraft(displayName, surname, phoneE164)
        return try {
            when (val saved = contactsRepository.addContact(draft)) {
                is ContactSaveResult.Added -> AddContactResult.Added(saved.contact)
                is ContactSaveResult.Duplicate -> AddContactResult.Duplicate(saved.existing)
                ContactSaveResult.Failed -> AddContactResult.Failed
                ContactSaveResult.LookupUnavailable -> AddContactResult.LookupUnavailable
            }
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (_: Exception) {
            AddContactResult.Failed
        }
    }
}

class OpenConversationUseCase(
    private val contactsRepository: ContactsRepository,
) {
    suspend operator fun invoke(contact: Contact): ConversationOpenResult {
        if (contact.status != ContactStatus.LINKED || contact.linkedUserId.isNullOrBlank()) {
            return ConversationOpenResult.ContactPending
        }
        return try {
            contactsRepository.openConversation(contact.id)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (_: Exception) {
            ConversationOpenResult.Failed
        }
    }
}
