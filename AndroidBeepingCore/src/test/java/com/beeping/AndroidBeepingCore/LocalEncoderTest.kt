package com.beeping.AndroidBeepingCore

import android.content.Context
import app.cash.turbine.test
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LocalEncoderTest {

    private val context: Context = mockk(relaxed = true)

    @Test
    fun `decoded emits nothing when native lib not loaded`() = runTest {
        // On the JVM unit test runtime, libbeepingcore.so cannot load — so
        // LocalEncoder.decoded() must complete without emitting anything.
        val encoder = LocalEncoder(context = context)

        encoder.decoded().test {
            awaitComplete()
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
