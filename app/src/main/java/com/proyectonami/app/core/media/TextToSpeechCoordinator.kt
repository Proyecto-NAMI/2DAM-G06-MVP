package com.proyectonami.app.core.media

import com.proyectonami.app.core.domain.tts.TextToSpeechController
import com.proyectonami.app.core.domain.tts.TextToSpeechEngine
import com.proyectonami.app.core.domain.tts.TextToSpeechFailure
import com.proyectonami.app.core.domain.tts.TextToSpeechState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class TextToSpeechCoordinator(
    private val engine: TextToSpeechEngine,
    private val languageTag: () -> String = { "es-ES" },
) : TextToSpeechController {
    private val mutableState = MutableStateFlow<TextToSpeechState>(TextToSpeechState.Idle)
    override val state: StateFlow<TextToSpeechState> = mutableState.asStateFlow()

    override fun speak(text: String) {
        if (text.isBlank()) return
        engine.speak(text, languageTag()).fold(
            onSuccess = { mutableState.value = TextToSpeechState.Speaking(text) },
            onFailure = { mutableState.value = TextToSpeechState.Failed(TextToSpeechFailure.ENGINE_UNAVAILABLE) },
        )
    }

    override fun stop() {
        engine.stop()
        mutableState.value = TextToSpeechState.Idle
    }
}
