package com.proyectonami.app.feature.contacts.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.proyectonami.app.core.domain.validation.PersonNameError
import com.proyectonami.app.core.domain.validation.PersonNameValidation
import com.proyectonami.app.core.domain.validation.PersonNameValidator
import com.proyectonami.app.core.domain.validation.PhoneNumberError
import com.proyectonami.app.core.domain.validation.PhoneNumberValidation
import com.proyectonami.app.core.domain.validation.PhoneNumberValidator
import com.proyectonami.app.feature.contacts.domain.AddContactResult
import com.proyectonami.app.feature.contacts.domain.AddContactUseCase
import com.proyectonami.app.feature.contacts.domain.Contact
import com.proyectonami.app.feature.contacts.domain.ContactFieldError
import com.proyectonami.app.feature.contacts.domain.NewContactRequest
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
    val saveFailed: Boolean = false,
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
 * Se validan todos en el mismo intento, como en la creación de cuenta (T-08),
 * para que la persona pueda corregirlos de una vez en lugar de descubrir un
 * error por envío. El caso de uso de T-12 sigue validando por su cuenta: esto
 * solo decide qué se muestra.
 */
fun validateContactForm(request: NewContactRequest): ContactFormValidation {
    val name = PersonNameValidator.validate(request.displayName)
    val surname = PersonNameValidator.validate(request.surname)
    val phone = PhoneNumberValidator.validate(request.phoneNumber)
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
                saveFailed = false,
            )
        is AddContactResult.Duplicate ->
            copy(
                isSaving = false,
                duplicateContact = result.existing,
                saveFailed = false,
            )
        is AddContactResult.Invalid ->
            copy(
                isSaving = false,
                duplicateContact = null,
                saveFailed = false,
            ).withFieldError(result.error)
        AddContactResult.Failed ->
            copy(
                isSaving = false,
                saveFailed = true,
            )
    }

private fun AddContactUiState.withFieldError(error: ContactFieldError): AddContactUiState =
    when (error) {
        is ContactFieldError.Name -> copy(nameError = error.reason)
        is ContactFieldError.Surname -> copy(surnameError = error.reason)
        is ContactFieldError.Phone -> copy(phoneNumberError = error.reason)
    }

/**
 * Presentación del alta de contactos (T-13, RF-03).
 *
 * Delega la validación y el guardado en `AddContactUseCase` (T-12) y no accede a
 * Firebase. La persona usuaria actual se consulta en cada envío porque la
 * pantalla se abre después de iniciar sesión.
 */
class AddContactViewModel(
    private val addContactUseCase: AddContactUseCase,
    private val currentUserId: () -> String?,
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(AddContactUiState())
    val uiState: StateFlow<AddContactUiState> = mutableUiState.asStateFlow()

    fun updateName(value: String) {
        mutableUiState.update {
            if (it.isFormEnabled) {
                it.copy(name = value, nameError = null, saveFailed = false)
            } else {
                it
            }
        }
    }

    fun updateSurname(value: String) {
        mutableUiState.update {
            if (it.isFormEnabled) {
                it.copy(surname = value, surnameError = null, saveFailed = false)
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
        val targetUserId = currentUserId()
        if (targetUserId.isNullOrBlank()) {
            // La pantalla solo se abre con sesión iniciada: si no la hay, se puede reintentar.
            mutableUiState.update { it.copy(saveFailed = true) }
            return
        }
        val request =
            NewContactRequest(
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
                        saveFailed = false,
                    )
                }
            is ContactFormValidation.Valid -> saveValidatedContact(targetUserId, validation)
        }
    }

    /** Deja el formulario listo para añadir otro contacto después de guardar uno. */
    fun addAnotherContact() {
        mutableUiState.value = AddContactUiState()
    }

    private fun saveValidatedContact(
        userId: String,
        validation: ContactFormValidation.Valid,
    ) {
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
                saveFailed = false,
            )
        }
        viewModelScope.launch {
            val result =
                addContactUseCase.add(
                    userId = userId,
                    request =
                        NewContactRequest(
                            displayName = validation.displayName,
                            surname = validation.surname,
                            phoneNumber = validation.phoneE164,
                        ),
                )
            mutableUiState.update { it.afterAddResult(result) }
        }
    }
}
