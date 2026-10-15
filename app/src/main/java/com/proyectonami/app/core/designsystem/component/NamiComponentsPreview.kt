package com.proyectonami.app.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.proyectonami.app.R
import com.proyectonami.app.core.designsystem.theme.NamiSpacing
import com.proyectonami.app.core.designsystem.theme.NamiTheme

/*
 * Vistas previas para revisar T-04 en dos tamaños de pantalla (teléfono
 * pequeño de 320 dp y teléfono estándar de 411 dp), con letra al 100 % y al
 * 200 %, y en modo claro y oscuro.
 */

@Composable
private fun NamiComponentsShowcase(darkTheme: Boolean = false) {
    NamiTheme(darkTheme = darkTheme) {
        Surface(color = MaterialTheme.colorScheme.background) {
            Column(
                modifier =
                    Modifier
                        .verticalScroll(rememberScrollState())
                        .padding(NamiSpacing.Large),
                verticalArrangement = Arrangement.spacedBy(NamiSpacing.Medium),
            ) {
                NamiPrimaryButton(
                    text = stringResource(R.string.designsystem_preview_primary),
                    icon = painterResource(R.drawable.ic_nami_send),
                    onClick = {},
                    modifier = Modifier.fillMaxWidth(),
                )
                NamiSecondaryButton(
                    text = stringResource(R.string.designsystem_preview_secondary),
                    icon = painterResource(R.drawable.ic_nami_add),
                    onClick = {},
                    modifier = Modifier.fillMaxWidth(),
                )
                NamiTextField(
                    value = "Ana",
                    onValueChange = {},
                    label = stringResource(R.string.designsystem_preview_field_label),
                    supportingText = stringResource(R.string.designsystem_preview_field_supporting),
                )
                NamiTextField(
                    value = "",
                    onValueChange = {},
                    label = stringResource(R.string.designsystem_preview_field_label),
                    errorMessage = stringResource(R.string.designsystem_preview_field_error),
                )
            }
        }
    }
}

@Preview(name = "Pequeño 320 dp - 100 %", widthDp = 320, heightDp = 640, showBackground = true)
@Composable
private fun SmallPhonePreview() = NamiComponentsShowcase()

@Preview(name = "Pequeño 320 dp - 200 %", widthDp = 320, heightDp = 640, fontScale = 2f, showBackground = true)
@Composable
private fun SmallPhoneLargeTextPreview() = NamiComponentsShowcase()

@Preview(name = "Estándar 411 dp - 100 %", widthDp = 411, heightDp = 891, showBackground = true)
@Composable
private fun StandardPhonePreview() = NamiComponentsShowcase()

@Preview(name = "Estándar 411 dp - 200 % oscuro", widthDp = 411, heightDp = 891, fontScale = 2f, showBackground = true)
@Composable
private fun StandardPhoneLargeTextDarkPreview() = NamiComponentsShowcase(darkTheme = true)
