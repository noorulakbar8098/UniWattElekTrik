package com.example.uniwattelektrik.feature.user.presentation.screens

import com.example.uniwattelektrik.core.performance.TrackScreenPerformance

import com.example.uniwattelektrik.core.theme.appScreenBackground

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.uniwattelektrik.core.components.AppCard
import com.example.uniwattelektrik.core.components.EmptyState
import com.example.uniwattelektrik.core.theme.AppTheme
import com.example.uniwattelektrik.feature.workforce.data.remote.NotificationRecord
import com.example.uniwattelektrik.feature.workforce.presentation.WorkforceViewModel
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

@Composable
fun NotificationsScreen(
    onBack: () -> Unit,
    workforceVm: WorkforceViewModel? = null,
    userId: String = "",
    adminId: String = "",
    modifier: Modifier = Modifier,
) {
    TrackScreenPerformance("NotificationsScreen")
    com.example.uniwattelektrik.core.theme.SetStatusBar(
        color = AppTheme.Bg, darkIcons = true,
    )

    // Live notifications from Firestore (falls back to empty list if no VM)
    val notifications by workforceVm?.notifications?.collectAsStateWithLifecycle()
        ?: run {
            val flow = kotlinx.coroutines.flow.MutableStateFlow(emptyList<NotificationRecord>())
            flow.collectAsStateWithLifecycle()
        }

    val unreadCount = notifications.count { !it.isRead }

    Column(modifier = modifier.fillMaxSize().background(appScreenBackground())) {

        // ── Header ─────────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            com.example.uniwattelektrik.core.components.GlassBackButton(
                onClick = onBack,
                bg      = AppTheme.Surface,
                border  = AppTheme.Ink100,
                tint    = AppTheme.Ink900,
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Notifications",
                    style = com.example.uniwattelektrik.core.theme.AppTypography.HeaderTitle
                        .copy(color = AppTheme.Ink900),
                )
                if (unreadCount > 0) {
                    Text(
                        "$unreadCount unread",
                        color     = AppTheme.Brand,
                        fontSize  = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
            if (unreadCount > 0) {
                Text(
                    "Mark all read",
                    color      = AppTheme.Brand,
                    fontSize   = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier   = Modifier.clickable { workforceVm?.markAllNotificationsRead() },
                )
            }
        }

        // ── List ───────────────────────────────────────────────────────────
        if (notifications.isEmpty()) {
            EmptyState(emoji = "🔔", title = "All caught up", body = "You'll see new alerts here.")
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 100.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(notifications, key = { it.id }) { n ->
                    LiveNotificationRow(
                        n       = n,
                        onClick = { workforceVm?.markNotificationRead(n.id) },
                    )
                }
            }
        }
    }
}

// ── Notification type helpers ──────────────────────────────────────────────

private fun notifEmoji(type: String): String = when (type) {
    "task_assigned", "task_updated" -> "📋"
    "leave_approved"                -> "✅"
    "leave_rejected"                -> "❌"
    "leave_request"                 -> "🏖"
    "check_in"                      -> "📍"
    "alert"                         -> "⚠️"
    else                            -> "🔔"
}

private fun notifTint(type: String) = when (type) {
    "task_assigned", "task_updated" -> AppTheme.Brand
    "leave_approved"                -> AppTheme.Success
    "leave_rejected"                -> AppTheme.Danger
    "leave_request"                 -> AppTheme.Warning
    "alert"                         -> AppTheme.Danger
    else                            -> AppTheme.Brand
}

private fun relativeTime(createdAtMs: Long?): String {
    if (createdAtMs == null) return ""
    val nowMs = com.example.uniwattelektrik.platform.nowEpochMillis()
    val diff  = nowMs - createdAtMs
    return when {
        diff < 60_000L            -> "Just now"
        diff < 3_600_000L         -> "${diff / 60_000} min ago"
        diff < 86_400_000L        -> "${diff / 3_600_000} h ago"
        diff < 2 * 86_400_000L    -> "Yesterday"
        else -> {
            val local = Instant.fromEpochMilliseconds(createdAtMs)
                .toLocalDateTime(TimeZone.currentSystemDefault())
            "${local.dayOfMonth} ${local.month.name.lowercase().replaceFirstChar(Char::uppercaseChar)}"
        }
    }
}

@Composable
private fun LiveNotificationRow(n: NotificationRecord, onClick: () -> Unit) {
    val tint  = notifTint(n.type)
    val emoji = notifEmoji(n.type)
    AppCard(
        modifier = Modifier.clickable(onClick = onClick),
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(tint.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) { Text(emoji, fontSize = 18.sp) }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        n.title,
                        color      = AppTheme.Ink900,
                        fontSize   = 14.sp,
                        fontWeight = if (n.isRead) FontWeight.Normal else FontWeight.SemiBold,
                        modifier   = Modifier.weight(1f),
                    )
                    if (!n.isRead) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(AppTheme.Brand),
                        )
                    }
                }
                Text(n.body,  color = AppTheme.Ink500, fontSize = 12.sp, lineHeight = 17.sp)
                Text(
                    relativeTime(n.createdAt),
                    color    = AppTheme.Ink300,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }
}
