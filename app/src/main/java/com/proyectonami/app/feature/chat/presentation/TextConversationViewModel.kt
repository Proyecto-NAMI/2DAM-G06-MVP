package com.proyectonami.app.feature.chat.presentation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.proyectonami.app.core.domain.media.AudioPlaybackController
import com.proyectonami.app.core.domain.media.AudioPlaybackFailure
import com.proyectonami.app.core.domain.media.AudioPlaybackSourceRepository
import com.proyectonami.app.core.domain.media.AudioPlaybackSourceResult
import com.proyectonami.app.core.domain.media.AudioPlaybackState
import com.proyectonami.app.core.domain.messages.AudioMessageRecord
import com.proyectonami.app.core.domain.messages.ObserveTextMessagesUseCase
import com.proyectonami.app.core.domain.messages.SendTextMessageUseCase
import com.proyectonami.app.core.domain.messages.TextMessageFailure
import com.proyectonami.app.core.domain.messages.TextMessageSendState
import com.proyectonami.app.core.domain.messages.TextMessagesRemoteState
import com.proyectonami.app.core.domain.messages.prepareOutgoingTextMessage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
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
    private val audioPlaybackController: AudioPlaybackController? = null,
    private val audioPlaybackSourceRepository: AudioPlaybackSourceRepository? = null,
) : ViewModel() {
    private val mutableUiState =
        MutableStateFlow(
            TextConversationUiState(
                draft = savedStateHandle.get<String>(DRAFT_KEY).orEmpty(),
            ),
        )
    val uiState: StateFlow<TextConversationUiState> = mutableUiState.asStateFlow()
    private val fallbackPlaybackState = MutableStateFlow<AudioPlaybackState>(AudioPlaybackState.Idle)
    val playbackState: StateFlow<AudioPlaybackState> = audioPlaybackController?.state ?: fallbackPlaybackState.asStateFlow()
    private var playbackJob: Job? = null

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

    fun toggleAudio(message: AudioMessageRecord) {
        val controller = audioPlaybackController ?: return
        val sourceRepository = audioPlaybackSourceRepository ?: return
        when (controller.state.value) {
            AudioPlaybackState.Playing(message.clientMessageId) -> {
                controller.pause()
                return
            }
            AudioPlaybackState.Paused(message.clientMessageId) -> {
                controller.resume()
                return
            }
            AudioPlaybackState.Preparing(message.clientMessageId) -> return
            else -> Unit
        }
        playbackJob?.cancel()
        controller.prepare(message.clientMessageId)
        playbackJob =
            viewModelScope.launch {
                try {
                    when (val source = sourceRepository.prepare(message)) {
                        is AudioPlaybackSourceResult.Ready ->
                            controller.play(message.clientMessageId, source.localPath)
                        is AudioPlaybackSourceResult.Failed ->
                            controller.fail(message.clientMessageId, source.reason)
                    }
                } catch (error: CancellationException) {
                    throw error
                } catch (_: Exception) {
                    controller.fail(message.clientMessageId, AudioPlaybackFailure.UNKNOWN)
                }
            }
    }

    fun onScreenExit() {
        playbackJob?.cancel()
        audioPlaybackController?.stop()
    }

    fun onSessionEnd() {
        onScreenExit()
        audioPlaybackSourceRepository?.clear()
    }

    override fun onCleared() {
        onSessionEnd()
    }

    companion object {
        private const val DRAFT_KEY = "text_conversation_draft"
    }
}
