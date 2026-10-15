package com.proyectonami.app.core.data.firebase

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class FirebasePathsTest {
    @Test
    fun pathsKeepFirebaseIdentifiersInExpectedCollections() {
        assertEquals("users/user-1", FirebasePaths.user("user-1"))
        assertEquals(
            "users/user-1/contacts/contact-2",
            FirebasePaths.contact("user-1", "contact-2"),
        )
        assertEquals(
            "conversations/conversation-1/messages/message-1",
            FirebasePaths.message("conversation-1", "message-1"),
        )
        assertEquals(
            "conversations/conversation-1/audio/message-1/note.m4a",
            FirebasePaths.audio("conversation-1", "message-1"),
        )
    }

    @Test
    fun pathsRejectBlankOrNestedIdentifiers() {
        assertThrows(IllegalArgumentException::class.java) {
            FirebasePaths.user(" ")
        }
        assertThrows(IllegalArgumentException::class.java) {
            FirebasePaths.conversation("conversation/other")
        }
    }
}
