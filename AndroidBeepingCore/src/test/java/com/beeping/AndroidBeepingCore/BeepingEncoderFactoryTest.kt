package com.beeping.AndroidBeepingCore

import android.content.Context
import io.mockk.mockk
import org.junit.Assert.assertTrue
import org.junit.Test

class BeepingEncoderFactoryTest {
    private val context: Context = mockk(relaxed = true)

    @Test
    fun `Local mode produces LocalEncoder`() {
        val encoder = BeepingEncoderFactory.create(BeepingMode.Local, context)
        assertTrue(
            "expected LocalEncoder, got ${encoder::class.simpleName}",
            encoder is LocalEncoder,
        )
        encoder.close()
    }

    @Test
    fun `Cloud mode produces CloudEncoder with apiKey and endpoint`() {
        val encoder =
            BeepingEncoderFactory.create(
                BeepingMode.Cloud(apiKey = "k", endpoint = "https://e"),
                context,
            )
        assertTrue(
            "expected CloudEncoder, got ${encoder::class.simpleName}",
            encoder is CloudEncoder,
        )
        encoder.close()
    }
}
