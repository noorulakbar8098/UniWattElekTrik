package com.example.uniwattelektrik

import android.app.Application

/**
 * Custom Application class — the ONE entry point Android guarantees to run
 * before any other component (Activities, BroadcastReceivers, Services,
 * Workers) in this process.
 *
 * Why this matters: `AndroidAppContext.application` used to be seeded only
 * in `MainActivity.onCreate`. When the OS spawned a fresh process to deliver
 * an alarm broadcast or an FCM message while the app was killed, MainActivity
 * never ran → the `lateinit` context stayed uninitialised → `LocalNotifier`
 * crashed silently and the user saw no notification until they re-opened the
 * app (which then ran MainActivity and posted the notification belatedly).
 *
 * Seeding the context here closes that gap for every entry path.
 */
class UniWattApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        AndroidAppContext.init(this)
    }
}

