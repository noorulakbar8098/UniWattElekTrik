package com.example.uniwattelektrik

import android.content.Context

/**
 * Static holder for the Android application context — consumed by `actual` classes in
 * androidMain that need a `Context` (e.g., [com.example.uniwattelektrik.platform.SessionStorage]).
 * Initialised once from [MainActivity.onCreate].
 */
internal object AndroidAppContext {
    lateinit var application: Context
        private set

    fun init(context: Context) {
        if (!::application.isInitialized) {
            application = context.applicationContext
        }
    }
}

