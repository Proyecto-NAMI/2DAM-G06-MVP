package com.proyectonami.app.core.domain.tts

import kotlinx.coroutines.flow.StateFlow

sealed interface TextToSpeechState {
    data object Idle : TextToSpeechState

    data class Speaking(
        val resourceId: String,
    ) : TextToSpeechState

    data class Failed(
        val resourceId: String,
        val reason: TextToSpeechFailure,
    ) : TextToSpeechState
}

enum class TextToSpeechFailure { ENGINE_UNAVAILABLE, LANGUAGE_UNAVAILABLE, UNKNOWN }

class TextToSpeechException(
    val reason: TextToSpeechFailure,
    cause: Throwable? = null,
) : IllegalStateException(reason.name, cause)

interface TextToSpeechEngine {
    fun setOnCompletionListener(listener: () -> Unit)

    fun setOnErrorListener(listener: (TextToSpeechFailure) -> Unit)

    fun speak(
        text: String,
        languageTag: String,
    ): Result<Unit>

    fun stop()

    fun close()
}

interface TextToSpeechController {
    val state: StateFlow<TextToSpeechState>

    fun speak(
        resourceId: String,
        text: String,
    )

    fun stop()
}
