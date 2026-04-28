/* ----------------------------------------------------------------------------
 * BeepingCoreLJNI java class
 * ----------------------------------------------------------------------------- */

package com.beeping.AndroidBeepingCore;

import android.util.Log;

public class BeepingCoreJNI {

  public static final int BC_TOKEN_START = 0;
  public static final int BC_TOKEN_END_OK = 1;
  public static final int BC_TOKEN_END_BAD = 2;
  public static final int BC_END_PLAY = 3;

  public BeepingCore mBeepingCore;
  private static final String TAG = "BEEPING:JNI";
  private static volatile boolean sNativeLoaded = false;

  static
  {
    try {
        System.loadLibrary("beepingcore");
        sNativeLoaded = true;
    } catch (UnsatisfiedLinkError e) {
        Log.e(TAG, "native code library failed to load.", e);
        sNativeLoaded = false;
    }
  }


  public BeepingCoreJNI(BeepingCore beepingCore)
  {
    mBeepingCore = beepingCore;
  }

  public static boolean isNativeLoaded() {
    return sNativeLoaded;
  }

  public void BeepingCallback(int value)
  {
    final String TAG = "BEEPING:JNI:TOKEN";

    if (value == BC_TOKEN_START) {
        mBeepingCore.BeepingCallback(value);
    }
    else if (value == BC_TOKEN_END_OK) {
        mBeepingCore.BeepingCallback(value);
    }
    else if (value == BC_TOKEN_END_BAD) {
        mBeepingCore.BeepingCallback(value);
    }
    else if (value == BC_END_PLAY) {
        mBeepingCore.BeepingCallback(value);
    }

  }

    public final native void start(long beepingObject);

    public final native long init();

    public final native int dealloc(long beepingObject);
    public final native int configure(int mode, long beepingObject);
    public final native int startBeepingListen(long beepingObject);
    public final native int stopBeepingListen(long beepingObject);
    public final native int getDecodedString(char[] code, long beepingObject);

}
