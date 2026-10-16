package com.proyectonami.app.feature.contacts.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ContactTest {
    @Test
    fun contactWithoutAccountIsPending() {
        val contact =
            Contact(
                id = "contact_1",
                displayName = "Ana",
                surname = "Ficticia",
                phoneE164 = "+34612345678",
            )

        assertEquals(ContactStatus.PENDING, contact.status)
        assertNull(contact.linkedUserId)
    }

    @Test
    fun contactWithAccountIsLinked() {
        val contact =
            Contact(
                id = "contact_1",
                displayName = "Ana",
                surname = "Ficticia",
                phoneE164 = "+34612345678",
                linkedUserId = "user-2",
            )

        assertEquals(ContactStatus.LINKED, contact.status)
        assertEquals("user-2", contact.linkedUserId)
    }
}
