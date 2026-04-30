package com.example.uniwattelektrik.core

actual object AppLog {
    actual fun d(tag: String, msg: String) { println("UniWatt D [$tag] $msg") }
    actual fun i(tag: String, msg: String) { println("UniWatt I [$tag] $msg") }
    actual fun w(tag: String, msg: String, throwable: Throwable?) {
        println("UniWatt W [$tag] $msg ${throwable?.message ?: ""}")
    }
    actual fun e(tag: String, msg: String, throwable: Throwable?) {
        println("UniWatt E [$tag] $msg ${throwable?.message ?: ""}")
        throwable?.printStackTrace()
    }
}

