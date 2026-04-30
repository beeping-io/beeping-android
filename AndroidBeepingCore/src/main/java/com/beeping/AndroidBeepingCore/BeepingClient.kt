package com.beeping.AndroidBeepingCore

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
 * Construction is via the public `BeepingClient.Builder` DSL added in BEE-58.
 * The constructor is `internal` to prevent direct instantiation outside the
 * module.
 *
 * **Status (BEE-57)**:
 *
 * - [listen] now collects from the configured [BeepingEncoder] (Local: JNI,
 *   Cloud: empty Flow stub — see `pending-006`). Failures are mapped to
 *   [BeepingEvent.Failed].
 * - [send] now encodes via [BeepingEncoder.encode] (Cloud: Ktor → WAV bytes
 *   from `beepbox-server`; Local: throws `NotImplementedError("BEE-65")`).
 *   Audio playback (`AudioTrack`) is **not yet implemented** — the encoded
 *   WAV bytes are discarded after the call. Playback lands with BEE-64
 *   (sample app + Compose debug console).
 *
 * Example (post-BEE-58):
 *
 * ```kotlin
 * val client = BeepingClient.Builder(context)
 *     .mode(BeepingMode.Cloud(apiKey = "…", endpoint = "https://api.beeping.io"))
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
 */
class BeepingClient internal constructor(
    private val mode: BeepingMode,
    private val encoder: BeepingEncoder,
) {

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val stopSignal = CompletableDeferred<Unit>()

    @Volatile
    private var closed = false

    /**
     * Returns a cold [Flow] of [BeepingEvent]s for the active listening session.
     *
     * Emits [BeepingEvent.Started] on collect, then [BeepingEvent.Decoded] for
     * each beep that the underlying [BeepingEncoder] decodes. On [close] (or
     * scope cancellation), emits [BeepingEvent.Stopped] and completes.
     * Encoder failures map to [BeepingEvent.Failed].
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
     *
     * @return [Result.success] on successful encode (and, post-BEE-64,
     *   successful playback). [Result.failure] with a [BeepingException]
     *   on a typed failure ([BeepingError.AuthenticationFailed],
     *   [BeepingError.RateLimited], [BeepingError.NetworkError],
     *   [BeepingError.DecoderInternal]). [Result.failure] with
     *   [IllegalArgumentException] if [BeepingPayload.payload] doesn't match
     *   the 5-char base32 key pattern.
     *
     * **Note (BEE-57)**: in Local mode this still throws
     * `NotImplementedError("BEE-65")` because the on-device encoder native
     * function isn't available yet. In Cloud mode it works against the live
     * `beepbox-server` (see `BEEPBOX_API_KEY` in `.env.local`).
     */
    suspend fun send(payload: BeepingPayload): Result<Unit> = runCatching {
        check(!closed) { "BeepingClient is closed" }

        @Suppress("UNUSED_VARIABLE")
        val wav = encoder.encode(payload.payload)
        // BEE-64 will write `wav` to AudioTrack here. Until then the bytes
        // are simply discarded after the encode round-trip — a deliberate
        // gap to avoid coupling BEE-57 to the Compose sample app.
        Unit
    }

    /**
     * Releases all resources and cancels any active listen session. Idempotent.
     */
    fun close() {
        if (closed) return
        closed = true
        stopSignal.complete(Unit)
        scope.cancel()
        encoder.close()
    }
}
