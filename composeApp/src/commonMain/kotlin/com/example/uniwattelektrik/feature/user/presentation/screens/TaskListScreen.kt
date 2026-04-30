package com.example.uniwattelektrik.feature.user.presentation.screens

import com.example.uniwattelektrik.core.theme.appScreenBackground

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.uniwattelektrik.core.components.AppCard
import com.example.uniwattelektrik.core.components.EmptyState
import com.example.uniwattelektrik.core.sample.SampleTask
import com.example.uniwattelektrik.core.sample.TaskPriority
import com.example.uniwattelektrik.core.theme.AppTheme
import com.example.uniwattelektrik.core.theme.AppTypography
import com.example.uniwattelektrik.core.theme.SetStatusBar
import com.example.uniwattelektrik.feature.workforce.data.remote.TaskRecord
import com.example.uniwattelektrik.feature.workforce.presentation.WorkforceViewModel

@Composable
fun TaskListScreen(
    onTaskClick: (taskId: String) -> Unit,
    modifier: Modifier = Modifier,
    workforceVm: WorkforceViewModel? = null,
) {
    var query           by remember { mutableStateOf("") }
    var priorityFilter  by remember { mutableStateOf<TaskPriority?>(null) }

    // Live Firestore tasks assigned to this user (from observeTasksForUser).
    val liveTasks by (workforceVm?.tasks?.collectAsStateWithLifecycle()
        ?: remember { kotlinx.coroutines.flow.MutableStateFlow(emptyList<TaskRecord>()) }
            .collectAsStateWithLifecycle())

    val tasks = liveTasks
        .map { it.toSampleTask() }
        .filter { task ->
            (priorityFilter == null || task.priority == priorityFilter) &&
            (query.isBlank() || task.title.contains(query, ignoreCase = true)
                              || task.location.contains(query, ignoreCase = true))
        }

    Column(modifier = modifier.fillMaxSize().background(appScreenBackground())) {

        // Status bar matches the screen bg (light) so dark icons read crisply.
        SetStatusBar(color = AppTheme.Bg, darkIcons = true)

        val listState = rememberLazyListState()
        val scrolled by remember {
            derivedStateOf {
                listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 0
            }
        }

        // ── Sticky header (title + search + chips) ─────────────────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(elevation = if (scrolled) 6.dp else 0.dp, spotColor = AppTheme.ShadowSpotSoft)
                .background(AppTheme.Bg)
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                "Tasks",
                style = AppTypography.HeaderTitle.copy(color = AppTheme.Ink900),
            )

            // Search pill
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(AppTheme.Surface)
                    .shadow(8.dp, RoundedCornerShape(14.dp), spotColor = AppTheme.ShadowSpotSoft)
                    .padding(horizontal = 14.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("🔍", fontSize = 14.sp)
                    Spacer(Modifier.width(10.dp))
                    Text(
                        if (query.isBlank()) "Search by title or location" else query,
                        color = if (query.isBlank()) AppTheme.Ink300 else AppTheme.Ink900,
                        fontSize = 14.sp,
                    )
                }
            }

            // Priority filter chips
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(label = "All",    selected = priorityFilter == null,
                           onClick = { priorityFilter = null },
                           tint = AppTheme.Brand, bg = AppTheme.Brand50)
                FilterChip(label = "High",   selected = priorityFilter == TaskPriority.High,
                           onClick = { priorityFilter = TaskPriority.High },
                           tint = AppTheme.High, bg = AppTheme.HighBg)
                FilterChip(label = "Medium", selected = priorityFilter == TaskPriority.Med,
                           onClick = { priorityFilter = TaskPriority.Med },
                           tint = AppTheme.Med, bg = AppTheme.MedBg)
                FilterChip(label = "Low",    selected = priorityFilter == TaskPriority.Low,
                           onClick = { priorityFilter = TaskPriority.Low },
                           tint = AppTheme.Low, bg = AppTheme.LowBg)
            }
        }

        // ── List ───────────────────────────────────────────────────────────
        if (tasks.isEmpty()) {
            EmptyState(emoji = "📋", title = "No tasks match",
                       body = "Try a different filter or check back later.")
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                state = listState,
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 100.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(tasks, key = { it.id }) { task ->
                    TaskRow(task = task, onClick = { onTaskClick(task.id) })
                }
            }
        }
    }
}

@Composable
private fun FilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    tint: Color,
    bg: Color,
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) bg else AppTheme.Surface)
            .shadow(
                elevation = if (selected) 0.dp else 2.dp,
                shape     = RoundedCornerShape(10.dp),
                spotColor = AppTheme.ShadowSpotSoft,
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
    ) {
        Text(
            label,
            color      = if (selected) tint else AppTheme.Ink500,
            fontSize   = 12.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
        )
    }
}

@Composable
private fun TaskRow(task: SampleTask, onClick: () -> Unit) {
    AppCard(onClick = onClick, contentPadding = 0.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Priority strip
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(72.dp)
                    .clip(RoundedCornerShape(topEnd = 4.dp, bottomEnd = 4.dp))
                    .background(task.priority.color)
            )
            Spacer(Modifier.width(14.dp))
            Column(
                modifier = Modifier.weight(1f).padding(vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(task.title, color = AppTheme.Ink900, fontSize = 15.sp,
                     fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("📍 ${task.location} · ${task.distanceKm} km",
                     color = AppTheme.Ink500, fontSize = 12.sp)
                Row(verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatusPill(label = task.priority.label, color = task.priority.color, bg = task.priority.bg)
                    StatusPill(label = task.status.label,  color = task.status.color,  bg = task.status.bg)
                }
            }
            Column(
                modifier = Modifier.padding(end = 16.dp),
                horizontalAlignment = Alignment.End,
            ) {
                Text(task.time, color = AppTheme.Ink900, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Text(task.day, color = AppTheme.Ink500, fontSize = 11.sp)
            }
        }
    }
}

/** Maps a Firestore TaskRecord onto the UI's SampleTask shape. */
private fun TaskRecord.toSampleTask(): SampleTask = SampleTask(
    id = id,
    title = title,
    description = "",
    location = location,
    distanceKm = 0.0,
    time = time,
    day = day,
    priority = when (priority.lowercase()) {
        "high"   -> TaskPriority.High
        "low"    -> TaskPriority.Low
        else     -> TaskPriority.Med
    },
    status = when (status) {
        "InProgress" -> com.example.uniwattelektrik.core.sample.TaskStatus.InProgress
        "Done"       -> com.example.uniwattelektrik.core.sample.TaskStatus.Done
        else         -> com.example.uniwattelektrik.core.sample.TaskStatus.Todo
    },
    assigneeInitials = assigneeInitials.ifBlank { "??" },
)

@Composable
private fun StatusPill(label: String, color: Color, bg: Color) {
    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(bg)
            .padding(horizontal = 8.dp, vertical = 3.dp),
    ) {
        Text(label, color = color, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
    }
}
