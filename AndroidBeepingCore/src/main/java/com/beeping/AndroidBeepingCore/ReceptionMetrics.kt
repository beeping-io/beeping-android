package com.beeping.AndroidBeepingCore

/**
 * Signal-quality metrics for a decoded payload, read from `beeping-core` at
 * `DECODE_COMPLETE` — BEE-2313. Attached to [BeepingPayload.metrics] for
 * payloads emitted by [BeepingClient.listen]; `null` for payloads constructed
 * by consumers for [BeepingClient.send].
 *
 * @property confidence Overall decoder confidence in `[0.0f, 1.0f]`
 *   (`BEEPING_GetConfidence`). Mirrors [BeepingPayload.confidence].
 * @property confidenceError Reed–Solomon error-correction confidence
 *   (`BEEPING_GetConfidenceError`).
 * @property confidenceNoise Signal-to-noise confidence (`BEEPING_GetConfidenceNoise`).
 * @property receivedBeepsVolume Mean volume of the received beeps
 *   (`BEEPING_GetReceivedBeepsVolume`).
 * @property decodedMode The band the payload was decoded in (`BEEPING_GetDecodedMode`).
 */
data class ReceptionMetrics(
    val confidence: Float,
    val confidenceError: Float,
    val confidenceNoise: Float,
    val receivedBeepsVolume: Float,
    val decodedMode: DecodedMode,
)
