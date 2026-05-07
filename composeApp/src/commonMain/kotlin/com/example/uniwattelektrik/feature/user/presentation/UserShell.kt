package com.example.uniwattelektrik.feature.user.presentation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Assignment
import androidx.compose.material.icons.outlined.BeachAccess
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.uniwattelektrik.core.theme.AppTheme
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
import com.example.uniwattelektrik.feature.user.presentation.screens.LiveMapScreen
import com.example.uniwattelektrik.feature.user.presentation.screens.NotificationsScreen
import com.example.uniwattelektrik.feature.user.presentation.screens.ProfileScreen
import com.example.uniwattelektrik.feature.user.presentation.screens.CompleteWorkScreen
import com.example.uniwattelektrik.feature.user.presentation.screens.TaskDetailScreen
import com.example.uniwattelektrik.feature.user.presentation.screens.TaskListScreen
import com.example.uniwattelektrik.platform.PlatformBackHandler

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
    // Re-subscribe + show shimmer when app returns from background.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        workforceVm.refresh()
    }
    val workforceError  by workforceVm.error.collectAsStateWithLifecycle()

    // ── Free, in-app notifications ───────────────────────────────────────────
    // Listens for new tasks assigned to this user + leave approval/rejection
    // and posts local notifications. Cancelled on logout (uid change) or
    // when the shell leaves composition.
    val notifScope = rememberCoroutineScope()
    DisposableEffect(user.id) {
        AppContainer.userNotificationsCoordinator.start(notifScope, user.id)
        onDispose { AppContainer.userNotificationsCoordinator.stop() }
    }

    // ── FCM deep-link consumer ───────────────────────────────────────────────
    // When a notification tap on Android publishes a route key into
    // [DeepLinkBus], jump to the matching tab. Unknown keys are ignored
    // (e.g. "inventory" — handled by AdminShell instead).
    val deepLink by com.example.uniwattelektrik.core.notification.DeepLinkBus.route
        .collectAsStateWithLifecycle()
    LaunchedEffect(deepLink) {
        when (deepLink) {
            "home"    -> nav.selectTab(UserRoute.Home)
            "tasks"   -> nav.selectTab(UserRoute.Tasks)
            "leave"   -> nav.selectTab(UserRoute.Leave)
            "livemap" -> nav.selectTab(UserRoute.LiveMap)
            "profile" -> nav.selectTab(UserRoute.Profile)
            else      -> return@LaunchedEffect
        }
        com.example.uniwattelektrik.core.notification.DeepLinkBus.consume()
    }

    val tabs = remember {
        listOf(
            BottomNavItem(key = "home",    label = "Home",    icon = Icons.Outlined.Home),
            BottomNavItem(key = "tasks",   label = "Tasks",   icon = Icons.AutoMirrored.Outlined.Assignment),
            BottomNavItem(key = "leave",   label = "Leave",   icon = Icons.Outlined.BeachAccess),
            BottomNavItem(key = "livemap", label = "Map",     icon = Icons.Outlined.Map),
            BottomNavItem(key = "profile", label = "Profile", icon = Icons.Outlined.Person),
        )
    }

    val current      = nav.current
    val isTopLevel   = current is UserRoute.Tab
    val selectedKey  = when (current) {
        UserRoute.Home    -> "home"
        UserRoute.Tasks   -> "tasks"
        UserRoute.Leave   -> "leave"
        UserRoute.LiveMap -> "livemap"
        UserRoute.Profile -> "profile"
        else              -> "home"
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.navigationBars),
    ) {
        // ── System back navigation ─────────────────────────────────────────
        // Priority: pop the in-memory stack first; if we're already on a
        // top-level tab that isn't Home, route to Home; if we're on Home,
        // disable the handler and let the OS finish the Activity.
        PlatformBackHandler(enabled = current != UserRoute.Home) {
            if (!nav.pop()) nav.selectTab(UserRoute.Home)
        }

        // ── Active screen ──────────────────────────────────────────────────
        when (val r = current) {
            UserRoute.Home -> HomeScreen(
                user      = user,
                viewModel = viewModel,
                workforceVm = workforceVm,
                onOpenNotifications = { nav.navigate(UserRoute.Notifications) },
                onOpenTasks         = { nav.selectTab(UserRoute.Tasks) },
                onNewLeave          = { nav.selectTab(UserRoute.Leave) },
                onViewMap           = { nav.selectTab(UserRoute.LiveMap) },
                onTaskDetail        = { id -> nav.navigate(UserRoute.TaskDetail(id)) },
            )
            UserRoute.Tasks -> TaskListScreen(
                onTaskClick = { id -> nav.navigate(UserRoute.TaskDetail(id)) },
                workforceVm = workforceVm,
            )
            UserRoute.Leave -> LeaveScreen(
                workforceVm  = workforceVm,
                userId       = user.id,
                adminId      = user.parentAdminId ?: "",
                employeeName = user.displayName?.takeIf { it.isNotBlank() } ?: user.email,
                department   = "",
            )
            UserRoute.Profile -> ProfileScreen(
                user        = user,
                initials    = user.initialsForAvatar(),
                name        = user.displayName?.takeIf { it.isNotBlank() } ?: user.email,
                role        = "Substation Engineer · L2",
                onLogout    = { viewModel.onEvent(com.example.uniwattelektrik.feature.auth.presentation.state.AuthUiEvent.Logout) },
                isAdmin     = false,
                workforceVm = workforceVm,
            )
            is UserRoute.TaskDetail -> TaskDetailScreen(
                taskId      = r.taskId,
                onBack      = { nav.pop() },
                onStartWork = { id -> nav.navigate(UserRoute.WorkCompletion(id)) },
                workforceVm = workforceVm,
                viewerRole      = com.example.uniwattelektrik.feature.user.presentation.screens.TaskDetailRole.User,
                currentUserId   = user.id,
                currentUserName = user.displayName?.takeIf { it.isNotBlank() } ?: user.email,
                adminUid        = user.parentAdminId ?: "",
            )
            is UserRoute.WorkCompletion -> CompleteWorkScreen(
                taskId      = r.taskId,
                adminId     = user.parentAdminId ?: "",
                userId      = user.id,
                workforceVm = workforceVm,
                onClose     = { nav.pop() },
                onSubmitted = { nav.selectTab(UserRoute.Home) },
            )
            UserRoute.Notifications -> NotificationsScreen(
                onBack      = { nav.pop() },
                workforceVm = workforceVm,
                userId      = user.id,
                adminId     = user.parentAdminId ?: "",
            )
            UserRoute.Attendance -> com.example.uniwattelektrik.core.components.EmptyState(
                emoji = "🚧",
                title = "Coming next",
                body  = "This screen is scaffolded — the design system + navigation are wired so we can drop the implementation in.",
            )
            UserRoute.LiveMap -> LiveMapScreen(
                workforceVm = workforceVm,
                userId      = user.id,
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
                        "livemap" -> nav.selectTab(UserRoute.LiveMap)
                        "profile" -> nav.selectTab(UserRoute.Profile)
                    }
                },
                modifier    = Modifier.align(Alignment.BottomCenter),
            )
        }

        // ── Offline error banner ───────────────────────────────────────────
        AnimatedVisibility(
            visible = workforceError != null,
            enter   = slideInVertically { -it },
            exit    = slideOutVertically { -it },
            modifier = Modifier.align(Alignment.TopCenter),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(AppTheme.Warning)
                    .padding(horizontal = 20.dp, vertical = 8.dp),
            ) {
                Text(
                    "⚠️  Changes will sync when you're back online",
                    color      = AppTheme.Ink900,
                    fontSize   = 12.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
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
