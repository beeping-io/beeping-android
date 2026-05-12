package com.beeping.AndroidBeepingCore

import com.beeping.AndroidBeepingCore.internal.api.apis.EncodingApi
import com.beeping.AndroidBeepingCore.internal.api.models.EncodeRequest
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.header
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.serialization.json.Json
import java.io.IOException

/**
 * Cloud-mode implementation of [BeepingEncoder] — delegates encode to
 * `beepbox-server` over HTTP via the openapi-generator-built [EncodingApi].
 *
 * The HTTP client is generated from the canonical `api/openapi.yaml` (vendored
 * from `beeping-io/beepbox`). See `AndroidBeepingCore/build.gradle.kts`
 * `openApiGenerate { ... }` block for codegen config.
 *
 * **Status (BEE-59)**:
 *
 * - [encode] uses the generated `EncodingApi.encodePayload(EncodeRequest)`.
 *   Returns the WAV bytes from the body. Maps 401/403/429/5xx → typed
 *   [BeepingException] mirrors of the contract.
 * - [decoded] returns [emptyFlow] — cyclic `/v1/decode` chunking is `pending-006`.
 *
 * `internal` — consumers select this via [BeepingMode.Cloud].
 */
internal class CloudEncoder(
    apiKey: String,
    endpoint: String,
    httpClientEngine: HttpClientEngine? = null,
    traceId: String = "anon",
) : BeepingEncoder {
    private val encodingApi: EncodingApi =
        EncodingApi(
            baseUrl = endpoint,
            httpClientEngine = httpClientEngine,
            // The generated ApiClient installs ContentNegotiation with an empty
            // config block (no converters registered). We re-install via the
            // httpClientConfig hook so kotlinx-serialization JSON is wired up.
            // Ktor merges the configs when a plugin is installed twice.
            // BEE-60: also installs defaultRequest with X-Trace-Id header.
            httpClientConfig = { config ->
                config.install(ContentNegotiation) {
                    json(Json { ignoreUnknownKeys = true })
                }
                config.defaultRequest {
                    header("X-Trace-Id", traceId)
                }
            },
        ).apply {
            setBearerToken(apiKey)
        }

    override suspend fun encode(key: String): ByteArray {
        require(key.matches(KEY_PATTERN)) {
            "Key must match the 5-char base32 pattern $KEY_PATTERN_STR (got '$key')"
        }

        val response =
            try {
                encodingApi.encodePayload(EncodeRequest(key = key))
            } catch (
                @Suppress("TooGenericExceptionCaught") cause: Throwable,
            ) {
                throw BeepingException(BeepingError.NetworkError(cause))
            }

        if (!response.success) {
            throw mapStatusToBeepingException(
                status = response.status,
                retryAfter = response.headers["Retry-After"]?.firstOrNull(),
            )
        }
        return response.body()
    }

    override fun decoded(): Flow<BeepingPayload> {
        // pending-006 — Cloud-mode live decoding requires AudioRecord chunking
        // + cyclic POST /v1/decode. Out of scope for BEE-57/59.
        return emptyFlow()
    }

    override fun close() {
        // The generated ApiClient owns a private HttpClient (no public accessor).
        // Resource cleanup happens implicitly via GC + Android process lifecycle.
        // pending-007 tracks closing the underlying client cleanly when
        // openapi-generator exposes it (or after a custom template).
    }

    companion object {
        private val KEY_PATTERN = Regex("^[0-9a-v]{5}$")
        private const val KEY_PATTERN_STR = "^[0-9a-v]{5}\$"

        private fun mapStatusToBeepingException(
            status: Int,
            retryAfter: String?,
        ): BeepingException {
            val error =
                when (status) {
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
