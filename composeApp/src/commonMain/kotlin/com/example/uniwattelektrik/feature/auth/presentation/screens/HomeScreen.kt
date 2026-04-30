package com.example.uniwattelektrik.feature.auth.presentation.screens

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.uniwattelektrik.feature.auth.domain.model.User
import com.example.uniwattelektrik.feature.auth.presentation.components.PremiumCheckInCard
import com.example.uniwattelektrik.feature.auth.presentation.components.PremiumGradientBackground
import com.example.uniwattelektrik.feature.auth.presentation.components.StatusIndicator
import com.example.uniwattelektrik.feature.auth.presentation.state.AuthUiEvent
import com.example.uniwattelektrik.feature.auth.presentation.viewmodel.AuthViewModel
import com.example.uniwattelektrik.feature.workforce.data.remote.TaskRecord
import com.example.uniwattelektrik.feature.workforce.presentation.WorkforceViewModel
import com.example.uniwattelektrik.platform.LocationData
import com.example.uniwattelektrik.platform.LocationProvider
import com.example.uniwattelektrik.platform.currentTimeFormatted
import kotlinx.coroutines.launch

// ─────────────────────────────────────────────────────────────────────────────
//  DATA MODELS  (drives every dynamic value rendered on this screen)
// ─────────────────────────────────────────────────────────────────────────────

data class HomeUiState(
    val profile: UserProfile,
    val checkIn: CheckInState,
    val stats: List<StatItem>,
    val tasks: List<Task>,
    val totalTaskCount: Int,
    val notificationCount: Int,
)

data class UserProfile(
    val initials: String,
    val greeting: String,
    val name: String,
    val role: String,
    val avatarGradient: List<Color>,
)

data class CheckInState(
    val isCheckedIn: Boolean,
    val location: String,
    val time: String,             // e.g. "09:24 AM"
    val shiftInfo: String,        // e.g. "Shift 1 · 09:00 – 17:00 · 7h 36m remaining"
    val gpsText: String,          // e.g. "GPS locked · 13.0827°N, 77.5877°E · accuracy 4m"
)

data class StatItem(
    val value: String,
    val label: String,
    val accent: StatAccent,
)

enum class StatAccent(val color: Color) {
    Active(Color(0xFF1E73E8)),
    Done(Color(0xFF22C55E)),
    Critical(Color(0xFFEF4444)),
}

data class Task(
    val id: String,
    val title: String,
    val location: String,
    val time: String,
    val day: String,
    val priority: TaskPriority,
)

enum class TaskPriority(val color: Color) {
    Critical(Color(0xFFEF4444)),
    High(Color(0xFFF59E0B)),
    Normal(Color(0xFF1E73E8)),
    Low(Color(0xFF22C55E)),
}

// ─────────────────────────────────────────────────────────────────────────────
//  PALETTE
// ─────────────────────────────────────────────────────────────────────────────

private val TextDark    = Color(0xFF0A1F44)
private val TextGray    = Color(0xFF6B7A99)
private val TextMuted   = Color(0xFF9AA3B5)
private val CardWhite   = Color(0xFFFFFFFF)
private val BlueAccent  = Color(0xFF1E73E8)
private val DeepBlue    = Color(0xFF0A3D91)
private val GreenStatus = Color(0xFF22C55E)

// ─────────────────────────────────────────────────────────────────────────────
//  ENTRY POINT  (wires AuthViewModel → dynamic state)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun HomeScreen(
    user: User,
    viewModel: AuthViewModel,
    workforceVm: WorkforceViewModel? = null,
    onOpenNotifications: () -> Unit = {},
    onOpenTasks: () -> Unit = {},
) {
    // Initial state derived from the signed-in user. In production this would come
    // from a HomeViewModel + repository; for now we seed with realistic data and
    // let the UI mutate it locally as location/time updates arrive.
    val initial = remember(user) { defaultHomeState(user) }
    var state       by remember { mutableStateOf(initial) }

    // Live tasks scoped by userId — overrides the seeded sample tasks once available.
    val liveTasks by (workforceVm?.tasks?.collectAsStateWithLifecycle()
        ?: remember { kotlinx.coroutines.flow.MutableStateFlow(emptyList<
                TaskRecord>()) }
            .collectAsStateWithLifecycle())

    val locationProvider = remember { LocationProvider() }
    val scope            = rememberCoroutineScope()

    // Track today's open attendance doc id so check-out can target the same record.
    // Sourced from the live `attendance` flow (set when admin/user is subscribed).
    val attendance by (workforceVm?.attendance?.collectAsStateWithLifecycle()
        ?: remember { kotlinx.coroutines.flow.MutableStateFlow(emptyList<
                com.example.uniwattelektrik.feature.workforce.data.remote.AttendanceRecord>()) }
            .collectAsStateWithLifecycle())
    // Re-derived every minute via [nowMs] so an unclosed record from a prior
    // day does NOT keep the toggle stuck on "Checked In" forever. We only
    // consider records whose check-in falls inside today's local window.
    val openAttendanceId: String? = run {
        val nowLocal = com.example.uniwattelektrik.platform.nowEpochMillis()
        val todayStart = nowLocal -
            com.example.uniwattelektrik.platform.minutesOfDay(nowLocal) * 60_000L
        val todayEnd = todayStart + 24L * 60L * 60L * 1000L
        attendance
            .firstOrNull {
                it.userId == user.id &&
                    it.checkOutMs == null &&
                    it.checkInMs in todayStart until todayEnd
            }
            ?.id
    }
    // Source of truth for the toggle is the live attendance flow, not local
    // state. Derive it on every render so navigating away/back never resets
    // the UI to "checked-out" while the snapshot listener re-warms. A tiny
    // optimistic override keeps the slider feeling instant on tap until the
    // snapshot listener catches up; it auto-clears once server agrees.
    val isCheckedInServer = openAttendanceId != null
    var optimisticToggle by remember { mutableStateOf<Boolean?>(null) }
    LaunchedEffect(isCheckedInServer, optimisticToggle) {
        if (optimisticToggle == isCheckedInServer) optimisticToggle = null
    }
    val isCheckedIn = optimisticToggle ?: isCheckedInServer

    // Resolve this employee's shift from the employees flow.
    val employees by (workforceVm?.employees?.collectAsStateWithLifecycle()
        ?: remember { kotlinx.coroutines.flow.MutableStateFlow(emptyList<
                com.example.uniwattelektrik.feature.workforce.data.remote.EmployeeRecord>()) }
            .collectAsStateWithLifecycle())
    val myShift: String = remember(employees, user.id) {
        employees.firstOrNull { it.id == user.id }?.shift ?: "Shift1"
    }

    // Live shift label with countdown — updates every minute while the screen is alive.
    var nowMs by remember { mutableStateOf(com.example.uniwattelektrik.platform.nowEpochMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            nowMs = com.example.uniwattelektrik.platform.nowEpochMillis()
            kotlinx.coroutines.delay(60_000L)
        }
    }
    LaunchedEffect(myShift, nowMs) {
        val info = buildShiftInfo(myShift, nowMs)
        if (state.checkIn.shiftInfo != info) {
            state = state.copy(checkIn = state.checkIn.copy(shiftInfo = info))
        }
    }

    // Fetch location once on screen entry so the card shows where the user
    // *currently* is even before they toggle anything. Falls back to the seed
    // values if permission is denied or location services are off.
    LaunchedEffect(Unit) {
        val loc = locationProvider.getCurrentLocation()
        if (loc != null) {
            state = state.copy(
                checkIn = state.checkIn.copy(
                    location = loc.placeName ?: loc.coordsText,
                    gpsText  = "GPS locked · ${loc.coordsText} · ${loc.accuracyText}",
                ),
            )
        } else {
            state = state.copy(
                checkIn = state.checkIn.copy(
                    location = "Location unavailable",
                    gpsText  = "GPS unavailable — check permissions",
                ),
            )
        }
    }

    // Render-time override: the toggle always reflects server truth (or the
    // brief optimistic override). Local state still drives time/location/shift
    // so the rest of the card stays smooth across re-entries.
    val renderState = state.copy(
        checkIn = state.checkIn.copy(isCheckedIn = isCheckedIn),
    )

    HomeScaffold(
        state       = renderState,
        onCheckInToggle = { newCheckedIn ->
            // Capture the moment-of-toggle: time is "now", location is freshly fetched.
            // Optimistic toggle override flips the slider instantly; it auto-clears
            // once the Firestore snapshot listener confirms the server state.
            optimisticToggle = newCheckedIn
            val nowTime = currentTimeFormatted()
            state = state.copy(
                checkIn = state.checkIn.copy(
                    time = nowTime,
                ),
            )
            // Persist to Firestore so the admin attendance screen can see it
            // in real time. Requires the user's owning admin id.
            val adminUid = user.parentAdminId
            if (workforceVm == null || adminUid.isNullOrBlank()) {
                com.example.uniwattelektrik.core.AppLog.w(
                    "HomeScreen",
                    "skip attendance write: workforceVm=${workforceVm != null} parentAdminId=$adminUid",
                )
            } else {
                // GPS-aware check-in/out: fetch the location FIRST, then classify
                // ON_TIME/LATE against the user's shift, then persist with coords.
                scope.launch {
                    val loc: LocationData? = locationProvider.getCurrentLocation()

                    // Update UI with the fresh fix (or fallback strings).
                    state = state.copy(
                        checkIn = state.checkIn.copy(
                            location = loc?.placeName ?: loc?.coordsText ?: "Location unavailable",
                            gpsText  = if (loc != null) "GPS locked · ${loc.coordsText} · ${loc.accuracyText}"
                                       else "GPS unavailable — check permissions",
                        ),
                    )

                    if (newCheckedIn) {
                        // Classify against the employee's shift (Shift1/Shift2).
                        val shiftCfg     = com.example.uniwattelektrik.feature.workforce.domain
                            .ShiftConfig.fromShiftKey(myShift)
                        val nowMillis    = com.example.uniwattelektrik.platform.nowEpochMillis()
                        val todayStartMs = nowMillis -
                            com.example.uniwattelektrik.platform.minutesOfDay(nowMillis) * 60_000L
                        val checkInStatus = com.example.uniwattelektrik.feature.workforce.domain
                            .classifyCheckIn(nowMillis, todayStartMs, shiftCfg)

                        workforceVm.markCheckIn(
                            adminUid     = adminUid,
                            userId       = user.id,
                            lat          = loc?.latitude,
                            lng          = loc?.longitude,
                            checkInStatus = checkInStatus,
                        ) { result ->
                            result.onFailure {
                                com.example.uniwattelektrik.core.AppLog.e(
                                    "HomeScreen", "markCheckIn FAILED: ${it.message}", it,
                                )
                            }.onSuccess {
                                com.example.uniwattelektrik.core.AppLog.i(
                                    "HomeScreen",
                                    "markCheckIn ok id=${it.id} status=$checkInStatus " +
                                        "loc=${loc?.coordsText ?: "n/a"}",
                                )
                            }
                        }

                        // Best-effort GPS ping in the /checkins collection so the
                        // admin map can dot the live location alongside attendance.
                        if (loc != null) {
                            workforceVm.recordCheckinPing(
                                adminUid  = adminUid,
                                userId    = user.id,
                                latitude  = loc.latitude,
                                longitude = loc.longitude,
                            )
                        }
                    } else {
                        // Check-out: update the open record with coords + timestamp.
                        openAttendanceId?.let { id ->
                            workforceVm.markCheckOut(
                                adminUid     = adminUid,
                                userId       = user.id,
                                attendanceId = id,
                                lat          = loc?.latitude,
                                lng          = loc?.longitude,
                            ) { result ->
                                result.onFailure {
                                    com.example.uniwattelektrik.core.AppLog.e(
                                        "HomeScreen", "markCheckOut FAILED: ${it.message}", it,
                                    )
                                }.onSuccess {
                                    com.example.uniwattelektrik.core.AppLog.i(
                                        "HomeScreen",
                                        "markCheckOut ok id=$id loc=${loc?.coordsText ?: "n/a"}",
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        onAvatarClick    = { viewModel.onEvent(AuthUiEvent.Logout) }, // dev shortcut → profile tab logout normally
        onBellClick      = onOpenNotifications,
        onTaskClick      = { onOpenTasks() },
        onViewAllTasks   = onOpenTasks,
    )
}

// ─────────────────────────────────────────────────────────────────────────────
//  SCAFFOLD
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun HomeScaffold(
    state: HomeUiState,
    onCheckInToggle: (Boolean) -> Unit,
    onAvatarClick: () -> Unit,
    onBellClick: () -> Unit,
    onTaskClick: (Task) -> Unit,
    onViewAllTasks: () -> Unit,
) {
    PremiumGradientBackground {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start  = 20.dp,
                end    = 20.dp,
                top    = 16.dp,
                bottom = 100.dp,    // leaves room for the shell's bottom nav
            ),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            item {
                HeaderSection(
                    profile           = state.profile,
                    notificationCount = state.notificationCount,
                    onAvatarClick     = onAvatarClick,
                    onBellClick       = onBellClick,
                )
            }
            item {
                CheckInCard(
                    state    = state.checkIn,
                    onToggle = onCheckInToggle,
                )
            }
            item { StatsRow(stats = state.stats) }
            item {
                TaskSection(
                    tasks       = state.tasks,
                    totalCount  = state.totalTaskCount,
                    onTaskClick = onTaskClick,
                    onViewAll   = onViewAllTasks,
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  HEADER
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun HeaderSection(
    profile: UserProfile,
    notificationCount: Int,
    onAvatarClick: () -> Unit,
    onBellClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier              = modifier.fillMaxWidth(),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        // Avatar — gradient rounded square
        Box(
            modifier = Modifier
                .size(56.dp)
                .shadow(
                    elevation = 10.dp,
                    shape     = RoundedCornerShape(16.dp),
                    spotColor = Color(0x40EC8552),
                )
                .clip(RoundedCornerShape(16.dp))
                .background(Brush.linearGradient(profile.avatarGradient))
                .clickable(onClick = onAvatarClick),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                profile.initials,
                color         = Color.White,
                fontSize      = 18.sp,
                fontWeight    = FontWeight.Bold,
                letterSpacing = 0.5.sp,
            )
        }

        // Greeting + name + role
        Column(modifier = Modifier.weight(1f)) {
            Text(
                profile.greeting,
                color    = TextGray,
                fontSize = 13.sp,
            )
            Text(
                profile.name,
                color      = TextDark,
                fontSize   = 20.sp,
                fontWeight = FontWeight.Bold,
                maxLines   = 1,
                overflow   = TextOverflow.Ellipsis,
            )
            Text(
                "// ${profile.role}",
                color         = TextMuted,
                fontSize      = 11.sp,
                fontFamily    = FontFamily.Monospace,
                letterSpacing = 0.4.sp,
                maxLines      = 2,
                overflow      = TextOverflow.Ellipsis,
            )
        }

        // Notification bell — rounded square + red dot
        Box(
            modifier = Modifier
                .size(48.dp)
                .shadow(
                    elevation = 6.dp,
                    shape     = RoundedCornerShape(14.dp),
                    spotColor = Color(0x140A1F44),
                )
                .clip(RoundedCornerShape(14.dp))
                .background(Color.White)
                .clickable(onClick = onBellClick),
            contentAlignment = Alignment.Center,
        ) {
            // Emoji-style golden bell — renders colorful by default, no tint
            Text(
                text     = "🔔",   // 🔔
                fontSize = 22.sp,
            )
            if (notificationCount > 0) {
                Box(
                    modifier = Modifier
                        .size(9.dp)
                        .align(Alignment.TopEnd)
                        .offset(x = (-10).dp, y = 10.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFEF4444))
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  CHECK-IN CARD
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun CheckInCard(
    state: CheckInState,
    onToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    PremiumCheckInCard(
        modifier = modifier,
        gradientStart = BlueAccent,
        gradientEnd = DeepBlue,
    ) {
        Column(
            modifier             = Modifier.fillMaxWidth().padding(22.dp),
            verticalArrangement  = Arrangement.spacedBy(12.dp),
        ) {
            // Status row — pulsing dot + animated text via StatusIndicator
            StatusIndicator(
                isCheckedIn  = state.isCheckedIn,
                activeText   = "Checked-in · ${state.location}",
                inactiveText = "Checked-out · ${state.location}",
            )

            // Time — split AM/PM into smaller suffix
            Row(
                verticalAlignment     = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                val parts  = state.time.split(" ")
                val digits = parts.firstOrNull().orEmpty()
                val suffix = parts.drop(1).joinToString(" ")
                Text(
                    digits,
                    color         = Color.White,
                    fontSize      = 38.sp,
                    fontWeight    = FontWeight.Bold,
                    letterSpacing = (-1).sp,
                )
                if (suffix.isNotBlank()) {
                    Text(
                        suffix,
                        color      = Color.White.copy(alpha = 0.85f),
                        fontSize   = 18.sp,
                        fontWeight = FontWeight.Medium,
                        modifier   = Modifier.padding(bottom = 6.dp),
                    )
                }
            }

            Text(
                state.shiftInfo,
                color    = Color.White.copy(alpha = 0.85f),
                fontSize = 13.sp,
            )

            Spacer(Modifier.height(2.dp))

            CheckInToggle(
                isCheckedIn = state.isCheckedIn,
                onToggle    = onToggle,
            )

            // GPS pill
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.White.copy(alpha = 0.16f))
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Icon(
                    imageVector        = Icons.Filled.LocationOn,
                    contentDescription = null,
                    tint               = Color.White,
                    modifier           = Modifier.size(18.dp),
                )
                Text(
                    state.gpsText,
                    color    = Color.White,
                    fontSize = 13.sp,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun CheckInToggle(
    isCheckedIn: Boolean,
    onToggle: (Boolean) -> Unit,
) {
    val containerShape = RoundedCornerShape(14.dp)
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(containerShape)
            .background(Color.White.copy(alpha = 0.18f))
            .padding(4.dp),
    ) {
        val halfWidth = maxWidth / 2
        val offsetX   by animateDpAsState(
            targetValue   = if (isCheckedIn) 0.dp else halfWidth,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness    = Spring.StiffnessMediumLow,
            ),
            label = "checkin-slider",
        )
        // Sliding white pill
        Box(
            modifier = Modifier
                .offset(x = offsetX)
                .width(halfWidth)
                .fillMaxHeight()
                .shadow(
                    elevation = 4.dp,
                    shape     = RoundedCornerShape(10.dp),
                    spotColor = Color(0x301E73E8),
                )
                .clip(RoundedCornerShape(10.dp))
                .background(Color.White)
        )
        Row(Modifier.fillMaxSize()) {
            CheckInTab(
                label    = "Checked In",
                selected = isCheckedIn,
                modifier = Modifier.weight(1f),
                onClick  = { onToggle(true) },
            )
            CheckInTab(
                label    = "Check Out",
                selected = !isCheckedIn,
                modifier = Modifier.weight(1f),
                onClick  = { onToggle(false) },
            )
        }
    }
}

@Composable
private fun CheckInTab(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            color      = if (selected) TextDark else Color.White.copy(alpha = 0.85f),
            fontSize   = 14.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  STATS
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun StatsRow(
    stats: List<StatItem>,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier              = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        stats.forEach { stat ->
            StatsCard(stat = stat, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
fun StatsCard(
    stat: StatItem,
    modifier: Modifier = Modifier,
) {
    val cardShape = RoundedCornerShape(20.dp)
    Column(
        modifier = modifier
            .shadow(
                elevation = 12.dp,
                shape     = cardShape,
                spotColor = Color(0x140A1F44),
            )
            .clip(cardShape)
            .background(CardWhite)
            .padding(horizontal = 14.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            stat.value,
            color         = stat.accent.color,
            fontSize      = 28.sp,
            fontWeight    = FontWeight.Bold,
            letterSpacing = (-0.5).sp,
        )
        Text(
            stat.label,
            color         = TextGray,
            fontSize      = 11.sp,
            fontWeight    = FontWeight.SemiBold,
            letterSpacing = 0.6.sp,
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  TASKS
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun TaskSection(
    tasks: List<Task>,
    totalCount: Int,
    onTaskClick: (Task) -> Unit,
    onViewAll: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier            = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier          = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "Today's Tasks",
                color      = TextDark,
                fontSize   = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier   = Modifier.weight(1f),
            )
            Text(
                "View all ($totalCount)",
                color      = BlueAccent,
                fontSize   = 14.sp,
                fontWeight = FontWeight.SemiBold,
                modifier   = Modifier.clickable(onClick = onViewAll),
            )
        }
        tasks.forEach { task ->
            TaskItem(task = task, onClick = { onTaskClick(task) })
        }
    }
}

@Composable
fun TaskItem(
    task: Task,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val cardShape = RoundedCornerShape(20.dp)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 10.dp,
                shape     = cardShape,
                spotColor = Color(0x140A1F44),
            )
            .clip(cardShape)
            .background(CardWhite)
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Coloured priority indicator strip on the left
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(58.dp)
                .clip(RoundedCornerShape(topEnd = 4.dp, bottomEnd = 4.dp))
                .background(task.priority.color)
        )
        Spacer(Modifier.width(14.dp))
        Column(
            modifier            = Modifier.weight(1f).padding(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                task.title,
                color      = TextDark,
                fontSize   = 15.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines   = 1,
                overflow   = TextOverflow.Ellipsis,
            )
            Text(
                "📍 ${task.location}",
                color    = TextGray,
                fontSize = 12.sp,
            )
        }
        Column(
            modifier            = Modifier.padding(end = 18.dp, top = 16.dp, bottom = 16.dp),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                task.time,
                color      = TextDark,
                fontSize   = 14.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                task.day,
                color    = TextGray,
                fontSize = 11.sp,
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  DEFAULT STATE  (derives initials/name from the signed-in [User])
// ─────────────────────────────────────────────────────────────────────────────

// ─────────────────────────────────────────────────────────────────────────────
//  DEFAULT STATE  (derives initials/name from the signed-in [User])
// ─────────────────────────────────────────────────────────────────────────────

/** Replace seeded tasks/stats with live Firestore tasks scoped by userId. */
private fun HomeUiState.applyLiveTasks(live: List<TaskRecord>): HomeUiState {
    if (live.isEmpty()) return this
    val mapped = live.take(3).map { rec ->
        Task(
            id       = rec.id,
            title    = rec.title,
            location = rec.location,
            time     = rec.time,
            day      = rec.day,
            priority = when (rec.priority) {
                "High"   -> TaskPriority.Critical
                "Medium" -> TaskPriority.High
                else      -> TaskPriority.Normal
            },
        )
    }
    val active   = live.count { it.status != "Done" }
    val done     = live.count { it.status == "Done" }
    val critical = live.count { it.priority == "High" && it.status != "Done" }
    return copy(
        tasks          = mapped,
        totalTaskCount = live.size,
        stats          = listOf(
            StatItem("$active",   "ACTIVE",     StatAccent.Active),
            StatItem("$done",     "DONE TODAY", StatAccent.Done),
            StatItem("$critical", "CRITICAL",   StatAccent.Critical),
        ),
    )
}

private fun defaultHomeState(user: User): HomeUiState {
    val rawName = user.displayName?.takeIf { it.isNotBlank() } ?: user.email.substringBefore("@")
    val parts   = rawName.split(" ", "_", ".", "-").filter { it.isNotBlank() }
    val cleanName = parts.joinToString(" ") { it.replaceFirstChar(Char::uppercaseChar) }
        .ifBlank { "User" }
    val initials = parts.take(2).mapNotNull { it.firstOrNull()?.uppercaseChar() }
        .joinToString("")
        .ifEmpty { rawName.take(2).uppercase() }

    return HomeUiState(
        profile = UserProfile(
            initials       = initials,
            greeting       = "Good morning,",
            name           = cleanName,
            role           = "Substation Engineer · L2",
            avatarGradient = listOf(Color(0xFFFFB28A), Color(0xFFEC8552)),
        ),
        checkIn = CheckInState(
            isCheckedIn = true,
            location    = "Locating…",
            time        = currentTimeFormatted(),
            shiftInfo   = "Shift 1 · 09:00 – 17:00",
            gpsText     = "Acquiring GPS fix…",
        ),
        stats = listOf(
            StatItem("3", "ACTIVE",     StatAccent.Active),
            StatItem("8", "DONE TODAY", StatAccent.Done),
            StatItem("1", "CRITICAL",   StatAccent.Critical),
        ),
        tasks = listOf(
            Task(
                id       = "T-001",
                title    = "Transformer oil leak — 11kV",
                location = "Yelahanka · 2.4 km",
                time     = "10:30",
                day      = "Today",
                priority = TaskPriority.Critical,
            ),
            Task(
                id       = "T-002",
                title    = "Insulator replacement — 33kV",
                location = "Whitefield · 5.1 km",
                time     = "13:00",
                day      = "Today",
                priority = TaskPriority.High,
            ),
            Task(
                id       = "T-003",
                title    = "Routine breaker inspection",
                location = "Hebbal Substation",
                time     = "15:30",
                day      = "Today",
                priority = TaskPriority.Normal,
            ),
        ),
        totalTaskCount    = 11,
        notificationCount = 3,
    )
}

// ─────────────────────────────────────────────────────────────────────────────
//  SHIFT HELPERS
// ─────────────────────────────────────────────────────────────────────────────

/** Returns shift bounds in (startHour, endHour, label). 24h. */
private fun shiftBounds(shift: String): Triple<Int, Int, String> = when (shift) {
    "Shift2" -> Triple(13, 23, "SHIFT 13:00 — 23:00")
    else     -> Triple(9, 18, "SHIFT 09:00 — 18:00")
}

/**
 * Builds the live shift info line shown on the check-in card, e.g.
 *   "SHIFT 09:00 — 18:00 · 6h 18m to go"
 *   "SHIFT 09:00 — 18:00 · starts in 1h 30m"
 *   "SHIFT 09:00 — 18:00 · ended"
 */
private fun buildShiftInfo(shift: String, nowMs: Long): String {
    val (startH, endH, label) = shiftBounds(shift)
    val mins = com.example.uniwattelektrik.platform.minutesOfDay(nowMs)
    val startMin = startH * 60
    val endMin = endH * 60
    val suffix = when {
        mins < startMin -> {
            val diff = startMin - mins
            "starts in ${diff / 60}h ${diff % 60}m"
        }
        mins in startMin until endMin -> {
            val diff = endMin - mins
            "${diff / 60}h ${diff % 60}m to go"
        }
        else -> "ended"
    }
    return "$label · $suffix"
}
