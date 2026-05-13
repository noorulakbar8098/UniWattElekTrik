package com.example.uniwattelektrik.feature.admin.presentation.reports

import com.example.uniwattelektrik.feature.workforce.data.remote.MonthlyStockSnapshot
import com.example.uniwattelektrik.feature.workforce.data.remote.TaskRecord
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus

/* ═══════════════════════════════════════════════════════════════════════════
 *  ReportsTrends — pure-function aggregators for the multi-month sparklines
 *  on the Reports screen. The current report month is always the last point
 *  (so the chart trails it); earlier months are derived by re-running the
 *  per-month logic on the existing live flows.
 *
 *  Snapshot-based trends (stock value) accept a pre-fetched list of
 *  [MonthlyStockSnapshot]? — the screen subscribes to N adjacent snapshot
 *  docs and passes them here; missing months render as zeros so the chart
 *  shape stays interpretable.
 * ═══════════════════════════════════════════════════════════════════════════ */

/** A single (month, value) point on a sparkline. */
data class TrendPoint(val month: ReportMonth, val value: Float)

object ReportsTrends {

    /** Tasks completed per month, oldest → newest, [monthsBack] points incl. current. */
    fun tasksCompletedTrend(
        tasks       : List<TaskRecord>,
        currentMonth: ReportMonth,
        monthsBack  : Int = 6,
        tz          : TimeZone = TimeZone.currentSystemDefault(),
    ): List<TrendPoint> = trailingMonths(currentMonth, monthsBack).map { month ->
        val (start, end) = month.bounds(tz)
        val count = tasks.count { (it.completedAt ?: 0L) in start until end }
        TrendPoint(month, count.toFloat())
    }

    /**
     * Reopen rate per month as a 0..100 percentage, oldest → newest.
     * Defined as (reopen events landed in month / tasks completed in month) * 100.
     * Months with zero completions render as 0.
     */
    fun reopenRateTrend(
        tasks       : List<TaskRecord>,
        currentMonth: ReportMonth,
        monthsBack  : Int = 6,
        tz          : TimeZone = TimeZone.currentSystemDefault(),
    ): List<TrendPoint> = trailingMonths(currentMonth, monthsBack).map { month ->
        val (start, end) = month.bounds(tz)
        val completed = tasks.count { (it.completedAt ?: 0L) in start until end }
        val reopens   = tasks.sumOf { t -> t.reopens.count { it in start until end } }
        val pct       = if (completed == 0) 0f else (reopens.toFloat() / completed) * 100f
        TrendPoint(month, pct.coerceAtMost(100f))
    }

    /**
     * Stock value per month, oldest → newest. Each [snapshots] entry is the
     * end-of-month snapshot for that month (or null if the Cloud Function
     * hadn't run yet). Aligned with [trailingMonths]([currentMonth], n).
     */
    fun stockValueTrend(
        snapshots   : List<MonthlyStockSnapshot?>,
        currentMonth: ReportMonth,
        currentValue: Double,
        monthsBack  : Int = 6,
    ): List<TrendPoint> {
        val months = trailingMonths(currentMonth, monthsBack)
        return months.mapIndexed { i, m ->
            val v = if (i == months.lastIndex) {
                // The "current" point is the live closing value — the
                // snapshot for *this* month hasn't been written yet.
                currentValue.toFloat()
            } else {
                (snapshots.getOrNull(i)?.totalValue ?: 0.0).toFloat()
            }
            TrendPoint(m, v)
        }
    }

    /* ─── Helpers ───────────────────────────────────────────────────────── */

    /** Returns [monthsBack] [ReportMonth]s ending at [currentMonth] inclusive. */
    fun trailingMonths(currentMonth: ReportMonth, monthsBack: Int): List<ReportMonth> {
        val first = LocalDate(currentMonth.year, currentMonth.month, 1)
        return (monthsBack - 1 downTo 0).map { offset ->
            val d = first.minus(offset, DateTimeUnit.MONTH)
            ReportMonth(d.year, d.month)
        }
    }
}
