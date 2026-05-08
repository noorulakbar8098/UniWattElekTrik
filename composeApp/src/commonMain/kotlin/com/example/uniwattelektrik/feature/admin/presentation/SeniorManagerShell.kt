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
import androidx.compose.material.icons.outlined.Inventory2
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.uniwattelektrik.core.components.AppBottomNavBar
import com.example.uniwattelektrik.core.components.BottomNavItem
import com.example.uniwattelektrik.core.navigation.SeniorManagerRoute
import com.example.uniwattelektrik.core.navigation.rememberSeniorManagerNavigator
import com.example.uniwattelektrik.di.AppContainer
import com.example.uniwattelektrik.feature.admin.presentation.screens.AdminAttendanceScreen
import com.example.uniwattelektrik.feature.admin.presentation.screens.AdminInventoryScreen
import com.example.uniwattelektrik.feature.admin.presentation.screens.AdminTasksScreen
import com.example.uniwattelektrik.feature.admin.presentation.screens.NewTaskScreen
import com.example.uniwattelektrik.feature.auth.domain.model.User
import com.example.uniwattelektrik.feature.auth.presentation.state.AuthUiEvent
import com.example.uniwattelektrik.feature.auth.presentation.viewmodel.AuthViewModel
import com.example.uniwattelektrik.feature.user.presentation.screens.ProfileScreen
import com.example.uniwattelektrik.feature.user.presentation.screens.TaskDetailScreen
import com.example.uniwattelektrik.platform.PlatformBackHandler
import kotlinx.coroutines.launch

/**
 * Limited admin shell for employees with `permission == "super"` (Senior Manager).
 *
 * Visible tabs:  Tasks · Inventory · Attendance · Profile
 * Hidden tabs:   Dashboard · Team (Employees)
 * Hidden features: Link Manager, Danger Zone, Leave Approvals, Add Employee
 */
@Composable
fun SeniorManagerShell(
    user: User,
    viewModel: AuthViewModel,
) {
    val nav = rememberSeniorManagerNavigator()
    val workforceVm = remember { AppContainer.createWorkforceViewModel() }
    val inventoryVm = remember { AppContainer.createInventoryViewModel() }

    // Load data for the parent admin tenant.
    // parentAdminId is the owning admin's Firebase UID — use it as the tenant key.
    val adminUid = user.parentAdminId ?: user.id
    LaunchedEffect(adminUid) {
        workforceVm.loadForAdmin(adminUid)
        inventoryVm.loadForAdmin(adminUid)
    }

    val coroutineScope = rememberCoroutineScope()
    val snackbarHost = remember { SnackbarHostState() }

    // In-app notifications for senior manager (same coordinator as admin).
    DisposableEffect(adminUid) {
        AppContainer.adminNotificationsCoordinator.start(coroutineScope, adminUid)
        onDispose { AppContainer.adminNotificationsCoordinator.stop() }
    }

    // Excel import for inventory.
    val excelParser = remember { com.example.uniwattelektrik.core.platform.createExcelParser() }
    val launchExcelPicker = com.example.uniwattelektrik.core.platform.rememberExcelFilePicker { uri ->
        coroutineScope.launch {
            try {
                inventoryVm.stagedSheets = excelParser.parse(uri)
                nav.navigate(SeniorManagerRoute.ImportSpareItemsPreview)
            } catch (e: Exception) {
                snackbarHost.showSnackbar("Failed to read Excel: ${e.message ?: "unknown error"}")
            }
        }
    }

    val tabs = remember {
        listOf(
            BottomNavItem(key = "tasks",      label = "Tasks",   icon = Icons.AutoMirrored.Outlined.Assignment),
            BottomNavItem(key = "inventory",  label = "Stock",   icon = Icons.Outlined.Inventory2),
            BottomNavItem(key = "attendance", label = "Attend",  icon = Icons.Outlined.LocationOn),
            BottomNavItem(key = "more",       label = "More",    icon = Icons.Outlined.Settings),
        )
    }

    val current    = nav.current
    val isTopLevel = current is SeniorManagerRoute.Tab
    val selectedKey = when (current) {
        SeniorManagerRoute.Tasks      -> "tasks"
        SeniorManagerRoute.Inventory  -> "inventory"
        SeniorManagerRoute.Attendance -> "attendance"
        SeniorManagerRoute.Profile    -> "more"
        else                          -> "tasks"
    }

    Box(modifier = Modifier.fillMaxSize()) {

        PlatformBackHandler(enabled = current != SeniorManagerRoute.Tasks) {
            when (current) {
                SeniorManagerRoute.ImportSpareItemsPreview -> {
                    inventoryVm.stagedSheets = null
                    nav.pop()
                }
                else -> {
                    if (!nav.pop()) nav.selectTab(SeniorManagerRoute.Tasks)
                }
            }
        }

        AnimatedContent(
            targetState = current,
            transitionSpec = {
                if (targetState is SeniorManagerRoute.Tab && initialState is SeniorManagerRoute.Tab) {
                    (fadeIn(tween(300)) + scaleIn(initialScale = 0.98f))
                        .togetherWith(fadeOut(tween(200)) + scaleOut(targetScale = 0.98f))
                } else {
                    if (nav.isForward(initialState, targetState)) {
                        (slideInHorizontally(tween(350)) { it } + fadeIn(tween(350)))
                            .togetherWith(slideOutHorizontally(tween(350)) { -it / 3 } + fadeOut(tween(350)))
                    } else {
                        (slideInHorizontally(tween(350)) { -it / 3 } + fadeIn(tween(350)))
                            .togetherWith(slideOutHorizontally(tween(350)) { it } + fadeOut(tween(350)))
                    }
                }
            },
            label = "SeniorManagerTransition",
        ) { r ->
            when (r) {
                // ── Tasks ──────────────────────────────────────────────────
                SeniorManagerRoute.Tasks -> AdminTasksScreen(
                    workforceVm = workforceVm,
                    adminUid    = adminUid,
                    onAssign    = { nav.navigate(SeniorManagerRoute.NewTask()) },
                    onBack      = { nav.selectTab(SeniorManagerRoute.Tasks) },
                    onTaskClick = { id -> nav.navigate(SeniorManagerRoute.TaskDetail(id)) },
                    onEditTask  = { id -> nav.navigate(SeniorManagerRoute.NewTask(editTaskId = id)) },
                )
                is SeniorManagerRoute.TaskDetail -> TaskDetailScreen(
                    taskId          = r.taskId,
                    onBack          = { nav.pop() },
                    onStartWork     = { nav.pop() },
                    workforceVm     = workforceVm,
                    viewerRole      = com.example.uniwattelektrik.feature.user.presentation.screens.TaskDetailRole.Admin,
                    currentUserId   = user.id,
                    currentUserName = user.displayName?.takeIf { it.isNotBlank() } ?: user.email,
                    adminUid        = adminUid,
                    onEdit          = { id -> nav.navigate(SeniorManagerRoute.NewTask(editTaskId = id)) },
                )
                is SeniorManagerRoute.NewTask -> NewTaskScreen(
                    workforceVm      = workforceVm,
                    inventoryVm      = inventoryVm,
                    adminUid         = adminUid,
                    onBack           = { nav.pop() },
                    onCreated        = { nav.pop() },
                    adminDisplayName = user.displayName?.takeIf { it.isNotBlank() } ?: user.email,
                    editTaskId       = r.editTaskId,
                )

                // ── Inventory ──────────────────────────────────────────────
                SeniorManagerRoute.Inventory -> AdminInventoryScreen(
                    inventoryVm   = inventoryVm,
                    adminId       = adminUid,
                    onManage      = { nav.navigate(SeniorManagerRoute.InventoryManagement) },
                    onAddSpare    = { nav.navigate(SeniorManagerRoute.SpareItemForm(null)) },
                    onImportExcel = launchExcelPicker,
                    onItemClick   = { item -> nav.navigate(SeniorManagerRoute.SpareItemDetail(item)) },
                )
                SeniorManagerRoute.InventoryManagement ->
                    com.example.uniwattelektrik.feature.admin.presentation.screens.inventory.InventoryManagementScreen(
                        onBack        = { nav.pop() },
                        onDepartments = { nav.navigate(SeniorManagerRoute.Departments) },
                        onEquipment   = { nav.navigate(SeniorManagerRoute.Equipment) },
                        onPriceList   = { nav.navigate(SeniorManagerRoute.SpareList) },
                    )
                SeniorManagerRoute.Departments ->
                    com.example.uniwattelektrik.feature.admin.presentation.screens.inventory.DepartmentScreen(
                        onBack      = { nav.pop() },
                        inventoryVm = inventoryVm,
                        adminId     = adminUid,
                    )
                SeniorManagerRoute.Equipment ->
                    com.example.uniwattelektrik.feature.admin.presentation.screens.inventory.EquipmentScreen(
                        onBack      = { nav.pop() },
                        inventoryVm = inventoryVm,
                        adminId     = adminUid,
                    )
                SeniorManagerRoute.SpareList ->
                    com.example.uniwattelektrik.feature.admin.presentation.screens.inventory.SpareListScreen(
                        adminId     = adminUid,
                        inventoryVm = inventoryVm,
                        onBack      = { nav.pop() },
                        onAdd       = { nav.navigate(SeniorManagerRoute.SpareItemForm(null)) },
                        onEdit      = { item -> nav.navigate(SeniorManagerRoute.SpareItemForm(item)) },
                        onItemClick = { item -> nav.navigate(SeniorManagerRoute.SpareItemDetail(item)) },
                    )
                is SeniorManagerRoute.SpareItemDetail ->
                    com.example.uniwattelektrik.feature.admin.presentation.screens.inventory.SpareItemDetailScreen(
                        item    = r.item,
                        onBack  = { nav.pop() },
                        onEdit  = { nav.pop(); nav.navigate(SeniorManagerRoute.SpareItemForm(r.item)) },
                        onDelete = {
                            inventoryVm.deleteSpareItem(adminUid, r.item.id)
                            nav.pop()
                        },
                    )
                is SeniorManagerRoute.SpareItemForm ->
                    com.example.uniwattelektrik.feature.admin.presentation.screens.inventory.SpareItemFormScreen(
                        initial     = r.item,
                        onBack      = { nav.pop() },
                        inventoryVm = inventoryVm,
                        onSave      = { item ->
                            inventoryVm.saveSpareItem(adminUid, item)
                            nav.pop()
                        },
                    )
                SeniorManagerRoute.ImportSpareItemsPreview ->
                    com.example.uniwattelektrik.feature.admin.presentation.screens.inventory.ImportSpareItemsPreviewScreen(
                        adminId     = adminUid,
                        inventoryVm = inventoryVm,
                        onBack      = { inventoryVm.stagedSheets = null; nav.pop() },
                        onSuccess   = { n ->
                            inventoryVm.stagedSheets = null
                            nav.pop()
                            coroutineScope.launch { snackbarHost.showSnackbar("Imported $n spares") }
                        },
                    )

                // ── Attendance ─────────────────────────────────────────────
                SeniorManagerRoute.Attendance -> AdminAttendanceScreen(
                    workforceVm = workforceVm,
                    onBack      = { nav.selectTab(SeniorManagerRoute.Tasks) },
                )

                // ── Profile / More ─────────────────────────────────────────
                SeniorManagerRoute.Profile -> ProfileScreen(
                    user        = user,
                    initials    = seniorInitials(user),
                    name        = user.displayName?.takeIf { it.isNotBlank() } ?: user.email,
                    role        = "Senior Manager",
                    onLogout    = { viewModel.onEvent(AuthUiEvent.Logout) },
                    workforceVm = null,    // hide admin-only modules
                    onNavigate  = { },     // no admin routes from here
                    isSeniorManager = true,
                )
            }
        }

        if (isTopLevel) {
            AppBottomNavBar(
                items       = tabs,
                selectedKey = selectedKey,
                onSelected  = { key ->
                    when (key) {
                        "tasks"      -> nav.selectTab(SeniorManagerRoute.Tasks)
                        "inventory"  -> nav.selectTab(SeniorManagerRoute.Inventory)
                        "attendance" -> nav.selectTab(SeniorManagerRoute.Attendance)
                        "more"       -> nav.selectTab(SeniorManagerRoute.Profile)
                    }
                },
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }

        SnackbarHost(hostState = snackbarHost, modifier = Modifier.align(Alignment.BottomCenter))
    }
}

private fun seniorInitials(user: User): String {
    val raw = user.displayName?.takeIf { it.isNotBlank() } ?: user.email.substringBefore("@")
    val parts = raw.split(" ", "_", ".", "-").filter { it.isNotBlank() }
    return parts.take(2).mapNotNull { it.firstOrNull()?.uppercaseChar() }
        .joinToString("")
        .ifEmpty { raw.take(2).uppercase() }
}
