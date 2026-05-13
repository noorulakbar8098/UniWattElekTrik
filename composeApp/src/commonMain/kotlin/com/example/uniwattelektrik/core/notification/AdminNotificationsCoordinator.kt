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
 *  - Task transitions out of "Done" (reopen) → "Task reopened".
 *  - Task transitions from no-acceptedAt → has-acceptedAt → "Task accepted".
 *  - A task's checklist `done` count grows → "Checklist updated".
 *  - A task's attachment list grows → "New photo / file added".
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

        // ── Tasks: notify on every meaningful per-task transition ───────────
        // We snapshot four fields per task across emissions and fire a
        // distinct notification when any of them transitions in a way the
        // admin should know about. All five derivations share one Firestore
        // subscription — much cheaper than a flow-per-trigger fan-out.
        run {
            data class TaskSnap(
                val status: String,
                val acceptedAt: Long?,
                val checklistDone: Int,
                val attachmentsCount: Int,
            )
            val prev = mutableMapOf<String, TaskSnap>()
            var primed = false
            directory.observeTasksForAdmin(adminId)
                .onEach { tasks ->
                    val current = tasks.associate { t ->
                        t.id to TaskSnap(
                            status           = t.status,
                            acceptedAt       = t.acceptedAt,
                            checklistDone    = t.checklist.count { it.done },
                            attachmentsCount = t.attachments.size,
                        )
                    }
                    if (!primed) {
                        prev.putAll(current)
                        primed = true
                        return@onEach
                    }
                    tasks.forEach { t ->
                        val before = prev[t.id]
                        val after  = current[t.id] ?: return@forEach
                        prev[t.id] = after
                        // Per-task admin preference: when the admin un-ticked
                        // "Notify" while creating / editing this task we stay
                        // silent across every transition, including completion.
                        if (!t.notifyAssignee) return@forEach
                        val who = t.assigneeName.ifBlank { "Employee" }

                        // 1. Task accepted (acceptedAt was null, now set).
                        if (before != null && before.acceptedAt == null && after.acceptedAt != null) {
                            notifier.notify(
                                id       = stableId("task_accepted", t.id),
                                title    = "Task accepted",
                                body     = "$who accepted \"${t.title}\"",
                                routeKey = "tasks",
                                taskId   = t.id,
                            )
                        }
                        // 2. Task completed.
                        if ((before == null || !before.status.equals("Done", ignoreCase = true))
                            && after.status.equals("Done", ignoreCase = true)) {
                            notifier.notify(
                                id       = stableId("task_done", t.id),
                                title    = "Task completed",
                                body     = "$who marked \"${t.title}\" as done",
                                routeKey = "tasks",
                                taskId   = t.id,
                            )
                        }
                        // 3. Task reopened (was Done, now active again).
                        if (before != null
                            && before.status.equals("Done", ignoreCase = true)
                            && !after.status.equals("Done", ignoreCase = true)) {
                            notifier.notify(
                                id       = stableId("task_reopened", t.id),
                                title    = "Task reopened",
                                body     = "$who reopened \"${t.title}\"",
                                routeKey = "tasks",
                                taskId   = t.id,
                            )
                        }
                        // 4. Checklist progress (a previously-unchecked item got ticked).
                        if (before != null && after.checklistDone > before.checklistDone) {
                            val total = t.checklist.size
                            notifier.notify(
                                id       = stableId("task_checklist", t.id),
                                title    = "Checklist updated",
                                body     = "$who ticked an item on \"${t.title}\" · ${after.checklistDone}/$total",
                                routeKey = "tasks",
                                taskId   = t.id,
                            )
                        }
                        // 5. New attachment added (photo / file / signature).
                        if (before != null && after.attachmentsCount > before.attachmentsCount) {
                            val n = after.attachmentsCount - before.attachmentsCount
                            notifier.notify(
                                id       = stableId("task_attachment", t.id),
                                title    = if (n == 1) "New attachment" else "New attachments",
                                body     = "$who added ${if (n == 1) "a file" else "$n files"} to \"${t.title}\"",
                                routeKey = "tasks",
                                taskId   = t.id,
                            )
                        }
                    }
                    // Drop entries for tasks the admin no longer has so the map
                    // doesn't grow unboundedly across the session.
                    prev.keys.retainAll(current.keys)
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

        // ── Task notes (chat) ───────────────────────────────────────────────
        // Subscribe to /tasks/{id}/notes for every task this admin owns and
        // fire a local notification whenever the assigned **user** posts a
        // note. We diff the active task list per emission so listeners are
        // added for new tasks and cancelled for removed ones.
        run {
            val noteJobs = mutableMapOf<String, Job>()
            directory.observeTasksForAdmin(adminId)
                .onEach { tasks ->
                    val ids = tasks.map { it.id }.toSet()
                    // Cancel listeners for tasks the admin no longer owns.
                    val gone = noteJobs.keys - ids
                    gone.forEach { id -> noteJobs.remove(id)?.cancel() }
                    // Start a listener for any newly-arrived task.
                    tasks.forEach { t ->
                        if (noteJobs.containsKey(t.id)) return@forEach
                        val seen = mutableSetOf<String>()
                        var primedTask = false
                        noteJobs[t.id] = directory.observeTaskNotes(t.id)
                            .onEach { notes ->
                                if (!primedTask) {
                                    notes.forEach { seen.add(it.id) }
                                    primedTask = true
                                    return@onEach
                                }
                                notes.forEach { n ->
                                    if (n.id in seen) return@forEach
                                    seen.add(n.id)
                                    // Only user-authored notes are interesting
                                    // to the admin (skip our own echoes).
                                    if (!n.role.equals("user", ignoreCase = true)) return@forEach
                                    val who = n.authorName.ifBlank { t.assigneeName.ifBlank { "User" } }
                                    val body = if (n.message.length > 140)
                                        n.message.take(137) + "…" else n.message
                                    notifier.notify(
                                        id       = stableId("task_note", n.id),
                                        title    = "$who · ${t.title}",
                                        body     = body,
                                        routeKey = "tasks",
                                        taskId   = t.id,
                                    )
                                }
                            }
                            .catch { /* one bad note stream shouldn't kill the others */ }
                            .launchIn(childScope)
                    }
                }
                .catch { /* swallow */ }
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
