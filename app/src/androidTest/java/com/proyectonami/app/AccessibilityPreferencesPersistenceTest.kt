package com.proyectonami.app

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.proyectonami.app.core.data.preferences.DataStoreAccessibilityPreferencesRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AccessibilityPreferencesPersistenceTest {
    private val context = ApplicationProvider.getApplicationContext<NamiApplication>()

    @After
    fun resetPreference() {
        runBlocking {
            val repository = DataStoreAccessibilityPreferencesRepository(context)
            repository.setTextScale(1f)
            repository.setReadMessagesAloud(false)
        }
    }

    @Test
    fun accessibilityPreferencesSurviveRepositoryRecreation() =
        runBlocking {
            DataStoreAccessibilityPreferencesRepository(context).apply {
                setTextScale(2f)
                setReadMessagesAloud(true)
            }

            val recreatedRepository = DataStoreAccessibilityPreferencesRepository(context)
            val persisted = recreatedRepository.preferences.first()

            assertEquals(2f, persisted.textScale)
            assertTrue(persisted.readMessagesAloud)
        }
}
