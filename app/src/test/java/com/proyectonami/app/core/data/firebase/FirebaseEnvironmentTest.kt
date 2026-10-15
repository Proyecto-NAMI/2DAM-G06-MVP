package com.proyectonami.app.core.data.firebase

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class FirebaseEnvironmentTest {
    @Test
    fun emulatorEnvironmentAcceptsOnlyDemoProjects() {
        val environment = emulatorEnvironment(projectId = "demo-nami")

        assertEquals("demo-nami", environment.projectId)
    }

    @Test
    fun emulatorEnvironmentRejectsRealProjectIdentifiers() {
        assertThrows(IllegalArgumentException::class.java) {
            emulatorEnvironment(projectId = "nami-production")
        }
    }

    @Test
    fun emulatorEnvironmentRejectsInvalidPorts() {
        assertThrows(IllegalArgumentException::class.java) {
            emulatorEnvironment(projectId = "demo-nami", authPort = 0)
        }
    }

    private fun emulatorEnvironment(
        projectId: String,
        authPort: Int = 9099,
    ): FirebaseEnvironment =
        FirebaseEnvironment(
            projectId = projectId,
            applicationId = "1:1234567890:android:0000000000000000000000",
            apiKey = "fake-api-key",
            storageBucket = "$projectId.appspot.com",
            useEmulators = true,
            emulatorHost = "10.0.2.2",
            authEmulatorPort = authPort,
            firestoreEmulatorPort = 8080,
            storageEmulatorPort = 9199,
        )
}
