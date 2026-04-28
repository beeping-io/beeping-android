package com.beeping.AndroidBeepingCore;

import org.junit.Test;

import static org.junit.Assert.assertNotNull;

public class BeepingCoreJNITest {

    @Test
    public void isNativeLoaded_isCallableWithoutException() {
        Boolean loaded = Boolean.valueOf(BeepingCoreJNI.isNativeLoaded());
        assertNotNull("isNativeLoaded should be callable and return a boolean value", loaded);
    }
}
