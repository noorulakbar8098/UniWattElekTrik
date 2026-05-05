package com.example.uniwattelektrik.core.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

/* ──────────────────────────────────────────────────────────────────────────
 *  USER (Employee) routes
 * ────────────────────────────────────────────────────────────────────────── */

sealed interface UserRoute {
    /** Top-level tabs reachable from the bottom nav. */
    sealed interface Tab : UserRoute { val tabKey: TabKey }

    data object Home              : Tab { override val tabKey = TabKey.Home }
    data object Tasks             : Tab { override val tabKey = TabKey.Tasks }
    data object Leave             : Tab { override val tabKey = TabKey.Leave }
    data object Profile           : Tab { override val tabKey = TabKey.Profile }

    /** Stack-pushed routes (no bottom nav highlight; back returns to previous). */
    data class TaskDetail(val taskId: String) : UserRoute
    data object Notifications                : UserRoute
    data class WorkCompletion(val taskId: String)  : UserRoute
    data object Attendance                   : UserRoute
    data object LiveMap                      : UserRoute

    enum class TabKey { Home, Tasks, Leave, Profile }
}

/* ──────────────────────────────────────────────────────────────────────────
 *  ADMIN routes
 * ────────────────────────────────────────────────────────────────────────── */

sealed interface AdminRoute {
    sealed interface Tab : AdminRoute { val tabKey: TabKey }

    data object Dashboard   : Tab { override val tabKey = TabKey.Dashboard }
    data object Employees   : Tab { override val tabKey = TabKey.Employees }
    data object Tasks       : Tab { override val tabKey = TabKey.Tasks }
    data object Attendance  : Tab { override val tabKey = TabKey.Attendance }
    data object Inventory   : Tab { override val tabKey = TabKey.Inventory }
    data object Profile     : Tab { override val tabKey = TabKey.Profile }

    data class EmployeeDetail(val employeeId: String) : AdminRoute
    data class TaskDetail(val taskId: String) : AdminRoute
    data object Notifications        : AdminRoute
    data object Financials           : AdminRoute
    data object NewTask              : AdminRoute
    data object InventoryManagement  : AdminRoute
    data object Departments          : AdminRoute
    data object Equipment            : AdminRoute
    data object SpareList            : AdminRoute
    data class  SpareItemForm(val item: com.example.uniwattelektrik.feature.admin.presentation.screens.inventory.SpareItem? = null) : AdminRoute
    data class  SpareItemDetail(val item: com.example.uniwattelektrik.feature.admin.presentation.screens.inventory.SpareItem) : AdminRoute
    data object ImportSpareItemsPreview : AdminRoute

    enum class TabKey { Dashboard, Employees, Tasks, Attendance, Inventory, Profile }
}

/* ──────────────────────────────────────────────────────────────────────────
 *  Navigator state holders
 *
 *  Tiny in-memory back stack (no Compose-Nav dependency, KMP-clean).
 *  `navigate()` pushes; `pop()` returns to previous; tapping a tab clears
 *  the stack to that tab's root.
 * ────────────────────────────────────────────────────────────────────────── */

class UserNavigator internal constructor() {
    private val stack = mutableListOf<UserRoute>(UserRoute.Home)
    var current by mutableStateOf<UserRoute>(UserRoute.Home)
        private set

    fun navigate(route: UserRoute) {
        stack += route
        current = route
    }

    /** Switch top-level tab — replaces the entire stack with that tab's root. */
    fun selectTab(tab: UserRoute.Tab) {
        stack.clear()
        stack += tab
        current = tab
    }

    /** Returns true if the back-press was consumed; false means caller should exit. */
    fun pop(): Boolean {
        if (stack.size <= 1) return false
        stack.removeAt(stack.lastIndex)
        current = stack.last()
        return true
    }

    /**
     * Simple heuristic to determine if navigation is "forward" (deeper into stack)
     * or "backward" (popping/returning).
     */
    fun isForward(from: AdminRoute, to: AdminRoute): Boolean {
        // If 'to' is a tab, it's usually considered a reset or horizontal move,
        // but if we are moving from a Tab to a Non-Tab, it's definitely forward.
        if (from is AdminRoute.Tab && to !is AdminRoute.Tab) return true
        
        // Specific inventory module flow
        if (from is AdminRoute.Inventory && to is AdminRoute.InventoryManagement) return true
        if (from is AdminRoute.InventoryManagement && (to is AdminRoute.Departments || to is AdminRoute.Equipment || to is AdminRoute.SpareList)) return true
        if (to is AdminRoute.SpareItemForm) return true
        if (to is AdminRoute.SpareItemDetail) return true
        if (to is AdminRoute.ImportSpareItemsPreview) return true

        return false
    }
}

class AdminNavigator internal constructor() {
    private val stack = mutableListOf<AdminRoute>(AdminRoute.Dashboard)
    var current by mutableStateOf<AdminRoute>(AdminRoute.Dashboard)
        private set

    fun navigate(route: AdminRoute) {
        stack += route
        current = route
    }

    fun selectTab(tab: AdminRoute.Tab) {
        stack.clear()
        stack += tab
        current = tab
    }

    fun pop(): Boolean {
        if (stack.size <= 1) return false
        stack.removeAt(stack.lastIndex)
        current = stack.last()
        return true
    }

    /**
     * Simple heuristic to determine if navigation is "forward" (deeper into stack)
     * or "backward" (popping/returning).
     */
    fun isForward(from: AdminRoute, to: AdminRoute): Boolean {
        // If 'to' is a tab, it's usually considered a reset or horizontal move,
        // but if we are moving from a Tab to a Non-Tab, it's definitely forward.
        if (from is AdminRoute.Tab && to !is AdminRoute.Tab) return true
        
        // Specific inventory module flow
        if (from is AdminRoute.Inventory && to is AdminRoute.InventoryManagement) return true
        if (from is AdminRoute.InventoryManagement && (to is AdminRoute.Departments || to is AdminRoute.Equipment || to is AdminRoute.SpareList)) return true
        if (to is AdminRoute.SpareItemForm) return true
        if (to is AdminRoute.SpareItemDetail) return true
        if (to is AdminRoute.ImportSpareItemsPreview) return true

        return false
    }
}

@Composable fun rememberUserNavigator(): UserNavigator   = remember { UserNavigator() }
@Composable fun rememberAdminNavigator(): AdminNavigator = remember { AdminNavigator() }
