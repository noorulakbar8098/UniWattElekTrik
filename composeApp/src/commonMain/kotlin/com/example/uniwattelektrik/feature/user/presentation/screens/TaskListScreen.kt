package com.example.uniwattelektrik.feature.user.presentation.screens

import com.example.uniwattelektrik.core.performance.TrackScreenPerformance

import androidx.compose.foundation.background
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.uniwattelektrik.core.components.DsEmptyState
import com.example.uniwattelektrik.core.components.DsFilterChip
import com.example.uniwattelektrik.core.components.DsGlassSearchBar
import com.example.uniwattelektrik.core.components.DsTaskCard
import com.example.uniwattelektrik.core.components.PremiumHeaderBackground
import com.example.uniwattelektrik.core.components.PremiumHeaderStatusBarColor
import com.example.uniwattelektrik.core.sample.SampleTask
import com.example.uniwattelektrik.core.sample.TaskPriority
import com.example.uniwattelektrik.core.sample.TaskStatus
import com.example.uniwattelektrik.core.theme.AppShapes
import com.example.uniwattelektrik.core.theme.AppTheme
import com.example.uniwattelektrik.core.theme.AppTypography
import com.example.uniwattelektrik.core.theme.SetStatusBar
import com.example.uniwattelektrik.core.theme.appScreenBackground
import com.example.uniwattelektrik.feature.workforce.data.remote.TaskRecord
import com.example.uniwattelektrik.feature.workforce.presentation.WorkforceViewModel
import kotlin.math.absoluteValue

/* ── Presentation helpers — pure functions, no side-effects ───────────── */

private fun TaskPriority.accentColor() = when (this) {
    TaskPriority.Danger  -> AppTheme.Danger
    TaskPriority.Warning -> AppTheme.Warning
    TaskPriority.Success -> AppTheme.Success
}

private fun progressFor(task: SampleTask): Float = when (task.status) {
    TaskStatus.Done       -> 1.0f
    TaskStatus.InProgress -> ((task.id.hashCode().absoluteValue % 70) + 30) / 100f
    else                  -> 0f
}

private fun taskCode(task: SampleTask): String {
    val n = (task.id.hashCode().absoluteValue % 9000) + 1000
    return "#TASK-$n"
}

/* ═══════════════════════════════════════════════════════════════════════════
 *  TaskListScreen
 * ═══════════════════════════════════════════════════════════════════════════ */

@Composable
fun TaskListScreen(
    onTaskClick : (taskId: String) -> Unit,
    modifier    : Modifier = Modifier,
    workforceVm : WorkforceViewModel? = null,
) {
    TrackScreenPerformance("TaskListScreen")
    var query          by remember { mutableStateOf("") }
    var priorityFilter by remember { mutableStateOf<TaskPriority?>(null) }

    val liveTasks by (workforceVm?.tasks?.collectAsStateWithLifecycle()
        ?: remember { kotlinx.coroutines.flow.MutableStateFlow(emptyList<TaskRecord>()) }
            .collectAsStateWithLifecycle())

    val allTasks = liveTasks.map { it.toSampleTask() }

    val filtered = allTasks.filter { task ->
        (priorityFilter == null || task.priority == priorityFilter) &&
        (query.isBlank()
            || task.title.contains(query, ignoreCase = true)
            || task.location.contains(query, ignoreCase = true))
    }

    val activeCount = remember(allTasks) { allTasks.count { it.status != TaskStatus.Done } }
    val doneCount   = remember(allTasks) { allTasks.count { it.status == TaskStatus.Done } }

    SetStatusBar(color = PremiumHeaderStatusBarColor, darkIcons = false)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(appScreenBackground()),
    ) {
        // ── Gradient header (fixed, does not scroll) ─────────────────────
        TaskListHeader(
            activeCount   = activeCount,
            doneCount     = doneCount,
            query         = query,
            onQueryChange = { query = it },
        )

        // ── Priority filter chips ─────────────────────────────────────────
        LazyRow(
            contentPadding        = PaddingValues(horizontal = AppTheme.SpLg),
            horizontalArrangement = Arrangement.spacedBy(AppTheme.SpSm),
            modifier              = Modifier
                .fillMaxWidth()
                .padding(top = AppTheme.SpMd, bottom = AppTheme.SpSm),
        ) {
            item {
                DsFilterChip(
                    label    = "All",
                    selected = priorityFilter == null,
                    tint     = AppTheme.Brand,
                    onClick  = { priorityFilter = null },
                )
            }
            item {
                DsFilterChip(
                    label    = "High",
                    selected = priorityFilter == TaskPriority.Danger,
                    tint     = AppTheme.Danger,
                    onClick  = { priorityFilter = TaskPriority.Danger },
                )
            }
            item {
                DsFilterChip(
                    label    = "Medium",
                    selected = priorityFilter == TaskPriority.Warning,
                    tint     = AppTheme.Warning,
                    onClick  = { priorityFilter = TaskPriority.Warning },
                )
            }
            item {
                DsFilterChip(
                    label    = "Low",
                    selected = priorityFilter == TaskPriority.Success,
                    tint     = AppTheme.Success,
                    onClick  = { priorityFilter = TaskPriority.Success },
                )
            }
        }

        // ── Task list / empty state ───────────────────────────────────────
        if (filtered.isEmpty()) {
            DsEmptyState(
                emoji = "📋",
                title = "No tasks match",
                body  = "Try a different filter or check back later.",
            )
        } else {
            LazyColumn(
                modifier            = Modifier.fillMaxSize(),
                contentPadding      = PaddingValues(
                    start  = AppTheme.SpLg,
                    end    = AppTheme.SpLg,
                    top    = AppTheme.SpSm,
                    bottom = 100.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(AppTheme.SpMd),
            ) {
                items(filtered, key = { it.id }) { task ->
                    DsTaskCard(
                        taskCode         = taskCode(task),
                        title            = task.title,
                        locationText     = "${task.location} · ${task.distanceKm} km",
                        dueText          = "${task.day}  ${task.time}",
                        accentColor      = task.priority.accentColor(),
                        priorityLabel    = task.priority.label,
                        statusLabel      = task.status.label,
                        statusColor      = task.status.color,
                        statusBg         = task.status.bg,
                        assigneeInitials = task.assigneeInitials,
                        showProgress     = task.status == TaskStatus.InProgress,
                        progressFraction = progressFor(task),
                        isCompleted      = task.status == TaskStatus.Done,
                        onClick          = { onTaskClick(task.id) },
                    )
                }
            }
        }
    }
}

/* ── Header ───────────────────────────────────────────────────────────── */

@Composable
private fun TaskListHeader(
    activeCount  : Int,
    doneCount    : Int,
    query        : String,
    onQueryChange: (String) -> Unit,
) {
    PremiumHeaderBackground(roundedBottom = true, cornerRadius = 30.dp) {
        Column(
            modifier = Modifier
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(horizontal = AppTheme.SpLg, vertical = AppTheme.SpXl),
        ) {
            // Eyebrow row: label + live count badge
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text  = "MY TASKS",
                    style = AppTypography.labelSmall.copy(
                        color         = Color.White.copy(alpha = 0.70f),
                        letterSpacing = 1.8.sp,
                    ),
                )
                Spacer(Modifier.weight(1f))
                Row(
                    modifier          = Modifier
                        .clip(AppShapes.pill)
                        .background(Color.White.copy(alpha = 0.18f))
                        .padding(horizontal = AppTheme.SpSm, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF6EE7B7)),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text  = "$activeCount active",
                        style = AppTypography.captionLarge.copy(
                            color      = Color.White,
                            fontWeight = FontWeight.SemiBold,
                        ),
                    )
                }
            }

            Spacer(Modifier.height(AppTheme.SpMd))

            // Display title
            Text(
                text  = "Task Board",
                style = AppTypography.displayMedium.copy(color = Color.White),
            )
            Spacer(Modifier.height(AppTheme.SpXs))
            Text(
                text  = "$activeCount active · $doneCount completed",
                style = AppTypography.bodyMedium.copy(color = Color.White.copy(alpha = 0.80f)),
            )

            Spacer(Modifier.height(AppTheme.SpXl))

            // Glass search bar embedded in the gradient header
            DsGlassSearchBar(
                query         = query,
                onQueryChange = onQueryChange,
                placeholder   = "Search tasks, locations…",
                onDark        = true,
            )
        }
    }
}

/* ── Mapper: Firestore TaskRecord → SampleTask ────────────────────────── */

private fun TaskRecord.toSampleTask(): SampleTask = SampleTask(
    id               = id,
    title            = title,
    description      = "",
    location         = location,
    distanceKm       = 0.0,
    time             = time,
    day              = day,
    priority         = when (priority.lowercase()) {
        "high"   -> TaskPriority.Danger
        "low"    -> TaskPriority.Success
        else     -> TaskPriority.Warning
    },
    status           = when (status) {
        "InProgress" -> TaskStatus.InProgress
        "Done"       -> TaskStatus.Done
        else         -> TaskStatus.Todo
    },
    assigneeInitials = assigneeInitials.ifBlank { "??" },
)
