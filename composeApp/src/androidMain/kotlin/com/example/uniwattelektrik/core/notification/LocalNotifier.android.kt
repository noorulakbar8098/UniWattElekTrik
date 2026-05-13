package com.example.uniwattelektrik.core.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.uniwattelektrik.AndroidAppContext
import com.example.uniwattelektrik.MainActivity
import com.example.uniwattelektrik.R

/**
 * Android implementation of [LocalNotifier]. Reuses the same channel id /
 * icon / accent color as the FCM path so users see a consistent style
 * whether the notification originates from a Firestore listener or from a
 * future server push.
 */
actual class LocalNotifier actual constructor() {

    private val context: Context get() = AndroidAppContext.application

    init {
        ensureChannel()
    }

    actual fun notify(id: Int, title: String, body: String, routeKey: String?, taskId: String?) {
        val launch = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            if (!routeKey.isNullOrBlank()) putExtra("type", routeKey)
            // Optional task id — MainActivity reads it and forwards through
            // DeepLinkBus.taskId so the shell can pop the detail screen.
            if (!taskId.isNullOrBlank()) putExtra("taskId", taskId)
        }
        val pi = PendingIntent.getActivity(
            context,
            id,
            launch,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notif = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_notify)
            .setColor(context.getColor(R.color.uw_notification_accent))
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setAutoCancel(true)
            .setContentIntent(pi)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        // POST_NOTIFICATIONS permission (Android 13+) is requested at app start
        // by MainActivity. If it's still denied, NotificationManagerCompat
        // silently drops the post — no crash.
        runCatching { NotificationManagerCompat.from(context).notify(id, notif) }
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = context.getSystemService(NotificationManager::class.java) ?: return
        if (nm.getNotificationChannel(CHANNEL_ID) != null) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            "General",
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = "Tasks, leave & inventory updates"
            enableLights(true)
            enableVibration(true)
        }
        nm.createNotificationChannel(channel)
    }

    private companion object {
        const val CHANNEL_ID = "uw_default"
    }
}

