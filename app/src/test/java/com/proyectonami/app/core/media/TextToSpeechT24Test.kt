package com.proyectonami.app.core.media

import com.proyectonami.app.core.domain.tts.TextToSpeechEngine
import com.proyectonami.app.core.domain.tts.TextToSpeechException
import com.proyectonami.app.core.domain.tts.TextToSpeechFailure
import com.proyectonami.app.core.domain.tts.TextToSpeechState
import org.junit.Assert.assertEquals
import org.junit.Test

class TextToSpeechT24Test {
    @Test
    fun simulatedEngineCanStartAndStopReading() {
        val engine = FakeEngine(Result.success(Unit))
        var interruptedAudio = 0
        val coordinator = TextToSpeechCoordinator(engine, beforeSpeak = { interruptedAudio++ })
        coordinator.speak("message-1", "Mensaje ficticio")
        assertEquals(TextToSpeechState.Speaking("message-1"), coordinator.state.value)
        assertEquals(1, interruptedAudio)
        coordinator.stop()
        assertEquals(TextToSpeechState.Idle, coordinator.state.value)
        assertEquals(listOf("stop", "Mensaje ficticio:es-ES", "stop"), engine.events)
    }

    @Test
    fun unavailableEngineIsExplainedWithoutHidingText() {
        val coordinator = TextToSpeechCoordinator(FakeEngine(Result.failure(IllegalStateException())))
        coordinator.speak("message-2", "Texto visible")
        assertEquals(
            TextToSpeechState.Failed("message-2", TextToSpeechFailure.ENGINE_UNAVAILABLE),
            coordinator.state.value,
        )
    }

    @Test
    fun languageFailureAndAsynchronousCompletionAreReported() {
        val engine = FakeEngine(Result.failure(TextToSpeechException(TextToSpeechFailure.LANGUAGE_UNAVAILABLE)))
        val coordinator = TextToSpeechCoordinator(engine)

        coordinator.speak("message-3", "Texto visible")
        assertEquals(
            TextToSpeechState.Failed("message-3", TextToSpeechFailure.LANGUAGE_UNAVAILABLE),
            coordinator.state.value,
        )

        engine.result = Result.success(Unit)
        coordinator.speak("message-3", "Texto visible")
        engine.complete()
        assertEquals(TextToSpeechState.Idle, coordinator.state.value)
    }

    @Test
    fun aNewReadingStopsThePreviousOneAndCloseReleasesTheEngine() {
        val engine = FakeEngine(Result.success(Unit))
        val coordinator = TextToSpeechCoordinator(engine)

        coordinator.speak("message-1", "Primero")
        coordinator.speak("message-2", "Segundo")
        coordinator.close()

        assertEquals(
            listOf("stop", "Primero:es-ES", "stop", "Segundo:es-ES", "stop", "close"),
            engine.events,
        )
        assertEquals(TextToSpeechState.Idle, coordinator.state.value)
    }

    private class FakeEngine(
        var result: Result<Unit>,
    ) : TextToSpeechEngine {
        val events = mutableListOf<String>()
        private var completionListener: () -> Unit = {}

        override fun setOnCompletionListener(listener: () -> Unit) {
            completionListener = listener
        }

        override fun setOnErrorListener(listener: (TextToSpeechFailure) -> Unit) = Unit

        override fun speak(
            text: String,
            languageTag: String,
        ): Result<Unit> {
            events += "$text:$languageTag"
            return result
        }

        override fun stop() {
            events += "stop"
        }

        override fun close() {
            events += "close"
        }

        fun complete() = completionListener()
    }
}
