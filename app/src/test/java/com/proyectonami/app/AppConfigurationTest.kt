package com.proyectonami.app

import org.junit.Assert.assertEquals
import org.junit.Test

class AppConfigurationTest {
    @Test
    fun applicationIdMatchesNamiPackage() {
        assertEquals("com.proyectonami.app", BuildConfig.APPLICATION_ID)
    }
}
