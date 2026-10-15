package com.proyectonami.app.core.data.firebase

object FirebasePaths {
    fun user(userId: String): String = "users/${safeSegment("userId", userId)}"

    fun contact(
        userId: String,
        contactId: String,
    ): String = "${user(userId)}/contacts/${safeSegment("contactId", contactId)}"

    fun conversation(conversationId: String): String = "conversations/${safeSegment("conversationId", conversationId)}"

    fun message(
        conversationId: String,
        messageId: String,
    ): String = "${conversation(conversationId)}/messages/${safeSegment("messageId", messageId)}"

    fun audio(
        conversationId: String,
        messageId: String,
    ): String = "${conversation(conversationId)}/audio/${safeSegment("messageId", messageId)}/note.m4a"

    private fun safeSegment(
        label: String,
        value: String,
    ): String {
        require(value.isNotBlank()) { "$label no puede estar vacío" }
        require('/' !in value) { "$label no puede contener '/'" }
        return value
    }
}
