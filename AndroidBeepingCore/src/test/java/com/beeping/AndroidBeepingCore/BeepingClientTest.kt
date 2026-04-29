package com.beeping.AndroidBeepingCore

import app.cash.turbine.test
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BeepingClientTest {

    private fun client() = BeepingClient(BeepingMode.Local, NoOpEncoder())

    @Test
    fun `listen emits Started immediately on collect and Stopped on close`() = runTest {
        val client = client()
        client.listen().test {
            assertEquals(BeepingEvent.Started, awaitItem())
            client.close()
            assertEquals(BeepingEvent.Stopped, awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `listen after close throws`() = runTest {
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
    fun `send throws NotImplementedError in BEE-56 shell`() = runTest {
        val client = client()
        try {
            client.send(BeepingPayload(payload = "TEST1"))
            error("Expected NotImplementedError")
        } catch (e: NotImplementedError) {
            assertNotNull("error must be raised by TODO()", e)
        } finally {
            client.close()
        }
    }

    @Test
    fun `close is idempotent`() = runTest {
        val client = client()
        client.close()
        client.close() // must not throw
    }

    @Test
    fun `BeepingMode Cloud carries apiKey and endpoint`() {
        val cloud = BeepingMode.Cloud(apiKey = "k", endpoint = "https://e")
        assertEquals("k", cloud.apiKey)
        assertEquals("https://e", cloud.endpoint)
    }
}

/** Test double that satisfies the [Encoder] contract without doing any I/O. */
private class NoOpEncoder : Encoder {
    override suspend fun encode(payload: String): ByteArray = ByteArray(0)
    override fun decoded(): Flow<BeepingPayload> = emptyFlow()
    override fun close() {}
}
