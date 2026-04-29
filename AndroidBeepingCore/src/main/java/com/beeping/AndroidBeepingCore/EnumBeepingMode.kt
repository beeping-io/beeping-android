package com.beeping.AndroidBeepingCore

/**
 * Audio frequency profile passed to the JNI `configure(mode, ...)` native call.
 *
 * **Different from the public [BeepingMode]** which selects Local-vs-Cloud
 * runtime. This enum selects the audible/ultrasonic frequency band used by
 * the on-device decoder.
 *
 * `internal` because consumers don't pick this directly — the encoder
 * strategies (BEE-57 `LocalEncoder`) configure the JNI bridge with whichever
 * value is appropriate.
 */
internal enum class EnumBeepingMode(val value: Int) {
    MODE_AUDIBLE(0),
    MODE_NONAUDIBLE(1),
    MODE_HIDDEN(2),
    MODE_ALL(3),
    MODE_CUSTOM(4),
}
