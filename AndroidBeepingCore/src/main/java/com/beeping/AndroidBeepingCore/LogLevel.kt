package com.beeping.AndroidBeepingCore

/**
 * Log verbosity for the Beeping SDK.
 *
 * Stored at construction time via [BeepingClient.Builder.logLevel]. The
 * Timber JSON sink + trace-ID propagation that consumes it lands in BEE-60.
 *
 * Levels are ordered from most verbose to silent:
 * `VERBOSE > DEBUG > INFO > WARN > ERROR > NONE`.
 */
enum class LogLevel {
    VERBOSE,
    DEBUG,
    INFO,
    WARN,
    ERROR,
    NONE,
}
