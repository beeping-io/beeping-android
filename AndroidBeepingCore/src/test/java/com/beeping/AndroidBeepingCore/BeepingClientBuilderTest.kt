package com.beeping.AndroidBeepingCore

import android.content.Context
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BeepingClientBuilderTest {
    private val context: Context = mockk(relaxed = true)

    @Test
    fun `Builder defaults to Local mode`() {
        val client = BeepingClient.Builder(context).build()
        assertNotNull("Builder should produce a client with Local default", client)
        client.close()
    }

    @Test
    fun `Builder build with Cloud mode succeeds with valid params`() {
        val client =
            BeepingClient
                .Builder(context)
                .mode(BeepingMode.Cloud(apiKey = "k", endpoint = "https://api.beeping.io"))
                .build()
        assertNotNull(client)
        client.close()
    }

    @Test
    fun `Builder Cloud mode requires non-blank apiKey`() {
        val ex =
            runCatching {
                BeepingClient
                    .Builder(context)
                    .mode(BeepingMode.Cloud(apiKey = "", endpoint = "https://api.beeping.io"))
                    .build()
            }.exceptionOrNull()

        assertTrue(
            "expected IllegalArgumentException, got ${ex?.javaClass?.simpleName}",
            ex is IllegalArgumentException,
        )
        assertTrue(
            "message should mention 'apiKey', got '${ex?.message}'",
            ex?.message?.contains("apiKey") == true,
        )
    }

    @Test
    fun `Builder Cloud mode rejects whitespace-only apiKey`() {
        val ex =
            runCatching {
                BeepingClient
                    .Builder(context)
                    .mode(BeepingMode.Cloud(apiKey = "   ", endpoint = "https://api.beeping.io"))
                    .build()
            }.exceptionOrNull()

        assertTrue(ex is IllegalArgumentException)
    }

    @Test
    fun `Builder Cloud mode validates endpoint URL pattern`() {
        val ex =
            runCatching {
                BeepingClient
                    .Builder(context)
                    .mode(BeepingMode.Cloud(apiKey = "k", endpoint = "not-a-url"))
                    .build()
            }.exceptionOrNull()

        assertTrue(
            "expected IllegalArgumentException, got ${ex?.javaClass?.simpleName}",
            ex is IllegalArgumentException,
        )
        assertTrue(
            "message should mention 'endpoint', got '${ex?.message}'",
            ex?.message?.contains("endpoint") == true,
        )
    }

    @Test
    fun `Builder logLevel and telemetryEnabled setters compose with mode`() {
        // BEE-58 storage only — wiring lives in BEE-60/61. Test verifies the
        // Builder accepts the calls and produces a client without throwing.
        val client =
            BeepingClient
                .Builder(context)
                .mode(BeepingMode.Local)
                .logLevel(LogLevel.DEBUG)
                .telemetryEnabled(true)
                .build()

        assertNotNull(client)
        client.close()
    }

    @Test
    fun `Builder is fluent — all setters return Builder for chaining`() {
        // Compile-time check: chaining must work without intermediate vals.
        val client =
            BeepingClient
                .Builder(context)
                .logLevel(LogLevel.NONE)
                .mode(BeepingMode.Local)
                .telemetryEnabled(false)
                .logLevel(LogLevel.VERBOSE) // re-setting same setter
                .build()

        assertNotNull(client)
        client.close()
    }
}
