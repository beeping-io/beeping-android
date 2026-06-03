package com.beeping.AndroidBeepingCore

/**
 * A decoded scheduled transmission, split into its code prefix and the beep's
 * position within the schedule — BEE-2314.
 *
 * Scheduled payloads have the layout `code + 4-char zero-padded base-32
 * timestamp`, emitted by [BeepingClient.sendScheduled] (BEE-2240). Recover one
 * from a decoded [BeepingPayload] via [BeepingPayload.parseScheduled].
 *
 * @property code The code prefix (the payload without the 4-char timestamp).
 * @property timestampSec The beep's rounded position within the schedule, in
 *   seconds. The beep index is `timestampSec / interval` for a caller that
 *   knows the schedule's interval.
 */
data class ScheduledPayload(
    val code: String,
    val timestampSec: Int,
)

/** Number of trailing base-32 timestamp chars in a scheduled payload. */
internal const val SCHEDULED_TIMESTAMP_CHARS = 4

/** Minimum scheduled-payload length: ≥1 code char + 4 timestamp chars. */
internal const val SCHEDULED_MIN_LENGTH = SCHEDULED_TIMESTAMP_CHARS + 1

/**
 * Pure assembly of a [ScheduledPayload] from a raw payload and a native-decoded
 * [timestampSec]. Returns `null` when the payload is too short or the native
 * parse failed (`timestampSec < 0`). Split out from [BeepingPayload.parseScheduled]
 * so the non-native logic is unit-testable.
 */
internal fun assembleScheduledPayload(
    payload: String,
    timestampSec: Int,
): ScheduledPayload? =
    if (payload.length < SCHEDULED_MIN_LENGTH || timestampSec < 0) {
        null
    } else {
        ScheduledPayload(code = payload.dropLast(SCHEDULED_TIMESTAMP_CHARS), timestampSec = timestampSec)
    }
