package com.proyectonami.app.core.media

import android.media.MediaPlayer
import com.proyectonami.app.core.domain.media.AudioPlayer
import java.io.File

/** Reproductor local usado para escuchar una nota antes de enviarla. */
class AndroidAudioPlayer : AudioPlayer {
    private val lock = Any()
    private var mediaPlayer: MediaPlayer? = null
    private var completionListener: () -> Unit = {}

    override fun setOnCompletionListener(listener: () -> Unit) {
        synchronized(lock) { completionListener = listener }
    }

    override fun play(resourceId: String) {
        synchronized(lock) {
            val file = File(resourceId)
            require(file.isFile && file.canRead()) { "Audio preview is unavailable" }
            stopLocked()
            val player = MediaPlayer()
            try {
                player.setDataSource(file.absolutePath)
                player.setOnCompletionListener {
                    synchronized(lock) { stopLocked() }
                    completionListener()
                }
                player.prepare()
                player.start()
                mediaPlayer = player
            } catch (error: Exception) {
                player.release()
                throw error
            }
        }
    }

    override fun pause() {
        synchronized(lock) {
            mediaPlayer?.takeIf { it.isPlaying }?.pause()
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
