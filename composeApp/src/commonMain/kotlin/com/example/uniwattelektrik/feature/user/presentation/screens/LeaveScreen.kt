package com.example.uniwattelektrik.feature.user.presentation.screens

import com.example.uniwattelektrik.core.components.ToastController
import com.example.uniwattelektrik.core.performance.TrackScreenPerformance

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.style.TextOverflow
import com.example.uniwattelektrik.platform.nowEpochMillis
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.uniwattelektrik.core.theme.AppShapes
import com.example.uniwattelektrik.core.theme.AppTheme
import com.example.uniwattelektrik.core.theme.AppTypography
import com.example.uniwattelektrik.core.theme.SetStatusBar
import com.example.uniwattelektrik.core.theme.appScreenBackground
import com.example.uniwattelektrik.feature.workforce.data.remote.LeaveRecord
import com.example.uniwattelektrik.feature.workforce.presentation.WorkforceViewModel
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

// ─── Design tokens ────────────────────────────────────────────────────────────

private val CardBg       = AppTheme.Surface
private val InputBg      = AppTheme.SurfaceMuted
private val LabelColor   = AppTheme.Ink500
private val ValueColor   = AppTheme.Ink900
private val BorderColor  = AppTheme.Ink100

// ─── Date helpers (kotlinx.datetime, KMP-safe) ────────────────────────────────

private val MONTHS = listOf(
    "Jan","Feb","Mar","Apr","May","Jun","Jul","Aug","Sep","Oct","Nov","Dec",
)

internal fun Long.toLeaveDate(): String {
    val local = Instant.fromEpochMilliseconds(this)
        .toLocalDateTime(TimeZone.currentSystemDefault())
    return "${local.dayOfMonth} ${MONTHS[local.monthNumber - 1]} ${local.year}"
}

private fun leaveDays(fromMs: Long, toMs: Long): Int {
    if (toMs < fromMs) return 0
    return ((toMs - fromMs) / (1000L * 60 * 60 * 24) + 1).toInt()
}

// ─── Leave type metadata ──────────────────────────────────────────────────────

private data class LeaveTypeMeta(
    val key  : String,
    val emoji: String,
    val label: String,
)

private val LEAVE_TYPES = listOf(
    LeaveTypeMeta("Casual", "☕", "Casual"),
    LeaveTypeMeta("Sick",   "🤒", "Sick"),
    LeaveTypeMeta("Earned", "🌴", "Earned"),
)

private val LEAVE_BALANCES = mapOf(
    "Casual" to 12,
    "Sick"   to 10,
    "Earned" to 18,
)

// ─── Tab ─────────────────────────────────────────────────────────────────────

private enum class LeaveTab { Apply, History, Balance }

/* ═══════════════════════════════════════════════════════════════════════════
 *  LeaveScreen
 * ═══════════════════════════════════════════════════════════════════════════ */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeaveScreen(
    workforceVm  : WorkforceViewModel,
    userId       : String,
    adminId      : String,
    employeeName : String,
    department   : String = "",
    modifier     : Modifier = Modifier,
    onBack       : () -> Unit = {},
) {
    TrackScreenPerformance("LeaveScreen")
    val leaveRequests by workforceVm.leaveRequests.collectAsStateWithLifecycle()
    val actionInProgress by workforceVm.actionInProgress.collectAsStateWithLifecycle()

    SetStatusBar(color = AppTheme.Bg, darkIcons = true)

    // ── Apply-form bottom sheet state ───────────────────────────────────
    var applyOpen by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // ── "All" toggle — collapses to 3, expands to all ───────────────────
    var showAll by remember { mutableStateOf(false) }

    // ── Balance maths: used = sum of approved totalDays per type ────────
    val usedByType = remember(leaveRequests) {
        leaveRequests
            .filter { it.status.equals("approved", ignoreCase = true) }
            .groupBy { it.leaveType }
            .mapValues { (_, list) -> list.sumOf { it.totalDays } }
    }

    // FY label — Apr→Mar Indian fiscal-year window.
    val fyLabel = remember {
        val now = Instant.fromEpochMilliseconds(nowEpochMillis())
            .toLocalDateTime(TimeZone.currentSystemDefault())
        val fyStart = if (now.monthNumber >= 4) now.year else now.year - 1
        "FY $fyStart"
    }

    Box(modifier = modifier.fillMaxSize()) {
    Column(modifier = Modifier.fillMaxSize().background(appScreenBackground())) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(horizontal = AppTheme.SpLg, vertical = AppTheme.SpMd),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppTheme.SpMd),
        ) {
            // Back chip
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .shadow(2.dp, RoundedCornerShape(12.dp), spotColor = Color(0x14000000))
                    .clip(RoundedCornerShape(12.dp))
                    .background(AppTheme.Surface)
                    .border(1.dp, AppTheme.Ink100, RoundedCornerShape(12.dp))
                    .clickable(onClick = onBack),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint               = AppTheme.Ink900,
                    modifier           = Modifier.size(20.dp),
                )
            }

            // Title block
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    fyLabel,
                    style = AppTypography.labelSmall.copy(
                        color         = AppTheme.Ink500,
                        letterSpacing = 1.4.sp,
                    ),
                )
                Text(
                    "Leave",
                    style = AppTypography.displayMedium.copy(color = AppTheme.Ink900),
                )
            }

            // + Apply CTA
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(
                        Brush.horizontalGradient(listOf(AppTheme.Brand, AppTheme.Brand700))
                    )
                    .clickable { applyOpen = true }
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Icon(
                    Icons.Filled.Add,
                    contentDescription = null,
                    tint               = Color.White,
                    modifier           = Modifier.size(16.dp),
                )
                Text(
                    "Apply",
                    style = AppTypography.labelLarge.copy(
                        color      = Color.White,
                        fontWeight = FontWeight.Bold,
                    ),
                )
            }
        }

        // ── Body (scrollable) ──────────────────────────────────────────
        LazyColumn(
            modifier            = Modifier.fillMaxSize(),
            contentPadding      = PaddingValues(
                start  = AppTheme.SpLg,
                end    = AppTheme.SpLg,
                top    = AppTheme.SpSm,
                bottom = 110.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(AppTheme.SpMd),
        ) {
            // 1. Balance cards row.
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    LeaveBalanceCard(
                        modifier  = Modifier.weight(1f),
                        label     = "Casual",
                        used      = usedByType["Casual"] ?: 0,
                        total     = LEAVE_BALANCES["Casual"] ?: 0,
                        gradient  = listOf(Color(0xFF60A5FA), Color(0xFF1D4ED8)),
                    )
                    LeaveBalanceCard(
                        modifier  = Modifier.weight(1f),
                        label     = "Sick",
                        used      = usedByType["Sick"] ?: 0,
                        total     = LEAVE_BALANCES["Sick"] ?: 0,
                        gradient  = listOf(Color(0xFFFBBF24), Color(0xFFD97706)),
                    )
                    LeaveBalanceCard(
                        modifier  = Modifier.weight(1f),
                        label     = "Earned",
                        used      = usedByType["Earned"] ?: 0,
                        total     = LEAVE_BALANCES["Earned"] ?: 0,
                        gradient  = listOf(Color(0xFF34D399), Color(0xFF059669)),
                    )
                }
            }

            // 2. Section header.
            item {
                Spacer(Modifier.height(AppTheme.SpSm))
                Row(
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier              = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        "Recent applications",
                        style = AppTypography.titleLarge.copy(
                            color      = AppTheme.Ink900,
                            fontWeight = FontWeight.ExtraBold,
                        ),
                    )
                    if (leaveRequests.size > 3) {
                        Text(
                            text     = if (showAll) "Less ←" else "All →",
                            style    = AppTypography.labelLarge.copy(
                                color      = AppTheme.Brand,
                                fontWeight = FontWeight.Bold,
                            ),
                            modifier = Modifier.clickable { showAll = !showAll },
                        )
                    }
                }
            }

            // 3. Application list.
            val visible = leaveRequests
                .sortedByDescending { it.fromDateMs }
                .let { if (showAll) it else it.take(3) }

            if (visible.isEmpty()) {
                item {
                    Box(
                        modifier         = Modifier
                            .fillMaxWidth()
                            .clip(AppShapes.card)
                            .background(AppTheme.Surface)
                            .padding(28.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            "No leave applications yet — tap + Apply to start.",
                            style = AppTypography.bodyMedium.copy(color = AppTheme.Ink500),
                        )
                    }
                }
            } else {
                items(visible, key = { it.id }) { req ->
                    LeaveApplicationCard(req)
                }
            }
        }
    }

    // ── Apply form bottom sheet ─────────────────────────────────────────
    if (applyOpen) {
        ModalBottomSheet(
            onDismissRequest = { applyOpen = false },
            sheetState       = sheetState,
            containerColor   = AppTheme.Bg,
            shape            = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        ) {
            LeaveApplyTab(
                workforceVm  = workforceVm,
                userId       = userId,
                adminId      = adminId,
                employeeName = employeeName,
                department   = department,
                onSubmitted  = { applyOpen = false },
            )
        }
    }
        com.example.uniwattelektrik.core.components.LoadingOverlay(
            visible = actionInProgress,
            message = "Submitting…",
        )
    }
}

/* ─── Balance card (gradient tile with decorative bubbles) ─────────────── */

@Composable
private fun LeaveBalanceCard(
    label   : String,
    used    : Int,
    total   : Int,
    gradient: List<Color>,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .height(104.dp)
            .shadow(8.dp, RoundedCornerShape(20.dp), spotColor = gradient.last().copy(alpha = 0.35f))
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.linearGradient(gradient),
            )
            .padding(14.dp),
    ) {
        // Decorative translucent bubbles (top-right premium feel)
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(64.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.10f)),
        )
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 22.dp, end = 22.dp)
                .size(36.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.08f)),
        )

        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                label,
                style = AppTypography.labelLarge.copy(
                    color      = Color.White,
                    fontWeight = FontWeight.SemiBold,
                ),
            )
            Text(
                used.toString().padStart(2, '0'),
                style = AppTypography.displayLarge.copy(
                    color         = Color.White,
                    fontWeight    = FontWeight.ExtraBold,
                    fontSize      = 30.sp,
                    letterSpacing = (-0.6).sp,
                ),
            )
            Text(
                "/ $total days",
                style = AppTypography.bodySmall.copy(
                    color = Color.White.copy(alpha = 0.85f),
                ),
            )
        }
    }
}

/* ─── Recent application card ──────────────────────────────────────────── */

@Composable
private fun LeaveApplicationCard(req: LeaveRecord) {
    val (title, body) = remember(req.reason) { splitReason(req.reason, req.leaveType) }
    val statusColor = when (req.status.lowercase()) {
        "approved" -> AppTheme.Success
        "rejected" -> AppTheme.Danger
        else       -> AppTheme.Warning
    }
    val statusBg = when (req.status.lowercase()) {
        "approved" -> AppTheme.SuccessBg
        "rejected" -> AppTheme.DangerBg
        else       -> AppTheme.WarningBg
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, AppShapes.card, spotColor = Color(0x14000000))
            .clip(AppShapes.card)
            .background(AppTheme.Surface)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            verticalAlignment     = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier              = Modifier.fillMaxWidth(),
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text     = title,
                    style    = AppTypography.titleSmall.copy(
                        color      = AppTheme.Ink900,
                        fontWeight = FontWeight.Bold,
                    ),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text  = "${formatRange(req.fromDateMs, req.toDateMs)}  ·  ${req.totalDays} day${if (req.totalDays == 1) "" else "s"}  ·  ${req.leaveType.uppercase()}",
                    style = AppTypography.bodySmall.copy(color = AppTheme.Ink500),
                )
            }
            // Status pill
            Row(
                modifier              = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(statusBg)
                    .padding(horizontal = 10.dp, vertical = 5.dp),
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(statusColor))
                Text(
                    text  = req.status.replaceFirstChar { it.uppercase() },
                    style = AppTypography.labelSmall.copy(
                        color      = statusColor,
                        fontWeight = FontWeight.Bold,
                    ),
                )
            }
        }

        if (body.isNotBlank()) {
            // Hairline divider above the body line.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(AppTheme.Ink100),
            )
            Text(
                text  = body,
                style = AppTypography.bodyMedium.copy(color = AppTheme.Ink700),
            )
        }
    }
}

/* ─── Helpers ──────────────────────────────────────────────────────────── */

/** Split the freeform reason into a short title (first sentence) + body. */
private fun splitReason(reason: String, leaveType: String): Pair<String, String> {
    val r = reason.trim()
    if (r.isEmpty()) return leaveType to ""
    val firstStop = r.indexOf('.').takeIf { it in 1 until 80 } ?: -1
    return if (firstStop > 0) {
        r.substring(0, firstStop).trim() to r.substring(firstStop + 1).trim()
    } else if (r.length > 60) {
        r.take(60).trimEnd() + "…" to r
    } else {
        r to ""
    }
}

/** Format a from/to range like "14 — 15 Apr" or "14 Apr — 2 May". */
private fun formatRange(fromMs: Long, toMs: Long): String {
    val tz = TimeZone.currentSystemDefault()
    val from = Instant.fromEpochMilliseconds(fromMs).toLocalDateTime(tz).date
    val to   = Instant.fromEpochMilliseconds(toMs).toLocalDateTime(tz).date
    val sameMonth = from.month == to.month && from.year == to.year
    return if (from == to) {
        "${from.dayOfMonth} ${MONTHS[from.monthNumber - 1]}"
    } else if (sameMonth) {
        "${from.dayOfMonth} — ${to.dayOfMonth} ${MONTHS[from.monthNumber - 1]}"
    } else {
        "${from.dayOfMonth} ${MONTHS[from.monthNumber - 1]} — ${to.dayOfMonth} ${MONTHS[to.monthNumber - 1]}"
    }
}

/* ─── Tab pill ───────────────────────────────────────────────────────────── */

@Composable
private fun TabPill(label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier) {
    val bg by animateColorAsState(
        targetValue = if (selected) AppTheme.Surface else Color.Transparent,
        animationSpec = tween(200),
        label = "tab_bg",
    )
    Box(
        modifier = modifier
            .fillMaxSize()
            .clip(AppShapes.medium)
            .background(bg)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            style = AppTypography.labelLarge.copy(
                color      = if (selected) AppTheme.Ink900 else AppTheme.Ink500,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            ),
        )
    }
}

/* ═══════════════════════════════════════════════════════════════════════════
 *  Apply tab
 * ═══════════════════════════════════════════════════════════════════════════ */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LeaveApplyTab(
    workforceVm  : WorkforceViewModel,
    userId       : String,
    adminId      : String,
    employeeName : String,
    department   : String,
    onSubmitted  : () -> Unit = {},
) {
    var selectedType    by remember { mutableStateOf("Casual") }
    var fromDateMs      by remember { mutableStateOf<Long?>(null) }
    var toDateMs        by remember { mutableStateOf<Long?>(null) }
    var reason          by remember { mutableStateOf("") }

    var showFromPicker  by remember { mutableStateOf(false) }
    var showToPicker    by remember { mutableStateOf(false) }
    var submitSuccess   by remember { mutableStateOf(false) }
    var submitError     by remember { mutableStateOf<String?>(null) }
    var submitting      by remember { mutableStateOf(false) }

    // Validation
    val fromOk  = fromDateMs != null
    val toOk    = toDateMs != null && (toDateMs!! >= (fromDateMs ?: 0L))
    val reasonOk = reason.trim().length >= 5
    val canSubmit = !submitting && fromOk && toOk && reasonOk

    val totalDays = if (fromOk && toOk) leaveDays(fromDateMs!!, toDateMs!!) else 0

    // Date pickers
    if (showFromPicker) {
        val state = rememberDatePickerState(initialSelectedDateMillis = fromDateMs)
        DatePickerDialog(
            onDismissRequest = { showFromPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    fromDateMs = state.selectedDateMillis
                    if (toDateMs != null && toDateMs!! < (fromDateMs ?: 0L)) toDateMs = null
                    showFromPicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showFromPicker = false }) { Text("Cancel") }
            },
        ) { DatePicker(state = state) }
    }

    if (showToPicker) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = toDateMs ?: fromDateMs,
        )
        DatePickerDialog(
            onDismissRequest = { showToPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    toDateMs = state.selectedDateMillis
                    showToPicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showToPicker = false }) { Text("Cancel") }
            },
        ) { DatePicker(state = state) }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier            = Modifier.weight(1f),
            contentPadding      = PaddingValues(
                start  = AppTheme.SpLg,
                end    = AppTheme.SpLg,
                top    = AppTheme.SpMd,
                bottom = AppTheme.SpMd,
            ),
            verticalArrangement = Arrangement.spacedBy(AppTheme.SpMd),
        ) {

            // Leave type selector
            item {
                FormCard(title = "Leave Type") {
                    Row(horizontalArrangement = Arrangement.spacedBy(AppTheme.SpMd)) {
                        LEAVE_TYPES.forEach { type ->
                            LeaveTypeTile(
                                meta     = type,
                                selected = type.key == selectedType,
                                onClick  = { selectedType = type.key },
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            }

            // Date range
            item {
                FormCard(title = "Date Range") {
                    Row(horizontalArrangement = Arrangement.spacedBy(AppTheme.SpMd)) {
                        DatePickField(
                            label     = "From",
                            dateText  = fromDateMs?.toLeaveDate() ?: "Select date",
                            hasValue  = fromDateMs != null,
                            onClick   = { showFromPicker = true },
                            modifier  = Modifier.weight(1f),
                        )
                        DatePickField(
                            label    = "To",
                            dateText = toDateMs?.toLeaveDate() ?: "Select date",
                            hasValue = toDateMs != null,
                            onClick  = { showToPicker = true },
                            modifier = Modifier.weight(1f),
                        )
                    }
                    if (fromOk && toOk && totalDays > 0) {
                        Spacer(Modifier.height(AppTheme.SpSm))
                        Box(
                            modifier = Modifier
                                .clip(AppShapes.small)
                                .background(AppTheme.Brand50)
                                .padding(horizontal = AppTheme.SpSm, vertical = 4.dp),
                        ) {
                            Text(
                                "$totalDays day${if (totalDays > 1) "s" else ""} selected",
                                style = AppTypography.captionLarge.copy(
                                    color      = AppTheme.Brand,
                                    fontWeight = FontWeight.SemiBold,
                                ),
                            )
                        }
                    }
                }
            }

            // Reason
            item {
                FormCard(title = "Reason") {
                    OutlinedTextField(
                        value           = reason,
                        onValueChange   = { reason = it },
                        modifier        = Modifier.fillMaxWidth().height(110.dp),
                        placeholder     = {
                            Text(
                                "Briefly describe your reason…",
                                style = AppTypography.bodySmall.copy(color = AppTheme.Ink300),
                            )
                        },
                        shape           = AppShapes.medium,
                        colors          = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor   = InputBg,
                            unfocusedContainerColor = InputBg,
                            focusedBorderColor      = AppTheme.Brand,
                            unfocusedBorderColor    = BorderColor,
                        ),
                        maxLines = 4,
                    )
                    if (reason.isNotBlank() && reason.trim().length < 5) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Please enter at least 5 characters.",
                            style = AppTypography.captionSmall.copy(color = AppTheme.Danger),
                        )
                    }
                }
            }

            // Validation summary
            if (!canSubmit && (fromDateMs != null || toDateMs != null || reason.isNotBlank())) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        if (!fromOk) ValidationHint("Select a start date")
                        if (fromOk && !toOk) ValidationHint("End date must be on or after start date")
                        if (!reasonOk && reason.isNotBlank()) ValidationHint("Reason is too short")
                    }
                }
            }

            // Error / success feedback
            if (submitError != null) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(AppShapes.medium)
                            .background(AppTheme.DangerBg)
                            .padding(AppTheme.SpMd),
                    ) {
                        Text(
                            "Failed: $submitError",
                            style = AppTypography.bodySmall.copy(color = AppTheme.Danger),
                        )
                    }
                }
            }
            if (submitSuccess) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(AppShapes.medium)
                            .background(AppTheme.SuccessBg)
                            .padding(AppTheme.SpMd),
                    ) {
                        Text(
                            "✅  Request submitted! Your admin will review it shortly.",
                            style = AppTypography.bodySmall.copy(color = AppTheme.Success),
                        )
                    }
                }
            }
        }

        // Sticky submit button — sits above the 76 dp app bottom nav bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(AppTheme.Surface)
                .padding(
                    start  = AppTheme.SpLg,
                    end    = AppTheme.SpLg,
                    top    = AppTheme.SpMd,
                    bottom = 76.dp + AppTheme.SpMd,
                ),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .shadow(
                        elevation = if (canSubmit) 8.dp else 0.dp,
                        shape     = AppShapes.large,
                        spotColor = AppTheme.Brand.copy(alpha = 0.40f),
                    )
                    .clip(AppShapes.large)
                    .background(if (canSubmit) AppTheme.Brand else AppTheme.Ink100)
                    .clickable(enabled = canSubmit) {
                        submitting   = true
                        submitError  = null
                        submitSuccess = false
                        workforceVm.submitLeaveRequest(
                            adminId      = adminId,
                            userId       = userId,
                            employeeName = employeeName,
                            department   = department,
                            leaveType    = selectedType,
                            fromDateMs   = fromDateMs!!,
                            toDateMs     = toDateMs!!,
                            totalDays    = totalDays,
                            reason       = reason.trim(),
                        ) { result ->
                            submitting = false
                            result
                                .onSuccess {
                                    submitSuccess = true
                                    ToastController.info(
                                        title = "Leave application sent",
                                        body  = "$totalDays-day $selectedType leave · awaiting approval",
                                    )
                                    // Reset form
                                    selectedType = "Casual"
                                    fromDateMs   = null
                                    toDateMs     = null
                                    reason       = ""
                                    // Close the host (e.g. bottom sheet).
                                    onSubmitted()
                                }
                                .onFailure {
                                    submitError = it.message
                                    ToastController.error(
                                        title = "Couldn't send leave request",
                                        body  = it.message ?: "Please check your connection and try again",
                                    )
                                }
                        }
                    },
                contentAlignment = Alignment.Center,
            ) {
                if (submitting) {
                    Text(
                        "Submitting…",
                        style = AppTypography.labelLarge.copy(color = Color.White),
                    )
                } else {
                    Text(
                        "Submit Request",
                        style = AppTypography.labelLarge.copy(
                            color = if (canSubmit) Color.White else AppTheme.Ink300,
                        ),
                    )
                }
            }
        }
    }
}

/* ─── FormCard wrapper ───────────────────────────────────────────────────── */

@Composable
private fun FormCard(
    title  : String,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(2.dp, AppShapes.large, ambientColor = Color.Transparent,
                spotColor = AppTheme.Ink100)
            .clip(AppShapes.large)
            .background(CardBg)
            .padding(AppTheme.SpLg),
        verticalArrangement = Arrangement.spacedBy(AppTheme.SpMd),
    ) {
        Text(
            title,
            style = AppTypography.labelLarge.copy(
                color      = ValueColor,
                fontWeight = FontWeight.SemiBold,
            ),
        )
        content()
    }
}

/* ─── Leave type tile ────────────────────────────────────────────────────── */

@Composable
private fun LeaveTypeTile(
    meta    : LeaveTypeMeta,
    selected: Boolean,
    onClick : () -> Unit,
    modifier: Modifier,
) {
    val bg by animateColorAsState(
        targetValue   = if (selected) AppTheme.Brand50 else InputBg,
        animationSpec = tween(180),
        label         = "tile_bg",
    )
    val border by animateColorAsState(
        targetValue   = if (selected) AppTheme.Brand else Color.Transparent,
        animationSpec = tween(180),
        label         = "tile_border",
    )
    Column(
        modifier = modifier
            .height(90.dp)
            .clip(AppShapes.medium)
            .background(bg)
            .border(1.5.dp, border, AppShapes.medium)
            .clickable(onClick = onClick)
            .padding(8.dp),
        verticalArrangement   = Arrangement.Center,
        horizontalAlignment   = Alignment.CenterHorizontally,
    ) {
        Text(meta.emoji, fontSize = 26.sp)
        Spacer(Modifier.height(4.dp))
        Text(
            meta.label,
            style = AppTypography.captionLarge.copy(
                color      = if (selected) AppTheme.Brand else AppTheme.Ink700,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            ),
        )
    }
}

/* ─── Date field ─────────────────────────────────────────────────────────── */

@Composable
private fun DatePickField(
    label   : String,
    dateText: String,
    hasValue: Boolean,
    onClick : () -> Unit,
    modifier: Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            label,
            style = AppTypography.captionLarge.copy(
                color      = LabelColor,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.5.sp,
            ),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .clip(AppShapes.medium)
                .background(InputBg)
                .border(
                    width = 1.dp,
                    color = if (hasValue) AppTheme.Brand.copy(alpha = 0.50f) else BorderColor,
                    shape = AppShapes.medium,
                )
                .clickable(onClick = onClick)
                .padding(horizontal = AppTheme.SpMd),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppTheme.SpSm),
        ) {
            Icon(
                Icons.Outlined.CalendarMonth,
                contentDescription = null,
                tint     = if (hasValue) AppTheme.Brand else AppTheme.Ink300,
                modifier = Modifier.size(18.dp),
            )
            Text(
                dateText,
                style = AppTypography.bodySmall.copy(
                    color = if (hasValue) ValueColor else AppTheme.Ink300,
                ),
            )
        }
    }
}

/* ─── Validation hint ────────────────────────────────────────────────────── */

@Composable
private fun ValidationHint(message: String) {
    Row(
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppTheme.SpXs),
    ) {
        Box(
            modifier = Modifier.size(6.dp).clip(CircleShape)
                .background(AppTheme.Warning),
        )
        Text(message, style = AppTypography.captionSmall.copy(color = AppTheme.Ink500))
    }
}

/* ═══════════════════════════════════════════════════════════════════════════
 *  History tab
 * ═══════════════════════════════════════════════════════════════════════════ */

@Composable
private fun LeaveHistoryTab(requests: List<LeaveRecord>) {
    if (requests.isEmpty()) {
        Box(
            modifier         = Modifier.fillMaxSize().padding(AppTheme.SpXxl),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("📋", fontSize = 48.sp)
                Spacer(Modifier.height(AppTheme.SpMd))
                Text(
                    "No leave history",
                    style = AppTypography.titleMedium.copy(color = AppTheme.Ink900),
                )
                Text(
                    "Submit your first request from the Apply tab.",
                    style = AppTypography.bodySmall.copy(color = AppTheme.Ink500),
                )
            }
        }
        return
    }
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
        items(requests, key = { it.id }) { request ->
            LeaveHistoryCard(request = request)
        }
    }
}

@Composable
private fun LeaveHistoryCard(request: LeaveRecord) {
    val meta         = LEAVE_TYPES.find { it.key == request.leaveType }
        ?: LEAVE_TYPES.first()
    val statusColor  = leaveStatusColor(request.status)
    val statusBg     = leaveStatusBg(request.status)
    val statusLabel  = request.status.replaceFirstChar { it.uppercase() }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(2.dp, AppShapes.large, ambientColor = Color.Transparent,
                spotColor = AppTheme.Ink100)
            .clip(AppShapes.large)
            .background(CardBg)
            .padding(AppTheme.SpLg),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier          = Modifier.fillMaxWidth(),
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(AppShapes.medium)
                    .background(AppTheme.Brand50),
                contentAlignment = Alignment.Center,
            ) { Text(meta.emoji, fontSize = 22.sp) }

            Spacer(Modifier.width(AppTheme.SpMd))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "${meta.label} Leave",
                    style = AppTypography.titleMedium.copy(
                        color      = ValueColor,
                        fontWeight = FontWeight.SemiBold,
                    ),
                )
                Text(
                    "${request.fromDateMs.toLeaveDate()} → ${request.toDateMs.toLeaveDate()}",
                    style = AppTypography.captionLarge.copy(color = AppTheme.Ink500),
                )
            }

            Box(
                modifier = Modifier
                    .clip(AppShapes.pill)
                    .background(statusBg)
                    .padding(horizontal = AppTheme.SpSm, vertical = 4.dp),
            ) {
                Text(
                    statusLabel,
                    style = AppTypography.captionLarge.copy(
                        color      = statusColor,
                        fontWeight = FontWeight.SemiBold,
                    ),
                )
            }
        }

        Spacer(Modifier.height(AppTheme.SpMd))
        Box(
            modifier = Modifier.fillMaxWidth().height(1.dp)
                .background(AppTheme.Ink100),
        )
        Spacer(Modifier.height(AppTheme.SpMd))

        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            LeaveStat(label = "Total Days", value = "${request.totalDays}")
            LeaveStat(label = "Type",       value = meta.label)
            LeaveStat(label = "Status",     value = statusLabel, valueColor = statusColor)
        }

        if (request.reason.isNotBlank()) {
            Spacer(Modifier.height(AppTheme.SpMd))
            Text(
                "\"${request.reason}\"",
                style    = AppTypography.bodySmall.copy(
                    color = AppTheme.Ink500,
                    fontWeight = FontWeight.Normal,
                ),
                maxLines = 2,
            )
        }

        if (request.status == "rejected" && request.rejectionReason.isNotBlank()) {
            Spacer(Modifier.height(AppTheme.SpSm))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(AppShapes.small)
                    .background(AppTheme.DangerBg)
                    .padding(AppTheme.SpSm),
            ) {
                Text(
                    "Rejection note: ${request.rejectionReason}",
                    style = AppTypography.captionSmall.copy(color = AppTheme.Danger),
                )
            }
        }
    }
}

@Composable
private fun LeaveStat(label: String, value: String, valueColor: Color = AppTheme.Ink900) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            value,
            style = AppTypography.titleMedium.copy(
                color      = valueColor,
                fontWeight = FontWeight.Bold,
            ),
        )
        Text(label, style = AppTypography.captionSmall.copy(color = AppTheme.Ink500))
    }
}

/* ═══════════════════════════════════════════════════════════════════════════
 *  Balance tab
 * ═══════════════════════════════════════════════════════════════════════════ */

@Composable
private fun LeaveBalanceTab(requests: List<LeaveRecord>) {
    val usedByType = remember(requests) {
        requests
            .filter { it.status == "approved" }
            .groupBy { it.leaveType }
            .mapValues { (_, list) -> list.sumOf { it.totalDays } }
    }

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
        items(LEAVE_TYPES, key = { it.key }) { type ->
            val total     = LEAVE_BALANCES[type.key] ?: 0
            val used      = usedByType[type.key] ?: 0
            val remaining = (total - used).coerceAtLeast(0)
            LeaveBalanceCard(meta = type, total = total, used = used, remaining = remaining)
        }
    }
}

@Composable
private fun LeaveBalanceCard(
    meta     : LeaveTypeMeta,
    total    : Int,
    used     : Int,
    remaining: Int,
) {
    val fraction = if (total > 0) used.toFloat() / total else 0f
    val barColor = when {
        fraction > 0.80f -> AppTheme.Danger
        fraction > 0.50f -> AppTheme.Warning
        else             -> AppTheme.Success
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(2.dp, AppShapes.large, ambientColor = Color.Transparent,
                spotColor = AppTheme.Ink100)
            .clip(AppShapes.large)
            .background(CardBg)
            .padding(AppTheme.SpLg),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(AppShapes.medium)
                    .background(AppTheme.Brand50),
                contentAlignment = Alignment.Center,
            ) { Text(meta.emoji, fontSize = 24.sp) }

            Spacer(Modifier.width(AppTheme.SpMd))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "${meta.label} Leave",
                    style = AppTypography.titleMedium.copy(
                        color      = ValueColor,
                        fontWeight = FontWeight.SemiBold,
                    ),
                )
                Text(
                    "$used of $total days used",
                    style = AppTypography.captionLarge.copy(color = AppTheme.Ink500),
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "$remaining",
                    style = AppTypography.displayMedium.copy(
                        color      = barColor,
                        fontWeight = FontWeight.Bold,
                    ),
                )
                Text(
                    "days left",
                    style = AppTypography.captionSmall.copy(color = AppTheme.Ink500),
                )
            }
        }

        Spacer(Modifier.height(AppTheme.SpMd))

        LinearProgressIndicator(
            progress          = { fraction },
            modifier          = Modifier.fillMaxWidth().height(6.dp).clip(AppShapes.pill),
            color             = barColor,
            trackColor        = AppTheme.Ink50,
            strokeCap         = StrokeCap.Round,
        )
    }
}

/* ─── Status helpers ─────────────────────────────────────────────────────── */

private fun leaveStatusColor(status: String): Color = when (status) {
    "approved" -> AppTheme.Success
    "rejected" -> AppTheme.Danger
    else       -> AppTheme.Warning
}

private fun leaveStatusBg(status: String): Color = when (status) {
    "approved" -> AppTheme.SuccessBg
    "rejected" -> AppTheme.DangerBg
    else       -> AppTheme.WarningBg
}
