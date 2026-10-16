package com.proyectonami.app.core.data.firebase

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.proyectonami.app.core.domain.messages.OutgoingTextMessage
import com.proyectonami.app.core.domain.messages.TextMessageFailure
import com.proyectonami.app.core.domain.messages.TextMessageSendResult
import com.proyectonami.app.core.domain.messages.TextMessagesRepository
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class FirebaseTextMessagesRepository(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
) : TextMessagesRepository {
    override suspend fun send(message: OutgoingTextMessage): TextMessageSendResult {
        val userId =
            auth.currentUser?.uid
                ?: return TextMessageSendResult.Failed(TextMessageFailure.UNAUTHENTICATED)
        if (userId != message.senderId) {
            return TextMessageSendResult.Failed(TextMessageFailure.NOT_AUTHORIZED)
        }
        val conversationReference = firestore.document(FirebasePaths.conversation(message.conversationId))
        val messageReference = firestore.document(FirebasePaths.message(message.conversationId, message.clientMessageId))

        return suspendCancellableCoroutine { continuation ->
            firestore
                .runTransaction { transaction ->
                    val conversation = transaction.get(conversationReference)
                    val participantIds = conversation.get("participantIds") as? List<*>
                    if (participantIds == null ||
                        participantIds.size != 2 ||
                        participantIds.any { it !is String || it.isBlank() } ||
                        participantIds.distinct().size != 2
                    ) {
                        return@runTransaction TextMessageSendResult.Failed(TextMessageFailure.INVALID_CONVERSATION)
                    }
                    if (userId !in participantIds) {
                        return@runTransaction TextMessageSendResult.Failed(TextMessageFailure.NOT_AUTHORIZED)
                    }
                    val existing = transaction.get(messageReference)
                    if (existing.exists()) {
                        // A retry after a lost acknowledgement must not update activity or unread counts.
                        return@runTransaction if (
                            existing.getString("clientMessageId") == message.clientMessageId &&
                            existing.getString("senderId") == message.senderId &&
                            existing.getString("type") == "text" &&
                            existing.getString("text") == message.text &&
                            existing.getString("status") == "sent"
                        ) {
                            TextMessageSendResult.Sent
                        } else {
                            TextMessageSendResult.Failed(TextMessageFailure.ID_CONFLICT)
                        }
                    }

                    val recipientId = participantIds.filterIsInstance<String>().single { it != userId }
                    transaction.set(
                        messageReference,
                        mapOf(
                            "clientMessageId" to message.clientMessageId,
                            "senderId" to message.senderId,
                            "type" to "text",
                            "text" to message.text,
                            "createdAt" to FieldValue.serverTimestamp(),
                            "status" to "sent",
                        ),
                    )
                    transaction.update(
                        conversationReference,
                        FieldPath.of("lastActivityAt"),
                        FieldValue.serverTimestamp(),
                        FieldPath.of("unreadCountByUser", recipientId),
                        FieldValue.increment(1),
                    )
                    TextMessageSendResult.Sent
                }.addOnCompleteListener { task ->
                    if (continuation.isActive) {
                        continuation.resume(
                            if (task.isSuccessful) {
                                task.result
                            } else {
                                TextMessageSendResult.Failed(task.exception.toTextMessageFailure())
                            },
                        )
                    }
                }
        }
    }
}

private fun Exception?.toTextMessageFailure(): TextMessageFailure =
    when ((this as? FirebaseFirestoreException)?.code) {
        FirebaseFirestoreException.Code.UNAUTHENTICATED -> TextMessageFailure.UNAUTHENTICATED
        FirebaseFirestoreException.Code.PERMISSION_DENIED -> TextMessageFailure.NOT_AUTHORIZED
        FirebaseFirestoreException.Code.UNAVAILABLE,
        FirebaseFirestoreException.Code.DEADLINE_EXCEEDED,
        -> TextMessageFailure.NETWORK
        else -> TextMessageFailure.UNKNOWN
    }
