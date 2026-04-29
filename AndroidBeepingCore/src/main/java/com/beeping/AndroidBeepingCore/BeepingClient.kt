package com.beeping.AndroidBeepingCore

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * Public entry point of the Beeping Android SDK.
 *
 * `BeepingClient` is the modern, instance-based replacement for the legacy
 * `BeepingCore(Context)` API (removed in BEE-53/BEE-56). It exposes:
 *
 * - [listen] — a cold [Flow] of [BeepingEvent] for the active listening session.
 * - [send] — a `suspend` function that encodes and emits a [BeepingPayload].
 * - [close] — releases native resources and cancels in-flight work.
 *
 * Construction is via the public `BeepingClient.Builder` DSL added in BEE-58.
 * The constructor is `internal` to prevent direct instantiation outside the
 * module.
 *
 * **Status (BEE-56 — API SHELL only)**: the encode/decode pipeline lands in
 * BEE-57 (strategy pattern + LocalEncoder/CloudEncoder). In this version
 * [send] throws [NotImplementedError] and [listen] only emits [BeepingEvent.Started]
 * and [BeepingEvent.Stopped] — no [BeepingEvent.Decoded] until BEE-57.
 *
 * Example (post-BEE-58):
 *
 * ```kotlin
 * val client = BeepingClient.Builder(context)
 *     .mode(BeepingMode.Local)
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
 * client.send(BeepingPayload(payload = "HOLA1"))
 * client.close()
 * ```
 */
class BeepingClient internal constructor(
    private val mode: BeepingMode,
    private val encoder: Encoder,
) {

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val stopSignal = CompletableDeferred<Unit>()

    @Volatile
    private var closed = false

    /**
     * Returns a cold [Flow] of [BeepingEvent]s for the active listening session.
     *
     * The flow emits [BeepingEvent.Started] on collect. On [close] (or scope
     * cancellation) the flow emits [BeepingEvent.Stopped] and completes.
     *
     * **Status (BEE-56)**: only `Started` and `Stopped` are emitted — `Decoded`
     * and `Failed` will be wired in BEE-57 by collecting [Encoder.decoded].
     */
    fun listen(): Flow<BeepingEvent> = flow {
        check(!closed) { "BeepingClient is closed" }

        emit(BeepingEvent.Started)

        // BEE-57 will replace this stopSignal.await() with:
        //   encoder.decoded().collect { emit(BeepingEvent.Decoded(it)) }
        // wrapping it in a try/catch that maps Encoder failures to
        // BeepingEvent.Failed(BeepingError.…).
        stopSignal.await()

        emit(BeepingEvent.Stopped)
    }

    /**
     * Encodes [payload] and emits it via the configured [BeepingMode].
     *
     * Returns [Result.success] on successful emission, [Result.failure] with
     * a [BeepingError] cause on a recoverable failure.
     *
     * **Status (BEE-56)**: throws [NotImplementedError] — the implementation
     * lands in BEE-57 once the [Encoder] strategies are wired.
     */
    @Suppress("UNUSED_PARAMETER")
    suspend fun send(payload: BeepingPayload): Result<Unit> {
        check(!closed) { "BeepingClient is closed" }
        @Suppress("ImplicitNothingReturn")
        TODO("BEE-57 — encoder.encode(payload.payload) and play the resulting PCM frames")
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
