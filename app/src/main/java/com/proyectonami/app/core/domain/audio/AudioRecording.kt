package com.proyectonami.app.core.domain.audio

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

const val DEFAULT_AUDIO_LIMIT_MILLIS: Long = 120_000L
const val DEFAULT_AUDIO_LIMIT_BYTES: Long = 5L * 1024L * 1024L

sealed interface AudioRecordingState {
    data object Idle : AudioRecordingState

    data class Recording(
        val elapsedMillis: Long,
    ) : AudioRecordingState

    data class Preview(
        val localPath: String,
        val durationMillis: Long,
        val sizeBytes: Long,
    ) : AudioRecordingState

    data object PermissionDeniedTemporary : AudioRecordingState

    data object PermissionDeniedPermanent : AudioRecordingState

    data object Interrupted : AudioRecordingState

    data object LimitReached : AudioRecordingState

    data class Failed(
        val reason: AudioRecordingFailure,
    ) : AudioRecordingState
}

enum class AudioRecordingFailure {
    EMPTY_RECORDING,
    OUT_OF_SPACE,
    RECORDER_UNAVAILABLE,
    UNKNOWN,
}

interface AudioRecorder {
    suspend fun start(localPath: String): Result<Unit>

    suspend fun stop(): Result<AudioRecordingResult>

    suspend fun cancel()
}

data class AudioRecordingResult(
    val localPath: String,
    val durationMillis: Long,
    val sizeBytes: Long,
)

interface AudioClock {
    fun elapsedMillis(): Long
}

class ObserveRecordingDurationUseCase(
    private val clock: AudioClock,
    private val intervalMillis: Long = 250L,
) {
    operator fun invoke(): Flow<Long> =
        flow {
            while (true) {
                emit(clock.elapsedMillis())
                kotlinx.coroutines.delay(intervalMillis)
            }
        }
}

data class AudioLimits(
    val maxDurationMillis: Long = DEFAULT_AUDIO_LIMIT_MILLIS,
    val maxSizeBytes: Long = DEFAULT_AUDIO_LIMIT_BYTES,
)

fun AudioRecordingResult.isEmptyOrOverLimit(limits: AudioLimits): Boolean =
    durationMillis <= 0L || sizeBytes <= 0L || durationMillis > limits.maxDurationMillis || sizeBytes > limits.maxSizeBytes
