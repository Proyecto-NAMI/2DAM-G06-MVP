package com.proyectonami.app.core.domain.validation

import org.junit.Assert.assertEquals
import org.junit.Test

class PersonNameValidatorTest {
    @Test
    fun simpleValidNameIsAccepted() {
        assertValid(expected = "Ana", raw = "Ana")
    }

    @Test
    fun surroundingSpacesAndRepeatedSpacesAreNormalized() {
        assertValid(expected = "Ana María", raw = "  Ana   María  ")
        assertValid(expected = "Ana María", raw = "\nAna\tMaría\n")
    }

    @Test
    fun nonBreakingSpaceFromAnotherContactIsNormalized() {
        assertValid(expected = "Ana María", raw = "Ana\u00A0María")
    }

    @Test
    fun oneCharacterIsTheMinimumLength() {
        assertValid(expected = "O", raw = "O")
    }

    @Test
    fun maximumLengthIsAccepted() {
        assertValid(expected = "A".repeat(PersonNameValidator.MAX_LENGTH), raw = "A".repeat(PersonNameValidator.MAX_LENGTH))
    }

    @Test
    fun longerThanMaximumIsRejected() {
        assertInvalid(expected = PersonNameError.TOO_LONG, raw = "A".repeat(PersonNameValidator.MAX_LENGTH + 1))
    }

    @Test
    fun nonBmpLettersCountAsOneCharacter() {
        assertValid(expected = "𐐀".repeat(PersonNameValidator.MAX_LENGTH), raw = "𐐀".repeat(PersonNameValidator.MAX_LENGTH))
        assertInvalid(expected = PersonNameError.TOO_LONG, raw = "𐐀".repeat(PersonNameValidator.MAX_LENGTH + 1))
    }

    @Test
    fun emptyOrBlankNamesAreRejected() {
        assertInvalid(expected = PersonNameError.EMPTY, raw = "")
        assertInvalid(expected = PersonNameError.EMPTY, raw = "   ")
        assertInvalid(expected = PersonNameError.EMPTY, raw = "\u00A0")
    }

    @Test
    fun internationalNamesAreAccepted() {
        assertValid(expected = "Núñez", raw = "Núñez")
        assertValid(expected = "O'Brien", raw = "O'Brien")
        assertValid(expected = "O’Connor", raw = "O’Connor")
        assertValid(expected = "Jean-Luc", raw = "Jean-Luc")
        assertValid(expected = "María José", raw = "María José")
        assertValid(expected = "李小龙", raw = "李小龙")
        assertValid(expected = "محمد", raw = "محمد")
        assertValid(expected = "Þór", raw = "Þór")
        assertValid(expected = "Nguyễn Thị Hương", raw = "Nguyễn Thị Hương")
    }

    @Test
    fun combiningAccentIsAcceptedAndKeepsTheComposedText() {
        assertValid(expected = "Jose\u0301", raw = "Jose\u0301")
    }

    @Test
    fun digitsAndSymbolsAreRejected() {
        assertInvalid(expected = PersonNameError.INVALID_CHARACTERS, raw = "Ana2")
        assertInvalid(expected = PersonNameError.INVALID_CHARACTERS, raw = "Ana#")
        assertInvalid(expected = PersonNameError.INVALID_CHARACTERS, raw = "Ana 😀")
        assertInvalid(expected = PersonNameError.INVALID_CHARACTERS, raw = "Ana\u0007")
    }

    @Test
    fun separatorsCannotOpenOrCloseTheName() {
        assertInvalid(expected = PersonNameError.INVALID_FORMAT, raw = "-Ana")
        assertInvalid(expected = PersonNameError.INVALID_FORMAT, raw = "Ana-")
        assertInvalid(expected = PersonNameError.INVALID_FORMAT, raw = "'Ana")
        assertInvalid(expected = PersonNameError.INVALID_FORMAT, raw = "Ana.")
    }

    @Test
    fun repeatedSeparatorsAreRejected() {
        assertInvalid(expected = PersonNameError.INVALID_FORMAT, raw = "Jean--Luc")
        assertInvalid(expected = PersonNameError.INVALID_FORMAT, raw = "Ana..")
    }

    @Test
    fun everyErrorIsReachable() {
        val examples =
            mapOf(
                PersonNameError.EMPTY to "",
                PersonNameError.TOO_LONG to "A".repeat(PersonNameValidator.MAX_LENGTH + 1),
                PersonNameError.INVALID_CHARACTERS to "Ana2",
                PersonNameError.INVALID_FORMAT to "-Ana",
            )
        assertEquals(
            "Cada error debe tener un caso de prueba",
            PersonNameError.entries.toSet(),
            examples.keys,
        )
        examples.forEach { (error, raw) -> assertInvalid(expected = error, raw = raw) }
    }

    private fun assertValid(
        expected: String,
        raw: String,
    ) {
        assertEquals(
            "«$raw» debería ser válido",
            PersonNameValidation.Valid(expected),
            PersonNameValidator.validate(raw),
        )
    }

    private fun assertInvalid(
        expected: PersonNameError,
        raw: String,
    ) {
        assertEquals(
            "«$raw» debería fallar con $expected",
            PersonNameValidation.Invalid(expected),
            PersonNameValidator.validate(raw),
        )
    }
}
