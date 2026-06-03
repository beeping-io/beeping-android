package com.beeping.AndroidBeepingCore

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** BEE-2314 — scheduled-payload split assembly + graceful native-absent path. */
class ScheduledPayloadTest {
    @Test
    fun `assemble splits code from the 4-char timestamp suffix`() {
        val result = assembleScheduledPayload("abcd0042", timestampSec = 42)
        assertEquals(ScheduledPayload(code = "abcd", timestampSec = 42), result)
    }

    @Test
    fun `assemble keeps a single-char code at the minimum length`() {
        // 5 chars = 1 code char + 4 timestamp chars.
        assertEquals(ScheduledPayload(code = "a", timestampSec = 0), assembleScheduledPayload("a0000", 0))
    }

    @Test
    fun `assemble returns null when payload is shorter than 5 chars`() {
        assertNull(assembleScheduledPayload("ab12", 7))
        assertNull(assembleScheduledPayload("", 0))
    }

    @Test
    fun `assemble returns null on a negative native parse result`() {
        // -2 = ParseScheduledPayload reported an invalid base-32 trailer.
        assertNull(assembleScheduledPayload("abcd0042", timestampSec = -2))
    }

    @Test
    fun `parseScheduled returns null on the JVM where the native library is absent`() {
        // BeepingCoreJNI.isNativeLoaded() is false off-device — parseScheduled
        // must degrade to null rather than hit an UnsatisfiedLinkError.
        assertNull(BeepingPayload(payload = "abcd0042").parseScheduled())
    }
}
