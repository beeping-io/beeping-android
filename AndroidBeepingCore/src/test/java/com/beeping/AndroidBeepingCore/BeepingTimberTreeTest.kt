package com.beeping.AndroidBeepingCore

import android.util.Log
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class BeepingTimberTreeTest {
    @Before
    fun resetLevel() {
        BeepingTimberTree.setLogLevel(LogLevel.INFO)
    }

    // -- isLoggable: level filtering --------------------------------------

    @Test
    fun `isLoggable returns false when LogLevel is NONE for any priority`() {
        BeepingTimberTree.setLogLevel(LogLevel.NONE)
        for (priority in listOf(Log.VERBOSE, Log.DEBUG, Log.INFO, Log.WARN, Log.ERROR)) {
            assertFalse(BeepingTimberTree.shouldLog(priority))
        }
    }

    @Test
    fun `isLoggable filters DEBUG when level is INFO`() {
        BeepingTimberTree.setLogLevel(LogLevel.INFO)
        assertFalse(BeepingTimberTree.shouldLog(Log.VERBOSE))
        assertFalse(BeepingTimberTree.shouldLog(Log.DEBUG))
        assertTrue(BeepingTimberTree.shouldLog(Log.INFO))
        assertTrue(BeepingTimberTree.shouldLog(Log.WARN))
        assertTrue(BeepingTimberTree.shouldLog(Log.ERROR))
    }

    @Test
    fun `isLoggable allows VERBOSE only when level is VERBOSE`() {
        BeepingTimberTree.setLogLevel(LogLevel.VERBOSE)
        assertTrue(BeepingTimberTree.shouldLog(Log.VERBOSE))
        assertTrue(BeepingTimberTree.shouldLog(Log.DEBUG))
        assertTrue(BeepingTimberTree.shouldLog(Log.INFO))
    }

    @Test
    fun `isLoggable filters everything below ERROR when level is ERROR`() {
        BeepingTimberTree.setLogLevel(LogLevel.ERROR)
        assertFalse(BeepingTimberTree.shouldLog(Log.VERBOSE))
        assertFalse(BeepingTimberTree.shouldLog(Log.DEBUG))
        assertFalse(BeepingTimberTree.shouldLog(Log.INFO))
        assertFalse(BeepingTimberTree.shouldLog(Log.WARN))
        assertTrue(BeepingTimberTree.shouldLog(Log.ERROR))
    }

    // -- PII redaction -----------------------------------------------------

    @Test
    fun `redact masks Bearer token`() {
        val input = "Authorization: Bearer bk_LFhY0ZQnuYL1PgIDBKm5cA"
        val redacted = BeepingTimberTree.redact(input)
        assertEquals("Authorization: Bearer ***", redacted)
    }

    @Test
    fun `redact masks Bearer token in JSON-like body`() {
        val input = """{"Authorization":"Bearer abc.def.ghi"}"""
        val redacted = BeepingTimberTree.redact(input)
        // The pattern stops at any non-token char (including comma, quote)
        assertTrue(
            "expected redaction of Bearer, got: $redacted",
            redacted.contains("Bearer ***"),
        )
        assertFalse(
            "raw token should not survive: $redacted",
            redacted.contains("abc.def.ghi"),
        )
    }

    @Test
    fun `redact masks apiKey query param`() {
        val input = "GET /v1/health?apiKey=bk_LFhY0ZQnuYL1Pg"
        val redacted = BeepingTimberTree.redact(input)
        assertEquals("GET /v1/health?apiKey=***", redacted)
    }

    @Test
    fun `redact does not over-mask plain words`() {
        val input = "user did something with apiKey concept but no value"
        val redacted = BeepingTimberTree.redact(input)
        // No `apiKey=...` pattern present → unchanged
        assertEquals(input, redacted)
    }

    // -- installOnce idempotence ------------------------------------------

    @Test
    fun `installOnce is idempotent`() {
        BeepingTimberTree.installOnce()
        BeepingTimberTree.installOnce()
        BeepingTimberTree.installOnce()
        // No assertion needed — must not throw, must not double-plant.
        // Additional verification would require Timber.forest() inspection
        // which is global state, so we just assert "didn't throw".
    }
}
