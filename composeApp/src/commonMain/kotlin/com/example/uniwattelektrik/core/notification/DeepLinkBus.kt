package com.example.uniwattelektrik.core.notification

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * One-shot deep-link bus written by the platform layer (e.g. Android's
 * `MainActivity` translating an FCM-tap intent) and consumed by the role
 * shells (`AdminShell` / `UserShell`).
 *
 * Routes are simple bottom-nav keys reused from the existing shells:
 *   "tasks"   – open the tasks tab
 *   "leave"   – open the leave tab
 *   "inventory" – open the inventory tab
 *   "home"    – open the dashboard / home tab
 *
 * The shell that successfully consumes the value should call [consume] so
 * the same deep-link isn't re-played on configuration change.
 */
object DeepLinkBus {
    private val _route = MutableStateFlow<String?>(null)
    val route: StateFlow<String?> = _route

    fun publish(key: String?) {
        if (key.isNullOrBlank()) return
        _route.value = key
    }

    fun consume() {
        _route.value = null
    }
}

