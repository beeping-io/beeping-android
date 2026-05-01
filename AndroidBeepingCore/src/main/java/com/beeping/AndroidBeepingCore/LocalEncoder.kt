package com.beeping.AndroidBeepingCore

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.emptyFlow

/**
 * On-device implementation of [BeepingEncoder] backed by the JNI bridge to
 * `libbeepingcore.so`.
 *
 * **Status (BEE-58)**:
 *
 * - [encode] **NOT implemented** — depends on a future encoder native function
 *   that will land with `beeping-core` Phase 1 + BEE-65. Throws
 *   `NotImplementedError("BEE-65")` for now (after key validation).
 * - [decoded] checks `RECORD_AUDIO` permission first; if denied, throws
 *   [BeepingException] with [BeepingError.MissingMicPermission] so
 *   [BeepingClient.listen] can surface it as [BeepingEvent.Failed]. Otherwise
 *   wires the JNI callback to a cold [Flow]. The full audio session
 *   orchestration (`AudioManager.requestAudioFocus`, `AudioRecord` sample-rate
 *   negotiation) still lives inside `libbeepingcore.so` — the Kotlin side
 *   only owns the JNI lifecycle.
 *
 * The library does NOT request `RECORD_AUDIO` permission itself — that
 * couples the SDK to UI code. The host Activity must call
 * `ActivityCompat.requestPermissions` before collecting [BeepingClient.listen].
 *
 * `internal` — consumers select this implicitly via [BeepingMode.Local].
 */
internal class LocalEncoder(
    private val context: Context,
    private val jni: BeepingCoreJNI = BeepingCoreJNI(),
    @Suppress("unused") traceId: String = "anon",
) : BeepingEncoder {
    override suspend fun encode(key: String): ByteArray {
        require(key.matches(KEY_PATTERN)) {
            "Key must match the 5-char base32 pattern $KEY_PATTERN_STR (got '$key')"
        }
        @Suppress("ImplicitNothingReturn")
        TODO("BEE-65 — LocalEncoder.encode() requires the encoder native function from beeping-core")
    }

    override fun decoded(): Flow<BeepingPayload> =
        callbackFlow {
            // Permission must be granted by the host Activity BEFORE collecting
            // listen(). Failing fast lets BeepingClient.listen() surface a
            // typed BeepingEvent.Failed(MissingMicPermission) via its catch{}.
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED
            ) {
                throw BeepingException(BeepingError.MissingMicPermission)
            }

            if (!BeepingCoreJNI.isNativeLoaded()) {
                // Native lib unavailable — surface via the same BeepingException
                // path so BeepingClient.listen() emits a typed Failed event.
                throw BeepingException(BeepingError.NativeLibraryNotLoaded)
            }

            val beepingObject = jni.init()
            val nativeThread =
                Thread {
                    Thread.currentThread().priority = Thread.MAX_PRIORITY
                    jni.start(beepingObject)
                }.also { it.start() }

            jni.callback = { value ->
                if (value == BeepingCoreJNI.BC_TOKEN_END_OK) {
                    val buf = CharArray(BEEP_BUFFER_SIZE)
                    jni.getDecodedString(buf, beepingObject)
                    val decoded = String(buf).trimEnd(' ')
                    trySend(BeepingPayload(payload = decoded))
                }
            }

            jni.configure(MODE_NONAUDIBLE_JNI, beepingObject)
            jni.startBeepingListen(beepingObject)

            awaitClose {
                jni.stopBeepingListen(beepingObject)
                jni.callback = null
                jni.dealloc(beepingObject)
                try {
                    nativeThread.join(JOIN_TIMEOUT_MS)
                } catch (e: InterruptedException) {
                    Thread.currentThread().interrupt()
                }
            }
        }

    override fun close() {
        jni.callback = null
    }

    companion object {
        private val KEY_PATTERN = Regex("^[0-9a-v]{5}$")
        private const val KEY_PATTERN_STR = "^[0-9a-v]{5}\$"

        private const val MODE_NONAUDIBLE_JNI = 3

        private const val BEEP_BUFFER_SIZE = 10
        private const val JOIN_TIMEOUT_MS = 1000L

        @Suppress("unused")
        internal fun emptyDecodedFlow(): Flow<BeepingPayload> = emptyFlow()
    }
}
