package com.proyectonami.app.feature.contacts.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class ContactIdFactoryTest {
    @Test
    fun samePhoneAlwaysProducesTheSameIdentifier() {
        assertEquals(
            ContactIdFactory.forPhoneE164("+34612345678"),
            ContactIdFactory.forPhoneE164("+34612345678"),
        )
    }

    @Test
    fun differentPhonesProduceDifferentIdentifiers() {
        assertNotEquals(
            ContactIdFactory.forPhoneE164("+34612345678"),
            ContactIdFactory.forPhoneE164("+34612345679"),
        )
    }

    @Test
    fun identifierKeepsItsShapeAndHidesThePhone() {
        val identifier = ContactIdFactory.forPhoneE164("+15555550100")
        assertTrue("Falta el prefijo propio de NAMI: $identifier", identifier.startsWith("contact_"))
        assertEquals("contact_".length + 24, identifier.length)
        assertFalse("El identificador no debe contener el teléfono: $identifier", identifier.contains("15555550100"))
    }

    @Test
    fun identifierIsStableSoStoredContactsKeepWorking() {
        // Valor fijado a propósito: cambiarlo dejaría huérfanos los contactos ya guardados.
        assertEquals(
            "contact_0c6b33781ba1b920b2693ca9",
            ContactIdFactory.forPhoneE164("+34612345678"),
        )
    }

    @Test
    fun onlyNormalizedPhonesAreAccepted() {
        assertThrows(IllegalArgumentException::class.java) {
            ContactIdFactory.forPhoneE164("34612345678")
        }
        assertThrows(IllegalArgumentException::class.java) {
            ContactIdFactory.forPhoneE164("")
        }
    }
}
