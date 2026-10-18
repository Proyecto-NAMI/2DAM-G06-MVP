package com.proyectonami.app.feature.settings.presentation

import com.proyectonami.app.core.domain.accessibility.AccessibilityPreferences
import com.proyectonami.app.core.domain.accessibility.AccessibilityPreferencesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsViewModelTest {
    @Test
    fun changesBothPreferencesAndReflectsTheirPersistedValues() {
        val repository = FakeAccessibilityPreferencesRepository()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        val viewModel = SettingsViewModel(repository, scope)

        assertEquals(1f, viewModel.uiState.value.textScale)
        assertFalse(viewModel.uiState.value.readMessagesAloud)

        viewModel.selectTextScale(TextScaleOption.MAXIMUM)
        viewModel.setReadMessagesAloud(true)

        assertEquals(2f, viewModel.uiState.value.textScale)
        assertTrue(viewModel.uiState.value.readMessagesAloud)
        assertEquals(listOf(2f), repository.textScaleWrites)
        assertEquals(listOf(true), repository.readAloudWrites)

        val recreated = SettingsViewModel(repository, scope)
        assertEquals(SettingsUiState(2f, true), recreated.uiState.value)
        scope.cancel()
    }

    private class FakeAccessibilityPreferencesRepository : AccessibilityPreferencesRepository {
        private val values = MutableStateFlow(AccessibilityPreferences())
        override val preferences = values
        val textScaleWrites = mutableListOf<Float>()
        val readAloudWrites = mutableListOf<Boolean>()

        override suspend fun setTextScale(scale: Float) {
            textScaleWrites += scale
            values.value = values.value.copy(textScale = scale.coerceIn(1f, 2f))
        }

        override suspend fun setReadMessagesAloud(enabled: Boolean) {
            readAloudWrites += enabled
            values.value = values.value.copy(readMessagesAloud = enabled)
        }
    }
}
