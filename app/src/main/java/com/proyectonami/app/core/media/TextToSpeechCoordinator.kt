package com.proyectonami.app.core.media

import com.proyectonami.app.core.domain.tts.TextToSpeechController
import com.proyectonami.app.core.domain.tts.TextToSpeechEngine
import com.proyectonami.app.core.domain.tts.TextToSpeechException
import com.proyectonami.app.core.domain.tts.TextToSpeechFailure
import com.proyectonami.app.core.domain.tts.TextToSpeechState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class TextToSpeechCoordinator(
    private val engine: TextToSpeechEngine,
    private val languageTag: () -> String = { "es-ES" },
    private val beforeSpeak: () -> Unit = {},
) : TextToSpeechController {
    private val mutableState = MutableStateFlow<TextToSpeechState>(TextToSpeechState.Idle)
    override val state: StateFlow<TextToSpeechState> = mutableState.asStateFlow()

    init {
        engine.setOnCompletionListener { mutableState.value = TextToSpeechState.Idle }
        engine.setOnErrorListener { reason ->
            val resourceId = mutableState.value.resourceIdOrNull() ?: return@setOnErrorListener
            mutableState.value = TextToSpeechState.Failed(resourceId, reason)
        }
    }

    @Synchronized
    override fun speak(
        resourceId: String,
        text: String,
    ) {
        if (resourceId.isBlank() || text.isBlank()) return
        engine.stop()
        beforeSpeak()
        mutableState.value = TextToSpeechState.Speaking(resourceId)
        engine.speak(text, languageTag()).onFailure { error ->
            mutableState.value =
                TextToSpeechState.Failed(
                    resourceId = resourceId,
                    reason = (error as? TextToSpeechException)?.reason ?: TextToSpeechFailure.ENGINE_UNAVAILABLE,
                )
        }
    }

    @Synchronized
    override fun stop() {
        runCatching { engine.stop() }
        mutableState.value = TextToSpeechState.Idle
    }

    fun close() {
        stop()
        engine.close()
    }
}

private fun TextToSpeechState.resourceIdOrNull(): String? =
    when (this) {
        TextToSpeechState.Idle -> null
        is TextToSpeechState.Speaking -> resourceId
        is TextToSpeechState.Failed -> resourceId
    }
