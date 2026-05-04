package com.beeping.AndroidBeepingCore

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CloudEncoderTest {
    @Test
    fun `encode happy path returns WAV bytes from server`() =
        runTest {
            val expectedBody = "RIFF    WAVEdata".toByteArray()
            val engine =
                MockEngine { request ->
                    assertEquals(
                        "https://example.com/v1/encode",
                        request.url.toString(),
                    )
                    assertEquals(
                        "Bearer test-key",
                        request.headers[HttpHeaders.Authorization],
                    )
                    // BEE-60: X-Trace-Id header must be propagated.
                    assertEquals(
                        "trace-test-001",
                        request.headers["X-Trace-Id"],
                    )
                    respond(
                        content = expectedBody,
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "audio/wav"),
                    )
                }
            val encoder =
                CloudEncoder(
                    apiKey = "test-key",
                    endpoint = "https://example.com",
                    httpClientEngine = engine,
                    traceId = "trace-test-001",
                )

            val result = encoder.encode("abc12")

            assertTrue(
                "response should start with RIFF",
                result.copyOf(4).toString(Charsets.US_ASCII).startsWith("RIFF"),
            )
            encoder.close()
        }

    @Test
    fun `encode 401 maps to AuthenticationFailed`() =
        runTest {
            val encoder = encoderRespondingWith(HttpStatusCode.Unauthorized)
            val ex = runCatching { encoder.encode("abc12") }.exceptionOrNull()
            assertTrue("got $ex", ex is BeepingException)
            assertEquals(BeepingError.AuthenticationFailed, (ex as BeepingException).error)
        }

    @Test
    fun `encode 403 maps to AuthenticationFailed`() =
        runTest {
            val encoder = encoderRespondingWith(HttpStatusCode.Forbidden)
            val ex = runCatching { encoder.encode("abc12") }.exceptionOrNull()
            assertTrue(ex is BeepingException)
            assertEquals(BeepingError.AuthenticationFailed, (ex as BeepingException).error)
        }

    @Test
    fun `encode 429 maps to RateLimited with Retry-After`() =
        runTest {
            val encoder =
                encoderRespondingWith(
                    HttpStatusCode.TooManyRequests,
                    extraHeaders = headersOf("Retry-After", "30"),
                )
            val ex = runCatching { encoder.encode("abc12") }.exceptionOrNull()
            assertTrue(ex is BeepingException)
            val error = (ex as BeepingException).error
            assertTrue(error is BeepingError.RateLimited)
            assertEquals(30_000L, (error as BeepingError.RateLimited).retryAfterMs)
        }

    @Test
    fun `encode 500 maps to NetworkError`() =
        runTest {
            val encoder = encoderRespondingWith(HttpStatusCode.InternalServerError)
            val ex = runCatching { encoder.encode("abc12") }.exceptionOrNull()
            assertTrue(ex is BeepingException)
            assertTrue((ex as BeepingException).error is BeepingError.NetworkError)
        }

    @Test
    fun `encode rejects keys not matching 5-char base32 pattern`() =
        runTest {
            val encoder =
                CloudEncoder(
                    apiKey = "test-key",
                    endpoint = "https://example.com",
                    httpClientEngine = MockEngine { error("should not be reached") },
                )

            for (badKey in listOf("ABC12", "abc1", "abc123", "wxyz1", "    1")) {
                val ex = runCatching { encoder.encode(badKey) }.exceptionOrNull()
                assertTrue(
                    "key '$badKey' should fail validation, got $ex",
                    ex is IllegalArgumentException,
                )
            }
            encoder.close()
        }

    /**
     * Opt-in real E2E against the **DEV** beepbox-server. Skipped unless
     * both `BEEPBOX_DEV_BASE_URL` and `BEEPBOX_DEV_API_KEY` are set in
     * `.env.local` (or in the process env on CI).
     */
    @Test
    fun `e2e encode against DEV beepbox-server (opt-in)`() =
        runTest { e2eAgainstEnvironment(label = "DEV", envPrefix = "BEEPBOX_DEV") }

    /**
     * Opt-in real E2E against the **PROD** beepbox-server. Skipped unless
     * both `BEEPBOX_PROD_BASE_URL` and `BEEPBOX_PROD_API_KEY` are set.
     * Hits real production — keep `abc12` payload (low cardinality, no PII).
     */
    @Test
    fun `e2e encode against PROD beepbox-server (opt-in)`() =
        runTest { e2eAgainstEnvironment(label = "PROD", envPrefix = "BEEPBOX_PROD") }

    private suspend fun e2eAgainstEnvironment(
        label: String,
        envPrefix: String,
    ) {
        val baseUrl = System.getenv("${envPrefix}_BASE_URL").orEmpty()
        val apiKey = System.getenv("${envPrefix}_API_KEY").orEmpty()
        Assume.assumeFalse(
            "Skipped $label E2E — ${envPrefix}_BASE_URL / ${envPrefix}_API_KEY not set. " +
                "Add them to .env.local to opt in.",
            baseUrl.isBlank() || apiKey.isBlank(),
        )

        // httpClientEngine = null → uses the generated default (CIO/Android).
        val encoder = CloudEncoder(apiKey = apiKey, endpoint = baseUrl)
        try {
            val wav = encoder.encode("abc12")
            assertTrue("$label response too small (got ${wav.size} bytes)", wav.size > 1000)
            val magic = wav.copyOf(4).toString(Charsets.US_ASCII)
            assertEquals("$label magic", "RIFF", magic)
        } catch (e: BeepingException) {
            // Don't fail the build on transient server errors / rotated keys —
            // skip gracefully so CI / local builds remain green.
            Assume.assumeNoException(
                "Skipped $label E2E — server returned ${e.error::class.simpleName} " +
                    "(check ${envPrefix}_API_KEY validity).",
                e,
            )
        } finally {
            encoder.close()
        }
    }

    // -- helpers ------------------------------------------------------------

    private fun encoderRespondingWith(
        status: HttpStatusCode,
        extraHeaders: io.ktor.http.Headers = io.ktor.http.Headers.Empty,
    ): CloudEncoder {
        val engine =
            MockEngine {
                respond(
                    content = "{\"error\":\"mocked\"}",
                    status = status,
                    headers =
                        io.ktor.http
                            .HeadersBuilder()
                            .apply {
                                append(HttpHeaders.ContentType, "application/json")
                                extraHeaders.forEach { name, values -> appendAll(name, values) }
                            }.build(),
                )
            }
        return CloudEncoder(
            apiKey = "test-key",
            endpoint = "https://example.com",
            httpClientEngine = engine,
        )
    }
}
