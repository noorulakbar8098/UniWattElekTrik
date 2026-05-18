package com.example.uniwattelektrik.core.notification

/**
 * Schedules **local** notifications that fire at a wall-clock time even when
 * the app is killed / device is in Doze.
 *
 * Backed by `AlarmManager.setExactAndAllowWhileIdle` on Android (survives
 * Doze, but NOT reboot — see [BootCompletedReceiver] for the rehydration).
 *
 * Use cases:
 *  - "Task X starts in 15 min"          → schedule(id, taskStart - 15min, …)
 *  - "Daily 9 AM check-in reminder"     → repeating via re-schedule on fire
 *  - "Leave starts tomorrow"            → schedule(id, leaveStart, …)
 *
 * NOTE: This is NOT a substitute for FCM push. Remote events (a coworker
 * assigning you a task, an admin approving a leave) MUST flow through FCM
 * because the server is the only thing that can wake a killed app for an
 * arbitrary, unscheduled event. ReminderScheduler is only for events whose
 * trigger time you know in advance.
 */
expect class ReminderScheduler() {
    /**
     * Schedule a one-shot local notification.
     *
     * Calling this with an `id` that is already scheduled REPLACES the prior
     * alarm — handy for editing a task's due date.
     *
     * @param id        stable identifier (e.g. `"task:$taskId"`)
     * @param triggerAtMillis  epoch-millis at which the notification fires
     * @param title     tray title
     * @param body      tray body
     * @param routeKey  DeepLinkBus route key — "tasks", "leave", etc.
     * @param taskId    optional Firestore task id for deep-linking detail
     */
    fun schedule(
        id: String,
        triggerAtMillis: Long,
        title: String,
        body: String,
        routeKey: String? = null,
        taskId: String? = null,
    )

    /** Cancel a pending alarm. No-op if `id` isn't scheduled. */
    fun cancel(id: String)

    /** Cancel every reminder owned by this app. */
    fun cancelAll()
}

