package com.beeping.AndroidBeepingCore

import android.content.Context
import android.util.Log
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * BEE-2226 — instrumented diagnostic that asserts the entire SDK plumbing
 * (Kotlin → JNI shim → beeping-core C API) is connected and functional on
 * the emulator with the real `.so` files loaded.
 *
 * **What this proves**:
 * - `libbeepingcore.so` + `libbeeping_jni.so` load at runtime.
 * - `BEEPING_Create` + chdir workaround don't SIGABRT.
 * - `BEEPING_Configure` accepts mode/sample-rate/buffer-size and returns ≥ 0.
 * - `BEEPING_EncodeDataToAudioBuffer` produces a non-empty audio buffer for
 *   a valid base32 payload — fingerprint of a real chirp.
 * - `BEEPING_GetEncodedAudioBuffer` drains the buffer cleanly across multiple
 *   calls until exhausted.
 * - `BEEPING_DecodeAudioBuffer` reaches `DECODE_COMPLETE` (-3) when fed back
 *   the encoder's own samples — both halves of the codec agree on the
 *   protocol shape.
 * - `BEEPING_GetDecodedData` is callable on a completed decoder without
 *   crashing.
 * - `BEEPING_GetConfidence` returns a value (possibly NaN under in-process
 *   feed conditions).
 *
 * **What this does NOT assert** (intentionally — tracked as `pending-014`
 * + upstream `BEE-2228`): an exact char-for-char round-trip of the payload.
 * In an in-process direct-feed loopback the encoder + decoder of beeping-core
 * v0.8.0 produce a deterministic-but-mismatched output (e.g. `"abc12"` →
 * `null` after RS integrity check failure). Real-world acoustic capture
 * (microphone → AudioRecord → decoder) does not have this issue because the
 * mic introduces natural noise + AGC that the decoder relies on for token
 * alignment.
 */
@RunWith(AndroidJUnit4::class)
class SdkPlumbingTest {
    @Test
    fun encode_then_decode_pipeline_is_alive_and_reaches_completion() {
        val ctx: Context = ApplicationProvider.getApplicationContext()
        val filesDir = ctx.filesDir.absolutePath

        assertTrue("native libs must load on the emulator", BeepingCoreJNI.isNativeLoaded())
        val jni = BeepingCoreJNI()

        // ── ENCODE ──────────────────────────────────────────────────────────
        val encHandle = jni.create(filesDir)
        assertNotEquals("BEEPING_Create returned null for encoder", 0L, encHandle)
        val cfgEnc = jni.configure(encHandle, MODE_INAUDIBLE, SAMPLE_RATE, BUFFER_SIZE)
        assertTrue("encoder configure failed (rc=$cfgEnc)", cfgEnc >= 0)

        val totalSamples = jni.encode(encHandle, PAYLOAD, ENCODE_TYPE_PURE_TONES)
        assertTrue("encode produced no samples (got $totalSamples)", totalSamples > 0)
        Log.i(TAG, "encoded $totalSamples samples for payload '$PAYLOAD'")

        val samples = FloatArray(totalSamples)
        val chunk = FloatArray(BUFFER_SIZE)
        var written = 0
        while (written < totalSamples) {
            val n = jni.readEncodedBuffer(encHandle, chunk)
            // n <= 0 = empty; n < BUFFER_SIZE = end of internal buffer.
            val copy = minOf(maxOf(n, 0), samples.size - written)
            if (copy > 0) {
                System.arraycopy(chunk, 0, samples, written, copy)
                written += copy
            }
            if (n < BUFFER_SIZE) break
        }
        jni.destroy(encHandle)
        assertTrue("drained 0 samples from the encoder", written > 0)
        assertEquals(
            "drain count must match the encoder's reported total",
            totalSamples,
            written,
        )

        var minSample = Float.POSITIVE_INFINITY
        var maxSample = Float.NEGATIVE_INFINITY
        for (s in samples) {
            if (s < minSample) minSample = s
            if (s > maxSample) maxSample = s
        }
        Log.i(TAG, "sample range = [$minSample, $maxSample]")
        assertTrue(
            "encoder output must be a real audio waveform, not silence",
            maxSample - minSample > 0.1f,
        )

        // ── DECODE ──────────────────────────────────────────────────────────
        // Pre/post pad with silence — the decoder needs a calibration window
        // before the chirp and slack after the last token to fire DECODE_COMPLETE.
        // MODE_ALL accepts both audible + inaudible; empirically MODE_INAUDIBLE
        // alone never reached DECODE_COMPLETE in this in-process feed (upstream
        // codec calibration assumes mic-introduced noise).
        val preSilence = BUFFER_SIZE * 4
        val postSilence = BUFFER_SIZE * 16
        val raw = preSilence + written + postSilence
        val rounded = ((raw + BUFFER_SIZE - 1) / BUFFER_SIZE) * BUFFER_SIZE
        val padded = FloatArray(rounded)
        System.arraycopy(samples, 0, padded, preSilence, written)

        val decHandle = jni.create(filesDir)
        assertNotEquals("BEEPING_Create returned null for decoder", 0L, decHandle)
        val cfgDec = jni.configure(decHandle, MODE_ALL, SAMPLE_RATE, BUFFER_SIZE)
        assertTrue("decoder configure failed (rc=$cfgDec)", cfgDec >= 0)

        var sawStartToken = false
        var sawComplete = false
        var positiveTokenCount = 0
        var firstCompleteAt = -1
        var i = 0
        val pcmChunk = FloatArray(BUFFER_SIZE)
        while (i < padded.size) {
            System.arraycopy(padded, i, pcmChunk, 0, BUFFER_SIZE)
            val state = jni.decodeBuffer(decHandle, pcmChunk, BUFFER_SIZE)
            when {
                state == BeepingCoreJNI.DECODE_START_TOKEN -> sawStartToken = true
                state == BeepingCoreJNI.DECODE_COMPLETE -> {
                    if (!sawComplete) firstCompleteAt = i
                    sawComplete = true
                }
                state > 0 -> positiveTokenCount++
            }
            i += BUFFER_SIZE
        }

        // getDecodedData on a completed decoder must NOT crash, even if it
        // returns null (integrity failure under in-process feed — upstream).
        val decoded =
            if (sawComplete) {
                jni.getDecodedData(decHandle)
            } else {
                null
            }
        val confidence = jni.getConfidence(decHandle)
        Log.i(
            TAG,
            "decode result: start=$sawStartToken, complete=$sawComplete @ $firstCompleteAt, " +
                "tokens=$positiveTokenCount, getDecodedData='$decoded', confidence=$confidence",
        )

        jni.destroy(decHandle)

        // ── ASSERT: SDK plumbing alive ──────────────────────────────────────
        assertTrue(
            "decoder must detect the encoder's start token (proves spectral " +
                "analysis sees the chirp)",
            sawStartToken,
        )
        assertTrue(
            "decoder must decode at least one individual token (proves the " +
                "spectral grid matches between encoder and decoder)",
            positiveTokenCount > 0,
        )
        assertTrue(
            "decoder must reach DECODE_COMPLETE (proves the full codec " +
                "state machine + Reed-Solomon path is exercised end-to-end)",
            sawComplete,
        )
        // confidence may be NaN under in-process feed (RS check failed);
        // the call itself must not crash, which already passed if we got here.
    }

    private companion object {
        private const val TAG = "BEE-2226-plumbing"
        private const val PAYLOAD = "abc12"
        private const val MODE_INAUDIBLE = 3
        private const val MODE_ALL = 5
        private const val ENCODE_TYPE_PURE_TONES = 0
        private const val SAMPLE_RATE = 44_100f
        private const val BUFFER_SIZE = 4096
    }
}
