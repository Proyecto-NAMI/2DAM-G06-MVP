package com.proyectonami.app.core.domain.validation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class PhoneNumberValidatorTest {
    @Test
    fun internationalNumberIsNormalizedToE164() {
        assertValid(expected = "+34612345678", raw = "+34612345678")
    }

    @Test
    fun spacesAndPunctuationAreDiscarded() {
        assertValid(expected = "+34612345678", raw = "+34 612 345 678")
        assertValid(expected = "+34612345678", raw = "+34.612.345.678")
        assertValid(expected = "+14155552671", raw = "+1 (415) 555-2671")
        assertValid(expected = "+34612345678", raw = "  +34 612 345 678  ")
    }

    @Test
    fun numberWithoutPrefixUsesTheDefaultCallingCode() {
        assertValid(expected = "+34612345678", raw = "612345678", defaultCallingCode = "34")
        assertValid(expected = "+34612345678", raw = "612 345 678", defaultCallingCode = "+34")
    }

    @Test
    fun numberWithoutPrefixAndWithoutDefaultIsRejected() {
        assertInvalid(expected = PhoneNumberError.MISSING_COUNTRY_CODE, raw = "612345678")
    }

    @Test
    fun emptyNumberIsRejected() {
        assertInvalid(expected = PhoneNumberError.EMPTY, raw = "")
        assertInvalid(expected = PhoneNumberError.EMPTY, raw = "   ")
    }

    @Test
    fun lettersAndMisplacedPlusAreRejected() {
        assertInvalid(expected = PhoneNumberError.INVALID_CHARACTERS, raw = "612 345 ABC")
        assertInvalid(expected = PhoneNumberError.INVALID_CHARACTERS, raw = "+34612345678#")
        assertInvalid(expected = PhoneNumberError.INVALID_CHARACTERS, raw = "34+612345678")
        assertInvalid(expected = PhoneNumberError.INVALID_CHARACTERS, raw = "++34612345678")
    }

    @Test
    fun minimumAndMaximumDigitCountsAreEnforced() {
        assertValid(expected = "+3461234", raw = "+3461234")
        assertValid(expected = "+123456789012345", raw = "+123456789012345")
        assertInvalid(expected = PhoneNumberError.TOO_SHORT, raw = "+34612")
        assertInvalid(expected = PhoneNumberError.TOO_SHORT, raw = "+")
        assertInvalid(expected = PhoneNumberError.TOO_LONG, raw = "+1234567890123456")
    }

    @Test
    fun countryCodeCannotStartWithZero() {
        assertInvalid(expected = PhoneNumberError.INVALID_COUNTRY_CODE, raw = "+0612345678")
    }

    @Test
    fun digitsFromOtherAlphabetsAreConverted() {
        assertValid(expected = "+34612345678", raw = "+٣٤٦١٢٣٤٥٦٧٨")
    }

    @Test
    fun invalidDefaultCallingCodeFailsFast() {
        assertThrows(IllegalArgumentException::class.java) {
            PhoneNumberValidator.validate("612345678", defaultCallingCode = "034")
        }
        assertThrows(IllegalArgumentException::class.java) {
            PhoneNumberValidator.validate("612345678", defaultCallingCode = "1234")
        }
        assertThrows(IllegalArgumentException::class.java) {
            PhoneNumberValidator.validate("612345678", defaultCallingCode = "")
        }
        assertThrows(IllegalArgumentException::class.java) {
            PhoneNumberValidator.validate("612345678", defaultCallingCode = "3x4")
        }
    }

    @Test
    fun defaultCallingCodeConvertsInternationalDigitsToAscii() {
        assertValid(expected = "+34612345678", raw = "612345678", defaultCallingCode = "٣٤")
        assertValid(expected = "+34612345678", raw = "612345678", defaultCallingCode = "+٣٤")
    }

    @Test
    fun everyErrorIsReachable() {
        val examples =
            mapOf(
                PhoneNumberError.EMPTY to "",
                PhoneNumberError.INVALID_CHARACTERS to "612 345 ABC",
                PhoneNumberError.MISSING_COUNTRY_CODE to "612345678",
                PhoneNumberError.TOO_SHORT to "+34612",
                PhoneNumberError.TOO_LONG to "+1234567890123456",
                PhoneNumberError.INVALID_COUNTRY_CODE to "+0612345678",
            )
        assertEquals(
            "Cada error debe tener un caso de prueba",
            PhoneNumberError.entries.toSet(),
            examples.keys,
        )
        examples.forEach { (error, raw) -> assertInvalid(expected = error, raw = raw) }
    }

    private fun assertValid(
        expected: String,
        raw: String,
        defaultCallingCode: String? = null,
    ) {
        assertEquals(
            "«$raw» debería ser válido",
            PhoneNumberValidation.Valid(expected),
            PhoneNumberValidator.validate(raw, defaultCallingCode),
        )
    }

    private fun assertInvalid(
        expected: PhoneNumberError,
        raw: String,
    ) {
        assertEquals(
            "«$raw» debería fallar con $expected",
            PhoneNumberValidation.Invalid(expected),
            PhoneNumberValidator.validate(raw),
        )
    }
}
