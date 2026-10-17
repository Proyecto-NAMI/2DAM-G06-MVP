package com.proyectonami.app.core.media

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import com.proyectonami.app.core.domain.tts.TextToSpeechEngine
import com.proyectonami.app.core.domain.tts.TextToSpeechException
import com.proyectonami.app.core.domain.tts.TextToSpeechFailure
import java.util.Locale

/** Motor TTS del sistema. No registra ni persiste el texto que recibe. */
class AndroidTextToSpeechEngine(
    context: Context,
) : TextToSpeechEngine {
    private val lock = Any()
    private var lifecycle = EngineLifecycle.INITIALIZING
    private var pendingSpeech: PendingSpeech? = null

    @Volatile private var completionListener: () -> Unit = {}

    @Volatile private var errorListener: (TextToSpeechFailure) -> Unit = {}
    private val textToSpeech =
        TextToSpeech(context.applicationContext) { status -> onInitialized(status) }
            .apply {
                setOnUtteranceProgressListener(
                    object : UtteranceProgressListener() {
                        override fun onStart(utteranceId: String?) = Unit

                        override fun onDone(utteranceId: String?) {
                            completionListener()
                        }

                        @Deprecated("Android keeps this callback for legacy engines")
                        override fun onError(utteranceId: String?) {
                            errorListener(TextToSpeechFailure.UNKNOWN)
                        }

                        override fun onError(
                            utteranceId: String?,
                            errorCode: Int,
                        ) {
                            errorListener(TextToSpeechFailure.UNKNOWN)
                        }
                    },
                )
            }

    override fun setOnCompletionListener(listener: () -> Unit) {
        synchronized(lock) { completionListener = listener }
    }

    override fun setOnErrorListener(listener: (TextToSpeechFailure) -> Unit) {
        synchronized(lock) { errorListener = listener }
    }

    override fun speak(
        text: String,
        languageTag: String,
    ): Result<Unit> =
        synchronized(lock) {
            when (lifecycle) {
                EngineLifecycle.INITIALIZING -> {
                    pendingSpeech = PendingSpeech(text, languageTag)
                    Result.success(Unit)
                }
                EngineLifecycle.READY -> speakNow(text, languageTag)
                EngineLifecycle.FAILED,
                EngineLifecycle.CLOSED,
                -> Result.failure(TextToSpeechException(TextToSpeechFailure.ENGINE_UNAVAILABLE))
            }
        }

    override fun stop() {
        synchronized(lock) {
            pendingSpeech = null
            if (lifecycle == EngineLifecycle.READY) textToSpeech.stop()
        }
    }

    override fun close() {
        synchronized(lock) {
            if (lifecycle == EngineLifecycle.CLOSED) return
            pendingSpeech = null
            textToSpeech.stop()
            textToSpeech.shutdown()
            lifecycle = EngineLifecycle.CLOSED
        }
    }

    private fun onInitialized(status: Int) {
        val pending: PendingSpeech?
        val failure: TextToSpeechFailure?
        synchronized(lock) {
            if (lifecycle == EngineLifecycle.CLOSED) return
            if (status == TextToSpeech.SUCCESS) {
                lifecycle = EngineLifecycle.READY
                pending = pendingSpeech
                pendingSpeech = null
                failure = null
            } else {
                lifecycle = EngineLifecycle.FAILED
                pending = null
                pendingSpeech = null
                failure = TextToSpeechFailure.ENGINE_UNAVAILABLE
            }
        }
        failure?.let(errorListener)
        pending?.let { request ->
            speak(request.text, request.languageTag).onFailure { error ->
                errorListener((error as? TextToSpeechException)?.reason ?: TextToSpeechFailure.UNKNOWN)
            }
        }
    }

    private fun speakNow(
        text: String,
        languageTag: String,
    ): Result<Unit> =
        runCatching {
            val locale = Locale.forLanguageTag(languageTag)
            if (locale.language.isBlank()) {
                throw TextToSpeechException(TextToSpeechFailure.LANGUAGE_UNAVAILABLE)
            }
            val availability = textToSpeech.isLanguageAvailable(locale)
            if (availability == TextToSpeech.LANG_MISSING_DATA || availability == TextToSpeech.LANG_NOT_SUPPORTED) {
                throw TextToSpeechException(TextToSpeechFailure.LANGUAGE_UNAVAILABLE)
            }
            if (textToSpeech.setLanguage(locale) < TextToSpeech.LANG_AVAILABLE) {
                throw TextToSpeechException(TextToSpeechFailure.LANGUAGE_UNAVAILABLE)
            }
            if (textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, UTTERANCE_ID) == TextToSpeech.ERROR) {
                throw TextToSpeechException(TextToSpeechFailure.ENGINE_UNAVAILABLE)
            }
        }

    private data class PendingSpeech(
        val text: String,
        val languageTag: String,
    )

    private enum class EngineLifecycle { INITIALIZING, READY, FAILED, CLOSED }

    private companion object {
        const val UTTERANCE_ID = "nami-message"
    }
}
