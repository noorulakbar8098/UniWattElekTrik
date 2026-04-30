package com.example.uniwattelektrik.core

import android.util.Log

private const val ROOT_TAG = "UniWatt"

actual object AppLog {
    actual fun d(tag: String, msg: String) { Log.d(ROOT_TAG, "[$tag] $msg") }
    actual fun i(tag: String, msg: String) { Log.i(ROOT_TAG, "[$tag] $msg") }
    actual fun w(tag: String, msg: String, throwable: Throwable?) {
        if (throwable != null) Log.w(ROOT_TAG, "[$tag] $msg", throwable)
        else Log.w(ROOT_TAG, "[$tag] $msg")
    }
    actual fun e(tag: String, msg: String, throwable: Throwable?) {
        if (throwable != null) Log.e(ROOT_TAG, "[$tag] $msg", throwable)
        else Log.e(ROOT_TAG, "[$tag] $msg")
    }
}

