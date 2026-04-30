package com.example.uniwattelektrik.feature.admin.presentation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Assignment
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.uniwattelektrik.core.components.AppBottomNavBar
import com.example.uniwattelektrik.core.components.BottomNavItem
import com.example.uniwattelektrik.core.components.EmptyState
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
    val showAddEmployee = remember { mutableStateOf(false) }
    LaunchedEffect(user.id) { workforceVm.loadForAdmin(user.id) }

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
        when (val r = current) {
            AdminRoute.Dashboard -> AdminHomeScreen(
                user                = user,
                workforceVm         = workforceVm,
                onOpenNotifications = { nav.navigate(AdminRoute.Notifications) },
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
            )
            AdminRoute.NewTask   -> NewTaskScreen(
                workforceVm = workforceVm,
                adminUid    = user.id,
                onBack      = { nav.pop() },
                onCreated   = { nav.pop() },
            )
            AdminRoute.Attendance -> AdminAttendanceScreen(
                workforceVm = workforceVm,
                onBack      = { nav.selectTab(AdminRoute.Dashboard) },
            )
            AdminRoute.Inventory -> AdminInventoryScreen()
            AdminRoute.Profile   -> ProfileScreen(
                user     = user,
                initials = user.adminInitials(),
                name     = user.displayName?.takeIf { it.isNotBlank() } ?: user.email,
                role     = "Operations Admin · ${user.adminId ?: "—"}",
                onLogout = { viewModel.onEvent(AuthUiEvent.Logout) },
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
    }
}

private fun User.adminInitials(): String {
    val raw = displayName?.takeIf { it.isNotBlank() } ?: email.substringBefore("@")
    val parts = raw.split(" ", "_", ".", "-").filter { it.isNotBlank() }
    return parts.take(2).mapNotNull { it.firstOrNull()?.uppercaseChar() }
        .joinToString("")
        .ifEmpty { raw.take(2).uppercase() }
}
