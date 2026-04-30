package com.example.uniwattelektrik.platform

import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * Returns the current wall-clock time formatted as 12-hour `hh:mm AM/PM`.
 *
 * Uses [TimeZone.currentSystemDefault] so it reflects the device locale.
 * `kotlin.time.Clock` is the modern Kotlin 2.x replacement for the
 * deprecated `kotlinx.datetime.Clock`; we bridge to kotlinx-datetime's
 * `LocalDateTime` via epoch milliseconds for KMP compatibility.
 */
@OptIn(ExperimentalTime::class)
fun currentTimeFormatted(): String {
    val ms     = Clock.System.now().toEpochMilliseconds()
    val now    = Instant.fromEpochMilliseconds(ms).toLocalDateTime(TimeZone.currentSystemDefault())
    val hour24 = now.hour
    val minute = now.minute
    val hour12 = when {
        hour24 == 0  -> 12
        hour24 > 12  -> hour24 - 12
        else         -> hour24
    }
    val ampm = if (hour24 < 12) "AM" else "PM"
    val hh   = hour12.toString().padStart(2, '0')
    val mm   = minute.toString().padStart(2, '0')
    return "$hh:$mm $ampm"
}

@OptIn(ExperimentalTime::class)
fun nowEpochMillis(): Long = Clock.System.now().toEpochMilliseconds()

/** Wall-clock minutes since midnight in the device's current time zone. */
@OptIn(ExperimentalTime::class)
fun minutesOfDay(epochMs: Long): Int {
    val ldt = Instant.fromEpochMilliseconds(epochMs)
        .toLocalDateTime(TimeZone.currentSystemDefault())
    return ldt.hour * 60 + ldt.minute
}
