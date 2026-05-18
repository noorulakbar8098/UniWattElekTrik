package com.example.uniwattelektrik.core.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.uniwattelektrik.AndroidAppContext

/**
 * Rehydrates every persisted reminder after a device reboot.
 *
 * AlarmManager intentionally clears every alarm across reboot — apps are
 * expected to re-schedule via a BOOT_COMPLETED receiver. We also handle
 * MY_PACKAGE_REPLACED so reminders survive an OTA / Play Store update.
 *
 * BOOT_COMPLETED only fires for apps that have been launched at least once
 * since install (Android's "stopped state" rule). After the first launch,
 * this receiver runs after every reboot, even if the user never opens the
 * app again — that's how scheduled reminders keep working in killed state.
 */
class BootCompletedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action != Intent.ACTION_BOOT_COMPLETED &&
            action != Intent.ACTION_LOCKED_BOOT_COMPLETED &&
            action != Intent.ACTION_MY_PACKAGE_REPLACED) return

        // Make sure AndroidAppContext is initialised even before the
        // launcher Activity runs — the scheduler depends on it.
        AndroidAppContext.init(context.applicationContext)

        val scheduler = ReminderScheduler()
        val now = System.currentTimeMillis()
        ReminderStore.all(context).forEach { e ->
            // Drop already-elapsed reminders (we'd otherwise spam the user
            // with N catch-up notifications minutes after they boot).
            if (e.triggerAtMillis <= now) {
                ReminderStore.remove(context, e.id)
                return@forEach
            }
            scheduler.schedule(
                id              = e.id,
                triggerAtMillis = e.triggerAtMillis,
                title           = e.title,
                body            = e.body,
                routeKey        = e.routeKey,
                taskId          = e.taskId,
            )
        }
    }
}

