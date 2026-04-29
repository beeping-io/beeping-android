package com.beeping.AndroidBeepingCore

import android.util.Log

/**
 * JNI bridge to `libbeepingcore.so`.
 *
 * **Critical**: the JVM signatures of the native methods + the [BeepingCallback]
 * method MUST stay binary-compatible with the C symbols in the native library.
 * The native lib looks up methods by name + JVM signature; renaming or changing
 * parameter types will break the load at runtime.
 *
 * The corresponding native symbols (descriptive — see the C source):
 * ```
 * Java_com_beeping_AndroidBeepingCore_BeepingCoreJNI_init        ()J
 * Java_com_beeping_AndroidBeepingCore_BeepingCoreJNI_start       (J)V
 * Java_com_beeping_AndroidBeepingCore_BeepingCoreJNI_dealloc     (J)I
 * Java_..._configure          (IJ)I
 * Java_..._startBeepingListen (J)I
 * Java_..._stopBeepingListen  (J)I
 * Java_..._getDecodedString   ([CJ)I
 * ```
 *
 * BEE-56 refactor: the constructor no longer takes a `BeepingCore` ref (the
 * legacy class was removed); the callback is now a settable lambda field that
 * the [Encoder] strategy (LocalEncoder, BEE-57) wires up before starting a
 * listen session.
 */
class BeepingCoreJNI {

    /**
     * Receiver of native callbacks. Set by [Encoder] implementations (BEE-57
     * `LocalEncoder`) before starting a listen session; cleared on stop.
     *
     * Receives one of [BC_TOKEN_START], [BC_TOKEN_END_OK], [BC_TOKEN_END_BAD],
     * [BC_END_PLAY].
     */
    @Volatile
    var callback: ((Int) -> Unit)? = null

    /**
     * Called from native via JNI (`(I)V`). DO NOT rename or change signature.
     * Dispatches the token to the registered [callback] (if any).
     */
    fun BeepingCallback(value: Int) {
        when (value) {
            BC_TOKEN_START,
            BC_TOKEN_END_OK,
            BC_TOKEN_END_BAD,
            BC_END_PLAY,
            -> callback?.invoke(value)
        }
    }

    external fun start(beepingObject: Long)
    external fun init(): Long
    external fun dealloc(beepingObject: Long): Int
    external fun configure(mode: Int, beepingObject: Long): Int
    external fun startBeepingListen(beepingObject: Long): Int
    external fun stopBeepingListen(beepingObject: Long): Int
    external fun getDecodedString(code: CharArray, beepingObject: Long): Int

    companion object {
        const val BC_TOKEN_START: Int = 0
        const val BC_TOKEN_END_OK: Int = 1
        const val BC_TOKEN_END_BAD: Int = 2
        const val BC_END_PLAY: Int = 3

        private const val TAG = "BEEPING:JNI"

        @Volatile
        private var sNativeLoaded: Boolean = false

        init {
            try {
                System.loadLibrary("beepingcore")
                sNativeLoaded = true
            } catch (e: UnsatisfiedLinkError) {
                Log.e(TAG, "native code library failed to load.", e)
                sNativeLoaded = false
            }
        }

        @JvmStatic
        fun isNativeLoaded(): Boolean = sNativeLoaded
    }
}
