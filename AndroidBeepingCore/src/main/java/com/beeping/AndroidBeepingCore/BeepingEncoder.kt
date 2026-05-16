package com.beeping.AndroidBeepingCore

import kotlinx.coroutines.flow.Flow

/**
 * Strategy interface for the encode/decode pipeline.
 *
 * Two implementations:
 *
 * - [LocalEncoder]: wraps the JNI bridge to `libbeepingcore.so` (on-device).
 * - [CloudEncoder]: HTTP client for `beepbox-server` via Ktor (network).
 *
 * Selection is owned by [BeepingEncoderFactory] based on the [BeepingMode]
 * passed by the consumer through (BEE-58) `BeepingClient.Builder`.
 *
 * The interface is `internal`: consumers do not implement or reference it
 * directly.
 */
internal interface BeepingEncoder {
    /**
     * Encode [key] (5 base32 chars `[0-9a-v]`) to PCM frames ready for playback.
     *
     * @return raw 16-bit PCM mono bytes at 44100 Hz, including a RIFF/WAVE
     *   header (the cloud endpoint returns a complete WAV blob; the local
     *   encoder is expected to do the same).
     * @throws IllegalArgumentException if the key doesn't match `^[0-9a-v]{5}$`.
     * @throws BeepingException for typed errors (auth, rate limit, network).
     * @throws NotImplementedError until the corresponding native function or
     *   service endpoint is available.
     */
    suspend fun encode(key: String): ByteArray

    /**
     * BEE-2240: encode [key] repeated as N beeps scheduled across [duration]
     * seconds, starting at [startTime], one beep every [interval] seconds.
     *
     * Local mode: backed by `BEEPING_EncodeWithSchedule` in `beeping-core`.
     * Cloud mode: throws [BeepingException] with [BeepingError.SchedulingNotSupported]
     * until the equivalent beepbox endpoint exists.
     *
     * @return raw 16-bit PCM mono WAV bytes at 44100 Hz, ready for [WavPlayer].
     * @param beepGainDb dB gain applied to each beep (clamped to `[-60, +12]`).
     * @param audible if `true`, encode in the audible 3.3-10 kHz band (useful
     *   for QA / debugging). Default `false` = ultrasonic 17.8-21 kHz, the
     *   production setting.
     */
    suspend fun encodeScheduled(
        key: String,
        duration: Float,
        startTime: Float,
        interval: Float,
        beepGainDb: Float,
        audible: Boolean,
    ): ByteArray

    /**
     * Cold flow of decoded beeps. Cancelling the collector stops decoding
     * and releases any active mic / network resources.
     */
    fun decoded(): Flow<BeepingPayload>

    /** Releases all resources. Idempotent. */
    fun close()
}
