package com.beeping.AndroidBeepingCore

import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.headers
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsBytes
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.IOException

/**
 * Cloud-mode implementation of [BeepingEncoder] — talks HTTP to
 * `beepbox-server` (Phase 2: deployed at `https://api.beeping.io` PROD or
 * `https://beepbox-server-…a.run.app` Cloud Run dev URL).
 *
 * **Endpoints used**:
 *
 * ```
 * POST /v1/encode
 *   Headers: Authorization: Bearer <apiKey>, Content-Type: application/json
 *   Body:    {"key": "<5 base32 chars>"}
 *   200:     binary WAV (RIFF · PCM 16-bit · mono · 44100 Hz)
 *   401/403: auth failure
 *   429:     rate limited (Retry-After header)
 *   5xx:     server error
 * ```
 *
 * **Status (BEE-57)**:
 *
 * - [encode] fully implemented + tested with Ktor MockEngine.
 * - [decoded] returns [emptyFlow] — cyclic POST `/v1/decode` with
 *   AudioRecord-captured chunks is `pending-006` (out of scope here).
 *
 * `internal` — consumers select this via [BeepingMode.Cloud].
 */
internal class CloudEncoder(
    private val apiKey: String,
    private val endpoint: String,
    private val httpClient: HttpClient = defaultHttpClient(),
) : BeepingEncoder {

    override suspend fun encode(key: String): ByteArray {
        require(key.matches(KEY_PATTERN)) {
            "Key must match the 5-char base32 pattern $KEY_PATTERN_STR (got '$key')"
        }

        val response = try {
            httpClient.post("$endpoint/v1/encode") {
                contentType(ContentType.Application.Json)
                headers {
                    append(HttpHeaders.Authorization, "Bearer $apiKey")
                }
                setBody(EncodeRequest(key = key))
            }
        } catch (cause: Throwable) {
            throw BeepingException(BeepingError.NetworkError(cause))
        }

        if (!response.status.isSuccess()) {
            throw mapStatusToBeepingException(response.status.value, response.headers["Retry-After"])
        }

        return response.bodyAsBytes()
    }

    override fun decoded(): Flow<BeepingPayload> {
        // pending-006 — Cloud-mode live decoding requires AudioRecord chunking
        // + cyclic POST /v1/decode. Out of scope for BEE-57.
        return emptyFlow()
    }

    override fun close() {
        httpClient.close()
    }

    @Serializable
    private data class EncodeRequest(val key: String)

    companion object {
        private val KEY_PATTERN = Regex("^[0-9a-v]{5}$")
        private const val KEY_PATTERN_STR = "^[0-9a-v]{5}\$"

        /** Default HTTP client — Android engine + JSON content negotiation. */
        fun defaultHttpClient(): HttpClient = HttpClient(Android) {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }

        private fun mapStatusToBeepingException(status: Int, retryAfter: String?): BeepingException {
            val error = when (status) {
                401, 403 -> BeepingError.AuthenticationFailed
                429 -> {
                    val seconds = retryAfter?.toLongOrNull() ?: DEFAULT_RETRY_AFTER_SECONDS
                    BeepingError.RateLimited(retryAfterMs = seconds * MILLIS_PER_SECOND)
                }
                else -> BeepingError.NetworkError(IOException("HTTP $status"))
            }
            return BeepingException(error)
        }

        private const val DEFAULT_RETRY_AFTER_SECONDS = 60L
        private const val MILLIS_PER_SECOND = 1000L
    }
}
