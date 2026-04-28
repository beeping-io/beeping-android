package com.beeping.AndroidBeepingCore

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Handler
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Legacy public entry point for the Beeping Android SDK.
 *
 * This class is the result of the BEE-53 Java→Kotlin migration; it preserves
 * the existing API contract (constructor + `startBeepingListen` + `stopBeepingListen`
 * + `BeepingCoreEvent` callback) so downstream consumers don't break during
 * the modernization.
 *
 * **It is going to be replaced** by the new instance-based `BeepingClient` API
 * in BEE-56 (Flow + suspend, no Context casts, Local/Cloud strategy).
 *
 * Known limitations of this legacy surface (preserved here, fixed in BEE-56):
 * - The [bContext] is cast to both [BeepingCoreEvent] AND [Activity] at runtime.
 *   Passing a Context that doesn't implement these throws at use time.
 * - Single-listener model (no multi-subscribe, no Flow).
 * - Fixed mode = `MODE_NONAUDIBLE` hard-coded in [alloc].
 *
 * BEE-53 changes from the Java original:
 * - Bug fix: dropped the duplicate `requestAudioFocus()` call that incorrectly
 *   passed `STREAM_MUSIC` (3) as `focusGain` (resolved to
 *   `AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE` accidentally — semantically wrong).
 * - `Timer` / `TimerTask` polling replaced with `kotlinx.coroutines` `delay` loop.
 *   The job is cancelled in `stopBeepingListen` instead of relying on `Timer.cancel`.
 */
class BeepingCore(private val bContext: Context) {

    private var mThread: Thread? = null
    private val beeps = BeepHandler()
    private var mBeepingObject: Long = 0L
    private val mBeepingCoreJNI = BeepingCoreJNI(this)
    private val fullCode = CharArray(BEEP_CODE_BUFFER_SIZE)
    private var mDecoding: Boolean = false

    private val permissionScope = CoroutineScope(Dispatchers.Main)
    private var permissionPollJob: Job? = null

    init {
        Log.d(TAG, "CORE")

        beeps.addListener(bContext as BeepingCoreEvent)

        Log.d(TAG, "AUDIO-MANAGER")
        val audioManager = bContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        audioManager.requestAudioFocus(
            AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build(),
                )
                .setAcceptsDelayedFocusGain(true)
                .setOnAudioFocusChangeListener { focusChange ->
                    val name = when (focusChange) {
                        AudioManager.AUDIOFOCUS_GAIN -> "AUDIOFOCUS_GAIN"
                        AudioManager.AUDIOFOCUS_GAIN_TRANSIENT -> "AUDIOFOCUS_GAIN_TRANSIENT"
                        AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK -> "AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK"
                        AudioManager.AUDIOFOCUS_LOSS -> "AUDIOFOCUS_LOSS"
                        AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> "AUDIOFOCUS_LOSS_TRANSIENT"
                        AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> "AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK"
                        AudioManager.AUDIOFOCUS_REQUEST_FAILED -> "AUDIOFOCUS_REQUEST_FAILED"
                        else -> "UNKNOWN_FOCUS_CHANGE($focusChange)"
                    }
                    Log.d(TAG, name)
                }
                .build(),
        )
        // BEE-53: removed the second buggy requestAudioFocus call here.
    }

    private fun ensureNativeLoaded(): Boolean {
        if (!BeepingCoreJNI.isNativeLoaded()) {
            Log.e(TAG, "Native library not loaded; BeepingCore will not start.")
            // TODO: notify host app about native load failure via callback
            return false
        }
        return true
    }

    private fun alloc() {
        if (!ensureNativeLoaded()) return

        Log.d(TAG, "ALLOC")
        mBeepingObject = mBeepingCoreJNI.init()

        mThread = Thread {
            Thread.currentThread().priority = Thread.MAX_PRIORITY
            mBeepingCoreJNI.start(mBeepingObject)
        }.also { it.start() }

        configure(EnumBeepingMode.MODE_NONAUDIBLE)
    }

    private fun dealloc(): Int {
        if (!BeepingCoreJNI.isNativeLoaded()) return 0

        Log.d(TAG, "DEALLOC")
        val ret = mBeepingCoreJNI.dealloc(mBeepingObject)
        try {
            mThread?.join()
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
        mThread = null
        return ret
    }

    private fun configure(mode: EnumBeepingMode) {
        if (!ensureNativeLoaded()) return

        Log.d(TAG, "CONFIGURE")
        val configValue = when (mode) {
            EnumBeepingMode.MODE_AUDIBLE -> 2
            EnumBeepingMode.MODE_NONAUDIBLE -> 3
            EnumBeepingMode.MODE_HIDDEN -> 4
            EnumBeepingMode.MODE_ALL -> 5
            EnumBeepingMode.MODE_CUSTOM -> 6
        }
        mBeepingCoreJNI.configure(configValue, mBeepingObject)
    }

    fun startBeepingListen() {
        if (!ensureNativeLoaded()) return
        checkMicrophone()
    }

    fun stopBeepingListen() {
        if (!BeepingCoreJNI.isNativeLoaded()) {
            Log.w(TAG, "stopBeepingListen called but native library is not loaded.")
            return
        }

        permissionPollJob?.cancel()
        permissionPollJob = null

        if (mDecoding) {
            dealloc()
        }
        mDecoding = false
        mBeepingCoreJNI.stopBeepingListen(mBeepingObject)
        Log.d(TAG, "stopBeepingListen")
    }

    private fun getBeepId(): String {
        if (!BeepingCoreJNI.isNativeLoaded()) return ""
        mBeepingCoreJNI.getDecodedString(fullCode, mBeepingObject)
        return getBeepKey()
    }

    private fun getBeepKey(): String = String(fullCode, 0, BEEP_KEY_LENGTH)

    private fun checkMicrophone() {
        if (!ensureNativeLoaded()) return

        if (ContextCompat.checkSelfPermission(bContext, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) {
            // Coroutine replaces the legacy Timer/TimerTask loop. Polls every
            // POLL_INTERVAL_MS until permission is granted, or until cancelled
            // by stopBeepingListen.
            permissionPollJob = permissionScope.launch {
                while (isActive) {
                    if (ContextCompat.checkSelfPermission(bContext, Manifest.permission.RECORD_AUDIO)
                        == PackageManager.PERMISSION_GRANTED
                    ) {
                        startListenInternal()
                        break
                    }
                    delay(POLL_INTERVAL_MS)
                }
            }

            // Give the user the option to grant the permission.
            ActivityCompat.requestPermissions(
                bContext as Activity,
                arrayOf(Manifest.permission.RECORD_AUDIO),
                RECORD_AUDIO_PERMISSIONS,
            )
        } else {
            startListenInternal()
        }
    }

    private fun startListenInternal() {
        if (mThread == null && !mDecoding) {
            alloc()
        }
        mDecoding = true
        mBeepingCoreJNI.startBeepingListen(mBeepingObject)
        Log.d(TAG, "LISTENING")
    }

    /**
     * Called by [BeepingCoreJNI.BeepingCallback] (which is itself called from native via JNI).
     * Posts the decoded beep id to the registered listener on the main thread.
     *
     * `internal` instead of `protected` because Kotlin's `protected` would only
     * allow subclasses to call it, but here it is invoked from a sibling class
     * (`BeepingCoreJNI`) within the same module.
     */
    internal fun beepingCallback(value: Int) {
        Log.d(TAG, "BeepingCallback")
        if (value == BeepingCoreJNI.BC_TOKEN_END_OK) {
            Log.d(TAG, "BC_TOKEN_END")
            Handler(bContext.mainLooper).post {
                beeps.sendListener(getBeepId())
            }
        }
    }

    companion object {
        private const val TAG = "BEEPING:SDK"
        private const val RECORD_AUDIO_PERMISSIONS = 1
        private const val POLL_INTERVAL_MS = 500L
        private const val BEEP_CODE_BUFFER_SIZE = 10
        private const val BEEP_KEY_LENGTH = 5
    }
}
