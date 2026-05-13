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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.outlined.Schedule
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.uniwattelektrik.core.performance.TrackScreenPerformance
import com.example.uniwattelektrik.core.theme.SetStatusBar
import com.example.uniwattelektrik.feature.workforce.data.remote.AttendanceRecord
import com.example.uniwattelektrik.feature.workforce.data.remote.LeaveRecord
import com.example.uniwattelektrik.feature.workforce.presentation.WorkforceViewModel
import com.example.uniwattelektrik.platform.nowEpochMillis
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.Month
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.daysUntil
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import kotlin.time.ExperimentalTime

/* ─── Design tokens (local — match the AdminAttendanceScreen palette) ───── */

private val ScreenBg     = Color(0xFFF5F7FA)
private val CardBg       = Color.White
private val InkPrimary   = Color(0xFF0D1B3E)
private val InkSecondary = Color(0xFF4E6083)
private val InkMuted     = Color(0xFFB0B8C1)
private val Hairline     = Color(0xFFE5E9F1)
private val Brand        = Color(0xFF1A6BF5)
private val ShadowSoft   = Color(0x14000000)

private val Success      = Color(0xFF16A34A)
private val SuccessBg    = Color(0xFFDCFCE7)
private val Warning      = Color(0xFFD97706)
private val WarningBg    = Color(0xFFFEF3C7)
private val Danger       = Color(0xFFDC2626)
private val DangerBg     = Color(0xFFFEE2E2)
private val Violet       = Color(0xFF7C3AED)
private val VioletBg     = Color(0xFFEDE9FE)

/** Per-day status used to colour each calendar cell. */
private enum class DayStatus { Present, Late, Absent, Leave, Future, OutOfMonth }

/* ═══════════════════════════════════════════════════════════════════════════
 *  EmployeeAttendanceDetailScreen — month grid + recent days timeline
 * ═══════════════════════════════════════════════════════════════════════════
 *
 *  Pulls per-employee attendance + approved leave from the live workforce
 *  view-model and renders a coloured month grid plus a recent-days list.
 *  Used by admins to drill into an individual's attendance history.
 */

@OptIn(ExperimentalTime::class)
@Composable
fun EmployeeAttendanceDetailScreen(
    userId      : String,
    workforceVm : WorkforceViewModel,
    onBack      : () -> Unit,
    modifier    : Modifier = Modifier,
) {
    TrackScreenPerformance("EmployeeAttendanceDetailScreen")
    SetStatusBar(color = Color.White, darkIcons = true)

    val employees   by workforceVm.employees.collectAsStateWithLifecycle()
    val attendance  by workforceVm.attendance.collectAsStateWithLifecycle()
    val leaves      by workforceVm.leaveRequests.collectAsStateWithLifecycle()

    val tz    = TimeZone.currentSystemDefault()
    val today = remember {
        Instant.fromEpochMilliseconds(nowEpochMillis())
            .toLocalDateTime(tz).date
    }

    // Per-user filtered slices.
    val myEmp        = employees.firstOrNull { it.id == userId }
    val myAttendance = remember(attendance, userId) { attendance.filter { it.userId == userId } }
    val myLeaves     = remember(leaves, userId) {
        leaves.filter { it.userId == userId && it.status == "approved" }
    }

    // Currently-viewed month (defaults to the current month, navigable).
    var viewedYear  by remember { mutableStateOf(today.year) }
    var viewedMonth by remember { mutableStateOf(today.month) }

    // Build the day-status map for the visible month.
    val dayStatus = remember(viewedYear, viewedMonth, myAttendance, myLeaves, today) {
        buildDayStatusMap(
            year       = viewedYear,
            month      = viewedMonth,
            today      = today,
            tz         = tz,
            attendance = myAttendance,
            leaves     = myLeaves,
        )
    }

    // Counts for the header pill ("X / Y days") — only the days up to and
    // including today in the viewed month, so the pill never overpromises.
    val (presentCount, totalCount) = remember(dayStatus, today, viewedYear, viewedMonth) {
        val daysInMonth = monthLength(viewedYear, viewedMonth)
        val cutoffDay = if (today.year == viewedYear && today.month == viewedMonth) today.dayOfMonth
                       else daysInMonth
        var present = 0
        var total = 0
        for (d in 1..cutoffDay) {
            val date = LocalDate(viewedYear, viewedMonth, d)
            val s = dayStatus[date]
            if (s == DayStatus.Present || s == DayStatus.Late) present++
            if (s != null && s != DayStatus.OutOfMonth) total++
        }
        present to total
    }

    Column(modifier = modifier.fillMaxSize().background(ScreenBg)) {

        // ── Top bar ────────────────────────────────────────────────────
        TopBar(
            employeeName = myEmp?.name ?: "Employee",
            monthLabel   = "${viewedMonth.shortName()} $viewedYear",
            presentCount = presentCount,
            totalCount   = totalCount,
            onBack       = onBack,
        )

        // ── Body ───────────────────────────────────────────────────────
        LazyColumn(
            modifier            = Modifier.fillMaxSize(),
            contentPadding      = PaddingValues(start = 18.dp, end = 18.dp, top = 18.dp, bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            item {
                CalendarCard(
                    year      = viewedYear,
                    month     = viewedMonth,
                    today     = today,
                    dayStatus = dayStatus,
                    onPrev    = {
                        val prev = LocalDate(viewedYear, viewedMonth, 1).minus(1, DateTimeUnit.MONTH)
                        viewedYear  = prev.year
                        viewedMonth = prev.month
                    },
                    onNext    = {
                        val next = LocalDate(viewedYear, viewedMonth, 1).plus(1, DateTimeUnit.MONTH)
                        viewedYear  = next.year
                        viewedMonth = next.month
                    },
                )
            }
            item { LegendRow() }
            item {
                Row(
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier              = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        "Recent days",
                        fontSize   = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color      = InkPrimary,
                    )
                    Text(
                        "Export →",
                        fontSize   = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color      = Brand,
                        modifier   = Modifier.clickable { /* TODO export */ },
                    )
                }
            }

            val recent = myAttendance
                .sortedByDescending { it.checkInMs }
                .take(10)

            if (recent.isEmpty()) {
                item {
                    Box(
                        modifier         = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(CardBg)
                            .padding(28.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            "No check-ins yet for this employee.",
                            color    = InkSecondary,
                            fontSize = 13.sp,
                        )
                    }
                }
            } else {
                items(recent, key = { it.id }) { rec ->
                    RecentDayCard(record = rec, tz = tz)
                }
            }
        }
    }
}

/* ─── Top bar ──────────────────────────────────────────────────────────── */

@Composable
private fun TopBar(
    employeeName: String,
    monthLabel  : String,
    presentCount: Int,
    totalCount  : Int,
    onBack      : () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(CardBg)
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        // Back button
        Box(
            modifier         = Modifier
                .size(42.dp)
                .shadow(2.dp, RoundedCornerShape(12.dp), spotColor = ShadowSoft)
                .clip(RoundedCornerShape(12.dp))
                .background(CardBg)
                .border(1.dp, Hairline, RoundedCornerShape(12.dp))
                .clickable(onClick = onBack),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = InkPrimary, modifier = Modifier.size(20.dp))
        }

        // Title + subtitle
        Column(modifier = Modifier.weight(1f).padding(horizontal = 14.dp)) {
            Text(monthLabel, fontSize = 12.sp, color = InkSecondary, fontWeight = FontWeight.Medium)
            Text(
                if (employeeName.isBlank()) "Attendance" else employeeName,
                fontSize   = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                color      = InkPrimary,
                letterSpacing = (-0.4).sp,
            )
        }

        // Days pill — uses the DS chip with an optional leading dot so the
        // "X / Y days" indicator stays visually consistent with the rest of
        // the app's status pills.
        com.example.uniwattelektrik.core.components.DsStatusChip(
            label           = "$presentCount / $totalCount days",
            tint            = Success,
            background      = SuccessBg,
            leadingDotColor = Success,
        )
    }
}

/* ─── Calendar card ────────────────────────────────────────────────────── */

@Composable
private fun CalendarCard(
    year     : Int,
    month    : Month,
    today    : LocalDate,
    dayStatus: Map<LocalDate, DayStatus>,
    onPrev   : () -> Unit,
    onNext   : () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(8.dp, RoundedCornerShape(20.dp), spotColor = ShadowSoft)
            .clip(RoundedCornerShape(20.dp))
            .background(CardBg)
            .padding(18.dp),
    ) {
        // Month header row
        Row(
            modifier              = Modifier.fillMaxWidth(),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                "${month.fullName()} $year",
                fontSize   = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                color      = InkPrimary,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MonthArrow(icon = Icons.Filled.ChevronLeft,  onClick = onPrev)
                MonthArrow(icon = Icons.Filled.ChevronRight, onClick = onNext)
            }
        }

        Spacer(Modifier.height(14.dp))

        // Weekday header (Monday-first)
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            listOf("M", "T", "W", "T", "F", "S", "S").forEach { d ->
                Text(
                    d,
                    fontSize   = 11.sp,
                    color      = InkMuted,
                    fontWeight = FontWeight.Bold,
                    textAlign  = TextAlign.Center,
                    modifier   = Modifier.weight(1f),
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        // 6 × 7 day grid
        val cells = remember(year, month) { buildMonthCells(year, month) }
        cells.chunked(7).forEach { week ->
            Row(
                modifier              = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                week.forEach { date ->
                    val status = if (date.month == month) dayStatus[date] ?: DayStatus.OutOfMonth
                                 else DayStatus.OutOfMonth
                    DayCell(
                        date     = date,
                        status   = status,
                        isToday  = date == today,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }

        Spacer(Modifier.height(6.dp))
    }
}

@Composable
private fun MonthArrow(
    icon   : androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
) {
    Box(
        modifier         = Modifier
            .size(34.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFFF1F4F9))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = InkPrimary, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun DayCell(
    date    : LocalDate,
    status  : DayStatus,
    isToday : Boolean,
    modifier: Modifier = Modifier,
) {
    val (bg, fg) = when {
        isToday                   -> Brand to Color.White
        status == DayStatus.Present -> SuccessBg to Success
        status == DayStatus.Late    -> WarningBg to Warning
        status == DayStatus.Absent  -> DangerBg  to Danger
        status == DayStatus.Leave   -> VioletBg  to Violet
        status == DayStatus.OutOfMonth -> Color.Transparent to InkMuted
        else                        -> Color.Transparent to InkPrimary
    }
    Box(
        modifier = modifier
            .padding(2.dp)
            .height(40.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(bg),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                date.dayOfMonth.toString(),
                fontSize   = 13.sp,
                fontWeight = if (isToday) FontWeight.ExtraBold else FontWeight.SemiBold,
                color      = fg,
            )
            if (isToday) {
                Spacer(Modifier.height(2.dp))
                Box(modifier = Modifier.size(4.dp).clip(CircleShape).background(Color.White))
            }
        }
    }
}

/* ─── Legend ───────────────────────────────────────────────────────────── */

@Composable
private fun LegendRow() {
    Row(
        modifier              = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment     = Alignment.CenterVertically,
    ) {
        LegendDot("Present", Success)
        LegendDot("Late",    Warning)
        LegendDot("Absent",  Danger)
        LegendDot("Leave",   Violet)
    }
}

@Composable
private fun LegendDot(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(6.dp))
        Text(label, fontSize = 12.sp, color = InkSecondary, fontWeight = FontWeight.Medium)
    }
}

/* ─── Recent-day card ──────────────────────────────────────────────────── */

@OptIn(ExperimentalTime::class)
@Composable
private fun RecentDayCard(record: AttendanceRecord, tz: TimeZone) {
    val dt        = Instant.fromEpochMilliseconds(record.checkInMs).toLocalDateTime(tz)
    val date      = dt.date
    val dayPill   = date.dayOfWeek.shortName().uppercase()
    val dayNum    = date.dayOfMonth
    val checkIn   = "${dt.hour.pad()}:${dt.minute.pad()}"
    val checkOut  = record.checkOutMs?.let {
        val o = Instant.fromEpochMilliseconds(it).toLocalDateTime(tz)
        "${o.hour.pad()}:${o.minute.pad()}"
    }
    val durLabel = record.checkOutMs?.let {
        val mins = (it - record.checkInMs) / 60_000L
        if (mins >= 60) "${mins / 60}h ${mins % 60}m" else "${mins}m"
    }
    val isLate = record.checkInStatus.equals("LATE", ignoreCase = true)

    Row(
        modifier              = Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(16.dp), spotColor = ShadowSoft)
            .clip(RoundedCornerShape(16.dp))
            .background(CardBg)
            .padding(14.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        // Day pill (FRI 25)
        Column(
            modifier            = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFFF1F4F9))
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(dayPill, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = InkSecondary, letterSpacing = 1.sp)
            Text(dayNum.toString(), fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, color = InkPrimary)
        }

        // Title + IN/OUT/duration line
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text       = locationLabel(record),
                fontSize   = 15.sp,
                fontWeight = FontWeight.Bold,
                color      = InkPrimary,
            )
            Spacer(Modifier.height(3.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Schedule, null, tint = InkMuted, modifier = Modifier.size(13.dp))
                Spacer(Modifier.width(5.dp))
                val line = buildString {
                    append("IN ").append(checkIn)
                    if (checkOut != null) append("  ·  OUT ").append(checkOut)
                    if (durLabel != null) append("  ·  ").append(durLabel)
                }
                Text(line, fontSize = 12.sp, color = InkSecondary)
            }
        }

        // Status pill
        if (isLate) {
            Row(
                modifier              = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(WarningBg)
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(Warning))
                val mins = ((record.checkInMs - shiftStartFor(record, tz)) / 60_000L).coerceAtLeast(1)
                Text("+${mins}m late", fontSize = 11.sp, color = Warning, fontWeight = FontWeight.Bold)
            }
        } else {
            Row(
                modifier              = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(SuccessBg)
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(Success))
                Icon(Icons.Filled.Check, null, tint = Success, modifier = Modifier.size(13.dp))
            }
        }
    }
}

/* ─── Date / data helpers ──────────────────────────────────────────────── */

@OptIn(ExperimentalTime::class)
private fun shiftStartFor(record: AttendanceRecord, tz: TimeZone): Long {
    val dt = Instant.fromEpochMilliseconds(record.checkInMs).toLocalDateTime(tz)
    val day = dt.date
    return day.atStartOfDayIn(tz).toEpochMilliseconds() + 9L * 3_600_000L
}

private fun locationLabel(rec: AttendanceRecord): String {
    val lat = rec.checkInLat
    val lng = rec.checkInLng
    return if (lat != null && lng != null)
        "Field check-in · ${formatLatLng(lat, lng)}"
    else
        "Field check-in"
}

private fun formatLatLng(lat: Double, lng: Double): String {
    fun trim(d: Double) = ((d * 1000).toInt() / 1000.0).toString()
    return "${trim(lat)}, ${trim(lng)}"
}

private fun Int.pad(): String = if (this < 10) "0$this" else toString()

private fun monthLength(year: Int, month: Month): Int {
    val first = LocalDate(year, month, 1)
    val nextFirst = first.plus(1, DateTimeUnit.MONTH)
    return first.daysUntil(nextFirst)
}

/** Builds 6 × 7 = 42 cells starting from the Monday on/before the 1st. */
private fun buildMonthCells(year: Int, month: Month): List<LocalDate> {
    val first = LocalDate(year, month, 1)
    // Monday-first offset (DayOfWeek.MONDAY.ordinal == 0).
    val offset = first.dayOfWeek.ordinal
    val gridStart = first.minus(offset, DateTimeUnit.DAY)
    return List(42) { i -> gridStart.plus(i, DateTimeUnit.DAY) }
}

@OptIn(ExperimentalTime::class)
private fun buildDayStatusMap(
    year      : Int,
    month     : Month,
    today     : LocalDate,
    tz        : TimeZone,
    attendance: List<AttendanceRecord>,
    leaves    : List<LeaveRecord>,
): Map<LocalDate, DayStatus> {
    val daysInMonth = monthLength(year, month)
    val map = mutableMapOf<LocalDate, DayStatus>()

    // Bucket attendance by date (local day).
    val attByDate = attendance.associateBy {
        Instant.fromEpochMilliseconds(it.checkInMs).toLocalDateTime(tz).date
    }

    // Pre-compute leave date sets (inclusive of fromDate..toDate).
    val leaveDates: Set<LocalDate> = leaves.flatMap { lr ->
        val from = Instant.fromEpochMilliseconds(lr.fromDateMs).toLocalDateTime(tz).date
        val to   = Instant.fromEpochMilliseconds(lr.toDateMs).toLocalDateTime(tz).date
        val span = from.daysUntil(to).coerceAtLeast(0)
        (0..span).map { from.plus(it, DateTimeUnit.DAY) }
    }.toSet()

    for (d in 1..daysInMonth) {
        val date = LocalDate(year, month, d)
        val status = when {
            date in leaveDates       -> DayStatus.Leave
            attByDate[date] != null  -> {
                if (attByDate[date]!!.checkInStatus.equals("LATE", ignoreCase = true))
                    DayStatus.Late else DayStatus.Present
            }
            date > today             -> DayStatus.Future
            date.dayOfWeek == DayOfWeek.SUNDAY || date.dayOfWeek == DayOfWeek.SATURDAY ->
                DayStatus.Future        // weekends — no marker
            else                     -> DayStatus.Absent
        }
        map[date] = status
    }
    return map
}

private fun Month.shortName(): String = when (this) {
    Month.JANUARY -> "Jan"; Month.FEBRUARY -> "Feb"; Month.MARCH -> "Mar"
    Month.APRIL   -> "Apr"; Month.MAY -> "May"; Month.JUNE -> "Jun"
    Month.JULY    -> "Jul"; Month.AUGUST -> "Aug"; Month.SEPTEMBER -> "Sep"
    Month.OCTOBER -> "Oct"; Month.NOVEMBER -> "Nov"; Month.DECEMBER -> "Dec"
    else          -> name.take(3).lowercase().replaceFirstChar { it.uppercase() }
}

private fun Month.fullName(): String = when (this) {
    Month.JANUARY   -> "January";   Month.FEBRUARY -> "February"
    Month.MARCH     -> "March";     Month.APRIL    -> "April"
    Month.MAY       -> "May";       Month.JUNE     -> "June"
    Month.JULY      -> "July";      Month.AUGUST   -> "August"
    Month.SEPTEMBER -> "September"; Month.OCTOBER  -> "October"
    Month.NOVEMBER  -> "November";  Month.DECEMBER -> "December"
    else            -> name.lowercase().replaceFirstChar { it.uppercase() }
}

private fun DayOfWeek.shortName(): String = when (this) {
    DayOfWeek.MONDAY    -> "Mon"
    DayOfWeek.TUESDAY   -> "Tue"
    DayOfWeek.WEDNESDAY -> "Wed"
    DayOfWeek.THURSDAY  -> "Thu"
    DayOfWeek.FRIDAY    -> "Fri"
    DayOfWeek.SATURDAY  -> "Sat"
    DayOfWeek.SUNDAY    -> "Sun"
    else                -> name.take(3).lowercase().replaceFirstChar { it.uppercase() }
}
