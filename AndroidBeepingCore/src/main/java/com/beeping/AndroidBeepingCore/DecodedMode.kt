package com.beeping.AndroidBeepingCore

/**
 * The frequency band a payload was actually decoded in, as classified by
 * `beeping-core` (`BEEPING_GetDecodedMode`) — BEE-2313.
 *
 * This is the decode-time **result** (what the decoder detected), distinct from
 * the [BeepingEncodingMode] *configure* input. It is the home of "hidden", which
 * `beeping-core` v0.8.1 reports as a classification but does not expose as a
 * selectable encode band (see BEE-2305).
 *
 * @property rawValue The integer returned by `BEEPING_GetDecodedMode`.
 */
enum class DecodedMode(
    val rawValue: Int,
) {
    AUDIBLE(0),
    NON_AUDIBLE(1),
    HIDDEN(2),

    /** Returned when the native value is unrecognized (e.g. no decode yet). */
    UNKNOWN(-1),
    ;

    companion object {
        /** Maps a `BEEPING_GetDecodedMode` value to a [DecodedMode]; [UNKNOWN] if unmapped. */
        fun fromRaw(rawValue: Int): DecodedMode = entries.firstOrNull { it.rawValue == rawValue } ?: UNKNOWN
    }
}
