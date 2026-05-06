package com.example.uniwattelektrik.feature.admin.presentation.screens

import com.example.uniwattelektrik.core.theme.appScreenBackground

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Watch
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.layout
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.uniwattelektrik.feature.workforce.data.remote.AttendanceRecord
import com.example.uniwattelektrik.feature.workforce.data.remote.EmployeeRecord
import com.example.uniwattelektrik.feature.workforce.presentation.WorkforceViewModel
import kotlin.math.absoluteValue
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/* ─── Local design tokens (keep AppTheme stable) ────────────────────────── */
private val GradStart    = Color(0xFF3B82F6)
private val GradMid      = Color(0xFF1D4ED8)
private val GradEnd      = Color(0xFF0F172A)
private val ScreenBg     = Color(0xFFF5F7FB)
private val CardBg       = Color(0xFFFFFFFF)
private val InkPrimary   = Color(0xFF0F172A)
private val InkSecondary = Color(0xFF64748B)
private val InkMuted     = Color(0xFF94A3B8)
private val Highlight    = Color(0xFF2979FF)
private val Success      = Color(0xFF22C55E)
private val SuccessBg    = Color(0xFFE6F9F0)
private val Warning      = Color(0xFFF59E0B)
private val WarningBg    = Color(0xFFFEF3E2)
private val Danger       = Color(0xFFEF4444)
private val DangerBg     = Color(0xFFFEE2E2)
private val ShadowSoft   = Color(0x14172C50)
private val WhiteAlpha20 = Color(0x33FFFFFF)
private val WhiteAlpha70 = Color(0xB3FFFFFF)

/* Map palette */
private val MapBg        = Color(0xFFDDE7F5)
private val MapGrid      = Color(0xFFC9D5E8)
private val MapRoad      = Color(0xFFFFFFFF)

/**
 * Live attendance + GPS overview screen.
 *
 *  ┌───────────────────────────────────┐
 *  │ Gradient header — title + LIVE    │
 *  ├───────────────────────────────────┤
 *  │ 3 stat cards (Checked-in/Late/    │
 *  │ Absent), spaced clear of header   │
 *  ├───────────────────────────────────┤
 *  │ Map view with pins + filter chips │
 *  ├───────────────────────────────────┤
 *  │ Recent check-ins list             │
 *  └───────────────────────────────────┘
 *
 * Stats and pins are derived deterministically from the live `employees` list
 * (hash-bucketed) so the screen behaves identically across reloads. When real
 * check-in data is wired into [WorkforceViewModel], swap the `employees.map`
 * sites for direct field reads — the layout and components stay the same.
 */
@Composable
fun AdminAttendanceScreen(
    workforceVm: WorkforceViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val employees   by workforceVm.employees.collectAsStateWithLifecycle()
    val attendance  by workforceVm.attendance.collectAsStateWithLifecycle()
    val checkins    by workforceVm.checkins.collectAsStateWithLifecycle()

    com.example.uniwattelektrik.core.theme.SetStatusBar(color = GradStart, darkIcons = false)

    /* Build today's attendance map: userId -> latest AttendanceRecord (today only). */
    val todayStartMs = remember(attendance) { startOfTodayUtcMs() }
    val attendanceByUser: Map<String, AttendanceRecord> =
        remember(attendance, todayStartMs) {
            attendance
                .asSequence()
                .filter { it.dateMs >= todayStartMs || it.checkInMs >= todayStartMs }
                .sortedByDescending { it.checkInMs }
                .associateBy { it.userId }   // last (most recent) wins for each user
        }

    /* Bucket each employee by REAL data:
       - CheckedIn: has an attendance row today AND check-in is on time (≤ shiftStartMs)
       - Late:      has an attendance row today AND check-in after shiftStartMs
       - Absent:    no attendance row today                                            */
    val shiftStartMs = remember(todayStartMs) { todayStartMs + 9 * 60 * 60 * 1000L /* 09:00 local UTC */ }
    fun bucket(e: EmployeeRecord): AttendanceStatus {
        val rec = attendanceByUser[e.id] ?: return AttendanceStatus.Absent
        return if (rec.checkInMs > 0L && rec.checkInMs > shiftStartMs)
            AttendanceStatus.Late
        else
            AttendanceStatus.CheckedIn
    }
    val records = employees.map { it to bucket(it) }
    val checkedIn = records.count { it.second == AttendanceStatus.CheckedIn }
    val late      = records.count { it.second == AttendanceStatus.Late }
    val absent    = records.count { it.second == AttendanceStatus.Absent }
    val onField   = checkedIn + late          // anyone who clocked in today

    /* Pin task buckets — for the filter chips above the map.
       Still hash-based (we don't track task zones yet) but only includes users
       who actually checked in today, so the map reflects reality. */
    fun zoneOf(e: EmployeeRecord): MapZone {
        val h = (e.id.hashCode().absoluteValue / 7) % 100
        return when {
            h < 55 -> MapZone.OnTask
            h < 85 -> MapZone.Transit
            else   -> MapZone.Alert
        }
    }
    val onFieldRecords = records.filter { it.second != AttendanceStatus.Absent }
    val pins = onFieldRecords.take(8).map { (e, _) ->
        MapPin(
            initials = e.name.take(2).uppercase().ifBlank { "??" },
            zone     = zoneOf(e),
        )
    }
    val onTaskCt  = pins.count { it.zone == MapZone.OnTask }
    val transitCt = pins.count { it.zone == MapZone.Transit }
    val alertCt   = pins.count { it.zone == MapZone.Alert }

    LazyColumn(
        modifier = modifier.fillMaxSize().background(appScreenBackground()),
        contentPadding = PaddingValues(bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp),
    ) {

        /* ── 1. Gradient header (no overlap with stats below) ───────────── */
        item {
            com.example.uniwattelektrik.core.components.PremiumHeaderBackground(
                roundedBottom = false,
            ) {
                Column(
                    modifier = Modifier
                        .windowInsetsPadding(WindowInsets.statusBars)
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {

                        // Back button hidden — Attendance & GPS is a top-level tab.

                        // Titles
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Attendance & GPS",
                                color    = Color.White,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = (-0.2).sp,
                            )
                            Text(
                                "LIVE  ·  $onField ON FIELD",
                                color = WhiteAlpha70,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = 1.6.sp,
                            )
                        }

                        // LIVE pill (top-right)
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
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .clip(CircleShape)
                                        .background(Success),
                                )
                                Text(
                                    "LIVE",
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.2.sp,
                                )
                            }
                        }
                    }
                }
            }
        }

        /* ── 2. Stat cards — rectangular trio, sits cleanly BELOW header ── */
        item {
            Row(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .padding(top = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                StatTile(
                    modifier  = Modifier.weight(1f),
                    icon      = Icons.Filled.Check,
                    iconBg    = Success,
                    value     = checkedIn.toString(),
                    label     = "Checked-in",
                )
                StatTile(
                    modifier  = Modifier.weight(1f),
                    icon      = Icons.Filled.Watch,
                    iconBg    = Warning,
                    value     = late.toString(),
                    label     = "Late",
                )
                StatTile(
                    modifier  = Modifier.weight(1f),
                    icon      = Icons.Filled.Close,
                    iconBg    = Danger,
                    value     = absent.toString(),
                    label     = "Absent",
                )
            }
        }

        /* ── 3. Live map with real GPS pings ─────────────────────────────── */
        item {
            // Latest ping per user → one marker per employee
            val latestByUser = remember(checkins) {
                checkins
                    .sortedByDescending { it.timestampMs }
                    .associateBy { it.userId }
            }
            val markers = remember(latestByUser, employees) {
                latestByUser.values.map { ping ->
                    val emp = employees.firstOrNull { it.id == ping.userId }
                    com.example.uniwattelektrik.feature.admin.presentation.components.MapMarker(
                        id = ping.id,
                        title = emp?.name ?: "Employee",
                        snippet = emp?.role ?: "GPS ping",
                        latitude = ping.latitude,
                        longitude = ping.longitude,
                    )
                }
            }
            // 1.5dp gradient border (header palette) around the map.
            val mapShape = RoundedCornerShape(22.dp)
            val borderBrush = Brush.linearGradient(listOf(GradStart, GradMid, GradEnd))
            Box(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .padding(top = 14.dp, bottom = 4.dp)
                    .fillMaxWidth()
                    .height(280.dp)
                    .shadow(12.dp, mapShape, spotColor = ShadowSoft)
                    .clip(mapShape)
                    .background(borderBrush)        // outer = gradient border
                    .padding(1.5.dp)                // border thickness
                    .clip(RoundedCornerShape(20.5.dp))
                    .background(MapBg),
            ) {
                com.example.uniwattelektrik.feature.admin.presentation.components.AttendanceMap(
                    markers = markers,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }

        /* ── 4. Recent check-ins ────────────────────────────────────────── */
        item {
            Row(
                modifier = Modifier
                    .padding(horizontal = 20.dp, vertical = 8.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "Recent check-ins",
                    color = InkPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(SuccessBg)
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Box(modifier = Modifier
                            .size(7.dp).clip(CircleShape).background(Success))
                        Text("LIVE", color = Success, fontSize = 10.sp,
                             fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
                    }
                }
            }
        }

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
                    val rows = records.filter { it.second != AttendanceStatus.Absent }.take(5)
                    rows.forEachIndexed { idx, (employee, status) ->
                        CheckInRow(
                            employee = employee,
                            status   = status,
                            record   = attendanceByUser[employee.id],
                            shiftStartMs = shiftStartMs,
                        )
                        if (idx < rows.lastIndex) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(1.dp)
                                    .padding(start = 76.dp)
                                    .background(Color(0xFFEEF2F7)),
                            )
                        }
                    }
                    if (rows.isEmpty()) {
                        Text(
                            "No check-ins yet today.",
                            color = InkSecondary,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(20.dp),
                        )
                    }
                }
            }
        }
    }
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
 *  MAP — background grid + roads + chips + pins
 * ──────────────────────────────────────────────────────────────────────── */

@Composable
private fun MapBackground() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        // Grid (every 32 px)
        val step = 32f
        var x = 0f
        while (x < w) {
            drawLine(MapGrid, Offset(x, 0f), Offset(x, h), strokeWidth = 1f)
            x += step
        }
        var y = 0f
        while (y < h) {
            drawLine(MapGrid, Offset(0f, y), Offset(w, y), strokeWidth = 1f)
            y += step
        }
        // Two intersecting "roads"
        drawLine(MapRoad, Offset(0f, h * 0.55f), Offset(w, h * 0.42f),
                 strokeWidth = 14f)
        drawLine(MapRoad, Offset(w * 0.45f, 0f), Offset(w * 0.62f, h),
                 strokeWidth = 12f)
    }
}

@Composable
private fun MapChip(label: String, count: Int, dot: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .shadow(6.dp, RoundedCornerShape(50), spotColor = ShadowSoft)
            .clip(RoundedCornerShape(50))
            .background(Color.White)
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Box(modifier = Modifier.size(7.dp).clip(CircleShape).background(dot))
            Text(label, color = InkPrimary, fontSize = 12.sp,
                 fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.weight(1f))
            Text("$count", color = InkSecondary, fontSize = 12.sp,
                 fontWeight = FontWeight.Bold)
        }
    }
}

private enum class MapZone(val color: Color) {
    OnTask  (Success),
    Transit (Highlight),
    Alert   (Danger),
}

private data class MapPin(val initials: String, val zone: MapZone)

/** Tear-drop pin shape — pointed at the bottom centre. */
private val PinShape = GenericShape { size: Size, _ ->
    val w = size.width
    val h = size.height
    val r = w / 2f
    moveTo(r, h)                      // bottom point
    cubicTo(r * 0.0f, h * 0.78f, 0f, h * 0.45f, 0f, r) // left curve
    arcTo(
        rect = androidx.compose.ui.geometry.Rect(0f, 0f, w, w),
        startAngleDegrees = 180f,
        sweepAngleDegrees = 180f,
        forceMoveTo = false,
    )
    cubicTo(w, h * 0.45f, w * 1.0f, h * 0.78f, r, h)
    close()
    fillType = PathFillType.NonZero
}

@Composable
private fun MapPin(pin: MapPin, modifier: Modifier = Modifier) {
    Box(modifier = modifier) {
        // Soft drop shadow blob
        Box(
            modifier = Modifier
                .size(width = 30.dp, height = 8.dp)
                .align(Alignment.BottomCenter)
                .offset(y = 4.dp)
                .clip(CircleShape)
                .background(Color(0x33000000)),
        )
        // Pin tear-drop
        Box(
            modifier = Modifier
                .size(width = 36.dp, height = 48.dp)
                .clip(PinShape)
                .background(pin.zone.color),
            contentAlignment = Alignment.TopCenter,
        ) {
            // White inner circle with initials
            Box(
                modifier = Modifier
                    .padding(top = 4.dp)
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(Color.White),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    pin.initials,
                    color = pin.zone.color,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

/** Deterministic pin placement (% of map width / height). */
private fun pinAnchor(idx: Int): Pair<Float, Float> = when (idx) {
    0 -> 0.30f to 0.45f
    1 -> 0.55f to 0.55f
    2 -> 0.18f to 0.65f
    3 -> 0.62f to 0.40f
    4 -> 0.38f to 0.72f
    5 -> 0.72f to 0.62f
    6 -> 0.48f to 0.30f
    else -> 0.78f to 0.78f
}

private fun Modifier.pinOffset(xPct: Float, yPct: Float): Modifier =
    layout { measurable, constraints ->
        val placeable = measurable.measure(constraints)
        val w = constraints.maxWidth
        val h = constraints.maxHeight
        layout(placeable.width, placeable.height) {
            val xPx = (w * xPct).toInt() - placeable.width / 2
            val yPx = (h * yPct).toInt() - placeable.height / 2
            placeable.placeRelative(xPx, yPx)
        }
    }

/* ────────────────────────────────────────────────────────────────────────
 *  RECENT CHECK-IN ROW
 * ──────────────────────────────────────────────────────────────────────── */

private enum class AttendanceStatus(
    val label: String, val color: Color, val bg: Color, val emoji: String,
) {
    CheckedIn("ON-TIME", Success, SuccessBg, "✓"),
    Late     ("LATE",    Warning, WarningBg, "⌚"),
    Absent   ("ABSENT",  Danger,  DangerBg,  "✕"),
}

@OptIn(ExperimentalTime::class)
@Composable
private fun CheckInRow(
    employee: EmployeeRecord,
    status: AttendanceStatus,
    record: AttendanceRecord?,
    shiftStartMs: Long,
) {
    val time = record?.checkInMs?.let { formatHm(it) } ?: "--:--"
    val checkOutLabel = record?.checkOutMs?.let { "out " + formatHm(it) } ?: "on duty"
    val drift = when {
        record == null -> "--"
        status == AttendanceStatus.Late -> {
            val mins = ((record.checkInMs - shiftStartMs) / 60_000L).coerceAtLeast(0L)
            "+${mins}m"
        }
        else -> "+0m"
    }

    val initials = employee.name
        .split(" ")
        .take(2)
        .mapNotNull { it.firstOrNull()?.uppercaseChar() }
        .joinToString("")
        .ifEmpty { employee.name.take(2).uppercase() }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Avatar
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(status.bg),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                initials,
                color = status.color,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
            )
        }
        Spacer(Modifier.width(14.dp))

        // Name + meta
        Column(modifier = Modifier.weight(1f)) {
            Text(
                employee.name,
                color = InkPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    time,
                    color = InkMuted,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    "  ${status.emoji} ${status.label}  ·  $checkOutLabel",
                    color = InkMuted,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 0.4.sp,
                )
            }
        }

        // Drift indicator
        Text(
            drift,
            color = if (status == AttendanceStatus.Late) Warning else Success,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

@OptIn(ExperimentalTime::class)
private fun formatHm(epochMs: Long): String {
    val ldt = Instant.fromEpochMilliseconds(epochMs)
        .toLocalDateTime(TimeZone.currentSystemDefault())
    val hh = ldt.hour.toString().padStart(2, '0')
    val mm = ldt.minute.toString().padStart(2, '0')
    return "$hh:$mm"
}

@OptIn(ExperimentalTime::class)
private fun startOfTodayUtcMs(): Long {
    val nowMs = Clock.System.now().toEpochMilliseconds()
    val dayMs = 24L * 60L * 60L * 1000L
    return nowMs - (nowMs % dayMs)
}

