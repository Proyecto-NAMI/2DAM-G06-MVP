package com.proyectonami.app.core.designsystem.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private fun NamiColorTokens.toLightScheme(): ColorScheme = lightColorScheme(
    primary = Color(primary),
    onPrimary = Color(onPrimary),
    primaryContainer = Color(primaryContainer),
    onPrimaryContainer = Color(onPrimaryContainer),
    secondary = Color(secondary),
    onSecondary = Color(onSecondary),
    secondaryContainer = Color(secondaryContainer),
    onSecondaryContainer = Color(onSecondaryContainer),
    error = Color(error),
    onError = Color(onError),
    errorContainer = Color(errorContainer),
    onErrorContainer = Color(onErrorContainer),
    background = Color(background),
    onBackground = Color(onBackground),
    surface = Color(surface),
    onSurface = Color(onSurface),
    surfaceVariant = Color(surfaceVariant),
    onSurfaceVariant = Color(onSurfaceVariant),
    outline = Color(outline),
)

private fun NamiColorTokens.toDarkScheme(): ColorScheme = darkColorScheme(
    primary = Color(primary),
    onPrimary = Color(onPrimary),
    primaryContainer = Color(primaryContainer),
    onPrimaryContainer = Color(onPrimaryContainer),
    secondary = Color(secondary),
    onSecondary = Color(onSecondary),
    secondaryContainer = Color(secondaryContainer),
    onSecondaryContainer = Color(onSecondaryContainer),
    error = Color(error),
    onError = Color(onError),
    errorContainer = Color(errorContainer),
    onErrorContainer = Color(onErrorContainer),
    background = Color(background),
    onBackground = Color(onBackground),
    surface = Color(surface),
    onSurface = Color(onSurface),
    surfaceVariant = Color(surfaceVariant),
    onSurfaceVariant = Color(onSurfaceVariant),
    outline = Color(outline),
)

private val LightColors = NamiLightColorTokens.toLightScheme()
private val DarkColors = NamiDarkColorTokens.toDarkScheme()

/**
 * Tema de NAMI (T-04, RNF-01).
 *
 * No usa colores dinámicos del sistema (Material You) a propósito: los colores
 * del fondo de pantalla podrían romper el contraste verificado en las pruebas.
 */
@Composable
fun NamiTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = NamiTypography,
        content = content,
    )
}
