package com.proyectonami.app.core.designsystem.theme

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** T-04 / RNF-01: el contraste de la paleta cumple WCAG 2.2 AA en modo claro y oscuro. */
class NamiColorContrastTest {
    private val schemes =
        mapOf(
            "claro" to NamiLightColorTokens,
            "oscuro" to NamiDarkColorTokens,
        )

    private fun NamiColorTokens.textPairs() =
        mapOf(
            "onPrimary/primary" to (onPrimary to primary),
            "onPrimaryContainer/primaryContainer" to (onPrimaryContainer to primaryContainer),
            "onSecondary/secondary" to (onSecondary to secondary),
            "onSecondaryContainer/secondaryContainer" to (onSecondaryContainer to secondaryContainer),
            "onError/error" to (onError to error),
            "onErrorContainer/errorContainer" to (onErrorContainer to errorContainer),
            "onBackground/background" to (onBackground to background),
            "onSurface/surface" to (onSurface to surface),
            "onSurfaceVariant/surface" to (onSurfaceVariant to surface),
            "onSurfaceVariant/surfaceVariant" to (onSurfaceVariant to surfaceVariant),
            // Texto de error y de botones secundarios dibujado directamente sobre la superficie.
            "error/surface" to (error to surface),
            "primary/surface" to (primary to surface),
        )

    private fun NamiColorTokens.nonTextPairs() =
        mapOf(
            "outline/surface" to (outline to surface),
            "primary/background" to (primary to background),
            "error/background" to (error to background),
        )

    @Test
    fun textColorsMeetAaContrast() {
        schemes.forEach { (schemeName, tokens) ->
            tokens.textPairs().forEach { (pairName, pair) ->
                val ratio = contrastRatio(pair.first, pair.second)
                assertTrue(
                    "Modo $schemeName, $pairName: contraste $ratio < $MIN_TEXT_CONTRAST",
                    ratio >= MIN_TEXT_CONTRAST,
                )
            }
        }
    }

    @Test
    fun componentColorsMeetNonTextContrast() {
        schemes.forEach { (schemeName, tokens) ->
            tokens.nonTextPairs().forEach { (pairName, pair) ->
                val ratio = contrastRatio(pair.first, pair.second)
                assertTrue(
                    "Modo $schemeName, $pairName: contraste $ratio < $MIN_NON_TEXT_CONTRAST",
                    ratio >= MIN_NON_TEXT_CONTRAST,
                )
            }
        }
    }

    @Test
    fun contrastRatioMatchesWcagReferenceValues() {
        assertEquals(21.0, contrastRatio(0xFF000000, 0xFFFFFFFF), 0.01)
        assertEquals(1.0, contrastRatio(0xFF777777, 0xFF777777), 0.001)
        // #767676 sobre blanco es el gris más claro que alcanza 4,5:1.
        assertEquals(4.54, contrastRatio(0xFF767676, 0xFFFFFFFF), 0.01)
    }

    @Test
    fun touchTargetsAreAtLeast48dp() {
        assertTrue(NamiSize.MinTouchTarget.value >= 48f)
        assertTrue(NamiSize.PrimaryControlHeight.value >= NamiSize.MinTouchTarget.value)
    }
}
