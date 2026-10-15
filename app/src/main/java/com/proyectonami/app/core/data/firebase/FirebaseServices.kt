package com.proyectonami.app.core.data.firebase

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.MemoryCacheSettings
import com.google.firebase.storage.FirebaseStorage

data class FirebaseServices(
    val auth: FirebaseAuth,
    val firestore: FirebaseFirestore,
    val storage: FirebaseStorage,
)

object FirebaseServicesFactory {
    private const val APP_NAME = "nami"
    private val configuredEmulatorApps = mutableSetOf<String>()

    @Synchronized
    fun create(
        context: Context,
        environment: FirebaseEnvironment,
    ): FirebaseServices {
        val app =
            FirebaseApp.getApps(context).firstOrNull { it.name == APP_NAME }
                ?: FirebaseApp.initializeApp(context, environment.toFirebaseOptions(), APP_NAME)

        require(app.options.projectId == environment.projectId) {
            "La aplicación Firebase existente usa otro projectId"
        }

        val services =
            FirebaseServices(
                auth = FirebaseAuth.getInstance(app),
                firestore = FirebaseFirestore.getInstance(app),
                storage = FirebaseStorage.getInstance(app),
            )

        if (environment.useEmulators && configuredEmulatorApps.add(app.name)) {
            services.auth.useEmulator(
                environment.emulatorHost,
                environment.authEmulatorPort,
            )
            services.firestore.useEmulator(
                environment.emulatorHost,
                environment.firestoreEmulatorPort,
            )
            services.firestore.firestoreSettings =
                FirebaseFirestoreSettings
                    .Builder()
                    .setLocalCacheSettings(MemoryCacheSettings.newBuilder().build())
                    .build()
            services.storage.useEmulator(
                environment.emulatorHost,
                environment.storageEmulatorPort,
            )
        }

        return services
    }
}
