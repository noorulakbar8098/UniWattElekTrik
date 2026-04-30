package com.example.uniwattelektrik.feature.workforce.domain

import com.example.uniwattelektrik.feature.workforce.data.remote.AttendanceRecord
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Pure domain helpers for the Check-In / Check-Out feature.
 *
 * Lives in `commonMain` so both the user app (writes attendance) and the admin
 * console (reads + classifies) share the same logic — no per-screen drift.
 */

/* ────────────────────────────────────────────────────────────────────────
 * Shift configuration
 * ──────────────────────────────────────────────────────────────────────── */

/**
 * Defines the working hours used to classify a check-in as on-time or late
 * and to decide whether a missing check-in should be treated as Absent.
 *
 * Times are wall-clock local hours/minutes (no timezone) — paired with
 * [todayStartMs] (already in the device's local TZ) at use-sites.
 */
data class ShiftConfig(
    val shiftStartHour: Int,
    val shiftStartMinute: Int,
    val shiftEndHour: Int,
    val shiftEndMinute: Int,
    /** Tolerance after [shiftStartHour]/[shiftStartMinute] before late kicks in. */
    val gracePeriodMins: Int = 15,
) {
    fun startMillisFromMidnight(): Long =
        (shiftStartHour * 60L + shiftStartMinute) * 60_000L

    fun endMillisFromMidnight(): Long =
        (shiftEndHour * 60L + shiftEndMinute) * 60_000L

    fun graceMillis(): Long = gracePeriodMins * 60_000L

    companion object {
        /** Sensible default: 09:00 – 18:00 with a 15-minute grace window. */
        val Default = ShiftConfig(9, 0, 18, 0, 15)

        /** Maps the existing per-employee shift key on `EmployeeRecord`. */
        fun fromShiftKey(key: String): ShiftConfig = when (key) {
            "Shift2" -> ShiftConfig(13, 0, 23, 0, 15)
            else     -> Default            // "Shift1" or unknown
        }
    }
}

/* ────────────────────────────────────────────────────────────────────────
 * Geo-fence
 * ──────────────────────────────────────────────────────────────────────── */

/** A circular worksite anchor used to validate check-in locations. */
data class GeoFence(
    val lat: Double,
    val lng: Double,
    val radiusMeters: Double,
)

/** Returns `true` if the point [pLat], [pLng] is within the fence radius. */
fun GeoFence.contains(pLat: Double, pLng: Double): Boolean =
    haversineMeters(lat, lng, pLat, pLng) <= radiusMeters

/** Great-circle distance in metres (earth radius 6 371 000 m). */
fun haversineMeters(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
    val r = 6_371_000.0
    val dLat = (lat2 - lat1).toRadians()
    val dLng = (lng2 - lng1).toRadians()
    val a = sin(dLat / 2).let { it * it } +
        cos(lat1.toRadians()) * cos(lat2.toRadians()) *
        sin(dLng / 2).let { it * it }
    val c = 2 * atan2(sqrt(a), sqrt(1 - a))
    return r * c
}

private fun Double.toRadians(): Double = this * PI / 180.0

/* ────────────────────────────────────────────────────────────────────────
 * Attendance state machine
 * ──────────────────────────────────────────────────────────────────────── */

/** The 4 states an employee can be in for a given working day. */
enum class AttendanceState { NotCheckedIn, CheckedIn, CheckedOut, Absent }

/**
 * Computes the current [AttendanceState] for an employee given:
 *  - [record]: today's attendance row (null if none yet),
 *  - [shift]: their shift configuration,
 *  - [nowMs]: wall-clock time,
 *  - [todayStartMs]: midnight of the current local day, in epoch millis.
 *
 * The "Absent" classification is purely synthesised on the read side — it
 * fires only after `shiftStart + grace` has passed with no record. We never
 * write Absent to Firestore (that would require a Cloud Function).
 */
fun attendanceStateFor(
    record: AttendanceRecord?,
    shift: ShiftConfig,
    nowMs: Long,
    todayStartMs: Long,
): AttendanceState {
    if (record != null) {
        return if (record.checkOutMs != null) AttendanceState.CheckedOut
        else AttendanceState.CheckedIn
    }
    val absentThreshold = todayStartMs + shift.startMillisFromMidnight() + shift.graceMillis()
    return if (nowMs >= absentThreshold) AttendanceState.Absent
    else AttendanceState.NotCheckedIn
}

/**
 * Returns `"ON_TIME"` if [nowMs] falls before `shiftStart + grace`, else `"LATE"`.
 *
 * Stored verbatim in `attendance/{id}.checkInStatus` so the admin doesn't have
 * to re-classify on read.
 */
fun classifyCheckIn(
    nowMs: Long,
    todayStartMs: Long,
    shift: ShiftConfig,
): String {
    val cutoff = todayStartMs + shift.startMillisFromMidnight() + shift.graceMillis()
    return if (nowMs <= cutoff) "ON_TIME" else "LATE"
}

/**
 * Total minutes a check-in is late by (0 if on-time or no record).
 * Useful for the "+12m" drift chip in the admin list.
 */
fun lateByMinutes(
    record: AttendanceRecord,
    shift: ShiftConfig,
    todayStartMs: Long,
): Long {
    val shiftStart = todayStartMs + shift.startMillisFromMidnight()
    return if (record.checkInMs > shiftStart)
        ((record.checkInMs - shiftStart) / 60_000L).coerceAtLeast(0L)
    else 0L
}

