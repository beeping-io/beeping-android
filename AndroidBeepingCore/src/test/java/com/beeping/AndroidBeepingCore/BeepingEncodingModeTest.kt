package com.beeping.AndroidBeepingCore

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * BEE-2305 — guards [BeepingEncodingMode] against drift from `beeping-core`'s
 * `BEEPING_MODE` enum and verifies the encode/decode configure mapping.
 */
class BeepingEncodingModeTest {
    @Test
    fun `rawValue matches beeping-core BEEPING_MODE constants`() {
        // Mirror of BeepingCoreLib_api.h: AUDIBLE=2, INAUDIBLE=3, ALL=5.
        assertEquals(2, BeepingEncodingMode.AUDIBLE.rawValue)
        assertEquals(3, BeepingEncodingMode.NON_AUDIBLE.rawValue)
        assertEquals(5, BeepingEncodingMode.ALL.rawValue)
    }

    @Test
    fun `exactly three selectable modes are exposed`() {
        // beeping-core v0.8.1 has no selectable 'hidden' configure mode.
        assertEquals(3, BeepingEncodingMode.entries.size)
    }

    @Test
    fun `encodeConfigureMode passes AUDIBLE and NON_AUDIBLE through and maps ALL to inaudible`() {
        assertEquals(2, BeepingEncodingMode.AUDIBLE.encodeConfigureMode)
        assertEquals(3, BeepingEncodingMode.NON_AUDIBLE.encodeConfigureMode)
        // ALL is decode-only → encode falls back to inaudible (production default).
        assertEquals(3, BeepingEncodingMode.ALL.encodeConfigureMode)
    }

    @Test
    fun `decodeConfigureMode is the rawValue for every mode`() {
        assertEquals(2, BeepingEncodingMode.AUDIBLE.decodeConfigureMode)
        assertEquals(3, BeepingEncodingMode.NON_AUDIBLE.decodeConfigureMode)
        assertEquals(5, BeepingEncodingMode.ALL.decodeConfigureMode)
    }
}
