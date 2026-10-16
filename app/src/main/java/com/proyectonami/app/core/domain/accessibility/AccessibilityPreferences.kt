package com.proyectonami.app.core.domain.accessibility

import kotlinx.coroutines.flow.Flow

data class AccessibilityPreferences(
    val textScale: Float = 1f,
    val readMessagesAloud: Boolean = false,
)

interface AccessibilityPreferencesRepository {
    val preferences: Flow<AccessibilityPreferences>

    suspend fun setTextScale(scale: Float)

    suspend fun setReadMessagesAloud(enabled: Boolean)
}
