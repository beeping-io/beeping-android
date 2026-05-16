package com.beeping.sample

import android.app.Application
import com.beeping.AndroidBeepingCore.BeepingTimberTree

/**
 * BEE-64 — Application class. Plants [BeepingTimberTree] before any
 * `BeepingClient` is built so that the live-log [BeepingTimberTree.logs]
 * stream is wired from process start. The SDK Builder also calls
 * `installOnce()` defensively, but doing it here means the debug console
 * captures even pre-Builder logs (none today, but cheap insurance).
 */
class BeepingSampleApp : Application() {
    override fun onCreate() {
        super.onCreate()
        BeepingTimberTree.installOnce()
    }
}
