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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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

    val filtered = remember(allRequests, statusFilter, typeFilter) {
        allRequests.filter { req ->
            (statusFilter == "All" || req.status.equals(statusFilter, ignoreCase = true)) &&
            (typeFilter   == "All" || req.leaveType.equals(typeFilter, ignoreCase = true))
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

        // Filter strip: Status
        LazyRow(
            contentPadding        = PaddingValues(horizontal = AppTheme.SpLg),
            horizontalArrangement = Arrangement.spacedBy(AppTheme.SpSm),
            modifier              = Modifier
                .fillMaxWidth()
                .padding(top = AppTheme.SpMd, bottom = AppTheme.SpXs),
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
                    label    = if (filter == "All") filter
                               else "${LEAVE_TYPE_EMOJI[filter] ?: ""}  $filter",
                    selected = typeFilter == filter,
                    tint     = AppTheme.Brand,
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
                    contentPadding      = PaddingValues(
                        start  = AppTheme.SpLg,
                        end    = AppTheme.SpLg,
                        bottom = 80.dp,
                    ),
                    verticalArrangement = Arrangement.spacedBy(AppTheme.SpMd),
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

@Composable
private fun LeaveApprovalCard(
    request  : LeaveRecord,
    onApprove: () -> Unit,
    onReject : () -> Unit,
) {
    val isPending  = request.status == "pending"
    val statusColor = leaveStatusColor(request.status)
    val statusBg    = leaveStatusBg(request.status)
    val emoji       = LEAVE_TYPE_EMOJI[request.leaveType] ?: "📋"
    val initials    = request.employeeName.toInitials()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation    = AppElevation.card,
                shape        = AppShapes.large,
                spotColor    = AppTheme.Ink100,
                ambientColor = Color.Transparent,
            )
            .clip(AppShapes.large)
            .background(CardBg),
    ) {
        // Status accent strip at top
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(3.dp)
                .background(
                    Brush.horizontalGradient(
                        listOf(statusColor, statusColor.copy(alpha = 0.40f)),
                    ),
                ),
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppTheme.SpLg),
        ) {

            // Row 1: Avatar + name + status chip
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier          = Modifier.fillMaxWidth(),
            ) {
                DsAvatarBubble(initials = initials, size = AppTheme.AvatarMd)
                Spacer(Modifier.width(AppTheme.SpMd))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        request.employeeName,
                        style    = AppTypography.titleMedium.copy(
                            color      = AppTheme.Ink900,
                            fontWeight = FontWeight.SemiBold,
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (request.department.isNotBlank()) {
                        Text(
                            request.department,
                            style = AppTypography.captionLarge.copy(color = AppTheme.Ink500),
                        )
                    }
                }
                com.example.uniwattelektrik.core.components.DsStatusChip(
                    label      = request.status.replaceFirstChar { it.uppercase() },
                    tint       = statusColor,
                    background = statusBg,
                )
            }

            Spacer(Modifier.height(AppTheme.SpMd))

            // Row 2: Leave type chip + date range + days badge
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier          = Modifier.fillMaxWidth(),
            ) {
                Box(
                    modifier = Modifier
                        .clip(AppShapes.pill)
                        .background(AppTheme.Brand50)
                        .padding(horizontal = AppTheme.SpSm, vertical = 5.dp),
                ) {
                    Text(
                        "$emoji  ${request.leaveType}",
                        style = AppTypography.captionLarge.copy(
                            color      = AppTheme.Brand,
                            fontWeight = FontWeight.SemiBold,
                        ),
                    )
                }
                Spacer(Modifier.width(AppTheme.SpSm))
                Text(
                    "${request.fromDateMs.toLeaveDate()} → ${request.toDateMs.toLeaveDate()}",
                    style    = AppTypography.captionLarge.copy(color = AppTheme.Ink700),
                    modifier = Modifier.weight(1f),
                )
                com.example.uniwattelektrik.core.components.DsStatusChip(
                    label      = "${request.totalDays}d",
                    tint       = AppTheme.Ink700,
                    background = AppTheme.Ink50,
                )
            }

            // Row 3: Reason preview
            if (request.reason.isNotBlank()) {
                Spacer(Modifier.height(AppTheme.SpSm))
                Text(
                    "\"${request.reason}\"",
                    style    = AppTypography.bodySmall.copy(color = AppTheme.Ink500),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            // Rejection note
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
                        "Note: ${request.rejectionReason}",
                        style = AppTypography.captionSmall.copy(color = AppTheme.Danger),
                    )
                }
            }

            // Action buttons (pending only)
            AnimatedVisibility(
                visible = isPending,
                enter   = fadeIn(tween(200)),
                exit    = fadeOut(tween(200)),
            ) {
                Column {
                    Spacer(Modifier.height(AppTheme.SpMd))
                    Box(
                        modifier = Modifier.fillMaxWidth().height(1.dp)
                            .background(AppTheme.Ink100),
                    )
                    Spacer(Modifier.height(AppTheme.SpMd))

                    Row(horizontalArrangement = Arrangement.spacedBy(AppTheme.SpMd)) {
                        // Reject
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .clip(AppShapes.medium)
                                .border(1.dp, AppTheme.Danger.copy(alpha = 0.50f), AppShapes.medium)
                                .background(AppTheme.DangerBg)
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
                                    fontWeight = FontWeight.SemiBold,
                                ),
                            )
                        }

                        // Approve
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .shadow(4.dp, AppShapes.medium,
                                    spotColor = AppTheme.Success.copy(alpha = 0.30f),
                                    ambientColor = Color.Transparent)
                                .clip(AppShapes.medium)
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(AppTheme.Success, AppTheme.Success.copy(alpha = 0.80f)),
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
                                    color      = Color.White,
                                    fontWeight = FontWeight.SemiBold,
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

@Composable
private fun FilterChip(
    label   : String,
    selected: Boolean,
    tint    : Color,
    onClick : () -> Unit,
) {
    val bg by animateColorAsState(
        targetValue   = if (selected) tint.copy(alpha = 0.12f) else FilterInactiveBg,
        animationSpec = tween(180),
        label         = "chip_bg",
    )
    Box(
        modifier = Modifier
            .height(34.dp)
            .clip(AppShapes.pill)
            .background(bg)
            .border(
                width = if (selected) 1.dp else 0.dp,
                color = if (selected) tint.copy(alpha = 0.40f) else Color.Transparent,
                shape = AppShapes.pill,
            )
            .clickable(onClick = onClick)
            .padding(horizontal = AppTheme.SpMd),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            style = AppTypography.labelLarge.copy(
                color      = if (selected) tint else AppTheme.Ink500,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            ),
        )
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
