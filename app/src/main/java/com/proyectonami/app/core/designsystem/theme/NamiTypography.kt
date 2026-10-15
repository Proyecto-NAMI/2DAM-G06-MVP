package com.proyectonami.app.core.designsystem.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Tipografía de NAMI (T-04, RNF-01).
 *
 * Tamaños mayores que los de Material 3 para personas con visión reducida.
 * Todo se expresa en `sp`, de modo que respeta el tamaño de letra del sistema
 * (hasta el 200 %). El texto más pequeño no baja de 14 sp.
 */
private fun style(size: Int, lineHeight: Int, weight: FontWeight = FontWeight.Normal) = TextStyle(
    fontFamily = FontFamily.SansSerif,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = lineHeight.sp,
)

val NamiTypography = Typography(
    displayLarge = style(48, 56),
    displayMedium = style(42, 50),
    displaySmall = style(36, 44),
    headlineLarge = style(34, 42, FontWeight.SemiBold),
    headlineMedium = style(30, 38, FontWeight.SemiBold),
    headlineSmall = style(26, 34, FontWeight.SemiBold),
    titleLarge = style(24, 32, FontWeight.SemiBold),
    titleMedium = style(20, 28, FontWeight.Medium),
    titleSmall = style(18, 26, FontWeight.Medium),
    bodyLarge = style(20, 30),
    bodyMedium = style(18, 26),
    bodySmall = style(16, 24),
    labelLarge = style(18, 24, FontWeight.SemiBold),
    labelMedium = style(16, 22, FontWeight.Medium),
    labelSmall = style(14, 20, FontWeight.Medium),
)
