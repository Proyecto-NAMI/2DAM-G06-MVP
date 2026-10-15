package com.proyectonami.app

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.android.gms.tasks.Tasks
import com.google.firebase.storage.StorageMetadata
import com.proyectonami.app.core.data.firebase.FirebaseEnvironment
import com.proyectonami.app.core.data.firebase.FirebaseRepositoryContainer
import com.proyectonami.app.core.data.firebase.FirebaseServicesFactory
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class FirebaseEmulatorSmokeTest {
    private val environment = FirebaseEnvironment.fromBuildConfig()
    private val context = ApplicationProvider.getApplicationContext<NamiApplication>()
    private val services = FirebaseServicesFactory.create(context, environment)
    private val repositories = FirebaseRepositoryContainer.create(context, environment)

    @After
    fun signOut() {
        repositories.authRepository.signOut()
    }

    @Test
    fun authFirestoreAndStorageWorkWithFictitiousData() {
        assumeTrue(environment.useEmulators)

        val authResult =
            Tasks.await(
                services.auth.signInAnonymously(),
                TIMEOUT_SECONDS,
                TimeUnit.SECONDS,
            )
        val userId = requireNotNull(authResult.user?.uid)
        val conversationId = "conversation-$userId"
        val messageId = "message-1"

        Tasks.await(
            repositories.firestoreRepository.userDocument(userId).set(
                mapOf(
                    "displayName" to "Persona Ficticia",
                    "surname" to "Prueba",
                ),
            ),
            TIMEOUT_SECONDS,
            TimeUnit.SECONDS,
        )
        Tasks.await(
            repositories.firestoreRepository.conversationDocument(conversationId).set(
                mapOf("participantIds" to listOf(userId, "usuario-ficticio-2")),
            ),
            TIMEOUT_SECONDS,
            TimeUnit.SECONDS,
        )

        val metadata =
            StorageMetadata
                .Builder()
                .setContentType("audio/mp4")
                .build()
        val audioReference =
            repositories.audioStorageRepository.audioReference(
                conversationId = conversationId,
                messageId = messageId,
            )
        Tasks.await(
            audioReference.putBytes("audio-ficticio".encodeToByteArray(), metadata),
            TIMEOUT_SECONDS,
            TimeUnit.SECONDS,
        )

        assertEquals(
            "audio/mp4",
            Tasks.await(audioReference.metadata, TIMEOUT_SECONDS, TimeUnit.SECONDS).contentType,
        )
        assertEquals(userId, repositories.authRepository.currentUserId)
    }

    companion object {
        private const val TIMEOUT_SECONDS = 20L
    }
}
