package com.proyectonami.app.core.media

import com.proyectonami.app.core.domain.tts.TextToSpeechEngine
import com.proyectonami.app.core.domain.tts.TextToSpeechFailure
import com.proyectonami.app.core.domain.tts.TextToSpeechState
import org.junit.Assert.assertEquals
import org.junit.Test

class TextToSpeechT24Test {
    @Test
    fun simulatedEngineCanStartAndStopReading() {
        val engine = FakeEngine(Result.success(Unit))
        val coordinator = TextToSpeechCoordinator(engine)
        coordinator.speak("Mensaje ficticio")
        assertEquals(TextToSpeechState.Speaking("Mensaje ficticio"), coordinator.state.value)
        coordinator.stop()
        assertEquals(TextToSpeechState.Idle, coordinator.state.value)
        assertEquals(listOf("Mensaje ficticio", "stop"), engine.events)
    }

    @Test
    fun unavailableEngineIsExplainedWithoutHidingText() {
        val coordinator = TextToSpeechCoordinator(FakeEngine(Result.failure(IllegalStateException())))
        coordinator.speak("Texto visible")
        assertEquals(TextToSpeechState.Failed(TextToSpeechFailure.ENGINE_UNAVAILABLE), coordinator.state.value)
    }

    private class FakeEngine(
        private val result: Result<Unit>,
    ) : TextToSpeechEngine {
        val events = mutableListOf<String>()

        override fun speak(
            text: String,
            languageTag: String,
        ): Result<Unit> {
            events += text
            return result
        }

        override fun stop() {
            events += "stop"
        }
    }
}
