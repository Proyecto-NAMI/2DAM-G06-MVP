package com.proyectonami.app.core.domain.performance

import org.junit.Assert.assertFalse
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
}
