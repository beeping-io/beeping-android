package com.beeping.AndroidBeepingCore

import android.content.Context
import android.media.MediaPlayer
import androidx.core.net.toUri
import java.io.File
import java.util.concurrent.atomic.AtomicReference

/**
 * Plays a WAV byte array via [MediaPlayer]. Internal helper used by
 * [BeepingClient.send] to emit the encoded audio after [BeepingEncoder.encode]
 * returns the WAV bytes.
 *
 * The bytes are written to a single rotating file under `cacheDir` and played
 * from a `file://` URI. We rotate the same path because Android's MediaPlayer
 * does not accept raw byte arrays from memory — going through a file is the
 * simplest reliable path that works on every API level since 24 (our minSdk).
 *
 * Concurrency: only one playback at a time; calling [play] while a previous
 * playback is in flight resets and replaces it.
 *
 * `internal` — consumers reach this only via [BeepingClient.send].
 */
internal class WavPlayer(
    private val context: Context,
) {
    private val current = AtomicReference<MediaPlayer?>()

    fun play(wav: ByteArray) {
        current.getAndSet(null)?.runCatching { release() }

        val file = File(context.cacheDir, CACHE_FILE_NAME)
        file.writeBytes(wav)

        val player =
            MediaPlayer().apply {
                setDataSource(context, file.toUri())
                setOnCompletionListener { runCatching { release() } }
                setOnErrorListener { mp, _, _ ->
                    runCatching { mp.release() }
                    true
                }
                prepare()
                start()
            }
        current.set(player)
    }

    fun close() {
        current.getAndSet(null)?.runCatching { release() }
    }

    private companion object {
        private const val CACHE_FILE_NAME = "beeping-send.wav"
    }
}
