package com.beeping.AndroidBeepingCore

import android.content.Context
import java.util.UUID

/**
 * Factory that picks the right [BeepingEncoder] for the given [BeepingMode].
 *
 * Used by `BeepingClient.Builder.build()` (BEE-58) — `internal` so the
 * mapping mode→encoder is not part of the public API surface.
 */
internal object BeepingEncoderFactory {
    /**
     * Returns a fresh [BeepingEncoder] instance configured for [mode].
     *
     * - [BeepingMode.Local] → [LocalEncoder] backed by [BeepingCoreJNI], using
     *   [encodingMode] for the encode/decode band (BEE-2305).
     * - [BeepingMode.Cloud] → [CloudEncoder] backed by Ktor + the configured
     *   API key + endpoint. The [traceId] is added as `X-Trace-Id` header
     *   on every outbound HTTP request. [encodingMode] is **not** plumbed to
     *   beepbox-server yet — Cloud encode uses the server default (pending).
     */
    fun create(
        mode: BeepingMode,
        context: Context,
        traceId: String = UUID.randomUUID().toString().take(TRACE_ID_LEN),
        encodingMode: BeepingEncodingMode = BeepingEncodingMode.ALL,
    ): BeepingEncoder =
        when (mode) {
            is BeepingMode.Local ->
                LocalEncoder(context = context, encodingMode = encodingMode, traceId = traceId)
            is BeepingMode.Cloud ->
                CloudEncoder(
                    apiKey = mode.apiKey,
                    endpoint = mode.endpoint,
                    traceId = traceId,
                )
        }

    private const val TRACE_ID_LEN = 8
}
