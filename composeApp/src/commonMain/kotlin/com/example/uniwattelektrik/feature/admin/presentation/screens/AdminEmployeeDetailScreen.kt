package com.example.uniwattelektrik.feature.admin.presentation.screens

import com.example.uniwattelektrik.core.performance.TrackScreenPerformance

import com.example.uniwattelektrik.core.theme.appScreenBackground
import com.example.uniwattelektrik.core.theme.AppShapes
import com.example.uniwattelektrik.core.theme.AppTheme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.uniwattelektrik.feature.workforce.data.remote.AttendanceRecord
import com.example.uniwattelektrik.feature.workforce.data.remote.EmployeeRecord
import com.example.uniwattelektrik.feature.workforce.data.remote.TaskRecord
import com.example.uniwattelektrik.feature.workforce.presentation.WorkforceViewModel
import com.example.uniwattelektrik.platform.nowEpochMillis
import kotlin.math.absoluteValue
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
// ─── Design tokens — backed by enterprise system ────────────────────────────
private val ScreenBg     = AppTheme.Bg
private val CardBg       = AppTheme.Surface
private val InkPrimary   = AppTheme.Ink900
private val InkSecondary = AppTheme.Ink500
private val InkMuted     = AppTheme.Ink300
private val Brand        = AppTheme.Brand
private val BrandDeep    = AppTheme.Brand700
private val Brand50      = AppTheme.Brand50
private val Success      = AppTheme.Success
private val SuccessBg    = AppTheme.SuccessBg
private val Warning      = AppTheme.Warning
private val WarningBg    = AppTheme.WarningBg
private val Danger       = AppTheme.Danger
private val DangerBg     = AppTheme.DangerBg
private val DividerSoft  = AppTheme.Ink100
private val ShadowSoft   = AppTheme.ShadowMd
private val Purple       = AppTheme.Violet
private val PurpleBg     = AppTheme.PriorityUrgentBg


/* ── Local design tokens (match the spec) ───────────────────────────────── */

/**
 * Premium Employee Profile screen.
 *
 *  ┌ Gradient header — back · "Employee Profile" · ⋮ · ✏️ ─┐
 *  │ Avatar (gradient) + name + role + status pill        │
 *  ├ Floating stats card (Tasks · SLA · Rating · Attend.) ┤
 *  │ Tabs — Overview · Tasks · Performance · Docs         │
 *  │ Quick actions — Message · Call · Locate              │
 *  │ Contact card (phone, email, address)                 │
 *  │ Joined info                                          │
 *  │ Performance ring + metrics                           │
 *  │ Active assignments list                              │
 *  └ Bottom actions — Reassign · Reports · Suspend ───────┘
 */
@Composable
fun AdminEmployeeDetailScreen(
    employeeId: String,
    workforceVm: WorkforceViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    adminUid: String = "",
) {
    TrackScreenPerformance("AdminEmployeeDetailScreen")
    val employees  by workforceVm.employees.collectAsStateWithLifecycle()
    val tasks      by workforceVm.tasks.collectAsStateWithLifecycle()
    val attendance by workforceVm.attendance.collectAsStateWithLifecycle()

    com.example.uniwattelektrik.core.theme.SetStatusBar(color = Brand, darkIcons = false)

    val employee = employees.firstOrNull { it.id == employeeId }
    val myTasks  = tasks.filter { it.userId == employeeId }
    var tab      by remember { mutableStateOf("Overview") }

    if (employee == null) {
        Box(
            modifier = modifier.fillMaxSize().background(appScreenBackground()),
            contentAlignment = Alignment.Center,
        ) {
            Text("Employee not found", color = InkSecondary, fontSize = 14.sp)
        }
        return
    }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope             = rememberCoroutineScope()

    val myAttendance = remember(attendance, employeeId) {
        attendance.filter { it.userId == employeeId }
            .sortedByDescending { it.checkInMs }
    }

    val activeTaskCount = myTasks.count { it.status != "Done" }
    val completedCount  = myTasks.count { it.status == "Done" }
    val totalTasks      = myTasks.size

    // ── Real performance calculation ────────────────────────────────────────
    val nowMs = remember { nowEpochMillis() }
    val thirtyDaysMs = 30L * 24 * 60 * 60 * 1000
    val recentAttendance = myAttendance.filter { it.checkInMs >= nowMs - thirtyDaysMs }

    val attendancePct = if (recentAttendance.isEmpty()) 0 else {
        val days = recentAttendance.map {
            Instant.fromEpochMilliseconds(it.checkInMs)
                .toLocalDateTime(TimeZone.currentSystemDefault()).date
        }.toSet().size
        ((days.toFloat() / 26f) * 100).toInt().coerceIn(0, 100)
    }

    val qualityPct = if (totalTasks == 0) 0 else
        ((completedCount.toFloat() / totalTasks) * 100).toInt()

    val completedOnTime = myTasks.count { task ->
        task.status == "Done" && task.completedAt != null && task.scheduledDateMs != null &&
            task.completedAt <= task.scheduledDateMs
    }
    val slaPct = if (completedCount == 0) 0 else
        ((completedOnTime.toFloat() / completedCount) * 100).toInt()

    val overallPct = when {
        totalTasks == 0 && recentAttendance.isEmpty() -> 0
        else -> ((attendancePct * 0.4f) + (qualityPct * 0.4f) + (slaPct * 0.2f)).toInt()
    }

    val performance = OverallPerformance(
        overall    = overallPct,
        sla        = slaPct,
        quality    = qualityPct,
        attendance = attendancePct,
        customer   = 0.0,
    )

    val rating = if (overallPct == 0) 0.0 else (overallPct / 20.0).coerceIn(0.0, 5.0)

    Box(modifier = modifier.fillMaxSize()) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().background(appScreenBackground()),
        contentPadding = PaddingValues(bottom = 110.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        item {
            Box {
                ProfileHeader(
                    employee = employee,
                    onBack   = onBack,
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .offset(y = 460.dp.minus(40.dp)),
                ) { }
            }
        }

        // Stats card
        item {
            Box(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .offset(y = (-32).dp),
            ) {
                StatsCard(
                    tasks      = totalTasks,
                    slaPct     = slaPct,
                    rating     = rating,
                    attendance = attendancePct,
                )
            }
        }

        // Tabs
        item {
            Box(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .offset(y = (-16).dp),
            ) {
                TabsRow(
                    current = tab,
                    onSelect = { tab = it },
                )
            }
        }

        // ── Overview tab content ────────────────────────────────────────────
        if (tab == "Overview") {
            // Quick actions
            item {
                Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                    QuickActions(
                        onMessage = {},
                        onCall    = {},
                        onLocate  = {},
                    )
                }
                Spacer(Modifier.height(20.dp))
            }

            // Contact card
            item {
                Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                    ContactCard(employee = employee, onEdit = {})
                }
                Spacer(Modifier.height(16.dp))
            }

            // Joined card
            item {
                Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                    JoinedRow(employee = employee)
                }
                Spacer(Modifier.height(20.dp))
            }

            // Active assignments (top 3 preview)
            item {
                Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                    ActiveAssignmentsCard(tasks = myTasks.take(3))
                }
                Spacer(Modifier.height(20.dp))
            }
        }

        // ── Tasks tab content ───────────────────────────────────────────────
        if (tab == "Tasks") {
            item {
                Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                    ActiveAssignmentsCard(tasks = myTasks)
                }
                Spacer(Modifier.height(20.dp))
            }
        }

        // ── Performance tab content ─────────────────────────────────────────
        if (tab == "Performance") {
            item {
                Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                    PremiumPerformanceHero(performance = performance)
                }
                Spacer(Modifier.height(14.dp))
            }
            item {
                Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                    PremiumMetricGrid(performance = performance)
                }
                Spacer(Modifier.height(14.dp))
            }
            item {
                Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                    PremiumTaskMetrics(
                        total     = totalTasks,
                        completed = completedCount,
                        onTime    = completedOnTime,
                        active    = activeTaskCount,
                    )
                }
                Spacer(Modifier.height(14.dp))
            }
            item {
                Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                    PremiumAttendanceLog(records = myAttendance.take(14))
                }
                Spacer(Modifier.height(20.dp))
            }
        }

        // ── Docs tab content ────────────────────────────────────────────────
        if (tab == "Docs") {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 40.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "Coming soon",
                        color = InkSecondary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
        }

        // Bottom actions (always visible)
        item {
            Row(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                BottomAction(
                    icon = Icons.Filled.SwapHoriz,
                    label = "Reassign",
                    tint = Warning,
                    bg = Color(0xFFFEF3C7),
                    modifier = Modifier.weight(1f),
                    onClick = {
                        scope.launch { snackbarHostState.showSnackbar("Coming soon") }
                    },
                )
                BottomAction(
                    icon = Icons.Filled.Insights,
                    label = "Reports",
                    tint = Brand,
                    bg = Color(0xFFE6F0FE),
                    modifier = Modifier.weight(1f),
                    onClick = {
                        scope.launch { snackbarHostState.showSnackbar("Coming soon") }
                    },
                )
                BottomAction(
                    icon = Icons.Filled.Block,
                    label = "Suspend",
                    tint = Danger,
                    bg = Color(0xFFFEE2E2),
                    modifier = Modifier.weight(1f),
                    labelColor = Danger,
                    onClick = {
                        workforceVm.updateEmployeeStatus(
                            adminId    = adminUid,
                            employeeId = employee.id,
                            status     = "Relieved",
                        )
                        onBack()
                    },
                )
            }
        }
    } // end LazyColumn

    SnackbarHost(
        hostState = snackbarHostState,
        modifier  = Modifier.align(Alignment.BottomCenter),
    )
    } // end outer Box
}

/* ─────────────────────────────────────────────────────────────────────── *
 *  HEADER
 * ─────────────────────────────────────────────────────────────────────── */

@Composable
private fun ProfileHeader(
    employee: EmployeeRecord,
    onBack: () -> Unit,
) {
    com.example.uniwattelektrik.core.components.PremiumHeaderBackground(
        roundedBottom = false,
    ) {
        Column(
            modifier = Modifier
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(horizontal = 18.dp)
                .padding(top = 14.dp, bottom = 60.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                com.example.uniwattelektrik.core.components.GlassBackButton(
                    onClick = onBack,
                )
                Spacer(Modifier.width(14.dp))
                Text(
                    "Employee Profile",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                )
                GlassButton(onClick = {}) {
                    Icon(
                        imageVector = Icons.Filled.MoreVert,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp),
                    )
                }
                Spacer(Modifier.width(8.dp))
                /* Edit button — solid white circle with brand-blue icon */
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .shadow(8.dp, RoundedCornerShape(14.dp), spotColor = Color(0x33000000))
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color.White)
                        .clickable {},
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Filled.Edit,
                        contentDescription = null,
                        tint = Brand,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }

            Spacer(Modifier.height(22.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                /* Avatar with gradient + status dot */
                Box(modifier = Modifier.size(108.dp)) {
                    val gradient = avatarGradientFor(employee.id)
                    Box(
                        modifier = Modifier
                            .size(108.dp)
                            .shadow(20.dp, RoundedCornerShape(28.dp),
                                    spotColor = Color(0x55000000))
                            .clip(RoundedCornerShape(28.dp))
                            .background(Brush.linearGradient(gradient)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            employee.name.take(2).uppercase().ifBlank { "??" },
                            color = Color.White,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    /* Online status — green dot bottom-right */
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .align(Alignment.BottomEnd)
                            .offset(x = (-4).dp, y = (-4).dp)
                            .clip(CircleShape)
                            .background(Success)
                            .border(3.dp, Color.White, CircleShape),
                    )
                }

                Spacer(Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        employee.name.ifBlank { "Unnamed" },
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        listOfNotNull(
                            employee.role.ifBlank { null },
                            employee.department.ifBlank { null },
                        ).joinToString(" · ").ifBlank { "Field Operations" },
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )

                    Spacer(Modifier.height(10.dp))
                    /* EMP-XXXX · STATUS pill */
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(Color.White.copy(alpha = 0.18f))
                            .border(1.dp, Color.White.copy(alpha = 0.25f),
                                    RoundedCornerShape(50))
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            shortEmpCode(employee.id),
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp,
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "•",
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 11.sp,
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            employee.status.uppercase().replace("ONLEAVE", "ON-LEAVE")
                                .ifBlank { "ON-SITE" },
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GlassButton(onClick: () -> Unit, content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White.copy(alpha = 0.18f))
            .border(1.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
        content = { content() },
    )
}

/* ─────────────────────────────────────────────────────────────────────── *
 *  STATS CARD
 * ─────────────────────────────────────────────────────────────────────── */

@Composable
private fun StatsCard(
    tasks: Int,
    slaPct: Int,
    rating: Double,
    attendance: Int,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .shadow(16.dp, RoundedCornerShape(20.dp), spotColor = ShadowSoft)
            .clip(RoundedCornerShape(20.dp))
            .background(CardBg)
            .padding(vertical = 16.dp),
    ) {
        StatItem(value = tasks.toString(), label = "TASKS",
                 valueColor = InkPrimary, modifier = Modifier.weight(1f))
        VerticalDivider()
        StatItem(value = "$slaPct%", label = "SLA HIT",
                 valueColor = Success, percentSuffix = true,
                 modifier = Modifier.weight(1f))
        VerticalDivider()
        StatItem(value = "${"%.1f".format(rating)}★", label = "RATING",
                 valueColor = InkPrimary, modifier = Modifier.weight(1f))
        VerticalDivider()
        StatItem(value = "$attendance%", label = "ATTEND.",
                 valueColor = Success, percentSuffix = true,
                 modifier = Modifier.weight(1f))
    }
}

@Composable
private fun StatItem(
    value: String,
    label: String,
    valueColor: Color,
    percentSuffix: Boolean = false,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        if (percentSuffix && value.endsWith("%")) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    value.removeSuffix("%"),
                    color = valueColor,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    "%",
                    color = valueColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(bottom = 4.dp, start = 1.dp),
                )
            }
        } else {
            Text(
                value,
                color = valueColor,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
            )
        }
        Text(
            label,
            color = InkSecondary,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.6.sp,
        )
    }
}

@Composable
private fun VerticalDivider() {
    Box(
        modifier = Modifier
            .width(1.dp)
            .fillMaxHeight()
            .padding(vertical = 8.dp)
            .background(DividerSoft),
    )
}

/* ─────────────────────────────────────────────────────────────────────── *
 *  TABS
 * ─────────────────────────────────────────────────────────────────────── */

@Composable
private fun TabsRow(current: String, onSelect: (String) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        listOf("Overview", "Tasks", "Performance", "Docs").forEach { name ->
            val selected = current == name
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(40.dp)
                    .shadow(
                        if (selected) 10.dp else 0.dp,
                        RoundedCornerShape(50),
                        spotColor = if (selected) Brand.copy(alpha = 0.5f) else Color.Transparent,
                    )
                    .clip(RoundedCornerShape(50))
                    .background(
                        if (selected) Brush.horizontalGradient(listOf(Brand, BrandDeep))
                        else Brush.horizontalGradient(listOf(CardBg, CardBg)),
                    )
                    .clickable { onSelect(name) },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    name,
                    color = if (selected) Color.White else InkPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

/* ─────────────────────────────────────────────────────────────────────── *
 *  QUICK ACTIONS
 * ─────────────────────────────────────────────────────────────────────── */

@Composable
private fun QuickActions(
    onMessage: () -> Unit,
    onCall: () -> Unit,
    onLocate: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        QuickActionCard(
            icon = Icons.AutoMirrored.Filled.Message, label = "Message",
            tint = Brand, bg = Color(0xFFE6F0FE),
            onClick = onMessage, modifier = Modifier.weight(1f),
        )
        QuickActionCard(
            icon = Icons.Filled.Call, label = "Call",
            tint = Success, bg = Color(0xFFDCFCE7),
            onClick = onCall, modifier = Modifier.weight(1f),
        )
        QuickActionCard(
            icon = Icons.Filled.LocationOn, label = "Locate",
            tint = Danger, bg = Color(0xFFFEE2E2),
            onClick = onLocate, modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun QuickActionCard(
    icon: ImageVector,
    label: String,
    tint: Color,
    bg: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .height(96.dp)
            .shadow(10.dp, RoundedCornerShape(18.dp), spotColor = ShadowSoft)
            .clip(RoundedCornerShape(18.dp))
            .background(CardBg)
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(bg),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon, contentDescription = null,
                tint = tint, modifier = Modifier.size(20.dp),
            )
        }
        Text(label, color = InkPrimary, fontSize = 13.sp,
             fontWeight = FontWeight.SemiBold)
    }
}

/* ─────────────────────────────────────────────────────────────────────── *
 *  CONTACT CARD
 * ─────────────────────────────────────────────────────────────────────── */

@Composable
private fun ContactCard(employee: EmployeeRecord, onEdit: () -> Unit) {
    SectionCard(title = "Contact", trailingAction = "Edit", onTrailing = onEdit) {
        ContactRow(
            icon = Icons.Filled.Phone, tint = Brand,
            label = "PHONE", value = employee.phone.ifBlank { "—" },
        )
        DashedDivider()
        ContactRow(
            icon = Icons.Filled.Email, tint = Brand,
            label = "EMAIL", value = employee.email.ifBlank { "—" },
        )
        DashedDivider()
        ContactRow(
            icon = Icons.Filled.Home, tint = Brand,
            label = "ADDRESS", value = employee.address.ifBlank { "—" },
        )
    }
}

@Composable
private fun ContactRow(icon: ImageVector, tint: Color, label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFFE6F0FE)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon, contentDescription = null,
                tint = tint, modifier = Modifier.size(20.dp),
            )
        }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(label, color = InkSecondary, fontSize = 10.sp,
                 fontWeight = FontWeight.SemiBold, letterSpacing = 0.8.sp)
            Spacer(Modifier.height(2.dp))
            Text(value, color = InkPrimary, fontSize = 14.sp,
                 fontWeight = FontWeight.Bold,
                 maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        Icon(
            imageVector = Icons.Filled.ChevronRight,
            contentDescription = null,
            tint = InkMuted,
            modifier = Modifier.size(20.dp),
        )
    }
}

/* ─────────────────────────────────────────────────────────────────────── *
 *  JOINED ROW
 * ─────────────────────────────────────────────────────────────────────── */

@Composable
private fun JoinedRow(employee: EmployeeRecord) {
    SectionCard {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFE6F0FE)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.Event,
                    contentDescription = null,
                    tint = Brand,
                    modifier = Modifier.size(20.dp),
                )
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("JOINED", color = InkSecondary, fontSize = 10.sp,
                     fontWeight = FontWeight.SemiBold, letterSpacing = 0.8.sp)
                Spacer(Modifier.height(2.dp))
                Text(
                    formatJoinedLine(employee.joiningDateMs),
                    color = InkPrimary, fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

/* ─────────────────────────────────────────────────────────────────────── *
 *  PERFORMANCE CARD
 * ─────────────────────────────────────────────────────────────────────── */

private data class OverallPerformance(
    val overall: Int,
    val sla: Int,
    val quality: Int,
    val attendance: Int,
    val customer: Double,
)

private fun perfGrade(pct: Int): Pair<String, Color> = when {
    pct >= 85 -> "EXCELLENT" to Color(0xFF22C55E)
    pct >= 70 -> "GOOD"      to Color(0xFF3B82F6)
    pct >= 50 -> "AVERAGE"   to Color(0xFFF59E0B)
    else      -> "BELOW AVG" to Color(0xFFEF4444)
}

/* ─────────────────────────────────────────────────────────────────────── *
 *  PREMIUM PERFORMANCE HERO
 * ─────────────────────────────────────────────────────────────────────── */

@Composable
private fun PremiumPerformanceHero(performance: OverallPerformance) {
    val (gradeLabel, gradeColor) = perfGrade(performance.overall)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(20.dp, RoundedCornerShape(24.dp), spotColor = Brand.copy(alpha = 0.30f))
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.linearGradient(
                    listOf(Color(0xFF1E3A8A), Color(0xFF1D4ED8), Color(0xFF2563EB)),
                )
            )
            .padding(horizontal = 22.dp, vertical = 22.dp),
    ) {
        // Decorative background circles
        androidx.compose.foundation.Canvas(modifier = Modifier.matchParentSize()) {
            drawCircle(
                color  = Color.White.copy(alpha = 0.04f),
                radius = 180.dp.toPx(),
                center = androidx.compose.ui.geometry.Offset(size.width * 0.85f, -30f),
            )
            drawCircle(
                color  = Color.White.copy(alpha = 0.03f),
                radius = 90.dp.toPx(),
                center = androidx.compose.ui.geometry.Offset(size.width * 0.10f, size.height * 1.15f),
            )
        }

        Column {
            Row(
                modifier          = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Left — title + grade pill
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Performance Score",
                        color      = Color.White.copy(alpha = 0.82f),
                        fontSize   = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.4.sp,
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        "Last 30 days",
                        color    = Color.White.copy(alpha = 0.50f),
                        fontSize = 11.sp,
                    )
                    Spacer(Modifier.height(14.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(gradeColor.copy(alpha = 0.18f))
                            .border(1.dp, gradeColor.copy(alpha = 0.45f), RoundedCornerShape(999.dp))
                            .padding(horizontal = 14.dp, vertical = 5.dp),
                    ) {
                        Text(
                            gradeLabel,
                            color      = gradeColor,
                            fontSize   = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                        )
                    }
                }

                // Right — score ring
                Box(
                    modifier          = Modifier.size(130.dp),
                    contentAlignment  = Alignment.Center,
                ) {
                    GradientProgressRing(percent = performance.overall)
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "${performance.overall}",
                            color      = Color.White,
                            fontSize   = 38.sp,
                            fontWeight = FontWeight.Bold,
                            lineHeight = 38.sp,
                        )
                        Text(
                            "/ 100",
                            color    = Color.White.copy(alpha = 0.55f),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }
            }

            Spacer(Modifier.height(18.dp))
            Box(Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = 0.13f)))
            Spacer(Modifier.height(16.dp))

            // Bottom mini stats row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment     = Alignment.CenterVertically,
            ) {
                HeroMiniStat(label = "SLA",     value = "${performance.sla}%")
                Box(Modifier.width(1.dp).height(34.dp).background(Color.White.copy(alpha = 0.15f)))
                HeroMiniStat(label = "Quality", value = "${performance.quality}%")
                Box(Modifier.width(1.dp).height(34.dp).background(Color.White.copy(alpha = 0.15f)))
                HeroMiniStat(label = "Attend.", value = "${performance.attendance}%")
            }
        }
    }
}

@Composable
private fun HeroMiniStat(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(2.dp))
        Text(label, color = Color.White.copy(alpha = 0.55f), fontSize = 10.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun GradientProgressRing(percent: Int) {
    androidx.compose.foundation.Canvas(
        modifier = Modifier.size(130.dp).aspectRatio(1f),
    ) {
        val stroke = 12.dp.toPx()
        val sweep  = (percent.coerceIn(0, 100) / 100f) * 360f
        val inset  = stroke / 2f
        val arcSz  = Size(size.width - stroke, size.height - stroke)
        val tl     = androidx.compose.ui.geometry.Offset(inset, inset)
        // Track
        drawArc(
            color = Color.White.copy(alpha = 0.20f),
            startAngle = -90f, sweepAngle = 360f, useCenter = false,
            style   = Stroke(width = stroke, cap = StrokeCap.Round),
            size    = arcSz, topLeft = tl,
        )
        // Filled arc
        if (sweep > 0f) {
            drawArc(
                color = Color.White,
                startAngle = -90f, sweepAngle = sweep, useCenter = false,
                style   = Stroke(width = stroke, cap = StrokeCap.Round),
                size    = arcSz, topLeft = tl,
            )
        }
    }
}

/* ─────────────────────────────────────────────────────────────────────── *
 *  PREMIUM METRIC GRID  (2 × 2 coloured tiles)
 * ─────────────────────────────────────────────────────────────────────── */

@Composable
private fun PremiumMetricGrid(performance: OverallPerformance) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MetricTile(
                label    = "SLA Adherence",
                value    = performance.sla,
                icon     = Icons.Filled.Insights,
                gradient = listOf(Color(0xFF1E40AF), Color(0xFF3B82F6)),
                modifier = Modifier.weight(1f),
            )
            MetricTile(
                label    = "Task Quality",
                value    = performance.quality,
                icon     = Icons.AutoMirrored.Filled.Assignment,
                gradient = listOf(Color(0xFF065F46), Color(0xFF22C55E)),
                modifier = Modifier.weight(1f),
            )
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MetricTile(
                label    = "Attendance",
                value    = performance.attendance,
                icon     = Icons.Filled.Event,
                gradient = listOf(Color(0xFF92400E), Color(0xFFF59E0B)),
                modifier = Modifier.weight(1f),
            )
            MetricTile(
                label    = "Customer ★",
                value    = (performance.customer * 20).toInt().coerceIn(0, 100),
                icon     = Icons.Filled.Star,
                gradient = listOf(Color(0xFF4C1D95), Color(0xFF8B5CF6)),
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun MetricTile(
    label: String,
    value: Int,
    icon: ImageVector,
    gradient: List<Color>,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .height(140.dp)
            .shadow(12.dp, RoundedCornerShape(20.dp), spotColor = gradient.last().copy(alpha = 0.28f))
            .clip(RoundedCornerShape(20.dp))
            .background(Brush.linearGradient(gradient))
            .padding(16.dp),
    ) {
        // Decorative circle accent
        Box(
            modifier = Modifier
                .size(64.dp)
                .align(Alignment.TopEnd)
                .offset(x = 14.dp, y = (-14).dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.07f)),
        )
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            // Icon chip
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.White.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon, contentDescription = null,
                    tint = Color.White, modifier = Modifier.size(18.dp),
                )
            }
            Column {
                Text(
                    "$value%",
                    color      = Color.White,
                    fontSize   = 28.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 28.sp,
                )
                Spacer(Modifier.height(1.dp))
                Text(
                    label,
                    color    = Color.White.copy(alpha = 0.72f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                )
                Spacer(Modifier.height(8.dp))
                // Progress bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(Color.White.copy(alpha = 0.22f)),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(value.coerceIn(0, 100) / 100f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(999.dp))
                            .background(Color.White),
                    )
                }
            }
        }
    }
}

/* ─────────────────────────────────────────────────────────────────────── *
 *  PREMIUM TASK METRICS
 * ─────────────────────────────────────────────────────────────────────── */

@Composable
private fun PremiumTaskMetrics(total: Int, completed: Int, onTime: Int, active: Int) {
    SectionCard(title = "Task Breakdown") {
        if (total == 0) {
            Box(
                modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text("No tasks assigned", color = InkMuted, fontSize = 13.sp)
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // Summary badges
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    TaskStatBadge(value = total.toString(),     label = "Total",   color = InkPrimary, bg = Color(0xFFF1F5F9))
                    TaskStatBadge(value = completed.toString(), label = "Done",    color = Success,    bg = Color(0xFFDCFCE7))
                    TaskStatBadge(value = active.toString(),    label = "Active",  color = Warning,    bg = Color(0xFFFEF3C7))
                    TaskStatBadge(value = onTime.toString(),    label = "On-Time", color = Brand,      bg = Color(0xFFE6F0FE))
                }
                // Progress bars
                val completionPct = if (total > 0) (completed.toFloat() / total * 100).toInt() else 0
                PerformanceBar(label = "Completion Rate",   pct = completionPct, fillColor = Success)

                val onTimePct = if (completed > 0) (onTime.toFloat() / completed * 100).toInt() else 0
                PerformanceBar(label = "On-Time Delivery",  pct = onTimePct,     fillColor = Brand)

                val activePct = if (total > 0) (active.toFloat() / total * 100).toInt() else 0
                PerformanceBar(label = "In-Progress Share", pct = activePct,     fillColor = Warning)
            }
        }
    }
}

@Composable
private fun TaskStatBadge(value: String, label: String, color: Color, bg: Color) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(bg)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(value, color = color, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(2.dp))
        Text(label, color = color.copy(alpha = 0.65f), fontSize = 9.sp,
             fontWeight = FontWeight.SemiBold, letterSpacing = 0.4.sp)
    }
}

@Composable
private fun PerformanceBar(label: String, pct: Int, fillColor: Color) {
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                label,
                color      = InkSecondary,
                fontSize   = 12.sp,
                fontWeight = FontWeight.Medium,
                modifier   = Modifier.weight(1f),
            )
            Text("$pct%", color = fillColor, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(7.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(fillColor.copy(alpha = 0.12f)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(pct.coerceIn(0, 100) / 100f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(999.dp))
                    .background(Brush.horizontalGradient(listOf(fillColor, fillColor.copy(alpha = 0.65f)))),
            )
        }
    }
}

/* ─────────────────────────────────────────────────────────────────────── *
 *  PREMIUM ATTENDANCE LOG
 * ─────────────────────────────────────────────────────────────────────── */

@Composable
private fun PremiumAttendanceLog(records: List<AttendanceRecord>) {
    SectionCard(title = "Attendance Log · Last 14 days") {
        if (records.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text("No attendance records", color = InkMuted, fontSize = 13.sp)
            }
        } else {
            Column {
                records.forEachIndexed { i, record ->
                    PremiumAttendanceRow(record = record)
                    if (i < records.lastIndex) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .padding(start = 56.dp)
                                .background(DividerSoft),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PremiumAttendanceRow(record: AttendanceRecord) {
    val checkInTime = remember(record.checkInMs) {
        val local = Instant.fromEpochMilliseconds(record.checkInMs)
            .toLocalDateTime(TimeZone.currentSystemDefault())
        "${local.hour.toString().padStart(2, '0')}:${local.minute.toString().padStart(2, '0')}"
    }
    val checkOutTime = remember(record.checkOutMs) {
        record.checkOutMs?.let { ms ->
            val local = Instant.fromEpochMilliseconds(ms)
                .toLocalDateTime(TimeZone.currentSystemDefault())
            "${local.hour.toString().padStart(2, '0')}:${local.minute.toString().padStart(2, '0')}"
        }
    }
    val dateLabel = remember(record.dateMs) {
        val local = Instant.fromEpochMilliseconds(record.dateMs)
            .toLocalDateTime(TimeZone.currentSystemDefault())
        "${local.dayOfMonth} ${local.month.name.take(3).lowercase().replaceFirstChar { it.uppercase() }}"
    }
    val dayName = remember(record.dateMs) {
        val local = Instant.fromEpochMilliseconds(record.dateMs)
            .toLocalDateTime(TimeZone.currentSystemDefault())
        local.dayOfWeek.name.take(3).lowercase().replaceFirstChar { it.uppercase() }
    }
    val late   = record.checkInStatus == "LATE"
    val dotClr = if (late) Danger else Success
    val dotBg  = if (late) DangerBg else SuccessBg

    Row(
        modifier          = Modifier.fillMaxWidth().padding(vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Date circle
        Box(
            modifier = Modifier
                .size(44.dp)
                .shadow(4.dp, CircleShape, spotColor = dotClr.copy(alpha = 0.18f))
                .clip(CircleShape)
                .background(dotBg),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    dateLabel.substringBefore(" "),
                    color      = dotClr,
                    fontSize   = 14.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 14.sp,
                )
                Text(
                    dateLabel.substringAfter(" "),
                    color    = dotClr.copy(alpha = 0.70f),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.SemiBold,
                    lineHeight = 10.sp,
                )
            }
        }

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(dayName, color = InkPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(6.dp).clip(CircleShape).background(Success))
                Spacer(Modifier.width(4.dp))
                Text("In  $checkInTime", color = InkSecondary, fontSize = 11.sp)
                if (checkOutTime != null) {
                    Text("  ·  ", color = InkMuted, fontSize = 11.sp)
                    Box(Modifier.size(6.dp).clip(CircleShape).background(Danger.copy(alpha = 0.70f)))
                    Spacer(Modifier.width(4.dp))
                    Text("Out $checkOutTime", color = InkSecondary, fontSize = 11.sp)
                } else {
                    Text("  ·  On Duty", color = Success, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                }
            }
        }

        // Status pill
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(999.dp))
                .background(dotBg)
                .border(1.dp, dotClr.copy(alpha = 0.25f), RoundedCornerShape(999.dp))
                .padding(horizontal = 9.dp, vertical = 4.dp),
        ) {
            Text(
                if (late) "LATE" else "ON TIME",
                color      = dotClr,
                fontSize   = 9.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp,
            )
        }
    }
}


/* ─────────────────────────────────────────────────────────────────────── *
 *  ACTIVE ASSIGNMENTS
 * ─────────────────────────────────────────────────────────────────────── */

@Composable
private fun ActiveAssignmentsCard(tasks: List<TaskRecord>) {
    SectionCard(title = "Active assignments", trailingAction = "All →") {
        if (tasks.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text("No active assignments", color = InkMuted, fontSize = 13.sp)
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                tasks.forEachIndexed { i, t ->
                    AssignmentRow(task = t)
                    if (i != tasks.lastIndex) DashedDivider()
                }
            }
        }
    }
}

@Composable
private fun AssignmentRow(task: TaskRecord) {
    val accent = priorityAccentColor(task.priority)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .padding(vertical = 6.dp),
    ) {
        Box(
            modifier = Modifier
                .width(4.dp)
                .fillMaxHeight()
                .clip(RoundedCornerShape(50))
                .background(accent),
        )
        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (task.priority.equals("Danger", true)) {
                    Icon(
                        imageVector = Icons.Filled.Warning,
                        contentDescription = null,
                        tint = Danger,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.width(4.dp))
                }
                Text(
                    task.title,
                    color = InkPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.height(2.dp))
            Text(
                taskMetaLine(task),
                color = InkSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
            )
        }

        Spacer(Modifier.width(8.dp))

        Column(horizontalAlignment = Alignment.End) {
            Text(
                task.time.ifBlank { "—" },
                color = InkPrimary,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(6.dp))
            PriorityPill(label = priorityShortLabel(task.priority), tint = accent)
        }
    }
}

@Composable
private fun PriorityPill(label: String, tint: Color) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(tint.copy(alpha = 0.12f))
            .padding(horizontal = 8.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(5.dp).clip(CircleShape).background(tint),
        )
        Spacer(Modifier.width(5.dp))
        Text(label, color = tint, fontSize = 10.sp,
             fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
    }
}

/* ─────────────────────────────────────────────────────────────────────── *
 *  BOTTOM ACTIONS
 * ─────────────────────────────────────────────────────────────────────── */

@Composable
private fun BottomAction(
    icon: ImageVector,
    label: String,
    tint: Color,
    bg: Color,
    modifier: Modifier = Modifier,
    labelColor: Color = InkPrimary,
    onClick: () -> Unit = {},
) {
    Column(
        modifier = modifier
            .height(78.dp)
            .shadow(8.dp, RoundedCornerShape(16.dp), spotColor = ShadowSoft)
            .clip(RoundedCornerShape(16.dp))
            .background(CardBg)
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(bg),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon, contentDescription = null,
                tint = tint, modifier = Modifier.size(18.dp),
            )
        }
        Text(label, color = labelColor, fontSize = 12.sp,
             fontWeight = FontWeight.SemiBold)
    }
}

/* ─────────────────────────────────────────────────────────────────────── *
 *  SECTION CARD WRAPPER
 * ─────────────────────────────────────────────────────────────────────── */

@Composable
private fun SectionCard(
    title: String? = null,
    trailingAction: String? = null,
    onTrailing: () -> Unit = {},
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(10.dp, RoundedCornerShape(20.dp), spotColor = ShadowSoft)
            .clip(RoundedCornerShape(20.dp))
            .background(CardBg)
            .padding(horizontal = 18.dp, vertical = 16.dp),
    ) {
        if (title != null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .height(16.dp).width(3.dp)
                        .clip(RoundedCornerShape(50))
                        .background(Brand),
                )
                Spacer(Modifier.width(8.dp))
                Text(title, color = InkPrimary, fontSize = 16.sp,
                     fontWeight = FontWeight.Bold,
                     modifier = Modifier.weight(1f))
                if (trailingAction != null) {
                    Text(
                        trailingAction,
                        color = Brand,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.clickable(onClick = onTrailing),
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
        }
        content()
    }
}

/* ─────────────────────────────────────────────────────────────────────── *
 *  HELPERS
 * ─────────────────────────────────────────────────────────────────────── */

@Composable
private fun DashedDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .drawBehind {
                drawRoundRect(
                    color = DividerSoft,
                    style = Stroke(
                        width = 1.dp.toPx(),
                        pathEffect = androidx.compose.ui.graphics.PathEffect
                            .dashPathEffect(floatArrayOf(6f, 6f), 0f),
                    ),
                )
            },
    )
}

private fun shortEmpCode(id: String): String {
    val n = (id.hashCode().absoluteValue % 9000) + 1000
    return "EMP-$n"
}

private fun avatarGradientFor(seed: String): List<Color> {
    val palettes = listOf(
        listOf(Color(0xFFFFB28A), Color(0xFFEC8552)),  // peach (matches mock)
        listOf(Color(0xFF60A5FA), Color(0xFF1D4ED8)),  // blue
        listOf(Color(0xFFA78BFA), Color(0xFF6D28D9)),  // purple
        listOf(Color(0xFF34D399), Color(0xFF047857)),  // green
        listOf(Color(0xFFF472B6), Color(0xFFBE185D)),  // pink
    )
    val idx = (seed.hashCode().absoluteValue) % palettes.size
    return palettes[idx]
}

private fun priorityAccentColor(raw: String): Color = when (raw.lowercase()) {
    "high"   -> Danger
    "medium" -> Warning
    "low"    -> Success
    else     -> Brand
}

private fun priorityShortLabel(raw: String): String = when (raw.lowercase()) {
    "high"   -> "HIGH"
    "medium" -> "MED"
    "low"    -> "LOW"
    else     -> raw.uppercase()
}

private fun taskMetaLine(task: TaskRecord): String {
    val code = "#TASK-${(task.id.hashCode().absoluteValue % 9000) + 1000}"
    val statusLabel = when (task.status) {
        "InProgress" -> "IN PROGRESS"
        "Done"       -> "COMPLETED"
        else         -> "QUEUED"
    }
    val parts = mutableListOf(code, statusLabel)
    if (task.day.isNotBlank() && !task.day.equals("Today", true)) parts += task.day.uppercase()
    return parts.joinToString(" · ")
}

private fun formatJoinedLine(joinedMs: Long?): String {
    if (joinedMs == null) return "—"
    val ldt = Instant.fromEpochMilliseconds(joinedMs)
        .toLocalDateTime(TimeZone.UTC)
    val month = listOf("Jan","Feb","Mar","Apr","May","Jun",
                       "Jul","Aug","Sep","Oct","Nov","Dec")[ldt.monthNumber - 1]
    val date = "${ldt.dayOfMonth} $month ${ldt.year}"
    val years = yearsSince(joinedMs)
    return "$date  •  $years"
}

private fun yearsSince(epochMs: Long): String {
    val now = nowEpochMillis()
    val diff = (now - epochMs).coerceAtLeast(0)
    val years = diff / (365L * 24 * 60 * 60 * 1000)
    val months = (diff / (30L * 24 * 60 * 60 * 1000)) % 12
    val tenths = (months * 10 / 12)
    return if (years > 0) "$years.$tenths years" else "$months months"
}
