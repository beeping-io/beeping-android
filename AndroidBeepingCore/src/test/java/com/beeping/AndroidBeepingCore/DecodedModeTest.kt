package com.beeping.AndroidBeepingCore

import org.junit.Assert.assertEquals
import org.junit.Test

/** BEE-2313 — `BEEPING_GetDecodedMode` → [DecodedMode] mapping. */
class DecodedModeTest {
    @Test
    fun `fromRaw maps known beeping-core codes`() {
        assertEquals(DecodedMode.AUDIBLE, DecodedMode.fromRaw(0))
        assertEquals(DecodedMode.NON_AUDIBLE, DecodedMode.fromRaw(1))
        assertEquals(DecodedMode.HIDDEN, DecodedMode.fromRaw(2))
    }

    @Test
    fun `fromRaw maps unrecognized codes to UNKNOWN`() {
        assertEquals(DecodedMode.UNKNOWN, DecodedMode.fromRaw(-1))
        assertEquals(DecodedMode.UNKNOWN, DecodedMode.fromRaw(7))
        assertEquals(DecodedMode.UNKNOWN, DecodedMode.fromRaw(99))
    }
}
