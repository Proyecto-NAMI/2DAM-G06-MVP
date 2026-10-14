package com.proyectonami.app.core.designsystem.theme

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Espaciados comunes de NAMI (T-04). Usar estos valores en lugar de números sueltos. */
object NamiSpacing {
    val ExtraSmall: Dp = 4.dp
    val Small: Dp = 8.dp
    val Medium: Dp = 16.dp
    val Large: Dp = 24.dp
    val ExtraLarge: Dp = 32.dp
}

/** Tamaños de controles accesibles (RNF-01). */
object NamiSize {
    /** Objetivo táctil mínimo exigido por la constitución: 48 x 48 dp. */
    val MinTouchTarget: Dp = 48.dp

    /** Altura mínima de botones y campos principales, por encima del mínimo legal. */
    val PrimaryControlHeight: Dp = 56.dp

    /** Tamaño de los iconos que acompañan al texto de un control. */
    val ControlIcon: Dp = 24.dp

    /** Grosor de borde de los controles secundarios para que se distingan sin depender del color. */
    val ControlBorder: Dp = 2.dp
}
