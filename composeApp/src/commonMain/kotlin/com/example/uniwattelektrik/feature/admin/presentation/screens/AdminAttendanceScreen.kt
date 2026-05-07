package com.example.uniwattelektrik.feature.admin.presentation.screens

import com.example.uniwattelektrik.core.performance.TrackScreenPerformance

import com.example.uniwattelektrik.core.theme.AppTheme
import com.example.uniwattelektrik.core.theme.AppShapes
import com.example.uniwattelektrik.core.theme.appScreenBackground

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Watch
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.uniwattelektrik.core.components.AppPullToRefresh
import com.example.uniwattelektrik.feature.workforce.data.remote.AttendanceRecord
import com.example.uniwattelektrik.feature.workforce.presentation.WorkforceViewModel
import com.example.uniwattelektrik.platform.nowEpochMillis
import kotlin.time.ExperimentalTime
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.minus
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
private val Highlight    = AppTheme.Brand
private val GradStart    = AppTheme.Brand
private val GradMid      = AppTheme.Brand700
private val GradEnd      = AppTheme.Navy
private val WhiteAlpha20 = Color(0x33FFFFFF)
private val WhiteAlpha70 = Color(0xB3FFFFFF)
private val MapBg        = AppTheme.Brand50
private val MapGrid      = AppTheme.Ink100
private val MapRoad      = AppTheme.Surface


/* ─── Local design tokens (keep AppTheme stable) ────────────────────────── */

/* Map palette */

/**
 * Premium date-wise attendance + GPS screen.
 *
 *  ┌────────────────────────────────────────┐
 *  │ Gradient header — title + LIVE count   │
 *  ├────────────────────────────────────────┤
 *  │ Date selector pills (Today/Yesterday/…)│
 *  ├────────────────────────────────────────┤
 *  │ 3 stat tiles (On-time / Late / Absent) │
 *  ├────────────────────────────────────────┤
 *  │ GPS map — pins from selected date      │
 *  ├────────────────────────────────────────┤
 *  │ Attendance log — check-in & check-out  │
 *  │ with shift duration + LATE indicator   │
 *  └────────────────────────────────────────┘
 */
@OptIn(ExperimentalTime::class)
@Composable
fun AdminAttendanceScreen(
    workforceVm: WorkforceViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TrackScreenPerformance("AdminAttendanceScreen")
    val employees  by workforceVm.employees.collectAsStateWithLifecycle()
    val attendance by workforceVm.attendance.collectAsStateWithLifecycle()
    val checkins   by workforceVm.checkins.collectAsStateWithLifecycle()

    com.example.uniwattelektrik.core.theme.SetStatusBar(color = GradStart, darkIcons = false)

    val tz    = TimeZone.currentSystemDefault()
    val today = remember {
        Instant.fromEpochMilliseconds(nowEpochMillis())
            .toLocalDateTime(tz).date
    }
    // Last 7 days: index 0 = today, 1 = yesterday, …
    val dates = remember(today) { (0..6).map { d -> today.minus(d, DateTimeUnit.DAY) } }
    var selectedIdx by remember { mutableStateOf(0) }

    val selectedDate    = dates[selectedIdx]
    val selectedStartMs = remember(selectedDate) {
        selectedDate.atStartOfDayIn(tz).toEpochMilliseconds()
    }
    val selectedEndMs   = selectedStartMs + 86_400_000L   // +24 h
    val shiftStartMs    = selectedStartMs + 9L * 60 * 60 * 1000L // 09:00

    // ── Filter data for selected date ─────────────────────────────────────────
    val dayAttendance = remember(attendance, selectedStartMs, selectedEndMs) {
        attendance
            .filter { it.checkInMs in selectedStartMs until selectedEndMs }
            .sortedByDescending { it.checkInMs }
    }

    val onTimeCt = dayAttendance.count { it.checkInMs in 1L until shiftStartMs + 1 }
    val lateCt   = dayAttendance.count { it.checkInMs > shiftStartMs }
    val absentCt = (employees.size - dayAttendance.size).coerceAtLeast(0)

    // ── Today's live count for header ─────────────────────────────────────────
    val todayStartMs = remember(today) { today.atStartOfDayIn(tz).toEpochMilliseconds() }
    val todayOnField = remember(attendance, todayStartMs) {
        attendance.count { it.checkInMs in todayStartMs until todayStartMs + 86_400_000L }
    }

    // ── GPS markers for selected date ─────────────────────────────────────────
    val dayCheckins = remember(checkins, selectedStartMs, selectedEndMs) {
        checkins.filter { it.timestampMs in selectedStartMs until selectedEndMs }
    }
    val markers = remember(dayCheckins, employees) {
        dayCheckins
            .sortedByDescending { it.timestampMs }
            .associateBy { it.userId }
            .values
            .map { ping ->
                val emp = employees.firstOrNull { it.id == ping.userId }
                com.example.uniwattelektrik.feature.admin.presentation.components.MapMarker(
                    id        = ping.id,
                    title     = emp?.name ?: "Employee",
                    snippet   = emp?.role ?: "GPS ping",
                    latitude  = ping.latitude,
                    longitude = ping.longitude,
                )
            }
    }

    AppPullToRefresh(onRefresh = { workforceVm.refresh() }) {
    LazyColumn(
        modifier = modifier.fillMaxSize().background(appScreenBackground()),
        contentPadding = PaddingValues(bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp),
    ) {

        /* ── 1. Gradient header ───────────────────────────────────────────── */
        item {
            com.example.uniwattelektrik.core.components.PremiumHeaderBackground(roundedBottom = false) {
                Column(
                    modifier = Modifier
                        .windowInsetsPadding(WindowInsets.statusBars)
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Attendance & GPS",
                                color = Color.White, fontSize = 20.sp,
                                fontWeight = FontWeight.Bold, letterSpacing = (-0.2).sp,
                            )
                            Text(
                                "LIVE  ·  $todayOnField ON FIELD TODAY",
                                color = WhiteAlpha70, fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold, letterSpacing = 1.6.sp,
                            )
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(WhiteAlpha20)
                                .padding(horizontal = 10.dp, vertical = 5.dp),
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Box(modifier = Modifier.size(7.dp).clip(CircleShape).background(Success))
                                Text("LIVE", color = Color.White, fontSize = 10.sp,
                                     fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
                            }
                        }
                    }
                }
            }
        }

        /* ── 2. Date selector ────────────────────────────────────────────── */
        item {
            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                dates.forEachIndexed { idx, date ->
                    val label = when (idx) {
                        0    -> "Today"
                        1    -> "Yesterday"
                        else -> "${dayAbbrev(date.dayOfWeek.name)} ${date.dayOfMonth}"
                    }
                    val isSelected = idx == selectedIdx
                    Box(
                        modifier = Modifier
                            .shadow(
                                if (isSelected) 8.dp else 2.dp,
                                RoundedCornerShape(50),
                                spotColor = if (isSelected) Brand.copy(alpha = 0.30f) else ShadowSoft,
                            )
                            .clip(RoundedCornerShape(50))
                            .background(if (isSelected) Brand else CardBg)
                            .then(if (!isSelected) Modifier.border(1.dp, DividerSoft, RoundedCornerShape(50)) else Modifier)
                            .clickable { selectedIdx = idx }
                            .padding(horizontal = 18.dp, vertical = 10.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            label,
                            color = if (isSelected) Color.White else InkSecondary,
                            fontSize = 13.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        )
                    }
                }
            }
        }

        /* ── 3. Stats for selected date ──────────────────────────────────── */
        item {
            Row(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .padding(top = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                StatTile(Icons.Filled.Check,  Success, onTimeCt.toString(), "On-time", Modifier.weight(1f))
                StatTile(Icons.Filled.Watch,  Warning, lateCt.toString(),   "Late",    Modifier.weight(1f))
                StatTile(Icons.Filled.Close,  Danger,  absentCt.toString(), "Absent",  Modifier.weight(1f))
            }
        }

        /* ── 4. GPS map for selected date ────────────────────────────────── */
        item {
            val mapShape    = RoundedCornerShape(22.dp)
            val borderBrush = Brush.linearGradient(listOf(GradStart, GradMid, GradEnd))
            Box(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .padding(top = 14.dp, bottom = 4.dp)
                    .fillMaxWidth()
                    .height(240.dp)
                    .shadow(12.dp, mapShape, spotColor = ShadowSoft)
                    .clip(mapShape)
                    .background(borderBrush)
                    .padding(1.5.dp)
                    .clip(RoundedCornerShape(20.5.dp))
                    .background(MapBg),
            ) {
                com.example.uniwattelektrik.feature.admin.presentation.components.AttendanceMap(
                    markers  = markers,
                    modifier = Modifier.fillMaxSize(),
                )
                if (markers.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0x88F8FAFC)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Text("📍", fontSize = 28.sp)
                            Text(
                                "No GPS pings for this date",
                                color = InkSecondary, fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                            )
                        }
                    }
                }
            }
        }

        /* ── 5. Attendance log header ─────────────────────────────────────── */
        item {
            Row(
                modifier = Modifier
                    .padding(horizontal = 20.dp)
                    .padding(top = 18.dp, bottom = 8.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "Attendance Log",
                    color = InkPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(Brand50)
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                ) {
                    Text(
                        "${dayAttendance.size} records",
                        color = Brand, fontSize = 11.sp, fontWeight = FontWeight.Bold,
                    )
                }
            }
        }

        /* ── 6. Log cards ───────────────────────────────────────────────── */
        item {
            Box(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .fillMaxWidth()
                    .shadow(10.dp, RoundedCornerShape(22.dp), spotColor = ShadowSoft)
                    .clip(RoundedCornerShape(22.dp))
                    .background(CardBg),
            ) {
                Column {
                    if (dayAttendance.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(32.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Text("📋", fontSize = 32.sp)
                                Text(
                                    "No attendance records for this date",
                                    color = InkSecondary, fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                )
                            }
                        }
                    } else {
                        dayAttendance.forEachIndexed { idx, record ->
                            val emp = employees.firstOrNull { it.id == record.userId }
                            AttendanceLogRow(
                                record       = record,
                                employeeName = emp?.name ?: record.userId.take(8),
                                employeeRole = emp?.role ?: "—",
                                shiftStartMs = shiftStartMs,
                            )
                            if (idx < dayAttendance.lastIndex) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth().height(1.dp)
                                        .padding(start = 76.dp)
                                        .background(DividerSoft),
                                )
                            }
                        }
                    }
                }
            }
        }
    } // LazyColumn
    } // AppPullToRefresh
}

/* ────────────────────────────────────────────────────────────────────────
 *  STAT TILE
 * ──────────────────────────────────────────────────────────────────────── */

@Composable
private fun StatTile(
    icon: ImageVector,
    iconBg: Color,
    value: String,
    label: String,
    modifier: Modifier = Modifier,
) {
    // Compact rectangular tile: small icon on the left, number + label stacked
    // on the right. Three of these sit cleanly side-by-side without looking
    // like stretched squares.
    Row(
        modifier = modifier
            .shadow(10.dp, RoundedCornerShape(16.dp), spotColor = ShadowSoft)
            .clip(RoundedCornerShape(16.dp))
            .background(CardBg)
            .padding(horizontal = 10.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(iconBg),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector        = icon,
                contentDescription = null,
                tint               = Color.White,
                modifier           = Modifier.size(16.dp),
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                value,
                color = InkPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.3).sp,
                maxLines = 1,
            )
            Text(
                label,
                color = InkSecondary,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
            )
        }
    }
}

/* ────────────────────────────────────────────────────────────────────────
 *  PREMIUM ATTENDANCE LOG ROW
 * ──────────────────────────────────────────────────────────────────────── */

@Composable
private fun AttendanceLogRow(
    record: AttendanceRecord,
    employeeName: String,
    employeeRole: String,
    shiftStartMs: Long,
) {
    val isLate      = record.checkInMs > shiftStartMs
    val statusColor = if (isLate) Warning else Success
    val statusBg    = if (isLate) WarningBg else SuccessBg
    val statusLabel = if (isLate) "LATE" else "ON-TIME"
    val lateMins    = if (isLate) ((record.checkInMs - shiftStartMs) / 60_000L).coerceAtLeast(0L) else 0L

    val checkInTime  = formatHm(record.checkInMs)
    val checkOutMs   = record.checkOutMs
    val checkOutTime = if (checkOutMs != null && checkOutMs > 0L) formatHm(checkOutMs) else null
    val shiftDuration = if (checkOutMs != null && checkOutMs > record.checkInMs) {
        val totalMin = ((checkOutMs - record.checkInMs) / 60_000L).coerceAtLeast(0L)
        val h = totalMin / 60
        val m = totalMin % 60
        if (h > 0) "${h}h ${m}m" else "${m}m"
    } else null

    val initials = employeeName
        .split(" ")
        .take(2)
        .mapNotNull { it.firstOrNull()?.uppercaseChar() }
        .joinToString("")
        .ifEmpty { employeeName.take(2).uppercase() }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Avatar
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(statusBg),
            contentAlignment = Alignment.Center,
        ) {
            Text(initials, color = statusColor, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.width(14.dp))

        // Name + timing
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(employeeName, color = InkPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            // Check-in / check-out row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(Success))
                Text(checkInTime, color = InkSecondary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                if (checkOutTime != null) {
                    Text("→", color = InkMuted, fontSize = 11.sp)
                    Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(Danger))
                    Text(checkOutTime, color = InkSecondary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                } else {
                    Text("·  on duty", color = Success, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                }
            }
            // Shift duration
            if (shiftDuration != null) {
                Text(
                    "Shift: $shiftDuration",
                    color = InkMuted, fontSize = 11.sp, fontWeight = FontWeight.Medium,
                )
            }
        }

        Spacer(Modifier.width(8.dp))

        // Status pill + late indicator
        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(statusBg)
                    .padding(horizontal = 9.dp, vertical = 4.dp),
            ) {
                Text(
                    statusLabel, color = statusColor,
                    fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.6.sp,
                )
            }
            if (isLate && lateMins > 0) {
                Text("+${lateMins}m late", color = Warning, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

/* ────────────────────────────────────────────────────────────────────────
 *  HELPERS
 * ──────────────────────────────────────────────────────────────────────── */

private fun formatHm(epochMs: Long): String {
    val ldt = Instant.fromEpochMilliseconds(epochMs)
        .toLocalDateTime(TimeZone.currentSystemDefault())
    val hh = ldt.hour.toString().padStart(2, '0')
    val mm = ldt.minute.toString().padStart(2, '0')
    return "$hh:$mm"
}

private fun dayAbbrev(name: String): String = when (name.uppercase()) {
    "MONDAY"    -> "Mon"
    "TUESDAY"   -> "Tue"
    "WEDNESDAY" -> "Wed"
    "THURSDAY"  -> "Thu"
    "FRIDAY"    -> "Fri"
    "SATURDAY"  -> "Sat"
    "SUNDAY"    -> "Sun"
    else        -> name.take(3)
}

