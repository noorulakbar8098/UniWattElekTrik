package com.example.uniwattelektrik.core.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class SkeletonType {
    Auth,
    Dashboard,
    List,
    Form,
    // Screen-specific types that match the actual UI layout
    HomeAdmin,          // gradient header + 3 stat cards + recent task rows
    EmployeeList,       // rows with avatar + 2 text lines
    EmployeeDetail,     // profile header + stats row + tab bar + content cards
    TaskKanban,         // 3 kanban column skeletons
    AttendanceScreen,   // date pills + stat tiles + map placeholder + log rows
    InventoryList,      // search bar + pill row + item rows
    TaskDetail,         // header + stats card + section cards
}

/**
 * Subtle global shimmer sweep that can be layered on top of any screen.
 * Keep alpha low so content remains readable.
 */
@Composable
fun Modifier.shimmerOverlay(enabled: Boolean = true): Modifier {
    if (!enabled) return this

    val transition = rememberInfiniteTransition(label = "globalShimmer")
    val progress = transition.animateFloat(
        initialValue = -1f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "globalShimmerProgress",
    )

    return drawWithContent {
        drawContent()

        val width = size.width
        val startX = (progress.value - 1f) * width
        val endX = progress.value * width

        drawRect(
            brush = Brush.linearGradient(
                colors = listOf(
                    Color.Transparent,
                    Color.White.copy(alpha = 0.06f),
                    Color.Transparent,
                ),
                start = Offset(startX, 0f),
                end = Offset(endX, size.height),
            ),
        )
    }
}

@Composable
fun ScreenSkeletonOverlay(
    type: SkeletonType = SkeletonType.List,
    message: String = "Loading...",
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White.copy(alpha = 0.65f)),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            when (type) {
                SkeletonType.Auth            -> AuthSkeleton()
                SkeletonType.Dashboard       -> DashboardSkeleton()
                SkeletonType.List            -> ListSkeleton()
                SkeletonType.Form            -> FormSkeleton()
                SkeletonType.HomeAdmin       -> HomeAdminSkeleton()
                SkeletonType.EmployeeList    -> EmployeeListSkeleton()
                SkeletonType.EmployeeDetail  -> EmployeeDetailSkeleton()
                SkeletonType.TaskKanban      -> TaskKanbanSkeleton()
                SkeletonType.AttendanceScreen-> AttendanceSkeleton()
                SkeletonType.InventoryList   -> InventoryListSkeleton()
                SkeletonType.TaskDetail      -> TaskDetailSkeleton()
            }
        }

        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            CircularProgressIndicator(color = Color(0xFF2979FF), strokeWidth = 2.5.dp)
            Text(
                text = message,
                color = Color(0xFF334155),
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

/**
 * Inline skeleton — same shimmer blocks as [ScreenSkeletonOverlay] but
 * **without** the dim white scrim or the centred spinner. Use this when the
 * caller wants the skeleton to appear BELOW a sticky gradient header, so the
 * page chrome stays visible during the initial fetch.
 */
@Composable
fun InlineSkeleton(
    type: SkeletonType = SkeletonType.List,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        when (type) {
            SkeletonType.Auth             -> AuthSkeleton()
            SkeletonType.Dashboard        -> DashboardSkeleton()
            SkeletonType.List             -> ListSkeleton()
            SkeletonType.Form             -> FormSkeleton()
            SkeletonType.HomeAdmin        -> HomeAdminSkeleton()
            SkeletonType.EmployeeList     -> EmployeeListSkeleton()
            SkeletonType.EmployeeDetail   -> EmployeeDetailSkeleton()
            SkeletonType.TaskKanban       -> TaskKanbanSkeleton()
            SkeletonType.AttendanceScreen -> AttendanceSkeleton()
            SkeletonType.InventoryList    -> InventoryListSkeleton()
            SkeletonType.TaskDetail       -> TaskDetailSkeleton()
        }
    }
}

@Composable
private fun AuthSkeleton() {
    SkeletonBlock(modifier = Modifier.fillMaxWidth(0.45f).height(18.dp))
    SkeletonBlock(modifier = Modifier.fillMaxWidth(0.7f).height(14.dp))
    SkeletonBlock(modifier = Modifier.fillMaxWidth().height(56.dp))
    SkeletonBlock(modifier = Modifier.fillMaxWidth().height(56.dp))
    SkeletonBlock(modifier = Modifier.fillMaxWidth().height(52.dp))
}

@Composable
private fun DashboardSkeleton() {
    SkeletonBlock(modifier = Modifier.fillMaxWidth(0.55f).height(22.dp))
    SkeletonBlock(modifier = Modifier.fillMaxWidth().height(110.dp))
    Box(modifier = Modifier.fillMaxWidth()) {
        SkeletonBlock(modifier = Modifier.fillMaxWidth(0.48f).height(84.dp))
        SkeletonBlock(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .fillMaxWidth(0.48f)
                .height(84.dp),
        )
    }
    SkeletonBlock(modifier = Modifier.fillMaxWidth().height(88.dp))
}

@Composable
private fun ListSkeleton() {
    SkeletonBlock(modifier = Modifier.fillMaxWidth(0.35f).height(18.dp))
    SkeletonBlock(modifier = Modifier.fillMaxWidth().height(56.dp))
    SkeletonBlock(modifier = Modifier.fillMaxWidth().height(72.dp))
    SkeletonBlock(modifier = Modifier.fillMaxWidth().height(72.dp))
    SkeletonBlock(modifier = Modifier.fillMaxWidth().height(72.dp))
}

@Composable
private fun FormSkeleton() {
    SkeletonBlock(modifier = Modifier.fillMaxWidth(0.4f).height(18.dp))
    SkeletonBlock(modifier = Modifier.fillMaxWidth().height(52.dp))
    SkeletonBlock(modifier = Modifier.fillMaxWidth().height(52.dp))
    Box(modifier = Modifier.fillMaxWidth()) {
        SkeletonBlock(modifier = Modifier.fillMaxWidth(0.48f).height(52.dp))
        SkeletonBlock(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .fillMaxWidth(0.48f)
                .height(52.dp),
        )
    }
    SkeletonBlock(modifier = Modifier.fillMaxWidth().height(52.dp))
    SkeletonBlock(modifier = Modifier.fillMaxWidth().height(52.dp))
}

@Composable
private fun SkeletonBlock(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFFE2E8F0))
            .shimmerOverlay(),
    )
}

/* ── Screen-specific skeletons ─────────────────────────────────────────── */

/** Admin home: gradient header placeholder → 3 stat tiles → recent task rows */
@Composable
private fun HomeAdminSkeleton() {
    // Gradient header placeholder
    SkeletonBlock(modifier = Modifier.fillMaxWidth().height(140.dp).clip(RoundedCornerShape(0.dp)))
    Spacer(Modifier.height(12.dp))
    // 3 stat tiles
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        repeat(3) { SkeletonBlock(modifier = Modifier.weight(1f).height(80.dp)) }
    }
    Spacer(Modifier.height(14.dp))
    SkeletonBlock(modifier = Modifier.fillMaxWidth(0.4f).height(18.dp))
    Spacer(Modifier.height(8.dp))
    // Task rows
    repeat(4) {
        SkeletonBlock(modifier = Modifier.fillMaxWidth().height(72.dp))
        Spacer(Modifier.height(8.dp))
    }
}

/** Employee list: avatar + 2 text lines per row */
@Composable
private fun EmployeeListSkeleton() {
    // Search bar
    SkeletonBlock(modifier = Modifier.fillMaxWidth().height(48.dp))
    Spacer(Modifier.height(12.dp))
    repeat(6) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SkeletonBlock(modifier = Modifier.size(48.dp).clip(RoundedCornerShape(14.dp)))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                SkeletonBlock(modifier = Modifier.fillMaxWidth(0.55f).height(14.dp))
                SkeletonBlock(modifier = Modifier.fillMaxWidth(0.35f).height(11.dp))
            }
            SkeletonBlock(modifier = Modifier.width(52.dp).height(24.dp).clip(RoundedCornerShape(50.dp)))
        }
        Spacer(Modifier.height(14.dp))
    }
}

/** Employee detail: header + stats row + tab bar + 2 content cards */
@Composable
private fun EmployeeDetailSkeleton() {
    SkeletonBlock(modifier = Modifier.fillMaxWidth().height(220.dp).clip(RoundedCornerShape(0.dp)))
    Spacer(Modifier.height(12.dp))
    // Stats row
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        repeat(4) { SkeletonBlock(modifier = Modifier.weight(1f).height(60.dp)) }
    }
    Spacer(Modifier.height(10.dp))
    // Tab bar
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        repeat(4) { SkeletonBlock(modifier = Modifier.weight(1f).height(34.dp).clip(RoundedCornerShape(50.dp))) }
    }
    Spacer(Modifier.height(14.dp))
    SkeletonBlock(modifier = Modifier.fillMaxWidth().height(100.dp))
    Spacer(Modifier.height(10.dp))
    SkeletonBlock(modifier = Modifier.fillMaxWidth().height(80.dp))
}

/** Task kanban: 3 column headers + 2-3 card stubs per column */
@Composable
private fun TaskKanbanSkeleton() {
    SkeletonBlock(modifier = Modifier.fillMaxWidth(0.4f).height(18.dp))
    Spacer(Modifier.height(12.dp))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        repeat(3) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SkeletonBlock(modifier = Modifier.fillMaxWidth().height(28.dp).clip(RoundedCornerShape(50.dp)))
                repeat(3) { SkeletonBlock(modifier = Modifier.fillMaxWidth().height(80.dp)) }
            }
        }
    }
}

/** Attendance screen: date pills + stat tiles + map box + log rows */
@Composable
private fun AttendanceSkeleton() {
    SkeletonBlock(modifier = Modifier.fillMaxWidth().height(120.dp).clip(RoundedCornerShape(0.dp)))
    Spacer(Modifier.height(12.dp))
    // Date pills row
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        repeat(4) {
            SkeletonBlock(modifier = Modifier.width(80.dp).height(36.dp).clip(RoundedCornerShape(50.dp)))
        }
    }
    Spacer(Modifier.height(12.dp))
    // Stat tiles
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        repeat(3) { SkeletonBlock(modifier = Modifier.weight(1f).height(70.dp)) }
    }
    Spacer(Modifier.height(12.dp))
    // Map placeholder
    SkeletonBlock(modifier = Modifier.fillMaxWidth().height(220.dp))
    Spacer(Modifier.height(14.dp))
    SkeletonBlock(modifier = Modifier.fillMaxWidth(0.35f).height(18.dp))
    Spacer(Modifier.height(8.dp))
    repeat(4) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SkeletonBlock(modifier = Modifier.size(46.dp).clip(RoundedCornerShape(14.dp)))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                SkeletonBlock(modifier = Modifier.fillMaxWidth(0.5f).height(13.dp))
                SkeletonBlock(modifier = Modifier.fillMaxWidth(0.7f).height(11.dp))
            }
            SkeletonBlock(modifier = Modifier.width(56.dp).height(24.dp).clip(RoundedCornerShape(50.dp)))
        }
        Spacer(Modifier.height(12.dp))
    }
}

/** Inventory list: search bar + pill row + item rows */
@Composable
private fun InventoryListSkeleton() {
    SkeletonBlock(modifier = Modifier.fillMaxWidth().height(48.dp))
    Spacer(Modifier.height(10.dp))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        repeat(4) {
            SkeletonBlock(modifier = Modifier.width(72.dp).height(32.dp).clip(RoundedCornerShape(50.dp)))
        }
    }
    Spacer(Modifier.height(14.dp))
    repeat(5) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SkeletonBlock(modifier = Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                SkeletonBlock(modifier = Modifier.fillMaxWidth(0.55f).height(13.dp))
                SkeletonBlock(modifier = Modifier.fillMaxWidth(0.35f).height(10.dp))
            }
            SkeletonBlock(modifier = Modifier.width(60.dp).height(13.dp))
        }
        Spacer(Modifier.height(12.dp))
    }
}

/** Task detail: header + stats card + 3 section cards */
@Composable
private fun TaskDetailSkeleton() {
    SkeletonBlock(modifier = Modifier.fillMaxWidth().height(200.dp).clip(RoundedCornerShape(0.dp)))
    Spacer(Modifier.height(12.dp))
    // Stats card
    SkeletonBlock(modifier = Modifier.fillMaxWidth().height(72.dp))
    Spacer(Modifier.height(10.dp))
    // Section cards
    repeat(3) {
        SkeletonBlock(modifier = Modifier.fillMaxWidth().height(90.dp))
        Spacer(Modifier.height(10.dp))
    }
}

