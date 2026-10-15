package com.proyectonami.app.core.data.firebase

import com.google.firebase.FirebaseOptions
import com.proyectonami.app.BuildConfig

data class FirebaseEnvironment(
    val projectId: String,
    val applicationId: String,
    val apiKey: String,
    val storageBucket: String,
    val useEmulators: Boolean,
    val emulatorHost: String,
    val authEmulatorPort: Int,
    val firestoreEmulatorPort: Int,
    val storageEmulatorPort: Int,
) {
    init {
        require(projectId.isNotBlank()) { "Falta NAMI_FIREBASE_PROJECT_ID" }
        require(applicationId.isNotBlank()) { "Falta NAMI_FIREBASE_APP_ID" }
        require(apiKey.isNotBlank()) { "Falta NAMI_FIREBASE_API_KEY" }
        require(storageBucket.isNotBlank()) { "Falta NAMI_FIREBASE_STORAGE_BUCKET" }
        if (useEmulators) {
            require(projectId.startsWith("demo-")) {
                "El entorno de emuladores debe usar un proyecto demo-* sin recursos reales"
            }
            require(emulatorHost.isNotBlank()) { "Falta NAMI_FIREBASE_EMULATOR_HOST" }
            require(authEmulatorPort in 1..65535)
            require(firestoreEmulatorPort in 1..65535)
            require(storageEmulatorPort in 1..65535)
        }
    }

    fun toFirebaseOptions(): FirebaseOptions =
        FirebaseOptions
            .Builder()
            .setProjectId(projectId)
            .setApplicationId(applicationId)
            .setApiKey(apiKey)
            .setStorageBucket(storageBucket)
            .build()

    companion object {
        fun fromBuildConfig(): FirebaseEnvironment =
            FirebaseEnvironment(
                projectId = BuildConfig.FIREBASE_PROJECT_ID,
                applicationId = BuildConfig.FIREBASE_APP_ID,
                apiKey = BuildConfig.FIREBASE_API_KEY,
                storageBucket = BuildConfig.FIREBASE_STORAGE_BUCKET,
                useEmulators = BuildConfig.USE_FIREBASE_EMULATORS,
                emulatorHost = BuildConfig.FIREBASE_EMULATOR_HOST,
                authEmulatorPort = BuildConfig.FIREBASE_AUTH_EMULATOR_PORT,
                firestoreEmulatorPort = BuildConfig.FIREBASE_FIRESTORE_EMULATOR_PORT,
                storageEmulatorPort = BuildConfig.FIREBASE_STORAGE_EMULATOR_PORT,
            )
    }
}
