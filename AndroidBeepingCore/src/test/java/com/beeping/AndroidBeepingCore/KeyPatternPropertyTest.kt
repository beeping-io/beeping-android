package com.beeping.AndroidBeepingCore

import io.kotest.property.Arb
import io.kotest.property.arbitrary.string
import io.kotest.property.checkAll
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Property-based test (Kotest 5.9 `kotest-property` lib) executed inside a
 * standard JUnit 4 `@Test` — no framework swap needed. Verifies that the
 * 5-char base32 [a-v0-9] regex enforced by `LocalEncoder.encode` rejects
 * EVERY randomly generated string that doesn't match.
 *
 * The property: for every random string `s` that does NOT match the pattern,
 * `encoder.encode(s)` must throw [IllegalArgumentException]. 200 iterations
 * of `Arb.string(0..12)` produce essentially zero matches, so the property
 * is exercised on all inputs — `kotest-property` keeps the test clean.
 */
class KeyPatternPropertyTest {

    private val pattern = Regex("^[0-9a-v]{5}$")

    @Test
    fun `LocalEncoder rejects every string that does not match base32-5 pattern`() = runTest {
        val encoder = LocalEncoder(mockk(relaxed = true))

        checkAll(Arb.string(minSize = 0, maxSize = 12)) { random ->
            if (pattern.matches(random)) return@checkAll

            val thrown = runCatching { encoder.encode(random) }.exceptionOrNull()
            assertTrue(
                "encode('$random') should reject with IllegalArgumentException, got $thrown",
                thrown is IllegalArgumentException,
            )
        }
    }
}
