package com.beeping.AndroidBeepingCore

import org.junit.Assert.assertNotNull
import org.junit.Test

class BeepingCoreJNITest {
    @Test
    fun isNativeLoaded_isCallableWithoutException() {
        val loaded = BeepingCoreJNI.isNativeLoaded()
        // The native lib obviously won't load on the JVM unit test runtime;
        // the assertion here is just that the static accessor is reachable
        // and returns a non-null Boolean (i.e. did not throw).
        @Suppress("USELESS_IS_CHECK")
        assertNotNull(
            "isNativeLoaded should be callable and return a boolean value",
            loaded as Boolean?,
        )
    }
}
