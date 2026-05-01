package com.beeping.AndroidBeepingCore

import android.content.Context
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * Robolectric smoke test — verifies the test classpath can resolve a real
 * `Context` via [RuntimeEnvironment.getApplication] without an emulator.
 * This unblocks future tests that need real `Context`-backed code paths
 * (resource lookup, SharedPreferences, etc.) without paying the cost of
 * an instrumented run.
 *
 * `sdk = 33` is pinned because Robolectric ≤ 4.14 doesn't ship pre-built
 * shadows for SDK 35 (compileSdk). Library tests run on SDK 33 shadows —
 * production code targets SDK 35 unchanged.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class RobolectricSmokeTest {
    @Test
    fun `application context is resolvable`() {
        val ctx: Context = RuntimeEnvironment.getApplication()
        assertNotNull("Robolectric must provide an Application context", ctx)
        assertNotNull("Application context must expose its packageName", ctx.packageName)
    }
}
