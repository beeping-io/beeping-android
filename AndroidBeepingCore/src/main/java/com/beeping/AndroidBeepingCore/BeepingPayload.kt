package com.beeping.AndroidBeepingCore

/**
 * A short payload sent or received over sound.
 *
 * The payload string is typically 5–9 chars (the legacy SDK used 5-char
 * "beep keys"). The exact maximum length depends on the chosen encoding
 * profile — see [BeepingEncodingMode].
 *
 * @property payload The decoded / to-be-encoded text.
 * @property timestamp Wall-clock time of decode/encode (millis since epoch).
 *   Defaults to "now" when constructed by a consumer for [BeepingClient.send].
 * @property confidence Decoder confidence in `[0.0f, 1.0f]`. Set by the SDK
 *   when emitted as part of [BeepingEvent.Decoded]; defaults to `1.0f` for
 *   payloads constructed by consumers for [BeepingClient.send]. Mirrors
 *   [metrics]`.confidence` when decoded.
 * @property metrics Full signal-quality metrics (BEE-2313), populated by the SDK
 *   for decoded payloads; `null` for payloads constructed for [BeepingClient.send].
 */
data class BeepingPayload(
    val payload: String,
    val timestamp: Long = System.currentTimeMillis(),
    val confidence: Float = 1.0f,
    val metrics: ReceptionMetrics? = null,
) {
    /**
     * BEE-2314: parses this payload as a scheduled transmission
     * (`code + 4-char base-32 timestamp`, emitted by [BeepingClient.sendScheduled]).
     *
     * Returns `null` if the payload is too short, not a valid scheduled payload,
     * or the native library isn't loaded.
     */
    fun parseScheduled(): ScheduledPayload? {
        if (!BeepingCoreJNI.isNativeLoaded()) return null
        return assembleScheduledPayload(payload, BeepingCoreJNI().parseScheduledTimestamp(payload))
    }
}
