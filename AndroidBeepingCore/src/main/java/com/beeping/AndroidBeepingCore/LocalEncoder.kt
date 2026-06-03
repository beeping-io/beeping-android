package com.beeping.AndroidBeepingCore

import android.Manifest
import android.content.Context
import android.content.Context.AUDIO_SERVICE
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * On-device implementation of [BeepingEncoder] backed by the JNI shim
 * `libbeeping_jni.so` (BEE-2226) over the beeping-core C API.
 *
 * [encode] generates an INAUDIBLE (ultrasonic ~17-21 kHz) WAV byte array from
 * a 5-char base32 key, ready to feed [WavPlayer.play].
 *
 * [decoded] opens an [AudioRecord] in `MODE_ALL` (audible + inaudible) and
 * forwards PCM chunks to `decodeBuffer`. Each `DECODE_COMPLETE` state pulls
 * the decoded payload via `getDecodedData` and emits it.
 *
 * The host Activity must grant `RECORD_AUDIO` before collecting [decoded].
 * If denied, the flow fails fast with [BeepingError.MissingMicPermission].
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
        if (!BeepingCoreJNI.isNativeLoaded()) {
            throw BeepingException(BeepingError.NativeLibraryNotLoaded)
        }

        val handle = jni.create()
        check(handle != 0L) { "BEEPING_Create returned null handle" }

        try {
            val cfg = jni.configure(handle, BEEPING_MODE_INAUDIBLE, SAMPLE_RATE.toFloat(), BUFFER_SIZE)
            check(cfg >= 0) { "BEEPING_Configure failed (rc=$cfg)" }

            val total = jni.encode(handle, key, ENCODE_TYPE_PURE_TONES)
            check(total > 0) { "BEEPING_EncodeDataToAudioBuffer returned $total" }

            val drained = drainEncodedSamples(handle, total)
            check(drained.isNotEmpty()) { "drained 0 samples after encode" }
            return floatPcmToWavBytes(drained, SAMPLE_RATE)
        } finally {
            jni.destroy(handle)
        }
    }

    override suspend fun encodeScheduled(
        key: String,
        duration: Float,
        startTime: Float,
        interval: Float,
        beepGainDb: Float,
        audible: Boolean,
    ): ByteArray {
        require(key.matches(KEY_PATTERN)) {
            "Key must match the 5-char base32 pattern $KEY_PATTERN_STR (got '$key')"
        }
        if (!BeepingCoreJNI.isNativeLoaded()) {
            throw BeepingException(BeepingError.NativeLibraryNotLoaded)
        }

        val handle = jni.create()
        check(handle != 0L) { "BEEPING_Create returned null handle" }

        try {
            val mode = if (audible) BEEPING_MODE_AUDIBLE else BEEPING_MODE_INAUDIBLE
            val cfg = jni.configure(handle, mode, SAMPLE_RATE.toFloat(), BUFFER_SIZE)
            check(cfg >= 0) { "BEEPING_Configure failed (rc=$cfg)" }

            val pcm =
                jni.encodeWithSchedule(handle, key, ENCODE_TYPE_PURE_TONES, duration, startTime, interval, beepGainDb)
                    ?: error(
                        "BEEPING_EncodeWithSchedule returned null " +
                            "(duration=$duration startTime=$startTime interval=$interval)",
                    )
            check(pcm.isNotEmpty()) { "scheduled encode produced 0 samples" }
            return floatPcmToWavBytes(pcm, SAMPLE_RATE)
        } finally {
            jni.destroy(handle)
        }
    }

    private fun drainEncodedSamples(
        handle: Long,
        total: Int,
    ): FloatArray {
        val samples = FloatArray(total)
        val chunk = FloatArray(BUFFER_SIZE)
        var written = 0
        while (written < total) {
            val n = jni.readEncodedBuffer(handle, chunk)
            // n <= 0 = empty; n < BUFFER_SIZE = end of internal buffer.
            // Either way, the next readEncodedBuffer would return <= 0
            // and we drop out of the loop on the next iteration — but to
            // avoid a wasted JNI call, return immediately.
            val copy = minOf(maxOf(n, 0), samples.size - written)
            if (copy > 0) {
                System.arraycopy(chunk, 0, samples, written, copy)
                written += copy
            }
            if (n < BUFFER_SIZE) return if (written == total) samples else samples.copyOf(written)
        }
        return samples
    }

    override fun decoded(): Flow<BeepingPayload> =
        callbackFlow {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED
            ) {
                throw BeepingException(BeepingError.MissingMicPermission)
            }
            if (!BeepingCoreJNI.isNativeLoaded()) {
                throw BeepingException(BeepingError.NativeLibraryNotLoaded)
            }

            val handle = jni.create()
            check(handle != 0L) { "BEEPING_Create returned null handle" }

            val record = openAudioRecord()

            val cfg = jni.configure(handle, BEEPING_MODE_ALL, SAMPLE_RATE.toFloat(), BUFFER_SIZE)
            if (cfg < 0) {
                record.release()
                jni.destroy(handle)
                error("BEEPING_Configure failed (rc=$cfg)")
            }

            record.startRecording()

            // BEE-2307: emit BeepingError.AudioFocusLost on a real loss of audio
            // focus (incoming call, assistant, another app grabbing the mic).
            // Terminal for this session — close the flow with the typed cause so
            // BeepingClient.listen() surfaces it as Failed(AudioFocusLost) + Stopped.
            val focusGuard = AudioFocusGuard(context.getSystemService(AUDIO_SERVICE) as AudioManager)
            focusGuard.request {
                close(BeepingException(BeepingError.AudioFocusLost))
            }

            val readerJob =
                launch(Dispatchers.IO) {
                    val shortBuf = ShortArray(BUFFER_SIZE)
                    val floatBuf = FloatArray(BUFFER_SIZE)
                    while (isActive) {
                        val n = record.read(shortBuf, 0, shortBuf.size)
                        if (n <= 0) continue
                        for (i in 0 until n) {
                            floatBuf[i] = shortBuf[i] / SHORT_TO_FLOAT_DIVISOR
                        }
                        val state = jni.decodeBuffer(handle, floatBuf, n)
                        if (state == BeepingCoreJNI.DECODE_COMPLETE) {
                            jni.getDecodedData(handle)?.let { payload ->
                                trySend(BeepingPayload(payload = payload.trimEnd(' ')))
                            }
                        }
                    }
                }

            awaitClose {
                focusGuard.abandon()
                readerJob.cancel()
                runCatching { record.stop() }
                runCatching { record.release() }
                jni.destroy(handle)
            }
        }

    /**
     * Opens a MONO 16-bit PCM [AudioRecord] on the mic at [SAMPLE_RATE], sized
     * to the larger of the platform minimum and four JNI buffers. The caller
     * must have verified `RECORD_AUDIO` before invoking.
     */
    @Suppress("MissingPermission") // RECORD_AUDIO checked by decoded() before this call
    private fun openAudioRecord(): AudioRecord {
        val minBufferBytes =
            AudioRecord.getMinBufferSize(
                SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
            )
        val recordBufferBytes = maxOf(minBufferBytes, BUFFER_SIZE * Short.SIZE_BYTES * 4)
        return AudioRecord(
            MediaRecorder.AudioSource.MIC,
            SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            recordBufferBytes,
        )
    }

    override fun close() {
        // No persistent state — handles are owned per-encode and per-decode session.
    }

    private fun floatPcmToWavBytes(
        samples: FloatArray,
        sampleRate: Int,
    ): ByteArray {
        val numSamples = samples.size
        val dataSize = numSamples * Short.SIZE_BYTES
        val out = ByteArrayOutputStream(WAV_HEADER_SIZE + dataSize)
        val le = ByteBuffer.allocate(WAV_HEADER_SIZE).order(ByteOrder.LITTLE_ENDIAN)

        le.put("RIFF".toByteArray())
        le.putInt(WAV_HEADER_SIZE - WAV_RIFF_PREFIX_SIZE + dataSize)
        le.put("WAVE".toByteArray())
        le.put("fmt ".toByteArray())
        le.putInt(WAV_FMT_CHUNK_SIZE)
        le.putShort(WAV_PCM_FORMAT)
        le.putShort(WAV_MONO_CHANNELS)
        le.putInt(sampleRate)
        le.putInt(sampleRate * Short.SIZE_BYTES) // byte rate
        le.putShort(Short.SIZE_BYTES.toShort()) // block align
        le.putShort(WAV_BITS_PER_SAMPLE)
        le.put("data".toByteArray())
        le.putInt(dataSize)
        out.write(le.array())

        val pcm = ByteBuffer.allocate(dataSize).order(ByteOrder.LITTLE_ENDIAN)
        for (s in samples) {
            val clipped = s.coerceIn(-1f, 1f)
            pcm.putShort((clipped * MAX_PCM_AMPLITUDE).toInt().toShort())
        }
        out.write(pcm.array())
        return out.toByteArray()
    }

    companion object {
        private val KEY_PATTERN = Regex("^[0-9a-v]{5}$")
        private const val KEY_PATTERN_STR = "^[0-9a-v]{5}\$"

        // BEEPING_MODE enum mirror — from BeepingCoreLib_api.h.
        private const val BEEPING_MODE_AUDIBLE = 2
        private const val BEEPING_MODE_INAUDIBLE = 3
        private const val BEEPING_MODE_ALL = 5

        // Encoding type: 0 = pure tones (default). 1 = R2D2 ornament, 2 = melody.
        private const val ENCODE_TYPE_PURE_TONES = 0

        // Sample rate fixed at 44.1 kHz — matches beeping-core's INAUDIBLE band
        // and AudioRecord's universally-supported rate.
        private const val SAMPLE_RATE = 44100

        // Frames exchanged with the JNI shim per encode-drain / decode-feed call.
        private const val BUFFER_SIZE = 4096

        private const val SHORT_TO_FLOAT_DIVISOR = 32768f
        private const val MAX_PCM_AMPLITUDE = 32767f

        // Standard 44-byte WAV header for PCM 16-bit mono.
        private const val WAV_HEADER_SIZE = 44
        private const val WAV_RIFF_PREFIX_SIZE = 8
        private const val WAV_FMT_CHUNK_SIZE = 16
        private const val WAV_PCM_FORMAT: Short = 1
        private const val WAV_MONO_CHANNELS: Short = 1
        private const val WAV_BITS_PER_SAMPLE: Short = 16
    }
}
