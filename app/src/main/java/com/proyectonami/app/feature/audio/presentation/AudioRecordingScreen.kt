package com.proyectonami.app.feature.audio.presentation

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import com.proyectonami.app.R
import com.proyectonami.app.core.designsystem.component.NamiPrimaryButton
import com.proyectonami.app.core.designsystem.component.NamiSecondaryButton
import com.proyectonami.app.core.designsystem.theme.NamiSpacing
import com.proyectonami.app.core.domain.audio.AudioRecordingFailure
import com.proyectonami.app.core.domain.audio.AudioRecordingResult
import com.proyectonami.app.core.domain.audio.AudioRecordingState
import com.proyectonami.app.core.domain.media.AudioPlaybackState

@Composable
fun AudioRecordingRoute(
    viewModel: AudioRecordingViewModel,
    onSend: (AudioRecordingResult) -> Unit,
    onDismiss: () -> Unit,
) {
    val state by viewModel.state.collectAsState()
    val playbackState by viewModel.playbackState.collectAsState()
    val context = LocalContext.current
    val activity = context.findActivity()
    val lifecycleOwner = activity as? LifecycleOwner
    var permissionRequestCount by rememberSaveable { mutableIntStateOf(0) }
    val permissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            val permanentlyDenied =
                !granted &&
                    permissionRequestCount > 1 &&
                    activity?.let { !ActivityCompat.shouldShowRequestPermissionRationale(it, Manifest.permission.RECORD_AUDIO) } == true
            viewModel.permissionResult(granted, permanentlyDenied)
            if (granted) viewModel.start()
        }
    val requestRecording = {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            viewModel.start()
        } else {
            permissionRequestCount += 1
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }
    val dismissSafely = {
        viewModel.onScreenExit()
        onDismiss()
    }

    LaunchedEffect(viewModel) {
        if (state == AudioRecordingState.Idle) requestRecording()
    }

    DisposableEffect(viewModel, lifecycleOwner) {
        val observer =
            LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_STOP) viewModel.onScreenExit()
            }
        lifecycleOwner?.lifecycle?.addObserver(observer)
        onDispose {
            lifecycleOwner?.lifecycle?.removeObserver(observer)
            viewModel.onScreenExit()
        }
    }

    AlertDialog(
        onDismissRequest = dismissSafely,
        title = {
            Text(
                text = stringResource(R.string.audio_recording_title),
                modifier = Modifier.semantics { heading() },
                style = MaterialTheme.typography.headlineSmall,
            )
        },
        text = {
            AudioRecordingContent(
                state = state,
                playbackState = playbackState,
                onRecord = requestRecording,
                onStop = viewModel::stop,
                onListen = viewModel::playPreview,
                onDelete = viewModel::deletePreview,
                onSend = {
                    viewModel.prepareToSend()?.let(onSend)
                    onDismiss()
                },
                onOpenSettings = { context.openApplicationSettings() },
                onContinueWithText = dismissSafely,
            )
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = dismissSafely) {
                Text(stringResource(R.string.audio_recording_close))
            }
        },
    )
}

@Composable
fun AudioRecordingContent(
    state: AudioRecordingState,
    playbackState: AudioPlaybackState,
    onRecord: () -> Unit,
    onStop: () -> Unit,
    onListen: () -> Unit,
    onDelete: () -> Unit,
    onSend: () -> Unit,
    onOpenSettings: () -> Unit,
    onContinueWithText: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(NamiSpacing.Medium),
    ) {
        Text(
            text = recordingMessage(state),
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            style = MaterialTheme.typography.bodyLarge,
            color =
                if (
                    state is AudioRecordingState.Failed ||
                    state is AudioRecordingState.PermissionDeniedTemporary ||
                    state is AudioRecordingState.PermissionDeniedPermanent ||
                    state is AudioRecordingState.Interrupted ||
                    state is AudioRecordingState.LimitReached
                ) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
        )
        when (state) {
            AudioRecordingState.Idle ->
                NamiPrimaryButton(
                    text = stringResource(R.string.audio_recording_start),
                    icon = painterResource(R.drawable.ic_nami_mic),
                    onClick = onRecord,
                    modifier = Modifier.fillMaxWidth(),
                )
            is AudioRecordingState.Recording ->
                NamiPrimaryButton(
                    text = stringResource(R.string.audio_recording_stop),
                    icon = painterResource(R.drawable.ic_nami_stop),
                    onClick = onStop,
                    modifier = Modifier.fillMaxWidth(),
                )
            is AudioRecordingState.Preview -> {
                val isPlaying =
                    playbackState is AudioPlaybackState.Playing &&
                        playbackState.resourceId == state.localPath
                NamiSecondaryButton(
                    text = stringResource(if (isPlaying) R.string.audio_preview_stop_listening else R.string.audio_preview_listen),
                    icon = painterResource(if (isPlaying) R.drawable.ic_nami_stop else R.drawable.ic_nami_play),
                    onClick = onListen,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (playbackState is AudioPlaybackState.Failed) {
                    Text(
                        text = stringResource(R.string.audio_preview_playback_failed),
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Assertive },
                    )
                }
                NamiSecondaryButton(
                    text = stringResource(R.string.audio_preview_delete),
                    icon = painterResource(R.drawable.ic_nami_delete),
                    onClick = onDelete,
                    modifier = Modifier.fillMaxWidth(),
                )
                NamiPrimaryButton(
                    text = stringResource(R.string.audio_preview_send),
                    icon = painterResource(R.drawable.ic_nami_send),
                    onClick = onSend,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            AudioRecordingState.PermissionDeniedPermanent -> {
                NamiPrimaryButton(
                    text = stringResource(R.string.audio_permission_open_settings),
                    icon = painterResource(R.drawable.ic_nami_settings),
                    onClick = onOpenSettings,
                    modifier = Modifier.fillMaxWidth(),
                )
                ContinueWithTextButton(onContinueWithText)
            }
            AudioRecordingState.PermissionDeniedTemporary,
            AudioRecordingState.Interrupted,
            AudioRecordingState.LimitReached,
            is AudioRecordingState.Failed,
            -> {
                NamiPrimaryButton(
                    text = stringResource(R.string.audio_recording_try_again),
                    icon = painterResource(R.drawable.ic_nami_mic),
                    onClick = onRecord,
                    modifier = Modifier.fillMaxWidth(),
                )
                ContinueWithTextButton(onContinueWithText)
            }
        }
    }
}

@Composable
private fun ContinueWithTextButton(onClick: () -> Unit) {
    NamiSecondaryButton(
        text = stringResource(R.string.audio_continue_with_text),
        icon = painterResource(R.drawable.ic_nami_back),
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun recordingMessage(state: AudioRecordingState): String =
    when (state) {
        AudioRecordingState.Idle -> stringResource(R.string.audio_recording_ready)
        is AudioRecordingState.Recording -> stringResource(R.string.audio_recording_in_progress, state.elapsedMillis / 1_000L)
        is AudioRecordingState.Preview -> stringResource(R.string.audio_preview_ready, state.durationMillis / 1_000L)
        AudioRecordingState.PermissionDeniedTemporary -> stringResource(R.string.audio_permission_denied_temporary)
        AudioRecordingState.PermissionDeniedPermanent -> stringResource(R.string.audio_permission_denied_permanent)
        AudioRecordingState.Interrupted -> stringResource(R.string.audio_recording_interrupted)
        AudioRecordingState.LimitReached -> stringResource(R.string.audio_recording_limit_reached)
        is AudioRecordingState.Failed ->
            stringResource(
                when (state.reason) {
                    AudioRecordingFailure.EMPTY_RECORDING -> R.string.audio_recording_empty
                    AudioRecordingFailure.OUT_OF_SPACE -> R.string.audio_recording_out_of_space
                    AudioRecordingFailure.RECORDER_UNAVAILABLE -> R.string.audio_recording_unavailable
                    AudioRecordingFailure.UNKNOWN -> R.string.audio_recording_failed
                },
            )
    }

private tailrec fun Context.findActivity(): Activity? =
    when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }

private fun Context.openApplicationSettings() {
    startActivity(
        Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.fromParts("package", packageName, null),
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
    )
}
