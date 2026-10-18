package com.proyectonami.app.feature.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.dp
import com.proyectonami.app.core.designsystem.theme.NamiTheme
import com.proyectonami.app.feature.settings.presentation.SettingsScreen
import com.proyectonami.app.feature.settings.presentation.SettingsUiState
import com.proyectonami.app.feature.settings.presentation.TextScaleOption
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class SettingsScreenTest {
    @get:Rule val composeRule = createComposeRule()

    @Test
    fun controlsApplyImmediatelyAndExposeStateAndAction() {
        var state by mutableStateOf(SettingsUiState())
        var backRequests = 0
        composeRule.setContent {
            NamiTheme(textScale = state.textScale) {
                SettingsScreen(
                    state = state,
                    onTextScaleSelected = { state = state.copy(textScale = it.scale) },
                    onReadMessagesAloudChange = { state = state.copy(readMessagesAloud = it) },
                    onBack = { backRequests++ },
                )
            }
        }

        composeRule
            .onNodeWithText("Normal · 100 %")
            .assertHasClickAction()
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Seleccionado"))
        composeRule.onNodeWithText("Máximo · 200 %").performScrollTo().performClick()
        composeRule.onNodeWithText("Tamaño actual: Máximo · 200 %").performScrollTo().assertIsDisplayed()
        composeRule
            .onNodeWithText("Máximo · 200 %")
            .performScrollTo()
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Seleccionado"))

        composeRule.onNodeWithText("Activar lectura en voz alta").performScrollTo().performClick()
        composeRule.onNodeWithText("Lectura en voz alta activada").performScrollTo().assertIsDisplayed()
        composeRule
            .onNodeWithText("Desactivar lectura en voz alta")
            .assertHasClickAction()
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Activada"))
        composeRule.onNodeWithText("Volver a conversaciones").performScrollTo().performClick()

        assertEquals(SettingsUiState(2f, true), state)
        assertEquals(1, backRequests)
    }

    @Test
    fun maximumTextRemainsUsableAt320Dp() {
        assertMaximumTextAtWidth(320)
    }

    @Test
    fun maximumTextRemainsUsableAt411Dp() {
        assertMaximumTextAtWidth(411)
    }

    private fun assertMaximumTextAtWidth(widthDp: Int) {
        var scaleSelection: TextScaleOption? = null
        var readAloudChange: Boolean? = null
        composeRule.setContent {
            Box(Modifier.size(widthDp.dp, 720.dp)) {
                NamiTheme(textScale = 2f) {
                    SettingsScreen(
                        state = SettingsUiState(textScale = 2f, readMessagesAloud = false),
                        onTextScaleSelected = { scaleSelection = it },
                        onReadMessagesAloudChange = { readAloudChange = it },
                        onBack = {},
                    )
                }
            }
        }

        composeRule.onNodeWithText("Ajustes").assertIsDisplayed()
        composeRule
            .onNodeWithText("Máximo · 200 %")
            .performScrollTo()
            .assertIsDisplayed()
            .performClick()
        composeRule
            .onNodeWithText("Activar lectura en voz alta")
            .performScrollTo()
            .assertIsDisplayed()
            .performClick()
        composeRule.onNodeWithText("Volver a conversaciones").performScrollTo().assertIsDisplayed()

        assertEquals(TextScaleOption.MAXIMUM, scaleSelection)
        assertTrue(readAloudChange == true)
    }
}
