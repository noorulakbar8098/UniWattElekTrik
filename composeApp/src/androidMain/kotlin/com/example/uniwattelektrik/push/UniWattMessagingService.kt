package com.example.uniwattelektrik.push

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
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
 * Token persistence: forwards rotated tokens to [PushTokenRegistrar] which
 * writes them under `admins/{uid}.fcmTokens` or `users/{uid}.fcmTokens`
 * depending on the role of the currently signed-in user.
 *
 * Display: builds a heads-up notification on the `uw_default` channel with a
 * deep-link `PendingIntent` carrying the FCM `data` map as Activity extras.
 */
class UniWattMessagingService : FirebaseMessagingService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNewToken(token: String) {
        scope.launch { PushTokenRegistrar.onTokenRefreshed(token) }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val title = message.notification?.title ?: message.data["title"] ?: "UniWatt"
        val body  = message.notification?.body  ?: message.data["body"]  ?: ""
        showNotification(title, body, message.data)
    }

    private fun showNotification(title: String, body: String, data: Map<String, String>) {
        val nm = getSystemService(NotificationManager::class.java) ?: return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
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

        val launch = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            data.forEach { (k, v) -> putExtra(k, v) }
        }
        val pi = PendingIntent.getActivity(
            this,
            (System.currentTimeMillis() and 0x7fffffff).toInt(),
            launch,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notif = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_notify)
            .setColor(getColor(R.color.uw_notification_accent))
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setAutoCancel(true)
            .setContentIntent(pi)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        nm.notify((System.currentTimeMillis() and 0x7fffffff).toInt(), notif)
    }

    companion object {
        const val CHANNEL_ID = "uw_default"
    }
}

