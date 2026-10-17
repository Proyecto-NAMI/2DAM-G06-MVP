package com.proyectonami.app.feature.chat.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import com.proyectonami.app.R
import com.proyectonami.app.core.designsystem.component.NamiPrimaryButton
import com.proyectonami.app.core.designsystem.component.NamiSecondaryButton
import com.proyectonami.app.core.designsystem.component.NamiTextField
import com.proyectonami.app.core.designsystem.theme.NamiSpacing
import com.proyectonami.app.core.domain.audio.AudioRecordingResult
import com.proyectonami.app.core.domain.messages.TextMessageSendState
import com.proyectonami.app.core.domain.messages.TextMessagesRemoteState
import com.proyectonami.app.feature.audio.presentation.AudioRecordingRoute
import com.proyectonami.app.feature.audio.presentation.AudioRecordingViewModel

@Composable
fun TextConversationRoute(
    viewModel: TextConversationViewModel,
    onBack: () -> Unit,
    audioRecordingViewModel: AudioRecordingViewModel? = null,
    onAudioReadyToSend: (AudioRecordingResult) -> Unit = {},
) {
    val state by viewModel.uiState.collectAsState()
    var showAudioRecording by rememberSaveable { mutableStateOf(false) }
    var audioPreparedForSend by rememberSaveable { mutableStateOf(false) }
    Column(
        Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .imePadding()
            .padding(NamiSpacing.Large),
        verticalArrangement = Arrangement.spacedBy(NamiSpacing.Medium),
    ) {
        Text(
            stringResource(R.string.chat_title),
            modifier = Modifier.semantics { heading() },
            style = MaterialTheme.typography.headlineLarge,
        )
        when (val history = state.history) {
            TextMessagesRemoteState.Loading -> {
                CircularProgressIndicator()
                Text(stringResource(R.string.chat_history_loading), modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
            }
            TextMessagesRemoteState.Failed ->
                Text(
                    stringResource(R.string.chat_history_error),
                    color = MaterialTheme.colorScheme.error,
                    modifier =
                        Modifier.semantics {
                            liveRegion =
                                LiveRegionMode.Assertive
                        },
                )
            is TextMessagesRemoteState.Loaded ->
                if (history.messages.isEmpty()) {
                    Text(stringResource(R.string.chat_history_empty), modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(NamiSpacing.Small),
                    ) {
                        items(history.messages, key = { it.clientMessageId }) { message ->
                            Text("${message.text} (${message.status})", style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
        }
        when (state.sendState) {
            is TextMessageSendState.Pending -> {
                Row(horizontalArrangement = Arrangement.spacedBy(NamiSpacing.Small)) {
                    CircularProgressIndicator()
                    Text(
                        stringResource(R.string.chat_sending),
                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                    )
                }
            }
            is TextMessageSendState.Sent ->
                Text(
                    stringResource(R.string.chat_message_sent),
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                )
            else -> Unit
        }
        state.sendFailure?.let {
            Text(
                stringResource(R.string.chat_send_error),
                color = MaterialTheme.colorScheme.error,
                modifier =
                    Modifier.semantics {
                        liveRegion =
                            LiveRegionMode.Assertive
                    },
            )
        }
        if (audioPreparedForSend) {
            Text(
                stringResource(R.string.audio_prepared_for_send),
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(NamiSpacing.Small)) {
            NamiTextField(
                value = state.draft,
                onValueChange = viewModel::updateDraft,
                label = stringResource(R.string.chat_composer_label),
                modifier = Modifier.weight(1f),
                enabled = state.sendState !is TextMessageSendState.Pending,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
            )
            NamiPrimaryButton(
                text = stringResource(R.string.chat_send),
                icon = painterResource(R.drawable.ic_nami_send),
                onClick = viewModel::send,
                enabled = state.draft.isNotBlank() && state.sendState !is TextMessageSendState.Pending,
            )
        }
        if (audioRecordingViewModel != null) {
            NamiSecondaryButton(
                text = stringResource(R.string.audio_recording_open),
                icon = painterResource(R.drawable.ic_nami_mic),
                onClick = {
                    audioPreparedForSend = false
                    showAudioRecording = true
                },
                modifier = Modifier.fillMaxWidth(),
            )
        }
        NamiPrimaryButton(
            text = stringResource(R.string.chat_back),
            icon = painterResource(R.drawable.ic_nami_back),
            onClick = onBack,
            modifier = Modifier.fillMaxWidth(),
        )
    }
    if (showAudioRecording && audioRecordingViewModel != null) {
        AudioRecordingRoute(
            viewModel = audioRecordingViewModel,
            onSend = { result ->
                audioPreparedForSend = true
                onAudioReadyToSend(result)
            },
            onDismiss = { showAudioRecording = false },
        )
    }
}
