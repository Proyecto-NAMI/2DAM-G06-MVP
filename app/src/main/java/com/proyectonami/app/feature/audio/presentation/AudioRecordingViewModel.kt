package com.proyectonami.app.feature.audio.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.proyectonami.app.core.domain.audio.AudioClock
import com.proyectonami.app.core.domain.audio.AudioLimits
import com.proyectonami.app.core.domain.audio.AudioRecorder
import com.proyectonami.app.core.domain.audio.AudioRecorderException
import com.proyectonami.app.core.domain.audio.AudioRecordingFailure
import com.proyectonami.app.core.domain.audio.AudioRecordingResult
import com.proyectonami.app.core.domain.audio.AudioRecordingState
import com.proyectonami.app.core.domain.audio.AudioRecordingValidation
import com.proyectonami.app.core.domain.audio.ObserveRecordingDurationUseCase
import com.proyectonami.app.core.domain.audio.validate
import com.proyectonami.app.core.domain.media.AudioPlaybackController
import com.proyectonami.app.core.domain.media.AudioPlaybackState
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
    private val playbackController: AudioPlaybackController? = null,
    private val mediaRegistration: AutoCloseable? = null,
) : ViewModel() {
    private val mutableState = MutableStateFlow<AudioRecordingState>(AudioRecordingState.Idle)
    val state: StateFlow<AudioRecordingState> = mutableState.asStateFlow()
    private val fallbackPlaybackState = MutableStateFlow<AudioPlaybackState>(AudioPlaybackState.Idle)
    val playbackState: StateFlow<AudioPlaybackState> = playbackController?.state ?: fallbackPlaybackState.asStateFlow()
    private var durationJob: Job? = null

    fun permissionResult(
        granted: Boolean,
        permanentlyDenied: Boolean = false,
    ) {
        if (!granted) {
            mutableState.value =
                if (permanentlyDenied) {
                    AudioRecordingState.PermissionDeniedPermanent
                } else {
                    AudioRecordingState.PermissionDeniedTemporary
                }
        }
    }

    fun start() {
        if (mutableState.value is AudioRecordingState.Recording) return
        viewModelScope.launch {
            playbackController?.stop()
            val path = localPath()
            recorder
                .start(path)
                .onSuccess {
                    mutableState.value = AudioRecordingState.Recording(0L)
                    durationJob?.cancel()
                    durationJob =
                        launch {
                            ObserveRecordingDurationUseCase(clock).invoke().collect { elapsed ->
                                if (elapsed >= limits.maxDurationMillis || recorder.currentSizeBytes() >= limits.maxSizeBytes) {
                                    recorder.cancel()
                                    mutableState.value = AudioRecordingState.LimitReached
                                    durationJob?.cancel()
                                } else {
                                    mutableState.update { current ->
                                        if (current is AudioRecordingState.Recording) current.copy(elapsedMillis = elapsed) else current
                                    }
                                }
                            }
                        }
                }.onFailure { error ->
                    mutableState.value =
                        AudioRecordingState.Failed(
                            (error as? AudioRecorderException)?.reason ?: AudioRecordingFailure.RECORDER_UNAVAILABLE,
                        )
                }
        }
    }

    fun stop() {
        if (mutableState.value !is AudioRecordingState.Recording) return
        durationJob?.cancel()
        viewModelScope.launch {
            recorder.stop().fold(
                onSuccess = { result ->
                    mutableState.value =
                        when (result.validate(limits)) {
                            AudioRecordingValidation.Valid ->
                                AudioRecordingState.Preview(result.localPath, result.durationMillis, result.sizeBytes)
                            AudioRecordingValidation.Empty -> {
                                recorder.cancel()
                                AudioRecordingState.Failed(AudioRecordingFailure.EMPTY_RECORDING)
                            }
                            AudioRecordingValidation.OverLimit -> {
                                recorder.cancel()
                                AudioRecordingState.LimitReached
                            }
                        }
                },
                onFailure = { error ->
                    mutableState.value =
                        AudioRecordingState.Failed(
                            (error as? AudioRecorderException)?.reason ?: AudioRecordingFailure.UNKNOWN,
                        )
                },
            )
        }
    }

    fun interrupt() {
        durationJob?.cancel()
        playbackController?.stop()
        viewModelScope.launch {
            recorder.cancel()
            mutableState.value = AudioRecordingState.Interrupted
        }
    }

    fun deletePreview() {
        durationJob?.cancel()
        playbackController?.stop()
        viewModelScope.launch {
            recorder.cancel()
            mutableState.value = AudioRecordingState.Idle
        }
    }

    fun playPreview() {
        val preview = mutableState.value as? AudioRecordingState.Preview ?: return
        when (val playback = playbackState.value) {
            is AudioPlaybackState.Playing ->
                if (playback.resourceId == preview.localPath) playbackController?.stop() else playbackController?.play(preview.localPath)
            else -> playbackController?.play(preview.localPath)
        }
    }

    fun prepareToSend(): AudioRecordingResult? {
        val preview = mutableState.value as? AudioRecordingState.Preview ?: return null
        playbackController?.stop()
        return AudioRecordingResult(preview.localPath, preview.durationMillis, preview.sizeBytes)
    }

    fun onScreenExit() {
        when (mutableState.value) {
            is AudioRecordingState.Recording -> interrupt()
            else -> playbackController?.stop()
        }
    }

    override fun onCleared() {
        durationJob?.cancel()
        playbackController?.stop()
        recorder.close()
        mediaRegistration?.close()
    }
}
