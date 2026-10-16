package com.proyectonami.app.core.domain.accessibility

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class AccessibilityPreferencesT25Test {
    @Test
    fun preferenceChangesAreObservableAndTextScaleIsBounded() =
        runBlocking {
            val repository = FakeRepository()
            repository.setTextScale(4f)
            repository.setReadMessagesAloud(true)
            assertEquals(AccessibilityPreferences(2f, true), repository.preferences.first())
            repository.setTextScale(0.5f)
            assertEquals(1f, repository.preferences.first().textScale)
        }

    private class FakeRepository : AccessibilityPreferencesRepository {
        private val values = MutableStateFlow(AccessibilityPreferences())
        override val preferences = values

        override suspend fun setTextScale(scale: Float) {
            values.value = values.value.copy(textScale = scale.coerceIn(1f, 2f))
        }

        override suspend fun setReadMessagesAloud(enabled: Boolean) {
            values.value = values.value.copy(readMessagesAloud = enabled)
        }
    }
}
