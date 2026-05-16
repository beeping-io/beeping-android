package com.beeping.AndroidBeepingCore

import timber.log.Timber

/**
 * Thin facade over Timber that injects the per-session [traceId] into every
 * log line via the tag. Used by all internal components (BeepingClient,
 * LocalEncoder, CloudEncoder) so log lines are correlatable across the SDK.
 *
 * The actual formatting + filtering + PII redaction live in [BeepingTimberTree].
 *
 * `internal` — not part of the public API.
 */
internal class BeepingLogger(
    private val traceId: String,
) {
    private val tag: String = "Beeping[trace=$traceId]"

    fun v(
        message: String,
        t: Throwable? = null,
    ) {
        if (t != null) Timber.tag(tag).v(t, message) else Timber.tag(tag).v(message)
    }

    fun d(
        message: String,
        t: Throwable? = null,
    ) {
        if (t != null) Timber.tag(tag).d(t, message) else Timber.tag(tag).d(message)
    }

    fun i(
        message: String,
        t: Throwable? = null,
    ) {
        if (t != null) Timber.tag(tag).i(t, message) else Timber.tag(tag).i(message)
    }

    fun w(
        message: String,
        t: Throwable? = null,
    ) {
        if (t != null) Timber.tag(tag).w(t, message) else Timber.tag(tag).w(message)
    }

    fun e(
        message: String,
        t: Throwable? = null,
    ) {
        if (t != null) Timber.tag(tag).e(t, message) else Timber.tag(tag).e(message)
    }
}
