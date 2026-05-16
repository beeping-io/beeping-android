package com.beeping.AndroidBeepingCore

import timber.log.Timber

/**
 * JNI bridge from Kotlin to `libbeeping_jni.so` (the BEE-2226 shim), which
 * forwards calls to the beeping-core C API in `libbeepingcore.so`.
 *
 * **Critical**: the JVM signatures of the `external fun`s MUST stay binary-
 * compatible with the `extern "C"` symbols emitted by `beeping_jni.cpp` —
 * the JVM looks methods up by name + JNI signature. Renaming or changing
 * parameter types breaks load at runtime with `UnsatisfiedLinkError`.
 *
 * Threading: this class is **not** internally synchronized. Callers must
 * serialize encode/decode calls on the same handle (LocalEncoder does this
 * by owning a single handle per coroutine context).
 *
 * Decoder return codes (from `BEEPING_DecodeAudioBuffer`):
 * - [DECODE_NO_DATA] (-1): no recognizable signal yet
 * - [DECODE_START_TOKEN] (-2): start token detected
 * - [DECODE_COMPLETE] (-3): a complete word decoded — call [getDecodedData]
 * - any value ≥ 0: a single token index
 */
class BeepingCoreJNI {
    /**
     * Create a beeping-core handle. beeping-core ≥ 0.8.1 emits its logger
     * to `logcat` on Android (BEE-2227), so no filesystem prep is needed
     * from the caller.
     */
    external fun create(): Long

    external fun destroy(handle: Long)

    external fun configure(
        handle: Long,
        mode: Int,
        samplingRate: Float,
        bufferSize: Int,
    ): Int

    /**
     * Encode [payload] into the internal audio buffer of [handle].
     *
     * @param type 0 = pure tones (default), 1 = tones + R2D2 ornament,
     *             2 = melody mode (melody string not exposed in this binding).
     * @return total number of float32 samples generated, or -1 on bad handle.
     */
    external fun encode(
        handle: Long,
        payload: String,
        type: Int,
    ): Int

    /**
     * Drain a chunk of encoded samples into [out]. Call repeatedly until the
     * return value is less than `out.size` — that signals end of the buffer.
     *
     * @return number of samples written to [out].
     */
    external fun readEncodedBuffer(
        handle: Long,
        out: FloatArray,
    ): Int

    /**
     * Feed PCM samples to the decoder.
     *
     * @param size number of valid samples in [pcm] (allows reusing a larger array).
     * @return decode state (see class KDoc).
     */
    external fun decodeBuffer(
        handle: Long,
        pcm: FloatArray,
        size: Int,
    ): Int

    /**
     * Pull the last decoded string. Returns `null` when no data is available
     * **or** when the decoded payload failed integrity checks. A non-null
     * return is always a successfully-decoded payload.
     */
    external fun getDecodedData(handle: Long): String?

    external fun getConfidence(handle: Long): Float

    /**
     * BEE-2240: compute the timestamps of each beep in a `(duration, startTime,
     * interval)` schedule. Pure utility — does not need a handle.
     *
     * Returns `null` on invalid params (e.g. `duration < 2.3`, `interval <= 0`,
     * or `startTime + 2.3 > duration`).
     */
    external fun computeBeepSchedule(
        duration: Float,
        startTime: Float,
        interval: Float,
    ): DoubleArray?

    /**
     * BEE-2240: encode [code] repeated as N beeps scheduled across [duration]
     * seconds. Each beep's payload is `code + 4-char base-32 timestamp` —
     * decoder side can recover the beep's position via the scheduled-payload
     * helpers.
     *
     * @param type 0 = pure tones, 1 = tones + R2D2 (`melody` not exposed).
     * @param beepGainDb dB gain (clamped upstream to `[-60, +12]`).
     * @return a float PCM buffer of `floor(duration * sampleRate)` samples,
     *   or `null` on invalid params / unconfigured handle.
     */
    external fun encodeWithSchedule(
        handle: Long,
        code: String,
        type: Int,
        duration: Float,
        startTime: Float,
        interval: Float,
        beepGainDb: Float,
    ): FloatArray?

    companion object {
        const val DECODE_NO_DATA: Int = -1
        const val DECODE_START_TOKEN: Int = -2
        const val DECODE_COMPLETE: Int = -3

        private const val TAG = "BEEPING:JNI"

        @Volatile
        private var sNativeLoaded: Boolean = false

        init {
            try {
                // beepingcore must be loaded first — beeping_jni links against it.
                System.loadLibrary("beepingcore")
                System.loadLibrary("beeping_jni")
                sNativeLoaded = true
            } catch (e: UnsatisfiedLinkError) {
                Timber.tag(TAG).e(e, "native code library failed to load.")
                sNativeLoaded = false
            }
        }

        @JvmStatic
        fun isNativeLoaded(): Boolean = sNativeLoaded
    }
}
