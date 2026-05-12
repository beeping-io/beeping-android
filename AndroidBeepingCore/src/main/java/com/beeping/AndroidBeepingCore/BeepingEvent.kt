package com.beeping.AndroidBeepingCore

/**
 * Events emitted by [BeepingClient.listen] during a listening session.
 *
 * The hierarchy is `sealed` so consumers can exhaustively pattern-match
 * in a `when` block without an `else` branch:
 *
 * ```kotlin
 * client.listen().collect { event ->
 *     when (event) {
 *         is BeepingEvent.Started -> /* session up */
 *         is BeepingEvent.Decoded -> handle(event.payload)
 *         is BeepingEvent.Failed  -> handle(event.reason)
 *         BeepingEvent.Stopped    -> /* session closed */
 *     }
 * }
 * ```
 */
sealed class BeepingEvent {
    /** The listening session is up and ready to receive beeps. */
    data object Started : BeepingEvent()

    /**
     * A valid beep was decoded.
     *
     * @property payload The decoded payload — see [BeepingPayload].
     */
    data class Decoded(
        val payload: BeepingPayload,
    ) : BeepingEvent()

    /**
     * A failure occurred. Some [BeepingError] variants are recoverable and
     * the SDK may continue listening; others are terminal and the flow
     * will subsequently emit [Stopped].
     *
     * @property reason The typed [BeepingError].
     */
    data class Failed(
        val reason: BeepingError,
    ) : BeepingEvent()

    /** The session is closed. No more events will be emitted. */
    data object Stopped : BeepingEvent()
}
