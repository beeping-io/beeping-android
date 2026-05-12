package com.beeping.AndroidBeepingCore

/**
 * Errors that can be reported via [BeepingEvent.Failed] or wrapped in the
 * [Result.failure] of [BeepingClient.send].
 *
 * The hierarchy is `sealed` so consumers can exhaustively pattern-match in
 * a `when` block without an `else` branch.
 */
sealed class BeepingError {
    /** RECORD_AUDIO permission was denied or revoked at runtime. Recoverable. */
    data object MissingMicPermission : BeepingError()

    /** Audio focus was lost mid-session (e.g. an incoming call). Recoverable. */
    data object AudioFocusLost : BeepingError()

    /** `libbeepingcore.so` failed to load. Non-recoverable in [BeepingMode.Local]. */
    data object NativeLibraryNotLoaded : BeepingError()

    /**
     * HTTP / I/O error talking to `beepbox-server`. Recoverable (with retry).
     *
     * @property cause The underlying [Throwable].
     */
    data class NetworkError(
        val cause: Throwable,
    ) : BeepingError()

    /** API key invalid or revoked. Non-recoverable in [BeepingMode.Cloud]. */
    data object AuthenticationFailed : BeepingError()

    /**
     * Server replied with `429 Too Many Requests`. Recoverable after waiting.
     *
     * @property retryAfterMs Server-suggested retry delay (millis).
     */
    data class RateLimited(
        val retryAfterMs: Long,
    ) : BeepingError()

    /**
     * Internal decoder/encoder bug. Non-recoverable; report as a GitHub issue.
     *
     * @property cause The underlying [Throwable].
     */
    data class DecoderInternal(
        val cause: Throwable,
    ) : BeepingError()
}

/**
 * Throwable adapter for [BeepingError] — used by encoders to fail a `suspend`
 * call. [BeepingClient.send] catches this and wraps it in `Result.failure(error)`.
 */
class BeepingException(
    val error: BeepingError,
) : Exception(error::class.simpleName)
