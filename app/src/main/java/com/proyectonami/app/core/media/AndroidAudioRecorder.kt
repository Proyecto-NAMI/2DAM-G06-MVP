package com.proyectonami.app.core.media

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import android.os.SystemClock
import com.proyectonami.app.core.domain.audio.AudioClock
import com.proyectonami.app.core.domain.audio.AudioRecorder
import com.proyectonami.app.core.domain.audio.AudioRecorderException
import com.proyectonami.app.core.domain.audio.AudioRecordingFailure
import com.proyectonami.app.core.domain.audio.AudioRecordingResult
import com.proyectonami.app.core.domain.audio.DEFAULT_AUDIO_LIMIT_BYTES
import com.proyectonami.app.core.domain.audio.DEFAULT_AUDIO_LIMIT_MILLIS
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

/** Grabador productivo que solo acepta archivos dentro de la caché privada de NAMI. */
class AndroidAudioRecorder(
    context: Context,
    private val nowMillis: () -> Long = SystemClock::elapsedRealtime,
) : AudioRecorder,
    AudioClock {
    private val applicationContext = context.applicationContext
    private val cacheDirectory = applicationContext.cacheDir.canonicalFile
    private val lock = Any()
    private var mediaRecorder: MediaRecorder? = null
    private var outputFile: File? = null
    private var startedAtMillis: Long? = null

    override suspend fun start(localPath: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                synchronized(lock) {
                    releaseLocked(deleteFile = true)
                    val file = File(localPath).canonicalFile
                    require(file.parentFile?.canonicalFile == cacheDirectory) {
                        "Audio output must stay in the private cache directory"
                    }
                    if (file.exists() && !file.delete()) {
                        throw IOException("Unable to replace temporary audio")
                    }
                    if (!file.createNewFile()) {
                        throw IOException("Unable to create temporary audio")
                    }
                    val recorder = createMediaRecorder()
                    try {
                        recorder.setAudioSource(MediaRecorder.AudioSource.MIC)
                        recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                        recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                        recorder.setAudioSamplingRate(AUDIO_SAMPLE_RATE_HZ)
                        recorder.setAudioEncodingBitRate(AUDIO_BIT_RATE_BPS)
                        recorder.setMaxDuration(DEFAULT_AUDIO_LIMIT_MILLIS.toInt())
                        recorder.setMaxFileSize(DEFAULT_AUDIO_LIMIT_BYTES)
                        recorder.setOutputFile(file.absolutePath)
                        recorder.prepare()
                        recorder.start()
                    } catch (error: Exception) {
                        runCatching { recorder.reset() }
                        recorder.release()
                        file.delete()
                        throw AudioRecorderException(
                            if (error is IOException) AudioRecordingFailure.OUT_OF_SPACE else AudioRecordingFailure.RECORDER_UNAVAILABLE,
                            error,
                        )
                    }
                    mediaRecorder = recorder
                    outputFile = file
                    startedAtMillis = nowMillis()
                }
            }
        }

    override suspend fun stop(): Result<AudioRecordingResult> =
        withContext(Dispatchers.IO) {
            runCatching {
                synchronized(lock) {
                    val recorder = mediaRecorder ?: throw AudioRecorderException(AudioRecordingFailure.RECORDER_UNAVAILABLE)
                    val file = outputFile ?: throw AudioRecorderException(AudioRecordingFailure.RECORDER_UNAVAILABLE)
                    val duration = elapsedMillis()
                    try {
                        recorder.stop()
                    } catch (error: RuntimeException) {
                        releaseLocked(deleteFile = true)
                        throw AudioRecorderException(AudioRecordingFailure.EMPTY_RECORDING, error)
                    }
                    recorder.reset()
                    recorder.release()
                    mediaRecorder = null
                    startedAtMillis = null
                    AudioRecordingResult(
                        localPath = file.absolutePath,
                        durationMillis = duration,
                        sizeBytes = file.length(),
                    )
                }
            }
        }

    override suspend fun cancel() {
        withContext(Dispatchers.IO) {
            synchronized(lock) { releaseLocked(deleteFile = true) }
        }
    }

    override fun currentSizeBytes(): Long = synchronized(lock) { outputFile?.length() ?: 0L }

    override fun elapsedMillis(): Long =
        synchronized(lock) {
            startedAtMillis?.let { started -> (nowMillis() - started).coerceAtLeast(0L) } ?: 0L
        }

    override fun close() {
        synchronized(lock) { releaseLocked(deleteFile = true) }
    }

    private fun releaseLocked(deleteFile: Boolean) {
        mediaRecorder?.let { recorder ->
            runCatching { recorder.stop() }
            runCatching { recorder.reset() }
            recorder.release()
        }
        mediaRecorder = null
        startedAtMillis = null
        if (deleteFile) outputFile?.delete()
        outputFile = null
    }

    @Suppress("DEPRECATION")
    private fun createMediaRecorder(): MediaRecorder =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(applicationContext)
        } else {
            MediaRecorder()
        }

    private companion object {
        const val AUDIO_SAMPLE_RATE_HZ = 22_050
        const val AUDIO_BIT_RATE_BPS = 64_000
    }
}
