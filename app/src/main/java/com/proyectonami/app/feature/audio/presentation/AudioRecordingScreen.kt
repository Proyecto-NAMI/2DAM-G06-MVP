package com.proyectonami.app.feature.audio.presentation

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.proyectonami.app.core.domain.audio.AudioRecordingState

@Composable
fun AudioRecordingRoute(viewModel: AudioRecordingViewModel) {
    val state by viewModel.state.collectAsState()
    var waitingForPermission by remember { mutableStateOf(false) }
    val permissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            waitingForPermission = false
            viewModel.permissionResult(granted)
            if (granted) viewModel.start()
        }
    LaunchedEffect(waitingForPermission) {
        if (waitingForPermission) permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
    }
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Nota de voz", style = MaterialTheme.typography.headlineLarge)
        Text(recordingMessage(state), modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
        when (state) {
            AudioRecordingState.Idle,
            AudioRecordingState.PermissionDeniedTemporary,
            AudioRecordingState.PermissionDeniedPermanent,
            AudioRecordingState.Interrupted,
            is AudioRecordingState.Failed,
            AudioRecordingState.LimitReached,
            -> Button(onClick = { waitingForPermission = true }, modifier = Modifier.fillMaxWidth()) { Text("Grabar") }
            is AudioRecordingState.Recording -> Button(onClick = viewModel::stop, modifier = Modifier.fillMaxWidth()) { Text("Detener") }
            is AudioRecordingState.Preview -> {
                Button(onClick = viewModel::deletePreview, modifier = Modifier.fillMaxWidth()) { Text("Borrar nota") }
            }
        }
    }
}

private fun recordingMessage(state: AudioRecordingState): String =
    when (state) {
        AudioRecordingState.Idle -> "Listo para grabar"
        is AudioRecordingState.Recording -> "Grabando ${state.elapsedMillis / 1000}s"
        is AudioRecordingState.Preview -> "Vista previa disponible (${state.durationMillis / 1000}s)"
        AudioRecordingState.PermissionDeniedTemporary -> "Permiso de micrófono denegado temporalmente"
        AudioRecordingState.PermissionDeniedPermanent -> "Permiso de micrófono denegado permanentemente"
        AudioRecordingState.Interrupted -> "La grabación fue interrumpida"
        AudioRecordingState.LimitReached -> "Se alcanzó el límite de la nota"
        is AudioRecordingState.Failed -> "No se pudo grabar la nota"
    }
