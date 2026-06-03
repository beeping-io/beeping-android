package com.beeping.AndroidBeepingCore

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import app.cash.turbine.test
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LocalEncoderTest {
    private lateinit var context: Context

    @Before
    fun setUp() {
        context = mockk(relaxed = true)
        // Default — RECORD_AUDIO granted. Individual tests override below.
        every {
            context.checkPermission(Manifest.permission.RECORD_AUDIO, any(), any())
        } returns PackageManager.PERMISSION_GRANTED
    }

    @Test
    fun `decoded throws BeepingException MissingMicPermission when RECORD_AUDIO denied`() =
        runTest {
            every {
                context.checkPermission(Manifest.permission.RECORD_AUDIO, any(), any())
            } returns PackageManager.PERMISSION_DENIED

            val encoder = LocalEncoder(context = context)

            encoder.decoded().test {
                val error = awaitError()
                assertTrue(
                    "expected BeepingException, got ${error::class.simpleName}",
                    error is BeepingException,
                )
                assertEquals(
                    BeepingError.MissingMicPermission,
                    (error as BeepingException).error,
                )
            }
            encoder.close()
        }

    @Test
    fun `decoded throws BeepingException NativeLibraryNotLoaded when native lib unavailable`() =
        runTest {
            // Permission granted (default), JNI not loaded on JVM tests.
            val encoder = LocalEncoder(context = context)

            encoder.decoded().test {
                val error = awaitError()
                assertTrue(error is BeepingException)
                assertEquals(
                    BeepingError.NativeLibraryNotLoaded,
                    (error as BeepingException).error,
                )
            }
            encoder.close()
        }

    @Test
    fun `encode rejects keys not matching 5-char base32 pattern before TODO`() =
        runTest {
            val encoder = LocalEncoder(context = context)
            for (badKey in listOf("ABC12", "abc1", "abc123", "wxyz1", "    1")) {
                val ex = runCatching { encoder.encode(badKey) }.exceptionOrNull()
                assertTrue(
                    "key '$badKey' should fail validation, got $ex",
                    ex is IllegalArgumentException,
                )
            }
            encoder.close()
        }

    @Test
    fun `encode throws BeepingException NativeLibraryNotLoaded on JVM tests without native libs`() =
        runTest {
            // BEE-2226: encode now reaches the JNI shim instead of throwing TODO.
            // The native libs (libbeepingcore.so + libbeeping_jni.so) only load
            // on Android runtime; on the JVM unit test runtime BeepingCoreJNI
            // surfaces the failure via a typed BeepingException.
            val encoder = LocalEncoder(context = context)
            val ex = runCatching { encoder.encode("abc12") }.exceptionOrNull()
            assertNotNull("encode() should throw on JVM (no native libs)", ex)
            assertTrue(
                "expected BeepingException, got ${ex?.javaClass?.simpleName}",
                ex is BeepingException,
            )
            assertEquals(
                BeepingError.NativeLibraryNotLoaded,
                (ex as BeepingException).error,
            )
            encoder.close()
        }

    @Test
    fun `close is idempotent`() =
        runTest {
            val encoder = LocalEncoder(context = context)
            encoder.close()
            encoder.close() // must not throw
        }

    @Test
    fun `BEE-2305 default encodingMode is ALL`() {
        assertEquals(BeepingEncodingMode.ALL, LocalEncoder(context = context).encodingMode)
    }

    @Test
    fun `BEE-2316 setAudioSignature accepts up to 2 seconds and rejects longer`() {
        val encoder = LocalEncoder(context = context)
        assertTrue("exactly 2 s must be accepted", encoder.setAudioSignature(FloatArray(44_100 * 2)))
        assertFalse("longer than 2 s must be rejected", encoder.setAudioSignature(FloatArray(44_100 * 2 + 1)))
    }

    @Test
    fun `BEE-2316 setAudioSignature clears on null or empty`() {
        val encoder = LocalEncoder(context = context)
        assertTrue(encoder.setAudioSignature(null))
        assertTrue(encoder.setAudioSignature(FloatArray(0)))
    }

    @Test
    fun `BEE-2305 factory passes encodingMode to LocalEncoder and defaults to ALL`() {
        val explicit =
            BeepingEncoderFactory.create(
                BeepingMode.Local,
                context,
                "t",
                BeepingEncodingMode.AUDIBLE,
            ) as LocalEncoder
        assertEquals(BeepingEncodingMode.AUDIBLE, explicit.encodingMode)

        val default = BeepingEncoderFactory.create(BeepingMode.Local, context, "t") as LocalEncoder
        assertEquals(BeepingEncodingMode.ALL, default.encodingMode)
    }
}
