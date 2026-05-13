package com.example.uniwattelektrik.feature.admin.presentation.reports

/* ═══════════════════════════════════════════════════════════════════════════
 *  ReportsAnomalies — Phase 3 / Case 3
 * ═══════════════════════════════════════════════════════════════════════════
 *
 *  Lightweight rule-based callouts that surface notable shifts at the top
 *  of the monthly report. Pure functions over the current [MonthlyReport]
 *  and (optionally) the previous month's report — no ML, no backend, no
 *  state. Rules are deliberately conservative so the strip stays signal,
 *  not noise: each rule needs both an absolute floor (so tiny workspaces
 *  don't trigger) AND a relative jump.
 *
 *  Adding a rule: drop a new function into [detect] that returns either
 *  null or an [Anomaly]. Keep the body short — admins glance, they don't
 *  read essays.
 */

enum class AnomalySeverity { INFO, WARNING, DANGER }

data class Anomaly(
    val severity: AnomalySeverity,
    val icon    : String,       // single glyph; rendered in a small badge
    val title   : String,       // one short clause
    val body    : String,       // one sentence with the supporting number
)

object ReportsAnomalies {

    fun detect(current: MonthlyReport, previous: MonthlyReport?): List<Anomaly> {
        val out = mutableListOf<Anomaly>()
        manualIssuanceSpike(current, previous)?.let { out += it }
        reopenRateAlert(current, previous)?.let { out += it }
        slaDrop(current, previous)?.let { out += it }
        zeroStockSurge(current)?.let { out += it }
        stockValueDrop(current)?.let { out += it }
        backlogGrowing(current)?.let { out += it }
        // Order: DANGER → WARNING → INFO so the eye lands on real fires first.
        return out.sortedBy { it.severity.ordinal * -1 }
    }

    /** Manual stock issuance up ≥2× MoM, with an absolute floor to keep
     *  small workspaces from tripping on noise. Signals inventory leakage. */
    private fun manualIssuanceSpike(curr: MonthlyReport, prev: MonthlyReport?): Anomaly? {
        val cur = curr.stock.consumedManualQty
        val pre = prev?.stock?.consumedManualQty ?: 0
        if (cur < 10 || pre <= 0) return null
        if (cur < pre * 2) return null
        val pct = ((cur - pre) * 100 / pre)
        return Anomaly(
            severity = AnomalySeverity.WARNING,
            icon     = "⚠",
            title    = "Manual issuance up $pct%",
            body     = "$cur units issued without a task link this month (was $pre). Possible inventory leakage.",
        )
    }

    /** Reopen rate at or above 15% AND up ≥10 pts MoM — i.e. genuine quality
     *  regression, not a one-off ticket. Without a previous month to compare,
     *  the 15% floor alone fires. */
    private fun reopenRateAlert(curr: MonthlyReport, prev: MonthlyReport?): Anomaly? {
        val cur = curr.tasks.reopenRatePct ?: return null
        if (cur < 15) return null
        val pre = prev?.tasks?.reopenRatePct
        if (pre != null && cur <= pre + 10) return null
        return Anomaly(
            severity = AnomalySeverity.DANGER,
            icon     = "↻",
            title    = "Reopen rate at $cur%",
            body     = if (pre != null) "Up from $pre% last month — review the top RCA causes below."
                       else "Quality regression — review the top RCA causes below.",
        )
    }

    /** On-time completion dropped ≥20 percentage points vs previous month
     *  AND the current month had at least 5 completed tasks (so a 1/2 →
     *  0/2 swing doesn't trigger). */
    private fun slaDrop(curr: MonthlyReport, prev: MonthlyReport?): Anomaly? {
        val cur = curr.tasks.slaCompliancePct ?: return null
        val pre = prev?.tasks?.slaCompliancePct ?: return null
        if (curr.tasks.completed < 5) return null
        if (cur >= pre - 20) return null
        return Anomaly(
            severity = AnomalySeverity.WARNING,
            icon     = "⏱",
            title    = "SLA dropped ${pre - cur} pts",
            body     = "On-time completion fell to $cur% (was $pre%) — chase the backlog before month-end.",
        )
    }

    /** Three or more items at zero stock right now. Reorder runway. */
    private fun zeroStockSurge(curr: MonthlyReport): Anomaly? {
        val n = curr.stock.zeroStockCount
        if (n < 3) return null
        return Anomaly(
            severity = AnomalySeverity.DANGER,
            icon     = "✕",
            title    = "$n items stocked-out",
            body     = "Reorder before they block field tasks — see the inventory section.",
        )
    }

    /** Closing stock value dropped ≥20% vs opening — flags either a large
     *  consumption month or value-misclassification on recent issuance. */
    private fun stockValueDrop(curr: MonthlyReport): Anomaly? {
        val opening = curr.stock.openingStockValue ?: return null
        if (opening <= 0.0) return null
        val closing = curr.stock.closingStockValue
        val pct = ((closing - opening) / opening) * 100.0
        if (pct >= -20.0) return null
        return Anomaly(
            severity = AnomalySeverity.WARNING,
            icon     = "↓",
            title    = "Stock value down ${kotlin.math.abs(pct).toInt()}%",
            body     = "Closing value below opening — confirm the issuance trail explains the drop.",
        )
    }

    /** Completion ratio under 50% with at least 10 tasks created — backlog
     *  is outrunning throughput. Info-level because it's not always bad. */
    private fun backlogGrowing(curr: MonthlyReport): Anomaly? {
        val created   = curr.tasks.created
        val completed = curr.tasks.completed
        if (created < 10) return null
        if (completed * 2 >= created) return null
        return Anomaly(
            severity = AnomalySeverity.INFO,
            icon     = "•",
            title    = "Backlog growing",
            body     = "$completed of $created tasks completed — pending work outpaces closure.",
        )
    }
}
