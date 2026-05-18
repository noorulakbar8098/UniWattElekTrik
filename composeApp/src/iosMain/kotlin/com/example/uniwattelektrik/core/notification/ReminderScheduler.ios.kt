package com.example.uniwattelektrik.core.notification

/**
 * iOS stub. A real implementation would use `UNUserNotificationCenter` with
 * a `UNCalendarNotificationTrigger` to fire even when the app is suspended.
 * Tracked for a future iteration — Android coverage is the priority.
 */
actual class ReminderScheduler actual constructor() {
    actual fun schedule(
        id: String,
        triggerAtMillis: Long,
        title: String,
        body: String,
        routeKey: String?,
        taskId: String?,
    ) { /* no-op */ }

    actual fun cancel(id: String) { /* no-op */ }
    actual fun cancelAll() { /* no-op */ }
}

