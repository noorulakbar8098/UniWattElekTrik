package com.example.uniwattelektrik.feature.admin.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.BeachAccess
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.PriorityHigh
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.uniwattelektrik.core.components.DsAllCaughtUpEmptyState
import com.example.uniwattelektrik.core.components.DsStatusChip
import com.example.uniwattelektrik.core.components.OperationsHeader
import com.example.uniwattelektrik.core.performance.TrackScreenPerformance
import com.example.uniwattelektrik.core.theme.AppShapes
import com.example.uniwattelektrik.core.theme.AppTheme
import com.example.uniwattelektrik.core.theme.AppTypography
import com.example.uniwattelektrik.core.theme.SetStatusBar
import com.example.uniwattelektrik.core.theme.appScreenBackground
import com.example.uniwattelektrik.feature.admin.presentation.InventoryViewModel
import com.example.uniwattelektrik.feature.admin.presentation.screens.inventory.SpareItem
import com.example.uniwattelektrik.feature.workforce.presentation.WorkforceViewModel
import com.example.uniwattelektrik.platform.nowEpochMillis

/* ─── Alert model ──────────────────────────────────────────────────────── */

private enum class AlertSeverity { Critical, Warning, Info }

/**
 * One actionable alert surfaced on the admin Alerts screen. All values are
 * presentation-ready strings so the screen body never has to format dates.
 */
private data class AlertItem(
    val id          : String,
    val severity    : AlertSeverity,
    val icon        : ImageVector,
    val title       : String,
    val subtitle    : String,
    val timestampMs : Long,
    val timeAgo     : String,
    val onTap       : () -> Unit,
)

/* ═══════════════════════════════════════════════════════════════════════════
 *  AlertsScreen — admin-side aggregation of everything that needs attention
 * ═══════════════════════════════════════════════════════════════════════════
 *
 *  Pulls live Firestore flows already in use elsewhere (tasks, leaves,
 *  attendance, spare-items) and derives alerts client-side. No backend
 *  changes required. Each alert is tappable — the host wires the
 *  navigation callbacks through.
 */
@Composable
fun AlertsScreen(
    workforceVm     : WorkforceViewModel,
    inventoryVm     : InventoryViewModel,
    onBack          : () -> Unit,
    onTaskClick     : (taskId: String) -> Unit = {},
    onLeaveClick    : () -> Unit = {},
    onAttendanceClick: () -> Unit = {},
    onSpareClick    : (SpareItem) -> Unit = {},
    modifier        : Modifier = Modifier,
) {
    TrackScreenPerformance("AlertsScreen")
    SetStatusBar(color = AppTheme.Brand, darkIcons = false)

    val tasks     by workforceVm.tasks.collectAsStateWithLifecycle()
    val leaves    by workforceVm.leaveRequests.collectAsStateWithLifecycle()
    val attendance by workforceVm.attendance.collectAsStateWithLifecycle()
    val spares    by inventoryVm.spareItems.collectAsStateWithLifecycle()

    val now = remember { nowEpochMillis() }

    val alerts = remember(tasks, leaves, attendance, spares, now) {
        buildList {
            // 1. Overdue tasks (dueDate in past, not yet Done)
            tasks.asSequence()
                .filter { rec ->
                    rec.status != "Done"
                        && rec.dueDate != null
                        && rec.dueDate < now
                }
                .forEach { rec ->
                    val overdueMs = now - (rec.dueDate ?: now)
                    add(
                        AlertItem(
                            id          = "overdue:${rec.id}",
                            severity    = AlertSeverity.Critical,
                            icon        = Icons.Filled.WarningAmber,
                            title       = "Overdue task",
                            subtitle    = "${rec.title} · assigned to ${rec.assigneeName.ifBlank { "—" }}",
                            timestampMs = rec.dueDate ?: now,
                            timeAgo     = humanAgo(overdueMs, suffix = "overdue"),
                            onTap       = { onTaskClick(rec.id) },
                        )
                    )
                }

            // 2. Critical-priority unassigned tasks
            tasks.asSequence()
                .filter { it.priority.equals("Danger", ignoreCase = true) && it.userId.isNullOrBlank() && it.status != "Done" }
                .forEach { rec ->
                    val createdMs = rec.createdAtMs ?: now
                    add(
                        AlertItem(
                            id          = "unassigned:${rec.id}",
                            severity    = AlertSeverity.Critical,
                            icon        = Icons.Filled.PriorityHigh,
                            title       = "High-priority task unassigned",
                            subtitle    = rec.title,
                            timestampMs = createdMs,
                            timeAgo     = humanAgo(now - createdMs, suffix = "ago"),
                            onTap       = { onTaskClick(rec.id) },
                        )
                    )
                }

            // 3. Stale pending leave (> 3 days old). createdAtMs is nullable on
            // legacy records — skip those rather than ageing them off "now".
            val threeDaysMs = 3L * 24L * 60L * 60L * 1000L
            leaves.asSequence()
                .filter { it.status.equals("pending", ignoreCase = true) }
                .forEach { lr ->
                    val created = lr.createdAtMs ?: return@forEach
                    if ((now - created) <= threeDaysMs) return@forEach
                    add(
                        AlertItem(
                            id          = "leave:${lr.id}",
                            severity    = AlertSeverity.Warning,
                            icon        = Icons.Filled.BeachAccess,
                            title       = "Leave awaiting approval",
                            subtitle    = "${lr.employeeName} · ${lr.totalDays}-day ${lr.leaveType}",
                            timestampMs = created,
                            timeAgo     = humanAgo(now - created, suffix = "ago"),
                            onTap       = onLeaveClick,
                        )
                    )
                }

            // 4. Late check-ins today
            val todayStart = startOfTodayMs(now)
            attendance.asSequence()
                .filter { it.checkInMs >= todayStart }
                .filter { it.checkInStatus.equals("LATE", ignoreCase = true) }
                .forEach { rec ->
                    add(
                        AlertItem(
                            id          = "late:${rec.id}",
                            severity    = AlertSeverity.Warning,
                            icon        = Icons.Filled.AccessTime,
                            title       = "Late check-in",
                            subtitle    = "Employee checked in after the shift start time today",
                            timestampMs = rec.checkInMs,
                            timeAgo     = humanAgo(now - rec.checkInMs, suffix = "ago"),
                            onTap       = onAttendanceClick,
                        )
                    )
                }

            // 5. Low-stock spares (< 20 units)
            spares.asSequence()
                .filter { it.stockQty < 20 }
                .forEach { item ->
                    add(
                        AlertItem(
                            id          = "stock:${item.id}",
                            severity    = if (item.stockQty < 5) AlertSeverity.Critical else AlertSeverity.Warning,
                            icon        = Icons.Filled.Inventory2,
                            title       = "Low stock: ${item.name}",
                            subtitle    = "Only ${item.stockQty} ${item.unit.ifBlank { "units" }} left — restock soon",
                            timestampMs = item.createdAtMs ?: now,
                            timeAgo     = "Live",
                            onTap       = { onSpareClick(item) },
                        )
                    )
                }
        }
            .sortedWith(
                // Critical first, then by recency (most recent first within each severity).
                compareBy<AlertItem> {
                    when (it.severity) {
                        AlertSeverity.Critical -> 0
                        AlertSeverity.Warning  -> 1
                        AlertSeverity.Info     -> 2
                    }
                }.thenByDescending { it.timestampMs }
            )
    }

    Column(modifier = modifier.fillMaxSize().background(appScreenBackground())) {

        // ── Operations header ────────────────────────────────────────────
        OperationsHeader(
            eyebrow  = "ALERTS",
            title    = "Needs attention",
            subtitle = if (alerts.isEmpty())
                          "Everything looks healthy. Nothing pressing right now."
                      else
                          "${alerts.count { it.severity == AlertSeverity.Critical }} critical · " +
                              "${alerts.count { it.severity == AlertSeverity.Warning }} warning",
            onBack   = onBack,
            actions  = {
                if (alerts.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .clip(AppShapes.pill)
                            .background(Color.White.copy(alpha = 0.10f))
                            .padding(horizontal = 10.dp, vertical = 5.dp),
                    ) {
                        Text(
                            "${alerts.size} active",
                            style = AppTypography.captionLarge.copy(
                                color      = Color.White,
                                fontWeight = FontWeight.SemiBold,
                            ),
                        )
                    }
                }
            },
        )

        // ── Body ─────────────────────────────────────────────────────────
        if (alerts.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(top = AppTheme.SpXl)) {
                DsAllCaughtUpEmptyState(
                    title    = "All clear ✨",
                    body     = "No alerts to action. We'll surface anything that needs your attention here.",
                    pillLabel = "ALL CLEAR",
                )
            }
        } else {
            LazyColumn(
                modifier            = Modifier.fillMaxSize(),
                contentPadding      = PaddingValues(
                    start  = AppTheme.SpLg,
                    end    = AppTheme.SpLg,
                    top    = AppTheme.SpMd,
                    bottom = 100.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(AppTheme.SpMd),
            ) {
                items(alerts, key = { it.id }) { alert ->
                    AlertCard(item = alert)
                }
            }
        }
    }
}

/* ─── Alert card ───────────────────────────────────────────────────────── */

@Composable
private fun AlertCard(item: AlertItem) {
    val (tint, bg, label) = when (item.severity) {
        AlertSeverity.Critical -> Triple(AppTheme.Danger,  AppTheme.DangerBg,  "Critical")
        AlertSeverity.Warning  -> Triple(AppTheme.Warning, AppTheme.WarningBg, "Warning")
        AlertSeverity.Info     -> Triple(AppTheme.Brand,   AppTheme.Brand50,   "Info")
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(6.dp, AppShapes.card, spotColor = tint.copy(alpha = 0.15f))
            .clip(AppShapes.card)
            .background(AppTheme.Surface)
            .clickable(onClick = item.onTap)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Severity icon tile
        Box(
            modifier         = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(bg),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector        = item.icon,
                contentDescription = null,
                tint               = tint,
                modifier           = Modifier.size(22.dp),
            )
        }

        Spacer(Modifier.padding(horizontal = AppTheme.SpSm))

        // Title + subtitle + timestamp
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text     = item.title,
                    style    = AppTypography.titleSmall.copy(
                        color      = AppTheme.Ink900,
                        fontWeight = FontWeight.Bold,
                    ),
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.padding(horizontal = AppTheme.SpXs))
                DsStatusChip(label = label, tint = tint, background = bg)
            }
            Text(
                text  = item.subtitle,
                style = AppTypography.bodySmall.copy(color = AppTheme.Ink500),
            )
            Text(
                text  = item.timeAgo,
                style = AppTypography.captionLarge.copy(color = AppTheme.Ink300),
            )
        }
    }
}

/* ─── Time helpers (no java.time — KMP-clean) ──────────────────────────── */

/** Format a duration in millis as "Xm/h/d ago" or "X overdue". */
private fun humanAgo(durationMs: Long, suffix: String): String {
    if (durationMs < 0) return suffix
    val mins = durationMs / 60_000L
    return when {
        mins < 1   -> "Just now"
        mins < 60  -> "${mins}m $suffix"
        mins < 1440 -> "${mins / 60}h $suffix"
        else        -> "${mins / 1440}d $suffix"
    }
}

/**
 * Start-of-today in epoch millis, computed from the local timezone offset
 * implied by the difference between `nowEpochMillis()` and the start of the
 * UTC day. Good enough for "is this from today?" classification.
 */
private fun startOfTodayMs(nowMs: Long): Long {
    val dayMs = 86_400_000L
    return (nowMs / dayMs) * dayMs
}
