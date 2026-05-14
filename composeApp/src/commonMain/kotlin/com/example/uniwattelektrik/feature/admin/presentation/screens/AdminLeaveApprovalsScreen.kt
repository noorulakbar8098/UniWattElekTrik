package com.example.uniwattelektrik.feature.admin.presentation.screens

import com.example.uniwattelektrik.core.components.ToastController
import com.example.uniwattelektrik.core.performance.TrackScreenPerformance

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.layout.IntrinsicSize
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.outlined.Info
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.uniwattelektrik.core.components.AppPullToRefresh
import com.example.uniwattelektrik.core.components.DsAvatarBubble
import com.example.uniwattelektrik.core.components.DsEmptyState
import com.example.uniwattelektrik.core.components.OperationsHeader
import com.example.uniwattelektrik.core.components.OperationsHeaderStatusBarColor
import com.example.uniwattelektrik.core.theme.AppElevation
import com.example.uniwattelektrik.core.theme.AppShapes
import com.example.uniwattelektrik.core.theme.AppTheme
import com.example.uniwattelektrik.core.theme.AppTypography
import com.example.uniwattelektrik.core.theme.SetStatusBar
import com.example.uniwattelektrik.core.theme.appScreenBackground
import com.example.uniwattelektrik.core.theme.premiumLayeredShadow
import com.example.uniwattelektrik.feature.user.presentation.screens.toLeaveDate
import com.example.uniwattelektrik.feature.workforce.data.remote.LeaveRecord
import com.example.uniwattelektrik.feature.workforce.presentation.WorkforceViewModel

// ─── Design tokens ────────────────────────────────────────────────────────────

private val CardBg        = AppTheme.Surface
private val FilterActiveBg = AppTheme.Brand50
private val FilterInactiveBg = AppTheme.Ink50

// ─── Filter definitions ───────────────────────────────────────────────────────

private val STATUS_FILTERS = listOf("All", "Pending", "Approved", "Rejected")
private val TYPE_FILTERS   = listOf("All", "Casual", "Sick", "Earned")

private val LEAVE_TYPE_EMOJI = mapOf(
    "Casual" to "☕",
    "Sick"   to "🤒",
    "Earned" to "🌴",
)

/* ═══════════════════════════════════════════════════════════════════════════
 *  AdminLeaveApprovalsScreen
 * ═══════════════════════════════════════════════════════════════════════════ */

@Composable
fun AdminLeaveApprovalsScreen(
    workforceVm: WorkforceViewModel,
    adminUid   : String,
    onBack     : () -> Unit = {},
    modifier   : Modifier = Modifier,
    initialStatusFilter: String = "All",
    /**
     * When false, the gradient title header is skipped so the screen can be
     * embedded as a tab inside another container (e.g. AdminAttendanceScreen).
     */
    showHeader : Boolean = true,
) {
    TrackScreenPerformance("AdminLeaveApprovalsScreen")
    val allRequests by workforceVm.leaveRequests.collectAsStateWithLifecycle()

    var statusFilter by remember { mutableStateOf(initialStatusFilter) }
    var typeFilter   by remember { mutableStateOf("All") }
    // Optional custom date filter. When set, only leave requests whose
    // [fromDateMs..toDateMs] window covers the selected date are shown.
    var dateFilterMs by remember { mutableStateOf<Long?>(null) }

    val filtered = remember(allRequests, statusFilter, typeFilter, dateFilterMs) {
        allRequests.filter { req ->
            val statusOk = statusFilter == "All" || req.status.equals(statusFilter, ignoreCase = true)
            val typeOk   = typeFilter   == "All" || req.leaveType.equals(typeFilter, ignoreCase = true)
            val dateOk   = dateFilterMs?.let { ms ->
                // Leave covers the picked date if ms falls within the request's
                // half-open window. We add 24h to toDateMs so a same-day leave
                // (from == to) still matches.
                ms in req.fromDateMs until (req.toDateMs + 86_400_000L)
            } ?: true
            statusOk && typeOk && dateOk
        }
    }

    val pendingCount = remember(allRequests) {
        allRequests.count { it.status == "pending" }
    }

    // Reject dialog state
    var rejectTarget      by remember { mutableStateOf<LeaveRecord?>(null) }
    var rejectionReason   by remember { mutableStateOf("") }

    SetStatusBar(color = OperationsHeaderStatusBarColor, darkIcons = false)

    if (rejectTarget != null) {
        RejectDialog(
            employeeName = rejectTarget!!.employeeName,
            reason       = rejectionReason,
            onReasonChange = { rejectionReason = it },
            onConfirm = {
                val target = rejectTarget!!
                workforceVm.rejectLeave(
                    leaveId         = target.id,
                    adminId         = adminUid,
                    userId          = target.userId,
                    rejectionReason = rejectionReason.trim(),
                )
                ToastController.error(
                    title = "Leave declined",
                    body  = "${target.employeeName} · ${target.leaveType}",
                )
                rejectTarget    = null
                rejectionReason = ""
            },
            onDismiss = {
                rejectTarget    = null
                rejectionReason = ""
            },
        )
    }

    val actionInProgress by workforceVm.actionInProgress.collectAsStateWithLifecycle()

    Box(modifier = modifier.fillMaxSize()) {
    Column(modifier = Modifier.fillMaxSize().background(appScreenBackground())) {

        // Operations header (suppressed when embedded as a tab).
        if (showHeader) {
            OperationsHeader(
                eyebrow  = "LEAVE APPROVALS",
                title    = "Leave Requests",
                subtitle = "${allRequests.size} total · $pendingCount awaiting review",
                onBack   = onBack,
                actions  = {
                    if (pendingCount > 0) {
                        Box(
                            modifier = Modifier
                                .clip(AppShapes.pill)
                                .background(Color.White.copy(alpha = 0.10f))
                                .padding(horizontal = 10.dp, vertical = 5.dp),
                        ) {
                            Text(
                                "$pendingCount pending",
                                style = AppTypography.captionLarge.copy(
                                    color      = Color.White,
                                    fontWeight = FontWeight.SemiBold,
                                ),
                            )
                        }
                    }
                },
            )
        }

        // Date filter pill — opens a Material3 DatePickerDialog. When a date
        // is picked, only leave requests whose [from..to] window covers that
        // date are shown. A trailing X clears the filter.
        DateFilterPill(
            selectedMs = dateFilterMs,
            onPick     = { dateFilterMs = it },
            onClear    = { dateFilterMs = null },
        )

        // Filter strip: Status
        LazyRow(
            contentPadding        = PaddingValues(horizontal = AppTheme.SpLg),
            horizontalArrangement = Arrangement.spacedBy(AppTheme.SpSm),
            modifier              = Modifier
                .fillMaxWidth()
                .padding(top = AppTheme.SpSm, bottom = AppTheme.SpXs),
        ) {
            items(STATUS_FILTERS) { filter ->
                FilterChip(
                    label    = filter,
                    selected = statusFilter == filter,
                    tint     = when (filter) {
                        "Pending"  -> AppTheme.Warning
                        "Approved" -> AppTheme.Success
                        "Rejected" -> AppTheme.Danger
                        else       -> AppTheme.Brand
                    },
                    icon     = when (filter) {
                        "Pending"  -> Icons.Filled.Schedule
                        "Approved" -> Icons.Filled.Check
                        "Rejected" -> Icons.Filled.Close
                        else       -> Icons.Filled.FilterList
                    },
                    onClick = { statusFilter = filter },
                )
            }
        }

        // Filter strip: Leave type
        LazyRow(
            contentPadding        = PaddingValues(horizontal = AppTheme.SpLg),
            horizontalArrangement = Arrangement.spacedBy(AppTheme.SpSm),
            modifier              = Modifier
                .fillMaxWidth()
                .padding(bottom = AppTheme.SpMd),
        ) {
            items(TYPE_FILTERS) { filter ->
                FilterChip(
                    label    = filter,
                    selected = typeFilter == filter,
                    // Per-type accent so each leave type carries its own
                    // identity colour even before being tapped:
                    //   Casual → brand blue · Sick → danger red · Earned → success green
                    tint     = when (filter) {
                        "Sick"   -> AppTheme.Danger
                        "Earned" -> AppTheme.Success
                        "Casual" -> AppTheme.Brand
                        else     -> AppTheme.Brand
                    },
                    emoji    = LEAVE_TYPE_EMOJI[filter],
                    icon     = if (filter == "All") Icons.Filled.FilterList else null,
                    onClick  = { typeFilter = filter },
                )
            }
        }

        // Content
        AppPullToRefresh(onRefresh = {}) {
            if (filtered.isEmpty()) {
                DsEmptyState(
                    emoji = "📭",
                    title = if (statusFilter == "Pending") "No pending requests"
                            else "Nothing matches filters",
                    body  = "Adjust the filters above or check back later.",
                )
            } else {
                LazyColumn(
                    modifier            = Modifier.fillMaxSize(),
                    // Extra top + bottom padding so the wide halo shadow on the
                    // first / last card isn't clipped by the filter strip or
                    // the bottom nav bar.
                    contentPadding      = PaddingValues(
                        start  = AppTheme.SpLg,
                        end    = AppTheme.SpLg,
                        top    = AppTheme.SpSm,
                        bottom = 88.dp,
                    ),
                    // Bump card-to-card spacing so adjacent halos don't bleed
                    // into each other.
                    verticalArrangement = Arrangement.spacedBy(18.dp),
                ) {
                    items(filtered, key = { it.id }) { request ->
                        LeaveApprovalCard(
                            request  = request,
                            onApprove = {
                                workforceVm.approveLeave(
                                    leaveId = request.id,
                                    adminId = adminUid,
                                    userId  = request.userId,
                                )
                                ToastController.success(
                                    title = "Leave approved",
                                    body  = "${request.employeeName} · ${request.leaveType}",
                                )
                            },
                            onReject = { rejectTarget = request },
                        )
                    }
                }
            }
        }
    }
        com.example.uniwattelektrik.core.components.LoadingOverlay(
            visible = actionInProgress,
            message = "Please wait…",
        )
    }
}

/* ─── Leave approval card ────────────────────────────────────────────────── */

/**
 * Premium leave-request card.
 *
 * Visual hierarchy:
 *   ▸ Status-colored vertical accent bar runs the full left edge.
 *   ▸ Card body sits on a soft white→ink50 top-light gradient with a
 *     status-tinted shadow — gives a premium, glassy feel.
 *   ▸ Avatar wears a status-colored ring + soft glow so the role of the
 *     card is readable at a glance.
 *   ▸ Date range is a structured info panel (FROM → TO) with a circular
 *     days counter on the right.
 *   ▸ Reason gets a left-border blockquote treatment.
 *   ▸ Rejection note picks up an info icon + danger-tinted block.
 *   ▸ Actions are a bordered "Reject" pill and a gradient "Approve" pill.
 */
@Composable
private fun LeaveApprovalCard(
    request  : LeaveRecord,
    onApprove: () -> Unit,
    onReject : () -> Unit,
) {
    val isPending   = request.status == "pending"
    val statusColor = leaveStatusColor(request.status)
    val statusBg    = leaveStatusBg(request.status)
    val emoji       = LEAVE_TYPE_EMOJI[request.leaveType] ?: "📋"
    val initials    = request.employeeName.toInitials()

    // ── Outer card shell ────────────────────────────────────────────────
    //
    // PREMIUM LAYERED SHADOW PASS:
    //  · Layer 1 — wide soft "halo" tinted with the status colour. This is
    //    what gives the card a hovering, premium feel. Elevation 22dp, low
    //    alpha so it stays atmospheric not gaudy.
    //  · Layer 2 — tight crisp ink shadow with a dark spot + faint ambient.
    //    Defines the edge cleanly so the card feels physical, not floaty.
    //
    // Stacking two .shadow() modifiers is the standard Compose pattern for
    // multi-tier shadows (Stripe / Linear / Notion all use the same trick).
    // The OUTER modifier draws first (back layer), the inner draws on top.
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Max)        // lets the accent bar match content height
            // Premium two-layer shadow: wide tinted halo + tight ink edge.
            // Shared with Tasks / Employees / Stock cards for visual parity.
            .premiumLayeredShadow(
                accentColor = statusColor,
                shape       = AppShapes.large,
            )
            .clip(AppShapes.large)
            .background(
                Brush.verticalGradient(
                    0f   to AppTheme.Surface,
                    0.6f to AppTheme.Surface,
                    1f   to AppTheme.Ink50.copy(alpha = 0.55f),
                ),
            )
            .border(
                width = 1.dp,
                color = AppTheme.Ink100.copy(alpha = 0.65f),
                shape = AppShapes.large,
            ),
    ) {
        // ── Vertical status accent bar (full height, 5dp wide) ─────────
        Box(
            modifier = Modifier
                .width(5.dp)
                .fillMaxHeight()
                .background(
                    Brush.verticalGradient(
                        listOf(statusColor, statusColor.copy(alpha = 0.55f)),
                    ),
                ),
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppTheme.SpLg),
        ) {

            // ── Row 1: Avatar (with status ring) + name + status chip ──
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier          = Modifier.fillMaxWidth(),
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .shadow(
                            elevation    = 4.dp,
                            shape        = CircleShape,
                            spotColor    = statusColor.copy(alpha = 0.32f),
                            ambientColor = Color.Transparent,
                        )
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(statusBg, statusBg.copy(alpha = 0.65f)),
                            ),
                        )
                        .border(2.dp, statusColor.copy(alpha = 0.55f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text       = initials,
                        style      = AppTypography.titleMedium.copy(
                            color      = statusColor,
                            fontWeight = FontWeight.ExtraBold,
                        ),
                    )
                }
                Spacer(Modifier.width(AppTheme.SpMd))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        request.employeeName,
                        style    = AppTypography.titleMedium.copy(
                            color      = AppTheme.Ink900,
                            fontWeight = FontWeight.Bold,
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (request.department.isNotBlank()) {
                        Text(
                            request.department,
                            style = AppTypography.captionLarge.copy(color = AppTheme.Ink500),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                // Premium status pill — soft tinted bg + dot indicator + border.
                Row(
                    modifier = Modifier
                        .shadow(2.dp, AppShapes.pill, spotColor = statusColor.copy(alpha = 0.25f))
                        .clip(AppShapes.pill)
                        .background(statusBg)
                        .border(1.dp, statusColor.copy(alpha = 0.35f), AppShapes.pill)
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(statusColor),
                    )
                    Text(
                        request.status.replaceFirstChar { it.uppercase() },
                        style = AppTypography.captionLarge.copy(
                            color         = statusColor,
                            fontWeight    = FontWeight.Bold,
                            letterSpacing = 0.3.sp,
                        ),
                    )
                }
            }

            Spacer(Modifier.height(AppTheme.SpMd))

            // ── Row 2: Structured date-range info panel ────────────────
            // Layout: [leave type pill] [FROM | → | TO] [days counter]
            Row(
                modifier              = Modifier
                    .fillMaxWidth()
                    .clip(AppShapes.medium)
                    .background(
                        Brush.linearGradient(
                            listOf(AppTheme.Ink50, AppTheme.Surface),
                        ),
                    )
                    .border(1.dp, AppTheme.Ink100.copy(alpha = 0.6f), AppShapes.medium)
                    .padding(horizontal = AppTheme.SpMd, vertical = AppTheme.SpSm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Leave type pill — emoji + name in brand color.
                Row(
                    modifier = Modifier
                        .clip(AppShapes.pill)
                        .background(AppTheme.Brand50)
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(emoji, fontSize = 12.sp)
                    Text(
                        request.leaveType,
                        style = AppTypography.captionSmall.copy(
                            color         = AppTheme.Brand,
                            fontWeight    = FontWeight.Bold,
                            letterSpacing = 0.2.sp,
                        ),
                    )
                }
                Spacer(Modifier.width(AppTheme.SpSm))
                // FROM column
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "FROM",
                        style = AppTypography.captionSmall.copy(
                            color         = AppTheme.Ink300,
                            fontWeight    = FontWeight.Bold,
                            letterSpacing = 0.6.sp,
                        ),
                    )
                    Text(
                        request.fromDateMs.toLeaveDate(),
                        style = AppTypography.captionLarge.copy(
                            color      = AppTheme.Ink900,
                            fontWeight = FontWeight.SemiBold,
                        ),
                    )
                }
                Icon(
                    imageVector        = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint               = AppTheme.Brand,
                    modifier           = Modifier.size(14.dp),
                )
                Spacer(Modifier.width(AppTheme.SpSm))
                // TO column
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "TO",
                        style = AppTypography.captionSmall.copy(
                            color         = AppTheme.Ink300,
                            fontWeight    = FontWeight.Bold,
                            letterSpacing = 0.6.sp,
                        ),
                    )
                    Text(
                        request.toDateMs.toLeaveDate(),
                        style = AppTypography.captionLarge.copy(
                            color      = AppTheme.Ink900,
                            fontWeight = FontWeight.SemiBold,
                        ),
                    )
                }
                Spacer(Modifier.width(AppTheme.SpSm))
                // Days counter — bold tile with number + "days" caption.
                Column(
                    modifier = Modifier
                        .clip(AppShapes.small)
                        .background(
                            Brush.linearGradient(
                                listOf(AppTheme.Brand.copy(alpha = 0.18f), AppTheme.Brand.copy(alpha = 0.08f)),
                            ),
                        )
                        .border(1.dp, AppTheme.Brand.copy(alpha = 0.30f), AppShapes.small)
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        request.totalDays.toString(),
                        style = AppTypography.titleMedium.copy(
                            color         = AppTheme.Brand,
                            fontWeight    = FontWeight.ExtraBold,
                            letterSpacing = (-0.3).sp,
                        ),
                    )
                    Text(
                        if (request.totalDays == 1) "day" else "days",
                        style = AppTypography.captionSmall.copy(
                            color         = AppTheme.Brand.copy(alpha = 0.75f),
                            fontWeight    = FontWeight.SemiBold,
                            letterSpacing = 0.3.sp,
                        ),
                    )
                }
            }

            // ── Row 3: Reason blockquote ───────────────────────────────
            if (request.reason.isNotBlank()) {
                Spacer(Modifier.height(AppTheme.SpMd))
                Row(modifier = Modifier.fillMaxWidth()) {
                    // Left accent bar
                    Box(
                        modifier = Modifier
                            .width(3.dp)
                            .height(34.dp)
                            .clip(AppShapes.pill)
                            .background(AppTheme.Brand.copy(alpha = 0.45f)),
                    )
                    Spacer(Modifier.width(AppTheme.SpSm))
                    Text(
                        text       = request.reason,
                        style      = AppTypography.bodySmall.copy(
                            color      = AppTheme.Ink700,
                            fontWeight = FontWeight.Medium,
                        ),
                        maxLines   = 3,
                        overflow   = TextOverflow.Ellipsis,
                        modifier   = Modifier.weight(1f).align(Alignment.CenterVertically),
                    )
                }
            }

            // ── Row 4: Rejection note (rejected only) ──────────────────
            if (request.status == "rejected" && request.rejectionReason.isNotBlank()) {
                Spacer(Modifier.height(AppTheme.SpMd))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(AppShapes.small)
                        .background(AppTheme.DangerBg)
                        .border(1.dp, AppTheme.Danger.copy(alpha = 0.35f), AppShapes.small)
                        .padding(AppTheme.SpSm),
                    verticalAlignment = Alignment.Top,
                ) {
                    Icon(
                        imageVector        = Icons.Outlined.Info,
                        contentDescription = null,
                        tint               = AppTheme.Danger,
                        modifier           = Modifier.size(14.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        request.rejectionReason,
                        style = AppTypography.captionSmall.copy(
                            color      = AppTheme.Danger,
                            fontWeight = FontWeight.SemiBold,
                        ),
                    )
                }
            }

            // ── Approved indicator (approved only) ────────────────────
            if (request.status == "approved") {
                Spacer(Modifier.height(AppTheme.SpMd))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(AppShapes.small)
                        .background(AppTheme.SuccessBg)
                        .border(1.dp, AppTheme.Success.copy(alpha = 0.30f), AppShapes.small)
                        .padding(AppTheme.SpSm),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector        = Icons.Filled.Check,
                        contentDescription = null,
                        tint               = AppTheme.Success,
                        modifier           = Modifier.size(14.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "Approved",
                        style = AppTypography.captionSmall.copy(
                            color      = AppTheme.Success,
                            fontWeight = FontWeight.Bold,
                        ),
                    )
                }
            }

            // ── Row 5: Action buttons (pending only) ───────────────────
            AnimatedVisibility(
                visible = isPending,
                enter   = fadeIn(tween(200)),
                exit    = fadeOut(tween(200)),
            ) {
                Column {
                    Spacer(Modifier.height(AppTheme.SpMd))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(
                                Brush.horizontalGradient(
                                    0f to Color.Transparent,
                                    0.5f to AppTheme.Ink100,
                                    1f to Color.Transparent,
                                ),
                            ),
                    )
                    Spacer(Modifier.height(AppTheme.SpMd))

                    Row(horizontalArrangement = Arrangement.spacedBy(AppTheme.SpMd)) {
                        // Reject — bordered danger pill.
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .shadow(2.dp, AppShapes.medium, spotColor = AppTheme.Danger.copy(alpha = 0.20f))
                                .clip(AppShapes.medium)
                                .border(1.5.dp, AppTheme.Danger.copy(alpha = 0.55f), AppShapes.medium)
                                .background(
                                    Brush.verticalGradient(
                                        listOf(AppTheme.Surface, AppTheme.DangerBg.copy(alpha = 0.55f)),
                                    ),
                                )
                                .clickable(onClick = onReject),
                            verticalAlignment     = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                        ) {
                            Icon(
                                Icons.Filled.Close,
                                contentDescription = "Reject",
                                tint     = AppTheme.Danger,
                                modifier = Modifier.size(16.dp),
                            )
                            Spacer(Modifier.width(AppTheme.SpXs))
                            Text(
                                "Reject",
                                style = AppTypography.labelLarge.copy(
                                    color      = AppTheme.Danger,
                                    fontWeight = FontWeight.Bold,
                                ),
                            )
                        }

                        // Approve — solid gradient success pill with deeper glow.
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .shadow(
                                    elevation    = 8.dp,
                                    shape        = AppShapes.medium,
                                    spotColor    = AppTheme.Success.copy(alpha = 0.45f),
                                    ambientColor = Color.Transparent,
                                )
                                .clip(AppShapes.medium)
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(AppTheme.Success, AppTheme.Success.copy(alpha = 0.78f)),
                                    ),
                                )
                                .clickable(onClick = onApprove),
                            verticalAlignment     = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                        ) {
                            Icon(
                                Icons.Filled.Check,
                                contentDescription = "Approve",
                                tint     = Color.White,
                                modifier = Modifier.size(16.dp),
                            )
                            Spacer(Modifier.width(AppTheme.SpXs))
                            Text(
                                "Approve",
                                style = AppTypography.labelLarge.copy(
                                    color         = Color.White,
                                    fontWeight    = FontWeight.Bold,
                                    letterSpacing = 0.2.sp,
                                ),
                            )
                        }
                    }
                }
            }
        }
    }
}

/* ─── Reject dialog ──────────────────────────────────────────────────────── */

@Composable
private fun RejectDialog(
    employeeName  : String,
    reason        : String,
    onReasonChange: (String) -> Unit,
    onConfirm     : () -> Unit,
    onDismiss     : () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor   = AppTheme.Surface,
        title = {
            Text(
                "Reject Leave Request",
                style = AppTypography.titleMedium.copy(color = AppTheme.Ink900),
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(AppTheme.SpMd)) {
                Text(
                    "You're rejecting $employeeName's request. Provide a brief reason (optional).",
                    style = AppTypography.bodySmall.copy(color = AppTheme.Ink500),
                )
                OutlinedTextField(
                    value         = reason,
                    onValueChange = onReasonChange,
                    modifier      = Modifier.fillMaxWidth().height(90.dp),
                    placeholder   = {
                        Text(
                            "Rejection reason…",
                            style = AppTypography.bodySmall.copy(color = AppTheme.Ink300),
                        )
                    },
                    shape  = AppShapes.medium,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor   = AppTheme.SurfaceMuted,
                        unfocusedContainerColor = AppTheme.SurfaceMuted,
                        focusedBorderColor      = AppTheme.Danger,
                        unfocusedBorderColor    = AppTheme.Ink100,
                    ),
                    maxLines = 3,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Reject", style = AppTypography.labelLarge.copy(color = AppTheme.Danger))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", style = AppTypography.labelLarge.copy(color = AppTheme.Ink500))
            }
        },
    )
}

/* ─── Filter chip ────────────────────────────────────────────────────────── */

/**
 * Premium two-tone filter chip.
 *
 * Active   — solid tint→tint·0.82 horizontal gradient, white text + white
 *            leading icon, deep tint-coloured shadow ("glow") for unmistakable
 *            "this is selected" affordance. Card scales to 1.04× on selection.
 * Inactive — clean white→ink50 vertical gradient, hairline ink100 border,
 *            ink700 text with the tint colour on the leading icon (so the
 *            colour identity of each chip is visible even when not selected).
 *
 * Either an [icon] (vector) OR an [emoji] (string) can be passed as the
 * leading visual — emoji renders in its native colour, icon inherits the
 * computed tint.
 */
@Composable
private fun FilterChip(
    label   : String,
    selected: Boolean,
    tint    : Color,
    onClick : () -> Unit,
    icon    : ImageVector? = null,
    emoji   : String? = null,
) {
    val scale by animateFloatAsState(
        targetValue   = if (selected) 1.04f else 1f,
        animationSpec = tween(220),
        label         = "chip_scale",
    )
    val elevation by animateDpAsState(
        targetValue   = if (selected) 8.dp else 1.dp,
        animationSpec = tween(220),
        label         = "chip_elev",
    )

    Box(
        modifier = Modifier
            .scale(scale)
            .height(38.dp)
            .shadow(
                elevation    = elevation,
                shape        = AppShapes.pill,
                spotColor    = if (selected) tint.copy(alpha = 0.45f)
                               else AppTheme.Ink100.copy(alpha = 0.6f),
                ambientColor = Color.Transparent,
            )
            .clip(AppShapes.pill)
            .background(
                if (selected)
                    Brush.horizontalGradient(
                        listOf(tint, tint.copy(alpha = 0.82f)),
                    )
                else
                    Brush.verticalGradient(
                        listOf(AppTheme.Surface, AppTheme.Ink50.copy(alpha = 0.65f)),
                    ),
            )
            .border(
                width = if (selected) 0.dp else 1.dp,
                color = if (selected) Color.Transparent
                        else AppTheme.Ink100,
                shape = AppShapes.pill,
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            // Leading visual — emoji (native colour) or vector icon.
            when {
                emoji != null -> Text(emoji, fontSize = 13.sp)
                icon  != null -> Icon(
                    imageVector        = icon,
                    contentDescription = null,
                    tint               = if (selected) Color.White else tint,
                    modifier           = Modifier.size(14.dp),
                )
            }
            Text(
                label,
                style = AppTypography.labelLarge.copy(
                    color         = if (selected) Color.White else AppTheme.Ink700,
                    fontWeight    = if (selected) FontWeight.Bold else FontWeight.SemiBold,
                    letterSpacing = if (selected) 0.3.sp else 0.1.sp,
                ),
            )
        }
    }
}

/* ─── Date filter pill ──────────────────────────────────────────────────── */

/**
 * Compact date-filter row shown above the status / type filter strips.
 *
 * - When no date is set, displays a soft surface card with a calendar icon
 *   and "Filter by date" copy — tapping it opens a Material3 DatePicker.
 * - When a date IS set, the card flips to the brand-tinted active style,
 *   shows the formatted date, and exposes a trailing X button to clear.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateFilterPill(
    selectedMs: Long?,
    onPick    : (Long) -> Unit,
    onClear   : () -> Unit,
) {
    var showPicker by remember { mutableStateOf(false) }
    val active = selectedMs != null

    Row(
        modifier              = Modifier
            .fillMaxWidth()
            .padding(horizontal = AppTheme.SpLg, vertical = AppTheme.SpSm),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppTheme.SpSm),
    ) {
        // Tappable card: opens picker.
        Row(
            modifier = Modifier
                .weight(1f)
                .shadow(if (active) 4.dp else 1.dp, AppShapes.pill, spotColor = AppTheme.Brand.copy(alpha = 0.2f))
                .clip(AppShapes.pill)
                .background(if (active) AppTheme.Brand.copy(alpha = 0.10f) else FilterInactiveBg)
                .border(
                    width = 1.dp,
                    color = if (active) AppTheme.Brand.copy(alpha = 0.45f) else Color.Transparent,
                    shape = AppShapes.pill,
                )
                .clickable { showPicker = true }
                .padding(horizontal = AppTheme.SpMd, vertical = 9.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppTheme.SpSm),
        ) {
            Icon(
                imageVector        = Icons.Filled.CalendarToday,
                contentDescription = null,
                tint               = if (active) AppTheme.Brand else AppTheme.Ink500,
                modifier           = Modifier.size(14.dp),
            )
            Text(
                text  = selectedMs?.toLeaveDate() ?: "Filter by date",
                style = AppTypography.labelLarge.copy(
                    color      = if (active) AppTheme.Brand else AppTheme.Ink500,
                    fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
                ),
            )
        }

        // Clear button — only when a filter is active.
        if (active) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .shadow(1.dp, CircleShape)
                    .clip(CircleShape)
                    .background(AppTheme.Danger.copy(alpha = 0.10f))
                    .border(1.dp, AppTheme.Danger.copy(alpha = 0.35f), CircleShape)
                    .clickable(onClick = onClear),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector        = Icons.Filled.Close,
                    contentDescription = "Clear date filter",
                    tint               = AppTheme.Danger,
                    modifier           = Modifier.size(14.dp),
                )
            }
        }
    }

    if (showPicker) {
        val initialMs = selectedMs
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = initialMs)
        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let(onPick)
                    showPicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showPicker = false }) { Text("Cancel") }
            },
        ) { DatePicker(state = pickerState) }
    }
}

/* ─── Pure helpers ───────────────────────────────────────────────────────── */

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

private fun String.toInitials(): String {
    val parts = trim().split(' ').filter { it.isNotBlank() }
    return when {
        parts.size >= 2 -> "${parts[0].first()}${parts[1].first()}".uppercase()
        parts.size == 1 -> parts[0].take(2).uppercase()
        else            -> "?"
    }
}
