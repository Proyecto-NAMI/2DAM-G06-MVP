package com.proyectonami.app.feature.contacts.domain

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Comprueba que el alta de contactos tiene mensajes comprensibles en
 * `res/values/strings.xml`, que es la parte verificable del criterio «duplicados
 * se rechazan con error comprensible» de T-12.
 *
 * La pantalla de T-13 será la que resuelva estos identificadores con
 * `stringResource`, indicando el nombre del contacto existente.
 */
class ContactMessagesTest {
    @Test
    fun duplicateMessageNamesTheExistingContact() {
        val message = stringValue("contact_add_duplicate")

        assertTrue(
            "El mensaje de duplicado debe indicar qué contacto existe: $message",
            message.contains("%1\$s"),
        )
    }

    @Test
    fun failureMessageIsNotEmpty() {
        assertTrue(stringValue("contact_add_failed").isNotBlank())
    }

    private fun stringValue(name: String): String {
        val stringsFile = stringsFile()
        val match =
            Regex("<string name=\"$name\">(.*?)</string>", RegexOption.DOT_MATCHES_ALL)
                .find(stringsFile.readText())
        return match?.groupValues?.get(1)?.trim()
            ?: throw AssertionError("Falta la cadena $name en ${stringsFile.path}")
    }

    private fun stringsFile(): File =
        listOf(
            File("src/main/res/values/strings.xml"),
            File("app/src/main/res/values/strings.xml"),
        ).firstOrNull(File::isFile)
            ?: throw AssertionError("No se encontró strings.xml desde ${File(".").absolutePath}")
}
