package com.proyectonami.app.feature.contacts.presentation

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Comprueba que el alta de contactos tiene mensajes comprensibles en
 * `res/values/strings.xml` (T-13, RF-03).
 *
 * La pantalla resuelve estos identificadores con `stringResource`, indicando el
 * nombre del contacto existente o el del contacto guardado.
 */
class AddContactMessagesTest {
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

    @Test
    fun unavailableLookupExplainsThatNothingWasSaved() {
        val message = stringValue("contact_add_lookup_unavailable")

        assertTrue("El aviso debe decir que no se guardó el contacto: $message", message.contains("no se ha guardado"))
    }

    @Test
    fun linkedConfirmationNamesTheContact() {
        assertTrue(stringValue("contact_add_success_linked").contains("%1\$s"))
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
