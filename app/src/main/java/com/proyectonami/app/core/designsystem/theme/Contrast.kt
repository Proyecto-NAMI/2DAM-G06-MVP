package com.proyectonami.app.core.designsystem.theme

import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/** Contraste mínimo WCAG 2.2 AA para texto normal. */
const val MIN_TEXT_CONTRAST = 4.5

/** Contraste mínimo WCAG 2.2 AA para texto grande, bordes y componentes gráficos. */
const val MIN_NON_TEXT_CONTRAST = 3.0

/** Luminancia relativa de un color ARGB según la definición de WCAG 2.2. */
fun relativeLuminance(argb: Long): Double {
    fun channel(shift: Int): Double {
        val value = ((argb shr shift) and 0xFF).toDouble() / 255.0
        return if (value <= 0.03928) value / 12.92 else ((value + 0.055) / 1.055).pow(2.4)
    }
    return 0.2126 * channel(16) + 0.7152 * channel(8) + 0.0722 * channel(0)
}

/** Relación de contraste WCAG entre dos colores ARGB opacos (de 1 a 21). */
fun contrastRatio(first: Long, second: Long): Double {
    val a = relativeLuminance(first)
    val b = relativeLuminance(second)
    return (max(a, b) + 0.05) / (min(a, b) + 0.05)
}
