package com.beeping.AndroidBeepingCore

import app.cash.turbine.test
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BeepingClientTest {
    private fun client(encoder: BeepingEncoder = NoOpEncoder()) = BeepingClient(BeepingMode.Local, encoder)

    @Test
    fun `listen emits Started immediately on collect and Stopped on close`() =
        runTest {
            val client = client()
            client.listen().test {
                assertEquals(BeepingEvent.Started, awaitItem())
                client.close()
                assertEquals(BeepingEvent.Stopped, awaitItem())
                awaitComplete()
            }
        }

    @Test
    fun `listen after close throws`() =
        runTest {
            val client = client()
            client.close()
            client.listen().test {
                val error = awaitError()
                assertTrue(
                    "expected IllegalStateException, got ${error::class.simpleName}",
                    error is IllegalStateException,
                )
                assertTrue(
                    "message should mention 'closed': '${error.message}'",
                    error.message?.contains("closed") == true,
                )
            }
        }

    @Test
    fun `listen maps encoder decoded payload to BeepingEvent_Decoded`() =
        runTest {
            val payloads =
                listOf(
                    BeepingPayload(payload = "abc120000", confidence = 0.9f),
                    BeepingPayload(payload = "xyz890000", confidence = 1.0f),
                )
            val encoder =
                object : BeepingEncoder {
                    override suspend fun encode(key: String): ByteArray = ByteArray(0)

                    override suspend fun encodeScheduled(
                        key: String,
                        duration: Float,
                        startTime: Float,
                        interval: Float,
                        beepGainDb: Float,
                        audible: Boolean,
                    ): ByteArray = ByteArray(0)

                    override fun decoded(): Flow<BeepingPayload> =
                        flow {
                            payloads.forEach { emit(it) }
                        }

                    override fun close() {}
                }

            client(encoder).listen().test {
                assertEquals(BeepingEvent.Started, awaitItem())
                assertEquals(BeepingEvent.Decoded(payloads[0]), awaitItem())
                assertEquals(BeepingEvent.Decoded(payloads[1]), awaitItem())
                assertEquals(BeepingEvent.Stopped, awaitItem())
                awaitComplete()
            }
        }

    @Test
    fun `listen maps BeepingException to BeepingEvent_Failed`() =
        runTest {
            val error = BeepingError.AuthenticationFailed
            val encoder =
                object : BeepingEncoder {
                    override suspend fun encode(key: String): ByteArray = ByteArray(0)

                    override suspend fun encodeScheduled(
                        key: String,
                        duration: Float,
                        startTime: Float,
                        interval: Float,
                        beepGainDb: Float,
                        audible: Boolean,
                    ): ByteArray = ByteArray(0)

                    override fun decoded(): Flow<BeepingPayload> =
                        flow {
                            throw BeepingException(error)
                        }

                    override fun close() {}
                }

            client(encoder).listen().test {
                assertEquals(BeepingEvent.Started, awaitItem())
                assertEquals(BeepingEvent.Failed(error), awaitItem())
                assertEquals(BeepingEvent.Stopped, awaitItem())
                awaitComplete()
            }
        }

    @Test
    fun `BEE-2313 listen propagates ReceptionMetrics on the Decoded payload`() =
        runTest {
            val metrics =
                ReceptionMetrics(
                    confidence = 0.9f,
                    confidenceError = 0.1f,
                    confidenceNoise = 0.3f,
                    receivedBeepsVolume = 0.7f,
                    decodedMode = DecodedMode.NON_AUDIBLE,
                    decodingBeginFreq = 17_800f,
                    decodingEndFreq = 21_000f,
                )
            val payload = BeepingPayload(payload = "abc12", confidence = 0.9f, metrics = metrics)
            val encoder =
                object : BeepingEncoder {
                    override suspend fun encode(key: String): ByteArray = ByteArray(0)

                    override suspend fun encodeScheduled(
                        key: String,
                        duration: Float,
                        startTime: Float,
                        interval: Float,
                        beepGainDb: Float,
                        audible: Boolean,
                    ): ByteArray = ByteArray(0)

                    override fun decoded(): Flow<BeepingPayload> = flow { emit(payload) }

                    override fun close() {}
                }

            client(encoder).listen().test {
                assertEquals(BeepingEvent.Started, awaitItem())
                val decoded = awaitItem() as BeepingEvent.Decoded
                assertEquals(metrics, decoded.payload.metrics)
                assertEquals(BeepingEvent.Stopped, awaitItem())
                awaitComplete()
            }
        }

    @Test
    fun `listen maps AudioFocusLost to Failed then Stopped`() =
        runTest {
            // BEE-2307: LocalEncoder closes the decode flow with
            // BeepingException(AudioFocusLost) on a real loss of audio focus.
            // listen() must surface it as Failed(AudioFocusLost) then Stopped.
            val encoder =
                object : BeepingEncoder {
                    override suspend fun encode(key: String): ByteArray = ByteArray(0)

                    override suspend fun encodeScheduled(
                        key: String,
                        duration: Float,
                        startTime: Float,
                        interval: Float,
                        beepGainDb: Float,
                        audible: Boolean,
                    ): ByteArray = ByteArray(0)

                    override fun decoded(): Flow<BeepingPayload> =
                        flow {
                            throw BeepingException(BeepingError.AudioFocusLost)
                        }

                    override fun close() {}
                }

            client(encoder).listen().test {
                assertEquals(BeepingEvent.Started, awaitItem())
                assertEquals(BeepingEvent.Failed(BeepingError.AudioFocusLost), awaitItem())
                assertEquals(BeepingEvent.Stopped, awaitItem())
                awaitComplete()
            }
        }

    @Test
    fun `send delegates to encoder_encode and returns success when bytes returned`() =
        runTest {
            val expectedKey = "abc12"
            var receivedKey: String? = null
            val encoder =
                object : BeepingEncoder {
                    override suspend fun encode(key: String): ByteArray {
                        receivedKey = key
                        return byteArrayOf(0x52, 0x49, 0x46, 0x46) // "RIFF" magic
                    }

                    override suspend fun encodeScheduled(
                        key: String,
                        duration: Float,
                        startTime: Float,
                        interval: Float,
                        beepGainDb: Float,
                        audible: Boolean,
                    ): ByteArray = ByteArray(0)

                    override fun decoded(): Flow<BeepingPayload> = emptyFlow()

                    override fun close() {}
                }
            val client = client(encoder)

            val result = client.send(BeepingPayload(payload = expectedKey))

            assertTrue("send should succeed when encoder returns bytes", result.isSuccess)
            assertEquals(expectedKey, receivedKey)
            client.close()
        }

    @Test
    fun `send wraps BeepingException as Result_failure with BeepingException`() =
        runTest {
            val encoder =
                object : BeepingEncoder {
                    override suspend fun encode(key: String): ByteArray =
                        throw BeepingException(BeepingError.AuthenticationFailed)

                    override suspend fun encodeScheduled(
                        key: String,
                        duration: Float,
                        startTime: Float,
                        interval: Float,
                        beepGainDb: Float,
                        audible: Boolean,
                    ): ByteArray = ByteArray(0)

                    override fun decoded(): Flow<BeepingPayload> = emptyFlow()

                    override fun close() {}
                }
            val client = client(encoder)

            val result = client.send(BeepingPayload(payload = "abc12"))

            assertTrue(result.isFailure)
            val err = result.exceptionOrNull()
            assertTrue(
                "expected BeepingException, got ${err?.javaClass?.simpleName}",
                err is BeepingException,
            )
            assertEquals(BeepingError.AuthenticationFailed, (err as BeepingException).error)
            client.close()
        }

    @Test
    fun `close is idempotent`() =
        runTest {
            val client = client()
            client.close()
            client.close() // must not throw
        }

    @Test
    fun `BEE-2315 coreVersion throws NativeLibraryNotLoaded on the JVM`() {
        // No .so on the JVM test runtime — coreVersion must surface the typed error.
        val ex = runCatching { client().coreVersion() }.exceptionOrNull()
        assertTrue("expected BeepingException, got ${ex?.javaClass?.simpleName}", ex is BeepingException)
        assertEquals(BeepingError.NativeLibraryNotLoaded, (ex as BeepingException).error)
    }

    @Test
    fun `BEE-2316 setNativeLogPath returns false on the JVM where native is absent`() {
        assertFalse(BeepingClient.setNativeLogPath("/tmp/beeping.log"))
        assertFalse(BeepingClient.setNativeLogPath(null))
    }

    @Test
    fun `BeepingMode Cloud carries apiKey and endpoint`() {
        val cloud = BeepingMode.Cloud(apiKey = "k", endpoint = "https://e")
        assertEquals("k", cloud.apiKey)
        assertEquals("https://e", cloud.endpoint)
    }
}

/** Test double that satisfies the [BeepingEncoder] contract without doing any I/O. */
private class NoOpEncoder : BeepingEncoder {
    override suspend fun encode(key: String): ByteArray = ByteArray(0)

    override suspend fun encodeScheduled(
        key: String,
        duration: Float,
        startTime: Float,
        interval: Float,
        beepGainDb: Float,
        audible: Boolean,
    ): ByteArray = ByteArray(0)

    override fun decoded(): Flow<BeepingPayload> = emptyFlow()

    override fun close() {}
}
