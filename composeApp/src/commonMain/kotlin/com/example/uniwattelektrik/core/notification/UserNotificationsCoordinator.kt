package com.example.uniwattelektrik.core.notification

import com.example.uniwattelektrik.feature.workforce.data.remote.WorkforceDirectory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

/**
 * Subscribes to user-facing Firestore flows and posts local notifications.
 *
 * Triggers:
 *  - New task assigned (a task id appears in `observeTasksForUser` that
 *    wasn't there at baseline) → "New task assigned".
 *  - Leave request transitions away from `pending` (approved / rejected) →
 *    "Leave approved" or "Leave rejected".
 */
class UserNotificationsCoordinator(
    private val directory: WorkforceDirectory,
    private val notifier: LocalNotifier,
) {
    private var job: Job? = null

    fun start(scope: CoroutineScope, userId: String) {
        if (job != null || userId.isBlank()) return
        val parent = Job(scope.coroutineContext[Job])
        val childScope = CoroutineScope(scope.coroutineContext + parent)
        job = parent

        // ── New tasks ───────────────────────────────────────────────────────
        run {
            val seenTasks = mutableSetOf<String>()
            var primed = false
            directory.observeTasksForUser(userId)
                .onEach { tasks ->
                    val ids = tasks.map { it.id }.toSet()
                    if (!primed) {
                        seenTasks.addAll(ids)
                        primed = true
                        return@onEach
                    }
                    val newIds = ids - seenTasks
                    seenTasks.clear(); seenTasks.addAll(ids)
                    newIds.forEach { id ->
                        val t = tasks.firstOrNull { it.id == id } ?: return@forEach
                        // Respect the per-task admin preference set when the
                        // task was created — silent tasks stay silent.
                        if (!t.notifyAssignee) return@forEach
                        val from = t.ownerAdminName.ifBlank { "Admin" }
                        notifier.notify(
                            id       = stableId("task_assigned", t.id),
                            title    = "New task assigned",
                            body     = "$from assigned: ${t.title}",
                            routeKey = "tasks",
                            taskId   = t.id,
                        )
                    }
                }
                .catch { /* swallow per-source errors so one bad flow can't crash the app */ }
                .launchIn(childScope)
        }

        // ── Leave status transitions ────────────────────────────────────────
        run {
            val prevStatus = mutableMapOf<String, String>()
            var primed = false
            directory.observeLeaveRequestsForUser(userId)
                .onEach { leaves ->
                    if (!primed) {
                        leaves.forEach { prevStatus[it.id] = it.status }
                        primed = true
                        return@onEach
                    }
                    leaves.forEach { l ->
                        val before = prevStatus[l.id]
                        prevStatus[l.id] = l.status
                        if (before != null &&
                            before.equals("pending", ignoreCase = true) &&
                            !l.status.equals("pending", ignoreCase = true)
                        ) {
                            val (title, body) = when (l.status.lowercase()) {
                                "approved" -> "Leave approved" to
                                        "Your ${l.totalDays}-day ${l.leaveType} leave was approved"
                                "rejected" -> "Leave rejected" to
                                        (l.rejectionReason.takeIf { it.isNotBlank() }
                                            ?: "Your ${l.leaveType} leave was rejected")
                                else -> "Leave updated" to "Status: ${l.status}"
                            }
                            notifier.notify(
                                id = stableId("leave_status", l.id),
                                title = title,
                                body = body,
                                routeKey = "leave",
                            )
                        }
                    }
                }
                .catch { /* swallow per-source errors so one bad flow can't crash the app */ }
                .launchIn(childScope)
        }

        // ── Task notes (chat) ───────────────────────────────────────────────
        // For every task assigned to this user, listen to /tasks/{id}/notes
        // and notify when the admin posts. Per-task listeners are added /
        // cancelled as the assigned-tasks set changes.
        run {
            val noteJobs = mutableMapOf<String, Job>()
            directory.observeTasksForUser(userId)
                .onEach { tasks ->
                    val ids = tasks.map { it.id }.toSet()
                    val gone = noteJobs.keys - ids
                    gone.forEach { id -> noteJobs.remove(id)?.cancel() }
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
                                    // Only admin notes notify the user (skip echoes).
                                    if (!n.role.equals("admin", ignoreCase = true)) return@forEach
                                    val who = n.authorName.ifBlank { t.ownerAdminName.ifBlank { "Admin" } }
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
