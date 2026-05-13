package com.example.uniwattelektrik.feature.admin.presentation.reports

/* ═══════════════════════════════════════════════════════════════════════════
 *  ReportsCsv — turn a MonthlyReport into shareable CSV strings.
 *
 *  We emit one CSV per section so admins can drop each into a different
 *  spreadsheet tab. Each section function returns a self-contained CSV
 *  including its header row; combine() concatenates them with a blank
 *  separator so a single-file export remains readable.
 * ═══════════════════════════════════════════════════════════════════════════ */

object ReportsCsv {

    /** All three sections as a single string — suitable for one-file export. */
    fun combined(report: MonthlyReport): String = buildString {
        appendLine("# UniWatt Elektrik — Monthly Report")
        appendLine("# Period: ${report.month.label()}")
        appendLine()
        appendLine("## Employees")
        append(employees(report))
        appendLine()
        appendLine("## Tasks")
        append(tasks(report))
        appendLine()
        appendLine("## Stock")
        append(stock(report))
    }

    fun employees(report: MonthlyReport): String = buildString {
        appendLine(csvLine(listOf(
            "Employee", "Role", "Days Present", "Days Late", "Days Absent",
            "Days On Leave", "Tasks Assigned", "Tasks Completed",
            "Work Hours", "Punctuality %", "On-time Completion %",
            "Reopens", "Quality Score",
        )))
        report.employees.forEach { row ->
            appendLine(csvLine(listOf(
                row.name,
                row.role,
                row.daysPresent.toString(),
                row.daysLate.toString(),
                row.daysAbsent.toString(),
                row.daysOnLeave.toString(),
                row.tasksAssigned.toString(),
                row.tasksCompleted.toString(),
                row.totalWorkHoursH.toString(),
                "${row.punctualityPct}%",
                row.onTimeCompletionPct?.let { "$it%" } ?: "—",
                row.reopensInMonth.toString(),
                row.qualityScore?.toString() ?: "—",
            )))
        }
    }

    fun tasks(report: MonthlyReport): String = buildString {
        val t = report.tasks
        appendLine(csvLine(listOf("Metric", "Value")))
        appendLine(csvLine(listOf("Created",        t.created.toString())))
        appendLine(csvLine(listOf("Completed",      t.completed.toString())))
        appendLine(csvLine(listOf("Pending",        t.pending.toString())))
        appendLine(csvLine(listOf("Overdue",        t.overdue.toString())))
        appendLine(csvLine(listOf(
            "Avg resolution (min)",
            t.avgResolutionMinutes?.toString() ?: "—",
        )))
        appendLine(csvLine(listOf(
            "SLA compliance",
            t.slaCompliancePct?.let { "$it%" } ?: "—",
        )))
        appendLine(csvLine(listOf("Total downtime (hrs)", t.totalDowntimeHours.toString())))
        appendLine(csvLine(listOf("Reopens this month",   t.reopensInMonth.toString())))
        appendLine(csvLine(listOf(
            "Reopen rate",
            t.reopenRatePct?.let { "$it%" } ?: "—",
        )))

        appendLine()
        appendLine(csvLine(listOf("Priority", "Count")))
        t.byPriority.entries
            .sortedByDescending { it.value }
            .forEach { (k, v) -> appendLine(csvLine(listOf(k, v.toString()))) }

        if (t.byDepartment.isNotEmpty()) {
            appendLine()
            appendLine(csvLine(listOf("Department", "Tasks")))
            t.byDepartment.forEach { (k, v) ->
                appendLine(csvLine(listOf(k, v.toString())))
            }
        }

        if (t.topRcaCauses.isNotEmpty()) {
            appendLine()
            appendLine(csvLine(listOf("Top RCA causes", "Count")))
            t.topRcaCauses.forEach { (k, v) ->
                appendLine(csvLine(listOf(k, v.toString())))
            }
        }
    }

    fun stock(report: MonthlyReport): String = buildString {
        val s = report.stock
        appendLine(csvLine(listOf("Metric", "Value")))
        // Opening / closing snapshot rows (omitted when no snapshot exists).
        if (s.openingStockQty != null) {
            appendLine(csvLine(listOf("Opening stock units", s.openingStockQty.toString())))
            appendLine(csvLine(listOf(
                "Opening stock value",
                s.openingStockValue?.let { formatMoney(it) } ?: "—",
            )))
        }
        appendLine(csvLine(listOf("Closing stock units", s.closingStockQty.toString())))
        appendLine(csvLine(listOf("Closing stock value", formatMoney(s.closingStockValue))))
        if (s.qtyDelta != null) {
            val sign = if (s.qtyDelta > 0) "+" else ""
            appendLine(csvLine(listOf("Stock units Δ", "$sign${s.qtyDelta}")))
            s.valueDelta?.let { d ->
                val vSign = if (d > 0) "+" else ""
                appendLine(csvLine(listOf("Stock value Δ", "$vSign${formatMoney(d)}")))
            }
        }
        appendLine(csvLine(listOf("Items consumed (units)", s.itemsConsumedQty.toString())))
        appendLine(csvLine(listOf("Items consumed (value)", formatMoney(s.itemsConsumedValue))))
        appendLine(csvLine(listOf("Issued via tasks (units)", s.consumedFromTasksQty.toString())))
        appendLine(csvLine(listOf("Issued via tasks (value)", formatMoney(s.consumedFromTasksValue))))
        appendLine(csvLine(listOf("Manual issuance (units)", s.consumedManualQty.toString())))
        appendLine(csvLine(listOf("Manual issuance (value)", formatMoney(s.consumedManualValue))))
        appendLine(csvLine(listOf("Low-stock items",  s.lowStockCount.toString())))
        appendLine(csvLine(listOf("Out-of-stock items", s.zeroStockCount.toString())))

        if (s.topConsumed.isNotEmpty()) {
            appendLine()
            appendLine(csvLine(listOf("Item", "Qty consumed", "Value")))
            s.topConsumed.forEach { row ->
                appendLine(csvLine(listOf(
                    row.itemName,
                    row.qty.toString(),
                    formatMoney(row.value),
                )))
            }
        }

        if (s.consumptionByEquipment.isNotEmpty()) {
            appendLine()
            appendLine(csvLine(listOf("Equipment", "Qty consumed")))
            s.consumptionByEquipment.forEach { (equipment, qty) ->
                appendLine(csvLine(listOf(equipment, qty.toString())))
            }
        }
    }

    /* ─── Helpers ──────────────────────────────────────────────────────── */

    /**
     * Format a row to a CSV-safe line. Wraps fields that contain a comma,
     * quote, or newline in double-quotes and escapes inner double-quotes.
     */
    private fun csvLine(fields: List<String>): String =
        fields.joinToString(",") { field ->
            val needsQuote = field.any { it == ',' || it == '"' || it == '\n' || it == '\r' }
            if (needsQuote) {
                "\"" + field.replace("\"", "\"\"") + "\""
            } else {
                field
            }
        }

    /** Minimal money formatter — two decimals, no locale handling for portability. */
    private fun formatMoney(d: Double): String {
        if (d == 0.0) return "0.00"
        val cents = kotlin.math.round(d * 100.0).toLong()
        val whole = cents / 100
        val frac  = (cents % 100).let { if (it < 0) -it else it }
        return "$whole.${frac.toString().padStart(2, '0')}"
    }
}
