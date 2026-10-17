package com.proyectonami.app.feature.audio

import android.Manifest
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.proyectonami.app.core.media.AndroidAudioRecorder
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class AndroidAudioRecorderTest {
    @Test
    fun recordsIntoPrivateCacheAndDeletesTheTemporaryFile() =
        runBlocking {
            val instrumentation = InstrumentationRegistry.getInstrumentation()
            val context = instrumentation.targetContext
            instrumentation.uiAutomation.grantRuntimePermission(context.packageName, Manifest.permission.RECORD_AUDIO)
            val recorder = AndroidAudioRecorder(context)
            val file = File(context.cacheDir, "audio-test-${UUID.randomUUID()}.m4a")

            try {
                assertTrue(recorder.start(file.absolutePath).isSuccess)
                delay(1_000L)
                val result = recorder.stop().getOrThrow()
                assertTrue(file.isFile)
                assertTrue(result.durationMillis > 0L)
                assertTrue(result.sizeBytes > 0L)
            } finally {
                recorder.cancel()
            }

            assertFalse(file.exists())
        }
}
