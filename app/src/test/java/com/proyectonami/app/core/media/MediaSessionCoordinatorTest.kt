package com.proyectonami.app.core.media

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MediaSessionCoordinatorTest {
    @Test
    fun stopAllStopsAndRemovesEveryRegisteredSession() {
        val coordinator = MediaSessionCoordinator()
        var firstStopCount = 0
        var secondStopCount = 0
        coordinator.register(ActiveMediaSession { firstStopCount++ })
        coordinator.register(ActiveMediaSession { secondStopCount++ })

        assertTrue(coordinator.stopAll())
        assertEquals(1, firstStopCount)
        assertEquals(1, secondStopCount)
        assertTrue(coordinator.stopAll())
        assertEquals(1, firstStopCount)
        assertEquals(1, secondStopCount)
    }

    @Test
    fun stopAllContinuesAfterOneSessionFailsAndReportsFailure() {
        val coordinator = MediaSessionCoordinator()
        var secondStopCount = 0
        coordinator.register(ActiveMediaSession { throw IllegalStateException("stop failed") })
        coordinator.register(ActiveMediaSession { secondStopCount++ })

        assertFalse(coordinator.stopAll())
        assertEquals(1, secondStopCount)
        assertTrue(coordinator.stopAll())
    }

    @Test
    fun closingRegistrationUnregistersSession() {
        val coordinator = MediaSessionCoordinator()
        var stopCount = 0
        val registration = coordinator.register(ActiveMediaSession { stopCount++ })
        registration.close()

        assertTrue(coordinator.stopAll())
        assertEquals(0, stopCount)
    }
}
