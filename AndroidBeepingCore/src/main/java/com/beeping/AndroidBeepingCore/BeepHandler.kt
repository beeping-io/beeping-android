package com.beeping.AndroidBeepingCore

internal class BeepHandler {
    private var listener: BeepingCoreEvent? = null

    fun addListener(listener: BeepingCoreEvent) {
        this.listener = listener
    }

    fun sendListener(beepId: String?) {
        if (beepId != null) {
            listener?.beepIdWith(beepId)
        }
    }
}
