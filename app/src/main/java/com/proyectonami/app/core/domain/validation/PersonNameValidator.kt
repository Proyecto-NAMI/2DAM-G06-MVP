package com.proyectonami.app.core.domain.validation

/**
 * Motivos por los que un nombre o unos apellidos no son válidos (T-06, RF-01).
 *
 * El dominio no contiene textos visibles: la capa de presentación traduce cada
 * motivo a un recurso de cadena llamado `validation_person_name_<motivo>`.
 */
enum class PersonNameError {
    EMPTY,
    TOO_LONG,
    INVALID_CHARACTERS,
    INVALID_FORMAT,
}

/** Resultado de validar un nombre: o el valor normalizado, o el motivo del fallo. */
sealed interface PersonNameValidation {
    /** El texto es válido y [value] es la forma normalizada que debe guardarse. */
    data class Valid(
        val value: String,
    ) : PersonNameValidation

    /** El texto no es válido por [error]. */
    data class Invalid(
        val error: PersonNameError,
    ) : PersonNameValidation
}

/**
 * Valida nombres y apellidos (T-06, RF-01) con reglas de dominio, sin dependencias
 * de Android ni de Firebase, para poder probarlas en la JVM y reutilizarlas en
 * cuenta y en contactos.
 *
 * Reglas del MVP:
 * - Se recortan los extremos y los espacios repetidos se reducen a uno solo.
 * - La longitud debe estar entre [MIN_LENGTH] y [MAX_LENGTH] caracteres Unicode
 *   completos, de modo que una letra acentuada o un emoji cuentan una sola vez.
 * - Se admiten letras de cualquier alfabeto, marcas de acento, espacios, guion,
 *   apóstrofo y punto. Cifras, emojis y otros símbolos se rechazan.
 * - El valor no puede empezar ni terminar con un separador ni repetirlo seguido.
 *
 * Los apellidos usan las mismas reglas que el nombre: la pantalla decide la
 * etiqueta y el texto, y este validador solo informa del motivo del fallo.
 */
object PersonNameValidator {
    /** Longitud mínima aceptada, ya normalizado el texto. */
    const val MIN_LENGTH = 1

    /** Longitud máxima aceptada, ya normalizado el texto. */
    const val MAX_LENGTH = 50

    private const val SPACE = ' '

    private val FORMATTING_CHARACTERS = setOf('-'.code, '\''.code, '’'.code, '.'.code)

    /**
     * Valida [raw] y devuelve el valor normalizado cuando es correcto.
     *
     * Solo se informa del primer problema encontrado, para poder mostrar un único
     * mensaje comprensible bajo el campo.
     */
    fun validate(raw: String): PersonNameValidation {
        val normalized = normalizeSpacing(raw)
        val length = normalized.codePointCount(0, normalized.length)
        return when {
            length < MIN_LENGTH -> PersonNameValidation.Invalid(PersonNameError.EMPTY)
            length > MAX_LENGTH -> PersonNameValidation.Invalid(PersonNameError.TOO_LONG)
            !hasOnlyAllowedCodePoints(normalized) ->
                PersonNameValidation.Invalid(PersonNameError.INVALID_CHARACTERS)
            !hasValidSeparators(normalized) ->
                PersonNameValidation.Invalid(PersonNameError.INVALID_FORMAT)
            else -> PersonNameValidation.Valid(normalized)
        }
    }

    /**
     * Recorta los extremos y convierte cualquier espacio Unicode (incluido el
     * espacio duro que se copia de otros contactos) en un único espacio normal.
     */
    private fun normalizeSpacing(raw: String): String {
        val normalized = StringBuilder(raw.length)
        var pendingSpace = false
        var index = 0
        while (index < raw.length) {
            val codePoint = raw.codePointAt(index)
            index += Character.charCount(codePoint)
            if (Character.isSpaceChar(codePoint) || Character.isWhitespace(codePoint)) {
                pendingSpace = normalized.isNotEmpty()
                continue
            }
            if (pendingSpace) {
                normalized.append(SPACE)
            }
            pendingSpace = false
            normalized.appendCodePoint(codePoint)
        }
        return normalized.toString()
    }

    private fun hasOnlyAllowedCodePoints(value: String): Boolean {
        var index = 0
        while (index < value.length) {
            val codePoint = value.codePointAt(index)
            index += Character.charCount(codePoint)
            if (!isAllowed(codePoint)) {
                return false
            }
        }
        return true
    }

    private fun isAllowed(codePoint: Int): Boolean =
        Character.isLetter(codePoint) ||
            isMark(codePoint) ||
            codePoint == SPACE.code ||
            codePoint in FORMATTING_CHARACTERS

    /** Las marcas cubren los acentos escritos como carácter combinante. */
    private fun isMark(codePoint: Int): Boolean =
        when (Character.getType(codePoint)) {
            Character.NON_SPACING_MARK.toInt(),
            Character.COMBINING_SPACING_MARK.toInt(),
            Character.ENCLOSING_MARK.toInt(),
            -> true

            else -> false
        }

    private fun hasValidSeparators(value: String): Boolean {
        if (value.codePointAt(0) in FORMATTING_CHARACTERS) {
            return false
        }
        if (value.codePointBefore(value.length) in FORMATTING_CHARACTERS) {
            return false
        }
        var previousWasSeparator = false
        var index = 0
        while (index < value.length) {
            val codePoint = value.codePointAt(index)
            index += Character.charCount(codePoint)
            val isSeparator = codePoint in FORMATTING_CHARACTERS
            if (isSeparator && previousWasSeparator) {
                return false
            }
            previousWasSeparator = isSeparator
        }
        return true
    }
}
