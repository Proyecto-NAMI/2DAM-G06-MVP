package com.proyectonami.app

import android.os.Build
import android.os.SystemClock
import android.util.Log
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import com.google.firebase.storage.FirebaseStorage
import com.proyectonami.app.core.data.firebase.FirebaseAudioNotesRepository
import com.proyectonami.app.core.data.firebase.FirebaseEnvironment
import com.proyectonami.app.core.data.firebase.FirebasePaths
import com.proyectonami.app.core.data.firebase.FirebaseRepositoryContainer
import com.proyectonami.app.core.data.firebase.FirebaseServicesFactory
import com.proyectonami.app.core.data.firebase.FirebaseTextMessagesRepository
import com.proyectonami.app.core.domain.audio.AudioNoteMetadata
import com.proyectonami.app.core.domain.audio.AudioUploadFailure
import com.proyectonami.app.core.domain.audio.AudioUploadState
import com.proyectonami.app.core.domain.conversations.ConversationsRemoteState
import com.proyectonami.app.core.domain.messages.OutgoingTextMessage
import com.proyectonami.app.core.domain.messages.TextMessageFailure
import com.proyectonami.app.core.domain.messages.TextMessageSendResult
import com.proyectonami.app.core.domain.performance.FlowPerformanceSummary
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.last
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.json.JSONArray
import org.json.JSONObject
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
import java.time.Instant
import java.util.UUID
import java.util.concurrent.TimeUnit

/** Evidencia reproducible de T-38 contra los repositorios productivos y Emulator Suite. */
@RunWith(AndroidJUnit4::class)
class FirebasePerformanceRecoveryTest {
    private val environment = FirebaseEnvironment.fromBuildConfig()
    private val context = ApplicationProvider.getApplicationContext<NamiApplication>()
    private val services = FirebaseServicesFactory.create(context, environment)
    private val repositories = FirebaseRepositoryContainer.create(context, environment)
    private lateinit var senderId: String
    private lateinit var conversationId: String

    @Before
    fun setUp() {
        assumeTrue(environment.useEmulators)
        listOf(
            environment.authEmulatorPort,
            environment.firestoreEmulatorPort,
            environment.functionsEmulatorPort,
            environment.storageEmulatorPort,
        ).forEach { port ->
            assumeTrue(
                "Firebase Emulator Suite no está en ejecución",
                runCatching {
                    Socket().use { it.connect(InetSocketAddress(environment.emulatorHost, port), 1_000) }
                }.isSuccess,
            )
        }
        services.auth.signOut()
        senderId = requireNotNull(await(services.auth.signInAnonymously()).user?.uid)
        conversationId = "performance-${UUID.randomUUID()}"
        seedConversation()
    }

    @After
    fun tearDown() {
        await(services.firestore.enableNetwork())
        services.auth.signOut()
    }

    @Test
    fun productiveFlowsMeetP95TargetsAndWriteReviewableEvidence() =
        runBlocking {
            repeat(WARM_UP_SAMPLES) {
                awaitConversationContent()
                assertEquals(TextMessageSendResult.Sent, repositories.textMessagesRepository.send(message("warm-up-$it")))
            }

            val startSamples =
                List(MEASURED_SAMPLES) {
                    measureMillis { awaitConversationContent() }
                }
            val sendSamples =
                List(MEASURED_SAMPLES) { sample ->
                    measureMillis {
                        assertEquals(
                            TextMessageSendResult.Sent,
                            repositories.textMessagesRepository.send(message("measured-$sample")),
                        )
                    }
                }

            val start = FlowPerformanceSummary("contenido de inicio", startSamples, START_TARGET_MILLIS)
            val send = FlowPerformanceSummary("confirmación de envío", sendSamples, SEND_TARGET_MILLIS)
            val evidence = writeEvidence(start, send)
            Log.i(PERFORMANCE_TAG, "inicio_p95_ms=${start.p95Millis}; envio_p95_ms=${send.p95Millis}")
            Log.i(PERFORMANCE_JSON_TAG, evidence.toString())

            assertTrue("Inicio p95 ${start.p95Millis} ms; objetivo < $START_TARGET_MILLIS ms", start.meetsTarget())
            assertTrue("Envío p95 ${send.p95Millis} ms; objetivo < $SEND_TARGET_MILLIS ms", send.meetsTarget())
        }

    @Test
    fun textAndAudioRecoverFromNetworkLossWithoutDuplicates() =
        runBlocking {
            val text = message("network-recovery")
            val unavailableApp =
                requireNotNull(
                    FirebaseApp.initializeApp(
                        context,
                        environment.toFirebaseOptions(),
                        "nami-network-loss-${UUID.randomUUID()}",
                    ),
                )
            val unavailableFirestore = FirebaseFirestore.getInstance(unavailableApp)
            val unavailableStorage = FirebaseStorage.getInstance(unavailableApp)
            unavailableFirestore.useEmulator("127.0.0.1", 1)
            unavailableStorage.useEmulator("127.0.0.1", 1)
            unavailableStorage.maxUploadRetryTimeMillis = 1_000L
            try {
                assertEquals(
                    TextMessageSendResult.Failed(TextMessageFailure.NETWORK),
                    withTimeout(OPERATION_TIMEOUT_MILLIS) {
                        FirebaseTextMessagesRepository(unavailableFirestore, services.auth).send(text)
                    },
                )
            } finally {
                await(unavailableFirestore.terminate())
            }
            assertEquals(TextMessageSendResult.Sent, repositories.textMessagesRepository.send(text))
            assertTrue(
                List(CONCURRENT_RETRIES) { async { repositories.textMessagesRepository.send(text) } }.awaitAll().all {
                    it == TextMessageSendResult.Sent
                },
            )

            val localAudio = audioFile()
            val audio =
                AudioNoteMetadata(
                    clientMessageId = UUID.randomUUID().toString(),
                    conversationId = conversationId,
                    senderId = senderId,
                    durationMillis = 1_000L,
                    sizeBytes = localAudio.length(),
                )
            try {
                val unavailableRepository =
                    FirebaseAudioNotesRepository(unavailableStorage, services.firestore, services.auth)
                val failed =
                    withTimeout(OPERATION_TIMEOUT_MILLIS) {
                        unavailableRepository.upload(audio, localAudio.absolutePath).last()
                    }
                assertEquals(AudioUploadState.Failed(audio, AudioUploadFailure.NETWORK), failed)
                assertTrue("El temporal debe conservarse mientras sea reintentable", localAudio.isFile)
            } finally {
                unavailableApp.delete()
            }

            assertEquals(
                AudioUploadState.Sent(audio),
                withTimeout(OPERATION_TIMEOUT_MILLIS) {
                    repositories.audioNotesRepository.upload(audio, localAudio.absolutePath).last()
                },
            )
            assertTrue(!localAudio.exists())

            val messages =
                await(
                    services.firestore
                        .collection("${FirebasePaths.conversation(conversationId)}/messages")
                        .get(Source.SERVER),
                )
            assertEquals(2, messages.size())
            assertEquals(
                2L,
                await(services.firestore.document(FirebasePaths.conversation(conversationId)).get(Source.SERVER)).getLong(
                    "unreadCountByUser.$RECIPIENT_ID",
                ),
            )
        }

    private suspend fun awaitConversationContent() {
        val state =
            withTimeout(OPERATION_TIMEOUT_MILLIS) {
                repositories.conversationsRepository
                    .observeConversations()
                    .first { it is ConversationsRemoteState.Loaded }
            } as ConversationsRemoteState.Loaded
        assertTrue(state.conversations.any { it.id == conversationId })
    }

    private fun seedConversation() {
        await(
            services.functions.getHttpsCallable("seedConversationForTests").call(
                mapOf(
                    "conversationId" to conversationId,
                    "otherUserId" to RECIPIENT_ID,
                    "currentUnreadCount" to 0,
                    "otherUnreadCount" to 0,
                    "activityMillis" to System.currentTimeMillis(),
                ),
            ),
        )
    }

    private fun message(label: String): OutgoingTextMessage = OutgoingTextMessage.create(conversationId, senderId, "Texto ficticio $label")

    private fun audioFile(): File =
        File(context.cacheDir, "t38-${UUID.randomUUID()}.m4a").apply {
            writeBytes("audio-ficticio-t38".encodeToByteArray())
        }

    private suspend fun measureMillis(block: suspend () -> Unit): Long {
        val startedAt = SystemClock.elapsedRealtimeNanos()
        block()
        val elapsedNanos = SystemClock.elapsedRealtimeNanos() - startedAt
        return ((elapsedNanos + NANOS_PER_MILLISECOND - 1L) / NANOS_PER_MILLISECOND).coerceAtLeast(1L)
    }

    private fun writeEvidence(
        start: FlowPerformanceSummary,
        send: FlowPerformanceSummary,
    ): JSONObject {
        val configuration = context.resources.configuration
        val evidence =
            JSONObject()
                .put("task", "T-38")
                .put("capturedAtUtc", Instant.now().toString())
                .put("firebaseProject", environment.projectId)
                .put(
                    "device",
                    JSONObject()
                        .put("manufacturer", Build.MANUFACTURER)
                        .put("model", Build.MODEL)
                        .put("api", Build.VERSION.SDK_INT)
                        .put("widthDp", configuration.screenWidthDp)
                        .put("heightDp", configuration.screenHeightDp),
                ).put("warmUpSamples", WARM_UP_SAMPLES)
                .put("flows", JSONArray().put(start.toJson()).put(send.toJson()))
                .put(
                    "limitations",
                    "Emulator Suite local; repetir en hardware físico antes de cerrar la revisión humana.",
                )
        File(context.filesDir, EVIDENCE_FILE_NAME).writeText(evidence.toString(2))
        return evidence
    }

    private fun FlowPerformanceSummary.toJson(): JSONObject =
        JSONObject()
            .put("name", name)
            .put("targetMillisExclusive", targetMillis)
            .put("sampleCount", samplesMillis.size)
            .put("minimumMillis", minimumMillis)
            .put("averageMillis", averageMillis)
            .put("p95Millis", p95Millis)
            .put("maximumMillis", maximumMillis)
            .put("samplesMillis", JSONArray(samplesMillis))
            .put("passed", meetsTarget())

    private fun <T> await(task: Task<T>): T = Tasks.await(task, 60, TimeUnit.SECONDS)

    private companion object {
        const val PERFORMANCE_TAG = "NAMI_PERFORMANCE"
        const val PERFORMANCE_JSON_TAG = "NAMI_PERFORMANCE_JSON"
        const val EVIDENCE_FILE_NAME = "t38-performance.json"
        const val RECIPIENT_ID = "t38-recipient-fictitious"
        const val WARM_UP_SAMPLES = 3
        const val MEASURED_SAMPLES = 20
        const val CONCURRENT_RETRIES = 4
        const val START_TARGET_MILLIS = 2_000L
        const val SEND_TARGET_MILLIS = 3_000L
        const val OPERATION_TIMEOUT_MILLIS = 60_000L
        const val NANOS_PER_MILLISECOND = 1_000_000L
    }
}
