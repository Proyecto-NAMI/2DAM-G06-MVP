package com.proyectonami.app.core.designsystem

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.DeviceConfigurationOverride
import androidx.compose.ui.test.FontScale
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.proyectonami.app.R
import com.proyectonami.app.core.designsystem.component.NamiPrimaryButton
import com.proyectonami.app.core.designsystem.component.NamiSecondaryButton
import com.proyectonami.app.core.designsystem.component.NamiTextField
import com.proyectonami.app.core.designsystem.theme.NamiTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** T-04 / RNF-01: tamaño táctil, texto al 200 % y semántica de los componentes base. */
@RunWith(AndroidJUnit4::class)
class NamiComponentsTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val longLabel = "Enviar un mensaje de voz a toda la familia"

    private fun setThemedContent(
        fontScale: Float = 1f,
        screenWidthDp: Int = 320,
        content: @Composable () -> Unit,
    ) {
        composeRule.setContent {
            DeviceConfigurationOverride(DeviceConfigurationOverride.FontScale(fontScale)) {
                NamiTheme(darkTheme = false) {
                    Box(modifier = Modifier.width(screenWidthDp.dp)) { content() }
                }
            }
        }
    }

    @Test
    fun primaryButtonMeetsMinimumTouchTargetAndClicks() {
        var clicks = 0
        setThemedContent {
            NamiPrimaryButton(
                text = "Enviar",
                icon = painterResource(R.drawable.ic_nami_send),
                onClick = { clicks++ },
            )
        }

        composeRule
            .onNodeWithText("Enviar")
            .assertIsDisplayed()
            .assertHeightIsAtLeast(48.dp)
            .assertWidthIsAtLeast(48.dp)
            .performClick()

        assertEquals(1, clicks)
    }

    @Test
    fun secondaryButtonMeetsMinimumTouchTarget() {
        setThemedContent {
            NamiSecondaryButton(
                text = "Añadir",
                icon = painterResource(R.drawable.ic_nami_add),
                onClick = {},
            )
        }

        composeRule
            .onNodeWithText("Añadir")
            .assertIsDisplayed()
            .assertHeightIsAtLeast(48.dp)
            .assertWidthIsAtLeast(48.dp)
    }

    @Test
    fun buttonTextIsNotClippedAt200PercentOnSmallScreen() {
        assertButtonTextFits(screenWidthDp = 320)
    }

    @Test
    fun buttonTextIsNotClippedAt200PercentOnStandardScreen() {
        assertButtonTextFits(screenWidthDp = 411)
    }

    private fun assertButtonTextFits(screenWidthDp: Int) {
        setThemedContent(fontScale = 2f, screenWidthDp = screenWidthDp) {
            NamiPrimaryButton(
                text = longLabel,
                icon = painterResource(R.drawable.ic_nami_send),
                onClick = {},
                modifier = Modifier.fillMaxWidth(),
            )
        }

        val layouts = mutableListOf<TextLayoutResult>()
        composeRule
            .onNodeWithText(longLabel, useUnmergedTree = true)
            .assertIsDisplayed()
            .performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }

        assertFalse("El texto del botón se recorta al 200 %", layouts.first().hasVisualOverflow)
        composeRule.onNodeWithText(longLabel).assertHeightIsAtLeast(48.dp)
    }

    @Test
    fun textFieldExposesErrorToScreenReaders() {
        val errorMessage = "Escribe tu nombre para continuar"
        setThemedContent {
            NamiTextField(
                value = "",
                onValueChange = {},
                label = "Nombre",
                errorMessage = errorMessage,
            )
        }

        // El campo queda marcado como erróneo para el lector y el mensaje es visible.
        composeRule
            .onNode(hasSetTextAction() and SemanticsMatcher.keyIsDefined(SemanticsProperties.Error))
            .assertExists()
        composeRule
            .onNode(hasSetTextAction())
            .assertHeightIsAtLeast(48.dp)
        composeRule
            .onNodeWithText(errorMessage, useUnmergedTree = true)
            .assertIsDisplayed()
    }

    @Test
    fun textFieldStaysUsableAt200Percent() {
        setThemedContent(fontScale = 2f) {
            NamiTextField(
                value = "Ana",
                onValueChange = {},
                label = "Nombre",
                supportingText = "Como te llaman tus familiares",
            )
        }

        composeRule
            .onNode(hasSetTextAction())
            .assertIsDisplayed()
            .assertHeightIsAtLeast(48.dp)
        composeRule
            .onNodeWithText("Como te llaman tus familiares", useUnmergedTree = true)
            .assertIsDisplayed()
    }
}
