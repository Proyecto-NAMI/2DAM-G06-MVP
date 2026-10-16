package com.proyectonami.app.feature.conversations.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.proyectonami.app.core.domain.conversations.ConversationsRemoteState
import com.proyectonami.app.core.domain.conversations.ObserveConversationsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ConversationsViewModel(
    observeConversations: ObserveConversationsUseCase,
) : ViewModel() {
    private val mutableState = MutableStateFlow<ConversationsRemoteState>(ConversationsRemoteState.Loading)
    val state: StateFlow<ConversationsRemoteState> = mutableState.asStateFlow()

    init {
        viewModelScope.launch {
            observeConversations().collect(mutableState::emit)
        }
    }
}
