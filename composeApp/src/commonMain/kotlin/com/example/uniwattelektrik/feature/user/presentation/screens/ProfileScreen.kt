package com.example.uniwattelektrik.feature.user.presentation.screens

import com.example.uniwattelektrik.core.performance.TrackScreenPerformance

import com.example.uniwattelektrik.core.theme.appScreenBackground

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Assignment
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.automirrored.outlined.PlaylistAdd
import androidx.compose.material.icons.outlined.AdminPanelSettings
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.Cake
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ContactPhone
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.Policy
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.SupervisorAccount
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material.icons.outlined.Warehouse
import androidx.compose.material.icons.outlined.Work
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.uniwattelektrik.core.components.AppCard
import com.example.uniwattelektrik.core.components.OperationsHeaderStatusBarColor
import com.example.uniwattelektrik.core.components.OperationsHeaderSurface
import com.example.uniwattelektrik.core.navigation.AdminRoute
import com.example.uniwattelektrik.core.theme.AppTheme
import com.example.uniwattelektrik.core.theme.SetStatusBar
import com.example.uniwattelektrik.feature.auth.domain.model.User
import com.example.uniwattelektrik.feature.workforce.data.remote.EmployeeRecord
import com.example.uniwattelektrik.feature.workforce.presentation.DeleteAllState
import com.example.uniwattelektrik.feature.workforce.presentation.WorkforceViewModel
import kotlin.math.roundToInt
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProfileScreen(
    user: User,
    initials: String,
    name: String,
    role: String,
    onLogout: () -> Unit,
    isAdmin: Boolean = true,
    workforceVm: WorkforceViewModel? = null,
    onNavigate: (AdminRoute) -> Unit = {},
    /** True when rendered inside SeniorManagerShell — hides admin-only settings. */
    isSeniorManager: Boolean = false,
    /** Called when the employee taps "Personal Information" in Settings (user mode only). */
    onPersonalInfo: () -> Unit = {},
    /**
     * Called when the user taps a row in the "Recent activity" feed.
     * The status hint mirrors the task's workflow state ("Todo" / "InProgress"
     * / "InReview" / "Done") so the host shell can deep-link into the task
     * list with the matching tab pre-selected — e.g. tapping a review-stage
     * task lands the user on the "In review" tab.
     */
    onOpenTaskList: (statusHint: String?) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    TrackScreenPerformance("ProfileScreen")
    SetStatusBar(color = OperationsHeaderStatusBarColor, darkIcons = false)

    // ── Live data ───────────────────────────────────────────────────────────
    val employees by workforceVm?.employees?.collectAsState() ?: remember { mutableStateOf(emptyList()) }
    val tasks     by workforceVm?.tasks?.collectAsState()     ?: remember { mutableStateOf(emptyList()) }

    // For user mode: look up own employee record
    val employeeRecord: EmployeeRecord? = if (!isAdmin) {
        employees.firstOrNull { it.id == user.id }
    } else null

    // ── User-mode derived stats ─────────────────────────────────────────────
    val leaveRequests by workforceVm?.leaveRequests?.collectAsState()
        ?: remember { mutableStateOf(emptyList()) }
    val attendanceList by workforceVm?.attendance?.collectAsState()
        ?: remember { mutableStateOf(emptyList()) }

    // Attendance streak: count distinct completed days
    val streakDays: Int = remember(attendanceList, user.id) {
        attendanceList
            .filter { it.userId == user.id && it.status == "COMPLETED" }
            .map { rec ->
                val d = Instant.fromEpochMilliseconds(rec.dateMs)
                    .toLocalDateTime(TimeZone.currentSystemDefault())
                "${d.year}-${d.month.ordinal}-${d.dayOfMonth}"
            }
            .toSet()
            .size
    }

    // Approximate start of current month
    val nowMs = com.example.uniwattelektrik.platform.nowEpochMillis()
    val nowLocal = Instant.fromEpochMilliseconds(nowMs)
        .toLocalDateTime(TimeZone.currentSystemDefault())
    val monthStartMs = nowMs -
        ((nowLocal.dayOfMonth - 1).toLong() * 86_400_000L) -
        (nowLocal.hour.toLong() * 3_600_000L) -
        (nowLocal.minute.toLong() * 60_000L) -
        (nowLocal.second.toLong() * 1_000L)

    val doneThisMonth     = tasks.count { it.status == "Done" && (it.completedAt ?: 0L) >= monthStartMs }
    val onTimeCount       = attendanceList.count { it.userId == user.id && it.checkInStatus == "ON_TIME" }
    val totalCheckins     = attendanceList.count { it.userId == user.id }
    val onTimeRate        = if (totalCheckins > 0) (onTimeCount * 100) / totalCheckins else 0
    val approvedLeaveDays = leaveRequests.count { it.status == "approved" || it.status == "Approved" }
    val leaveBalance      = (15 - approvedLeaveDays).coerceAtLeast(0)

    val doneTasks   = tasks.count { it.status == "Done" }
    val activeTasks = tasks.count { it.status != "Done" }
    val pendingTasks = tasks.count { it.status.equals("Todo", ignoreCase = true) }
    val inProgressTasks = tasks.count { it.status.equals("InProgress", ignoreCase = true) }
    val teamSize    = employees.size
    val efficiency  = if (tasks.isNotEmpty()) ((doneTasks.toFloat() / tasks.size) * 100).roundToInt() else 0

    // Operational intelligence — surfaced as actionable cards on the admin panel.
    val spareItems by workforceVm?.spareItems?.collectAsState() ?: remember { mutableStateOf(emptyList()) }
    val notifications by workforceVm?.notifications?.collectAsState() ?: remember { mutableStateOf(emptyList()) }
    val lowStockCount = spareItems.count { it.stockQty in 1..5 }
    val zeroStockCount = spareItems.count { it.stockQty <= 0 }
    val criticalStockCount = lowStockCount + zeroStockCount
    val unreadAlertsCount = notifications.count { !it.isRead }
    // Day window for "Attendance today" — local midnight.
    val startOfDayMs = nowMs -
        (nowLocal.hour.toLong() * 3_600_000L) -
        (nowLocal.minute.toLong() * 60_000L) -
        (nowLocal.second.toLong() * 1_000L)
    val attendanceToday = attendanceList
        .filter { it.checkInMs >= startOfDayMs }
        .map { it.userId }
        .toSet()
        .size
    val activeOperationsCount = inProgressTasks
    val overdueCount = tasks.count { t ->
        val due = t.dueDate ?: return@count false
        t.status != "Done" && due < nowMs
    }

    // Month-over-month trend for the headline "Completed" KPI.
    val lastMonthStartMs = monthStartMs -
        (28L * 86_400_000L)  // approximate — only used for a relative trend label
    val doneLastMonth = tasks.count {
        val ts = it.completedAt ?: 0L
        ts in lastMonthStartMs until monthStartMs
    }
    val doneDelta = doneThisMonth - doneLastMonth

    // ── Delete all state ────────────────────────────────────────────────────
    val deleteAllState by workforceVm?.deleteAllState?.collectAsState()
        ?: remember { mutableStateOf(DeleteAllState.Idle) }.let { s ->
            s.value.let { remember(it) { mutableStateOf(it) } }
        }

    var showConfirmDialog by remember { mutableStateOf(false) }
    var showResultDialog  by remember { mutableStateOf<String?>(null) }
    var showLogoutDialog  by remember { mutableStateOf(false) }

    LaunchedEffect(deleteAllState) {
        when (val s = deleteAllState) {
            is DeleteAllState.Success -> { showResultDialog = "All data deleted successfully."; workforceVm?.clearDeleteAllState() }
            is DeleteAllState.Error   -> { showResultDialog = "Delete failed: ${s.message}";   workforceVm?.clearDeleteAllState() }
            else -> Unit
        }
    }

    // ── Dialogs ─────────────────────────────────────────────────────────────
    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            title = { Text("Delete all data?", fontWeight = FontWeight.Bold) },
            text  = { Text("This will permanently delete all employees, tasks, attendance logs, inventory, check-ins and notifications for your account.\n\nThis action cannot be undone.", color = AppTheme.Ink500) },
            confirmButton = { TextButton(onClick = { showConfirmDialog = false; workforceVm?.deleteAllData(user.id) }) { Text("Delete", color = AppTheme.Danger, fontWeight = FontWeight.Bold) } },
            dismissButton = { TextButton(onClick = { showConfirmDialog = false }) { Text("Cancel") } },
        )
    }
    showResultDialog?.let { msg ->
        AlertDialog(
            onDismissRequest = { showResultDialog = null },
            title = { Text("Done") },
            text  = { Text(msg) },
            confirmButton = { TextButton(onClick = { showResultDialog = null }) { Text("OK") } },
        )
    }
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Sign out now?", fontWeight = FontWeight.Bold) },
            text  = { Text("You will be returned to the login screen.", color = AppTheme.Ink500) },
            confirmButton = { TextButton(onClick = { showLogoutDialog = false; onLogout() }) { Text("OK", fontWeight = FontWeight.SemiBold) } },
            dismissButton = { TextButton(onClick = { showLogoutDialog = false }) { Text("Cancel") } },
        )
    }

    // ── Main layout ─────────────────────────────────────────────────────────
    LazyColumn(
        modifier = modifier.fillMaxSize().background(appScreenBackground()),
        contentPadding = PaddingValues(bottom = 120.dp),
    ) {

        // 1. HEADER — uses the shared OperationsHeaderSurface so every main
        //    screen in the app stays visually consistent. The body keeps its
        //    own bespoke identity layout (avatar + name + status dot).
        item {
            OperationsHeaderSurface(bottomPadding = 28.dp) {
                    // Top utility row: workspace label + actions.
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column {
                            Text(
                                "WORKSPACE",
                                color = Color.White.copy(alpha = 0.45f),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = 1.4.sp,
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                if (isAdmin) "UniWatt Elektrik · Admin Console"
                                else         "UniWatt Elektrik · Field Operations",
                                color = Color.White.copy(alpha = 0.9f),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (isAdmin) GlassIconButton(Icons.Outlined.Edit, "Edit") { }
                            GlassIconButton(Icons.AutoMirrored.Outlined.Logout, "Sign out") {
                                showLogoutDialog = true
                            }
                        }
                    }

                    Spacer(Modifier.height(22.dp))

                    // Identity block: avatar + name/role/email with strong hierarchy.
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(contentAlignment = Alignment.BottomEnd) {
                            Box(
                                modifier = Modifier
                                    .size(68.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.linearGradient(
                                            listOf(Color(0xFFFFB28A), Color(0xFFEC8552)),
                                        ),
                                    )
                                    .border(2.dp, Color.White.copy(alpha = 0.2f), CircleShape),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    initials,
                                    color = Color.White,
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                )
                            }
                            // Online status dot — green ring matches AppTheme.Success.
                            Box(
                                modifier = Modifier
                                    .size(14.dp)
                                    .clip(CircleShape)
                                    .background(AppTheme.Ink900)
                                    .padding(2.dp)
                                    .clip(CircleShape)
                                    .background(AppTheme.Success),
                            )
                        }
                        Spacer(Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            // Role pill — quieter than the previous oversized badge.
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color.White.copy(alpha = 0.10f))
                                        .padding(horizontal = 7.dp, vertical = 2.dp),
                                ) {
                                    Text(
                                        if (isAdmin) "ADMIN" else "EMPLOYEE",
                                        color = Color.White.copy(alpha = 0.85f),
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.2.sp,
                                    )
                                }
                                if (!isAdmin && streakDays > 0) {
                                    Spacer(Modifier.width(6.dp))
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(Color(0xFFFFB28A).copy(alpha = 0.18f))
                                            .padding(horizontal = 7.dp, vertical = 2.dp),
                                    ) {
                                        Text(
                                            "$streakDays-DAY STREAK",
                                            color = Color(0xFFFFC9A5),
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 1.0.sp,
                                        )
                                    }
                                }
                            }
                            Spacer(Modifier.height(6.dp))
                            Text(
                                name,
                                color = Color.White,
                                fontSize = 24.sp,           // canonical header title size
                                fontWeight = FontWeight.ExtraBold,
                                maxLines = 1,
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                role.ifBlank { if (isAdmin) "Administrator" else "Field Technician" },
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                            )
                            Text(
                                user.email,
                                color = Color.White.copy(alpha = 0.45f),
                                fontSize = 11.sp,
                                maxLines = 1,
                            )
                        }
                    }
                }
            }
        // 2. PERFORMANCE KPIs — trend-aware, supporting metadata.
        item {
            Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp)) {
                SectionLabel("PERFORMANCE")
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    KpiCard(
                        icon       = Icons.Outlined.TaskAlt,
                        value      = doneTasks,
                        label      = "Completed",
                        accent     = AppTheme.Success,
                        accentBg   = AppTheme.SuccessBg,
                        trendDelta = doneDelta,
                        trendLabel = "vs last month",
                        modifier   = Modifier.weight(1f),
                    )
                    KpiCard(
                        icon       = Icons.Outlined.Speed,
                        value      = activeTasks,
                        label      = "Active",
                        accent     = AppTheme.Brand,
                        accentBg   = AppTheme.Brand50,
                        subtitle   = if (pendingTasks > 0) "$pendingTasks pending" else null,
                        modifier   = Modifier.weight(1f),
                    )
                }
                if (isAdmin) {
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        KpiCard(
                            icon     = Icons.Outlined.People,
                            value    = teamSize,
                            label    = "Team",
                            accent   = AppTheme.Brand,
                            accentBg = Color(0xFFF0F0FF),
                            subtitle = if (attendanceToday > 0) "$attendanceToday on shift today" else null,
                            modifier = Modifier.weight(1f),
                        )
                        KpiCard(
                            icon     = Icons.Outlined.CheckCircle,
                            value    = efficiency,
                            label    = "Efficiency",
                            accent   = AppTheme.Warning,
                            accentBg = AppTheme.WarningBg,
                            suffix   = "%",
                            subtitle = "$doneTasks of ${tasks.size} closed",
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }

        // 4. OPERATIONS — insight-driven cards instead of generic module nav.
        //    Each one surfaces a live count + a state hint and routes to the
        //    matching screen on tap. Severity drives the accent: red for
        //    things that need action right now, brand-blue for steady state.
        if (isAdmin) {
            item {
                Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)) {
                    SectionLabel("OPERATIONS")
                    Spacer(Modifier.height(12.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement   = Arrangement.spacedBy(10.dp),
                        maxItemsInEachRow     = 2,
                    ) {
                        OperationsCard(
                            icon      = Icons.AutoMirrored.Outlined.Assignment,
                            title     = "Pending Tasks",
                            value     = pendingTasks.toString(),
                            hint      = if (overdueCount > 0) "$overdueCount overdue"
                                        else if (pendingTasks == 0) "All caught up"
                                        else "Awaiting acceptance",
                            severity  = if (overdueCount > 0) OpsSeverity.Danger
                                        else if (pendingTasks > 0) OpsSeverity.Warning
                                        else OpsSeverity.Info,
                            modifier  = Modifier.weight(1f),
                            onClick   = { onNavigate(AdminRoute.Tasks) },
                        )
                        OperationsCard(
                            icon     = Icons.Outlined.Speed,
                            title    = "Active Operations",
                            value    = activeOperationsCount.toString(),
                            hint     = if (activeOperationsCount > 0) "In-progress now" else "No active work",
                            severity = OpsSeverity.Brand,
                            modifier = Modifier.weight(1f),
                            onClick  = { onNavigate(AdminRoute.Tasks) },
                        )
                        OperationsCard(
                            icon     = Icons.Outlined.LocationOn,
                            title    = "Attendance Today",
                            value    = "$attendanceToday / $teamSize",
                            hint     = if (teamSize > 0 && attendanceToday < teamSize)
                                            "${teamSize - attendanceToday} not checked in"
                                       else "Full roster present",
                            severity = if (teamSize > 0 && attendanceToday * 2 < teamSize)
                                            OpsSeverity.Warning else OpsSeverity.Info,
                            modifier = Modifier.weight(1f),
                            onClick  = { onNavigate(AdminRoute.Attendance) },
                        )
                        OperationsCard(
                            icon     = Icons.Outlined.Inventory2,
                            title    = "Low Stock",
                            value    = criticalStockCount.toString(),
                            hint     = when {
                                zeroStockCount > 0  -> "$zeroStockCount stocked-out"
                                lowStockCount > 0   -> "Items at risk"
                                else                -> "Inventory healthy"
                            },
                            severity = when {
                                zeroStockCount > 0 -> OpsSeverity.Danger
                                lowStockCount > 0  -> OpsSeverity.Warning
                                else               -> OpsSeverity.Info
                            },
                            modifier = Modifier.weight(1f),
                            onClick  = { onNavigate(AdminRoute.InventoryManagement) },
                        )
                        OperationsCard(
                            icon     = Icons.Outlined.Notifications,
                            title    = "Critical Alerts",
                            value    = unreadAlertsCount.toString(),
                            hint     = if (unreadAlertsCount > 0) "Tap to review"
                                       else "No unread alerts",
                            severity = if (unreadAlertsCount > 0) OpsSeverity.Danger
                                       else OpsSeverity.Info,
                            modifier = Modifier.weight(1f),
                            onClick  = { onNavigate(AdminRoute.Notifications) },
                        )
                        OperationsCard(
                            icon     = Icons.Outlined.Insights,
                            title    = "Monthly Report",
                            value    = "$doneThisMonth",
                            hint     = "Tasks closed this month",
                            severity = OpsSeverity.Brand,
                            modifier = Modifier.weight(1f),
                            onClick  = { onNavigate(AdminRoute.Reports) },
                        )
                    }
                }
            }
        }

        // 5. RECENT ACTIVITY (admin only) — timestamps, status chips, hairline rows.
        if (isAdmin) {
            item {
                Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        SectionLabel("RECENT ACTIVITY")
                        Spacer(Modifier.weight(1f))
                        // Subtle live indicator — pulses with new data via the
                        // snapshot listener; no extra wiring needed.
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(AppTheme.Success),
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "LIVE",
                            color = AppTheme.Success,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp,
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    AppCard(cornerRadius = 18.dp, contentPadding = 0.dp) {
                        Column {
                            val recentTasks = tasks
                                .filter { (it.updatedAt ?: it.createdAtMs ?: 0L) > 0L }
                                .sortedByDescending { it.updatedAt ?: it.createdAtMs ?: 0L }
                                .take(4)
                            if (recentTasks.isEmpty()) {
                                ActivityRowEmpty(
                                    icon  = Icons.AutoMirrored.Outlined.Assignment,
                                    title = "No activity yet",
                                    body  = "Create your first task to see it here.",
                                )
                            } else {
                                recentTasks.forEachIndexed { index, t ->
                                    val ts = t.updatedAt ?: t.createdAtMs ?: 0L
                                    ActivityRow(
                                        icon     = Icons.AutoMirrored.Outlined.Assignment,
                                        title    = t.title.ifBlank { "Untitled task" },
                                        subtitle = listOfNotNull(
                                            t.assigneeName.takeIf { it.isNotBlank() },
                                            t.departmentName.takeIf { it.isNotBlank() },
                                        ).joinToString(" · ").ifBlank { "Unassigned" },
                                        statusLabel = taskStatusLabel(t.status),
                                        statusTint  = taskStatusTint(t.status),
                                        statusBg    = taskStatusBg(t.status),
                                        relativeTime = formatRelativeTime(ts, nowMs),
                                        // Admin recent-activity rows route to the Tasks screen with
                                        // the matching workflow tab preselected (see AdminShell).
                                        onClick = { onOpenTaskList(t.status) },
                                    )
                                    if (index != recentTasks.lastIndex) HairlineDivider()
                                }
                            }
                        }
                    }
                }
            }
        }

        // 5b. "View full profile" shortcut card (user only)
        // Detailed fields live in EmployeePersonalInfoScreen — reachable from
        // Settings → Personal Information, or via the shortcut card below.
        if (!isAdmin) {
            item {
                Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)) {
                    SectionLabel("MY INFORMATION")
                    Spacer(Modifier.height(10.dp))
                    AppCard(cornerRadius = 20.dp, contentPadding = 0.dp) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(onClick = onPersonalInfo)
                                .padding(horizontal = 16.dp, vertical = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(AppTheme.Brand50),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    Icons.Outlined.Person,
                                    contentDescription = null,
                                    tint     = AppTheme.Brand,
                                    modifier = Modifier.size(22.dp),
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    employeeRecord?.name?.takeIf { it.isNotBlank() } ?: name,
                                    color      = AppTheme.Ink900,
                                    fontSize   = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    listOfNotNull(
                                        (employeeRecord?.role?.takeIf { it.isNotBlank() } ?: role).takeIf { it.isNotBlank() },
                                        employeeRecord?.department?.takeIf { it.isNotBlank() },
                                    ).joinToString(" · ").ifBlank { user.email },
                                    color    = AppTheme.Ink500,
                                    fontSize = 12.sp,
                                    maxLines = 1,
                                )
                            }
                            Text("›", color = AppTheme.Ink300, fontSize = 22.sp)
                        }
                    }
                }
            }
        }

        // 5c. THIS MONTH stats (user only) — trend-aware, matches admin KPI style.
        if (!isAdmin) {
            item {
                Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)) {
                    SectionLabel("THIS MONTH")
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        KpiCard(
                            icon       = Icons.Outlined.TaskAlt,
                            value      = doneThisMonth,
                            label      = "Tasks Done",
                            accent     = AppTheme.Success,
                            accentBg   = AppTheme.SuccessBg,
                            trendDelta = doneDelta,
                            trendLabel = "vs last month",
                            modifier   = Modifier.weight(1f),
                        )
                        KpiCard(
                            icon     = Icons.Outlined.Schedule,
                            value    = onTimeRate,
                            label    = "On-Time",
                            accent   = AppTheme.Brand,
                            accentBg = AppTheme.Brand50,
                            suffix   = "%",
                            subtitle = if (totalCheckins > 0) "$onTimeCount of $totalCheckins" else null,
                            modifier = Modifier.weight(1f),
                        )
                        KpiCard(
                            icon     = Icons.Outlined.Cake,
                            value    = leaveBalance,
                            label    = "Leave Left",
                            accent   = AppTheme.Warning,
                            accentBg = AppTheme.WarningBg,
                            subtitle = if (approvedLeaveDays > 0) "$approvedLeaveDays taken" else null,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }

        // 5d. RECENT ACTIVITY (user only) — own tasks, sorted newest first.
        if (!isAdmin) {
            item {
                Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        SectionLabel("RECENT ACTIVITY")
                        Spacer(Modifier.weight(1f))
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(AppTheme.Success),
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "LIVE",
                            color = AppTheme.Success,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp,
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    AppCard(cornerRadius = 18.dp, contentPadding = 0.dp) {
                        Column {
                            val myRecent = tasks
                                .filter { it.userId == user.id }
                                .filter { (it.updatedAt ?: it.createdAtMs ?: 0L) > 0L }
                                .sortedByDescending { it.updatedAt ?: it.createdAtMs ?: 0L }
                                .take(4)
                            if (myRecent.isEmpty()) {
                                ActivityRowEmpty(
                                    icon  = Icons.AutoMirrored.Outlined.Assignment,
                                    title = "No tasks yet",
                                    body  = "Tasks assigned to you will appear here.",
                                )
                            } else {
                                myRecent.forEachIndexed { index, t ->
                                    val ts = t.updatedAt ?: t.createdAtMs ?: 0L
                                    ActivityRow(
                                        icon     = Icons.AutoMirrored.Outlined.Assignment,
                                        title    = t.title.ifBlank { "Untitled task" },
                                        subtitle = listOfNotNull(
                                            t.departmentName.takeIf { it.isNotBlank() },
                                            t.location.takeIf { it.isNotBlank() },
                                        ).joinToString(" · ").ifBlank { "—" },
                                        statusLabel = taskStatusLabel(t.status),
                                        statusTint  = taskStatusTint(t.status),
                                        statusBg    = taskStatusBg(t.status),
                                        relativeTime = formatRelativeTime(ts, nowMs),
                                        // Tapping routes to the user task list with this task's
                                        // status tab focused — review-stage rows land on
                                        // "In review", in-progress rows on "In progress", etc.
                                        onClick = { onOpenTaskList(t.status) },
                                    )
                                    if (index != myRecent.lastIndex) HairlineDivider()
                                }
                            }
                        }
                    }
                }
            }
        }

        // 6. SETTINGS — admin-only.  Employees see a leaner profile (header,
        //    work stats, leave summary) without Personal Info / Security /
        //    Language rows; admins keep the full block including Link Manager.
        if (isAdmin) {
            item {
                Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
                    SectionLabel("SETTINGS")
                    Spacer(Modifier.height(10.dp))
                    AppCard(cornerRadius = 20.dp, contentPadding = 0.dp) {
                        Column {
                            // "Personal Information" row removed for admin —
                            // admins manage their own record via the team list
                            // / Link Manager, not through this settings tab.
                            SettingsRow(
                                icon = Icons.Outlined.Shield,
                                label = "Security & PIN",
                                trailingBadge = "COMING SOON",
                                enabled = false,
                            ) { }
                            HairlineDivider()
                            SettingsRow(
                                icon          = Icons.Outlined.Language,
                                label         = "Language & Region",
                                trailingBadge = "COMING SOON",
                                enabled       = false,
                            ) { }
                            HairlineDivider()
                            // Monthly report — aggregated KPIs + CSV export.
                            SettingsRow(
                                icon  = Icons.Outlined.Insights,
                                label = "Monthly Reports",
                                tint  = AppTheme.Brand,
                                onClick = { onNavigate(AdminRoute.Reports) },
                            )
                            // Link Manager — visible only to full admins (not senior managers)
                            if (!isSeniorManager) {
                                HairlineDivider()
                                SettingsRow(
                                    icon  = Icons.Outlined.AdminPanelSettings,
                                    label = "Link Manager",
                                    tint  = Color(0xFF6C3CE1),
                                    onClick = { onNavigate(AdminRoute.LinkManager) },
                                )
                            }
                        }
                    }
                }
            }
        }

        // 7. DANGER ZONE (admin only) + LOGOUT
        // Hidden by product decision — destructive bulk-delete is no longer
        // exposed in the admin More screen. Keep the block intact (gated to
        // `false`) so it's easy to re-enable later if needed.
        @Suppress("ConstantConditionIf")
        if (false && isAdmin && workforceVm != null) {
            item {
                Column(modifier = Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SectionLabel("DANGER ZONE")
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(AppTheme.DangerBg)
                            .clickable(enabled = deleteAllState !is DeleteAllState.Loading, onClick = { showConfirmDialog = true })
                            .padding(horizontal = 20.dp, vertical = 16.dp),
                    ) {
                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                            Column {
                                Text("Delete all data", color = AppTheme.Danger, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                                Text("Removes all employees, tasks, attendance & inventory", color = AppTheme.Danger.copy(alpha = 0.65f), fontSize = 11.sp)
                            }
                            if (deleteAllState is DeleteAllState.Loading) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = AppTheme.Danger, strokeWidth = 2.dp)
                            }
                        }
                    }
                }
            }
        }

        item {
            Box(
                modifier = Modifier
                    .padding(horizontal = 20.dp, vertical = 12.dp)
                    .fillMaxWidth()
                    .height(50.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(AppTheme.DangerBg)
                    .clickable { showLogoutDialog = true },
                contentAlignment = Alignment.Center,
            ) {
                Text("Sign out", color = AppTheme.Danger, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            }
        }

        // App version — anchored at the very bottom of the settings list.
        // Bumped manually with each release; sourced from versionName in
        // build.gradle.kts. Subtle so it doesn't compete with sign-out.
        item {
            Box(
                modifier         = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp, bottom = 24.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "v 1.0",
                    color      = AppTheme.Ink300,
                    fontSize   = 12.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 0.4.sp,
                )
            }
        }
    }
}

// ── Helpers ────────────────────────────────────────────────────────────────

private fun formatDateMs(ms: Long): String = try {
    val local = Instant.fromEpochMilliseconds(ms).toLocalDateTime(TimeZone.currentSystemDefault())
    val d = local.dayOfMonth.toString().padStart(2, '0')
    val m = (local.month.ordinal + 1).toString().padStart(2, '0')
    "$d / $m / ${local.year}"
} catch (_: Exception) { "—" }

// ── Private components ─────────────────────────────────────────────────────

@Composable
private fun GlassIconButton(icon: ImageVector, desc: String, onClick: () -> Unit) {
    // Outlined glass chip — quieter, more enterprise-feeling than a tinted fill.
    Box(
        modifier = Modifier.size(36.dp).clip(RoundedCornerShape(10.dp))
            .background(Color.White.copy(alpha = 0.08f))
            .border(1.dp, Color.White.copy(alpha = 0.18f), RoundedCornerShape(10.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = desc, tint = Color.White.copy(alpha = 0.9f),
             modifier = Modifier.size(16.dp))
    }
}

@Composable
private fun SectionLabel(text: String) {
    // 10sp · 1.4 letterSpacing · ink500 — the entire screen uses this exact
    // treatment for section headers so the rhythm stays consistent.
    Text(text, color = AppTheme.Ink500, fontSize = 10.sp, fontWeight = FontWeight.Bold,
        letterSpacing = 1.4.sp)
}

@Composable
private fun KpiCard(
    icon       : ImageVector,
    value      : Int,
    label      : String,
    accent     : Color,
    accentBg   : Color,
    modifier   : Modifier = Modifier,
    suffix     : String = "",
    /** Month-over-month delta — drives the trend caption. Null hides it. */
    trendDelta : Int? = null,
    trendLabel : String? = null,
    /** Static supporting metadata when no trend exists (e.g. "5 of 12"). */
    subtitle   : String? = null,
) {
    val anim = remember { Animatable(0f) }
    LaunchedEffect(value) {
        anim.snapTo(0f); anim.animateTo(value.toFloat(), tween(900, easing = FastOutSlowInEasing))
    }
    val shape = RoundedCornerShape(16.dp)
    Box(
        modifier = modifier
            .shadow(1.dp, shape, spotColor = Color(0x12000000))
            .clip(shape)
            .background(AppTheme.Surface)
            .border(1.dp, AppTheme.Ink100, shape),
    ) {
        Row {
            // Left-edge accent stripe — gives each card identity without
            // tinting the whole background.
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .fillMaxHeight()
                    .background(accent),
            )
            Column(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(accentBg),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(icon, null, tint = accent, modifier = Modifier.size(15.dp))
                    }
                }
                Text(
                    "${anim.value.roundToInt()}$suffix",
                    color = AppTheme.Ink900,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.ExtraBold,
                )
                Text(
                    label.uppercase(),
                    color = AppTheme.Ink500,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp,
                )
                when {
                    trendDelta != null && trendLabel != null -> TrendCaption(trendDelta, trendLabel)
                    subtitle != null -> Text(
                        subtitle,
                        color = AppTheme.Ink500,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
        }
    }
}

@Composable
private fun TrendCaption(delta: Int, label: String) {
    val (sign, tint) = when {
        delta > 0 -> "↑ $delta"  to AppTheme.Success
        delta < 0 -> "↓ ${-delta}" to AppTheme.Danger
        else      -> "·"          to AppTheme.Ink500
    }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(sign, color = tint, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
        Text(label, color = AppTheme.Ink500, fontSize = 10.sp)
    }
}

/* ─── Operations card — actionable insight tile ───────────────────────── */

private enum class OpsSeverity { Info, Warning, Danger, Brand }

@Composable
private fun OperationsCard(
    icon    : ImageVector,
    title   : String,
    value   : String,
    hint    : String,
    severity: OpsSeverity,
    modifier: Modifier = Modifier,
    onClick : () -> Unit,
) {
    val tint = when (severity) {
        OpsSeverity.Danger  -> AppTheme.Danger
        OpsSeverity.Warning -> AppTheme.Warning
        OpsSeverity.Brand   -> AppTheme.Brand
        OpsSeverity.Info    -> AppTheme.Ink700
    }
    val bg = when (severity) {
        OpsSeverity.Danger  -> AppTheme.DangerBg
        OpsSeverity.Warning -> AppTheme.WarningBg
        OpsSeverity.Brand   -> AppTheme.Brand50
        OpsSeverity.Info    -> AppTheme.Ink50
    }
    // Visual consistency — every severity sits on the same clean white surface
    // so a Danger card can't accidentally read as a washed-out "white shade"
    // inside a pink halo (caused by the old translucent DangerBg trick which
    // didn't match the fully-opaque icon-tile background sat on top of it).
    // Severity is now conveyed by the icon-tile colour + a 1dp tinted border
    // on Danger only — keeping the eye drawn to anything that needs action.
    val cardBg = AppTheme.Surface
    val borderColor = if (severity == OpsSeverity.Danger) {
        AppTheme.Danger.copy(alpha = 0.35f)
    } else {
        Color.Transparent
    }
    val shape = RoundedCornerShape(16.dp)
    Box(
        modifier = modifier
            .shadow(1.dp, shape, spotColor = Color(0x10000000))
            .clip(shape)
            .background(cardBg)
            .border(1.dp, color = borderColor, shape)
            .clickable(onClick = onClick)
            .padding(14.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(bg),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(icon, null, tint = tint, modifier = Modifier.size(17.dp))
                }
                Spacer(Modifier.weight(1f))
                Text("›", color = AppTheme.Ink300, fontSize = 18.sp)
            }
            Text(
                value,
                color = AppTheme.Ink900,
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
            )
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    title,
                    color = AppTheme.Ink900,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    hint,
                    color = tint.copy(alpha = if (severity == OpsSeverity.Info) 0.7f else 0.9f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
        }
    }
}

/* ─── Activity feed row ───────────────────────────────────────────────── */

@Composable
private fun ActivityRow(
    icon         : ImageVector,
    title        : String,
    subtitle     : String,
    statusLabel  : String,
    statusTint   : Color,
    statusBg     : Color,
    relativeTime : String,
    onClick      : (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(9.dp))
                .background(AppTheme.Ink50),
            contentAlignment = Alignment.Center,
        ) { Icon(icon, null, tint = AppTheme.Ink700, modifier = Modifier.size(16.dp)) }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                title,
                color = AppTheme.Ink900,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    subtitle,
                    color = AppTheme.Ink500,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (relativeTime.isNotBlank()) {
                    Text(
                        " · $relativeTime",
                        color = AppTheme.Ink300,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
        }
        Spacer(Modifier.width(10.dp))
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(statusBg)
                .padding(horizontal = 7.dp, vertical = 3.dp),
        ) {
            Text(
                statusLabel,
                color = statusTint,
                fontSize = 9.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 0.8.sp,
            )
        }
    }
}

@Composable
private fun ActivityRowEmpty(icon: ImageVector, title: String, body: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(9.dp))
                .background(AppTheme.Ink50),
            contentAlignment = Alignment.Center,
        ) { Icon(icon, null, tint = AppTheme.Ink500, modifier = Modifier.size(16.dp)) }
        Spacer(Modifier.width(12.dp))
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, color = AppTheme.Ink700, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Text(body,  color = AppTheme.Ink500, fontSize = 11.sp)
        }
    }
}

/* ─── Status + time helpers ───────────────────────────────────────────── */

private fun taskStatusLabel(status: String): String = when (status.lowercase()) {
    "done", "completed" -> "DONE"
    "inprogress"        -> "ACTIVE"
    "todo"              -> "OPEN"
    else                -> status.uppercase()
}

@Composable
private fun taskStatusTint(status: String): Color = when (status.lowercase()) {
    "done", "completed" -> AppTheme.Success
    "inprogress"        -> AppTheme.Brand
    "todo"              -> AppTheme.Warning
    else                -> AppTheme.Ink700
}

@Composable
private fun taskStatusBg(status: String): Color = when (status.lowercase()) {
    "done", "completed" -> AppTheme.SuccessBg
    "inprogress"        -> AppTheme.Brand50
    "todo"              -> AppTheme.WarningBg
    else                -> AppTheme.Ink50
}

/** "2m ago", "3h ago", "5d ago" — minimal, unambiguous, fits in tight rows. */
private fun formatRelativeTime(eventMs: Long, nowMs: Long): String {
    if (eventMs <= 0L) return ""
    val diff = (nowMs - eventMs).coerceAtLeast(0L)
    val minutes = diff / 60_000L
    if (minutes < 1L) return "now"
    if (minutes < 60L) return "${minutes}m ago"
    val hours = minutes / 60L
    if (hours < 24L) return "${hours}h ago"
    val days = hours / 24L
    if (days < 30L) return "${days}d ago"
    val months = days / 30L
    return "${months}mo ago"
}

@Composable
private fun SettingsRow(
    icon: ImageVector,
    label: String,
    tint: Color = AppTheme.Ink700,
    trailingBadge: String? = null,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    val iconBg = if (tint == AppTheme.Ink700) AppTheme.Ink50 else tint.copy(alpha = 0.12f)
    val rowAlpha = if (enabled) 1f else 0.55f
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 16.dp, vertical = 14.dp)
            .alpha(rowAlpha),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.size(34.dp).clip(RoundedCornerShape(10.dp)).background(iconBg),
            contentAlignment = Alignment.Center,
        ) { Icon(icon, null, tint = tint, modifier = Modifier.size(17.dp)) }
        Spacer(Modifier.width(14.dp))
        Text(
            label,
            color = if (tint == AppTheme.Ink700) AppTheme.Ink900 else tint,
            fontSize = 14.sp,
            fontWeight = if (tint == AppTheme.Ink700) FontWeight.Medium else FontWeight.SemiBold,
            modifier = Modifier.weight(1f),
        )
        if (trailingBadge != null) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(AppTheme.Brand50)
                    .padding(horizontal = 7.dp, vertical = 3.dp),
            ) {
                Text(
                    trailingBadge,
                    color = AppTheme.Brand,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.8.sp,
                )
            }
            Spacer(Modifier.width(8.dp))
        }
        Text("›", color = AppTheme.Ink300, fontSize = 18.sp)
    }
}

@Composable
private fun HairlineDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 60.dp)   // align with content, skipping the icon column
            .height(1.dp)
            .background(AppTheme.Ink100),
    )
}
