package com.proyectonami.app.feature.chat.presentation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.proyectonami.app.core.domain.messages.ObserveTextMessagesUseCase
import com.proyectonami.app.core.domain.messages.SendTextMessageUseCase
import com.proyectonami.app.core.domain.messages.TextMessageFailure
import com.proyectonami.app.core.domain.messages.TextMessageSendState
import com.proyectonami.app.core.domain.messages.TextMessagesRemoteState
import com.proyectonami.app.core.domain.messages.prepareOutgoingTextMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TextConversationUiState(
    val history: TextMessagesRemoteState = TextMessagesRemoteState.Loading,
    val draft: String = "",
    val sendState: TextMessageSendState? = null,
    val sendFailure: TextMessageFailure? = null,
    val announcement: TextConversationAnnouncement? = null,
)

enum class TextConversationAnnouncement {
    MessageSent,
}

class TextConversationViewModel(
    private val conversationId: String,
    private val senderId: () -> String?,
    private val observeTextMessages: ObserveTextMessagesUseCase,
    private val sendTextMessage: SendTextMessageUseCase,
    private val savedStateHandle: SavedStateHandle = SavedStateHandle(),
) : ViewModel() {
    private val mutableUiState =
        MutableStateFlow(
            TextConversationUiState(
                draft = savedStateHandle.get<String>(DRAFT_KEY).orEmpty(),
            ),
        )
    val uiState: StateFlow<TextConversationUiState> = mutableUiState.asStateFlow()

    init {
        viewModelScope.launch {
            observeTextMessages(conversationId).collect { history ->
                mutableUiState.update { it.copy(history = history) }
            }
        }
    }

    fun updateDraft(value: String) {
        mutableUiState.update { it.copy(draft = value, sendState = null, sendFailure = null, announcement = null) }
        savedStateHandle[DRAFT_KEY] = value
    }

    fun send() {
        val state = mutableUiState.value
        val sender = senderId()
        if (state.sendState is TextMessageSendState.Pending) return
        val retryMessage = (state.sendState as? TextMessageSendState.Failed)?.message
        val message = prepareOutgoingTextMessage(conversationId, sender, state.draft, retryMessage) ?: return
        mutableUiState.update {
            it.copy(
                draft = "",
                sendState = TextMessageSendState.Pending(message),
                sendFailure = null,
                announcement = null,
            )
        }
        savedStateHandle[DRAFT_KEY] = ""
        viewModelScope.launch {
            sendTextMessage(message).collect { sendState ->
                mutableUiState.update {
                    it.copy(
                        sendState = sendState,
                        sendFailure = (sendState as? TextMessageSendState.Failed)?.reason,
                        announcement =
                            if (sendState is TextMessageSendState.Sent) {
                                TextConversationAnnouncement.MessageSent
                            } else {
                                null
                            },
                        draft =
                            if (sendState is TextMessageSendState.Failed) {
                                sendState.message.text
                            } else {
                                it.draft
                            },
                    )
                }
                if (sendState is TextMessageSendState.Failed) {
                    savedStateHandle[DRAFT_KEY] = sendState.message.text
                }
            }
        }
    }

    companion object {
        private const val DRAFT_KEY = "text_conversation_draft"
    }
}
