package com.example.uniwattelektrik.feature.admin.presentation.reports

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.Month
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.daysUntil
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/* ═══════════════════════════════════════════════════════════════════════════
 *  Reports — monthly aggregation models
 * ═══════════════════════════════════════════════════════════════════════════
 *
 *  All data is computed client-side from the live workforce / inventory flows
 *  — no backend changes required. See ReportsAggregator for the pure
 *  build function and ReportsCsv for the export serializer.
 */

/** Identifies a specific calendar month for a report. */
data class ReportMonth(val year: Int, val month: Month) {
    fun label(): String = "${month.shortName()} $year"

    companion object {
        @OptIn(ExperimentalTime::class)
        fun current(): ReportMonth {
            val now = Instant.fromEpochMilliseconds(Clock.System.now().toEpochMilliseconds())
                .toLocalDateTime(TimeZone.currentSystemDefault())
            return ReportMonth(now.year, now.month)
        }
    }
}

/** Inclusive-exclusive start/end millis for the given [month] in [tz]. */
@OptIn(ExperimentalTime::class)
fun ReportMonth.bounds(tz: TimeZone = TimeZone.currentSystemDefault()): Pair<Long, Long> {
    val first = LocalDate(year, month, 1)
    val nextFirst = first.plus(1, DateTimeUnit.MONTH)
    val startMs = first.atStartOfDayIn(tz).toEpochMilliseconds()
    val endMs   = nextFirst.atStartOfDayIn(tz).toEpochMilliseconds()
    return startMs to endMs
}

/** Total inclusive days in the month — used for "X / Y days" denominators. */
fun ReportMonth.dayCount(): Int {
    val first = LocalDate(year, month, 1)
    val nextFirst = first.plus(1, DateTimeUnit.MONTH)
    return first.daysUntil(nextFirst)
}

/* ─── Top-level report ────────────────────────────────────────────────── */

data class MonthlyReport(
    val month     : ReportMonth,
    val employees : List<EmployeeReportRow>,
    val tasks     : TaskReportSummary,
    val stock     : StockReportSummary,
) {
    val generatedAtMs: Long
        @OptIn(ExperimentalTime::class)
        get() = Clock.System.now().toEpochMilliseconds()
}

/* ─── Employee section ────────────────────────────────────────────────── */

data class EmployeeReportRow(
    val userId             : String,
    val name               : String,
    val initials           : String,
    val role               : String,
    val daysPresent        : Int,
    val daysLate           : Int,
    val daysAbsent         : Int,
    val daysOnLeave        : Int,
    val tasksAssigned      : Int,
    val tasksCompleted     : Int,
    val totalWorkHoursH    : Int,
    val punctualityPct     : Int,
    /** Of tasks this employee completed in the month, % completed on or before dueDate. */
    val onTimeCompletionPct: Int?,
    /** Count of reopen events on this employee's tasks during the month. */
    val reopensInMonth     : Int,
    /**
     * Quality score 0–100: weighted blend of on-time completion %, punctuality %,
     * and (1 − reopen-rate). Null when the employee has no completed tasks.
     */
    val qualityScore       : Int?,
)

/* ─── Task section ────────────────────────────────────────────────────── */

data class TaskReportSummary(
    val created                 : Int,
    val completed               : Int,
    val pending                 : Int,
    val overdue                 : Int,
    val byPriority              : Map<String, Int>,   // "High" / "Medium" / "Low"
    val byDepartment            : List<Pair<String, Int>>,
    val topRcaCauses            : List<Pair<String, Int>>,
    val avgResolutionMinutes    : Long?,
    val slaCompliancePct        : Int?,
    val totalDowntimeHours      : Int,
    /** Total reopen events recorded during this month across all tasks. */
    val reopensInMonth          : Int,
    /** Reopen rate: reopens this month / tasks completed this month, as %. */
    val reopenRatePct           : Int?,
)

/* ─── Stock section ───────────────────────────────────────────────────── */

data class StockReportSummary(
    val closingStockQty       : Int,
    val closingStockValue     : Double,
    val itemsConsumedQty      : Int,
    val itemsConsumedValue    : Double,
    val topConsumed           : List<TopConsumedRow>,
    val lowStockCount         : Int,
    val zeroStockCount        : Int,
    // ── Phase 2 / Case 2: consumption breakdown by source ────────────────
    /** Units issued via task signoff (transaction had a taskId). */
    val consumedFromTasksQty  : Int = 0,
    /** Rupee value of issued-via-task units. */
    val consumedFromTasksValue: Double = 0.0,
    /** Units issued without a task link (manual stock corrections). */
    val consumedManualQty     : Int = 0,
    val consumedManualValue   : Double = 0.0,
    /** Top equipment categories by consumed units, derived via taskId join. */
    val consumptionByEquipment: List<Pair<String, Int>> = emptyList(),
    // ── Phase 2 / Case 3: opening-vs-closing (from monthly snapshot) ─────
    /** Opening stock units at the start of the report month. Null when no
     *  snapshot exists yet (e.g. very first month of an admin's account
     *  before the Cloud Function has run). */
    val openingStockQty       : Int?    = null,
    val openingStockValue     : Double? = null,
    val qtyDelta              : Int?    = null,    // closing − opening
    val valueDelta            : Double? = null,
)

data class TopConsumedRow(
    val itemId   : String,
    val itemName : String,
    val qty      : Int,
    val value    : Double,
)

/* ─── Helpers ─────────────────────────────────────────────────────────── */

internal fun Month.shortName(): String = when (this) {
    Month.JANUARY -> "Jan"; Month.FEBRUARY -> "Feb"; Month.MARCH -> "Mar"
    Month.APRIL   -> "Apr"; Month.MAY -> "May"; Month.JUNE -> "Jun"
    Month.JULY    -> "Jul"; Month.AUGUST -> "Aug"; Month.SEPTEMBER -> "Sep"
    Month.OCTOBER -> "Oct"; Month.NOVEMBER -> "Nov"; Month.DECEMBER -> "Dec"
    else          -> name.take(3).lowercase().replaceFirstChar { it.uppercase() }
}

internal fun Month.fullName(): String = when (this) {
    Month.JANUARY   -> "January";   Month.FEBRUARY -> "February"
    Month.MARCH     -> "March";     Month.APRIL    -> "April"
    Month.MAY       -> "May";       Month.JUNE     -> "June"
    Month.JULY      -> "July";      Month.AUGUST   -> "August"
    Month.SEPTEMBER -> "September"; Month.OCTOBER  -> "October"
    Month.NOVEMBER  -> "November";  Month.DECEMBER -> "December"
    else            -> name.lowercase().replaceFirstChar { it.uppercase() }
}
