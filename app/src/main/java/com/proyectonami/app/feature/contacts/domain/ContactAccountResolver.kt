package com.proyectonami.app.feature.contacts.domain

/** Resultado de intentar localizar la cuenta NAMI de un teléfono (T-11, T-12). */
sealed interface ResolvedAccount {
    /** Existe una cuenta con ese teléfono. */
    data class Found(
        val userId: String,
    ) : ResolvedAccount

    /** La búsqueda se hizo y no hay ninguna cuenta con ese teléfono. */
    data object NotFound : ResolvedAccount

    /** No hay una búsqueda segura disponible, así que no se puede afirmar que no exista. */
    data object Unavailable : ResolvedAccount
}

/**
 * Puerto de búsqueda exacta de una cuenta por teléfono (T-11, T-12).
 *
 * T-11 cerró la consulta del directorio desde el cliente porque permite enumerar
 * personas usuarias: las reglas de Firestore solo dejan leer el perfil propio.
 * Cualquier implementación real debe ser una operación de backend autenticada,
 * con App Check y cuota por UID, que devuelva solo el identificador mínimo
 * (ver `FIREBASE.md`). Hasta que exista y esté probada, el MVP usa
 * [DisabledContactAccountResolver] y guarda los contactos como pendientes.
 */
interface ContactAccountResolver {
    suspend fun resolveAccount(phoneE164: String): ResolvedAccount
}

/**
 * Implementación del MVP: la vinculación sigue cerrada, así que nunca se consulta
 * el directorio y los contactos se conservan como pendientes.
 */
object DisabledContactAccountResolver : ContactAccountResolver {
    override suspend fun resolveAccount(phoneE164: String): ResolvedAccount = ResolvedAccount.Unavailable
}
