package com.example.uniwattelektrik.push

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.uniwattelektrik.MainActivity
import com.example.uniwattelektrik.R
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Receives FCM lifecycle callbacks and incoming pushes.
 *
 * KILLED-STATE BEHAVIOR
 * ─────────────────────
 * Android FCM works in three app states:
 *
 *   • Foreground             → `onMessageReceived` fires for every payload.
 *   • Background (alive)     → if the FCM payload contains a `notification`
 *                              block, the system renders the tray notif and
 *                              `onMessageReceived` is NOT called. If the
 *                              payload is **data-only**, the system wakes
 *                              this service and `onMessageReceived` fires.
 *   • Killed / Force-stopped → identical to "Background" above, **provided
 *                              the user has launched the app at least once
 *                              since install** (Android's "stopped state"
 *                              rule). On force-stop, no delivery happens
 *                              until the user opens the app again.
 *
 * Our Cloud Functions send **data-only** messages (no `notification` block),
 * so this `onMessageReceived` is the single point where the tray
 * notification is built — in every state. That guarantees the tap-deep-link
 * always carries the `type` + `taskId` extras (system-rendered tray notifs
 * drop those on some OEM skins).
 */
class UniWattMessagingService : FirebaseMessagingService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNewToken(token: String) {
        scope.launch { PushTokenRegistrar.onTokenRefreshed(token) }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        // Support both data-only and mixed (notification + data) payloads.
        val data  = message.data
        val title = data["title"] ?: message.notification?.title ?: "UniWatt"
        val body  = data["body"]  ?: message.notification?.body  ?: ""
        if (title.isBlank() && body.isBlank()) return
        showNotification(title, body, data)
    }

    private fun showNotification(title: String, body: String, data: Map<String, String>) {
        ensureChannels()

        // Per-category channel → users can mute "Low stock" while keeping
        // "Task assigned" alerts. The single uw_default channel can't.
        val channelId = channelIdForType(data["type"])

        val launch = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            data.forEach { (k, v) -> putExtra(k, v) }
        }

        // Stable notification id per entity → repeated updates to the same
        // task / leave collapse into one tray row instead of spamming.
        val entityKey = data["taskId"]
            ?: data["leaveId"]
            ?: data["noteId"]
            ?: data["itemId"]
            ?: title   // fallback: title acts as the dedup key
        val notifId = ((data["type"].orEmpty() + ":" + entityKey).hashCode()) and 0x7fffffff

        // PendingIntent request code MUST be unique-per-extras-set, otherwise
        // FLAG_UPDATE_CURRENT silently reuses the previous extras (you'd open
        // a stale task). Reusing notifId guarantees uniqueness per entity.
        val pi = PendingIntent.getActivity(
            this,
            notifId,
            launch,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notif = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_stat_notify)
            .setColor(getColor(R.color.uw_notification_accent))
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setAutoCancel(true)
            .setContentIntent(pi)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            // Sound + vibration on Android < 8 (Oreo uses channel IMPORTANCE).
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .build()

        runCatching { NotificationManagerCompat.from(this).notify(notifId, notif) }
    }

    /** Map an FCM data `type` to one of our per-category channels. */
    private fun channelIdForType(type: String?): String = when (type?.lowercase()) {
        "task", "tasks"          -> CHANNEL_TASKS
        "leave", "approval"      -> CHANNEL_LEAVE
        "inventory", "low_stock" -> CHANNEL_INVENTORY
        "attendance"             -> CHANNEL_ATTENDANCE
        else                     -> CHANNEL_DEFAULT
    }

    /** Idempotently create every channel we ship. */
    private fun ensureChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = getSystemService(NotificationManager::class.java) ?: return

        fun ch(id: String, name: String, desc: String) {
            if (nm.getNotificationChannel(id) != null) return
            val c = NotificationChannel(id, name, NotificationManager.IMPORTANCE_HIGH).apply {
                description = desc
                enableLights(true)
                enableVibration(true)
                setShowBadge(true)
            }
            nm.createNotificationChannel(c)
        }
        ch(CHANNEL_DEFAULT,    "General",    "General updates")
        ch(CHANNEL_TASKS,      "Tasks",      "Assignments, status & notes")
        ch(CHANNEL_LEAVE,      "Leave",      "Leave requests & approvals")
        ch(CHANNEL_INVENTORY,  "Inventory",  "Low-stock alerts")
        ch(CHANNEL_ATTENDANCE, "Attendance", "Late check-ins")
    }

    companion object {
        // Kept for backwards compat with manifest meta-data + LocalNotifier.
        const val CHANNEL_ID         = "uw_default"
        const val CHANNEL_DEFAULT    = "uw_default"
        const val CHANNEL_TASKS      = "uw_tasks"
        const val CHANNEL_LEAVE      = "uw_leave"
        const val CHANNEL_INVENTORY  = "uw_inventory"
        const val CHANNEL_ATTENDANCE = "uw_attendance"
    }
}

