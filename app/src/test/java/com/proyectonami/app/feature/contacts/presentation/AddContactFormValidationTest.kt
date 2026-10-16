package com.proyectonami.app.feature.contacts.presentation

import com.proyectonami.app.core.domain.validation.PersonNameError
import com.proyectonami.app.core.domain.validation.PhoneNumberError
import com.proyectonami.app.feature.contacts.domain.NewContactRequest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Validación de los tres campos del alta de contactos (T-13, RF-03).
 *
 * Comprueba que se informan todos los campos incorrectos en el mismo intento y
 * que los valores válidos llegan normalizados al caso de uso de T-12.
 */
class AddContactFormValidationTest {
    @Test
    fun allThreeFieldsAreReportedAtOnceWhenTheyAreEmpty() {
        val validation = validateContactForm(NewContactRequest("", "", ""))

        val invalid = validation as ContactFormValidation.Invalid
        assertEquals(PersonNameError.EMPTY, invalid.nameError)
        assertEquals(PersonNameError.EMPTY, invalid.surnameError)
        assertEquals(PhoneNumberError.EMPTY, invalid.phoneNumberError)
    }

    @Test
    fun invalidNameAndPhoneAreReportedTogetherWithoutBlameOnTheSurname() {
        val validation =
            validateContactForm(
                NewContactRequest(
                    displayName = "Ana2",
                    surname = "Prueba",
                    phoneNumber = "+34 12",
                ),
            )

        val invalid = validation as ContactFormValidation.Invalid
        assertEquals(PersonNameError.INVALID_CHARACTERS, invalid.nameError)
        assertEquals(PhoneNumberError.TOO_SHORT, invalid.phoneNumberError)
        assertNull(invalid.surnameError)
    }

    @Test
    fun validFieldsArriveNormalizedToTheUseCase() {
        val validation =
            validateContactForm(
                NewContactRequest(
                    displayName = "  Ana   María ",
                    surname = " Prueba ",
                    phoneNumber = "+34 612 345 678",
                ),
            )

        assertEquals(
            ContactFormValidation.Valid(
                displayName = "Ana María",
                surname = "Prueba",
                phoneE164 = "+34612345678",
            ),
            validation,
        )
    }

    @Test
    fun accentedNamesAreValidAndKeepTheirCharacters() {
        val validation =
            validateContactForm(
                NewContactRequest(
                    displayName = "José",
                    surname = "Núñez",
                    phoneNumber = "+34 612 345 678",
                ),
            )

        assertEquals(
            ContactFormValidation.Valid(
                displayName = "José",
                surname = "Núñez",
                phoneE164 = "+34612345678",
            ),
            validation,
        )
    }

    @Test
    fun phoneWithoutCountryCodeIsRejectedWithItsOwnReason() {
        val validation =
            validateContactForm(
                NewContactRequest(
                    displayName = "Ana",
                    surname = "Prueba",
                    phoneNumber = "612345678",
                ),
            )

        val invalid = validation as ContactFormValidation.Invalid
        assertEquals(PhoneNumberError.MISSING_COUNTRY_CODE, invalid.phoneNumberError)
        assertNull(invalid.nameError)
        assertNull(invalid.surnameError)
    }
}
