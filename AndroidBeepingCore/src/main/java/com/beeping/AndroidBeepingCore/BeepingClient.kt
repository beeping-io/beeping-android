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
import java.util.UUID

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
    private val player: WavPlayer? = null,
    @Suppress("unused") private val logLevel: LogLevel = LogLevel.INFO,
    private val telemetryEnabled: Boolean = false,
    private val telemetryHook: TelemetryHook = TelemetryHook.NoOp,
    /**
     * Per-session trace ID, propagated as `X-Trace-Id` in cloud HTTP requests
     * and embedded in every log line via [BeepingLogger]. Useful for
     * correlating client logs with server-side traces.
     */
    val traceId: String = UUID.randomUUID().toString().take(TRACE_ID_LEN),
) {
    private val logger = BeepingLogger(traceId)
    private val emitter = TelemetryEmitter(telemetryHook, telemetryEnabled)
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val stopSignal = CompletableDeferred<Unit>()
    private val createdAtMillis = System.currentTimeMillis()
    private val modeString = if (mode is BeepingMode.Cloud) "cloud" else "local"

    @Volatile
    private var closed = false

    init {
        logger.i("BeepingClient created (mode=${mode::class.simpleName}, logLevel=$logLevel)")
        emitter.emit(TelemetryEvent.SdkInitialized(mode = modeString, traceId = traceId))
    }

    /**
     * Returns a cold [Flow] of [BeepingEvent]s for the active listening session.
     */
    fun listen(): Flow<BeepingEvent> =
        flow {
            check(!closed) { "BeepingClient is closed" }

            emit(BeepingEvent.Started)

            try {
                encoder
                    .decoded()
                    .map<BeepingPayload, BeepingEvent> { BeepingEvent.Decoded(it) }
                    .catch { cause ->
                        val error =
                            (cause as? BeepingException)?.error
                                ?: BeepingError.DecoderInternal(cause)
                        emit(BeepingEvent.Failed(error))
                    }.collect { emit(it) }
            } finally {
                emit(BeepingEvent.Stopped)
            }
        }

    /**
     * Encodes [payload] via the configured [BeepingMode] and (post-BEE-64)
     * plays it via `AudioTrack`.
     */
    suspend fun send(payload: BeepingPayload): Result<Unit> {
        check(!closed) { "BeepingClient is closed" }

        emitter.emit(
            TelemetryEvent.EncodeRequested(
                mode = modeString,
                keyLength = payload.payload.length,
                traceId = traceId,
            ),
        )
        val started = System.currentTimeMillis()

        return runCatching {
            val wav = encoder.encode(payload.payload)
            emitter.emit(
                TelemetryEvent.EncodeSucceeded(
                    traceId = traceId,
                    durationMs = System.currentTimeMillis() - started,
                    byteCount = wav.size,
                ),
            )
            // BEE-64: play the encoded WAV via MediaPlayer when a player was
            // wired by the Builder (always, except in unit tests that stub
            // it out for headless environments).
            player?.play(wav)
            Unit
        }.onFailure { e ->
            emitter.emit(
                TelemetryEvent.EncodeFailed(
                    traceId = traceId,
                    errorType = e::class.simpleName ?: "Unknown",
                ),
            )
        }
    }

    /**
     * BEE-2240: timestamps (seconds, relative to the start of the schedule) of
     * each beep that fits within `(duration, startTime, interval)`.
     *
     * Pure utility — does not allocate audio, does not require a [BeepingMode]
     * to be `Local`. Useful for UI previews of a scheduled transmission.
     *
     * @throws IllegalArgumentException if the upstream parameter validation
     *   fails (e.g. `duration < 2.3`, `interval <= 0`, `startTime + 2.3 > duration`).
     */
    fun computeBeepSchedule(
        duration: Float,
        startTime: Float = 0f,
        interval: Float = DEFAULT_INTERVAL_SECONDS,
    ): List<Double> {
        if (!BeepingCoreJNI.isNativeLoaded()) {
            throw BeepingException(BeepingError.NativeLibraryNotLoaded)
        }
        val timestamps =
            BeepingCoreJNI().computeBeepSchedule(duration, startTime, interval)
                ?: throw IllegalArgumentException(
                    "Invalid schedule (duration=$duration startTime=$startTime interval=$interval)",
                )
        return timestamps.toList()
    }

    /**
     * BEE-2240: encode [payload] repeated as N beeps over [duration] seconds
     * and play the resulting WAV via the configured [WavPlayer].
     *
     * Each beep's audio payload is `payload.payload + 4-char base-32 timestamp`,
     * which lets the receiver recover the beep's position within the schedule
     * via the scheduler-aware decode helpers.
     *
     * Only [BeepingMode.Local] is supported today; in [BeepingMode.Cloud] the
     * call fails with [BeepingError.SchedulingNotSupported] (no equivalent
     * beepbox endpoint yet).
     *
     * @param beepGainDb dB gain applied to each beep (clamped upstream to
     *   `[-60, +12]`). Default 0 = identity.
     * @param audible if `true`, encodes in the audible 3.3-10 kHz band — useful
     *   for QA / demos. Default `false` is the production-grade inaudible band.
     */
    suspend fun sendScheduled(
        payload: BeepingPayload,
        duration: Float,
        startTime: Float = 0f,
        interval: Float = DEFAULT_INTERVAL_SECONDS,
        beepGainDb: Float = 0f,
        audible: Boolean = false,
    ): Result<Unit> {
        check(!closed) { "BeepingClient is closed" }

        emitter.emit(
            TelemetryEvent.EncodeRequested(
                mode = modeString,
                keyLength = payload.payload.length,
                traceId = traceId,
            ),
        )
        val started = System.currentTimeMillis()

        return runCatching {
            val wav =
                encoder.encodeScheduled(payload.payload, duration, startTime, interval, beepGainDb, audible)
            emitter.emit(
                TelemetryEvent.EncodeSucceeded(
                    traceId = traceId,
                    durationMs = System.currentTimeMillis() - started,
                    byteCount = wav.size,
                ),
            )
            player?.play(wav)
            Unit
        }.onFailure { e ->
            emitter.emit(
                TelemetryEvent.EncodeFailed(
                    traceId = traceId,
                    errorType = e::class.simpleName ?: "Unknown",
                ),
            )
        }
    }

    /**
     * BEE-2315: the underlying `beeping-core` native library version string.
     *
     * @throws BeepingException with [BeepingError.NativeLibraryNotLoaded] if the
     *   native library failed to load (e.g. on a JVM/host test runtime).
     */
    fun coreVersion(): String {
        if (!BeepingCoreJNI.isNativeLoaded()) {
            throw BeepingException(BeepingError.NativeLibraryNotLoaded)
        }
        return BeepingCoreJNI().getVersion()
    }

    /** Releases all resources and cancels any active listen session. Idempotent. */
    fun close() {
        if (closed) return
        closed = true
        logger.i("BeepingClient closing")
        emitter.emit(
            TelemetryEvent.Closed(
                traceId = traceId,
                sessionDurationMs = System.currentTimeMillis() - createdAtMillis,
            ),
        )
        stopSignal.complete(Unit)
        scope.cancel()
        encoder.close()
        player?.close()
    }

    private companion object {
        private const val TRACE_ID_LEN = 8

        // BEE-2238 / BEE-2240: 2.3 s is the minimum gap between beeps imposed
        // by beeping-core (each beep occupies ~2.3 s of audio); also the natural
        // default interval.
        private const val DEFAULT_INTERVAL_SECONDS = 2.3f
    }

    /**
     * Builder DSL for constructing a [BeepingClient].
     *
     * Defaults:
     *
     * - [mode] = [BeepingMode.Local]
     * - [logLevel] = [LogLevel.INFO]
     * - [telemetryEnabled] = `false`
     * - [traceId] = autogenerated UUID-8 (override for external correlation)
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
    class Builder(
        private val context: Context,
    ) {
        private var mode: BeepingMode = BeepingMode.Local
        private var logLevel: LogLevel = LogLevel.INFO
        private var telemetryEnabled: Boolean = false
        private var telemetryHook: TelemetryHook = TelemetryHook.NoOp
        private var injectedTraceId: String? = null
        private var encodingMode: BeepingEncodingMode = BeepingEncodingMode.ALL

        /** Selects the encode/decode strategy. Default: [BeepingMode.Local]. */
        fun mode(value: BeepingMode): Builder = apply { this.mode = value }

        /**
         * Selects the audio frequency band for encode ([send]) and decode
         * ([listen]) in [BeepingMode.Local] (BEE-2305). Default [BeepingEncodingMode.ALL]
         * (listen on both bands; send falls back to inaudible). Has no effect in
         * [BeepingMode.Cloud] yet — beepbox-server uses its own default.
         */
        fun encodingMode(value: BeepingEncodingMode): Builder = apply { this.encodingMode = value }

        /**
         * Injects an external trace ID for end-to-end correlation (BEE-2306).
         *
         * Used verbatim — propagated to the `X-Trace-Id` HTTP header in
         * [BeepingMode.Cloud] and to every log line's tag — so a caller-supplied
         * id (e.g. one generated by `beeping_flutter`) ties client logs to
         * `beepbox-server` traces. When unset, [build] autogenerates a UUID-8.
         *
         * @throws IllegalArgumentException at [build] time if [value] is blank.
         */
        fun traceId(value: String): Builder = apply { this.injectedTraceId = value }

        /** Sets log verbosity. Wired to Timber by BEE-60. Default: [LogLevel.INFO]. */
        fun logLevel(value: LogLevel): Builder = apply { this.logLevel = value }

        /**
         * Enables/disables telemetry emission. **Default: `false` (opt-IN)** —
         * privacy-first per the SDK product principles.
         */
        fun telemetryEnabled(value: Boolean): Builder = apply { this.telemetryEnabled = value }

        /**
         * Sets the [TelemetryHook] sink. Default: [TelemetryHook.NoOp].
         * Has no effect if [telemetryEnabled] stays `false`.
         */
        fun telemetryHook(value: TelemetryHook): Builder = apply { this.telemetryHook = value }

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

            // Wire BEE-60 logging: install the tree once + apply the requested level.
            BeepingTimberTree.installOnce()
            BeepingTimberTree.setLogLevel(logLevel)

            // BEE-2306: an injected trace ID is used verbatim (external correlation
            // ids may exceed 8 chars); otherwise fall back to the autogenerated UUID-8.
            injectedTraceId?.let { require(it.isNotBlank()) { "traceId() must be non-blank" } }
            val traceId = injectedTraceId ?: UUID.randomUUID().toString().take(TRACE_ID_LEN)
            val encoder = BeepingEncoderFactory.create(mode, context, traceId, encodingMode)
            return BeepingClient(
                mode = mode,
                encoder = encoder,
                player = WavPlayer(context.applicationContext),
                logLevel = logLevel,
                telemetryEnabled = telemetryEnabled,
                telemetryHook = telemetryHook,
                traceId = traceId,
            )
        }
    }
}
