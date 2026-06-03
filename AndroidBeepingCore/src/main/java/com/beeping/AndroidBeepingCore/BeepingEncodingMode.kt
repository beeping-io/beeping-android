package com.beeping.AndroidBeepingCore

/**
 * Audio frequency profile selected via [BeepingClient.Builder.encodingMode]
 * (BEE-2305). Applies to both [BeepingClient.send] (encode band) and
 * [BeepingClient.listen] (decode band).
 *
 * [rawValue] mirrors `beeping-core`'s `BEEPING_MODE` enum
 * (`BeepingCoreLib_api.h`) **exactly** — the [BeepingEncodingModeTest] drift
 * guard fails the build if they diverge.
 *
 * Note: `beeping-core` v0.8.1 exposes three *configure* modes. "Hidden" is not
 * a selectable encode/decode band — it is a decode-time classification surfaced
 * by `BEEPING_GetDecodedMode` (BEE-2313 reception metrics), not an encoding mode.
 *
 * @property rawValue The `BEEPING_MODE_*` integer passed to the native
 *   `configure(mode, …)` call.
 */
enum class BeepingEncodingMode(
    val rawValue: Int,
) {
    /** Audible tones, 3.3–10 kHz. `BEEPING_MODE_AUDIBLE`. */
    AUDIBLE(2),

    /** Inaudible (ultrasonic) tones, 17.8–21 kHz. `BEEPING_MODE_INAUDIBLE`. */
    NON_AUDIBLE(3),

    /** Decode audible + inaudible simultaneously. `BEEPING_MODE_ALL`. */
    ALL(5),
    ;

    /**
     * Mode passed to `configure()` when **encoding** (`send`). [ALL] is a
     * decode-only band, so it falls back to [NON_AUDIBLE] (the production
     * ultrasonic default) — preserving the pre-BEE-2305 `send` behaviour.
     */
    internal val encodeConfigureMode: Int
        get() = if (this == ALL) NON_AUDIBLE.rawValue else rawValue

    /** Mode passed to `configure()` when **decoding** (`listen`) — used directly. */
    internal val decodeConfigureMode: Int
        get() = rawValue
}
