package com.example.uniwattelektrik.core.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.uniwattelektrik.AndroidAppContext
import com.example.uniwattelektrik.core.AppLog

/**
 * Receives the AlarmManager broadcast and posts the tray notification.
 *
 * Lifetime: BroadcastReceivers get ~10 seconds of execution before Android
 * kills them. We do nothing async here — [LocalNotifier.notify] is a sync
 * call into NotificationManagerCompat which completes in milliseconds.
 *
 * Cold-process safety: when the user has swiped the app away, this receiver
 * is the entry point — `MainActivity` never runs, so `AndroidAppContext`
 * stays `lateinit`-uninitialised and `LocalNotifier` would crash. We seed
 * it from [Context] before touching anything that depends on it. Same trick
 * as [BootCompletedReceiver].
 *
 * After firing, we also drop the entry from [ReminderStore] so it won't
 * be re-scheduled on the next reboot. Repeating reminders are implemented
 * by re-scheduling explicitly from the trigger handler — there's no
 * AlarmManager native "repeat" mode that survives Doze.
 */
class ReminderAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // Seed the app-wide context holder first — we may be running in a
        // brand-new process spawned by Android just to deliver this broadcast.
        AndroidAppContext.init(context.applicationContext)

        val id    = intent.getStringExtra(ReminderScheduler.EXTRA_ID)     ?: return
        val title = intent.getStringExtra(ReminderScheduler.EXTRA_TITLE)  ?: "UniWatt"
        val body  = intent.getStringExtra(ReminderScheduler.EXTRA_BODY)   ?: ""
        val route = intent.getStringExtra(ReminderScheduler.EXTRA_ROUTE)
        val task  = intent.getStringExtra(ReminderScheduler.EXTRA_TASK_ID)

        // Build a stable notif id from the reminder id so re-fires of the
        // same reminder (e.g. a daily one) collapse into a single tray row.
        val notifId = id.hashCode() and 0x7fffffff

        AppLog.i("ReminderAlarmRx", "onReceive id=$id notifId=$notifId title=$title")

        runCatching {
            LocalNotifier().notify(
                id       = notifId,
                title    = title,
                body     = body,
                routeKey = route,
                taskId   = task,
            )
        }.onFailure { AppLog.w("ReminderAlarmRx", "notify($id) failed", it) }

        // One-shot reminder — drop the persisted entry. (For repeating
        // reminders, callers should re-schedule the next occurrence right
        // before/after invoking ReminderScheduler.schedule().)
        ReminderStore.remove(context, id)
    }
}

