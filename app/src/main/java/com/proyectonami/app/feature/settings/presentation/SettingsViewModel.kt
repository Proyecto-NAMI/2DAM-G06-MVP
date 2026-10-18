package com.proyectonami.app.feature.settings.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.proyectonami.app.core.domain.accessibility.AccessibilityPreferencesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.math.abs

data class SettingsUiState(
    val textScale: Float = TextScaleOption.NORMAL.scale,
    val readMessagesAloud: Boolean = false,
)

enum class TextScaleOption(
    val scale: Float,
) {
    NORMAL(1f),
    LARGE(1.5f),
    MAXIMUM(2f),
    ;

    fun isSelected(currentScale: Float): Boolean = abs(currentScale - scale) < 0.01f
}

class SettingsViewModel(
    private val repository: AccessibilityPreferencesRepository,
    externalScope: CoroutineScope? = null,
) : ViewModel() {
    private val scope = externalScope ?: viewModelScope
    private val mutableUiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = mutableUiState.asStateFlow()

    init {
        scope.launch {
            repository.preferences.collect { preferences ->
                mutableUiState.value =
                    SettingsUiState(
                        textScale = preferences.textScale,
                        readMessagesAloud = preferences.readMessagesAloud,
                    )
            }
        }
    }

    fun selectTextScale(option: TextScaleOption) {
        if (option.isSelected(mutableUiState.value.textScale)) return
        scope.launch { repository.setTextScale(option.scale) }
    }

    fun setReadMessagesAloud(enabled: Boolean) {
        if (enabled == mutableUiState.value.readMessagesAloud) return
        scope.launch { repository.setReadMessagesAloud(enabled) }
    }
}
