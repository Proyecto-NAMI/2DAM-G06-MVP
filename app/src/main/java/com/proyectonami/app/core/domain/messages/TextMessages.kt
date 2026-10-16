package com.proyectonami.app.core.domain.messages

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.util.UUID

data class OutgoingTextMessage(
    val clientMessageId: String,
    val conversationId: String,
    val senderId: String,
    val text: String,
) {
    init {
        require(UUID.fromString(clientMessageId).toString() == clientMessageId) { "Invalid message UUID" }
        require(conversationId.isNotBlank() && '/' !in conversationId) { "Invalid conversation ID" }
        require(senderId.isNotBlank() && '/' !in senderId) { "Invalid sender ID" }
        require(text.isNotBlank()) { "Message text must not be blank" }
    }

    companion object {
        fun create(
            conversationId: String,
            senderId: String,
            text: String,
        ): OutgoingTextMessage =
            OutgoingTextMessage(
                clientMessageId = UUID.randomUUID().toString(),
                conversationId = conversationId,
                senderId = senderId,
                text = text,
            )
    }
}

enum class TextMessageFailure {
    UNAUTHENTICATED,
    NOT_AUTHORIZED,
    NETWORK,
    ID_CONFLICT,
    INVALID_CONVERSATION,
    UNKNOWN,
}

sealed interface TextMessageSendResult {
    data object Sent : TextMessageSendResult

    data class Failed(
        val reason: TextMessageFailure,
    ) : TextMessageSendResult
}

sealed interface TextMessageSendState {
    val message: OutgoingTextMessage

    data class Pending(
        override val message: OutgoingTextMessage,
    ) : TextMessageSendState

    data class Sent(
        override val message: OutgoingTextMessage,
    ) : TextMessageSendState

    data class Failed(
        override val message: OutgoingTextMessage,
        val reason: TextMessageFailure,
    ) : TextMessageSendState
}

interface TextMessagesRepository {
    suspend fun send(message: OutgoingTextMessage): TextMessageSendResult
}

class SendTextMessageUseCase(
    private val repository: TextMessagesRepository,
) {
    operator fun invoke(message: OutgoingTextMessage): Flow<TextMessageSendState> =
        flow {
            emit(TextMessageSendState.Pending(message))
            emit(
                when (val result = repository.send(message)) {
                    TextMessageSendResult.Sent -> TextMessageSendState.Sent(message)
                    is TextMessageSendResult.Failed -> TextMessageSendState.Failed(message, result.reason)
                },
            )
        }
}
