package com.proyectonami.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.proyectonami.app.R
import com.proyectonami.app.core.domain.validation.PersonNameError
import com.proyectonami.app.core.domain.validation.PhoneNumberError

/**
 * Traduce los motivos de validación de T-06 a recursos de cadenas.
 *
 * Lo comparten las pantallas que usan `PersonNameValidator` y
 * `PhoneNumberValidator`: la creación de cuenta (T-08) y el alta de contactos
 * (T-13). El dominio devuelve motivos y la interfaz decide el texto.
 */
@Composable
fun personNameErrorMessage(error: PersonNameError?): String? =
    error?.let {
        stringResource(
            when (it) {
                PersonNameError.EMPTY -> R.string.validation_person_name_empty
                PersonNameError.TOO_LONG -> R.string.validation_person_name_too_long
                PersonNameError.INVALID_CHARACTERS -> R.string.validation_person_name_invalid_characters
                PersonNameError.INVALID_FORMAT -> R.string.validation_person_name_invalid_format
            },
        )
    }

/** Igual que [personNameErrorMessage] para el teléfono de T-06. */
@Composable
fun phoneNumberErrorMessage(error: PhoneNumberError?): String? =
    error?.let {
        stringResource(
            when (it) {
                PhoneNumberError.EMPTY -> R.string.validation_phone_number_empty
                PhoneNumberError.INVALID_CHARACTERS -> R.string.validation_phone_number_invalid_characters
                PhoneNumberError.MISSING_COUNTRY_CODE -> R.string.validation_phone_number_missing_country_code
                PhoneNumberError.TOO_SHORT -> R.string.validation_phone_number_too_short
                PhoneNumberError.TOO_LONG -> R.string.validation_phone_number_too_long
                PhoneNumberError.INVALID_COUNTRY_CODE -> R.string.validation_phone_number_invalid_country_code
            },
        )
    }
