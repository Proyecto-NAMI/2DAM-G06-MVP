package com.proyectonami.app.core.media

import android.media.MediaPlayer
import com.proyectonami.app.core.domain.media.AudioPlaybackFailure
import com.proyectonami.app.core.domain.media.AudioPlayer
import com.proyectonami.app.core.domain.media.AudioPlayerException
import java.io.File

/** Reproductor para archivos privados preparados por grabación o por Firebase Storage. */
class AndroidAudioPlayer : AudioPlayer {
    private val lock = Any()
    private var mediaPlayer: MediaPlayer? = null
    private var completionListener: () -> Unit = {}
    private var errorListener: (AudioPlaybackFailure) -> Unit = {}

    override fun setOnCompletionListener(listener: () -> Unit) {
        synchronized(lock) { completionListener = listener }
    }

    override fun setOnErrorListener(listener: (AudioPlaybackFailure) -> Unit) {
        synchronized(lock) { errorListener = listener }
    }

    override fun play(localPath: String) {
        synchronized(lock) {
            val file = File(localPath)
            if (!file.isFile || !file.canRead()) {
                throw AudioPlayerException(AudioPlaybackFailure.MISSING_RESOURCE)
            }
            stopLocked()
            val player = MediaPlayer()
            try {
                player.setDataSource(file.absolutePath)
                player.setOnCompletionListener {
                    synchronized(lock) { stopLocked() }
                    completionListener()
                }
                player.setOnErrorListener { _, _, _ ->
                    synchronized(lock) { stopLocked() }
                    errorListener(AudioPlaybackFailure.NOT_DECODABLE)
                    true
                }
                player.prepare()
                player.start()
                mediaPlayer = player
            } catch (error: Exception) {
                player.release()
                throw AudioPlayerException(AudioPlaybackFailure.NOT_DECODABLE, error)
            }
        }
    }

    override fun pause() {
        synchronized(lock) {
            mediaPlayer?.takeIf { it.isPlaying }?.pause()
        }
    }

    override fun resume() {
        synchronized(lock) {
            val player = mediaPlayer ?: throw AudioPlayerException(AudioPlaybackFailure.MISSING_RESOURCE)
            player.start()
        }
    }

    override fun stop() {
        synchronized(lock) { stopLocked() }
    }

    private fun stopLocked() {
        mediaPlayer?.let { player ->
            runCatching { player.stop() }
            player.release()
        }
        mediaPlayer = null
    }
}
