package com.beeping.AndroidBeepingCore

/**
 * Sink for SDK operational telemetry. Implementations forward [TelemetryEvent]s
 * to whatever observability backend the consumer uses (Firebase Analytics,
 * Sentry, Datadog, Segment, custom HTTP, etc.).
 *
 * The SDK ships with [NoOp] as the default — telemetry is **opt-IN** via
 * [BeepingClient.Builder.telemetryEnabled] + [BeepingClient.Builder.telemetryHook].
 *
 * Implementations should:
 *
 * - Be **non-blocking** — the SDK calls [onEvent] synchronously from internal
 *   coroutines; offload to a background thread / worker if your sink does I/O.
 * - **Never throw** — the SDK's [TelemetryEmitter] catches throwables to
 *   protect the host app, but a misbehaving hook bloats logs.
 * - Treat the [TelemetryEvent] objects as PII-free (verified by tests). It
 *   is safe to forward them as-is to any analytics backend.
 *
 * Example:
 *
 * ```kotlin
 * class FirebaseTelemetryHook(private val analytics: FirebaseAnalytics) : TelemetryHook {
 *     override fun onEvent(event: TelemetryEvent) = when (event) {
 *         is TelemetryEvent.SdkInitialized -> analytics.logEvent("beeping_init", null)
 *         is TelemetryEvent.EncodeSucceeded -> analytics.logEvent("beeping_encode_ok", Bundle().apply {
 *             putLong("duration_ms", event.durationMs)
 *             putInt("byte_count", event.byteCount)
 *         })
 *         else -> { /* etc. */ }
 *     }
 * }
 *
 * BeepingClient.Builder(context)
 *     .telemetryEnabled(true)
 *     .telemetryHook(FirebaseTelemetryHook(FirebaseAnalytics.getInstance(context)))
 *     .build()
 * ```
 */
fun interface TelemetryHook {
    /** Called by the SDK on every operational event. Must not throw. */
    fun onEvent(event: TelemetryEvent)

    companion object {
        /** Default no-op hook — used when telemetry is disabled or no hook was configured. */
        @JvmField
        val NoOp: TelemetryHook = TelemetryHook { /* drop */ }
    }
}
