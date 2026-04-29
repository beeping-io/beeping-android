package com.beeping.AndroidBeepingCore

import kotlinx.coroutines.flow.Flow

/**
 * Strategy interface for the encode/decode pipeline.
 *
 * Two implementations land in BEE-57:
 *
 * - `LocalEncoder`: wraps the JNI bridge to `libbeepingcore.so`.
 * - `CloudEncoder`: HTTP client for `beepbox-server` via Ktor.
 *
 * The interface is `internal`: consumers do not implement or reference it
 * directly — selection is done via [BeepingMode] at construction time.
 */
internal interface Encoder {

    /**
     * Encode [payload] to PCM frames ready for playback.
     *
     * @return raw 16-bit PCM mono bytes at the encoder's native sample rate.
     */
    suspend fun encode(payload: String): ByteArray

    /**
     * Cold flow of decoded beeps. Cancelling the collector stops decoding
     * and releases any active mic / network resources.
     */
    fun decoded(): Flow<BeepingPayload>

    /** Releases all resources. Idempotent. */
    fun close()
}
