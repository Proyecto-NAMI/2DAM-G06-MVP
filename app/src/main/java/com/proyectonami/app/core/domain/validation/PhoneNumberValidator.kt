package com.proyectonami.app.core.domain.validation

/**
 * Motivos por los que un teléfono no es válido (T-06, RF-01).
 *
 * El dominio no contiene textos visibles: la capa de presentación traduce cada
 * motivo a un recurso de cadena llamado `validation_phone_number_<motivo>`.
 */
enum class PhoneNumberError {
    EMPTY,
    INVALID_CHARACTERS,
    MISSING_COUNTRY_CODE,
    TOO_SHORT,
    TOO_LONG,
    INVALID_COUNTRY_CODE,
}

/** Resultado de validar un teléfono: o el número E.164, o el motivo del fallo. */
sealed interface PhoneNumberValidation {
    /** El teléfono es válido y [e164] es la forma normalizada que debe guardarse. */
    data class Valid(
        val e164: String,
    ) : PhoneNumberValidation

    /** El teléfono no es válido por [error]. */
    data class Invalid(
        val error: PhoneNumberError,
    ) : PhoneNumberValidation
}

/**
 * Valida y normaliza teléfonos a formato internacional E.164 (T-06, RF-01), sin
 * dependencias de Android ni de Firebase.
 *
 * Reglas del MVP:
 * - Se admiten cifras de cualquier alfabeto, espacios, `+ - ( ) .`; se descarta el
 *   resto (letras, almohadilla, asterisco...). Las cifras se convierten a ASCII.
 * - El prefijo internacional `+` solo puede aparecer al principio. Si no aparece y
 *   se indica [defaultCallingCode], se antepone ese prefijo de país.
 * - El total de cifras, prefijo incluido, debe estar entre [MIN_DIGITS] y
 *   [MAX_DIGITS] y empezar por 1-9, como exige E.164.
 *
 * Limitación conocida: no se comprueba que el prefijo exista ni la longitud exacta
 * de cada país, porque eso requiere una tabla internacional que no está en el MVP.
 */
object PhoneNumberValidator {
    /** Cifras mínimas aceptadas, prefijo del país incluido. */
    const val MIN_DIGITS = 7

    /** Cifras máximas aceptadas, prefijo del país incluido. Límite de E.164. */
    const val MAX_DIGITS = 15

    private const val PLUS = '+'
    private const val E164_PREFIX = "+"
    private val FORMATTING_CHARACTERS = setOf('-'.code, '('.code, ')'.code, '.'.code)

    /**
     * Valida [raw] y devuelve el teléfono en E.164 cuando es correcto.
     *
     * @param defaultCallingCode prefijo de país que se antepone cuando la persona
     *   escribe el número sin `+` (por ejemplo `"34"`). Si es `null`, el número
     *   debe incluir el prefijo y, si no lo incluye, se informa con
     *   [PhoneNumberError.MISSING_COUNTRY_CODE].
     * @throws IllegalArgumentException si [defaultCallingCode] no tiene entre 1 y 3
     *   cifras o empieza por 0. Es un error de programación, no de la persona usuaria.
     */
    fun validate(
        raw: String,
        defaultCallingCode: String? = null,
    ): PhoneNumberValidation {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) {
            return PhoneNumberValidation.Invalid(PhoneNumberError.EMPTY)
        }

        val startsWithPlus = trimmed.startsWith(PLUS)
        val digits =
            extractDigits(trimmed)
                ?: return PhoneNumberValidation.Invalid(PhoneNumberError.INVALID_CHARACTERS)

        val internationalDigits =
            when {
                startsWithPlus -> digits
                defaultCallingCode != null -> normalizeCallingCode(defaultCallingCode) + digits
                else -> return PhoneNumberValidation.Invalid(PhoneNumberError.MISSING_COUNTRY_CODE)
            }

        return when {
            internationalDigits.length < MIN_DIGITS ->
                PhoneNumberValidation.Invalid(PhoneNumberError.TOO_SHORT)
            internationalDigits.length > MAX_DIGITS ->
                PhoneNumberValidation.Invalid(PhoneNumberError.TOO_LONG)
            internationalDigits.startsWith('0') ->
                PhoneNumberValidation.Invalid(PhoneNumberError.INVALID_COUNTRY_CODE)
            else -> PhoneNumberValidation.Valid(E164_PREFIX + internationalDigits)
        }
    }

    /** Devuelve solo las cifras ASCII, o `null` si hay un carácter no admitido. */
    private fun extractDigits(value: String): String? {
        val digits = StringBuilder(value.length)
        var index = 0
        while (index < value.length) {
            val codePoint = value.codePointAt(index)
            index += Character.charCount(codePoint)
            when {
                codePoint == PLUS.code -> if (index != 1) return null
                Character.isDigit(codePoint) -> digits.append(Character.digit(codePoint, 10))
                Character.isSpaceChar(codePoint) || Character.isWhitespace(codePoint) -> Unit
                codePoint in FORMATTING_CHARACTERS -> Unit
                else -> return null
            }
        }
        return digits.toString()
    }

    private fun normalizeCallingCode(callingCode: String): String {
        val digits = callingCode.removePrefix(E164_PREFIX).filter { it.isDigit() }
        require(digits.isNotEmpty() && digits.length <= 3 && !digits.startsWith('0')) {
            "defaultCallingCode debe tener entre 1 y 3 cifras y no empezar por 0: $callingCode"
        }
        return digits
    }
}
