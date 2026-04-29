package com.beeping.AndroidBeepingCore

/**
 * Where the encoding/decoding work runs.
 *
 * - [Local] runs on-device via the JNI bridge to `libbeepingcore.so`. No
 *   network calls; lowest latency; full privacy. Implementation lands with
 *   `LocalEncoder` in BEE-57.
 * - [Cloud] runs against the [`beepbox`](https://github.com/beeping-io/beepbox)
 *   HTTP server. Requires a valid `apiKey` and a reachable `endpoint`.
 *   Useful for low-end devices, central feature gating, and observability.
 *   Implementation lands with `CloudEncoder` in BEE-57.
 *
 * Selection happens at construction time via the (BEE-58) `BeepingClient.Builder`:
 *
 * ```kotlin
 * BeepingClient.Builder(context).mode(BeepingMode.Local).build()
 * BeepingClient.Builder(context)
 *     .mode(BeepingMode.Cloud(apiKey = "…", endpoint = "https://api.beeping.io"))
 *     .build()
 * ```
 */
sealed class BeepingMode {
    /** On-device JNI mode — no network, lowest latency. */
    data object Local : BeepingMode()

    /**
     * Cloud HTTP mode. Sends/receives encoded audio via the beepbox API.
     *
     * @property apiKey The customer-provisioned API key for `beepbox-server`.
     * @property endpoint Base URL of the API (e.g. `https://api.beeping.io`).
     */
    data class Cloud(val apiKey: String, val endpoint: String) : BeepingMode()
}
