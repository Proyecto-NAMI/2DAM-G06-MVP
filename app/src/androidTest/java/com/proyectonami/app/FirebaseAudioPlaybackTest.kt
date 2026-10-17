package com.proyectonami.app

import android.Manifest
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.android.gms.tasks.Tasks
import com.proyectonami.app.core.data.firebase.FirebaseAudioNotesRepository
import com.proyectonami.app.core.data.firebase.FirebaseAudioPlaybackSourceRepository
import com.proyectonami.app.core.data.firebase.FirebaseEnvironment
import com.proyectonami.app.core.data.firebase.FirebaseServicesFactory
import com.proyectonami.app.core.data.firebase.FirebaseTextMessagesRepository
import com.proyectonami.app.core.domain.audio.AudioNoteMetadata
import com.proyectonami.app.core.domain.audio.AudioUploadState
import com.proyectonami.app.core.domain.media.AudioPlaybackSourceResult
import com.proyectonami.app.core.domain.media.AudioPlaybackState
import com.proyectonami.app.core.domain.messages.AudioMessageRecord
import com.proyectonami.app.core.domain.messages.TextMessagesRemoteState
import com.proyectonami.app.core.media.AndroidAudioPlayer
import com.proyectonami.app.core.media.AndroidAudioRecorder
import com.proyectonami.app.core.media.MediaPlaybackCoordinator
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.last
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.net.InetSocketAddress
import java.net.Socket
import java.util.UUID
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class FirebaseAudioPlaybackTest {
    private val environment = FirebaseEnvironment.fromBuildConfig()
    private val context = ApplicationProvider.getApplicationContext<NamiApplication>()
    private val services = FirebaseServicesFactory.create(context, environment)
    private val uploadRepository = FirebaseAudioNotesRepository(services.storage, services.firestore, services.auth)
    private val playbackSource = FirebaseAudioPlaybackSourceRepository(context, services.storage)
    private val messagesRepository = FirebaseTextMessagesRepository(services.firestore, services.auth)

    @Before
    fun requireEmulatorsAndMicrophone() {
        assumeTrue(environment.useEmulators)
        for (
        port in
        listOf(
            environment.authEmulatorPort,
            environment.firestoreEmulatorPort,
            environment.functionsEmulatorPort,
            environment.storageEmulatorPort,
        )
        ) {
            assumeTrue(
                "Firebase Emulator Suite no está en ejecución",
                runCatching { Socket().use { it.connect(InetSocketAddress(environment.emulatorHost, port), 1_000) } }.isSuccess,
            )
        }
        InstrumentationRegistry.getInstrumentation().uiAutomation.grantRuntimePermission(
            context.packageName,
            Manifest.permission.RECORD_AUDIO,
        )
    }

    @After
    fun cleanUp() {
        playbackSource.clear()
        services.auth.signOut()
    }

    @Test
    fun realRecordedAudioIsListedDownloadedAndPlayedFromPrivateCache() =
        runBlocking {
            val senderId =
                requireNotNull(
                    Tasks.await(services.auth.signInAnonymously(), TIMEOUT_SECONDS, TimeUnit.SECONDS).user?.uid,
                )
            val conversationId = "audio-playback-${UUID.randomUUID()}"
            seedConversation(conversationId)
            val recorder = AndroidAudioRecorder(context)
            val recordedFile = File(context.cacheDir, "playback-source-${UUID.randomUUID()}.m4a")
            val player = AndroidAudioPlayer()
            val coordinator = MediaPlaybackCoordinator(player)

            try {
                assertTrue(recorder.start(recordedFile.absolutePath).isSuccess)
                delay(RECORDING_MILLIS)
                val recording = recorder.stop().getOrThrow()
                val metadata =
                    AudioNoteMetadata(
                        clientMessageId = UUID.randomUUID().toString(),
                        conversationId = conversationId,
                        senderId = senderId,
                        durationMillis = recording.durationMillis,
                        sizeBytes = recording.sizeBytes,
                    )
                assertEquals(AudioUploadState.Sent(metadata), uploadRepository.upload(metadata, recordedFile.absolutePath).last())

                val message =
                    withTimeout(TIMEOUT_SECONDS * 1_000L) {
                        messagesRepository
                            .observe(conversationId)
                            .mapNotNull { state ->
                                (state as? TextMessagesRemoteState.Loaded)
                                    ?.messages
                                    ?.filterIsInstance<AudioMessageRecord>()
                                    ?.singleOrNull { it.clientMessageId == metadata.clientMessageId }
                            }.first()
                    }
                assertEquals(recording.durationMillis, message.durationMillis)

                val source = playbackSource.prepare(message)
                assertTrue(source is AudioPlaybackSourceResult.Ready)
                val localPath = (source as AudioPlaybackSourceResult.Ready).localPath
                assertTrue(File(localPath).isFile)
                assertTrue(File(localPath).canonicalPath.startsWith(context.cacheDir.canonicalPath))

                coordinator.play(message.clientMessageId, localPath)
                assertEquals(AudioPlaybackState.Playing(message.clientMessageId), coordinator.state.value)
                coordinator.pause()
                assertEquals(AudioPlaybackState.Paused(message.clientMessageId), coordinator.state.value)
                coordinator.resume()
                assertEquals(AudioPlaybackState.Playing(message.clientMessageId), coordinator.state.value)
                coordinator.stop()
                assertEquals(AudioPlaybackState.Idle, coordinator.state.value)
            } finally {
                coordinator.stop()
                recorder.cancel()
            }
        }

    private fun seedConversation(conversationId: String) {
        Tasks.await(
            services.functions.getHttpsCallable("seedConversationForTests").call(
                mapOf(
                    "conversationId" to conversationId,
                    "otherUserId" to RECIPIENT_ID,
                    "currentUnreadCount" to 0,
                    "otherUnreadCount" to 0,
                    "activityMillis" to System.currentTimeMillis(),
                ),
            ),
            TIMEOUT_SECONDS,
            TimeUnit.SECONDS,
        )
    }

    private companion object {
        const val TIMEOUT_SECONDS = 60L
        const val RECORDING_MILLIS = 2_000L
        const val RECIPIENT_ID = "audio-playback-recipient-fictitious"
    }
}
