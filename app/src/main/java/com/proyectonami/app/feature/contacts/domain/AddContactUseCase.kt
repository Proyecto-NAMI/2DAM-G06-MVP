package com.proyectonami.app.feature.contacts.domain

import com.proyectonami.app.core.domain.validation.PersonNameError
import com.proyectonami.app.core.domain.validation.PersonNameValidation
import com.proyectonami.app.core.domain.validation.PersonNameValidator
import com.proyectonami.app.core.domain.validation.PhoneNumberError
import com.proyectonami.app.core.domain.validation.PhoneNumberValidation
import com.proyectonami.app.core.domain.validation.PhoneNumberValidator

/** Datos que la pantalla de nuevo contacto recoge sin validar (T-13). */
data class NewContactRequest(
    val displayName: String,
    val surname: String,
    val phoneNumber: String,
)

/**
 * Campo concreto que no pasó la validación y su motivo (T-06).
 *
 * La pantalla traduce cada motivo con los recursos `validation_person_name_*` y
 * `validation_phone_number_*` que ya existen.
 */
sealed interface ContactFieldError {
    data class Name(
        val reason: PersonNameError,
    ) : ContactFieldError

    data class Surname(
        val reason: PersonNameError,
    ) : ContactFieldError

    data class Phone(
        val reason: PhoneNumberError,
    ) : ContactFieldError
}

/** Resultado del alta de un contacto (RF-03). */
sealed interface AddContactResult {
    /** El contacto se guardó, vinculado o pendiente según [Contact.status]. */
    data class Added(
        val contact: Contact,
    ) : AddContactResult

    /** El teléfono ya pertenece a [existing]; no se creó otro contacto. */
    data class Duplicate(
        val existing: Contact,
    ) : AddContactResult

    /** Algún campo no es válido; no se guardó nada. */
    data class Invalid(
        val error: ContactFieldError,
    ) : AddContactResult

    /** Fallo recuperable al guardar; se puede reintentar sin duplicar. */
    data object Failed : AddContactResult
}

/**
 * Alta de un contacto de confianza (T-12, RF-03), independiente de Android.
 *
 * Reglas:
 * - Valida nombre, apellidos y teléfono con los validadores de T-06 y guarda la
 *   forma normalizada del teléfono en E.164.
 * - Si ya existe un contacto con ese teléfono, devuelve [AddContactResult.Duplicate]
 *   con el contacto existente para poder indicar cuál es.
 * - La vinculación depende de [accountResolver]. El MVP inyecta
 *   [DisabledContactAccountResolver], así que el contacto queda pendiente mientras
 *   T-11 mantenga cerrada la búsqueda de cuentas desde el cliente.
 */
class AddContactUseCase(
    private val repository: ContactRepository,
    private val accountResolver: ContactAccountResolver = DisabledContactAccountResolver,
) {
    suspend fun add(
        userId: String,
        request: NewContactRequest,
    ): AddContactResult {
        require(userId.isNotBlank()) { "userId no puede estar vacío" }

        val displayName =
            when (val validation = PersonNameValidator.validate(request.displayName)) {
                is PersonNameValidation.Valid -> validation.value
                is PersonNameValidation.Invalid ->
                    return AddContactResult.Invalid(ContactFieldError.Name(validation.error))
            }
        val surname =
            when (val validation = PersonNameValidator.validate(request.surname)) {
                is PersonNameValidation.Valid -> validation.value
                is PersonNameValidation.Invalid ->
                    return AddContactResult.Invalid(ContactFieldError.Surname(validation.error))
            }
        val phoneE164 =
            when (val validation = PhoneNumberValidator.validate(request.phoneNumber)) {
                is PhoneNumberValidation.Valid -> validation.e164
                is PhoneNumberValidation.Invalid ->
                    return AddContactResult.Invalid(ContactFieldError.Phone(validation.error))
            }

        val contact =
            Contact(
                id = ContactIdFactory.forPhoneE164(phoneE164),
                displayName = displayName,
                surname = surname,
                phoneE164 = phoneE164,
                linkedUserId = resolveLinkedUserId(phoneE164),
            )

        return when (val created = repository.createContact(userId, contact)) {
            ContactCreateResult.Created -> AddContactResult.Added(contact)
            is ContactCreateResult.AlreadyExists -> AddContactResult.Duplicate(created.contact)
            ContactCreateResult.Failed -> AddContactResult.Failed
        }
    }

    private suspend fun resolveLinkedUserId(phoneE164: String): String? =
        when (val resolved = accountResolver.resolveAccount(phoneE164)) {
            is ResolvedAccount.Found -> resolved.userId
            ResolvedAccount.NotFound, ResolvedAccount.Unavailable -> null
        }
}
