package com.proyectonami.app

import android.app.Application
import com.proyectonami.app.core.data.firebase.FirebaseEnvironment
import com.proyectonami.app.core.data.firebase.FirebaseRepositoryContainer
import com.proyectonami.app.core.media.AndroidAudioPlayer
import com.proyectonami.app.core.media.AndroidTextToSpeechEngine
import com.proyectonami.app.core.media.MediaPlaybackCoordinator
import com.proyectonami.app.core.media.MediaSessionCoordinator
import com.proyectonami.app.core.media.TextToSpeechCoordinator

class NamiApplication : Application() {
    val mediaSessionCoordinator = MediaSessionCoordinator()
    val audioPlaybackController by lazy { MediaPlaybackCoordinator(AndroidAudioPlayer()) }
    private val textToSpeechControllerDelegate =
        lazy {
            TextToSpeechCoordinator(
                engine = AndroidTextToSpeechEngine(this),
                beforeSpeak = audioPlaybackController::stop,
            )
        }
    val textToSpeechController by textToSpeechControllerDelegate

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

    override fun onTerminate() {
        if (textToSpeechControllerDelegate.isInitialized()) {
            textToSpeechController.close()
        }
        super.onTerminate()
    }
}
