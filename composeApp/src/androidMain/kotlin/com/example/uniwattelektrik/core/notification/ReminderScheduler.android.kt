package com.example.uniwattelektrik.core.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.uniwattelektrik.AndroidAppContext
import com.example.uniwattelektrik.core.AppLog

/**
 * Android implementation backed by [AlarmManager.setExactAndAllowWhileIdle].
 *
 * Why exact alarms:
 *  - `setExact` is killed by Doze (silently postponed up to ~9 minutes).
 *  - `setExactAndAllowWhileIdle` fires even in Doze, with a per-app rate
 *    limit (one alarm every ~9 min on idle devices — fine for reminders).
 *
 * Persistence: every scheduled reminder is written to [ReminderStore] so
 * [BootCompletedReceiver] can rehydrate them on `BOOT_COMPLETED`. AlarmManager
 * forgets all alarms across reboot — without this, every restart would
 * silently drop the user's reminders.
 *
 * On Android 12+ (S, API 31+) exact alarms require the
 * `SCHEDULE_EXACT_ALARM` permission. We declare it in the manifest; on
 * API 33+ the user can revoke it from Settings → Apps → Special access →
 * Alarms & reminders. If revoked we degrade to `setAndAllowWhileIdle`
 * (inexact, still wakes from Doze, may slip by a few minutes).
 */
actual class ReminderScheduler actual constructor() {

    private val context: Context get() = AndroidAppContext.application

    actual fun schedule(
        id: String,
        triggerAtMillis: Long,
        title: String,
        body: String,
        routeKey: String?,
        taskId: String?,
    ) {
        if (triggerAtMillis <= System.currentTimeMillis()) {
            // The trigger is already in the past — fire immediately via
            // LocalNotifier instead of scheduling a no-op alarm.
            LocalNotifier().notify(id.hashCode() and 0x7fffffff, title, body, routeKey, taskId)
            return
        }
        val am = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

        val pi = buildPendingIntent(id, title, body, routeKey, taskId)

        val canExact = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            am.canScheduleExactAlarms()
        } else true

        runCatching {
            if (canExact) {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pi)
            } else {
                // Permission revoked or unsupported — fall back to inexact.
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pi)
            }
        }.onFailure { AppLog.w(TAG, "schedule($id) failed", it) }

        ReminderStore.put(
            context,
            ReminderStore.Entry(id, triggerAtMillis, title, body, routeKey, taskId),
        )
    }

    actual fun cancel(id: String) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        // Build a PendingIntent with FLAG_NO_CREATE to find an existing one;
        // alarm cancellation requires an equivalent PendingIntent.
        val pi = PendingIntent.getBroadcast(
            context,
            id.hashCode(),
            Intent(context, ReminderAlarmReceiver::class.java).setAction(id),
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE,
        )
        if (pi != null) am.cancel(pi)
        ReminderStore.remove(context, id)
    }

    actual fun cancelAll() {
        ReminderStore.all(context).forEach { cancel(it.id) }
    }

    private fun buildPendingIntent(
        id: String,
        title: String,
        body: String,
        routeKey: String?,
        taskId: String?,
    ): PendingIntent {
        val intent = Intent(context, ReminderAlarmReceiver::class.java).apply {
            // Unique action per reminder id → AlarmManager treats two intents
            // with different actions as different alarms even if extras differ.
            action = id
            putExtra(EXTRA_ID, id)
            putExtra(EXTRA_TITLE, title)
            putExtra(EXTRA_BODY, body)
            if (!routeKey.isNullOrBlank()) putExtra(EXTRA_ROUTE, routeKey)
            if (!taskId.isNullOrBlank())   putExtra(EXTRA_TASK_ID, taskId)
        }
        return PendingIntent.getBroadcast(
            context,
            id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    internal companion object {
        const val TAG          = "ReminderScheduler"
        const val EXTRA_ID     = "uw.reminder.id"
        const val EXTRA_TITLE  = "uw.reminder.title"
        const val EXTRA_BODY   = "uw.reminder.body"
        const val EXTRA_ROUTE  = "uw.reminder.route"
        const val EXTRA_TASK_ID = "uw.reminder.taskId"
    }
}

