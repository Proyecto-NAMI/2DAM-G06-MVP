package com.proyectonami.app.feature.contacts.domain

import java.security.MessageDigest

/**
 * Identificador estable de un contacto a partir de su teléfono normalizado (T-12).
 *
 * Permite que el alta sea idempotente: el mismo teléfono produce siempre el mismo
 * documento, de modo que repetir el alta no crea duplicados. Se guarda un resumen
 * SHA-256 truncado en lugar del teléfono para no escribir datos personales en la
 * ruta del documento, que Firestore muestra en métricas y consolas.
 */
object ContactIdFactory {
    private const val ID_PREFIX = "contact_"
    private const val HASH_HEX_LENGTH = 24
    private const val SALT = "nami:contact:"
    private const val HEX_DIGITS = "0123456789abcdef"

    /**
     * Devuelve el identificador de [phoneE164], que debe estar ya normalizado.
     *
     * @throws IllegalArgumentException si el teléfono no está en formato E.164.
     */
    fun forPhoneE164(phoneE164: String): String {
        require(phoneE164.startsWith("+")) {
            "Se espera un teléfono normalizado en E.164, por ejemplo +34612345678"
        }
        val digest = MessageDigest.getInstance("SHA-256").digest("$SALT$phoneE164".toByteArray(Charsets.UTF_8))
        return ID_PREFIX + digest.toHex().take(HASH_HEX_LENGTH)
    }

    private fun ByteArray.toHex(): String =
        buildString(size * 2) {
            for (byte in this@toHex) {
                val value = byte.toInt() and 0xFF
                append(HEX_DIGITS[value ushr 4])
                append(HEX_DIGITS[value and 0x0F])
            }
        }
}
