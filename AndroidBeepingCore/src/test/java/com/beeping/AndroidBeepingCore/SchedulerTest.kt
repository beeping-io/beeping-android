package com.beeping.AndroidBeepingCore

import io.kotest.property.Arb
import io.kotest.property.PropTestConfig
import io.kotest.property.arbitrary.float
import io.kotest.property.checkAll
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * BEE-2240 — JVM-side checks for the scheduler API exposed by
 * [BeepingClient.computeBeepSchedule] and [BeepingClient.sendScheduled].
 *
 * `computeBeepSchedule` reaches into the JNI shim, which on the JVM unit-test
 * runtime can't load `libbeepingcore.so`. So we cover:
 *
 *  - the input-validation surface that runs before the JNI call
 *  - the parity contract between `Local` and `Cloud` modes:
 *      Cloud must fail with [BeepingError.SchedulingNotSupported]
 *
 * The acoustic round-trip of `encodeWithSchedule` (count-of-beeps, decode of
 * a scheduled payload) is covered by the instrumented `SdkPlumbingTest` on
 * the emulator, where the real `.so` is loaded.
 */
class SchedulerTest {
    @Test
    fun `computeBeepSchedule fails with NativeLibraryNotLoaded on JVM where no native libs are present`() {
        val client =
            BeepingClient
                .Builder(mockk(relaxed = true))
                .mode(BeepingMode.Local)
                .build()
        val ex =
            runCatching {
                client.computeBeepSchedule(duration = 10f, startTime = 0f, interval = 2.3f)
            }.exceptionOrNull()
        client.close()
        assertNotNull(ex)
        assertTrue("expected BeepingException, got $ex", ex is BeepingException)
        assertEquals(BeepingError.NativeLibraryNotLoaded, (ex as BeepingException).error)
    }

    @Test
    fun `sendScheduled in Cloud mode fails with SchedulingNotSupported`() =
        runTest {
            val cloud =
                CloudEncoder(
                    apiKey = "anykey",
                    endpoint = "https://example.invalid",
                )
            val ex =
                runCatching {
                    cloud.encodeScheduled(
                        key = "abc12",
                        duration = 10f,
                        startTime = 0f,
                        interval = 2.3f,
                        beepGainDb = 0f,
                        audible = false,
                    )
                }.exceptionOrNull()
            assertNotNull(ex)
            assertTrue("expected BeepingException, got $ex", ex is BeepingException)
            assertEquals(
                BeepingError.SchedulingNotSupported,
                (ex as BeepingException).error,
            )
            cloud.close()
        }

    @Test
    fun `sendScheduled rejects keys not matching 5-char base32 pattern up-front`() =
        runTest {
            // The same KEY_PATTERN guard is enforced by both LocalEncoder and CloudEncoder
            // before any network / native work, so we cover it through LocalEncoder
            // (cheap, no JNI required for the validation path).
            val encoder = LocalEncoder(context = mockk(relaxed = true))
            for (badKey in listOf("ABC12", "abc1", "abc123", "wxyz1", "")) {
                val ex =
                    runCatching {
                        encoder.encodeScheduled(
                            key = badKey,
                            duration = 10f,
                            startTime = 0f,
                            interval = 2.3f,
                            beepGainDb = 0f,
                            audible = false,
                        )
                    }.exceptionOrNull()
                assertTrue(
                    "key '$badKey' should fail validation, got $ex",
                    ex is IllegalArgumentException,
                )
            }
            encoder.close()
        }

    /**
     * Property: every valid `(duration, startTime, interval)` schedule must
     * round-trip through [BeepingError.SchedulingNotSupported] when given to a
     * Cloud encoder — no input combination accidentally bypasses the guard.
     */
    @Test
    fun `cloud sendScheduled rejects every valid schedule combination`() =
        runTest {
            val cloud =
                CloudEncoder(
                    apiKey = "anykey",
                    endpoint = "https://example.invalid",
                )
            checkAll(
                PropTestConfig(iterations = ITERATIONS),
                Arb.float(MIN_DURATION, MAX_DURATION),
                Arb.float(MIN_START, MAX_START),
                Arb.float(MIN_INTERVAL, MAX_INTERVAL),
            ) { duration, start, interval ->
                val ex =
                    runCatching {
                        cloud.encodeScheduled(
                            key = "abc12",
                            duration = duration,
                            startTime = start,
                            interval = interval,
                            beepGainDb = 0f,
                            audible = false,
                        )
                    }.exceptionOrNull()
                assertTrue(
                    "(d=$duration s=$start i=$interval) should reject with " +
                        "SchedulingNotSupported, got $ex",
                    ex is BeepingException &&
                        ex.error is BeepingError.SchedulingNotSupported,
                )
            }
            cloud.close()
        }

    private companion object {
        // BEEPING_ComputeBeepSchedule requires duration >= 2.3, interval > 0,
        // startTime + 2.3 <= duration. We sample inside that admissible region.
        private const val MIN_DURATION = 2.3f
        private const val MAX_DURATION = 60f
        private const val MIN_START = 0f
        private const val MAX_START = 5f
        private const val MIN_INTERVAL = 0.5f
        private const val MAX_INTERVAL = 5f
        private const val ITERATIONS = 50
    }
}
