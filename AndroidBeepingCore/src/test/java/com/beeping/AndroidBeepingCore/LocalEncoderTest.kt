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
    fun `decoded throws BeepingException MissingMicPermission when RECORD_AUDIO denied`() = runTest {
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
    fun `decoded throws BeepingException NativeLibraryNotLoaded when native lib unavailable`() = runTest {
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
    fun `encode rejects keys not matching 5-char base32 pattern before TODO`() = runTest {
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
    fun `encode throws NotImplementedError for valid key (BEE-65)`() = runTest {
        val encoder = LocalEncoder(context = context)
        val ex = runCatching { encoder.encode("abc12") }.exceptionOrNull()
        assertNotNull("encode() should throw — TODO BEE-65", ex)
        assertTrue(
            "expected NotImplementedError, got ${ex?.javaClass?.simpleName}",
            ex is NotImplementedError,
        )
        encoder.close()
    }

    @Test
    fun `close is idempotent`() = runTest {
        val encoder = LocalEncoder(context = context)
        encoder.close()
        encoder.close() // must not throw
    }
}
