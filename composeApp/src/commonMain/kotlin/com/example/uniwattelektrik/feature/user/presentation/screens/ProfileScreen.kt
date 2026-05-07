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
import com.example.uniwattelektrik.core.components.PremiumHeaderBackground
import com.example.uniwattelektrik.core.components.PremiumHeaderStatusBarColor
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
    modifier: Modifier = Modifier,
) {
    TrackScreenPerformance("ProfileScreen")
    SetStatusBar(color = PremiumHeaderStatusBarColor, darkIcons = false)

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
    val teamSize    = employees.size
    val efficiency  = if (tasks.isNotEmpty()) ((doneTasks.toFloat() / tasks.size) * 100).roundToInt() else 0

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

        // 1. HEADER
        item {
            PremiumHeaderBackground {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .windowInsetsPadding(WindowInsets.statusBars)
                        .padding(horizontal = 20.dp)
                        .padding(top = 18.dp, bottom = 32.dp),
                ) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        // Role badge
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.White.copy(alpha = 0.15f))
                                .border(1.dp, Color.White.copy(alpha = 0.20f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 10.dp, vertical = 5.dp),
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                if (isAdmin) {
                                    Icon(Icons.Outlined.AdminPanelSettings, null, tint = Color.White.copy(alpha = 0.9f), modifier = Modifier.size(14.dp))
                                    Text("ADMIN", color = Color.White.copy(alpha = 0.9f), fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                                } else {
                                    Icon(Icons.Outlined.Person, null, tint = Color.White.copy(alpha = 0.9f), modifier = Modifier.size(14.dp))
                                    Text("USER", color = Color.White.copy(alpha = 0.9f), fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                                }
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            GlassIconButton(Icons.Outlined.Edit, "Edit") { }
                            GlassIconButton(Icons.AutoMirrored.Outlined.Logout, "Logout") { showLogoutDialog = true }
                        }
                    }

                    Spacer(Modifier.height(20.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .shadow(16.dp, CircleShape, spotColor = Color.Black.copy(alpha = 0.3f))
                                .clip(CircleShape)
                                .background(Brush.linearGradient(listOf(Color(0xFFFFB28A), Color(0xFFEC8552))))
                                .border(2.dp, Color.White.copy(alpha = 0.3f), CircleShape),
                            contentAlignment = Alignment.Center,
                        ) { Text(initials, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold) }
                        Spacer(Modifier.width(16.dp))
                        Column {
                            Text(name, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                            Text(role, color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp, fontFamily = FontFamily.Monospace, letterSpacing = 0.3.sp)
                            Text(user.email, color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp)
                        }
                    }

                    // Streak badge (user only)
                    if (!isAdmin && streakDays > 0) {
                        Spacer(Modifier.height(14.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFFFFB28A).copy(alpha = 0.25f))
                                .border(1.dp, Color(0xFFFFB28A).copy(alpha = 0.40f), RoundedCornerShape(10.dp))
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                                Text("🔥", fontSize = 14.sp)
                                Text("$streakDays day streak", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
        }

        // 2. PERFORMANCE KPIs
        item {
            Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
                SectionLabel("PERFORMANCE")
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    KpiCard(Icons.Outlined.TaskAlt, doneTasks, "Completed", AppTheme.Success, AppTheme.SuccessBg, Modifier.weight(1f))
                    KpiCard(Icons.Outlined.Speed,   activeTasks, "Active",  AppTheme.Brand,   AppTheme.Brand50,  Modifier.weight(1f))
                }
                if (isAdmin) {
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        KpiCard(Icons.Outlined.People,      teamSize,   "Team Size",    AppTheme.Brand,   Color(0xFFF0F0FF),   Modifier.weight(1f))
                        KpiCard(Icons.Outlined.CheckCircle, efficiency, "Efficiency %", AppTheme.Warning, AppTheme.WarningBg, Modifier.weight(1f), suffix = "%")
                    }
                }
            }
        }

        // 4. MANAGEMENT (admin only)
        if (isAdmin) {
            item {
                Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
                    SectionLabel("MANAGEMENT")
                    Spacer(Modifier.height(10.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp), maxItemsInEachRow = 2) {
                        ModuleCard(Icons.Outlined.Inventory2,               "Inventory",  "Stock & pricing",    AppTheme.SuccessBg, AppTheme.Success, Modifier.weight(1f)) { onNavigate(AdminRoute.InventoryManagement) }
                        ModuleCard(Icons.AutoMirrored.Outlined.Assignment,  "Tasks",      "$activeTasks active", AppTheme.Brand50,  AppTheme.Brand,   Modifier.weight(1f)) { onNavigate(AdminRoute.Tasks) }
                        ModuleCard(Icons.Outlined.People,                   "Employees",  "$teamSize members",  Color(0xFFF0F0FF), AppTheme.Brand,   Modifier.weight(1f)) { onNavigate(AdminRoute.Employees) }
                        ModuleCard(Icons.Outlined.LocationOn,               "Attendance", "Today's log",        AppTheme.WarningBg,AppTheme.Warning, Modifier.weight(1f)) { onNavigate(AdminRoute.Attendance) }
                        ModuleCard(Icons.Outlined.Policy,                   "Compliance", "Reports",            AppTheme.DangerBg, AppTheme.Danger,  Modifier.weight(1f)) { }
                        ModuleCard(Icons.Outlined.Notifications,            "Alerts",     "Notifications",      AppTheme.Brand100, AppTheme.Brand700,Modifier.weight(1f)) { onNavigate(AdminRoute.Notifications) }
                    }
                }
            }
        }

        // 5. RECENT ACTIVITY (admin only)
        if (isAdmin) {
            item {
                Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)) {
                    SectionLabel("RECENT ACTIVITY")
                    Spacer(Modifier.height(10.dp))
                    AppCard(cornerRadius = 20.dp, contentPadding = 0.dp) {
                        Column {
                            val lastTask     = tasks.maxByOrNull { it.updatedAt ?: 0L }
                            val lastEmployee = employees.firstOrNull()
                            ActivityRow("📋", lastTask?.title ?: "No tasks yet",         lastTask?.let { "Status: ${it.status}" } ?: "Create your first task")
                            Divider()
                            ActivityRow("👤", lastEmployee?.name ?: "No employees yet",  lastEmployee?.let { "${it.role} · ${it.status}" } ?: "Add your first team member")
                            Divider()
                            ActivityRow("📦", "Inventory Updated", "Tap to manage stock & spare items")
                        }
                    }
                }
            }
        }

        // 5b. PROFILE INFORMATION (user only)
        if (!isAdmin) {
            item {
                Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)) {
                    SectionLabel("PROFILE INFORMATION")
                    Spacer(Modifier.height(10.dp))
                    AppCard(cornerRadius = 20.dp, contentPadding = 0.dp) {
                        Column {
                            ProfileInfoRow(Icons.Outlined.Person,        "Full Name", employeeRecord?.name?.takeIf { it.isNotBlank() } ?: name,       AppTheme.Brand50,   AppTheme.Brand)
                            Divider()
                            ProfileInfoRow(Icons.Outlined.Email,         "Email",     employeeRecord?.email?.takeIf { it.isNotBlank() } ?: user.email, AppTheme.Brand50,   AppTheme.Brand)
                            Divider()
                            ProfileInfoRow(Icons.Outlined.Phone,         "Phone",     employeeRecord?.phone?.takeIf { it.isNotBlank() } ?: "—",        AppTheme.SuccessBg, AppTheme.Success)
                            Divider()
                            ProfileInfoRow(Icons.Outlined.Badge,         "Role",      employeeRecord?.role?.takeIf { it.isNotBlank() } ?: role,         AppTheme.Brand100,  AppTheme.Brand700)
                            Divider()
                            ProfileInfoRow(Icons.Outlined.Person,        "Gender",    employeeRecord?.gender?.takeIf { it.isNotBlank() } ?: "—",       Color(0xFFF0F0FF),  AppTheme.Brand)
                        }
                    }

                    Spacer(Modifier.height(12.dp))
                    SectionLabel("WORK DETAILS")
                    Spacer(Modifier.height(10.dp))
                    AppCard(cornerRadius = 20.dp, contentPadding = 0.dp) {
                        Column {
                            ProfileInfoRow(Icons.Outlined.Work,              "Employment Type", employeeRecord?.employmentType?.takeIf { it.isNotBlank() } ?: "—", AppTheme.Brand50,   AppTheme.Brand)
                            Divider()
                            ProfileInfoRow(Icons.Outlined.Group,             "Department",      employeeRecord?.department?.takeIf { it.isNotBlank() } ?: "—",     AppTheme.SuccessBg, AppTheme.Success)
                            Divider()
                            ProfileInfoRow(Icons.Outlined.LocationOn,        "Zone",            employeeRecord?.zone?.takeIf { it.isNotBlank() } ?: "—",           AppTheme.WarningBg, AppTheme.Warning)
                            Divider()
                            ProfileInfoRow(Icons.Outlined.SupervisorAccount, "Reporting To",    employeeRecord?.reportingTo?.takeIf { it.isNotBlank() } ?: "—",    Color(0xFFF0F0FF),  AppTheme.Brand)
                            Divider()
                            ProfileInfoRow(Icons.Outlined.Schedule,          "Shift",           when (employeeRecord?.shift) { "Shift1" -> "Shift 1  (09:00 – 18:00)"; "Shift2" -> "Shift 2  (13:00 – 23:00)"; else -> employeeRecord?.shift?.takeIf { it.isNotBlank() } ?: "—" }, AppTheme.Brand50, AppTheme.Brand)
                            Divider()
                            ProfileInfoRow(Icons.Outlined.CalendarMonth,     "Joining Date",    employeeRecord?.joiningDateMs?.let { formatDateMs(it) } ?: "—",    AppTheme.SuccessBg, AppTheme.Success)
                            Divider()
                            ProfileInfoRow(Icons.Outlined.Cake,              "Date of Birth",   employeeRecord?.dateOfBirthMs?.let { formatDateMs(it) } ?: "—",    AppTheme.WarningBg, AppTheme.Warning)
                            Divider()
                            // Status badge row
                            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 13.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier.size(34.dp).clip(RoundedCornerShape(10.dp))
                                        .background(when (employeeRecord?.status) { "Active" -> AppTheme.SuccessBg; "OnLeave" -> AppTheme.WarningBg; else -> AppTheme.Ink50 }),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(Icons.Outlined.CheckCircle, null,
                                        tint = when (employeeRecord?.status) { "Active" -> AppTheme.Success; "OnLeave" -> AppTheme.Warning; else -> AppTheme.Ink500 },
                                        modifier = Modifier.size(18.dp))
                                }
                                Spacer(Modifier.width(14.dp))
                                Text("Status", color = AppTheme.Ink500, fontSize = 12.sp, modifier = Modifier.weight(1f))
                                val statusText  = employeeRecord?.status ?: "—"
                                val statusColor = when (statusText) { "Active" -> AppTheme.Success; "OnLeave" -> AppTheme.Warning; "Inactive" -> AppTheme.Danger; else -> AppTheme.Ink500 }
                                Box(modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(statusColor.copy(alpha = 0.12f)).padding(horizontal = 10.dp, vertical = 4.dp)) {
                                    Text(statusText, color = statusColor, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }

                    // Additional info — only render when data exists, using smart-cast via let
                    employeeRecord?.let { rec ->
                        if (rec.address.isNotBlank() || rec.emergencyName.isNotBlank()) {
                            Spacer(Modifier.height(12.dp))
                            SectionLabel("ADDITIONAL INFORMATION")
                            Spacer(Modifier.height(10.dp))
                            AppCard(cornerRadius = 20.dp, contentPadding = 0.dp) {
                                Column {
                                    if (rec.address.isNotBlank()) {
                                        ProfileInfoRow(Icons.Outlined.LocationOn, "Address", rec.address, AppTheme.WarningBg, AppTheme.Warning)
                                    }
                                    if (rec.emergencyName.isNotBlank()) {
                                        if (rec.address.isNotBlank()) Divider()
                                        ProfileInfoRow(
                                            icon   = Icons.Outlined.ContactPhone,
                                            label  = "Emergency Contact",
                                            value  = buildString {
                                                append(rec.emergencyName)
                                                if (rec.emergencyRelation.isNotBlank()) append("  ·  ${rec.emergencyRelation}")
                                            },
                                            iconBg   = AppTheme.DangerBg,
                                            iconTint = AppTheme.Danger,
                                        )
                                        if (rec.emergencyPhone.isNotBlank()) {
                                            Divider()
                                            ProfileInfoRow(Icons.Outlined.Phone, "Emergency Phone", rec.emergencyPhone, AppTheme.DangerBg, AppTheme.Danger)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 5c. WORK SUMMARY STATS (user only)
        if (!isAdmin) {
            item {
                Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)) {
                    SectionLabel("THIS MONTH")
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        WorkStatCard("✅", doneThisMonth.toString(), "Tasks Done",   AppTheme.SuccessBg, AppTheme.Success, Modifier.weight(1f))
                        WorkStatCard("⏱",  "$onTimeRate%",           "On-Time Rate", AppTheme.Brand50,   AppTheme.Brand,   Modifier.weight(1f))
                        WorkStatCard("🌴", leaveBalance.toString(),   "Leave Left",   AppTheme.WarningBg, AppTheme.Warning, Modifier.weight(1f))
                    }
                }
            }
        }

        // 6. SETTINGS
        item {
            Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
                SectionLabel("SETTINGS")
                Spacer(Modifier.height(10.dp))
                AppCard(cornerRadius = 20.dp, contentPadding = 0.dp) {
                    Column {
                        SettingsRow(Icons.Outlined.Person,   "Personal Information") { }
                        Divider()
                        SettingsRow(Icons.Outlined.Shield,   "Security & PIN") { }
                        Divider()
                        SettingsRow(Icons.Outlined.Language, "Language & Region") { }
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
private fun WorkStatCard(emoji: String, value: String, label: String, bg: Color, color: Color, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier = modifier.shadow(6.dp, shape, spotColor = AppTheme.ShadowSm).clip(shape).background(Color.White).padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(modifier = Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(bg), contentAlignment = Alignment.Center) {
            Text(emoji, fontSize = 16.sp)
        }
        Text(value, color = color, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Text(label, color = AppTheme.Ink500, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.3.sp)
    }
}

@Composable
private fun GlassIconButton(icon: ImageVector, desc: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier.size(36.dp).clip(RoundedCornerShape(10.dp))
            .background(Color.White.copy(alpha = 0.15f))
            .border(1.dp, Color.White.copy(alpha = 0.20f), RoundedCornerShape(10.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, contentDescription = desc, tint = Color.White, modifier = Modifier.size(18.dp)) }
}

@Composable
private fun SectionLabel(text: String) {
    Text(text, color = AppTheme.Ink500, fontSize = 11.sp, fontWeight = FontWeight.Bold,
        letterSpacing = 1.2.sp, modifier = Modifier.padding(start = 4.dp))
}

@Composable
private fun KpiCard(icon: ImageVector, value: Int, label: String, accent: Color, accentBg: Color, modifier: Modifier = Modifier, suffix: String = "") {
    val anim = remember { Animatable(0f) }
    LaunchedEffect(value) { anim.snapTo(0f); anim.animateTo(value.toFloat(), tween(900, easing = FastOutSlowInEasing)) }
    val shape = RoundedCornerShape(20.dp)
    Box(modifier = modifier.shadow(8.dp, shape, spotColor = AppTheme.ShadowSm).clip(shape).background(Color.White).padding(16.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(modifier = Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(accentBg), contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = accent, modifier = Modifier.size(22.dp))
            }
            Text("${anim.value.roundToInt()}$suffix", color = AppTheme.Ink900, fontSize = 26.sp, fontWeight = FontWeight.Bold)
            Text(label, color = AppTheme.Ink500, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.3.sp)
        }
    }
}

@Composable
private fun QuickActionChip(icon: ImageVector, label: String, bg: Color, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.95f else 1f, spring(0.7f, 800f), label = "chipScale")
    Box(
        modifier = Modifier.graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(RoundedCornerShape(14.dp)).background(bg)
            .clickable(interactionSource = interactionSource, indication = ripple(), onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(icon, null, tint = Color.White, modifier = Modifier.size(18.dp))
            Text(label, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun ModuleCard(icon: ImageVector, title: String, subtitle: String, iconBg: Color, iconTint: Color, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.95f else 1f, spring(0.7f, 800f), label = "moduleScale")
    val shape = RoundedCornerShape(20.dp)
    Box(
        modifier = modifier.graphicsLayer { scaleX = scale; scaleY = scale }
            .shadow(6.dp, shape, spotColor = AppTheme.ShadowSm).clip(shape).background(Color.White)
            .clickable(interactionSource = interactionSource, indication = ripple(), onClick = onClick).padding(16.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(modifier = Modifier.size(44.dp).clip(RoundedCornerShape(13.dp)).background(iconBg), contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = iconTint, modifier = Modifier.size(24.dp))
            }
            Text(title,    color = AppTheme.Ink900, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Text(subtitle, color = AppTheme.Ink500, fontSize = 11.sp)
        }
    }
}

@Composable
private fun ActivityRow(emoji: String, title: String, detail: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 13.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(emoji, fontSize = 22.sp)
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title,  color = AppTheme.Ink900, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
            Text(detail, color = AppTheme.Ink500, fontSize = 11.sp, maxLines = 1)
        }
    }
}

@Composable
private fun ProfileInfoRow(icon: ImageVector, label: String, value: String, iconBg: Color, iconTint: Color) {
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 13.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(34.dp).clip(RoundedCornerShape(10.dp)).background(iconBg), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = iconTint, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(label, color = AppTheme.Ink500, fontSize = 11.sp, fontWeight = FontWeight.Medium)
            Text(value, color = AppTheme.Ink900, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun SettingsRow(icon: ImageVector, label: String, onClick: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(34.dp).clip(RoundedCornerShape(10.dp)).background(AppTheme.Ink50), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = AppTheme.Ink700, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(14.dp))
        Text(label, color = AppTheme.Ink900, fontSize = 14.sp, modifier = Modifier.weight(1f))
        Text("›",   color = AppTheme.Ink300, fontSize = 18.sp)
    }
}

@Composable
private fun Divider() {
    Box(modifier = Modifier.fillMaxWidth().height(1.dp).padding(start = 52.dp).background(AppTheme.Ink100))
}
