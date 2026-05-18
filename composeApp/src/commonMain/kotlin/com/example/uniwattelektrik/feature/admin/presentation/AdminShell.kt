package com.example.uniwattelektrik.feature.admin.presentation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Assignment
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import kotlinx.coroutines.launch
import com.example.uniwattelektrik.core.components.AppBottomNavBar
import com.example.uniwattelektrik.core.components.BottomNavItem
import com.example.uniwattelektrik.core.components.EmptyState
import com.example.uniwattelektrik.core.components.ScreenSkeletonOverlay
import com.example.uniwattelektrik.core.components.SkeletonType
import com.example.uniwattelektrik.core.components.ToastHost
import com.example.uniwattelektrik.core.navigation.AdminNavigator
import com.example.uniwattelektrik.core.navigation.AdminRoute
import com.example.uniwattelektrik.core.navigation.rememberAdminNavigator
import com.example.uniwattelektrik.di.AppContainer
import com.example.uniwattelektrik.feature.admin.presentation.screens.AdminAttendanceScreen
import com.example.uniwattelektrik.feature.admin.presentation.screens.AlertsScreen
import com.example.uniwattelektrik.feature.admin.presentation.screens.LinkManagerScreen
import com.example.uniwattelektrik.feature.admin.presentation.screens.AdminEmployeeDetailScreen
import com.example.uniwattelektrik.feature.admin.presentation.screens.AdminEmployeesScreen
import com.example.uniwattelektrik.feature.admin.presentation.screens.AdminHomeScreen
import com.example.uniwattelektrik.feature.admin.presentation.screens.AdminInventoryScreen
import com.example.uniwattelektrik.feature.admin.presentation.screens.AdminLeaveApprovalsScreen
import com.example.uniwattelektrik.feature.admin.presentation.screens.AdminTasksScreen
import com.example.uniwattelektrik.feature.admin.presentation.screens.NewTaskScreen
import com.example.uniwattelektrik.feature.auth.domain.model.User
import com.example.uniwattelektrik.feature.auth.presentation.state.AuthUiEvent
import com.example.uniwattelektrik.feature.auth.presentation.viewmodel.AuthViewModel
import com.example.uniwattelektrik.feature.user.presentation.screens.NotificationsScreen
import com.example.uniwattelektrik.feature.user.presentation.screens.ProfileScreen
import com.example.uniwattelektrik.feature.user.presentation.screens.TaskDetailScreen
import com.example.uniwattelektrik.feature.workforce.presentation.WorkforceViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.uniwattelektrik.platform.PlatformBackHandler

/**
 * Top-level container for the Admin app — same shape as [com.example.uniwattelektrik.feature.user.presentation.UserShell]
 * but with admin-specific tabs (Dashboard / Employees / Tasks / Inventory) and a
 * different FAB action (broadcast / quick assign).
 */
@Composable
fun AdminShell(
    user: User,
    viewModel: AuthViewModel,
) {
    val nav: AdminNavigator = rememberAdminNavigator()
    val workforceVm: WorkforceViewModel = remember { AppContainer.createWorkforceViewModel() }
    val inventoryVm: InventoryViewModel = remember { AppContainer.createInventoryViewModel() }
    val showAddEmployee = remember { mutableStateOf(false) }
    // Transient — set when an admin home KPI ("Pending" / "Completed") is
    // tapped, consumed by AdminTasksScreen on its first composition so the
    // pager opens on the matching workflow tab.
    var tasksInitialTab by remember { mutableStateOf<String?>(null) }
    // Holds per-route `rememberSaveable` state (scroll positions, search
    // queries, etc.) across stack push/pop so users return to the same
    // visual position they left.
    val saveableHolder = rememberSaveableStateHolder()
    LaunchedEffect(user.id) { workforceVm.loadForAdmin(user.id) }
    // Start observing inventory live (admin adds data manually — no seeder).
    LaunchedEffect(user.id) {
        inventoryVm.loadForAdmin(user.id)
    }

    // ── FCM deep-link consumer ───────────────────────────────────────────────
    // When a notification tap on Android publishes a route key into
    // [DeepLinkBus], jump to the matching tab. Unknown keys are ignored
    // (e.g. "leave" — handled by UserShell instead).
    val deepLink by com.example.uniwattelektrik.core.notification.DeepLinkBus.route
        .collectAsStateWithLifecycle()
    val deepLinkTaskId by com.example.uniwattelektrik.core.notification.DeepLinkBus.taskId
        .collectAsStateWithLifecycle()
    LaunchedEffect(deepLink, deepLinkTaskId) {
        when (deepLink) {
            "home"       -> nav.selectTab(AdminRoute.Dashboard)
            "tasks"      -> {
                nav.selectTab(AdminRoute.Tasks)
                // If the notification carried a specific task id (e.g. a note
                // was added), pop the matching detail screen on top.
                deepLinkTaskId?.let { nav.navigate(AdminRoute.TaskDetail(it)) }
            }
            "employees"  -> nav.selectTab(AdminRoute.Employees)
            "attendance" -> nav.selectTab(AdminRoute.Attendance)
            "inventory"  -> nav.selectTab(AdminRoute.Inventory)
            "more"       -> nav.selectTab(AdminRoute.Profile)
            else         -> return@LaunchedEffect
        }
        com.example.uniwattelektrik.core.notification.DeepLinkBus.consume()
    }
    // Re-subscribe + show shimmer whenever the app returns from background.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        workforceVm.refresh()
    }
    val workforceLoading by workforceVm.loading.collectAsStateWithLifecycle()

    // ── Free, in-app notifications ───────────────────────────────────────────
    // No Cloud Functions, no Blaze plan. Each device runs Firestore listeners
    // and posts a local NotificationCompat on relevant changes (new leave,
    // task completed, low stock, late check-in). Started after sign-in,
    // cancelled when the shell leaves composition or the uid changes.
    val notifScope = rememberCoroutineScope()
    DisposableEffect(user.id) {
        AppContainer.adminNotificationsCoordinator.start(notifScope, user.id)
        onDispose { AppContainer.adminNotificationsCoordinator.stop() }
    }

    // ─── Excel import wiring ─────────────────────────────────────────────────
    val excelParser = remember { com.example.uniwattelektrik.core.platform.createExcelParser() }
    val coroutineScope = rememberCoroutineScope()
    val snackbarHost = remember { SnackbarHostState() }
    val launchExcelPicker = com.example.uniwattelektrik.core.platform.rememberExcelFilePicker { uri ->
        coroutineScope.launch {
            try {
                inventoryVm.stagedSheets = excelParser.parse(uri)
                nav.navigate(AdminRoute.ImportSpareItemsPreview)
            } catch (e: Exception) {
                snackbarHost.showSnackbar("Failed to read Excel: ${e.message ?: "unknown error"}")
            }
        }
    }

    // Surface bulk-delete results as a snackbar.
    val deleteResult by inventoryVm.deleteResult.collectAsStateWithLifecycle()
    androidx.compose.runtime.LaunchedEffect(deleteResult) {
        deleteResult?.let { n ->
            inventoryVm.consumeDeleteResult()
            snackbarHost.showSnackbar(
                if (n == 1) "1 spare deleted" else "$n spares deleted"
            )
        }
    }

    // Bottom nav is intentionally trimmed to 4 tabs to keep the bar uncluttered.
    // "Team" (employees) and "Stock" (inventory) live as dedicated rows inside
    // the Settings/More screen and are reachable from the Operations cards too.
    val tabs = remember {
        listOf(
            BottomNavItem(key = "home",       label = "Home",   icon = Icons.Outlined.Home),
            BottomNavItem(key = "tasks",      label = "Tasks",  icon = Icons.AutoMirrored.Outlined.Assignment),
            BottomNavItem(key = "attendance", label = "Attend", icon = Icons.Outlined.LocationOn),
            BottomNavItem(key = "more",       label = "More",   icon = Icons.Outlined.Settings),
        )
    }

    val current     = nav.current
    val isTopLevel  = current is AdminRoute.Tab
    val selectedKey = when (current) {
        AdminRoute.Dashboard  -> "home"
        AdminRoute.Employees  -> "employees"
        AdminRoute.Tasks      -> "tasks"
        AdminRoute.Attendance -> "attendance"
        AdminRoute.Inventory  -> "inventory"
        AdminRoute.Profile    -> "more"
        else                  -> "home"
    }

    Box(
        modifier = Modifier.fillMaxSize(),
    ) {
        // ── System back navigation ─────────────────────────────────────────
        // Priority: 1) Add-Employee sheet owns its own BackHandler (skip);
        // 2) Import preview special-cases stagedSheet cleanup; 3) pop stack;
        // 4) on a non-Dashboard tab → switch to Dashboard; 5) on Dashboard →
        // disable so the OS finishes the Activity.
        PlatformBackHandler(enabled = !showAddEmployee.value && current != AdminRoute.Dashboard) {
            when (current) {
                AdminRoute.ImportSpareItemsPreview -> {
                    inventoryVm.stagedSheets = null
                    nav.pop()
                }
                else -> {
                    if (!nav.pop()) nav.selectTab(AdminRoute.Dashboard)
                }
            }
        }

        AnimatedContent(
            targetState = current,
            transitionSpec = {
                if (targetState is AdminRoute.Tab && initialState is AdminRoute.Tab) {
                    // Tab switching: simple fade or slight scale
                    (fadeIn(animationSpec = tween(300)) + scaleIn(initialScale = 0.98f))
                        .togetherWith(fadeOut(animationSpec = tween(200)) + scaleOut(targetScale = 0.98f))
                } else {
                    // Forward navigation (stack push)
                    if (nav.isForward(initialState, targetState)) {
                        (slideInHorizontally(animationSpec = tween(350)) { it } + fadeIn(tween(350)))
                            .togetherWith(slideOutHorizontally(animationSpec = tween(350)) { -it / 3 } + fadeOut(tween(350)))
                    } else {
                        // Backward navigation (pop)
                        (slideInHorizontally(animationSpec = tween(350)) { -it / 3 } + fadeIn(tween(350)))
                            .togetherWith(slideOutHorizontally(animationSpec = tween(350)) { it } + fadeOut(tween(350)))
                    }
                }
            },
            label = "ScreenTransition"
        ) { r ->
            // Wrap each route in a SaveableStateProvider keyed by a stable
            // string per route variant. This survives push→pop cycles so any
            // `rememberSaveable` (LazyListState scroll position, search query,
            // tab index, …) inside list screens is preserved when the user
            // returns from a detail screen. ⇒ "back from detail lands me at
            // the same row I tapped".
            saveableHolder.SaveableStateProvider(routeKey(r)) {
            when (r) {
                AdminRoute.Dashboard -> AdminHomeScreen(
                    user                = user,
                    workforceVm         = workforceVm,
                    inventoryVm         = inventoryVm,
                    onOpenNotifications = { nav.navigate(AdminRoute.Notifications) },
                    onTaskClick         = { id -> nav.navigate(AdminRoute.TaskDetail(id)) },
                    onEmployeeClick     = { id -> nav.navigate(AdminRoute.EmployeeDetail(id)) },
                    onAttendanceClick   = { nav.selectTab(AdminRoute.Attendance) },
                    onSpareClick        = { item -> nav.navigate(AdminRoute.SpareItemDetail(item)) },
                    onLeaveRequestsClick = { nav.navigate(AdminRoute.LeaveApprovals()) },
                    onViewAllTasks      = { nav.selectTab(AdminRoute.Tasks) },
                    onViewAllEmployees  = { nav.selectTab(AdminRoute.Employees) },
                    onViewInventory     = { nav.selectTab(AdminRoute.Inventory) },
                    onPendingTasksClick = {
                        tasksInitialTab = "Todo"
                        nav.selectTab(AdminRoute.Tasks)
                    },
                    onCompletedTasksClick = {
                        tasksInitialTab = "Done"
                        nav.selectTab(AdminRoute.Tasks)
                    },
                    onAlertsClick = { nav.navigate(AdminRoute.Alerts) },
                )
                AdminRoute.Employees -> AdminEmployeesScreen(
                    adminUid        = user.id,
                    workforceVm     = workforceVm,
                    onEmployeeClick = { id -> nav.navigate(AdminRoute.EmployeeDetail(id)) },
                    onShowAddChange = { show -> showAddEmployee.value = show },
                )
                AdminRoute.Tasks     -> AdminTasksScreen(
                    workforceVm  = workforceVm,
                    adminUid     = user.id,
                    onAssign     = { nav.navigate(AdminRoute.NewTask()) },
                    onBack       = { nav.selectTab(AdminRoute.Dashboard) },
                    onTaskClick  = { id -> nav.navigate(AdminRoute.TaskDetail(id)) },
                    onEditTask   = { id -> nav.navigate(AdminRoute.NewTask(editTaskId = id)) },
                    initialTabKey         = tasksInitialTab,
                    onInitialTabConsumed  = { tasksInitialTab = null },
                )
                is AdminRoute.TaskDetail -> TaskDetailScreen(
                    taskId      = r.taskId,
                    onBack      = { nav.pop() },
                    onStartWork = { nav.pop() },
                    workforceVm = workforceVm,
                    viewerRole      = com.example.uniwattelektrik.feature.user.presentation.screens.TaskDetailRole.Admin,
                    currentUserId   = user.id,
                    currentUserName = user.displayName?.takeIf { it.isNotBlank() } ?: user.email,
                    adminUid        = user.id,
                    // Admin-only edit action — open NewTaskScreen in edit mode.
                    onEdit          = { id -> nav.navigate(AdminRoute.NewTask(editTaskId = id)) },
                )
                is AdminRoute.NewTask   -> NewTaskScreen(
                    workforceVm      = workforceVm,
                    inventoryVm      = inventoryVm,
                    adminUid         = user.id,
                    onBack           = { nav.pop() },
                    onCreated        = { nav.pop() },
                    adminDisplayName = user.displayName?.takeIf { it.isNotBlank() } ?: user.email,
                    editTaskId       = r.editTaskId,
                )
                AdminRoute.Attendance -> AdminAttendanceScreen(
                    workforceVm     = workforceVm,
                    onBack          = { nav.selectTab(AdminRoute.Dashboard) },
                    onEmployeeClick = { id -> nav.navigate(AdminRoute.EmployeeAttendanceDetail(id)) },
                    adminUid        = user.id,
                )
                AdminRoute.Inventory -> AdminInventoryScreen(
                    inventoryVm   = inventoryVm,
                    adminId       = user.id,
                    onManage      = { nav.navigate(AdminRoute.InventoryManagement) },
                    onAddSpare    = { nav.navigate(AdminRoute.SpareItemForm(null)) },
                    onImportExcel = launchExcelPicker,
                    onItemClick   = { item -> nav.navigate(AdminRoute.SpareItemDetail(item)) },
                )
                AdminRoute.ImportSpareItemsPreview ->
                    com.example.uniwattelektrik.feature.admin.presentation.screens.inventory.ImportSpareItemsPreviewScreen(
                        adminId     = user.id,
                        inventoryVm = inventoryVm,
                        onBack      = {
                            inventoryVm.stagedSheets = null
                            nav.pop()
                        },
                        onSuccess   = { n ->
                            inventoryVm.stagedSheets = null
                            nav.pop()
                            coroutineScope.launch { snackbarHost.showSnackbar("Imported $n spares") }
                        },
                    )
                AdminRoute.InventoryManagement -> com.example.uniwattelektrik.feature.admin.presentation.screens.inventory.InventoryManagementScreen(
                    onBack        = { nav.pop() },
                    onDepartments = { nav.navigate(AdminRoute.Departments) },
                    onEquipment   = { nav.navigate(AdminRoute.Equipment) },
                    onPriceList   = { nav.navigate(AdminRoute.SpareList) },
                )
                AdminRoute.Departments -> com.example.uniwattelektrik.feature.admin.presentation.screens.inventory.DepartmentScreen(
                    onBack      = { nav.pop() },
                    inventoryVm = inventoryVm,
                    adminId     = user.id,
                )
                AdminRoute.Equipment -> com.example.uniwattelektrik.feature.admin.presentation.screens.inventory.EquipmentScreen(
                    onBack      = { nav.pop() },
                    inventoryVm = inventoryVm,
                    adminId     = user.id,
                )
                AdminRoute.SpareList -> com.example.uniwattelektrik.feature.admin.presentation.screens.inventory.SpareListScreen(
                    adminId      = user.id,
                    inventoryVm  = inventoryVm,
                    onBack       = { nav.pop() },
                    onAdd        = { nav.navigate(AdminRoute.SpareItemForm(null)) },
                    onEdit       = { item -> nav.navigate(AdminRoute.SpareItemForm(item)) },
                    onItemClick  = { item -> nav.navigate(AdminRoute.SpareItemDetail(item)) },
                )
                is AdminRoute.SpareItemDetail -> {
                    // Pull the latest version of the spare from the live VM
                    // state so edits made via the form are reflected when the
                    // user returns to the Detail screen.
                    val liveSpares by inventoryVm.spareItems.collectAsStateWithLifecycle()
                    val freshItem = liveSpares.firstOrNull { it.id == r.item.id } ?: r.item
                    com.example.uniwattelektrik.feature.admin.presentation.screens.inventory.SpareItemDetailScreen(
                        item    = freshItem,
                        onBack  = { nav.pop() },
                        onEdit  = {
                            // Push Form on top of Detail so Save/Back returns
                            // to the Detail (Spare view) screen — not the list.
                            nav.navigate(AdminRoute.SpareItemForm(freshItem))
                        },
                        onDelete = {
                            inventoryVm.deleteSpareItem(user.id, freshItem.id) { nav.pop() }
                        },
                    )
                }
                is AdminRoute.SpareItemForm -> com.example.uniwattelektrik.feature.admin.presentation.screens.inventory.SpareItemFormScreen(
                    initial     = r.item,
                    onBack      = { nav.pop() },
                    inventoryVm = inventoryVm,
                    onSave      = { item ->
                        inventoryVm.saveSpareItem(user.id, item) { nav.pop() }
                    }
                )
                AdminRoute.Profile   -> ProfileScreen(
                    user         = user,
                    initials     = user.adminInitials(),
                    name         = user.displayName?.takeIf { it.isNotBlank() } ?: user.email,
                    role         = "Operations Admin · ${user.adminId ?: "—"}",
                    onLogout     = { viewModel.onEvent(AuthUiEvent.Logout) },
                    workforceVm  = workforceVm,
                    onNavigate   = { route -> nav.navigate(route) },
                    // Recent-activity rows route into AdminTasksScreen with the
                    // matching workflow tab focused. AdminTasksScreen uses the
                    // tab key "Review" (not "InReview"); other statuses pass
                    // through as-is. Null hint = leave tab default.
                    onOpenTaskList = { statusHint ->
                        tasksInitialTab = when (statusHint) {
                            "InReview" -> "Review"
                            null       -> null
                            else       -> statusHint
                        }
                        nav.navigate(AdminRoute.Tasks)
                    },
                )
                is AdminRoute.EmployeeDetail -> AdminEmployeeDetailScreen(
                    employeeId  = r.employeeId,
                    workforceVm = workforceVm,
                    onBack      = { nav.pop() },
                    adminUid    = user.id,
                    onEdit      = { id -> nav.navigate(AdminRoute.EditEmployee(id)) },
                )
                is AdminRoute.EmployeeAttendanceDetail ->
                    com.example.uniwattelektrik.feature.admin.presentation.screens.EmployeeAttendanceDetailScreen(
                        userId      = r.userId,
                        workforceVm = workforceVm,
                        onBack      = { nav.pop() },
                    )
                is AdminRoute.EditEmployee -> {
                    val employees by workforceVm.employees.collectAsStateWithLifecycle()
                    val initial = employees.firstOrNull { it.id == r.employeeId }
                    val workforceLoading2 by workforceVm.loading.collectAsStateWithLifecycle()
                    val workforceError2   by workforceVm.error.collectAsStateWithLifecycle()
                    if (initial == null) {
                        EmptyState(emoji = "👤", title = "Employee not found", body = "Go back and try again.")
                    } else {
                        com.example.uniwattelektrik.feature.admin.presentation.screens.AddEmployeeSheet(
                            saving          = workforceLoading2,
                            errorMsg        = workforceError2,
                            createdName     = null,
                            createdEmail    = null,
                            createdPassword = null,
                            createdPhone    = null,
                            initial         = initial,
                            onDoneSharing   = { nav.pop() },
                            onCancel        = {
                                workforceVm.clearError()
                                nav.pop()
                            },
                            onSubmit        = { draft ->
                                workforceVm.clearError()
                                workforceVm.updateEmployee(user.id, r.employeeId, draft) { result ->
                                    if (result.isSuccess) nav.pop()
                                }
                            },
                        )
                    }
                }
                AdminRoute.Alerts -> AlertsScreen(
                    workforceVm      = workforceVm,
                    inventoryVm      = inventoryVm,
                    onBack           = { nav.pop() },
                    onTaskClick      = { id -> nav.navigate(AdminRoute.TaskDetail(id)) },
                    onLeaveClick     = { nav.navigate(AdminRoute.LeaveApprovals("Pending")) },
                    onAttendanceClick = { nav.selectTab(AdminRoute.Attendance) },
                    onSpareClick     = { item -> nav.navigate(AdminRoute.SpareItemDetail(item)) },
                )
                AdminRoute.Reports ->
                    com.example.uniwattelektrik.feature.admin.presentation.reports.ReportsScreen(
                        workforceVm     = workforceVm,
                        inventoryVm     = inventoryVm,
                        adminId         = user.id,
                        onBack          = { nav.pop() },
                        onEmployeeOpen  = { id -> nav.navigate(AdminRoute.EmployeeAttendanceDetail(id)) },
                        onTaskBoardOpen = { nav.selectTab(AdminRoute.Tasks) },
                        onSpareListOpen = { nav.navigate(AdminRoute.SpareList) },
                    )
                is AdminRoute.LeaveApprovals -> AdminLeaveApprovalsScreen(
                    workforceVm         = workforceVm,
                    adminUid            = user.id,
                    onBack              = { nav.pop() },
                    initialStatusFilter = r.initialStatusFilter,
                )
                AdminRoute.LinkManager -> LinkManagerScreen(
                    adminId     = user.id,
                    workforceVm = workforceVm,
                    onBack      = { nav.pop() },
                )
                AdminRoute.Notifications -> NotificationsScreen(onBack = { nav.pop() })
                AdminRoute.Financials -> EmptyState(
                    emoji = "🚧", title = "Coming next",
                    body  = "Financial reports are scaffolded — design system + nav are wired.",
                )
            }
            } // SaveableStateProvider
        }

        if (isTopLevel && !showAddEmployee.value) {
            AppBottomNavBar(
                items       = tabs,
                selectedKey = selectedKey,
                onSelected  = { key ->
                    when (key) {
                        "home"       -> nav.selectTab(AdminRoute.Dashboard)
                        "employees"  -> nav.selectTab(AdminRoute.Employees)
                        "tasks"      -> nav.selectTab(AdminRoute.Tasks)
                        "attendance" -> nav.selectTab(AdminRoute.Attendance)
                        "inventory"  -> nav.selectTab(AdminRoute.Inventory)
                        "more"       -> nav.selectTab(AdminRoute.Profile)
                    }
                },
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }

        // List/detail screens render their own inline skeletons BELOW their
        // gradient header. Only show the dim full-screen overlay for routes
        // that don't have a header of their own (forms / coming-soon stubs).
        val showFullScreenOverlay = when (current) {
            is AdminRoute.NewTask,
            is AdminRoute.SpareItemForm,
            AdminRoute.Financials,
            AdminRoute.LinkManager,
            AdminRoute.ImportSpareItemsPreview -> true
            else -> false
        }
        if (workforceLoading && showFullScreenOverlay) {
            val skeletonType = when (current) {
                is AdminRoute.NewTask,
                is AdminRoute.SpareItemForm -> SkeletonType.Form
                else                        -> SkeletonType.List
            }
            ScreenSkeletonOverlay(type = skeletonType, message = "Loading workspace...")
        }

        SnackbarHost(
            hostState = snackbarHost,
            modifier = Modifier.align(Alignment.BottomCenter),
        )

        // ── Global toast host (top of screen) ──────────────────────────────
        ToastHost(modifier = Modifier.align(Alignment.TopCenter))
    }
}

private fun User.adminInitials(): String {
    val raw = displayName?.takeIf { it.isNotBlank() } ?: email.substringBefore("@")
    val parts = raw.split(" ", "_", ".", "-").filter { it.isNotBlank() }
    return parts.take(2).mapNotNull { it.firstOrNull()?.uppercaseChar() }
        .joinToString("")
        .ifEmpty { raw.take(2).uppercase() }
}

/**
 * Stable string key per [AdminRoute] variant — used by the parent
 * [androidx.compose.runtime.saveable.SaveableStateHolder] to bucket each
 * route's `rememberSaveable` slots. Detail routes include the entity id so
 * different items don't share state.
 */
private fun routeKey(r: AdminRoute): String = when (r) {
    AdminRoute.Dashboard               -> "dashboard"
    AdminRoute.Employees               -> "employees"
    AdminRoute.Tasks                   -> "tasks"
    AdminRoute.Attendance              -> "attendance"
    AdminRoute.Inventory               -> "inventory"
    AdminRoute.Profile                 -> "profile"
    AdminRoute.Notifications           -> "notifications"
    AdminRoute.Financials              -> "financials"
    AdminRoute.Alerts                  -> "alerts"
    AdminRoute.Reports                 -> "reports"
    AdminRoute.InventoryManagement     -> "inv_mgmt"
    AdminRoute.Departments             -> "departments"
    AdminRoute.Equipment               -> "equipment"
    AdminRoute.SpareList               -> "spare_list"
    AdminRoute.ImportSpareItemsPreview -> "import_preview"
    AdminRoute.LinkManager             -> "link_manager"
    is AdminRoute.EmployeeDetail       -> "employee_detail:${r.employeeId}"
    is AdminRoute.EmployeeAttendanceDetail -> "emp_attendance:${r.userId}"
    is AdminRoute.EditEmployee         -> "edit_employee:${r.employeeId}"
    is AdminRoute.TaskDetail           -> "task_detail:${r.taskId}"
    is AdminRoute.NewTask              -> "new_task:${r.editTaskId.orEmpty()}"
    is AdminRoute.SpareItemDetail      -> "spare_detail:${r.item.id}"
    is AdminRoute.SpareItemForm        -> "spare_form:${r.item?.id.orEmpty()}"
    is AdminRoute.LeaveApprovals       -> "leave_approvals:${r.initialStatusFilter}"
}

