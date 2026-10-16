package com.proyectonami.app.feature.contacts.domain

/** Resultado de intentar guardar un contacto en el almacenamiento remoto. */
sealed interface ContactCreateResult {
    /** El contacto se guardó porque no existía. */
    data object Created : ContactCreateResult

    /** Ya existía un contacto con el mismo identificador; se devuelve el guardado. */
    data class AlreadyExists(
        val contact: Contact,
    ) : ContactCreateResult

    /** Fallo recuperable (red o permisos); se puede reintentar sin duplicar. */
    data object Failed : ContactCreateResult
}

/**
 * Acceso a los contactos de confianza (T-12, RNF-05).
 *
 * La implementación remota trabaja siempre dentro de `users/{userId}/contacts`,
 * colección que las reglas de Firestore limitan a la persona propietaria. El
 * cliente no es la frontera de seguridad: la autorización se comprueba en las
 * reglas.
 */
interface ContactRepository {
    /**
     * Guarda [contact] para [userId] solo si ese contacto no existía.
     *
     * El identificador deriva del teléfono normalizado ([ContactIdFactory]), así
     * que repetir el alta del mismo teléfono apunta siempre al mismo documento y
     * no crea duplicados, ni siquiera si dos intentos se solapan.
     */
    suspend fun createContact(
        userId: String,
        contact: Contact,
    ): ContactCreateResult
}
