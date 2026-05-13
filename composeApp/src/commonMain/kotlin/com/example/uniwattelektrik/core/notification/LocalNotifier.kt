package com.example.uniwattelektrik.core.notification

/**
 * Posts a Material You-styled local notification on the host platform.
 *
 * Used by [AdminNotificationsCoordinator] / [UserNotificationsCoordinator] to
 * surface in-app events (new task, leave update, low stock, etc.) without
 * requiring a server / Cloud Functions / Blaze plan.
 *
 * Android: [androidx.core.app.NotificationCompat] on the existing
 * `uw_default` channel. Tap launches MainActivity with `type` extra so the
 * existing [DeepLinkBus] routing kicks in.
 *
 * iOS: no-op stub for now (Android-only feature).
 */
expect class LocalNotifier() {
    /**
     * Show (or replace) a notification.
     *
     * @param id stable per-event id — passing the same id replaces the
     *   previous notification rather than stacking a duplicate.
     * @param routeKey one of the values understood by `DeepLinkBus`
     *   ("tasks", "leave", "approvals", "inventory", "attendance", "home").
     * @param taskId optional Firestore task id — when present the tap
     *   intent carries it through to [DeepLinkBus.taskId] so the shell can
     *   pop the matching detail screen on top of the route.
     */
    fun notify(id: Int, title: String, body: String, routeKey: String?, taskId: String? = null)
}

