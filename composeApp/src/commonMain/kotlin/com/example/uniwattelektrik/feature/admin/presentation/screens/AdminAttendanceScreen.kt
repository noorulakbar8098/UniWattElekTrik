package com.example.uniwattelektrik.feature.admin.presentation.screens

import com.example.uniwattelektrik.core.performance.TrackScreenPerformance
import com.example.uniwattelektrik.core.theme.AppTheme
import com.example.uniwattelektrik.core.theme.appScreenBackground

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.UnfoldLess
import androidx.compose.material.icons.filled.UnfoldMore
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.People
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.uniwattelektrik.core.components.AppPullToRefresh
import com.example.uniwattelektrik.core.components.OperationsHeaderSurface
import com.example.uniwattelektrik.feature.admin.presentation.components.MapMarker
import com.example.uniwattelektrik.feature.workforce.data.remote.AttendanceRecord
import com.example.uniwattelektrik.feature.workforce.data.remote.EmployeeRecord
import com.example.uniwattelektrik.feature.workforce.presentation.WorkforceViewModel
import com.example.uniwattelektrik.platform.nowEpochMillis
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.minus
import kotlinx.datetime.toLocalDateTime
import kotlin.time.ExperimentalTime

// ─── Design tokens ─────────────────────────────────────────────────────────────
private val CardBg       = Color.White
private val ScreenBg     = Color(0xFFF4F6FB)
private val InkPrimary   = Color(0xFF1A1A2E)
private val InkSecondary = Color(0xFF6B7280)
private val InkMuted     = Color(0xFFB0B8C1)
private val Success      = Color(0xFF22C55E)
private val SuccessBg    = Color(0xFFDCFCE7)
private val Warning      = Color(0xFFF59E0B)
private val WarningBg    = Color(0xFFFFF8E1)
private val Danger       = Color(0xFFEF4444)
private val DangerBg     = Color(0xFFFEE2E2)
private val Brand        = Color(0xFF2979FF)
private val ShadowSoft   = Color(0x14000000)
private val WhiteA20     = Color(0x33FFFFFF)
private val WhiteA70     = Color(0xB3FFFFFF)

// ─── Filter enum ──────────────────────────────────────────────────────────────
private enum class AttendanceFilter(val label: String) {
    ALL("All"), ON_TIME("On Time"), LATE("Late"), ABSENT("Absent")
}

// ─── Derived UI model ─────────────────────────────────────────────────────────
private data class EmployeeAttendanceUi(
    val userId: String,
    val name: String,
    val initials: String,
    val department: String,
    val role: String,
    val status: String,          // "ON_TIME" | "LATE" | "ABSENT"
    val checkInMs: Long?,
    val checkOutMs: Long?,
    val checkInLat: Double?,
    val checkInLng: Double?,
    val shiftDuration: String?,
    val lateMins: Long,
)

// ─── Main screen ──────────────────────────────────────────────────────────────

@OptIn(ExperimentalTime::class, ExperimentalMaterial3Api::class)
@Composable
fun AdminAttendanceScreen(
    workforceVm: WorkforceViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    /** Tap on an employee row → push the per-employee attendance detail. */
    onEmployeeClick: (userId: String) -> Unit = {},
    /** Admin uid — required by the embedded leave approvals tab. */
    adminUid: String = "",
) {
    TrackScreenPerformance("AdminAttendanceScreen")

    val employees  by workforceVm.employees.collectAsStateWithLifecycle()
    val attendance by workforceVm.attendance.collectAsStateWithLifecycle()
    val checkins   by workforceVm.checkins.collectAsStateWithLifecycle()

    com.example.uniwattelektrik.core.theme.SetStatusBar(color = Brand, darkIcons = false)

    val tz    = TimeZone.currentSystemDefault()
    val today = remember {
        Instant.fromEpochMilliseconds(nowEpochMillis())
            .toLocalDateTime(tz).date
    }
    // Quick 7-day window (Today, Yesterday, then the previous 5 days). Custom
    // dates outside this window are supported via the calendar picker — the
    // picked date is shown as a "Custom" chip prepended to the row.
    val dates       = remember(today) { (0..6).map { d -> today.minus(d, DateTimeUnit.DAY) } }
    var selectedDate by remember(today) { mutableStateOf(today) }
    var activeFilter by remember { mutableStateOf(AttendanceFilter.ALL) }

    // ── "Last updated Xs ago" counter ──────────────────────────────────────────
    // NOTE: state + tick effect are deliberately NOT hoisted here — see the
    // private RelativeUpdatedTime composable at the bottom of the file.
    // Reading the counter at screen level would cause the entire scroll surface
    // to recompose every second, which is the single biggest scroll-jank
    // source in this file. Now it lives inside the leaf Text only.

    // ── Date math ─────────────────────────────────────────────────────────────
    val selectedStartMs = remember(selectedDate) { selectedDate.atStartOfDayIn(tz).toEpochMilliseconds() }
    val selectedEndMs   = selectedStartMs + 86_400_000L
    val shiftStartMs    = selectedStartMs + 9L * 3_600_000L   // 09:00 AM

    // ── Derived records ────────────────────────────────────────────────────────
    val dayAttendance = remember(attendance, selectedStartMs) {
        attendance
            .filter { it.checkInMs in selectedStartMs until selectedEndMs }
            .sortedByDescending { it.checkInMs }
    }

    val activeEmployees = remember(employees) { employees.filter { it.deletedAt == null && it.status != "Inactive" } }

    val onTimeCt = dayAttendance.count { it.checkInMs <= shiftStartMs }
    val lateCt   = dayAttendance.count { it.checkInMs > shiftStartMs }
    val absentCt = (activeEmployees.size - dayAttendance.size).coerceAtLeast(0)

    // ── Build full attendance UI model (present + absent) ─────────────────────
    val allUiItems = remember(dayAttendance, activeEmployees, shiftStartMs) {
        val presentIds = dayAttendance.map { it.userId }.toSet()

        // Present employees
        val present = dayAttendance.map { rec ->
            val emp      = activeEmployees.firstOrNull { it.id == rec.userId }
            val isLate   = rec.checkInMs > shiftStartMs
            val lateMins = if (isLate) ((rec.checkInMs - shiftStartMs) / 60_000L) else 0L
            val dur = if (rec.checkOutMs != null && rec.checkOutMs > rec.checkInMs) {
                val m = (rec.checkOutMs - rec.checkInMs) / 60_000L
                if (m >= 60) "${m / 60}h ${m % 60}m" else "${m}m"
            } else null
            EmployeeAttendanceUi(
                userId       = rec.userId,
                name         = emp?.name ?: rec.userId.take(8),
                initials     = initials(emp?.name ?: rec.userId),
                department   = emp?.department?.takeIf { it.isNotBlank() } ?: "—",
                role         = emp?.role?.takeIf { it.isNotBlank() } ?: "Employee",
                status       = if (isLate) "LATE" else "ON_TIME",
                checkInMs    = rec.checkInMs,
                checkOutMs   = rec.checkOutMs,
                checkInLat   = rec.checkInLat,
                checkInLng   = rec.checkInLng,
                shiftDuration = dur,
                lateMins     = lateMins,
            )
        }

        // Absent employees
        val absent = activeEmployees
            .filter { it.id !in presentIds }
            .map { emp ->
                EmployeeAttendanceUi(
                    userId       = emp.id,
                    name         = emp.name,
                    initials     = initials(emp.name),
                    department   = emp.department.takeIf { it.isNotBlank() } ?: "—",
                    role         = emp.role.takeIf { it.isNotBlank() } ?: "Employee",
                    status       = "ABSENT",
                    checkInMs    = null,
                    checkOutMs   = null,
                    checkInLat   = null,
                    checkInLng   = null,
                    shiftDuration = null,
                    lateMins     = 0L,
                )
            }

        present + absent
    }

    // ── Filtered list ──────────────────────────────────────────────────────────
    val filteredItems = remember(allUiItems, activeFilter) {
        when (activeFilter) {
            AttendanceFilter.ALL     -> allUiItems
            AttendanceFilter.ON_TIME -> allUiItems.filter { it.status == "ON_TIME" }
            AttendanceFilter.LATE    -> allUiItems.filter { it.status == "LATE" }
            AttendanceFilter.ABSENT  -> allUiItems.filter { it.status == "ABSENT" }
        }
    }

    // ── Map markers (filter-aware) ─────────────────────────────────────────────
    val markers = remember(checkins, employees, selectedStartMs, selectedEndMs, activeFilter) {
        val dayPings = checkins.filter { it.timestampMs in selectedStartMs until selectedEndMs }
        val latestByUser = dayPings.sortedByDescending { it.timestampMs }.associateBy { it.userId }
        latestByUser.values
            .filter { ping ->
                if (activeFilter == AttendanceFilter.ABSENT) return@filter false
                val rec = dayAttendance.firstOrNull { it.userId == ping.userId }
                when (activeFilter) {
                    AttendanceFilter.ON_TIME -> rec != null && rec.checkInMs <= shiftStartMs
                    AttendanceFilter.LATE    -> rec != null && rec.checkInMs > shiftStartMs
                    else                     -> true
                }
            }
            .map { ping ->
                val emp = employees.firstOrNull { it.id == ping.userId }
                MapMarker(
                    id        = ping.id,
                    title     = emp?.name ?: "Employee",
                    snippet   = emp?.role ?: "GPS ping",
                    latitude  = ping.latitude,
                    longitude = ping.longitude,
                )
            }
    }

    // ── Today on-field count ───────────────────────────────────────────────────
    val todayStartMs = remember(today) { today.atStartOfDayIn(tz).toEpochMilliseconds() }
    val todayOnField = remember(attendance, todayStartMs) {
        attendance.count { it.checkInMs in todayStartMs until todayStartMs + 86_400_000L }
    }

    // ── Tab pager: Attendance | Leave Requests ──────────────────────────────
    val pagerState = rememberPagerState(pageCount = { 2 })
    val coScope    = rememberCoroutineScope()

    // ── Embedded map expand/collapse state ──────────────────────────────────
    // Hoisted here (above the pager) so the toggle persists across tab swipes.
    var mapExpanded by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxSize().background(ScreenBg)) {

        // 1. Premium gradient header.
        //
        // The "Xs ago" ticker is encapsulated inside the header itself so the
        // timer can never bubble a recomposition up to the screen.
        AttendancePremiumHeader(
            todayOnField  = todayOnField,
            absentCt      = absentCt,
            lateCt        = lateCt,
            updateResetKey = attendance.size to checkins.size,
        )

        // 2. Tab row — moved directly under the header so the tabs are always
        //    visible at the top of the screen.
        PrimaryTabRow(
            selectedTabIndex = pagerState.currentPage,
            containerColor   = Color.White,
            contentColor     = Brand,
        ) {
            val tabs = listOf("Attendance", "Leave Requests")
            tabs.forEachIndexed { i, label ->
                Tab(
                    selected = pagerState.currentPage == i,
                    onClick  = { coScope.launch { pagerState.animateScrollToPage(i) } },
                    text = {
                        Text(
                            text       = label,
                            fontSize   = 13.sp,
                            fontWeight = if (pagerState.currentPage == i) FontWeight.Bold else FontWeight.Medium,
                        )
                    },
                )
            }
        }

        // 3. Live map.
        // IMPORTANT: rendered OUTSIDE the HorizontalPager, with a `visible`
        // flag driving an animated height-collapse to 0 when the user is on
        // the Leave tab. Keeping the composable mounted prevents the OSM
        // AndroidView from re-attaching across page swipes (which previously
        // caused a recursive dispatchGetDisplayList crash). The Leave tab
        // therefore gets the full vertical area for itself.
        AttendanceMapSection(
            markers       = markers,
            expanded      = mapExpanded,
            visible       = pagerState.currentPage == 0,
            onToggleExpand = { mapExpanded = !mapExpanded },
        )

        // 4. Page contents.
        HorizontalPager(
            state    = pagerState,
            modifier = Modifier.weight(1f).fillMaxWidth(),
        ) { page ->
            when (page) {
                0 -> AttendanceTabContent(
                    workforceVm    = workforceVm,
                    onTimeCt       = onTimeCt,
                    lateCt         = lateCt,
                    absentCt       = absentCt,
                    activeFilter   = activeFilter,
                    onActiveFilter = { activeFilter = it },
                    dates          = dates,
                    today          = today,
                    selectedDate   = selectedDate,
                    onSelectDate   = { selectedDate = it },
                    filteredItems  = filteredItems,
                    onEmployeeClick = onEmployeeClick,
                )
                else -> AdminLeaveApprovalsScreen(
                    workforceVm = workforceVm,
                    adminUid    = adminUid,
                    onBack      = onBack,
                    showHeader  = false,
                )
            }
        }
    }
}

/**
 * Body of the "Attendance" tab — analytics, date selector, filter bar, map,
 * and the employee list. Extracted so the tab can be swapped in cleanly via
 * the parent HorizontalPager.
 */
@Composable
private fun AttendanceTabContent(
    workforceVm    : WorkforceViewModel,
    onTimeCt       : Int,
    lateCt         : Int,
    absentCt       : Int,
    activeFilter   : AttendanceFilter,
    onActiveFilter : (AttendanceFilter) -> Unit,
    /** Quick-pick window — the latest 7 days ending at `today`. */
    dates          : List<kotlinx.datetime.LocalDate>,
    today          : kotlinx.datetime.LocalDate,
    selectedDate   : kotlinx.datetime.LocalDate,
    onSelectDate   : (kotlinx.datetime.LocalDate) -> Unit,
    filteredItems  : List<EmployeeAttendanceUi>,
    onEmployeeClick: (userId: String) -> Unit,
) {
    AppPullToRefresh(onRefresh = { workforceVm.refresh() }) {
        LazyColumn(
            modifier            = Modifier.fillMaxSize().background(ScreenBg),
            contentPadding      = PaddingValues(bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp),
        ) {
            item {
                Spacer(Modifier.height(18.dp))
                AttendanceAnalyticsRow(
                    onTimeCt     = onTimeCt,
                    lateCt       = lateCt,
                    absentCt     = absentCt,
                    activeFilter = activeFilter,
                    onFilter     = { f -> onActiveFilter(if (activeFilter == f) AttendanceFilter.ALL else f) },
                )
            }
            item {
                Spacer(Modifier.height(18.dp))
                DateSelectorRow(
                    dates        = dates,
                    today        = today,
                    selectedDate = selectedDate,
                    onSelect     = onSelectDate,
                )
            }
            item {
                Spacer(Modifier.height(14.dp))
                AttendanceFilterBar(
                    selected = activeFilter,
                    onSelect = onActiveFilter,
                )
            }
            // (Map lifted out of the pager — see parent Column.)
            item {
                Spacer(Modifier.height(22.dp))
                Row(
                    modifier              = Modifier.padding(horizontal = 20.dp).fillMaxWidth(),
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        "Employee Status",
                        fontWeight = FontWeight.Bold,
                        fontSize   = 18.sp,
                        color      = InkPrimary,
                    )
                    com.example.uniwattelektrik.core.components.DsStatusChip(
                        label      = "${filteredItems.size} shown",
                        tint       = Brand,
                        background = Color(0xFFEEF2FF),
                    )
                }
                Spacer(Modifier.height(10.dp))
            }

            if (filteredItems.isEmpty()) {
                item {
                    Box(
                        modifier         = Modifier.fillMaxWidth().padding(40.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Outlined.People, null, modifier = Modifier.size(48.dp), tint = Color(0xFFD1D5DB))
                            Spacer(Modifier.height(10.dp))
                            Text("No records for this filter", color = InkSecondary, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            } else {
                items(filteredItems, key = { it.userId + it.status }) { item ->
                    EmployeeAttendanceCard(
                        item    = item,
                        onClick = { onEmployeeClick(item.userId) },
                    )
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }
}

// ─── 1. Premium Header ─────────────────────────────────────────────────────────

@Composable
private fun AttendancePremiumHeader(
    todayOnField  : Int,
    absentCt      : Int,
    lateCt        : Int,
    /** Any state that should reset the "seconds ago" ticker — passing this as
     *  a key means the header's tick effect restarts when the underlying
     *  attendance / check-in counts change. */
    updateResetKey: Any = Unit,
) {
    // Pulsing dot animation
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue  = 1.0f,
        animationSpec = infiniteRepeatable(tween(900, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "pulseAlpha",
    )
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue  = 1.4f,
        animationSpec = infiniteRepeatable(tween(900, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "pulseScale",
    )

    OperationsHeaderSurface {
        Row(
            modifier              = Modifier.fillMaxWidth(),
            verticalAlignment     = Alignment.Top,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "ATTENDANCE",
                    color = Color.White.copy(alpha = 0.45f),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.4.sp,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "Field Workforce Live",
                    color      = Color.White,
                    fontSize   = 24.sp,             // canonical title size
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-0.4).sp,
                )
                Spacer(Modifier.height(2.dp))       // matches HeaderTitleToSubtitleGap
                Text(
                    "$todayOnField Active  ·  $absentCt Absent  ·  $lateCt Late",
                    color    = Color.White.copy(alpha = 0.65f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                )
            }

            // LIVE badge + last updated
            Column(horizontalAlignment = Alignment.End) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(Color.White.copy(alpha = 0.10f))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                ) {
                    Row(
                        verticalAlignment      = Alignment.CenterVertically,
                        horizontalArrangement  = Arrangement.spacedBy(6.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .scale(pulseScale)
                                .clip(CircleShape)
                                .background(Success.copy(alpha = pulse)),
                        )
                        Text("LIVE", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.5.sp)
                    }
                }
                Spacer(Modifier.height(6.dp))
                RelativeUpdatedTime(resetKey = updateResetKey)
            }
        }
    }
}

/* ─── Leaf: "Xs ago" ticker (scoped recomposition) ──────────────────────── */

/**
 * Owns its own per-second tick state. Skipping this lift would cause the
 * entire attendance screen — including the LazyColumn — to recompose every
 * second, blowing scroll smoothness.
 */
@Composable
private fun RelativeUpdatedTime(
    resetKey: Any,
    modifier: Modifier = Modifier,
) {
    var secondsAgo by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000L)
            secondsAgo = if (secondsAgo >= 59) 0 else secondsAgo + 1
        }
    }
    // Whenever upstream data changes, snap the counter back to 0.
    LaunchedEffect(resetKey) { secondsAgo = 0 }
    Text(
        text     = if (secondsAgo == 0) "Just updated" else "${secondsAgo}s ago",
        color    = WhiteA70,
        fontSize = 10.sp,
        modifier = modifier,
    )
}

// ─── 2. Analytics Cards ────────────────────────────────────────────────────────

@Composable
private fun AttendanceAnalyticsRow(
    onTimeCt: Int,
    lateCt: Int,
    absentCt: Int,
    activeFilter: AttendanceFilter,
    onFilter: (AttendanceFilter) -> Unit,
) {
    // Total of present + absent employees so we can show "% of team" bars.
    val total = (onTimeCt + lateCt + absentCt).coerceAtLeast(1)

    Row(
        modifier              = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        AttendanceStatCard(
            label       = "On Field",
            count       = onTimeCt,
            total       = total,
            icon        = Icons.Filled.Check,
            accentColor = Color(0xFF22C55E),
            glowColor   = Color(0x2622C55E),
            isSelected  = activeFilter == AttendanceFilter.ON_TIME,
            onClick     = { onFilter(AttendanceFilter.ON_TIME) },
            modifier    = Modifier.weight(1f),
        )
        AttendanceStatCard(
            label       = "Late",
            count       = lateCt,
            total       = total,
            icon        = Icons.Filled.Schedule,
            accentColor = Color(0xFFF59E0B),
            glowColor   = Color(0x26F59E0B),
            isSelected  = activeFilter == AttendanceFilter.LATE,
            onClick     = { onFilter(AttendanceFilter.LATE) },
            modifier    = Modifier.weight(1f),
        )
        AttendanceStatCard(
            label       = "Absent",
            count       = absentCt,
            total       = total,
            icon        = Icons.Filled.Close,
            accentColor = Color(0xFFEF4444),
            glowColor   = Color(0x26EF4444),
            isSelected  = activeFilter == AttendanceFilter.ABSENT,
            onClick     = { onFilter(AttendanceFilter.ABSENT) },
            modifier    = Modifier.weight(1f),
        )
    }
}

/**
 * Compact stat card — horizontal layout: square icon chip on the left,
 * count + label + animated fill-bar stacked on the right. A thin coloured
 * accent strip runs along the top edge. Tapping the card toggles its
 * attendance filter.
 *
 * Designed to roughly halve the height of the previous vertical layout so
 * three cards fit inline without dominating the screen — leaves more room
 * for the map and the employee list below.
 */
@Composable
private fun AttendanceStatCard(
    label: String,
    count: Int,
    total: Int,
    icon: ImageVector,
    accentColor: Color,
    glowColor: Color,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val elevation by animateDpAsState(
        targetValue   = if (isSelected) 12.dp else 3.dp,
        label         = "cardElev",
    )
    val scale by animateFloatAsState(
        targetValue   = if (isSelected) 1.03f else 1f,
        animationSpec = tween(200),
        label         = "cardScale",
    )
    val pct = if (total > 0) count.toFloat() / total else 0f
    val animPct by animateFloatAsState(
        targetValue   = pct,
        animationSpec = tween(700, easing = FastOutSlowInEasing),
        label         = "barPct",
    )

    Box(modifier = modifier.scale(scale)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(elevation, RoundedCornerShape(16.dp), spotColor = if (isSelected) glowColor else ShadowSoft)
                .clip(RoundedCornerShape(16.dp))
                .background(CardBg)
                .then(
                    if (isSelected)
                        Modifier.border(1.5.dp, accentColor.copy(alpha = 0.55f), RoundedCornerShape(16.dp))
                    else Modifier,
                )
                .clickable(onClick = onClick),
        ) {
            // ── Thin coloured top accent strip ──────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .background(
                        Brush.horizontalGradient(
                            listOf(accentColor, accentColor.copy(alpha = 0.55f)),
                        ),
                    ),
            )

            Row(
                modifier              = Modifier.padding(horizontal = 10.dp, vertical = 9.dp),
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(9.dp),
            ) {
                // ── Icon chip (left) + optional "active" dot ──────────
                Box {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(accentColor.copy(alpha = if (isSelected) 0.18f else 0.10f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(icon, null, tint = accentColor, modifier = Modifier.size(16.dp))
                    }
                    if (isSelected) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .align(Alignment.TopEnd)
                                .offset(x = 2.dp, y = (-2).dp)
                                .clip(CircleShape)
                                .background(accentColor)
                                .border(1.5.dp, CardBg, CircleShape),
                        )
                    }
                }

                // ── Right column: count + % inline, label, fill bar ────
                Column(
                    modifier            = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    Row(
                        verticalAlignment     = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            count.toString(),
                            color         = if (isSelected) accentColor else InkPrimary,
                            fontSize      = 20.sp,
                            fontWeight    = FontWeight.ExtraBold,
                            letterSpacing = (-0.4).sp,
                            lineHeight    = 20.sp,
                        )
                        Text(
                            "${(pct * 100).toInt()}%",
                            color      = accentColor.copy(alpha = 0.7f),
                            fontSize   = 9.sp,
                            fontWeight = FontWeight.Bold,
                            modifier   = Modifier.padding(bottom = 2.dp),
                        )
                    }
                    Text(
                        label,
                        color         = InkSecondary,
                        fontSize      = 10.sp,
                        fontWeight    = FontWeight.SemiBold,
                        letterSpacing = 0.2.sp,
                        maxLines      = 1,
                        overflow      = TextOverflow.Ellipsis,
                    )
                    // Animated fill bar — visual at-a-glance density.
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp)
                            .clip(RoundedCornerShape(999.dp))
                            .background(accentColor.copy(alpha = 0.12f)),
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(animPct.coerceIn(0f, 1f))
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(999.dp))
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(accentColor, accentColor.copy(alpha = 0.55f)),
                                    ),
                                ),
                        )
                    }
                }
            }
        }
    }
}

// ─── 3. Date Selector ─────────────────────────────────────────────────────────

/**
 * Professional day-card date picker.
 *
 * Top row     — month/year of the currently-selected date + "Pick date" button
 *               that opens a Material3 DatePickerDialog for arbitrary dates.
 * Bottom row  — horizontal row of vertical "day cards" (day-of-week ▸ date
 *               number ▸ active dot). When the picked date is OUTSIDE the
 *               quick 7-day window, a "Custom" chip is prepended so the user
 *               can see exactly which day they're viewing.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateSelectorRow(
    dates: List<kotlinx.datetime.LocalDate>,
    today: kotlinx.datetime.LocalDate,
    selectedDate: kotlinx.datetime.LocalDate,
    onSelect: (kotlinx.datetime.LocalDate) -> Unit,
) {
    var showPicker by remember { mutableStateOf(false) }
    val tz = TimeZone.currentSystemDefault()

    Column(modifier = Modifier.fillMaxWidth()) {
        // ── Top row: month/year + "Pick date" button ────────────────────
        Row(
            modifier              = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(
                    Icons.Filled.CalendarToday,
                    contentDescription = null,
                    tint               = Brand,
                    modifier           = Modifier.size(16.dp),
                )
                Text(
                    "${monthName(selectedDate.month.name)} ${selectedDate.year}",
                    color         = InkPrimary,
                    fontSize      = 14.sp,
                    fontWeight    = FontWeight.Bold,
                    letterSpacing = 0.2.sp,
                )
            }
            Box(
                modifier = Modifier
                    .shadow(2.dp, RoundedCornerShape(10.dp), spotColor = Brand.copy(alpha = 0.25f))
                    .clip(RoundedCornerShape(10.dp))
                    .background(Brand.copy(alpha = 0.10f))
                    .border(1.dp, Brand.copy(alpha = 0.30f), RoundedCornerShape(10.dp))
                    .clickable { showPicker = true }
                    .padding(horizontal = 10.dp, vertical = 6.dp),
            ) {
                Row(
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    Icon(
                        Icons.Filled.CalendarToday,
                        contentDescription = null,
                        tint               = Brand,
                        modifier           = Modifier.size(12.dp),
                    )
                    Text(
                        "Pick date",
                        color      = Brand,
                        fontSize   = 11.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        // ── Day cards row (scrolls horizontally) ────────────────────────
        LazyRow(
            contentPadding        = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // Custom date — shown ONLY when the picked date is outside the
            // 7-day quick window. Acts as a "you are viewing" indicator.
            val isCustom = selectedDate !in dates
            if (isCustom) {
                item(key = "custom-${selectedDate}") {
                    DayChip(
                        topLabel    = "CUSTOM",
                        number      = selectedDate.dayOfMonth,
                        bottomLabel = monthName(selectedDate.month.name).take(3).uppercase(),
                        isSelected  = true,
                        accent      = Color(0xFF8B5CF6),
                        onClick     = { /* already selected */ },
                    )
                }
            }
            items(dates, key = { it.toString() }) { date ->
                val isSelected = date == selectedDate
                val isToday    = date == today
                val isYest     = date == today.minus(1, DateTimeUnit.DAY)
                DayChip(
                    topLabel = when {
                        isToday -> "TODAY"
                        isYest  -> "YEST"
                        else    -> dayAbbrev(date.dayOfWeek.name).uppercase()
                    },
                    number   = date.dayOfMonth,
                    bottomLabel = monthName(date.month.name).take(3).uppercase(),
                    isSelected  = isSelected,
                    accent      = if (isToday) Brand else Color(0xFF334155),
                    onClick     = { onSelect(date) },
                )
            }
        }
    }

    if (showPicker) {
        val initialMs = selectedDate.atStartOfDayIn(tz).toEpochMilliseconds()
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = initialMs)
        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    val ms = pickerState.selectedDateMillis
                    if (ms != null) {
                        val picked = Instant.fromEpochMilliseconds(ms)
                            .toLocalDateTime(tz).date
                        onSelect(picked)
                    }
                    showPicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showPicker = false }) { Text("Cancel") }
            },
        ) { DatePicker(state = pickerState) }
    }
}

/**
 * A vertical "day card" used by [DateSelectorRow]. Renders an eyebrow label
 * (TODAY / YEST / day-of-week abbrev), a large date number, and a tiny
 * month abbrev underneath. The active card animates to filled-brand with a
 * white dot indicator; inactive cards sit on a white surface with a subtle
 * border.
 */
@Composable
private fun DayChip(
    topLabel: String,
    number: Int,
    bottomLabel: String,
    isSelected: Boolean,
    accent: Color,
    onClick: () -> Unit,
) {
    val scale by animateFloatAsState(
        targetValue   = if (isSelected) 1.04f else 1f,
        animationSpec = tween(220),
        label         = "dayScale",
    )
    val elevation by animateDpAsState(
        targetValue = if (isSelected) 10.dp else 1.dp,
        label       = "dayElev",
    )
    val bgColor by animateColorAsState(
        targetValue = if (isSelected) accent else CardBg,
        label       = "dayBg",
    )

    Box(
        modifier = Modifier
            .scale(scale)
            .size(width = 58.dp, height = 70.dp)
            .shadow(
                elevation = elevation,
                shape     = RoundedCornerShape(14.dp),
                spotColor = if (isSelected) accent.copy(alpha = 0.35f) else ShadowSoft,
            )
            .clip(RoundedCornerShape(14.dp))
            .background(bgColor)
            .then(
                if (!isSelected)
                    Modifier.border(1.dp, Color(0xFFE5E7EB), RoundedCornerShape(14.dp))
                else Modifier,
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(1.dp),
        ) {
            Text(
                text          = topLabel,
                color         = if (isSelected) Color.White.copy(alpha = 0.92f) else InkSecondary,
                fontSize      = 9.sp,
                fontWeight    = FontWeight.Bold,
                letterSpacing = 0.6.sp,
                maxLines      = 1,
            )
            Text(
                text          = number.toString(),
                color         = if (isSelected) Color.White else InkPrimary,
                fontSize      = 20.sp,
                fontWeight    = FontWeight.ExtraBold,
                letterSpacing = (-0.4).sp,
                lineHeight    = 22.sp,
            )
            Text(
                text          = bottomLabel,
                color         = if (isSelected) Color.White.copy(alpha = 0.75f) else InkSecondary.copy(alpha = 0.7f),
                fontSize      = 8.sp,
                fontWeight    = FontWeight.SemiBold,
                letterSpacing = 0.4.sp,
                maxLines      = 1,
            )
        }
        // Active dot at the bottom of the selected card.
        if (isSelected) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 4.dp)
                    .size(4.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.85f)),
            )
        }
    }
}

// ─── 4. Filter Bar ─────────────────────────────────────────────────────────────

@Composable
private fun AttendanceFilterBar(
    selected: AttendanceFilter,
    onSelect: (AttendanceFilter) -> Unit,
) {
    val filters = AttendanceFilter.values()
    // Animated offset for the sliding indicator
    val selectedIndex = filters.indexOf(selected)
    val animatedIndex by animateFloatAsState(
        targetValue   = selectedIndex.toFloat(),
        animationSpec = tween(300, easing = FastOutSlowInEasing),
        label         = "filterIndicator",
    )

    Box(
        modifier = Modifier
            .padding(horizontal = 20.dp)
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(16.dp), spotColor = ShadowSoft)
            .clip(RoundedCornerShape(16.dp))
            .background(CardBg)
            .padding(4.dp),
    ) {
        // Sliding pill
        Box(
            modifier = Modifier
                .fillMaxWidth(1f / filters.size)
                .offset(x = (animatedIndex * 100 / filters.size).dp.times(0f)) // handled via weight
                .height(38.dp)
        )
        Row(modifier = Modifier.fillMaxWidth()) {
            filters.forEach { filter ->
                val isSelected = selected == filter
                val textColor by animateColorAsState(
                    if (isSelected) Color.White else InkSecondary, tween(250), label = "txt"
                )
                val bgColor by animateColorAsState(
                    if (isSelected) Brand else Color.Transparent, tween(250), label = "bg"
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(bgColor)
                        .clickable { onSelect(filter) }
                        .padding(vertical = 9.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        filter.label,
                        color      = textColor,
                        fontSize   = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        maxLines   = 1,
                    )
                }
            }
        }
    }
}

// ─── 5. Map Section ────────────────────────────────────────────────────────────

@Composable
private fun AttendanceMapSection(
    markers: List<MapMarker>,
    expanded: Boolean,
    /** When false, the map collapses to 0 height (used on the Leave tab so it
     *  can claim the full screen). The composable stays mounted so the
     *  underlying AndroidView (OSM) is never re-attached during page swipes. */
    visible: Boolean,
    onToggleExpand: () -> Unit,
) {
    val mapShape    = RoundedCornerShape(22.dp)
    val borderBrush = Brush.linearGradient(listOf(Brand, Color(0xFF1565C0), Color(0xFF0D1B6E)))

    // Height target: collapsed → 0, otherwise compact (210dp) or expanded (430dp).
    val mapHeight by animateDpAsState(
        targetValue   = when {
            !visible -> 0.dp
            expanded -> 430.dp
            else     -> 210.dp
        },
        animationSpec = tween(380, easing = FastOutSlowInEasing),
        label         = "mapHeight",
    )
    // Top/bottom padding collapses too so we don't leave a visible gap when
    // the map is hidden on the Leave tab.
    val verticalPad by animateDpAsState(
        targetValue   = if (visible) 12.dp else 0.dp,
        animationSpec = tween(380, easing = FastOutSlowInEasing),
        label         = "mapVerticalPad",
    )

    Box(
        modifier = Modifier
            .padding(horizontal = 20.dp)
            .padding(top = verticalPad)
            .fillMaxWidth()
            .height(mapHeight)
            .shadow(16.dp, mapShape, spotColor = Brand.copy(alpha = 0.2f))
            .clip(mapShape)
            .background(borderBrush)
            .padding(1.5.dp)
            .clip(RoundedCornerShape(20.5.dp))
            .background(Color(0xFFE8F0FE)),
    ) {
        com.example.uniwattelektrik.feature.admin.presentation.components.AttendanceMap(
            markers  = markers,
            modifier = Modifier.fillMaxSize(),
        )
        if (markers.isEmpty()) {
            Box(
                modifier            = Modifier.fillMaxSize().background(Color(0x99F8FAFF)),
                contentAlignment    = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("📍", fontSize = 32.sp)
                    Text("No GPS pings for this view", color = InkSecondary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                }
            }
        }
        // Pin-count overlay (top-LEFT). Moved off the top-right to leave that
        // corner clean for the Streets / Satellite / Terrain style switcher
        // rendered inside the embedded AttendanceMap.
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(10.dp)
                .clip(RoundedCornerShape(50))
                .background(Color(0xCC1A1A2E))
                .padding(horizontal = 10.dp, vertical = 5.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Icon(Icons.Outlined.Map, null, tint = Color.White, modifier = Modifier.size(12.dp))
                Text("${markers.size} pins", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }

        // Expand/collapse pill (BOTTOM-RIGHT). Toggles map height in-place
        // between compact and expanded. Lives opposite the "View full map"
        // pill (bottom-left, inside AttendanceMap) and out of the way of the
        // style switcher (top-right).
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(10.dp)
                .shadow(6.dp, RoundedCornerShape(50), spotColor = Color(0x661A1A2E))
                .clip(RoundedCornerShape(50))
                .background(Color(0xE61A1A2E))
                .clickable(onClick = onToggleExpand)
                .padding(horizontal = 10.dp, vertical = 5.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Icon(
                    imageVector        = if (expanded) Icons.Filled.UnfoldLess else Icons.Filled.UnfoldMore,
                    contentDescription = if (expanded) "Collapse map" else "Expand map",
                    tint               = Color.White,
                    modifier           = Modifier.size(12.dp),
                )
                Text(
                    text       = if (expanded) "Collapse" else "Expand",
                    color      = Color.White,
                    fontSize   = 10.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

// ─── 6. Employee Card ──────────────────────────────────────────────────────────

@Composable
private fun EmployeeAttendanceCard(
    item: EmployeeAttendanceUi,
    onClick: () -> Unit,
) {
    val (statusColor, statusBg, statusLabel) = when (item.status) {
        "ON_TIME" -> Triple(Success, SuccessBg, "On Time")
        "LATE"    -> Triple(Warning, WarningBg, "Late")
        else      -> Triple(Danger,  DangerBg,  "Absent")
    }

    Row(
        modifier = Modifier
            .padding(horizontal = 20.dp)
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(18.dp), spotColor = ShadowSoft)
            .clip(RoundedCornerShape(18.dp))
            .background(CardBg)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Avatar
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(statusBg),
            contentAlignment = Alignment.Center,
        ) {
            Text(item.initials, color = statusColor, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
        }
        Spacer(Modifier.width(14.dp))

        // Info
        Column(modifier = Modifier.weight(1f)) {
            Text(item.name, color = InkPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(2.dp))
            Text(item.department, color = InkSecondary, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (item.checkInMs != null) {
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(Icons.Outlined.AccessTime, null, tint = InkMuted, modifier = Modifier.size(12.dp))
                    Text(
                        buildString {
                            append("In: ${formatHm(item.checkInMs)}")
                            if (item.checkOutMs != null) append("  Out: ${formatHm(item.checkOutMs)}")
                        },
                        color    = InkMuted,
                        fontSize = 11.sp,
                    )
                }
                if (item.lateMins > 0) {
                    Text("+${item.lateMins}m late", color = Warning, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        // Status badge
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(statusBg)
                .padding(horizontal = 10.dp, vertical = 5.dp),
        ) {
            Text(statusLabel, color = statusColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
    }
}

// ─── 7. Employee Detail Bottom Sheet ──────────────────────────────────────────

@Composable
private fun EmployeeDetailSheet(
    item: EmployeeAttendanceUi,
    onDismiss: () -> Unit,
) {
    val (statusColor, statusBg, statusLabel) = when (item.status) {
        "ON_TIME" -> Triple(Success, SuccessBg, "On Time")
        "LATE"    -> Triple(Warning, WarningBg, "Late")
        else      -> Triple(Danger,  DangerBg,  "Absent")
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(bottom = 36.dp),
    ) {
        // Handle bar
        Box(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .width(40.dp)
                .height(4.dp)
                .clip(RoundedCornerShape(50))
                .background(Color(0xFFE5E7EB))
        )
        Spacer(Modifier.height(20.dp))

        // Employee avatar + name
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(CircleShape)
                    .background(statusBg),
                contentAlignment = Alignment.Center,
            ) {
                Text(item.initials, color = statusColor, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
            }
            Column {
                Text(item.name, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = InkPrimary)
                Text(item.role, fontSize = 13.sp, color = InkSecondary)
                Spacer(Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(statusBg)
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                ) {
                    Text(statusLabel, color = statusColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        // Detail rows
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(Color(0xFFF9FAFB))
                .padding(16.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                DetailRow(Icons.Outlined.People,        "Department",    item.department)
                if (item.checkInMs != null)
                    DetailRow(Icons.Filled.Schedule,    "Check-in",      formatHm(item.checkInMs))
                if (item.checkOutMs != null)
                    DetailRow(Icons.Filled.Timer,       "Check-out",     formatHm(item.checkOutMs))
                if (item.shiftDuration != null)
                    DetailRow(Icons.Outlined.AccessTime, "Working hrs",  item.shiftDuration)
                if (item.checkInLat != null && item.checkInLng != null)
                    DetailRow(Icons.Filled.LocationOn,  "GPS location",
                        "${fmt4(item.checkInLat)}, ${fmt4(item.checkInLng)}")
                if (item.lateMins > 0)
                    DetailRow(Icons.Filled.Schedule,    "Late by",       "${item.lateMins} minutes", tint = Warning)
                if (item.status == "ABSENT")
                    DetailRow(Icons.Filled.Close,       "Status",        "Not checked in today", tint = Danger)
            }
        }
    }
}

@Composable
private fun DetailRow(
    icon: ImageVector,
    label: String,
    value: String,
    tint: Color = Brand,
) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(tint.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, null, tint = tint, modifier = Modifier.size(18.dp))
        }
        Column {
            Text(label, fontSize = 11.sp, color = InkMuted, fontWeight = FontWeight.Medium)
            Text(value, fontSize = 14.sp, color = InkPrimary, fontWeight = FontWeight.SemiBold)
        }
    }
}

// ─── Helpers ──────────────────────────────────────────────────────────────────

private fun formatHm(epochMs: Long): String {
    val ldt = Instant.fromEpochMilliseconds(epochMs).toLocalDateTime(TimeZone.currentSystemDefault())
    val hh  = ldt.hour.toString().padStart(2, '0')
    val mm  = ldt.minute.toString().padStart(2, '0')
    return "$hh:$mm"
}

private fun dayAbbrev(name: String) = when (name.uppercase()) {
    "MONDAY"    -> "Mon"; "TUESDAY"  -> "Tue"; "WEDNESDAY" -> "Wed"
    "THURSDAY"  -> "Thu"; "FRIDAY"   -> "Fri"; "SATURDAY"  -> "Sat"
    "SUNDAY"    -> "Sun"; else       -> name.take(3)
}

/** Short month name (e.g. "May", "December"). KMP-safe — does not rely on JVM's
 *  `Month.getDisplayName` which is unavailable on Kotlin/Native. */
private fun monthName(name: String): String = when (name.uppercase()) {
    "JANUARY"   -> "January"
    "FEBRUARY"  -> "February"
    "MARCH"     -> "March"
    "APRIL"     -> "April"
    "MAY"       -> "May"
    "JUNE"      -> "June"
    "JULY"      -> "July"
    "AUGUST"    -> "August"
    "SEPTEMBER" -> "September"
    "OCTOBER"   -> "October"
    "NOVEMBER"  -> "November"
    "DECEMBER"  -> "December"
    else        -> name.lowercase().replaceFirstChar { it.uppercase() }
}

private fun initials(name: String): String =
    name.split(" ").take(2).mapNotNull { it.firstOrNull()?.uppercaseChar() }.joinToString("")
        .ifEmpty { name.take(2).uppercase() }

/**
 * KMP-friendly fixed-precision formatter for `Double` values.
 *
 * `String.format` / `"%.4f".format(...)` is JVM-only and unavailable in
 * Kotlin/Native (iOS). This rounds half-away-from-zero to 4 decimal places
 * using only common stdlib primitives.
 */
private fun fmt4(value: Double): String {
    val scaled = kotlin.math.round(value * 10000.0).toLong()
    val sign   = if (scaled < 0) "-" else ""
    val abs    = kotlin.math.abs(scaled)
    val whole  = abs / 10000
    val frac   = (abs % 10000).toString().padStart(4, '0')
    return "$sign$whole.$frac"
}
