package com.beeping.AndroidBeepingCore

import android.content.Context
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

/**
 * Public entry point of the Beeping Android SDK.
 *
 * `BeepingClient` is the modern, instance-based replacement for the legacy
 * `BeepingCore(Context)` API. It exposes:
 *
 * - [listen] — a cold [Flow] of [BeepingEvent] for the active listening session.
 * - [send] — a `suspend` function that encodes and emits a [BeepingPayload].
 * - [close] — releases native resources and cancels in-flight work.
 *
 * Construction is via the public [Builder] DSL. The constructor is `internal`
 * to prevent direct instantiation outside the module.
 *
 * **Status (BEE-58)**: Builder DSL added with `.mode/.logLevel/.telemetryEnabled`.
 * `logLevel` and `telemetryEnabled` are storage-only here — the actual Timber
 * wiring lands in BEE-60, the telemetry hook in BEE-61.
 *
 * Example:
 *
 * ```kotlin
 * val client = BeepingClient.Builder(context)
 *     .mode(BeepingMode.Cloud(apiKey = "…", endpoint = "https://api.beeping.io"))
 *     .logLevel(LogLevel.DEBUG)
 *     .telemetryEnabled(false)
 *     .build()
 *
 * viewModelScope.launch {
 *     client.listen().collect { event ->
 *         when (event) {
 *             is BeepingEvent.Started -> /* session up */
 *             is BeepingEvent.Decoded -> handle(event.payload)
 *             is BeepingEvent.Failed  -> handle(event.reason)
 *             BeepingEvent.Stopped    -> /* session closed */
 *         }
 *     }
 * }
 *
 * client.send(BeepingPayload(payload = "abc12"))
 * client.close()
 * ```
 *
 * **Permission requirement**: in [BeepingMode.Local], the consumer must request
 * `RECORD_AUDIO` permission via `ActivityCompat.requestPermissions` BEFORE
 * collecting [listen]. If permission is missing the flow emits
 * [BeepingEvent.Failed] with [BeepingError.MissingMicPermission] and completes.
 * The library deliberately does NOT trigger the permission UI flow itself —
 * that belongs to the host Activity so the SDK doesn't couple to UI.
 */
class BeepingClient internal constructor(
    private val mode: BeepingMode,
    private val encoder: BeepingEncoder,
    @Suppress("unused") private val logLevel: LogLevel = LogLevel.INFO,
    @Suppress("unused") private val telemetryEnabled: Boolean = false,
) {

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val stopSignal = CompletableDeferred<Unit>()

    @Volatile
    private var closed = false

    /**
     * Returns a cold [Flow] of [BeepingEvent]s for the active listening session.
     */
    fun listen(): Flow<BeepingEvent> = flow {
        check(!closed) { "BeepingClient is closed" }

        emit(BeepingEvent.Started)

        try {
            encoder.decoded()
                .map<BeepingPayload, BeepingEvent> { BeepingEvent.Decoded(it) }
                .catch { cause ->
                    val error = (cause as? BeepingException)?.error
                        ?: BeepingError.DecoderInternal(cause)
                    emit(BeepingEvent.Failed(error))
                }
                .collect { emit(it) }
        } finally {
            emit(BeepingEvent.Stopped)
        }
    }

    /**
     * Encodes [payload] via the configured [BeepingMode] and (post-BEE-64)
     * plays it via `AudioTrack`.
     */
    suspend fun send(payload: BeepingPayload): Result<Unit> = runCatching {
        check(!closed) { "BeepingClient is closed" }

        @Suppress("UNUSED_VARIABLE")
        val wav = encoder.encode(payload.payload)
        // BEE-64 will write `wav` to AudioTrack here.
        Unit
    }

    /** Releases all resources and cancels any active listen session. Idempotent. */
    fun close() {
        if (closed) return
        closed = true
        stopSignal.complete(Unit)
        scope.cancel()
        encoder.close()
    }

    /**
     * Builder DSL for constructing a [BeepingClient].
     *
     * Defaults:
     *
     * - [mode] = [BeepingMode.Local]
     * - [logLevel] = [LogLevel.INFO]
     * - [telemetryEnabled] = `false`
     *
     * In [BeepingMode.Cloud]:
     *
     * - [BeepingMode.Cloud.apiKey] must be non-blank
     * - [BeepingMode.Cloud.endpoint] must start with `http`
     *
     * Either condition violated → [build] throws [IllegalArgumentException].
     *
     * Example:
     *
     * ```kotlin
     * BeepingClient.Builder(context)
     *     .mode(BeepingMode.Cloud(apiKey = "…", endpoint = "https://api.beeping.io"))
     *     .logLevel(LogLevel.DEBUG)
     *     .telemetryEnabled(false)
     *     .build()
     * ```
     */
    class Builder(private val context: Context) {
        private var mode: BeepingMode = BeepingMode.Local
        private var logLevel: LogLevel = LogLevel.INFO
        private var telemetryEnabled: Boolean = false

        /** Selects the encode/decode strategy. Default: [BeepingMode.Local]. */
        fun mode(value: BeepingMode): Builder = apply { this.mode = value }

        /** Sets log verbosity. Wired to Timber by BEE-60. Default: [LogLevel.INFO]. */
        fun logLevel(value: LogLevel): Builder = apply { this.logLevel = value }

        /** Enables/disables telemetry emission. Wired by BEE-61. Default: `false`. */
        fun telemetryEnabled(value: Boolean): Builder = apply { this.telemetryEnabled = value }

        /**
         * Validates the configuration and returns a fresh [BeepingClient].
         *
         * @throws IllegalArgumentException if [BeepingMode.Cloud] is selected
         *   with a blank apiKey or a malformed endpoint.
         */
        fun build(): BeepingClient {
            val mode = mode
            if (mode is BeepingMode.Cloud) {
                require(mode.apiKey.isNotBlank()) {
                    "BeepingMode.Cloud requires a non-blank apiKey"
                }
                require(mode.endpoint.startsWith("http")) {
                    "BeepingMode.Cloud.endpoint must start with http(s):// (got '${mode.endpoint}')"
                }
            }
            val encoder = BeepingEncoderFactory.create(mode, context)
            return BeepingClient(
                mode = mode,
                encoder = encoder,
                logLevel = logLevel,
                telemetryEnabled = telemetryEnabled,
            )
        }
    }
}
