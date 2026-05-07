package com.beeping.AndroidBeepingCore

import android.util.Log
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import timber.log.Timber

/**
 * Timber [Tree][Timber.Tree] that emits JSON-formatted log lines and applies
 * library-level concerns (level filtering, PII redaction).
 *
 * **Status (BEE-60)**:
 *
 * - Single process-global instance — installed via [installOnce]; calling
 *   it more than once is a no-op (idempotent).
 * - Filters by [LogLevel] set via [setLogLevel] (called by
 *   `BeepingClient.Builder.build()`).
 * - Redacts well-known PII patterns from log messages: `Bearer <token>`,
 *   `apiKey=<token>` (case-sensitive). Other patterns (emails, etc.) are
 *   not covered in BEE-60.
 *
 * Output format (one JSON line per log call):
 *
 * ```json
 * {"level":"INFO","tag":"Beeping[trace=a1b2c3d4]","msg":"…","ts":1762012345678}
 * ```
 *
 * The trace-id is encoded inside the `tag` (passed by [BeepingLogger]) — it
 * is parsed out at write time and emitted as a top-level `traceId` field
 * when present.
 */
object BeepingTimberTree : Timber.Tree() {
    @Volatile
    private var minLevel: LogLevel = LogLevel.INFO

    private val _logs =
        MutableSharedFlow<String>(
            replay = LOG_REPLAY_CACHE,
            extraBufferCapacity = 0,
            onBufferOverflow = BufferOverflow.DROP_OLDEST,
        )

    /**
     * Hot stream of recent JSON log lines, capped at [LOG_REPLAY_CACHE]. New
     * subscribers immediately receive the buffered backlog. Used by the
     * sample app's debug console (BEE-64) to render live log output.
     */
    val logs: SharedFlow<String> = _logs

    /** Updates the minimum log level. Thread-safe. */
    fun setLogLevel(level: LogLevel) {
        minLevel = level
    }

    /**
     * Plants this tree if it isn't already planted. Multiple calls are no-ops.
     * Called by `BeepingClient.Builder.build()` when the SDK is constructed.
     */
    @Synchronized
    fun installOnce() {
        if (Timber.forest().none { it === this }) {
            Timber.plant(this)
        }
    }

    override fun isLoggable(
        tag: String?,
        priority: Int,
    ): Boolean = shouldLog(priority)

    /** Public test-friendly mirror of the protected [isLoggable]. */
    @JvmStatic
    fun shouldLog(priority: Int): Boolean {
        if (minLevel == LogLevel.NONE) return false
        val incoming = priority.toLogLevel() ?: return false
        return incoming.ordinal >= minLevel.ordinal
    }

    override fun log(
        priority: Int,
        tag: String?,
        message: String,
        t: Throwable?,
    ) {
        val level = priority.toLogLevel() ?: return
        val redactedMessage = redact(message)
        val json = buildJsonLog(level, tag, redactedMessage, t)
        Log.println(priority, BEEPING_LOGCAT_TAG, json)
        _logs.tryEmit(json)
    }

    private fun Int.toLogLevel(): LogLevel? =
        when (this) {
            Log.VERBOSE -> LogLevel.VERBOSE
            Log.DEBUG -> LogLevel.DEBUG
            Log.INFO -> LogLevel.INFO
            Log.WARN -> LogLevel.WARN
            Log.ERROR, Log.ASSERT -> LogLevel.ERROR
            else -> null
        }

    /** Redacts known PII patterns. Visible-for-tests. */
    @JvmStatic
    fun redact(input: String): String =
        input
            .replace(BEARER_PATTERN, "Bearer ***")
            .replace(API_KEY_PATTERN, "apiKey=***")

    private fun buildJsonLog(
        level: LogLevel,
        tag: String?,
        message: String,
        t: Throwable?,
    ): String {
        val traceId = tag?.let { TRACE_TAG_PATTERN.matchEntire(it)?.groupValues?.getOrNull(1) }
        val sb = StringBuilder("""{"level":"${level.name}"""")
        if (tag != null) sb.append(""","tag":"${escape(tag)}"""")
        if (traceId != null) sb.append(""","traceId":"${escape(traceId)}"""")
        sb.append(""","msg":"${escape(message)}"""")
        sb.append(""","ts":${System.currentTimeMillis()}""")
        if (t != null) sb.append(""","exception":"${escape(t.toString())}"""")
        sb.append('}')
        return sb.toString()
    }

    private fun escape(s: String): String =
        s
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")

    private const val BEEPING_LOGCAT_TAG = "Beeping"
    private const val LOG_REPLAY_CACHE = 200
    private val BEARER_PATTERN = Regex("""Bearer\s+[^\s"',]+""")
    private val API_KEY_PATTERN = Regex("""apiKey=[^\s"&,]+""")
    private val TRACE_TAG_PATTERN = Regex("""Beeping\[trace=([^]]+)]""")
}
