package com.example.uniwattelektrik.core.notification

import com.example.uniwattelektrik.feature.workforce.data.remote.WorkforceDirectory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

/**
 * Subscribes to admin-facing Firestore flows and posts a local notification
 * when something noteworthy happens — without any server / Cloud Functions.
 *
 * Strategy: each flow's first emission is treated as the "baseline" — every
 * existing record is added to a seen-id set / prev-state map but **no
 * notification is posted**. From the second emission onward, only new or
 * transitioned records produce notifications. This avoids a notification
 * storm on app start / process restart.
 *
 * Triggers:
 *  - Task transitions to `status == "Done"` → "Task completed".
 *  - Leave request created with `status == "pending"` → "Leave request".
 *  - Attendance record with `checkInStatus == "LATE"` → "Late check-in".
 *  - Spare item `stockQty` crosses below 20 → "Low stock".
 *
 * Idempotent — calling [start] twice without [stop] is a no-op.
 */
class AdminNotificationsCoordinator(
    private val directory: WorkforceDirectory,
    private val notifier: LocalNotifier,
) {
    private var job: Job? = null

    /** Re-notify when stock dips back below this threshold from above. */
    private val lowStockThreshold = 20

    fun start(scope: CoroutineScope, adminId: String) {
        if (job != null || adminId.isBlank()) return
        val parent = Job(scope.coroutineContext[Job])
        val childScope = CoroutineScope(scope.coroutineContext + parent)
        job = parent

        // ── Tasks: notify when one transitions into "Done" ──────────────────
        run {
            val seenDone = mutableSetOf<String>()
            var primed = false
            directory.observeTasksForAdmin(adminId)
                .onEach { tasks ->
                    val doneIds = tasks.asSequence()
                        .filter { it.status.equals("Done", ignoreCase = true) }
                        .map { it.id }
                        .toSet()
                    if (!primed) {
                        seenDone.addAll(doneIds)
                        primed = true
                        return@onEach
                    }
                    val newlyDone = doneIds - seenDone
                    seenDone.clear(); seenDone.addAll(doneIds)
                    newlyDone.forEach { id ->
                        val t = tasks.firstOrNull { it.id == id } ?: return@forEach
                        val who = t.assigneeName.ifBlank { "Employee" }
                        notifier.notify(
                            id = stableId("task_done", t.id),
                            title = "Task completed",
                            body = "$who marked \"${t.title}\" as done",
                            routeKey = "tasks",
                        )
                    }
                }
                .catch { /* swallow per-source errors so one bad flow can't crash the app */ }
                .launchIn(childScope)
        }

        // ── Leave requests: notify on each new pending submission ───────────
        run {
            val seenPending = mutableSetOf<String>()
            var primed = false
            directory.observeLeaveRequestsForAdmin(adminId)
                .onEach { leaves ->
                    val pendingIds = leaves.asSequence()
                        .filter { it.status.equals("pending", ignoreCase = true) }
                        .map { it.id }
                        .toSet()
                    if (!primed) {
                        seenPending.addAll(pendingIds)
                        primed = true
                        return@onEach
                    }
                    val newPending = pendingIds - seenPending
                    seenPending.clear(); seenPending.addAll(pendingIds)
                    newPending.forEach { id ->
                        val l = leaves.firstOrNull { it.id == id } ?: return@forEach
                        notifier.notify(
                            id = stableId("leave_pending", l.id),
                            title = "New leave request",
                            body = "${l.employeeName} requested ${l.totalDays}-day ${l.leaveType} leave",
                            routeKey = "approvals",
                        )
                    }
                }
                .catch { /* swallow per-source errors so one bad flow can't crash the app */ }
                .launchIn(childScope)
        }

        // ── Attendance: notify on each new LATE check-in ────────────────────
        run {
            val seenLate = mutableSetOf<String>()
            var primed = false
            directory.observeAttendance(adminId)
                .onEach { logs ->
                    val lateIds = logs.asSequence()
                        .filter { it.checkInStatus.equals("LATE", ignoreCase = true) }
                        .map { it.id }
                        .toSet()
                    if (!primed) {
                        seenLate.addAll(lateIds)
                        primed = true
                        return@onEach
                    }
                    val newLate = lateIds - seenLate
                    seenLate.clear(); seenLate.addAll(lateIds)
                    newLate.forEach { id ->
                        notifier.notify(
                            id = stableId("attendance_late", id),
                            title = "Late check-in",
                            body = "An employee checked in late",
                            routeKey = "attendance",
                        )
                    }
                }
                .catch { /* swallow per-source errors so one bad flow can't crash the app */ }
                .launchIn(childScope)
        }

        // ── Inventory: notify when an item crosses below the threshold ──────
        run {
            val prevQty = mutableMapOf<String, Int>()
            var primed = false
            directory.observeSpareItems(adminId)
                .onEach { items ->
                    if (!primed) {
                        items.forEach { prevQty[it.id] = it.stockQty }
                        primed = true
                        return@onEach
                    }
                    items.forEach { item ->
                        val before = prevQty[item.id]
                        val after = item.stockQty
                        prevQty[item.id] = after
                        // Edge trigger only: was at/above threshold, now below.
                        // Brand new items inserted already-low also fire (before == null && after < threshold).
                        val crossed = (before == null && after < lowStockThreshold) ||
                                (before != null && before >= lowStockThreshold && after < lowStockThreshold)
                        if (crossed) {
                            notifier.notify(
                                id = stableId("low_stock", item.id),
                                title = "Low stock: ${item.name}",
                                body = "Only $after left — restock soon",
                                routeKey = "inventory",
                            )
                        }
                    }
                }
                .catch { /* swallow per-source errors so one bad flow can't crash the app */ }
                .launchIn(childScope)
        }
    }

    fun stop() {
        job?.cancel()
        job = null
    }
}

/** Deterministic 31-bit hash combining a kind tag with a Firestore doc id. */
internal fun stableId(kind: String, id: String): Int {
    var h = kind.hashCode()
    h = 31 * h + id.hashCode()
    return h and 0x7fffffff
}

