package com.beeping.AndroidBeepingCore

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TelemetryEmitterTest {

    @Test
    fun `emit is no-op when disabled`() {
        val received = mutableListOf<TelemetryEvent>()
        val emitter = TelemetryEmitter(
            hook = { received += it },
            enabled = false,
        )

        emitter.emit(TelemetryEvent.SdkInitialized(mode = "local", traceId = "trace1"))
        emitter.emit(TelemetryEvent.Closed(traceId = "trace1", sessionDurationMs = 100))

        assertTrue("Hook should not be invoked when telemetry disabled", received.isEmpty())
    }

    @Test
    fun `emit forwards every event when enabled`() {
        val received = mutableListOf<TelemetryEvent>()
        val emitter = TelemetryEmitter(
            hook = { received += it },
            enabled = true,
        )

        val events = listOf(
            TelemetryEvent.SdkInitialized(mode = "cloud", traceId = "t"),
            TelemetryEvent.EncodeRequested(mode = "cloud", keyLength = 5, traceId = "t"),
            TelemetryEvent.EncodeSucceeded(traceId = "t", durationMs = 10, byteCount = 1000),
            TelemetryEvent.EncodeFailed(traceId = "t", errorType = "BeepingException"),
            TelemetryEvent.Closed(traceId = "t", sessionDurationMs = 500),
        )
        events.forEach { emitter.emit(it) }

        assertEquals(events, received)
    }

    @Test
    fun `emit swallows hook exceptions — telemetry must not break the SDK`() {
        val emitter = TelemetryEmitter(
            hook = { throw RuntimeException("hook is broken") },
            enabled = true,
        )

        // Must NOT throw.
        emitter.emit(TelemetryEvent.SdkInitialized(mode = "local", traceId = "t"))
        emitter.emit(TelemetryEvent.Closed(traceId = "t", sessionDurationMs = 1))
    }
}
