package com.beeping.AndroidBeepingCore

/**
 * A short payload sent or received over sound.
 *
 * The payload string is typically 5–9 chars (the legacy SDK used 5-char
 * "beep keys"). The exact maximum length depends on the chosen encoding
 * profile — see `EnumBeepingMode` (internal).
 *
 * @property payload The decoded / to-be-encoded text.
 * @property timestamp Wall-clock time of decode/encode (millis since epoch).
 *   Defaults to "now" when constructed by a consumer for [BeepingClient.send].
 * @property confidence Decoder confidence in `[0.0f, 1.0f]`. Set by the SDK
 *   when emitted as part of [BeepingEvent.Decoded]; defaults to `1.0f` for
 *   payloads constructed by consumers for [BeepingClient.send].
 */
data class BeepingPayload(
    val payload: String,
    val timestamp: Long = System.currentTimeMillis(),
    val confidence: Float = 1.0f,
)
