package com.example.uniwattelektrik.feature.user.presentation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Assignment
import androidx.compose.material.icons.outlined.BeachAccess
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.uniwattelektrik.di.AppContainer
import com.example.uniwattelektrik.feature.workforce.presentation.WorkforceViewModel
import com.example.uniwattelektrik.core.components.AppBottomNavBar
import com.example.uniwattelektrik.core.components.BottomNavItem
import com.example.uniwattelektrik.core.navigation.UserNavigator
import com.example.uniwattelektrik.core.navigation.UserRoute
import com.example.uniwattelektrik.core.navigation.rememberUserNavigator
import com.example.uniwattelektrik.feature.auth.domain.model.User
import com.example.uniwattelektrik.feature.auth.presentation.screens.HomeScreen
import com.example.uniwattelektrik.feature.auth.presentation.viewmodel.AuthViewModel
import com.example.uniwattelektrik.feature.user.presentation.screens.LeaveScreen
import com.example.uniwattelektrik.feature.user.presentation.screens.NotificationsScreen
import com.example.uniwattelektrik.feature.user.presentation.screens.ProfileScreen
import com.example.uniwattelektrik.feature.user.presentation.screens.CompleteWorkScreen
import com.example.uniwattelektrik.feature.user.presentation.screens.TaskDetailScreen
import com.example.uniwattelektrik.feature.user.presentation.screens.TaskListScreen

/**
 * Top-level container for the Employee app. Owns the [UserNavigator] and
 * decides which screen to render plus whether the bottom nav is visible.
 */
@Composable
fun UserShell(
    user: User,
    viewModel: AuthViewModel,
) {
    val nav: UserNavigator = rememberUserNavigator()
    val workforceVm: WorkforceViewModel = remember { AppContainer.createWorkforceViewModel() }
    LaunchedEffect(user.id, user.parentAdminId) {
        workforceVm.loadForUser(user.id, adminUid = user.parentAdminId)
    }

    val tabs = remember {
        listOf(
            BottomNavItem(key = "home",    label = "Home",    icon = Icons.Outlined.Home),
            BottomNavItem(key = "tasks",   label = "Tasks",   icon = Icons.AutoMirrored.Outlined.Assignment),
            BottomNavItem(key = "leave",   label = "Leave",   icon = Icons.Outlined.BeachAccess),
            BottomNavItem(key = "profile", label = "Profile", icon = Icons.Outlined.Person),
        )
    }

    val current      = nav.current
    val isTopLevel   = current is UserRoute.Tab
    val selectedKey  = (current as? UserRoute.Tab)?.tabKey?.name?.lowercase() ?: "home"

    Box(
        modifier = Modifier
            .fillMaxSize()
            // Pad only the bottom (gesture handle) — each screen extends behind
            // the status bar and adds its own statusBars inset inside the header,
            // letting `SetStatusBar(color)` paint the gradient under the clock.
            .windowInsetsPadding(WindowInsets.navigationBars),
    ) {
        // ── Active screen ──────────────────────────────────────────────────
        when (val r = current) {
            UserRoute.Home -> HomeScreen(
                user      = user,
                viewModel = viewModel,
                workforceVm = workforceVm,
                onOpenNotifications = { nav.navigate(UserRoute.Notifications) },
                onOpenTasks         = { nav.selectTab(UserRoute.Tasks) },
            )
            UserRoute.Tasks -> TaskListScreen(
                onTaskClick = { id -> nav.navigate(UserRoute.TaskDetail(id)) },
                workforceVm = workforceVm,
            )
            UserRoute.Leave -> LeaveScreen()
            UserRoute.Profile -> ProfileScreen(
                user     = user,
                initials = user.initialsForAvatar(),
                name     = user.displayName?.takeIf { it.isNotBlank() } ?: user.email,
                role     = "Substation Engineer · L2",
                onLogout = { viewModel.onEvent(com.example.uniwattelektrik.feature.auth.presentation.state.AuthUiEvent.Logout) },
            )
            is UserRoute.TaskDetail -> TaskDetailScreen(
                taskId      = r.taskId,
                onBack      = { nav.pop() },
                onStartWork = { id -> nav.navigate(UserRoute.WorkCompletion(id)) },
                workforceVm = workforceVm,
            )
            is UserRoute.WorkCompletion -> CompleteWorkScreen(
                taskId      = r.taskId,
                onClose     = { nav.pop() },
                onSubmitted = { nav.selectTab(UserRoute.Home) },
            )
            UserRoute.Notifications -> NotificationsScreen(onBack = { nav.pop() })
            UserRoute.Attendance,
            UserRoute.LiveMap -> com.example.uniwattelektrik.core.components.EmptyState(
                emoji = "🚧",
                title = "Coming next",
                body  = "This screen is scaffolded — the design system + navigation are wired so we can drop the implementation in.",
            )
        }

        // ── Bottom nav (only on top-level tabs) ────────────────────────────
        if (isTopLevel) {
            AppBottomNavBar(
                items       = tabs,
                selectedKey = selectedKey,
                onSelected  = { key ->
                    when (key) {
                        "home"    -> nav.selectTab(UserRoute.Home)
                        "tasks"   -> nav.selectTab(UserRoute.Tasks)
                        "leave"   -> nav.selectTab(UserRoute.Leave)
                        "profile" -> nav.selectTab(UserRoute.Profile)
                    }
                },
                onFabClick  = { /* TODO: quick check-in / new task */ },
                modifier    = Modifier.align(Alignment.BottomCenter),
            )
        }
    }
}

/** Build the avatar initials from the User's display name, falling back to email. */
private fun User.initialsForAvatar(): String {
    val raw = displayName?.takeIf { it.isNotBlank() } ?: email.substringBefore("@")
    val parts = raw.split(" ", "_", ".", "-").filter { it.isNotBlank() }
    return parts.take(2).mapNotNull { it.firstOrNull()?.uppercaseChar() }
        .joinToString("")
        .ifEmpty { raw.take(2).uppercase() }
}
