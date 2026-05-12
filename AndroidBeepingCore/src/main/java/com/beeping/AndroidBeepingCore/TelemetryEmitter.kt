package com.beeping.AndroidBeepingCore

/**
 * Internal helper that forwards events to the configured [TelemetryHook]
 * when the session has telemetry enabled. Hook exceptions are swallowed —
 * telemetry must never break the app.
 *
 * `internal` — consumers use [TelemetryHook] directly via the Builder.
 */
internal class TelemetryEmitter(
    private val hook: TelemetryHook,
    private val enabled: Boolean,
) {
    fun emit(event: TelemetryEvent) {
        if (!enabled) return
        try {
            hook.onEvent(event)
        } catch (
            @Suppress("TooGenericExceptionCaught") t: Throwable,
        ) {
            // Telemetry MUST NOT break the SDK. Swallow + (silently) log
            // — Timber is wired in BEE-60 but using it here would risk
            // recursive failures if logging itself throws.
            @Suppress("PrintStackTrace")
            t.printStackTrace()
        }
    }
}
