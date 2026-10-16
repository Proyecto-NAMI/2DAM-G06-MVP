package com.proyectonami.app.feature.audio.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.proyectonami.app.core.domain.audio.AudioClock
import com.proyectonami.app.core.domain.audio.AudioLimits
import com.proyectonami.app.core.domain.audio.AudioRecorder
import com.proyectonami.app.core.domain.audio.AudioRecordingFailure
import com.proyectonami.app.core.domain.audio.AudioRecordingState
import com.proyectonami.app.core.domain.audio.ObserveRecordingDurationUseCase
import com.proyectonami.app.core.domain.audio.isEmptyOrOverLimit
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AudioRecordingViewModel(
    private val recorder: AudioRecorder,
    private val clock: AudioClock,
    private val localPath: () -> String,
    private val limits: AudioLimits = AudioLimits(),
    private val permissionPermanentlyDenied: () -> Boolean = { false },
) : ViewModel() {
    private val mutableState = MutableStateFlow<AudioRecordingState>(AudioRecordingState.Idle)
    val state: StateFlow<AudioRecordingState> = mutableState.asStateFlow()
    private var durationJob: Job? = null

    fun permissionResult(granted: Boolean) {
        if (!granted) {
            mutableState.value =
                if (permissionPermanentlyDenied()) {
                    AudioRecordingState.PermissionDeniedPermanent
                } else {
                    AudioRecordingState.PermissionDeniedTemporary
                }
        }
    }

    fun start() {
        if (mutableState.value is AudioRecordingState.Recording) return
        viewModelScope.launch {
            val path = localPath()
            recorder
                .start(path)
                .onSuccess {
                    mutableState.value = AudioRecordingState.Recording(0L)
                    durationJob?.cancel()
                    durationJob =
                        launch {
                            ObserveRecordingDurationUseCase(clock).invoke().collect { elapsed ->
                                if (elapsed >= limits.maxDurationMillis) {
                                    mutableState.value = AudioRecordingState.LimitReached
                                    recorder.cancel()
                                    durationJob?.cancel()
                                } else {
                                    mutableState.update { current ->
                                        if (current is AudioRecordingState.Recording) current.copy(elapsedMillis = elapsed) else current
                                    }
                                }
                            }
                        }
                }.onFailure { mutableState.value = AudioRecordingState.Failed(AudioRecordingFailure.RECORDER_UNAVAILABLE) }
        }
    }

    fun stop() {
        if (mutableState.value !is AudioRecordingState.Recording) return
        durationJob?.cancel()
        viewModelScope.launch {
            recorder.stop().fold(
                onSuccess = { result ->
                    mutableState.value =
                        if (result.isEmptyOrOverLimit(limits)) {
                            AudioRecordingState.Failed(AudioRecordingFailure.EMPTY_RECORDING)
                        } else {
                            AudioRecordingState.Preview(result.localPath, result.durationMillis, result.sizeBytes)
                        }
                },
                onFailure = { mutableState.value = AudioRecordingState.Failed(AudioRecordingFailure.UNKNOWN) },
            )
        }
    }

    fun interrupt() {
        durationJob?.cancel()
        viewModelScope.launch {
            recorder.cancel()
            mutableState.value = AudioRecordingState.Interrupted
        }
    }

    fun deletePreview() {
        durationJob?.cancel()
        viewModelScope.launch {
            recorder.cancel()
            mutableState.value = AudioRecordingState.Idle
        }
    }
}
