package com.example.uniwattelektrik.core.notification

/** iOS: no-op until UNUserNotificationCenter integration lands. */
actual class LocalNotifier actual constructor() {
    actual fun notify(id: Int, title: String, body: String, routeKey: String?) {
        // intentionally empty
    }
}

