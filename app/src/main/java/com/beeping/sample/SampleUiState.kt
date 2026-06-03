package com.beeping.sample

/**
 * BEE-64 — Three environments selectable from the UI.
 *
 * - [LOCAL] runs `BeepingMode.Local` (JNI shim → beeping-core C API);
 *   encode + decode both functional since BEE-2226.
 * - [DEV] / [PROD] hit `beepbox-server` via Ktor with the matching
 *   `BuildConfig.BEEPBOX_*_BASE_URL` + API key.
 */
enum class SampleEnv {
    LOCAL,
    DEV,
    PROD,
}

data class SampleUiState(
    val env: SampleEnv = SampleEnv.DEV,
    val key: String = "abc12",
    val busy: Boolean = false,
    val listening: Boolean = false,
    val lastDecoded: String? = null,
    // BEE-2313: human-readable reception metrics of the last decode.
    val lastMetrics: String? = null,
    val lastError: String? = null,
    val consoleOpen: Boolean = false,
)
