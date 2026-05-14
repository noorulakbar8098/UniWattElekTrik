package com.example.uniwattelektrik.feature.admin.presentation.screens

import com.example.uniwattelektrik.core.performance.TrackScreenPerformance

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TabRowDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.uniwattelektrik.core.components.AppPullToRefresh
import com.example.uniwattelektrik.core.components.DsAvatarBubble
import com.example.uniwattelektrik.core.components.DsAvatarStack
import com.example.uniwattelektrik.core.components.DsEmptyState
import com.example.uniwattelektrik.core.components.DsPriorityChip
import com.example.uniwattelektrik.core.components.DsProgressBar
import com.example.uniwattelektrik.core.components.DsStatusChip
import com.example.uniwattelektrik.core.components.OperationsHeader
import com.example.uniwattelektrik.core.components.OperationsHeaderStatusBarColor
import com.example.uniwattelektrik.core.theme.AppElevation
import com.example.uniwattelektrik.core.theme.AppShapes
import com.example.uniwattelektrik.core.theme.AppTheme
import com.example.uniwattelektrik.core.theme.AppTypography
import com.example.uniwattelektrik.core.theme.SetStatusBar
import com.example.uniwattelektrik.core.theme.appScreenBackground
import com.example.uniwattelektrik.core.theme.premiumLayeredShadow
import com.example.uniwattelektrik.feature.workforce.data.remote.TaskRecord
import com.example.uniwattelektrik.feature.workforce.data.remote.allAssigneeNames
import com.example.uniwattelektrik.feature.workforce.presentation.WorkforceViewModel
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue

// ─── Design tokens ────────────────────────────────────────────────────────────

private val TabActiveTint      = AppTheme.Brand
private val TabInactiveTint    = AppTheme.Ink500
private val TabBadgeActiveBg   = AppTheme.Brand50
private val TabBadgeInactiveBg = AppTheme.Ink50
private val TabRowBg           = AppTheme.Surface

// ─── Workflow tab definitions ─────────────────────────────────────────────────

private enum class WorkflowTab(
    val label      : String,
    val statusKey  : String,
    val emptyEmoji : String,
    val emptyTitle : String,
    val emptyBody  : String,
) {
    Pending(
        label      = "Pending",
        statusKey  = "Todo",
        emptyEmoji = "📋",
        emptyTitle = "No pending tasks",
        emptyBody  = "New tasks will appear here once created.",
    ),
    InProgress(
        label      = "In Progress",
        statusKey  = "InProgress",
        emptyEmoji = "⚡",
        emptyTitle = "Nothing in progress",
        emptyBody  = "Tasks move here when a team member starts work.",
    ),
    Review(
        label      = "Review",
        statusKey  = "Review",
        emptyEmoji = "🔍",
        emptyTitle = "No tasks in review",
        emptyBody  = "Send completed work here for admin sign-off.",
    ),
    Done(
        label      = "Done",
        statusKey  = "Done",
        emptyEmoji = "✅",
        emptyTitle = "No completed tasks",
        emptyBody  = "Finished tasks are archived here.",
    ),
}

// ─── Sort / filter model ──────────────────────────────────────────────────────

private enum class TaskSort(val label: String) {
    NewestFirst("Newest first"),
    OldestFirst("Oldest first"),
    PriorityHighLow("Priority (High → Low)"),
    DueSoonest("Due date (Soonest)"),
    TitleAZ("Title (A → Z)"),
}

private enum class PriorityFilter(val label: String) {
    All("All priorities"),
    High("High only"),
    Medium("Medium only"),
    Low("Low only"),
}

private enum class AssignmentFilter(val label: String) {
    All("All tasks"),
    Assigned("Assigned only"),
    Unassigned("Unassigned only"),
}

private fun priorityRank(raw: String): Int = when (raw.lowercase()) {
    "danger", "high"  -> 0
    "medium", "med"   -> 1
    "success", "low"  -> 2
    else              -> 3
}

private fun matchesPriority(task: TaskRecord, filter: PriorityFilter): Boolean =
    when (filter) {
        PriorityFilter.All    -> true
        PriorityFilter.High   -> task.priority.equals("Danger", true) || task.priority.equals("High", true)
        PriorityFilter.Medium -> task.priority.equals("Medium", true) || task.priority.equals("Med", true)
        PriorityFilter.Low    -> task.priority.equals("Success", true) || task.priority.equals("Low", true)
    }

private fun matchesAssignment(task: TaskRecord, filter: AssignmentFilter): Boolean =
    when (filter) {
        AssignmentFilter.All        -> true
        AssignmentFilter.Assigned   -> !task.userId.isNullOrBlank()
        AssignmentFilter.Unassigned -> task.userId.isNullOrBlank()
    }

private fun matchesQuery(task: TaskRecord, q: String): Boolean {
    if (q.isBlank()) return true
    val needle = q.trim().lowercase()
    return task.title.lowercase().contains(needle)
        || task.location.lowercase().contains(needle)
        || task.assigneeName.lowercase().contains(needle)
        || task.description.lowercase().contains(needle)
        || taskCode(task).lowercase().contains(needle)
}

private fun applySort(tasks: List<TaskRecord>, sort: TaskSort): List<TaskRecord> = when (sort) {
    TaskSort.NewestFirst     -> tasks.sortedByDescending { it.createdAtMs ?: 0L }
    TaskSort.OldestFirst     -> tasks.sortedBy           { it.createdAtMs ?: Long.MAX_VALUE }
    TaskSort.PriorityHighLow -> tasks.sortedWith(
        compareBy<TaskRecord> { priorityRank(it.priority) }
            .thenByDescending { it.createdAtMs ?: 0L },
    )
    TaskSort.DueSoonest      -> tasks.sortedBy {
        it.scheduledDateMs ?: it.dueDate ?: Long.MAX_VALUE
    }
    TaskSort.TitleAZ         -> tasks.sortedBy { it.title.lowercase() }
}

/* ═══════════════════════════════════════════════════════════════════════════
 *  AdminTasksScreen — tabbed workflow interface
 * ═══════════════════════════════════════════════════════════════════════════ */

@Composable
fun AdminTasksScreen(
    workforceVm : WorkforceViewModel,
    adminUid    : String = "",
    onAssign    : () -> Unit = {},
    onBack      : () -> Unit = {},
    onTaskClick : (taskId: String) -> Unit = {},
    onEditTask  : (taskId: String) -> Unit = {},
    modifier    : Modifier = Modifier,
    /**
     * Status key of the workflow tab to focus on first composition (one of
     * "Todo", "InProgress", "Review", "Done"). Null = leave default.
     */
    initialTabKey: String? = null,
    /** Invoked after [initialTabKey] has been honoured so callers can clear it. */
    onInitialTabConsumed: () -> Unit = {},
) {
    TrackScreenPerformance("AdminTasksScreen")
    val allTasks by workforceVm.tasks.collectAsStateWithLifecycle()
    val tasksLoading by workforceVm.loading.collectAsStateWithLifecycle()

    // ─── Search / sort / filter state ───────────────────────────────────────
    var query             by remember { mutableStateOf("") }
    var sort              by remember { mutableStateOf(TaskSort.NewestFirst) }
    var priorityFilter    by remember { mutableStateOf(PriorityFilter.All) }
    var assignmentFilter  by remember { mutableStateOf(AssignmentFilter.All) }

    val filtersActive by remember {
        derivedStateOf {
            query.isNotBlank()
                || priorityFilter != PriorityFilter.All
                || assignmentFilter != AssignmentFilter.All
                || sort != TaskSort.NewestFirst
        }
    }

    // Per-tab task lists after search + filter + sort. Recomputed only when an
    // upstream input changes — so swiping pages is allocation-free.
    val tabTasks = remember(allTasks, query, sort, priorityFilter, assignmentFilter) {
        WorkflowTab.entries.map { tab ->
            val byStatus = allTasks.filter { it.status == tab.statusKey }
            val filtered = byStatus.filter {
                matchesQuery(it, query)
                    && matchesPriority(it, priorityFilter)
                    && matchesAssignment(it, assignmentFilter)
            }
            applySort(filtered, sort)
        }
    }

    // Header counts always reflect the unfiltered totals so the user sees the
    // true workload, not the slice the current filters expose.
    val activeCount = remember(allTasks) {
        allTasks.count { it.status in setOf("Todo", "InProgress", "Review") }
    }
    val doneCount = remember(allTasks) { allTasks.count { it.status == "Done" } }

    val pagerState = rememberPagerState { WorkflowTab.entries.size }
    val scope      = rememberCoroutineScope()

    // Apply caller-requested initial tab once, then clear so it doesn't
    // override manual swipes on every recomposition.
    LaunchedEffect(initialTabKey) {
        val key = initialTabKey ?: return@LaunchedEffect
        val idx = WorkflowTab.entries.indexOfFirst { it.statusKey.equals(key, ignoreCase = true) }
        if (idx >= 0 && idx != pagerState.currentPage) {
            pagerState.scrollToPage(idx)
        }
        onInitialTabConsumed()
    }

    SetStatusBar(color = OperationsHeaderStatusBarColor, darkIcons = false)

    Box(modifier = modifier.fillMaxSize().background(appScreenBackground())) {

        Column(modifier = Modifier.fillMaxSize()) {

            // Gradient header
            TasksScreenHeader(activeCount = activeCount, doneCount = doneCount)

            // Tab strip
            TasksTabRow(
                pagerState = pagerState,
                tabCounts  = tabTasks.map { it.size },
                onTabClick = { idx -> scope.launch { pagerState.animateScrollToPage(idx) } },
            )

            // Search + sort + filter toolbar
            TasksToolbar(
                query              = query,
                onQueryChange      = { query = it },
                sort               = sort,
                onSortChange       = { sort = it },
                priorityFilter     = priorityFilter,
                onPriorityChange   = { priorityFilter = it },
                assignmentFilter   = assignmentFilter,
                onAssignmentChange = { assignmentFilter = it },
                filtersActive      = filtersActive,
                onClearAll         = {
                    query             = ""
                    sort              = TaskSort.NewestFirst
                    priorityFilter    = PriorityFilter.All
                    assignmentFilter  = AssignmentFilter.All
                },
            )

            // Paged content
            if (tasksLoading && allTasks.isEmpty()) {
                com.example.uniwattelektrik.core.components.InlineSkeleton(
                    type = com.example.uniwattelektrik.core.components.SkeletonType.TaskKanban,
                    modifier = Modifier.weight(1f),
                )
            } else {
            HorizontalPager(
                state    = pagerState,
                modifier = Modifier.weight(1f),
            ) { page ->
                val tab = WorkflowTab.entries[page]
                TaskTabPage(
                    tab            = tab,
                    tasks          = tabTasks[page],
                    filtersActive  = filtersActive,
                    onClearFilters = {
                        query             = ""
                        sort              = TaskSort.NewestFirst
                        priorityFilter    = PriorityFilter.All
                        assignmentFilter  = AssignmentFilter.All
                    },
                    onTaskClick    = onTaskClick,
                    onEditTask     = onEditTask,
                    onStatusChange = { task, status ->
                        workforceVm.changeTaskStatus(task.id, adminUid, task.userId, status)
                    },
                )
            }
            }
        }

        // FAB
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = AppTheme.SpXl, bottom = 100.dp)
                .size(AppTheme.FabSize)
                .shadow(AppElevation.fab, CircleShape, spotColor = AppTheme.Brand.copy(alpha = 0.55f))
                .clip(CircleShape)
                .background(Brush.linearGradient(listOf(AppTheme.Brand, AppTheme.Brand700)))
                .border(3.dp, Color.White, CircleShape)
                .clickable(onClick = onAssign),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Filled.Add,
                contentDescription = "New task",
                tint               = Color.White,
                modifier           = Modifier.size(AppTheme.IconXl),
            )
        }
    }
}

/* ─── Header ─────────────────────────────────────────────────────────────── */

@Composable
private fun TasksScreenHeader(activeCount: Int, doneCount: Int) {
    OperationsHeader(
        eyebrow  = "TASK BOARD",
        title    = "Task Management",
        subtitle = "$activeCount active · $doneCount completed",
        actions  = {
            Box(
                modifier = Modifier
                    .clip(AppShapes.pill)
                    .background(Color.White.copy(alpha = 0.10f))
                    .padding(horizontal = 10.dp, vertical = 5.dp),
            ) {
                Text(
                    "$activeCount active",
                    style = AppTypography.captionLarge.copy(
                        color      = Color.White,
                        fontWeight = FontWeight.SemiBold,
                    ),
                )
            }
        },
    )
}

/* ─── Tab row ────────────────────────────────────────────────────────────── */

@Composable
private fun TasksTabRow(
    pagerState: PagerState,
    tabCounts : List<Int>,
    onTabClick: (Int) -> Unit,
) {
    val tabs = WorkflowTab.entries
    ScrollableTabRow(
        selectedTabIndex = pagerState.currentPage,
        containerColor   = TabRowBg,
        contentColor     = TabActiveTint,
        edgePadding      = AppTheme.SpSm,
        indicator = { tabPositions ->
            if (tabPositions.isNotEmpty() && pagerState.currentPage < tabPositions.size) {
                with(TabRowDefaults) {
                    Box(
                        Modifier
                            .tabIndicatorOffset(tabPositions[pagerState.currentPage])
                            .padding(horizontal = AppTheme.SpMd)
                            .height(3.dp)
                            .clip(AppShapes.pill)
                            .background(TabActiveTint),
                    )
                }
            }
        },
        divider = {
            HorizontalDivider(color = AppTheme.Ink100, thickness = 1.dp)
        },
    ) {
        tabs.forEachIndexed { idx, tab ->
            val selected = pagerState.currentPage == idx
            val count    = tabCounts.getOrElse(idx) { 0 }
            Tab(
                selected               = selected,
                onClick                = { onTabClick(idx) },
                selectedContentColor   = TabActiveTint,
                unselectedContentColor = TabInactiveTint,
            ) {
                Row(
                    modifier          = Modifier.padding(
                        horizontal = AppTheme.SpSm,
                        vertical   = AppTheme.SpMd,
                    ),
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(AppTheme.SpXs),
                ) {
                    Text(
                        tab.label,
                        style = AppTypography.labelLarge.copy(
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                        ),
                    )
                    if (count > 0) {
                        Box(
                            modifier = Modifier
                                .clip(AppShapes.pill)
                                .background(if (selected) TabBadgeActiveBg else TabBadgeInactiveBg)
                                .padding(horizontal = 6.dp, vertical = 2.dp),
                        ) {
                            Text(
                                "$count",
                                style = AppTypography.captionSmall.copy(
                                    color      = if (selected) TabActiveTint else TabInactiveTint,
                                    fontWeight = FontWeight.Bold,
                                ),
                            )
                        }
                    }
                }
            }
        }
    }
}

/* ─── Search + sort + filter toolbar ─────────────────────────────────────── */

@Composable
private fun TasksToolbar(
    query              : String,
    onQueryChange      : (String) -> Unit,
    sort               : TaskSort,
    onSortChange       : (TaskSort) -> Unit,
    priorityFilter     : PriorityFilter,
    onPriorityChange   : (PriorityFilter) -> Unit,
    assignmentFilter   : AssignmentFilter,
    onAssignmentChange : (AssignmentFilter) -> Unit,
    filtersActive      : Boolean,
    onClearAll         : () -> Unit,
) {
    var sortMenu   by remember { mutableStateOf(false) }
    var filterMenu by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(TabRowBg)
            .padding(horizontal = AppTheme.SpLg, vertical = AppTheme.SpSm),
    ) {
        Row(
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppTheme.SpSm),
            modifier              = Modifier.fillMaxWidth(),
        ) {

            // ── Search box ──────────────────────────────────────────────────
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .weight(1f)
                    .height(40.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(AppTheme.Ink50)
                    .padding(horizontal = AppTheme.SpMd),
            ) {
                Icon(
                    Icons.Filled.Search,
                    contentDescription = null,
                    tint               = AppTheme.Ink500,
                    modifier           = Modifier.size(AppTheme.IconSm),
                )
                Spacer(Modifier.width(AppTheme.SpSm))
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                    if (query.isEmpty()) {
                        Text(
                            "Search title, location, assignee…",
                            style = AppTypography.bodySmall.copy(color = AppTheme.Ink300),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    BasicTextField(
                        value          = query,
                        onValueChange  = onQueryChange,
                        singleLine     = true,
                        textStyle      = LocalTextStyle.current.merge(
                            AppTypography.bodySmall.copy(color = AppTheme.Ink900),
                        ),
                        cursorBrush    = SolidColor(AppTheme.Brand),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        modifier       = Modifier.fillMaxWidth(),
                    )
                }
                if (query.isNotEmpty()) {
                    IconButton(
                        onClick  = { onQueryChange("") },
                        modifier = Modifier.size(28.dp),
                    ) {
                        Icon(
                            Icons.Filled.Close,
                            contentDescription = "Clear search",
                            tint               = AppTheme.Ink500,
                            modifier           = Modifier.size(AppTheme.IconSm),
                        )
                    }
                }
            }

            // ── Sort button ─────────────────────────────────────────────────
            ToolbarChipButton(
                icon          = Icons.Filled.SwapVert,
                contentDesc   = "Sort tasks",
                indicatorOn   = sort != TaskSort.NewestFirst,
                onClick       = { sortMenu = true },
            ) {
                DropdownMenu(
                    expanded         = sortMenu,
                    onDismissRequest = { sortMenu = false },
                    containerColor   = AppTheme.Surface,
                ) {
                    Text(
                        "Sort by",
                        style    = AppTypography.captionSmall.copy(
                            color      = AppTheme.Ink500,
                            fontWeight = FontWeight.SemiBold,
                        ),
                        modifier = Modifier.padding(
                            horizontal = AppTheme.SpMd,
                            vertical   = AppTheme.SpXs,
                        ),
                    )
                    TaskSort.entries.forEach { option ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    option.label,
                                    style = AppTypography.bodySmall.copy(
                                        color      = AppTheme.Ink700,
                                        fontWeight = if (option == sort) FontWeight.SemiBold else FontWeight.Normal,
                                    ),
                                )
                            },
                            trailingIcon = {
                                if (option == sort) {
                                    Icon(
                                        Icons.Filled.Check,
                                        contentDescription = null,
                                        tint               = AppTheme.Brand,
                                        modifier           = Modifier.size(AppTheme.IconSm),
                                    )
                                }
                            },
                            onClick = {
                                sortMenu = false
                                onSortChange(option)
                            },
                        )
                    }
                }
            }

            // ── Filter button ───────────────────────────────────────────────
            ToolbarChipButton(
                icon          = Icons.Filled.FilterList,
                contentDesc   = "Filter tasks",
                indicatorOn   = priorityFilter != PriorityFilter.All
                    || assignmentFilter != AssignmentFilter.All,
                onClick       = { filterMenu = true },
            ) {
                DropdownMenu(
                    expanded         = filterMenu,
                    onDismissRequest = { filterMenu = false },
                    containerColor   = AppTheme.Surface,
                ) {
                    Text(
                        "Priority",
                        style    = AppTypography.captionSmall.copy(
                            color      = AppTheme.Ink500,
                            fontWeight = FontWeight.SemiBold,
                        ),
                        modifier = Modifier.padding(
                            horizontal = AppTheme.SpMd,
                            vertical   = AppTheme.SpXs,
                        ),
                    )
                    PriorityFilter.entries.forEach { option ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    option.label,
                                    style = AppTypography.bodySmall.copy(
                                        color      = AppTheme.Ink700,
                                        fontWeight = if (option == priorityFilter) FontWeight.SemiBold else FontWeight.Normal,
                                    ),
                                )
                            },
                            trailingIcon = {
                                if (option == priorityFilter) {
                                    Icon(
                                        Icons.Filled.Check,
                                        contentDescription = null,
                                        tint               = AppTheme.Brand,
                                        modifier           = Modifier.size(AppTheme.IconSm),
                                    )
                                }
                            },
                            onClick = { onPriorityChange(option) },
                        )
                    }
                    HorizontalDivider(
                        color    = AppTheme.Ink100,
                        modifier = Modifier.padding(vertical = AppTheme.SpXs),
                    )
                    Text(
                        "Assignment",
                        style    = AppTypography.captionSmall.copy(
                            color      = AppTheme.Ink500,
                            fontWeight = FontWeight.SemiBold,
                        ),
                        modifier = Modifier.padding(
                            horizontal = AppTheme.SpMd,
                            vertical   = AppTheme.SpXs,
                        ),
                    )
                    AssignmentFilter.entries.forEach { option ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    option.label,
                                    style = AppTypography.bodySmall.copy(
                                        color      = AppTheme.Ink700,
                                        fontWeight = if (option == assignmentFilter) FontWeight.SemiBold else FontWeight.Normal,
                                    ),
                                )
                            },
                            trailingIcon = {
                                if (option == assignmentFilter) {
                                    Icon(
                                        Icons.Filled.Check,
                                        contentDescription = null,
                                        tint               = AppTheme.Brand,
                                        modifier           = Modifier.size(AppTheme.IconSm),
                                    )
                                }
                            },
                            onClick = { onAssignmentChange(option) },
                        )
                    }
                }
            }
        }

        // ── "Filters active" thin banner with clear-all action ──────────────
        if (filtersActive) {
            Spacer(Modifier.height(AppTheme.SpXs))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(AppShapes.pill)
                    .background(AppTheme.Brand50)
                    .padding(horizontal = AppTheme.SpMd, vertical = 6.dp),
            ) {
                Icon(
                    Icons.Filled.FilterList,
                    contentDescription = null,
                    tint               = AppTheme.Brand,
                    modifier           = Modifier.size(AppTheme.IconSm),
                )
                Spacer(Modifier.width(AppTheme.SpSm))
                Text(
                    "Filters active",
                    style    = AppTypography.captionSmall.copy(
                        color      = AppTheme.Brand,
                        fontWeight = FontWeight.SemiBold,
                    ),
                    modifier = Modifier.weight(1f),
                )
                Text(
                    "Clear all",
                    style    = AppTypography.captionSmall.copy(
                        color      = AppTheme.Brand,
                        fontWeight = FontWeight.SemiBold,
                    ),
                    modifier = Modifier
                        .clip(AppShapes.pill)
                        .clickable(onClick = onClearAll)
                        .padding(horizontal = AppTheme.SpSm, vertical = 2.dp),
                )
            }
        }
    }
}

/** Square chip-style button used for sort/filter; wraps the dropdown anchor. */
@Composable
private fun ToolbarChipButton(
    icon       : androidx.compose.ui.graphics.vector.ImageVector,
    contentDesc: String,
    indicatorOn: Boolean,
    onClick    : () -> Unit,
    menu       : @Composable () -> Unit,
) {
    Box {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(if (indicatorOn) AppTheme.Brand50 else AppTheme.Ink50)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                icon,
                contentDescription = contentDesc,
                tint               = if (indicatorOn) AppTheme.Brand else AppTheme.Ink700,
                modifier           = Modifier.size(AppTheme.IconSm),
            )
            if (indicatorOn) {
                // Small dot indicator in the corner
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(AppTheme.Brand),
                )
            }
        }
        menu()
    }
}

/* ─── Tab page ───────────────────────────────────────────────────────────── */

@Composable
private fun TaskTabPage(
    tab           : WorkflowTab,
    tasks         : List<TaskRecord>,
    filtersActive : Boolean,
    onClearFilters: () -> Unit,
    onTaskClick   : (taskId: String) -> Unit,
    onEditTask    : (taskId: String) -> Unit,
    onStatusChange: (TaskRecord, String) -> Unit,
) {
    AppPullToRefresh(onRefresh = {}) {
        if (tasks.isEmpty()) {
            if (filtersActive) {
                DsEmptyState(
                    emoji = "🔎",
                    title = "No matching tasks",
                    body  = "Try a different search term or clear the filters.",
                )
            } else {
                DsEmptyState(
                    emoji = tab.emptyEmoji,
                    title = tab.emptyTitle,
                    body  = tab.emptyBody,
                )
            }
        } else {
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
                items(tasks, key = { it.id }) { task ->
                    AdminWorkflowCard(
                        task           = task,
                        onClick        = { onTaskClick(task.id) },
                        onEditTask     = { onEditTask(task.id) },
                        onStatusChange = onStatusChange,
                    )
                }
            }
        }
    }
}

/* ─── Task card ──────────────────────────────────────────────────────────── */

@Composable
private fun AdminWorkflowCard(
    task          : TaskRecord,
    onClick       : () -> Unit,
    onEditTask    : () -> Unit,
    onStatusChange: (TaskRecord, String) -> Unit,
) {
    val accent      = priorityAccent(task.priority)
    val priorityLbl = priorityLabel(task.priority)
    val assigneeNames = task.allAssigneeNames()
    val displayName = when {
        assigneeNames.isEmpty() -> "Unassigned"
        assigneeNames.size == 1 -> assigneeNames.first()
        else -> "${assigneeNames.first()} +${assigneeNames.size - 1}"
    }
    val avatarInitialsList = deriveAvatarLettersList(task)

    var menuExpanded by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            // Premium two-layer shadow shared with Leave / Employees / Stock
            // cards — wide accent-tinted halo for the floating feel + a tight
            // ink edge for crisp silhouette.
            .premiumLayeredShadow(accentColor = accent, shape = AppShapes.card)
            .clip(AppShapes.card)
            .background(AppTheme.Surface)
            .clickable(onClick = onClick),
    ) {
        // Subtle diagonal tint
        Box(
            modifier = Modifier.matchParentSize().background(
                Brush.linearGradient(
                    colors = listOf(accent.copy(alpha = 0.05f), Color.Transparent),
                    start  = Offset(Float.POSITIVE_INFINITY, 0f),
                    end    = Offset(0f, Float.POSITIVE_INFINITY),
                ),
            ),
        )

        Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {

            // Priority accent strip
            Box(
                modifier = Modifier
                    .width(AppTheme.AccentStripWidth)
                    .fillMaxHeight()
                    .background(
                        Brush.verticalGradient(listOf(accent, accent.copy(alpha = 0.50f))),
                    ),
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(
                        start   = AppTheme.SpMd,
                        end     = AppTheme.SpXs,
                        top     = AppTheme.SpMd,
                        bottom  = AppTheme.SpMd,
                    ),
            ) {

                // Row 1: Priority badge + task code + action menu
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier          = Modifier.fillMaxWidth(),
                ) {
                    DsPriorityChip(label = priorityLbl, tint = accent)
                    Spacer(Modifier.width(AppTheme.SpSm))
                    Text(
                        taskCode(task),
                        style    = AppTypography.captionSmall.copy(color = AppTheme.Ink300),
                        modifier = Modifier.weight(1f),
                    )
                    Box {
                        IconButton(
                            onClick  = { menuExpanded = true },
                            modifier = Modifier.size(32.dp),
                        ) {
                            Icon(
                                Icons.Filled.MoreVert,
                                contentDescription = "Task actions",
                                tint               = AppTheme.Ink500,
                                modifier           = Modifier.size(AppTheme.IconMd),
                            )
                        }
                        TaskActionMenu(
                            expanded       = menuExpanded,
                            taskStatus     = task.status,
                            onDismiss      = { menuExpanded = false },
                            onViewDetails  = { menuExpanded = false; onClick() },
                            onEditTask     = { menuExpanded = false; onEditTask() },
                            onStatusChange = { newStatus ->
                                menuExpanded = false
                                onStatusChange(task, newStatus)
                            },
                        )
                    }
                }

                Spacer(Modifier.height(AppTheme.SpSm))

                // Row 2: Title
                Text(
                    task.title,
                    style    = AppTypography.titleMedium.copy(
                        color      = AppTheme.Ink900,
                        lineHeight = 22.sp,
                    ),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )

                // Row 3: Location + due date
                val loc = task.location.trim()
                val due = formatDue(task)
                if (loc.isNotBlank() || due.isNotBlank()) {
                    Spacer(Modifier.height(AppTheme.SpXs))
                    Row(
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(AppTheme.SpSm),
                        modifier              = Modifier.fillMaxWidth(),
                    ) {
                        if (loc.isNotBlank()) {
                            Box(
                                modifier = Modifier
                                    .clip(AppShapes.small)
                                    .background(AppTheme.Ink50)
                                    .padding(horizontal = AppTheme.SpSm, vertical = 3.dp),
                            ) {
                                Text(
                                    loc,
                                    style    = AppTypography.captionSmall.copy(color = AppTheme.Ink500),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                        if (due.isNotBlank()) {
                            Text(
                                due,
                                style = AppTypography.captionSmall.copy(
                                    color = if (task.priority.equals("Danger", ignoreCase = true)
                                        || task.priority.equals("High", ignoreCase = true))
                                        AppTheme.Danger else AppTheme.Ink300,
                                    fontWeight = FontWeight.Medium,
                                ),
                            )
                        }
                    }
                }

                // Progress bar (InProgress tasks only)
                if (task.status == "InProgress") {
                    val frac = progressFraction(task)
                    Spacer(Modifier.height(AppTheme.SpMd))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        DsProgressBar(
                            targetFraction = frac,
                            tint           = accent,
                            modifier       = Modifier.weight(1f),
                        )
                        Spacer(Modifier.width(AppTheme.SpSm))
                        Text(
                            "${(frac * 100).toInt()}%",
                            style = AppTypography.labelSmall.copy(
                                color      = accent,
                                fontWeight = FontWeight.SemiBold,
                            ),
                        )
                    }
                }

                Spacer(Modifier.height(AppTheme.SpMd))

                // Footer: Assignee + status badge
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier          = Modifier.fillMaxWidth(),
                ) {
                    if (avatarInitialsList.size <= 1) {
                        DsAvatarBubble(
                            initials = avatarInitialsList.firstOrNull() ?: "?",
                            size     = AppTheme.AvatarMd,
                        )
                    } else {
                        DsAvatarStack(
                            initialsList = avatarInitialsList,
                            size         = AppTheme.AvatarMd,
                            maxVisible   = 3,
                        )
                    }
                    Spacer(Modifier.width(AppTheme.SpSm))
                    Text(
                        displayName,
                        style    = AppTypography.captionLarge.copy(
                            color      = AppTheme.Ink700,
                            fontWeight = FontWeight.SemiBold,
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    DsStatusChip(
                        label      = statusLabel(task.status),
                        tint       = statusTint(task.status),
                        background = statusBg(task.status),
                    )
                }
            }
        }
    }
}

/* ─── Contextual action menu ─────────────────────────────────────────────── */

@Composable
private fun TaskActionMenu(
    expanded      : Boolean,
    taskStatus    : String,
    onDismiss     : () -> Unit,
    onViewDetails : () -> Unit,
    onEditTask    : () -> Unit,
    onStatusChange: (String) -> Unit,
) {
    DropdownMenu(
        expanded         = expanded,
        onDismissRequest = onDismiss,
        containerColor   = AppTheme.Surface,
    ) {
        when (taskStatus) {
            "Todo" -> {
                ActionItem("▶  Start Task")       { onStatusChange("InProgress") }
            }
            "InProgress" -> {
                ActionItem("⏸  Pause Task")       { onStatusChange("Todo") }
                ActionItem("🔍  Send for Review") { onStatusChange("InReview") }
                ActionItem("✅  Mark Complete")   { onStatusChange("Done") }
            }
            "InReview" -> {
                ActionItem("✅  Approve & Complete") { onStatusChange("Done") }
                ActionItem("↩  Return to Progress")  { onStatusChange("InProgress") }
            }
            "Done" -> {
                ActionItem("↩  Reopen Task") { onStatusChange("InProgress") }
            }
        }
        HorizontalDivider(
            color    = AppTheme.Ink100,
            modifier = Modifier.padding(vertical = AppTheme.SpXs),
        )
        ActionItem("✏️  Edit Task",     onClick = onEditTask)
        ActionItem("👁  View Details",  onClick = onViewDetails)
    }
}

@Composable
private fun ActionItem(
    label  : String,
    color  : Color = AppTheme.Ink700,
    onClick: () -> Unit,
) {
    DropdownMenuItem(
        text    = {
            Text(label, style = AppTypography.bodySmall.copy(color = color))
        },
        onClick = onClick,
    )
}

/* ─── Pure helpers ───────────────────────────────────────────────────────── */

private fun priorityAccent(raw: String): Color = when (raw.lowercase()) {
    "danger", "high" -> AppTheme.Danger
    "medium", "med"  -> AppTheme.Warning
    "success", "low" -> AppTheme.Ink300
    else             -> AppTheme.Brand
}

private fun priorityLabel(raw: String): String = when (raw.lowercase()) {
    "danger", "high"  -> "HIGH"
    "medium", "med"   -> "MED"
    "success", "low"  -> "LOW"
    else              -> raw.uppercase()
}

private fun statusLabel(status: String): String = when (status) {
    "Todo"       -> "PENDING"
    "InProgress" -> "IN PROGRESS"
    "InReview"   -> "REVIEW"
    "Done"       -> "DONE"
    else         -> status.uppercase()
}

private fun statusTint(status: String): Color = when (status) {
    "Todo"       -> AppTheme.Ink500
    "InProgress" -> AppTheme.Brand
    "InReview"   -> AppTheme.Warning
    "Done"       -> AppTheme.Success
    else         -> AppTheme.Ink500
}

private fun statusBg(status: String): Color = when (status) {
    "Todo"       -> AppTheme.Ink50
    "InProgress" -> AppTheme.Brand50
    "InReview"   -> AppTheme.WarningBg
    "Done"       -> AppTheme.SuccessBg
    else         -> AppTheme.Ink50
}

private fun taskCode(task: TaskRecord): String {
    val n = (task.id.hashCode().absoluteValue % 9000) + 1000
    return "#TASK-$n"
}

private fun formatDue(task: TaskRecord): String {
    val day  = task.day.trim()
    val time = task.time.trim()
    return when {
        day.equals("Today",    ignoreCase = true) -> "Today${if (time.isNotBlank()) " · $time" else ""}"
        day.equals("Tomorrow", ignoreCase = true) -> "Tomorrow${if (time.isNotBlank()) " · $time" else ""}"
        day.isNotBlank()                          -> "$day${if (time.isNotBlank()) " · $time" else ""}"
        else                                      -> time
    }
}

private fun progressFraction(task: TaskRecord): Float =
    ((task.id.hashCode().absoluteValue % 70) + 30) / 100f

private fun deriveAvatarLetters(task: TaskRecord): String {
    val stored = task.assigneeInitials.trim()
    if (stored.isNotBlank()) return stored.take(2).uppercase()
    val parts = task.assigneeName.trim().split(' ', '\t').filter { it.isNotBlank() }
    return when {
        parts.size >= 2 -> "${parts[0].first()}${parts[1].first()}".uppercase()
        parts.size == 1 -> parts[0].first().uppercase().toString()
        else            -> "?"
    }
}

/**
 * Per-assignee initials list — one 2-letter token per assignee, in order.
 * Used to drive [DsAvatarStack] on multi-assignee tasks. Falls back to the
 * single legacy [deriveAvatarLetters] when the task has no array of names.
 */
private fun deriveAvatarLettersList(task: TaskRecord): List<String> {
    val names = task.allAssigneeNames()
    if (names.isEmpty()) return listOf(deriveAvatarLetters(task))
    return names.map { full ->
        val parts = full.trim().split(' ', '\t').filter { it.isNotBlank() }
        when {
            parts.size >= 2 -> "${parts[0].first()}${parts[1].first()}".uppercase()
            parts.size == 1 -> parts[0].first().uppercase().toString()
            else            -> "?"
        }
    }
}

