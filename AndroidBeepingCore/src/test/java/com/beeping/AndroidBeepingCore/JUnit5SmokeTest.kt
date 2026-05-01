package com.beeping.AndroidBeepingCore

import org.junit.jupiter.api.Assertions.assertAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * Smoke test for the JUnit 5 (Jupiter) engine — verifies the Mannodermaus
 * Android JUnit 5 plugin is active alongside the JUnit Vintage engine that
 * keeps all existing JUnit 4 tests green.
 */
@DisplayName("JUnit 5 Jupiter engine smoke")
class JUnit5SmokeTest {
    @Test
    @DisplayName("assertAll groups multiple assertions in a single failure")
    fun `assertAll groups assertions`() {
        assertAll(
            { assertEquals(2, 1 + 1) },
            { assertEquals("ab", "a" + "b") },
            { assertEquals(listOf(1, 2), listOf(1) + listOf(2)) },
        )
    }

    @Test
    @DisplayName("assertThrows captures expected exception type")
    fun `assertThrows captures expected exception`() {
        val ex =
            assertThrows(IllegalArgumentException::class.java) {
                require(false) { "boom" }
            }
        assertEquals("boom", ex.message)
    }
}
