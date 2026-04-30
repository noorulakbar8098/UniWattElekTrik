package com.example.uniwattelektrik.core

/**
 * Tiny multiplatform logger. All logs go through a single "UniWatt" tag
 * so you can filter the whole app with:  adb logcat -s UniWatt:V
 *
 * Usage:
 *   AppLog.d("PATH", "admins/$adminId/users")
 *   AppLog.e("Firestore", "register failed", throwable)
 */
expect object AppLog {
    fun d(tag: String, msg: String)
    fun i(tag: String, msg: String)
    fun w(tag: String, msg: String, throwable: Throwable? = null)
    fun e(tag: String, msg: String, throwable: Throwable? = null)
}

