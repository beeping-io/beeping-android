package com.beeping.AndroidBeepingCore

import android.media.AudioManager
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * JVM unit tests for [AudioFocusGuard].
 *
 * On the plain JVM test runtime `Build.VERSION.SDK_INT` is 0, so the guard
 * takes the legacy `requestAudioFocus(listener, streamType, durationHint)`
 * path. That lets us capture the registered
 * [AudioManager.OnAudioFocusChangeListener] via a MockK slot and drive
 * `onAudioFocusChange` directly — simulating the system reporting a focus
 * change without an emulator (BEE-2307).
 *
 * The deprecated `requestAudioFocus(listener, …)` / `abandonAudioFocus`
 * overloads are exercised on purpose — they are the API 24–25 path the guard
 * takes when `SDK_INT < 26`, which is exactly the JVM-test runtime.
 */
@Suppress("DEPRECATION")
class AudioFocusGuardTest {
    private val granted = AudioManager.AUDIOFOCUS_REQUEST_GRANTED

    /**
     * Builds a guard over a mocked [AudioManager], requests focus with the
     * given [onLoss], and returns the captured focus-change listener so the
     * test can feed it focus codes.
     */
    private fun requestAndCaptureListener(
        am: AudioManager,
        onLoss: () -> Unit,
    ): AudioManager.OnAudioFocusChangeListener {
        val listenerSlot = slot<AudioManager.OnAudioFocusChangeListener>()
        every { am.requestAudioFocus(capture(listenerSlot), any(), any()) } returns granted
        AudioFocusGuard(am).request(onLoss)
        return listenerSlot.captured
    }

    @Test
    fun `AUDIOFOCUS_LOSS triggers onLoss`() {
        val am = mockk<AudioManager>(relaxed = true)
        var losses = 0
        val listener = requestAndCaptureListener(am) { losses++ }

        listener.onAudioFocusChange(AudioManager.AUDIOFOCUS_LOSS)

        assertEquals("AUDIOFOCUS_LOSS must signal a loss", 1, losses)
    }

    @Test
    fun `AUDIOFOCUS_LOSS_TRANSIENT triggers onLoss`() {
        val am = mockk<AudioManager>(relaxed = true)
        var losses = 0
        val listener = requestAndCaptureListener(am) { losses++ }

        listener.onAudioFocusChange(AudioManager.AUDIOFOCUS_LOSS_TRANSIENT)

        assertEquals("AUDIOFOCUS_LOSS_TRANSIENT must signal a loss", 1, losses)
    }

    @Test
    fun `AUDIOFOCUS_GAIN does not trigger onLoss`() {
        val am = mockk<AudioManager>(relaxed = true)
        var losses = 0
        val listener = requestAndCaptureListener(am) { losses++ }

        listener.onAudioFocusChange(AudioManager.AUDIOFOCUS_GAIN)

        assertEquals("regaining focus must NOT signal a loss", 0, losses)
    }

    @Test
    fun `AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK does not trigger onLoss`() {
        val am = mockk<AudioManager>(relaxed = true)
        var losses = 0
        val listener = requestAndCaptureListener(am) { losses++ }

        // Ducking lowers other apps' volume but does not interrupt mic capture.
        listener.onAudioFocusChange(AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK)

        assertEquals("ducking must NOT signal a loss", 0, losses)
    }

    @Test
    fun `request returns the AudioManager focus result`() {
        val am = mockk<AudioManager>(relaxed = true)
        every { am.requestAudioFocus(any(), any(), any()) } returns granted

        assertEquals(granted, AudioFocusGuard(am).request {})
    }

    @Test
    fun `abandon releases focus and is idempotent`() {
        val am = mockk<AudioManager>(relaxed = true)
        every { am.requestAudioFocus(any(), any(), any()) } returns granted

        val guard = AudioFocusGuard(am)
        guard.request {}
        guard.abandon()
        guard.abandon() // must not throw

        verify(atLeast = 1) { am.abandonAudioFocus(any()) }
    }
}
