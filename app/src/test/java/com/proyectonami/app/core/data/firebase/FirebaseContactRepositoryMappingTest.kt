package com.proyectonami.app.core.data.firebase

import com.proyectonami.app.feature.contacts.domain.Contact
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FirebaseContactRepositoryMappingTest {
    @Test
    fun linkedContactIsWrittenWithTheAgreedFields() {
        val contact =
            Contact(
                id = "contact_1",
                displayName = "Ana",
                surname = "Ficticia",
                phoneE164 = "+34612345678",
                linkedUserId = "user-2",
            )

        assertEquals(
            document(
                "displayName" to "Ana",
                "surname" to "Ficticia",
                "phoneE164" to "+34612345678",
                "status" to "linked",
                "linkedUserId" to "user-2",
            ),
            contactFields(contact),
        )
    }

    @Test
    fun pendingContactIsWrittenWithoutLinkedUser() {
        val fields =
            contactFields(
                Contact(
                    id = "contact_1",
                    displayName = "Ana",
                    surname = "Ficticia",
                    phoneE164 = "+34612345678",
                ),
            )

        assertEquals("pending", fields["status"])
        assertNull(fields["linkedUserId"])
    }

    @Test
    fun storedDocumentsAreReadBack() {
        assertEquals(
            Contact(
                id = "contact_1",
                displayName = "Ana",
                surname = "Ficticia",
                phoneE164 = "+34612345678",
            ),
            contactFromDocument(
                id = "contact_1",
                data =
                    document(
                        "displayName" to "Ana",
                        "surname" to "Ficticia",
                        "phoneE164" to "+34612345678",
                        "status" to "pending",
                        "linkedUserId" to null,
                    ),
            ),
        )
        assertEquals(
            Contact(
                id = "contact_1",
                displayName = "Ana",
                surname = "Ficticia",
                phoneE164 = "+34612345678",
                linkedUserId = "user-2",
            ),
            contactFromDocument(
                id = "contact_1",
                data =
                    document(
                        "displayName" to "Ana",
                        "surname" to "Ficticia",
                        "phoneE164" to "+34612345678",
                        "status" to "linked",
                        "linkedUserId" to "user-2",
                    ),
            ),
        )
    }

    @Test
    fun incoherentOrIncompleteDocumentsAreDiscarded() {
        assertNull(
            "Vinculado sin cuenta",
            contactFromDocument("contact_1", baseDocument() + document("status" to "linked")),
        )
        assertNull(
            "Pendiente con cuenta",
            contactFromDocument(
                "contact_1",
                baseDocument() + document("status" to "pending", "linkedUserId" to "user-2"),
            ),
        )
        assertNull(
            "Estado desconocido",
            contactFromDocument("contact_1", baseDocument() + document("status" to "otro")),
        )
        assertNull(
            "Faltan campos obligatorios",
            contactFromDocument("contact_1", document("status" to "pending")),
        )
        assertNull(
            "Cuenta en blanco",
            contactFromDocument(
                "contact_1",
                baseDocument() + document("status" to "linked", "linkedUserId" to "   "),
            ),
        )
    }

    private fun baseDocument(): Map<String, Any?> =
        document(
            "displayName" to "Ana",
            "surname" to "Ficticia",
            "phoneE164" to "+34612345678",
        )

    private fun document(vararg pairs: Pair<String, Any?>): Map<String, Any?> = pairs.toMap()
}
