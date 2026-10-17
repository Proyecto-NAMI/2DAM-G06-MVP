package com.proyectonami.app.core.data.firebase

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.proyectonami.app.core.domain.audio.AUDIO_NOTE_CONTENT_TYPE
import com.proyectonami.app.core.domain.audio.DEFAULT_AUDIO_LIMIT_BYTES
import com.proyectonami.app.core.domain.audio.DEFAULT_AUDIO_LIMIT_MILLIS
import com.proyectonami.app.core.domain.audio.audioNoteStoragePath
import com.proyectonami.app.core.domain.messages.AudioMessageRecord
import com.proyectonami.app.core.domain.messages.ConversationMessageRecord
import com.proyectonami.app.core.domain.messages.OutgoingTextMessage
import com.proyectonami.app.core.domain.messages.TextMessageFailure
import com.proyectonami.app.core.domain.messages.TextMessageRecord
import com.proyectonami.app.core.domain.messages.TextMessageSendResult
import com.proyectonami.app.core.domain.messages.TextMessagesRemoteState
import com.proyectonami.app.core.domain.messages.TextMessagesRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class FirebaseTextMessagesRepository(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
) : TextMessagesRepository {
    override fun observe(conversationId: String): Flow<TextMessagesRemoteState> =
        flow {
            emit(TextMessagesRemoteState.Loading)
            val userId = auth.currentUser?.uid
            if (userId == null || conversationId.isBlank() || '/' in conversationId) {
                emit(TextMessagesRemoteState.Failed)
                return@flow
            }
            emitAll(
                callbackFlow {
                    val listener =
                        firestore
                            .collection("conversations/$conversationId/messages")
                            .orderBy("createdAt")
                            .addSnapshotListener { snapshot, error ->
                                if (error != null || snapshot == null) {
                                    trySend(TextMessagesRemoteState.Failed)
                                } else {
                                    trySend(
                                        TextMessagesRemoteState.Loaded(
                                            snapshot.documents.mapNotNull { it.toConversationMessageRecord() },
                                        ),
                                    )
                                }
                            }
                    awaitClose {
                        listener.remove()
                    }
                },
            )
        }

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

private fun com.google.firebase.firestore.DocumentSnapshot.toConversationMessageRecord(): ConversationMessageRecord? {
    val clientMessageId = getString("clientMessageId") ?: return null
    val senderId = getString("senderId") ?: return null
    val createdAt = getTimestamp("createdAt") ?: return null
    val status = getString("status") ?: return null
    if (clientMessageId.isBlank() || senderId.isBlank() || status != "sent") return null
    return when (getString("type")) {
        "text" -> {
            val text = getString("text") ?: return null
            text.takeIf { it.isNotBlank() }?.let {
                TextMessageRecord(clientMessageId, senderId, it, createdAt.toDate().time, status)
            }
        }
        "audio" -> {
            val durationMillis = getLong("durationMillis") ?: return null
            val sizeBytes = getLong("sizeBytes") ?: return null
            val contentType = getString("contentType") ?: return null
            val storagePath = getString("storagePath") ?: return null
            if (
                durationMillis !in 1..DEFAULT_AUDIO_LIMIT_MILLIS ||
                sizeBytes !in 1..DEFAULT_AUDIO_LIMIT_BYTES ||
                contentType != AUDIO_NOTE_CONTENT_TYPE ||
                storagePath !=
                audioNoteStoragePath(
                    reference.parent.parent
                        ?.id
                        .orEmpty(),
                    clientMessageId,
                )
            ) {
                return null
            }
            AudioMessageRecord(
                clientMessageId = clientMessageId,
                conversationId =
                    reference.parent.parent
                        ?.id
                        .orEmpty(),
                senderId = senderId,
                durationMillis = durationMillis,
                sizeBytes = sizeBytes,
                contentType = contentType,
                storagePath = storagePath,
                createdAtEpochMillis = createdAt.toDate().time,
                status = status,
            )
        }
        else -> null
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
