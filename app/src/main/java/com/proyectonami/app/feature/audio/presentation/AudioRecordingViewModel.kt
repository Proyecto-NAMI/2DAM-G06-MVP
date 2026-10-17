package com.proyectonami.app.feature.audio.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.proyectonami.app.core.domain.audio.AudioClock
import com.proyectonami.app.core.domain.audio.AudioLimits
import com.proyectonami.app.core.domain.audio.AudioNoteMetadata
import com.proyectonami.app.core.domain.audio.AudioRecorder
import com.proyectonami.app.core.domain.audio.AudioRecorderException
import com.proyectonami.app.core.domain.audio.AudioRecordingFailure
import com.proyectonami.app.core.domain.audio.AudioRecordingResult
import com.proyectonami.app.core.domain.audio.AudioRecordingState
import com.proyectonami.app.core.domain.audio.AudioRecordingValidation
import com.proyectonami.app.core.domain.audio.AudioUploadState
import com.proyectonami.app.core.domain.audio.ObserveRecordingDurationUseCase
import com.proyectonami.app.core.domain.audio.UploadAudioNoteUseCase
import com.proyectonami.app.core.domain.audio.validate
import com.proyectonami.app.core.domain.media.AudioPlaybackController
import com.proyectonami.app.core.domain.media.AudioPlaybackState
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

class AudioRecordingViewModel(
    private val recorder: AudioRecorder,
    private val clock: AudioClock,
    private val localPath: () -> String,
    private val limits: AudioLimits = AudioLimits(),
    private val playbackController: AudioPlaybackController? = null,
    private val mediaRegistration: AutoCloseable? = null,
    private val conversationId: String? = null,
    private val senderId: () -> String? = { null },
    private val uploadAudioNote: UploadAudioNoteUseCase? = null,
) : ViewModel() {
    private val mutableState = MutableStateFlow<AudioRecordingState>(AudioRecordingState.Idle)
    val state: StateFlow<AudioRecordingState> = mutableState.asStateFlow()
    private val fallbackPlaybackState = MutableStateFlow<AudioPlaybackState>(AudioPlaybackState.Idle)
    val playbackState: StateFlow<AudioPlaybackState> = playbackController?.state ?: fallbackPlaybackState.asStateFlow()
    private val mutableUploadState = MutableStateFlow<AudioUploadState?>(null)
    val uploadState: StateFlow<AudioUploadState?> = mutableUploadState.asStateFlow()
    private var durationJob: Job? = null
    private var uploadJob: Job? = null
    private var pendingUpload: PendingAudioUpload? = null

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
        if (mutableUploadState.value is AudioUploadState.Pending || mutableUploadState.value is AudioUploadState.Progress) return
        viewModelScope.launch {
            if (mutableUploadState.value is AudioUploadState.Sent) mutableUploadState.value = null
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

    fun sendPreview(result: AudioRecordingResult) {
        if (uploadJob?.isActive == true) return
        val useCase = uploadAudioNote ?: return
        val conversation = conversationId ?: return
        val sender = senderId() ?: return
        val upload =
            pendingUpload?.takeIf { it.result.localPath == result.localPath }
                ?: PendingAudioUpload(
                    metadata =
                        AudioNoteMetadata(
                            clientMessageId = UUID.randomUUID().toString(),
                            conversationId = conversation,
                            senderId = sender,
                            durationMillis = result.durationMillis,
                            sizeBytes = result.sizeBytes,
                        ),
                    result = result,
                )
        pendingUpload = upload
        launchUpload(useCase, upload)
    }

    fun retryUpload() {
        if (uploadJob?.isActive == true) return
        val useCase = uploadAudioNote ?: return
        val upload = pendingUpload ?: return
        launchUpload(useCase, upload)
    }

    fun deletePendingUpload() {
        uploadJob?.cancel()
        uploadJob = null
        playbackController?.stop()
        viewModelScope.launch {
            recorder.cancel()
            pendingUpload = null
            mutableUploadState.value = null
            mutableState.value = AudioRecordingState.Idle
        }
    }

    private fun launchUpload(
        useCase: UploadAudioNoteUseCase,
        upload: PendingAudioUpload,
    ) {
        uploadJob =
            viewModelScope.launch {
                useCase(upload.metadata, upload.result.localPath).collect { state ->
                    mutableUploadState.value = state
                    if (state is AudioUploadState.Sent) {
                        recorder.cancel()
                        pendingUpload = null
                        mutableState.value = AudioRecordingState.Idle
                    }
                }
            }
    }

    fun onScreenExit() {
        when (mutableState.value) {
            is AudioRecordingState.Recording -> interrupt()
            else -> playbackController?.stop()
        }
    }

    override fun onCleared() {
        durationJob?.cancel()
        uploadJob?.cancel()
        playbackController?.stop()
        recorder.close()
        mediaRegistration?.close()
    }
}

private data class PendingAudioUpload(
    val metadata: AudioNoteMetadata,
    val result: AudioRecordingResult,
)
