package com.beeping.AndroidBeepingCore

/**
 * Operational event emitted by the SDK to the configured [TelemetryHook].
 *
 * **Privacy guarantees** (verified by [`TelemetryEventTest`]):
 *
 * - ❌ Never contains the raw payload, apiKey, endpoint URL, IP, device id,
 *   user id, or auth token.
 * - ❌ [EncodeFailed.errorType] holds only the class name of the exception
 *   (e.g. `BeepingException`), never the message.
 * - ✅ Mode is a stable string (`"local"` or `"cloud"`), never the
 *   [BeepingMode.Cloud] data class (which carries apiKey + endpoint).
 *
 * If you add a new variant, ensure no field name matches the forbidden
 * privacy patterns — the reflection-based privacy test enforces this.
 */
sealed class TelemetryEvent {

    /** Fired once when [BeepingClient] is constructed. */
    data class SdkInitialized(
        val mode: String,
        val traceId: String,
    ) : TelemetryEvent()

    /** Fired once when [BeepingClient.close] is called. */
    data class Closed(
        val traceId: String,
        val sessionDurationMs: Long,
    ) : TelemetryEvent()

    /**
     * Fired at the start of [BeepingClient.send].
     *
     * @property keyLength size of the payload string in chars (NOT the payload itself)
     */
    data class EncodeRequested(
        val mode: String,
        val keyLength: Int,
        val traceId: String,
    ) : TelemetryEvent()

    /** Fired on successful encode (before any future playback in BEE-64). */
    data class EncodeSucceeded(
        val traceId: String,
        val durationMs: Long,
        val byteCount: Int,
    ) : TelemetryEvent()

    /**
     * Fired when encode throws.
     *
     * @property errorType simple class name of the throwable
     *   (e.g. `"BeepingException"`, `"IllegalArgumentException"`).
     *   The exception MESSAGE is never emitted.
     */
    data class EncodeFailed(
        val traceId: String,
        val errorType: String,
    ) : TelemetryEvent()
}
