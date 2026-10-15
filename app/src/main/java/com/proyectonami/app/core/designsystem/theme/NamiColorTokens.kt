package com.proyectonami.app.core.designsystem.theme

/**
 * Tokens de color de NAMI (T-04, RNF-01).
 *
 * Los valores se guardan como ARGB en [Long] para poder comprobar su contraste
 * con pruebas unitarias en la JVM, sin depender de Android ni de Compose.
 * Cada pareja "fondo / contenido" debe cumplir WCAG 2.2 AA: 4,5:1 para texto
 * y 3:1 para bordes y componentes gráficos (ver NamiColorContrastTest).
 *
 * Paleta provisional: debe contrastarse con el prototipo de Figma y, si se
 * cambia, volver a pasar las pruebas de contraste.
 */
data class NamiColorTokens(
    val primary: Long,
    val onPrimary: Long,
    val primaryContainer: Long,
    val onPrimaryContainer: Long,
    val secondary: Long,
    val onSecondary: Long,
    val secondaryContainer: Long,
    val onSecondaryContainer: Long,
    val error: Long,
    val onError: Long,
    val errorContainer: Long,
    val onErrorContainer: Long,
    val background: Long,
    val onBackground: Long,
    val surface: Long,
    val onSurface: Long,
    val surfaceVariant: Long,
    val onSurfaceVariant: Long,
    val outline: Long,
)

val NamiLightColorTokens =
    NamiColorTokens(
        primary = 0xFF1A4F7A,
        onPrimary = 0xFFFFFFFF,
        primaryContainer = 0xFFD3E6F7,
        onPrimaryContainer = 0xFF0A2A42,
        secondary = 0xFF2D5F3F,
        onSecondary = 0xFFFFFFFF,
        secondaryContainer = 0xFFD4EBDA,
        onSecondaryContainer = 0xFF0E2A17,
        error = 0xFFB3261E,
        onError = 0xFFFFFFFF,
        errorContainer = 0xFFF9DEDC,
        onErrorContainer = 0xFF410E0B,
        background = 0xFFFBFCFE,
        onBackground = 0xFF191C1F,
        surface = 0xFFFBFCFE,
        onSurface = 0xFF191C1F,
        surfaceVariant = 0xFFE1E6EC,
        onSurfaceVariant = 0xFF3F474E,
        outline = 0xFF5F6B75,
    )

val NamiDarkColorTokens =
    NamiColorTokens(
        primary = 0xFF9ECBF2,
        onPrimary = 0xFF00304F,
        primaryContainer = 0xFF1A4F7A,
        onPrimaryContainer = 0xFFD3E6F7,
        secondary = 0xFFA8D5B4,
        onSecondary = 0xFF0E3A1F,
        secondaryContainer = 0xFF2D5F3F,
        onSecondaryContainer = 0xFFD4EBDA,
        error = 0xFFF2B8B5,
        onError = 0xFF601410,
        errorContainer = 0xFF8C1D18,
        onErrorContainer = 0xFFF9DEDC,
        background = 0xFF111417,
        onBackground = 0xFFE2E5E9,
        surface = 0xFF111417,
        onSurface = 0xFFE2E5E9,
        surfaceVariant = 0xFF3F474E,
        onSurfaceVariant = 0xFFC2C9D0,
        outline = 0xFF8C969F,
    )
