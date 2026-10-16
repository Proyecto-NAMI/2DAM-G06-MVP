package com.proyectonami.app.core.domain.tts

import kotlinx.coroutines.flow.StateFlow

sealed interface TextToSpeechState {
    data object Idle : TextToSpeechState

    data class Speaking(
        val text: String,
    ) : TextToSpeechState

    data class Failed(
        val reason: TextToSpeechFailure,
    ) : TextToSpeechState
}

enum class TextToSpeechFailure { ENGINE_UNAVAILABLE, LANGUAGE_UNAVAILABLE, UNKNOWN }

interface TextToSpeechEngine {
    fun speak(
        text: String,
        languageTag: String,
    ): Result<Unit>

    fun stop()
}

interface TextToSpeechController {
    val state: StateFlow<TextToSpeechState>

    fun speak(text: String)

    fun stop()
}
