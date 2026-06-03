package com.beeping.AndroidBeepingCore

import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build

/**
 * Wraps [AudioManager] audio-focus handling for a single [LocalEncoder.decoded]
 * listening session.
 *
 * On [request] it asks for audio focus and registers a change listener; the
 * first time the system reports a **real** loss of focus — an incoming call,
 * the voice assistant, or another app grabbing the mic — it invokes `onLoss`.
 * `LocalEncoder` surfaces that as [BeepingError.AudioFocusLost], which
 * [BeepingClient.listen] maps to [BeepingEvent.Failed] followed by
 * [BeepingEvent.Stopped]. The loss is treated as **terminal** for the session:
 * the host re-arms by collecting [BeepingClient.listen] again once focus is
 * regained.
 *
 * `AudioFocusRequest` is API 26+. On API 24–25 (the library `minSdk`) the
 * deprecated [AudioManager.requestAudioFocus] listener overload is used.
 *
 * Not thread-safe — owned by a single decode session.
 *
 * `internal` — wired implicitly by [LocalEncoder]; never part of the public API.
 */
internal class AudioFocusGuard(
    private val audioManager: AudioManager,
) {
    private var focusRequest: AudioFocusRequest? = null
    private var legacyListener: AudioManager.OnAudioFocusChangeListener? = null

    /**
     * Requests audio focus and registers [onLoss], invoked once per
     * `AUDIOFOCUS_LOSS` / `AUDIOFOCUS_LOSS_TRANSIENT` reported by the system.
     *
     * @return the raw `requestAudioFocus` result
     *   (`AUDIOFOCUS_REQUEST_GRANTED` / `_FAILED` / `_DELAYED`).
     */
    fun request(onLoss: () -> Unit): Int {
        val listener =
            AudioManager.OnAudioFocusChangeListener { focusChange ->
                if (isFocusLoss(focusChange)) {
                    onLoss()
                }
            }

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val attributes =
                AudioAttributes
                    .Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_UNKNOWN)
                    .build()
            val req =
                AudioFocusRequest
                    .Builder(AudioManager.AUDIOFOCUS_GAIN)
                    .setAudioAttributes(attributes)
                    .setOnAudioFocusChangeListener(listener)
                    .build()
            focusRequest = req
            audioManager.requestAudioFocus(req)
        } else {
            legacyListener = listener
            @Suppress("DEPRECATION")
            audioManager.requestAudioFocus(
                listener,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN,
            )
        }
    }

    /** Abandons any held focus and releases the listener. Idempotent. */
    fun abandon() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            focusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
            focusRequest = null
        } else {
            legacyListener?.let {
                @Suppress("DEPRECATION")
                audioManager.abandonAudioFocus(it)
            }
            legacyListener = null
        }
    }

    private companion object {
        /**
         * `true` for the two focus-change codes that mean another app took over
         * the audio path mid-session. `AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK` is
         * deliberately excluded — ducking does not interrupt mic capture.
         */
        fun isFocusLoss(focusChange: Int): Boolean =
            focusChange == AudioManager.AUDIOFOCUS_LOSS ||
                focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT
    }
}
