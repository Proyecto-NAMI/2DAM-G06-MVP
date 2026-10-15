package com.proyectonami.app

import android.app.Application
import com.proyectonami.app.core.data.firebase.FirebaseEnvironment
import com.proyectonami.app.core.data.firebase.FirebaseRepositoryContainer

class NamiApplication : Application() {
    val repositoryContainer: FirebaseRepositoryContainer by lazy {
        FirebaseRepositoryContainer.create(
            context = this,
            environment = FirebaseEnvironment.fromBuildConfig(),
        )
    }

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.USE_FIREBASE_EMULATORS) {
            repositoryContainer
        }
    }
}
