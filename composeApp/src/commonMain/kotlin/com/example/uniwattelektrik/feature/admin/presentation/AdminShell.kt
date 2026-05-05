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
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import kotlinx.coroutines.launch
import com.example.uniwattelektrik.core.components.AppBottomNavBar
import com.example.uniwattelektrik.core.components.BottomNavItem
import com.example.uniwattelektrik.core.components.EmptyState
import com.example.uniwattelektrik.core.components.ScreenSkeletonOverlay
import com.example.uniwattelektrik.core.components.SkeletonType
import com.example.uniwattelektrik.core.navigation.AdminNavigator
import com.example.uniwattelektrik.core.navigation.AdminRoute
import com.example.uniwattelektrik.core.navigation.rememberAdminNavigator
import com.example.uniwattelektrik.di.AppContainer
import com.example.uniwattelektrik.feature.admin.presentation.screens.AdminAttendanceScreen
import com.example.uniwattelektrik.feature.admin.presentation.screens.AdminEmployeeDetailScreen
import com.example.uniwattelektrik.feature.admin.presentation.screens.AdminEmployeesScreen
import com.example.uniwattelektrik.feature.admin.presentation.screens.AdminHomeScreen
import com.example.uniwattelektrik.feature.admin.presentation.screens.AdminInventoryScreen
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
    LaunchedEffect(user.id) { workforceVm.loadForAdmin(user.id) }
    // Start observing inventory live (admin adds data manually — no seeder).
    LaunchedEffect(user.id) {
        inventoryVm.loadForAdmin(user.id)
    }
    val workforceLoading by workforceVm.loading.collectAsStateWithLifecycle()

    // ─── Excel import wiring ─────────────────────────────────────────────────
    val excelParser = remember { com.example.uniwattelektrik.core.platform.createExcelParser() }
    val coroutineScope = rememberCoroutineScope()
    val snackbarHost = remember { SnackbarHostState() }
    val launchExcelPicker = com.example.uniwattelektrik.core.platform.rememberExcelFilePicker { uri ->
        coroutineScope.launch {
            try {
                inventoryVm.stagedSheet = excelParser.parse(uri)
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

    val tabs = remember {
        listOf(
            BottomNavItem(key = "home",       label = "Home",   icon = Icons.Outlined.Home),
            BottomNavItem(key = "employees",  label = "Team",   icon = Icons.Outlined.People),
            BottomNavItem(key = "tasks",      label = "Tasks",  icon = Icons.AutoMirrored.Outlined.Assignment),
            BottomNavItem(key = "attendance", label = "Attend", icon = Icons.Outlined.LocationOn),
            BottomNavItem(key = "inventory",  label = "Stock",  icon = Icons.Outlined.Inventory2),
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
                    onAssign     = { nav.navigate(AdminRoute.NewTask) },
                    onBack       = { nav.selectTab(AdminRoute.Dashboard) },
                    onTaskClick  = { id -> nav.navigate(AdminRoute.TaskDetail(id)) },
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
                )
                AdminRoute.NewTask   -> NewTaskScreen(
                    workforceVm      = workforceVm,
                    inventoryVm      = inventoryVm,
                    adminUid         = user.id,
                    onBack           = { nav.pop() },
                    onCreated        = { nav.pop() },
                    adminDisplayName = user.displayName?.takeIf { it.isNotBlank() } ?: user.email,
                )
                AdminRoute.Attendance -> AdminAttendanceScreen(
                    workforceVm = workforceVm,
                    onBack      = { nav.selectTab(AdminRoute.Dashboard) },
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
                            inventoryVm.stagedSheet = null
                            nav.pop()
                        },
                        onSuccess   = { n ->
                            inventoryVm.stagedSheet = null
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
                is AdminRoute.SpareItemDetail -> com.example.uniwattelektrik.feature.admin.presentation.screens.inventory.SpareItemDetailScreen(
                    item    = r.item,
                    onBack  = { nav.pop() },
                    onEdit  = {
                        nav.pop()
                        nav.navigate(AdminRoute.SpareItemForm(r.item))
                    },
                    onDelete = {
                        inventoryVm.deleteSpareItem(user.id, r.item.id)
                        nav.pop()
                    },
                )
                is AdminRoute.SpareItemForm -> com.example.uniwattelektrik.feature.admin.presentation.screens.inventory.SpareItemFormScreen(
                    initial = r.item,
                    onBack  = { nav.pop() },
                    onSave  = { item ->
                        inventoryVm.saveSpareItem(user.id, item)
                        nav.pop()
                    }
                )
                AdminRoute.Profile   -> ProfileScreen(
                    user         = user,
                    initials     = user.adminInitials(),
                    name         = user.displayName?.takeIf { it.isNotBlank() } ?: user.email,
                    role         = "Operations Admin · ${user.adminId ?: "—"}",
                    onLogout     = { viewModel.onEvent(AuthUiEvent.Logout) },
                    workforceVm  = workforceVm,
                    onNavigate   = { route -> nav.navigate(route) }
                )
                is AdminRoute.EmployeeDetail -> AdminEmployeeDetailScreen(
                    employeeId  = r.employeeId,
                    workforceVm = workforceVm,
                    onBack      = { nav.pop() },
                )
                AdminRoute.Notifications -> NotificationsScreen(onBack = { nav.pop() })
                AdminRoute.Financials -> EmptyState(
                    emoji = "🚧", title = "Coming next",
                    body  = "Financial reports are scaffolded — design system + nav are wired.",
                )
            }
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

        if (workforceLoading) {
            val skeletonType = when (current) {
                AdminRoute.Dashboard -> SkeletonType.Dashboard
                AdminRoute.NewTask,
                is AdminRoute.SpareItemForm -> SkeletonType.Form
                else -> SkeletonType.List
            }
            ScreenSkeletonOverlay(type = skeletonType, message = "Loading workspace...")
        }

        SnackbarHost(
            hostState = snackbarHost,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

private fun User.adminInitials(): String {
    val raw = displayName?.takeIf { it.isNotBlank() } ?: email.substringBefore("@")
    val parts = raw.split(" ", "_", ".", "-").filter { it.isNotBlank() }
    return parts.take(2).mapNotNull { it.firstOrNull()?.uppercaseChar() }
        .joinToString("")
        .ifEmpty { raw.take(2).uppercase() }
}
