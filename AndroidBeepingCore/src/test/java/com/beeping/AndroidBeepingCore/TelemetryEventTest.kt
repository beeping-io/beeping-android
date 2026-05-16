package com.beeping.AndroidBeepingCore

import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Reflection-based privacy guard: verifies that no [TelemetryEvent] variant
 * exposes a property whose name matches a known PII pattern.
 *
 * Adding a new variant or property that ends up named e.g. `apiKey` or
 * `endpoint` will fail this test until renamed or refactored.
 */
class TelemetryEventTest {
    private val forbiddenSubstrings =
        listOf(
            "payload", // raw decoded/encoded data
            "apikey", // auth secret
            "endpoint", // network destination
            "url", // any URL is suspect
            "ip", // IP address — but `ip` could match harmless prefixes; use word boundaries below
            "token", // auth tokens
            "auth", // authentication-related fields
            "deviceid",
            "userid",
            "email",
            "phone",
            "secret",
            "password",
        )

    private val variants =
        listOf(
            TelemetryEvent.SdkInitialized::class.java,
            TelemetryEvent.Closed::class.java,
            TelemetryEvent.EncodeRequested::class.java,
            TelemetryEvent.EncodeSucceeded::class.java,
            TelemetryEvent.EncodeFailed::class.java,
        )

    @Test
    fun `no TelemetryEvent variant exposes a PII-named field`() {
        val violations = mutableListOf<String>()
        for (cls in variants) {
            for (field in cls.declaredFields) {
                if (field.isSynthetic) continue
                val name = field.name.lowercase()
                forbiddenSubstrings.forEach { forbidden ->
                    if (matches(name, forbidden)) {
                        violations += "${cls.simpleName}.${field.name} matches '$forbidden'"
                    }
                }
            }
        }
        assertTrue(
            "TelemetryEvent variants must not expose PII-named fields. Violations: $violations",
            violations.isEmpty(),
        )
    }

    /**
     * For multi-letter forbidden words: substring match.
     * For 2-char patterns like "ip": only match as full word or with separators.
     */
    private fun matches(
        fieldName: String,
        forbidden: String,
    ): Boolean =
        if (forbidden.length <= 2) {
            // Word-boundary match for short patterns to avoid false positives like "duration".
            fieldName == forbidden ||
                fieldName.startsWith("${forbidden}_") ||
                fieldName.endsWith("_$forbidden") ||
                fieldName.contains("_${forbidden}_")
        } else {
            fieldName.contains(forbidden)
        }

    @Test
    fun `EncodeRequested exposes only sanitized fields`() {
        val fields =
            TelemetryEvent.EncodeRequested::class.java.declaredFields
                .filterNot { it.isSynthetic }
                .map { it.name }
                .toSet()
        // Whitelist of fields that ARE allowed in this variant.
        val whitelist = setOf("mode", "keyLength", "traceId")
        assertTrue(
            "Unexpected field(s) in EncodeRequested: ${fields - whitelist}",
            (fields - whitelist).isEmpty(),
        )
    }

    @Test
    fun `EncodeSucceeded exposes only sanitized fields`() {
        val fields =
            TelemetryEvent.EncodeSucceeded::class.java.declaredFields
                .filterNot { it.isSynthetic }
                .map { it.name }
                .toSet()
        val whitelist = setOf("traceId", "durationMs", "byteCount")
        assertTrue(
            "Unexpected field(s) in EncodeSucceeded: ${fields - whitelist}",
            (fields - whitelist).isEmpty(),
        )
    }

    @Test
    fun `EncodeFailed exposes only sanitized fields (errorType is class name not message)`() {
        val fields =
            TelemetryEvent.EncodeFailed::class.java.declaredFields
                .filterNot { it.isSynthetic }
                .map { it.name }
                .toSet()
        val whitelist = setOf("traceId", "errorType")
        assertTrue(
            "Unexpected field(s) in EncodeFailed: ${fields - whitelist}",
            (fields - whitelist).isEmpty(),
        )
    }
}
