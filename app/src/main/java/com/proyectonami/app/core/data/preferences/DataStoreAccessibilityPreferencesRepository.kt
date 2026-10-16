package com.proyectonami.app.core.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.proyectonami.app.core.domain.accessibility.AccessibilityPreferences
import com.proyectonami.app.core.domain.accessibility.AccessibilityPreferencesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.accessibilityDataStore by preferencesDataStore(name = "accessibility_preferences")

class DataStoreAccessibilityPreferencesRepository(
    private val context: Context,
) : AccessibilityPreferencesRepository {
    override val preferences: Flow<AccessibilityPreferences> =
        context.accessibilityDataStore.data.map { values ->
            AccessibilityPreferences(
                textScale = (values[TEXT_SCALE] ?: 1f).coerceIn(1f, 2f),
                readMessagesAloud = values[READ_ALOUD] ?: false,
            )
        }

    override suspend fun setTextScale(scale: Float) {
        context.accessibilityDataStore.edit { values -> values[TEXT_SCALE] = scale.coerceIn(1f, 2f) }
    }

    override suspend fun setReadMessagesAloud(enabled: Boolean) {
        context.accessibilityDataStore.edit { values -> values[READ_ALOUD] = enabled }
    }

    private companion object {
        val TEXT_SCALE = floatPreferencesKey("text_scale")
        val READ_ALOUD = booleanPreferencesKey("read_messages_aloud")
    }
}
