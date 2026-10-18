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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
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
import com.proyectonami.app.core.domain.audio.AudioUploadFailure
import com.proyectonami.app.core.domain.audio.AudioUploadState
import com.proyectonami.app.core.domain.media.AudioPlaybackFailure
import com.proyectonami.app.core.domain.media.AudioPlaybackState
import com.proyectonami.app.core.domain.messages.AudioMessageRecord
import com.proyectonami.app.core.domain.messages.ConversationMessageRecord
import com.proyectonami.app.core.domain.messages.TextMessageRecord
import com.proyectonami.app.core.domain.messages.TextMessageSendState
import com.proyectonami.app.core.domain.messages.TextMessagesRemoteState
import com.proyectonami.app.core.domain.tts.TextToSpeechFailure
import com.proyectonami.app.core.domain.tts.TextToSpeechState
import com.proyectonami.app.feature.audio.presentation.AudioRecordingRoute
import com.proyectonami.app.feature.audio.presentation.AudioRecordingViewModel

@Composable
fun TextConversationRoute(
    viewModel: TextConversationViewModel,
    onBack: () -> Unit,
    audioRecordingViewModel: AudioRecordingViewModel? = null,
    onAudioReadyToSend: (AudioRecordingResult) -> Unit = {},
    readMessagesAloud: Boolean = false,
) {
    DisposableEffect(viewModel) {
        onDispose { viewModel.onScreenExit() }
    }
    val state by viewModel.uiState.collectAsState()
    val playbackState by viewModel.playbackState.collectAsState()
    val textToSpeechState by viewModel.textToSpeechState.collectAsState()
    val audioUploadState =
        if (audioRecordingViewModel != null) {
            val uploadState by audioRecordingViewModel.uploadState.collectAsState()
            uploadState
        } else {
            null
        }
    var showAudioRecording by rememberSaveable { mutableStateOf(false) }
    val useLargeTextLayout = useLargeTextLayout()
    Column(
        Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .imePadding()
            .then(
                if (useLargeTextLayout) {
                    Modifier.verticalScroll(rememberScrollState())
                } else {
                    Modifier
                },
            ).padding(NamiSpacing.Large),
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
                } else if (useLargeTextLayout) {
                    Column(verticalArrangement = Arrangement.spacedBy(NamiSpacing.Small)) {
                        history.messages.forEach { message ->
                            ConversationMessage(
                                message = message,
                                playbackState = playbackState,
                                textToSpeechState = textToSpeechState,
                                readMessagesAloud = readMessagesAloud,
                                onToggleAudio = viewModel::toggleAudio,
                                onToggleSpeech = viewModel::toggleSpeech,
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(NamiSpacing.Small),
                    ) {
                        items(history.messages, key = { it.clientMessageId }) { message ->
                            ConversationMessage(
                                message = message,
                                playbackState = playbackState,
                                textToSpeechState = textToSpeechState,
                                readMessagesAloud = readMessagesAloud,
                                onToggleAudio = viewModel::toggleAudio,
                                onToggleSpeech = viewModel::toggleSpeech,
                            )
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
        AudioUploadStatus(
            state = audioUploadState,
            onRetry = { audioRecordingViewModel?.retryUpload() },
            onDelete = { audioRecordingViewModel?.deletePendingUpload() },
        )
        MessageComposer(
            value = state.draft,
            onValueChange = viewModel::updateDraft,
            onSend = viewModel::send,
            enabled = state.sendState !is TextMessageSendState.Pending,
        )
        if (audioRecordingViewModel != null) {
            NamiSecondaryButton(
                text = stringResource(R.string.audio_recording_open),
                icon = painterResource(R.drawable.ic_nami_mic),
                onClick = {
                    showAudioRecording = true
                },
                modifier = Modifier.fillMaxWidth(),
                enabled =
                    audioUploadState !is AudioUploadState.Pending &&
                        audioUploadState !is AudioUploadState.Progress &&
                        audioUploadState !is AudioUploadState.Failed,
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
                audioRecordingViewModel.sendPreview(result)
                onAudioReadyToSend(result)
            },
            onDismiss = { showAudioRecording = false },
        )
    }
}

@Composable
private fun MessageComposer(
    value: String,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit,
    enabled: Boolean,
) {
    val useStackedLayout = useLargeTextLayout()
    val sendEnabled = value.isNotBlank() && enabled

    if (useStackedLayout) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(NamiSpacing.Small),
        ) {
            MessageInput(
                value = value,
                onValueChange = onValueChange,
                enabled = enabled,
                modifier = Modifier.fillMaxWidth(),
            )
            SendMessageButton(
                onClick = onSend,
                enabled = sendEnabled,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    } else {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(NamiSpacing.Small),
        ) {
            MessageInput(
                value = value,
                onValueChange = onValueChange,
                enabled = enabled,
                modifier = Modifier.weight(1f),
            )
            SendMessageButton(onClick = onSend, enabled = sendEnabled)
        }
    }
}

@Composable
private fun useLargeTextLayout(): Boolean {
    val effectiveBodySize = MaterialTheme.typography.bodyLarge.fontSize.value * LocalDensity.current.fontScale
    return effectiveBodySize >= STACKED_COMPOSER_TEXT_SIZE_SP
}

@Composable
private fun MessageInput(
    value: String,
    onValueChange: (String) -> Unit,
    enabled: Boolean,
    modifier: Modifier,
) {
    NamiTextField(
        value = value,
        onValueChange = onValueChange,
        label = stringResource(R.string.chat_composer_label),
        modifier = modifier.testTag(CHAT_MESSAGE_INPUT_TAG),
        enabled = enabled,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
    )
}

@Composable
private fun SendMessageButton(
    onClick: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    NamiPrimaryButton(
        text = stringResource(R.string.chat_send),
        icon = painterResource(R.drawable.ic_nami_send),
        onClick = onClick,
        modifier = modifier.testTag(CHAT_SEND_BUTTON_TAG),
        enabled = enabled,
    )
}

private const val STACKED_COMPOSER_TEXT_SIZE_SP = 24f
private const val CHAT_MESSAGE_INPUT_TAG = "chat_message_input"
private const val CHAT_SEND_BUTTON_TAG = "chat_send_button"

@Composable
private fun ConversationMessage(
    message: ConversationMessageRecord,
    playbackState: AudioPlaybackState,
    textToSpeechState: TextToSpeechState,
    readMessagesAloud: Boolean,
    onToggleAudio: (AudioMessageRecord) -> Unit,
    onToggleSpeech: (TextMessageRecord) -> Unit,
) {
    when (message) {
        is TextMessageRecord ->
            TextMessage(
                message = message,
                textToSpeechState = textToSpeechState,
                readMessagesAloud = readMessagesAloud,
                onToggleSpeech = { onToggleSpeech(message) },
            )
        is AudioMessageRecord ->
            AudioMessage(
                message = message,
                playbackState = playbackState,
                onToggle = { onToggleAudio(message) },
            )
    }
}

@Composable
private fun TextMessage(
    message: TextMessageRecord,
    textToSpeechState: TextToSpeechState,
    readMessagesAloud: Boolean,
    onToggleSpeech: () -> Unit,
) {
    val isCurrent = textToSpeechState.resourceIdOrNull() == message.clientMessageId
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(NamiSpacing.Small),
    ) {
        Text(
            stringResource(R.string.chat_text_message, message.text),
            style = MaterialTheme.typography.bodyLarge,
        )
        if (readMessagesAloud) {
            if (isCurrent) {
                Text(
                    text = textToSpeechStatus(textToSpeechState),
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                    color =
                        if (textToSpeechState is TextToSpeechState.Failed) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                )
            }
            val isSpeaking = isCurrent && textToSpeechState is TextToSpeechState.Speaking
            val hasFailed = isCurrent && textToSpeechState is TextToSpeechState.Failed
            NamiSecondaryButton(
                text =
                    stringResource(
                        when {
                            isSpeaking -> R.string.tts_stop
                            hasFailed -> R.string.tts_retry
                            else -> R.string.tts_listen
                        },
                    ),
                icon =
                    painterResource(
                        when {
                            isSpeaking -> R.drawable.ic_nami_stop
                            hasFailed -> R.drawable.ic_nami_refresh
                            else -> R.drawable.ic_nami_play
                        },
                    ),
                onClick = onToggleSpeech,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun textToSpeechStatus(state: TextToSpeechState): String =
    when (state) {
        TextToSpeechState.Idle -> stringResource(R.string.tts_stopped)
        is TextToSpeechState.Speaking -> stringResource(R.string.tts_speaking)
        is TextToSpeechState.Failed -> stringResource(state.reason.textToSpeechMessageResource())
    }

private fun TextToSpeechFailure.textToSpeechMessageResource(): Int =
    when (this) {
        TextToSpeechFailure.ENGINE_UNAVAILABLE -> R.string.tts_engine_unavailable
        TextToSpeechFailure.LANGUAGE_UNAVAILABLE -> R.string.tts_language_unavailable
        TextToSpeechFailure.UNKNOWN -> R.string.tts_failed
    }

@Composable
private fun AudioMessage(
    message: AudioMessageRecord,
    playbackState: AudioPlaybackState,
    onToggle: () -> Unit,
) {
    val isCurrent = playbackState.resourceIdOrNull() == message.clientMessageId
    val seconds = ((message.durationMillis + 999L) / 1_000L).coerceAtLeast(1L).toInt()
    val actionText =
        when {
            isCurrent && playbackState is AudioPlaybackState.Preparing -> R.string.audio_history_preparing
            isCurrent && playbackState is AudioPlaybackState.Playing -> R.string.audio_history_pause
            isCurrent && playbackState is AudioPlaybackState.Paused -> R.string.audio_history_resume
            isCurrent && playbackState is AudioPlaybackState.Failed -> R.string.audio_history_retry
            else -> R.string.audio_history_play
        }
    val actionIcon =
        if (isCurrent && playbackState is AudioPlaybackState.Playing) {
            R.drawable.ic_nami_pause
        } else {
            R.drawable.ic_nami_play
        }
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(NamiSpacing.Small),
    ) {
        Text(
            stringResource(R.string.audio_history_message),
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            pluralStringResource(R.plurals.audio_history_duration, seconds, seconds),
            style = MaterialTheme.typography.bodyLarge,
        )
        Text(
            text = playbackStatus(playbackState, isCurrent),
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            color =
                if (isCurrent && playbackState is AudioPlaybackState.Failed) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
        )
        NamiSecondaryButton(
            text = stringResource(actionText),
            icon = painterResource(actionIcon),
            onClick = onToggle,
            modifier = Modifier.fillMaxWidth(),
            enabled = !(isCurrent && playbackState is AudioPlaybackState.Preparing),
        )
    }
}

@Composable
private fun playbackStatus(
    state: AudioPlaybackState,
    isCurrent: Boolean,
): String =
    if (!isCurrent) {
        stringResource(R.string.audio_history_stopped)
    } else {
        when (state) {
            AudioPlaybackState.Idle -> stringResource(R.string.audio_history_stopped)
            is AudioPlaybackState.Preparing -> stringResource(R.string.audio_history_preparing_status)
            is AudioPlaybackState.Playing -> stringResource(R.string.audio_history_playing)
            is AudioPlaybackState.Paused -> stringResource(R.string.audio_history_paused)
            is AudioPlaybackState.Failed -> stringResource(state.reason.playbackMessageResource())
        }
    }

private fun AudioPlaybackState.resourceIdOrNull(): String? =
    when (this) {
        AudioPlaybackState.Idle -> null
        is AudioPlaybackState.Preparing -> resourceId
        is AudioPlaybackState.Playing -> resourceId
        is AudioPlaybackState.Paused -> resourceId
        is AudioPlaybackState.Failed -> resourceId
    }

private fun TextToSpeechState.resourceIdOrNull(): String? =
    when (this) {
        TextToSpeechState.Idle -> null
        is TextToSpeechState.Speaking -> resourceId
        is TextToSpeechState.Failed -> resourceId
    }

private fun AudioPlaybackFailure.playbackMessageResource(): Int =
    when (this) {
        AudioPlaybackFailure.MISSING_RESOURCE -> R.string.audio_history_missing
        AudioPlaybackFailure.NOT_AUTHORIZED -> R.string.audio_history_not_authorized
        AudioPlaybackFailure.NETWORK -> R.string.audio_history_network
        AudioPlaybackFailure.NOT_DECODABLE -> R.string.audio_history_not_decodable
        AudioPlaybackFailure.UNKNOWN -> R.string.audio_history_failed
    }

@Composable
private fun AudioUploadStatus(
    state: AudioUploadState?,
    onRetry: () -> Unit,
    onDelete: () -> Unit,
) {
    when (state) {
        null -> Unit
        AudioUploadState.Pending -> {
            CircularProgressIndicator()
            Text(
                stringResource(R.string.audio_upload_pending),
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            )
        }
        is AudioUploadState.Progress -> {
            CircularProgressIndicator(progress = { state.fraction })
            Text(
                stringResource(R.string.audio_upload_progress, (state.fraction * 100).toInt()),
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            )
        }
        is AudioUploadState.Sent ->
            Text(
                stringResource(R.string.audio_upload_sent),
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            )
        is AudioUploadState.Failed -> {
            Text(
                text = stringResource(state.reason.messageResource()),
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Assertive },
            )
            Row(horizontalArrangement = Arrangement.spacedBy(NamiSpacing.Small)) {
                NamiPrimaryButton(
                    text = stringResource(R.string.audio_upload_retry),
                    icon = painterResource(R.drawable.ic_nami_refresh),
                    onClick = onRetry,
                    modifier = Modifier.weight(1f),
                )
                NamiSecondaryButton(
                    text = stringResource(R.string.audio_upload_delete),
                    icon = painterResource(R.drawable.ic_nami_delete),
                    onClick = onDelete,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

private fun AudioUploadFailure.messageResource(): Int =
    when (this) {
        AudioUploadFailure.UNAUTHENTICATED -> R.string.audio_upload_unauthenticated
        AudioUploadFailure.NETWORK -> R.string.audio_upload_network
        AudioUploadFailure.NOT_AUTHORIZED -> R.string.audio_upload_not_authorized
        AudioUploadFailure.INVALID_NOTE -> R.string.audio_upload_invalid
        AudioUploadFailure.ID_CONFLICT -> R.string.audio_upload_conflict
        AudioUploadFailure.UNKNOWN -> R.string.audio_upload_failed
    }
