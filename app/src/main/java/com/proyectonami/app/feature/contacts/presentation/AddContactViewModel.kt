package com.proyectonami.app.feature.contacts.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.proyectonami.app.core.domain.contacts.AddContactResult
import com.proyectonami.app.core.domain.contacts.AddContactUseCase
import com.proyectonami.app.core.domain.contacts.Contact
import com.proyectonami.app.core.domain.contacts.ConversationOpenResult
import com.proyectonami.app.core.domain.contacts.NewContact
import com.proyectonami.app.core.domain.contacts.OpenConversationUseCase
import com.proyectonami.app.core.domain.validation.PersonNameError
import com.proyectonami.app.core.domain.validation.PersonNameValidation
import com.proyectonami.app.core.domain.validation.PersonNameValidator
import com.proyectonami.app.core.domain.validation.PhoneNumberError
import com.proyectonami.app.core.domain.validation.PhoneNumberValidation
import com.proyectonami.app.core.domain.validation.PhoneNumberValidator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Estado inmutable de la pantalla `Nuevo contacto` (T-13, RF-03).
 *
 * Los errores se guardan como motivo de dominio (`T-06`) y la pantalla los
 * traduce a recursos de cadenas, igual que hace la creación de cuenta (T-08).
 */
data class AddContactUiState(
    val name: String = "",
    val surname: String = "",
    val phoneNumber: String = DEFAULT_PHONE_PREFIX,
    val nameError: PersonNameError? = null,
    val surnameError: PersonNameError? = null,
    val phoneNumberError: PhoneNumberError? = null,
    val isSaving: Boolean = false,
    /** Contacto guardado; mientras no sea nulo la pantalla muestra la confirmación. */
    val addedContact: Contact? = null,
    /** Contacto que ya tenía ese teléfono; el alta no se repite (RF-03). */
    val duplicateContact: Contact? = null,
    /**
     * No se pudo comprobar si el teléfono tiene cuenta NAMI, así que **no** se
     * guardó nada. Ocurre cuando la función `addContact` agota la cuota o no está
     * disponible; se puede reintentar.
     */
    val lookupUnavailable: Boolean = false,
    val saveFailed: Boolean = false,
    val isOpeningConversation: Boolean = false,
    val conversationOpenFailed: Boolean = false,
    val conversationPending: Boolean = false,
    val openedConversationId: String? = null,
) {
    /** Los campos siguen editables hasta que el contacto se guarda. */
    val isFormEnabled: Boolean
        get() = addedContact == null && !isSaving

    companion object {
        /**
         * Mismo prefijo inicial editable que la creación de cuenta (T-08), tal y
         * como decidió la duda 7 de `spec.md`: el alta de contactos se comporta
         * igual que el alta de cuenta.
         */
        const val DEFAULT_PHONE_PREFIX = "+34 "
    }
}

/** Resultado de validar los tres campos del formulario a la vez (T-13). */
sealed interface ContactFormValidation {
    /** Los tres campos son válidos y ya están normalizados. */
    data class Valid(
        val displayName: String,
        val surname: String,
        val phoneE164: String,
    ) : ContactFormValidation

    /** Al menos un campo no es válido; los motivos nulos corresponden a campos correctos. */
    data class Invalid(
        val nameError: PersonNameError? = null,
        val surnameError: PersonNameError? = null,
        val phoneNumberError: PhoneNumberError? = null,
    ) : ContactFormValidation
}

/**
 * Valida los tres campos con las reglas de T-06.
 *
 * El caso de uso de `core.domain.contacts` también valida, pero informa de un
 * único motivo y no distingue el nombre de los apellidos. Validar aquí los tres
 * campos a la vez permite señalarlos todos en un solo intento, como hace la
 * creación de cuenta (T-08), y enviar después la forma normalizada.
 */
fun validateContactForm(contact: NewContact): ContactFormValidation {
    val name = PersonNameValidator.validate(contact.displayName)
    val surname = PersonNameValidator.validate(contact.surname)
    val phone = PhoneNumberValidator.validate(contact.phoneNumber)
    val phoneE164 = (phone as? PhoneNumberValidation.Valid)?.e164
    if (name is PersonNameValidation.Valid && surname is PersonNameValidation.Valid && phoneE164 != null) {
        return ContactFormValidation.Valid(
            displayName = name.value,
            surname = surname.value,
            phoneE164 = phoneE164,
        )
    }
    return ContactFormValidation.Invalid(
        nameError = (name as? PersonNameValidation.Invalid)?.error,
        surnameError = (surname as? PersonNameValidation.Invalid)?.error,
        phoneNumberError = (phone as? PhoneNumberValidation.Invalid)?.error,
    )
}

/** Aplica el resultado del alta al estado de la pantalla (T-13, RF-03). */
fun AddContactUiState.afterAddResult(result: AddContactResult): AddContactUiState =
    when (result) {
        is AddContactResult.Added ->
            copy(
                isSaving = false,
                addedContact = result.contact,
                duplicateContact = null,
                lookupUnavailable = false,
                saveFailed = false,
            )
        is AddContactResult.Duplicate ->
            copy(
                isSaving = false,
                duplicateContact = result.existing,
                lookupUnavailable = false,
                saveFailed = false,
            )
        // El caso de uso devuelve un motivo por campo y sin separar nombre de
        // apellidos; la validación de la pantalla ya los distingue, así que estos
        // dos caminos solo cubren una discrepancia entre ambas validaciones.
        is AddContactResult.InvalidName ->
            copy(
                isSaving = false,
                nameError = result.error,
                lookupUnavailable = false,
                saveFailed = false,
            )
        is AddContactResult.InvalidPhone ->
            copy(
                isSaving = false,
                phoneNumberError = result.error,
                lookupUnavailable = false,
                saveFailed = false,
            )
        AddContactResult.LookupUnavailable ->
            copy(
                isSaving = false,
                lookupUnavailable = true,
            )
        AddContactResult.Failed ->
            copy(
                isSaving = false,
                saveFailed = true,
            )
    }

/**
 * Presentación del alta de contactos (T-13, RF-03).
 *
 * Delega la validación y el guardado en `AddContactUseCase`, que llama a la
 * función `addContact` de T-11: la comprobación de si el teléfono tiene cuenta
 * NAMI y la escritura del contacto ocurren en el servidor, nunca en el cliente.
 */
class AddContactViewModel(
    private val addContactUseCase: AddContactUseCase,
    private val openConversationUseCase: OpenConversationUseCase? = null,
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(AddContactUiState())
    val uiState: StateFlow<AddContactUiState> = mutableUiState.asStateFlow()

    fun updateName(value: String) {
        mutableUiState.update {
            if (it.isFormEnabled) {
                it.copy(name = value, nameError = null, saveFailed = false, lookupUnavailable = false)
            } else {
                it
            }
        }
    }

    fun updateSurname(value: String) {
        mutableUiState.update {
            if (it.isFormEnabled) {
                it.copy(surname = value, surnameError = null, saveFailed = false, lookupUnavailable = false)
            } else {
                it
            }
        }
    }

    fun updatePhoneNumber(value: String) {
        mutableUiState.update {
            if (it.isFormEnabled) {
                it.copy(
                    phoneNumber = value,
                    phoneNumberError = null,
                    duplicateContact = null,
                    saveFailed = false,
                    lookupUnavailable = false,
                )
            } else {
                it
            }
        }
    }

    /**
     * Valida y guarda el contacto.
     *
     * Si ya existe un contacto con ese teléfono, no se escribe nada y el estado
     * conserva el contacto existente para poder indicar cuál es (RF-03).
     */
    fun saveContact() {
        val previousState = mutableUiState.value
        if (!previousState.isFormEnabled) {
            return
        }
        val request =
            NewContact(
                displayName = previousState.name,
                surname = previousState.surname,
                phoneNumber = previousState.phoneNumber,
            )
        when (val validation = validateContactForm(request)) {
            is ContactFormValidation.Invalid ->
                mutableUiState.update {
                    it.copy(
                        nameError = validation.nameError,
                        surnameError = validation.surnameError,
                        phoneNumberError = validation.phoneNumberError,
                        duplicateContact = null,
                        lookupUnavailable = false,
                        saveFailed = false,
                    )
                }
            is ContactFormValidation.Valid -> saveValidatedContact(validation)
        }
    }

    /** Deja el formulario listo para añadir otro contacto después de guardar uno. */
    fun addAnotherContact() {
        mutableUiState.value = AddContactUiState()
    }

    fun openConversation(contact: Contact) {
        val useCase = openConversationUseCase ?: return
        if (mutableUiState.value.isOpeningConversation) return
        mutableUiState.update {
            it.copy(
                isOpeningConversation = true,
                conversationOpenFailed = false,
                conversationPending = false,
                openedConversationId = null,
            )
        }
        viewModelScope.launch {
            when (val result = useCase(contact)) {
                is ConversationOpenResult.Opened ->
                    mutableUiState.update {
                        it.copy(
                            isOpeningConversation = false,
                            openedConversationId = result.conversationId,
                        )
                    }
                ConversationOpenResult.ContactPending ->
                    mutableUiState.update {
                        it.copy(
                            isOpeningConversation = false,
                            conversationPending = true,
                        )
                    }
                ConversationOpenResult.NotAuthorized,
                ConversationOpenResult.Failed,
                ->
                    mutableUiState.update {
                        it.copy(
                            isOpeningConversation = false,
                            conversationOpenFailed = true,
                        )
                    }
            }
        }
    }

    fun consumeOpenedConversation() {
        mutableUiState.update { it.copy(openedConversationId = null) }
    }

    private fun saveValidatedContact(validation: ContactFormValidation.Valid) {
        mutableUiState.update {
            it.copy(
                name = validation.displayName,
                surname = validation.surname,
                phoneNumber = validation.phoneE164,
                nameError = null,
                surnameError = null,
                phoneNumberError = null,
                isSaving = true,
                duplicateContact = null,
                lookupUnavailable = false,
                saveFailed = false,
            )
        }
        viewModelScope.launch {
            val result =
                addContactUseCase(
                    NewContact(
                        displayName = validation.displayName,
                        surname = validation.surname,
                        phoneNumber = validation.phoneE164,
                    ),
                )
            mutableUiState.update { it.afterAddResult(result) }
        }
    }
}
