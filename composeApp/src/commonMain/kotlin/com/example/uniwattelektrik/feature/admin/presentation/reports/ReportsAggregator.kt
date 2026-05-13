package com.example.uniwattelektrik.feature.admin.presentation.reports

import com.example.uniwattelektrik.feature.admin.presentation.screens.inventory.SpareItem
import com.example.uniwattelektrik.feature.workforce.data.remote.AttendanceRecord
import com.example.uniwattelektrik.feature.workforce.data.remote.EmployeeRecord
import com.example.uniwattelektrik.feature.workforce.data.remote.InventoryTransaction
import com.example.uniwattelektrik.feature.workforce.data.remote.LeaveRecord
import com.example.uniwattelektrik.feature.workforce.data.remote.MonthlyStockSnapshot
import com.example.uniwattelektrik.feature.workforce.data.remote.TaskRecord
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.daysUntil
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import kotlin.time.ExperimentalTime

/* ═══════════════════════════════════════════════════════════════════════════
 *  ReportsAggregator — pure functions, no side-effects, no Compose imports.
 *
 *  Takes the live snapshots of the workforce / inventory flows and produces a
 *  fully-resolved [MonthlyReport]. Designed to be cheap enough to run on the
 *  main thread for typical workforce sizes (≤ a few hundred employees and
 *  a few thousand tasks per month). Heavier workloads should move this to
 *  a background dispatcher.
 * ═══════════════════════════════════════════════════════════════════════════ */

object ReportsAggregator {

    @OptIn(ExperimentalTime::class)
    fun build(
        month           : ReportMonth,
        employees       : List<EmployeeRecord>,
        tasks           : List<TaskRecord>,
        attendance      : List<AttendanceRecord>,
        leaves          : List<LeaveRecord>,
        spares          : List<SpareItem>,
        // Phase 2 / Case 2 — optional, empty list keeps the breakdown
        // collapsed (back-compat with any callers that haven't migrated).
        inventoryTxns   : List<InventoryTransaction> = emptyList(),
        // Phase 2 / Case 3 — snapshot of stock at the START of the report
        // month (written by the snapshotMonthlyStock Cloud Function). Null
        // when no snapshot exists yet → delta panel stays hidden.
        openingSnapshot : MonthlyStockSnapshot? = null,
        tz              : TimeZone = TimeZone.currentSystemDefault(),
    ): MonthlyReport {
        val (startMs, endMs) = month.bounds(tz)

        // Active employees (the report ignores deleted / inactive accounts).
        val activeEmps = employees.filter { it.deletedAt == null && it.status != "Inactive" }

        return MonthlyReport(
            month     = month,
            employees = buildEmployeeRows(activeEmps, tasks, attendance, leaves, month, startMs, endMs, tz),
            tasks     = buildTaskSummary(tasks, startMs, endMs),
            stock     = buildStockSummary(tasks, spares, inventoryTxns, openingSnapshot, startMs, endMs),
        )
    }

    /* ─── Employee section ─────────────────────────────────────────────── */

    @OptIn(ExperimentalTime::class)
    private fun buildEmployeeRows(
        employees : List<EmployeeRecord>,
        tasks     : List<TaskRecord>,
        attendance: List<AttendanceRecord>,
        leaves    : List<LeaveRecord>,
        month     : ReportMonth,
        startMs   : Long,
        endMs     : Long,
        tz        : TimeZone,
    ): List<EmployeeReportRow> {
        val daysInMonth = month.dayCount()

        // Pre-compute approved leave dates per user (set of LocalDate objects).
        val approvedLeavesByUser: Map<String, Set<LocalDate>> = leaves
            .filter { it.status.equals("approved", ignoreCase = true) }
            .groupBy { it.userId }
            .mapValues { (_, list) ->
                list.flatMap { lr ->
                    val from = Instant.fromEpochMilliseconds(lr.fromDateMs).toLocalDateTime(tz).date
                    val to   = Instant.fromEpochMilliseconds(lr.toDateMs).toLocalDateTime(tz).date
                    val span = from.daysUntil(to).coerceAtLeast(0)
                    (0..span).map { from.plus(it, DateTimeUnit.DAY) }
                }.toSet()
            }

        val attendanceByUser: Map<String, List<AttendanceRecord>> = attendance
            .filter { it.checkInMs in startMs until endMs }
            .groupBy { it.userId }

        val tasksByUser: Map<String?, List<TaskRecord>> = tasks
            .filter { it.userId != null }
            .groupBy { it.userId }

        return employees.map { emp ->
            val empAttendance = attendanceByUser[emp.id].orEmpty()

            // ── Attendance counters ────────────────────────────────────
            val daysPresent = empAttendance.count {
                it.checkInStatus.equals("ON_TIME", ignoreCase = true)
            }
            val daysLate = empAttendance.count {
                it.checkInStatus.equals("LATE", ignoreCase = true)
            }
            val leaveDates = approvedLeavesByUser[emp.id].orEmpty()
            // Count only leave dates that actually fall inside the report month.
            val daysOnLeave = leaveDates.count { d ->
                d.year == month.year && d.month == month.month
            }
            val daysWorked = daysPresent + daysLate
            val daysAbsent = (daysInMonth - daysWorked - daysOnLeave).coerceAtLeast(0)

            val punctualityPct = if (daysWorked > 0) (daysPresent * 100) / daysWorked else 0

            // ── Total work hours ───────────────────────────────────────
            val totalWorkMillis = empAttendance.sumOf { rec ->
                val out = rec.checkOutMs ?: return@sumOf 0L
                (out - rec.checkInMs).coerceAtLeast(0L)
            }
            val totalWorkHoursH = (totalWorkMillis / 3_600_000L).toInt()

            // ── Tasks for this employee in the month ────────────────────
            val empTasks = tasksByUser[emp.id].orEmpty()
            val tasksAssignedM = empTasks.count {
                val created = it.createdAtMs ?: 0L
                created in startMs until endMs
            }
            val completedTasksM = empTasks.filter { t ->
                val done = t.completedAt ?: 0L
                done in startMs until endMs
            }
            val tasksCompletedM = completedTasksM.size

            val onTimeCompletionPct = if (completedTasksM.isEmpty()) {
                null
            } else {
                val onTime = completedTasksM.count { t ->
                    val done = t.completedAt ?: return@count false
                    val due  = t.dueDate ?: return@count true   // no due date → never late
                    done <= due
                }
                (onTime * 100) / completedTasksM.size
            }

            // Count reopen events that fell inside the month for tasks
            // assigned to this employee — drives the per-row "Quality score".
            val reopensM = empTasks.sumOf { t ->
                t.reopens.count { it in startMs until endMs }
            }

            val qualityScore: Int? = if (completedTasksM.isEmpty()) {
                null
            } else {
                // 50% on-time + 30% punctuality + 20% (1 − reopen rate).
                val onTimeWeight = (onTimeCompletionPct ?: 100) * 0.50
                val punctWeight  = punctualityPct * 0.30
                val reopenRate   = reopensM.toDouble() / completedTasksM.size.toDouble()
                val qualityWeight = ((1.0 - reopenRate).coerceIn(0.0, 1.0) * 100) * 0.20
                (onTimeWeight + punctWeight + qualityWeight).toInt().coerceIn(0, 100)
            }

            EmployeeReportRow(
                userId              = emp.id,
                name                = emp.name.ifBlank { "—" },
                initials            = emp.name.take(2).uppercase().ifBlank { "??" },
                role                = emp.role.ifBlank { "Employee" },
                daysPresent         = daysPresent,
                daysLate            = daysLate,
                daysAbsent          = daysAbsent,
                daysOnLeave         = daysOnLeave,
                tasksAssigned       = tasksAssignedM,
                tasksCompleted      = tasksCompletedM,
                totalWorkHoursH     = totalWorkHoursH,
                punctualityPct      = punctualityPct,
                onTimeCompletionPct = onTimeCompletionPct,
                reopensInMonth      = reopensM,
                qualityScore        = qualityScore,
            )
        }
            // Order: quality first (drives the "top performer" framing),
            // then tasks-completed for raw productivity, then name.
            .sortedWith(
                compareByDescending<EmployeeReportRow> { it.qualityScore ?: -1 }
                    .thenByDescending { it.tasksCompleted }
                    .thenBy { it.name.lowercase() },
            )
    }

    /* ─── Task section ──────────────────────────────────────────────────── */

    private fun buildTaskSummary(
        tasks   : List<TaskRecord>,
        startMs : Long,
        endMs   : Long,
    ): TaskReportSummary {
        // Tasks created in the month — used for "created" total + breakdowns.
        val createdInMonth = tasks.filter {
            val c = it.createdAtMs ?: 0L
            c in startMs until endMs
        }
        // Tasks completed in the month (regardless of creation date) — used
        // for resolution / SLA stats. A task created in March and completed
        // in April rolls into April's report.
        val completedInMonth = tasks.filter {
            val d = it.completedAt ?: return@filter false
            d in startMs until endMs
        }

        val createdCount   = createdInMonth.size
        val completedCount = completedInMonth.size

        // "Pending right now" within month context = created in month but
        // not yet done by the end of the month.
        val pendingCount = createdInMonth.count {
            (it.completedAt ?: Long.MAX_VALUE) >= endMs && it.status != "Done"
        }
        val overdueCount = createdInMonth.count { t ->
            val due = t.dueDate ?: return@count false
            t.status != "Done" && due < endMs
        }

        val byPriority: Map<String, Int> = createdInMonth
            .groupingBy { normalizePriorityLabel(it.priority) }
            .eachCount()

        val byDepartment: List<Pair<String, Int>> = createdInMonth
            .filter { it.departmentName.isNotBlank() }
            .groupingBy { it.departmentName }
            .eachCount()
            .toList()
            .sortedByDescending { it.second }
            .take(5)

        val topRca: List<Pair<String, Int>> = completedInMonth
            .filter { it.rca.isNotBlank() }
            .groupingBy { it.rca }
            .eachCount()
            .toList()
            .sortedByDescending { it.second }
            .take(5)

        val avgResolutionMs: Long? = completedInMonth
            .mapNotNull { it.totalWorkDurationMs?.takeIf { d -> d > 0 } }
            .takeIf { it.isNotEmpty() }
            ?.let { it.sum() / it.size }
        val avgResolutionMinutes = avgResolutionMs?.let { it / 60_000L }

        val slaCompliancePct: Int? = if (completedInMonth.isEmpty()) {
            null
        } else {
            val onTime = completedInMonth.count { t ->
                val done = t.completedAt ?: return@count false
                val due  = t.dueDate ?: return@count true
                done <= due
            }
            (onTime * 100) / completedInMonth.size
        }

        val totalDowntimeHours = (completedInMonth.sumOf { it.downtimeMinutes } / 60)

        // Reopen totals — count timestamps that landed inside the month
        // across all tasks (not just ones created or completed this month,
        // since a long-running task can reopen many times).
        val reopensInMonth = tasks.sumOf { t ->
            t.reopens.count { it in startMs until endMs }
        }
        val reopenRatePct: Int? = if (completedCount == 0) {
            null
        } else {
            ((reopensInMonth * 100.0) / completedCount).toInt().coerceIn(0, 999)
        }

        return TaskReportSummary(
            created                 = createdCount,
            completed               = completedCount,
            pending                 = pendingCount,
            overdue                 = overdueCount,
            byPriority              = byPriority,
            byDepartment            = byDepartment,
            topRcaCauses            = topRca,
            avgResolutionMinutes    = avgResolutionMinutes,
            slaCompliancePct        = slaCompliancePct,
            totalDowntimeHours      = totalDowntimeHours,
            reopensInMonth          = reopensInMonth,
            reopenRatePct           = reopenRatePct,
        )
    }

    /* ─── Stock section ─────────────────────────────────────────────────── */

    private fun buildStockSummary(
        tasks          : List<TaskRecord>,
        spares         : List<SpareItem>,
        inventoryTxns  : List<InventoryTransaction>,
        openingSnapshot: MonthlyStockSnapshot?,
        startMs        : Long,
        endMs          : Long,
    ): StockReportSummary {
        // Consumption is derived from tasks completed within the month —
        // each task's `materialsUsed` lists what was consumed at signoff.
        val priceById   : Map<String, Double> = spares.associate { it.id to it.price }
        val nameOverride: Map<String, String> = spares.associate { it.id to it.name }

        val consumed = tasks
            .filter { (it.completedAt ?: 0L) in startMs until endMs }
            .flatMap { it.materialsUsed }

        val perItem: Map<String, TopConsumedRow> = consumed
            .groupBy { it.itemId }
            .mapValues { (id, list) ->
                val qty = list.sumOf { it.quantity }
                val unitPrice = priceById[id] ?: 0.0
                TopConsumedRow(
                    itemId   = id,
                    itemName = nameOverride[id] ?: list.first().itemName.ifBlank { id },
                    qty      = qty,
                    value    = qty * unitPrice,
                )
            }

        val itemsConsumedQty   = perItem.values.sumOf { it.qty }
        val itemsConsumedValue = perItem.values.sumOf { it.value }
        val topConsumed = perItem.values
            .sortedByDescending { it.value.takeIf { v -> v > 0 } ?: it.qty.toDouble() }
            .take(5)
            .toList()

        // ── Phase 2 / Case 2 — split via inventory_transactions ─────────
        // Each "issue" transaction in the month is bucketed as "from task"
        // (taskId present) or "manual" (taskId null). Manual deductions
        // surface inventory leakage that task signoffs don't capture.
        val taskById: Map<String, TaskRecord> = tasks.associateBy { it.id }
        val issuedInMonth = inventoryTxns.filter { txn ->
            val ts = txn.createdAt ?: return@filter false
            ts in startMs until endMs && txn.type.equals("issue", ignoreCase = true)
        }

        var fromTasksQty = 0
        var fromTasksValue = 0.0
        var manualQty = 0
        var manualValue = 0.0
        val byEquipment = mutableMapOf<String, Int>()
        for (txn in issuedInMonth) {
            val unitPrice = priceById[txn.itemId] ?: 0.0
            val txnValue  = txn.quantity * unitPrice
            if (!txn.taskId.isNullOrBlank()) {
                fromTasksQty   += txn.quantity
                fromTasksValue += txnValue
                val equip = taskById[txn.taskId]?.equipmentName
                    ?.takeIf { it.isNotBlank() } ?: "Unattributed"
                byEquipment[equip] = (byEquipment[equip] ?: 0) + txn.quantity
            } else {
                manualQty   += txn.quantity
                manualValue += txnValue
            }
        }
        val consumptionByEquipment = byEquipment
            .toList()
            .sortedByDescending { it.second }
            .take(5)

        // Closing stock = current (live).
        val closingStockQty   = spares.sumOf { it.stockQty }
        val closingStockValue = spares.sumOf { it.stockQty * it.price }
        val lowStockCount     = spares.count { it.stockQty in 1..19 }
        val zeroStockCount    = spares.count { it.stockQty <= 0 }

        // Opening totals come from the previous month's snapshot doc (written
        // by the snapshotMonthlyStock Cloud Function on the 1st at 00:05 IST).
        // When the snapshot is missing (first month of an admin's life) the
        // four delta fields stay null and the UI hides the panel.
        val openingQty   = openingSnapshot?.totalQty
        val openingValue = openingSnapshot?.totalValue
        val qtyDelta     = openingQty?.let { closingStockQty - it }
        val valueDelta   = openingValue?.let { closingStockValue - it }

        return StockReportSummary(
            closingStockQty        = closingStockQty,
            closingStockValue      = closingStockValue,
            itemsConsumedQty       = itemsConsumedQty,
            itemsConsumedValue     = itemsConsumedValue,
            topConsumed            = topConsumed,
            lowStockCount          = lowStockCount,
            zeroStockCount         = zeroStockCount,
            consumedFromTasksQty   = fromTasksQty,
            consumedFromTasksValue = fromTasksValue,
            consumedManualQty      = manualQty,
            consumedManualValue    = manualValue,
            consumptionByEquipment = consumptionByEquipment,
            openingStockQty        = openingQty,
            openingStockValue      = openingValue,
            qtyDelta               = qtyDelta,
            valueDelta             = valueDelta,
        )
    }

    /** Normalise TaskRecord.priority (Firestore stores lowercased) to label. */
    private fun normalizePriorityLabel(raw: String): String = when (raw.lowercase()) {
        "danger", "high", "critical" -> "High"
        "warning", "medium", "med"   -> "Medium"
        "success", "low"             -> "Low"
        else                         -> raw.replaceFirstChar { it.uppercase() }
    }
}
