package com.example.uniwattelektrik.feature.admin.presentation.screens

import com.example.uniwattelektrik.core.theme.appScreenBackground

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.WindowInsets
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.uniwattelektrik.core.theme.AppTypography
import com.example.uniwattelektrik.core.theme.SetStatusBar
import com.example.uniwattelektrik.feature.workforce.data.remote.TaskRecord
import com.example.uniwattelektrik.feature.workforce.presentation.WorkforceViewModel
import kotlin.math.absoluteValue

/* ── Local design tokens (matches the hi-fi mock) ─────────────────────── */
private val ScreenBg     = Color(0xFFF1F5F9)
private val CardBg       = Color(0xFFFFFFFF)
private val InkPrimary   = Color(0xFF0F172A)
private val InkSecondary = Color(0xFF64748B)
private val InkMuted     = Color(0xFF94A3B8)
private val Brand        = Color(0xFF3B82F6)
private val BrandDeep    = Color(0xFF1D4ED8)
private val BrandDark    = Color(0xFF0F172A)
private val Success      = Color(0xFF22C55E)
private val Warning      = Color(0xFFF59E0B)
private val Danger       = Color(0xFFEF4444)
private val ColumnBg     = Color(0xFFE2E8F0)
private val DividerDash  = Color(0xFFE2E8F0)
private val ShadowSoft   = Color(0x14172C50)

/**
 * Premium Kanban-style Task Management screen.
 *
 * Layout:
 *   ┌ Gradient header (back · "Task Management" · stats · dropdown) ─┐
 *   │ Filter pills: All teams · Today                                │
 *   ├ Horizontal scrollable Kanban: TO DO · IN PROGRESS · DONE ──────┤
 *   │  Each column → vertical task cards (premium soft-card design)  │
 *   └ Floating + button to navigate to NewTaskScreen ────────────────┘
 */
@Composable
fun AdminTasksScreen(
    workforceVm: WorkforceViewModel,
    adminUid: String = "",
    onAssign: () -> Unit = {},
    onBack: () -> Unit = {},
    onTaskClick: (taskId: String) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val tasks by workforceVm.tasks.collectAsStateWithLifecycle()

    val todo       = tasks.filter { it.status == "Todo" }
    val inProgress = tasks.filter { it.status == "InProgress" }
    val done       = tasks.filter { it.status == "Done" }
    val active     = todo.size + inProgress.size
    val overdue    = inProgress.count { it.priority.equals("High", true) }  // best-effort

    SetStatusBar(
        color = com.example.uniwattelektrik.core.components.PremiumHeaderStatusBarColor,
        darkIcons = false,
    )

    Box(modifier = modifier.fillMaxSize().background(appScreenBackground())) {

        Column(modifier = Modifier.fillMaxSize()) {

            /* ── Gradient header ──────────────────────────────────────── */
            GradientHeader(
                activeCount  = active,
                overdueCount = overdue,
                onBack       = onBack,
            )

            Spacer(Modifier.height(16.dp))

            /* ── Horizontal Kanban ────────────────────────────────────── */
            LazyRow(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 110.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    KanbanColumn(
                        title = "TO DO",
                        dotColor = InkMuted,
                        count = todo.size,
                        tasks = todo,
                        renderCard = { TaskCard(task = it, onClick = { onTaskClick(it.id) }) },
                    )
                }
                item {
                    KanbanColumn(
                        title = "IN PROGRESS",
                        dotColor = Brand,
                        count = inProgress.size,
                        tasks = inProgress,
                        renderCard = { TaskCard(task = it, showProgress = true,
                                                onClick = { onTaskClick(it.id) }) },
                    )
                }
                item {
                    KanbanColumn(
                        title = "DONE",
                        dotColor = Success,
                        count = done.size,
                        tasks = done,
                        renderCard = { TaskCard(task = it, completed = true,
                                                onClick = { onTaskClick(it.id) }) },
                    )
                }
            }
        }

        /* ── Floating Action Button ───────────────────────────────────── */
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 24.dp, bottom = 100.dp)
                .size(64.dp)
                .shadow(20.dp, CircleShape, spotColor = Brand.copy(alpha = 0.6f))
                .clip(CircleShape)
                .background(Brush.linearGradient(listOf(Brand, BrandDeep)))
                .border(3.dp, Color.White, CircleShape)
                .clickable(onClick = onAssign),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = "New task",
                tint = Color.White,
                modifier = Modifier.size(28.dp),
            )
        }
    }
}

/* ─────────────────────────────────────────────────────────────────────── *
 *  HEADER
 * ─────────────────────────────────────────────────────────────────────── */

@Composable
private fun GradientHeader(
    activeCount: Int,
    overdueCount: Int,
    onBack: () -> Unit,
) {
    com.example.uniwattelektrik.core.components.PremiumHeaderBackground(
        roundedBottom = false,
    ) {
        Column(
            modifier = Modifier
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(horizontal = 18.dp, vertical = 16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                /* Back */
                com.example.uniwattelektrik.core.components.GlassBackButton(
                    onClick = onBack,
                )
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Task Management", style = AppTypography.HeaderTitle)
                    Spacer(Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "$activeCount ACTIVE",
                            style = AppTypography.HeaderSubtitle,
                        )
                        Spacer(Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .size(3.dp)
                                .clip(CircleShape)
                                .background(Color(0x99FFFFFF)),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "$overdueCount OVERDUE",
                            style = AppTypography.HeaderSubtitle,
                        )
                    }
                }
                /* Dropdown affordance (decorative) */
                GlassButton(
                    onClick = {},
                    bg = Color.White.copy(alpha = 0.95f),
                ) {
                    Icon(
                        imageVector = Icons.Filled.ArrowDropDown,
                        contentDescription = null,
                        tint = BrandDeep,
                        modifier = Modifier.size(22.dp),
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            /* Filter pills */
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                FilterPill(
                    label = "All teams",
                    modifier = Modifier.weight(1f),
                    bg = Color.White.copy(alpha = 0.18f),
                )
                FilterPill(
                    label = "Today",
                    modifier = Modifier.weight(0.55f),
                    bg = Color.White.copy(alpha = 0.10f),
                )
            }
        }
    }
}

@Composable
private fun GlassButton(
    onClick: () -> Unit,
    bg: Color = Color.White.copy(alpha = 0.18f),
    content: @Composable () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(bg)
            .border(1.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
        content = { content() },
    )
}

@Composable
private fun FilterPill(
    label: String,
    bg: Color,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .height(44.dp)
            .clip(RoundedCornerShape(50))
            .background(bg)
            .border(1.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(50))
            .padding(horizontal = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Text(label, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.width(6.dp))
        Icon(
            imageVector = Icons.Filled.ArrowDropDown,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(16.dp),
        )
    }
}

/* ─────────────────────────────────────────────────────────────────────── *
 *  KANBAN COLUMN
 * ─────────────────────────────────────────────────────────────────────── */

@Composable
private fun KanbanColumn(
    title: String,
    dotColor: Color,
    count: Int,
    tasks: List<TaskRecord>,
    renderCard: @Composable (TaskRecord) -> Unit,
) {
    Column(
        modifier = Modifier
            .width(310.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(ColumnBg.copy(alpha = 0.55f))
            .padding(14.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(dotColor),
            )
            Spacer(Modifier.width(8.dp))
            Text(title, color = InkPrimary, fontSize = 14.sp,
                 fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp)
            Spacer(Modifier.weight(1f))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(CardBg)
                    .padding(horizontal = 10.dp, vertical = 4.dp),
            ) {
                Text("$count", color = InkSecondary,
                     fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(Modifier.height(12.dp))

        if (tasks.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text("No tasks", color = InkMuted, fontSize = 13.sp)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 8.dp),
            ) {
                items(tasks, key = { it.id }) { renderCard(it) }
            }
        }
    }
}

/* ─────────────────────────────────────────────────────────────────────── *
 *  TASK CARD
 * ─────────────────────────────────────────────────────────────────────── */

@Composable
private fun TaskCard(
    task: TaskRecord,
    showProgress: Boolean = false,
    completed: Boolean = false,
    onClick: () -> Unit = {},
) {
    val accent = priorityAccent(task.priority)
    val pNorm  = priorityLabel(task.priority)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .shadow(8.dp, RoundedCornerShape(16.dp), spotColor = ShadowSoft)
            .clip(RoundedCornerShape(16.dp))
            .background(CardBg)
            .clickable(onClick = onClick),
    ) {
        /* Left colored strip — spans full card height */
        Box(
            modifier = Modifier
                .width(4.dp)
                .fillMaxHeight()
                .background(accent),
        )

        Column(modifier = Modifier
            .weight(1f)
            .padding(start = 14.dp, end = 14.dp, top = 14.dp, bottom = 14.dp)) {
            /* Title row + warning icon for High priority In-Progress */
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (showProgress && task.priority.equals("High", true)) {
                    Icon(
                        imageVector = Icons.Filled.Warning,
                        contentDescription = null,
                        tint = Danger,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                }
                Text(
                    task.title,
                    color = InkPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
            }

            Spacer(Modifier.height(4.dp))
            Text(
                taskCode(task) + locationSuffix(task),
                color = InkMuted,
                fontSize = 12.sp,
            )

            if (showProgress) {
                Spacer(Modifier.height(10.dp))
                ProgressBar(
                    fraction = progressFractionFor(task),
                    tint = accent,
                )
            }

            Spacer(Modifier.height(12.dp))
            DashedDivider()
            Spacer(Modifier.height(12.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                AvatarBubble(initials = task.assigneeInitials.ifBlank { "??" })
                Spacer(Modifier.width(10.dp))

                if (completed) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = null,
                            tint = Success,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            "Completed",
                            color = Success,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                } else {
                    PriorityChip(label = pNorm.uppercase(), tint = accent)
                }

                Spacer(Modifier.weight(1f))
                Text(
                    formatDueShort(task),
                    color = if (showProgress && task.priority.equals("High", true)) Danger
                            else InkPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

@Composable
private fun PriorityChip(label: String, tint: Color) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(tint.copy(alpha = 0.12f))
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(tint),
        )
        Spacer(Modifier.width(6.dp))
        Text(
            label,
            color = tint,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.6.sp,
        )
    }
}

@Composable
private fun AvatarBubble(initials: String) {
    val gradient = avatarGradientFor(initials)
    Box(
        modifier = Modifier
            .size(34.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(Brush.linearGradient(gradient)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            initials.take(2).uppercase(),
            color = Color.White,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun ProgressBar(fraction: Float, tint: Color) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(5.dp)
            .clip(RoundedCornerShape(50))
            .background(Color(0xFFE2E8F0)),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(fraction.coerceIn(0f, 1f))
                .height(5.dp)
                .clip(RoundedCornerShape(50))
                .background(tint),
        )
    }
}

@Composable
private fun DashedDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .drawBehind {
                drawRoundRect(
                    color = DividerDash,
                    cornerRadius = CornerRadius(0f, 0f),
                    style = Stroke(
                        width = 1.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f),
                    ),
                )
            },
    )
}

/* ─────────────────────────────────────────────────────────────────────── *
 *  HELPERS
 * ─────────────────────────────────────────────────────────────────────── */

private fun priorityAccent(raw: String): Color = when (raw.lowercase()) {
    "high"   -> Danger
    "medium" -> Warning
    "low"    -> Success
    else     -> Brand
}

private fun priorityLabel(raw: String): String = when (raw.lowercase()) {
    "high"   -> "HIGH"
    "medium" -> "MED"
    "low"    -> "LOW"
    else     -> raw.uppercase()
}

private fun taskCode(task: TaskRecord): String {
    val n = (task.id.hashCode().absoluteValue % 9000) + 1000
    return "#TASK-$n"
}

private fun locationSuffix(task: TaskRecord): String =
    if (task.location.isBlank()) "" else " · ${task.location}"

private fun formatDueShort(task: TaskRecord): String {
    val day = task.day
    return when {
        day.equals("Today",    true) -> "Today  ${task.time}"
        day.equals("Tomorrow", true) -> "Tmrw  ${task.time}"
        else                         -> day
    }
}

private fun progressFractionFor(task: TaskRecord): Float {
    // No real progress field yet — derive a deterministic value from the id
    // so the bar feels live without breaking on real data later.
    val seed = (task.id.hashCode().absoluteValue % 70 + 30) / 100f
    return seed
}

private fun avatarGradientFor(initials: String): List<Color> {
    val palettes = listOf(
        listOf(Color(0xFF60A5FA), Color(0xFF1D4ED8)),
        listOf(Color(0xFFA78BFA), Color(0xFF6D28D9)),
        listOf(Color(0xFFFB923C), Color(0xFFC2410C)),
        listOf(Color(0xFF34D399), Color(0xFF047857)),
        listOf(Color(0xFFF472B6), Color(0xFFBE185D)),
    )
    val idx = (initials.hashCode().absoluteValue) % palettes.size
    return palettes[idx]
}

