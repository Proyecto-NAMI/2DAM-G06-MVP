package com.proyectonami.app.core.domain.validation

import com.proyectonami.app.core.domain.auth.PhoneAuthFailure
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Comprueba que cada motivo de error de T-06 tiene un mensaje comprensible en
 * `res/values/strings.xml`, siguiendo la convención
 * `validation_person_name_<motivo>` y `validation_phone_number_<motivo>`.
 *
 * Es la parte verificable del criterio «mensajes comprensibles» del issue: si se
 * añade un motivo nuevo sin texto, esta prueba falla. La pantalla de T-08 será la
 * que resuelva estos identificadores con `stringResource`.
 */
class ValidationMessagesTest {
    @Test
    fun everyPersonNameErrorHasAMessage() {
        assertEveryErrorHasAMessage("validation_person_name", PersonNameError.entries.map { it.name })
    }

    @Test
    fun everyPhoneNumberErrorHasAMessage() {
        assertEveryErrorHasAMessage("validation_phone_number", PhoneNumberError.entries.map { it.name })
    }

    @Test
    fun everyPhoneAuthFailureHasAMessage() {
        assertEveryErrorHasAMessage("auth_phone", PhoneAuthFailure.entries.map { it.name })
    }

    @Test
    fun lengthMessagesStateTheRealLimits() {
        val messages = loadMessages()
        assertTrue(
            "validation_person_name_too_long debe indicar el máximo real (${PersonNameValidator.MAX_LENGTH})",
            messages.getValue("validation_person_name_too_long").contains(PersonNameValidator.MAX_LENGTH.toString()),
        )
        assertTrue(
            "validation_phone_number_too_long debe indicar el máximo real (${PhoneNumberValidator.MAX_DIGITS})",
            messages.getValue("validation_phone_number_too_long").contains(PhoneNumberValidator.MAX_DIGITS.toString()),
        )
    }

    private fun assertEveryErrorHasAMessage(
        prefix: String,
        errorNames: List<String>,
    ) {
        val messages = loadMessages()
        errorNames.forEach { errorName ->
            val key = "${prefix}_${errorName.lowercase()}"
            val message = messages[key]
            assertNotNull("Falta la cadena $key en strings.xml", message)
            assertTrue("La cadena $key está vacía", !message.isNullOrBlank())
        }
    }

    private fun loadMessages(): Map<String, String> {
        val document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(stringsFile())
        val nodes = document.getElementsByTagName("string")
        return (0 until nodes.length).associate { index ->
            val element = nodes.item(index) as Element
            element.getAttribute("name") to element.textContent.trim()
        }
    }

    private fun stringsFile(): File {
        val candidates =
            listOf(
                File("src/main/res/values/strings.xml"),
                File("app/src/main/res/values/strings.xml"),
            )
        return candidates.firstOrNull(File::isFile)
            ?: throw AssertionError(
                "No se encontró src/main/res/values/strings.xml desde ${File(".").absolutePath}",
            )
    }
}
