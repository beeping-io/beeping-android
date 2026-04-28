package com.beeping.AndroidBeepingCore

/**
 * Callback interface implemented by the host Context (Activity / Application).
 *
 * Note: this interface is part of the **legacy public API** preserved for BEE-53.
 * It will be replaced by `Flow<BeepingEvent>` in BEE-56 with proper structured
 * concurrency and a sealed event hierarchy.
 */
interface BeepingCoreEvent {
    fun beepIdWith(beepId: String)
}
