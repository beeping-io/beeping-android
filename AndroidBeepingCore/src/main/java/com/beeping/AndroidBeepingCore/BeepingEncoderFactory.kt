package com.beeping.AndroidBeepingCore

import android.content.Context

/**
 * Factory that picks the right [BeepingEncoder] for the given [BeepingMode].
 *
 * Used by (BEE-58) `BeepingClient.Builder.build()` — `internal` so the
 * mapping mode→encoder is not part of the public API surface.
 */
internal object BeepingEncoderFactory {

    /**
     * Returns a fresh [BeepingEncoder] instance configured for [mode].
     *
     * - [BeepingMode.Local] → [LocalEncoder] backed by [BeepingCoreJNI].
     * - [BeepingMode.Cloud] → [CloudEncoder] backed by Ktor + the configured
     *   API key + endpoint.
     */
    fun create(mode: BeepingMode, context: Context): BeepingEncoder = when (mode) {
        is BeepingMode.Local -> LocalEncoder(context = context)
        is BeepingMode.Cloud -> CloudEncoder(
            apiKey = mode.apiKey,
            endpoint = mode.endpoint,
        )
    }
}
