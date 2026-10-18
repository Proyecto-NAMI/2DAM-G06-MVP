package com.proyectonami.app.core.domain.performance

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class FlowMetricsT27Test {
    @Test
    fun measurementsAreComparedWithTheAgreedTarget() {
        assertTrue(FlowMeasurement("abrir conversación", 900L, 1_000L).meetsTarget())
        assertTrue(FlowMeasurement("enviar texto", 2_000L, 2_000L).meetsTarget())
        assertFalse(FlowMeasurement("reintentar", 2_001L, 2_000L).meetsTarget())
    }

    @Test
    fun draftAndAudioRemainSafeOnlyWithoutDuplicatesOrBlankValues() {
        assertTrue(FlowSafetySnapshot("borrador ficticio", "cache/audio-ficticio.m4a", 0).isSafe())
        assertTrue(FlowSafetySnapshot(null, null, 0).isSafe())
        assertFalse(FlowSafetySnapshot(" ", "cache/audio-ficticio.m4a", 0).isSafe())
        assertFalse(FlowSafetySnapshot("borrador ficticio", "", 0).isSafe())
        assertFalse(FlowSafetySnapshot("borrador ficticio", "cache/audio-ficticio.m4a", 1).isSafe())
    }

    @Test
    fun p95UsesNearestRankAndRequiresAReviewableSample() {
        val samples = (1L..20L).toList()
        val summary = FlowPerformanceSummary("confirmación de envío", samples, 20L)

        assertEquals(19L, percentile95(samples))
        assertEquals(19L, summary.p95Millis)
        assertEquals(1L, summary.minimumMillis)
        assertEquals(20L, summary.maximumMillis)
        assertEquals(10.5, summary.averageMillis, 0.0)
        assertTrue(summary.meetsTarget())
        assertFalse(FlowPerformanceSummary("límite estricto", samples, 19L).meetsTarget())

        assertThrows(IllegalArgumentException::class.java) {
            FlowPerformanceSummary("muestra insuficiente", samples.dropLast(1), 20L)
        }
    }
}
