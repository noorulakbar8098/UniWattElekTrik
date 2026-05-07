package com.example.uniwattelektrik.core.performance

import com.example.uniwattelektrik.core.AppLog
import kotlin.concurrent.AtomicLong
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * iOS implementation: no remote Firebase Performance backend (would require
 * adding `FirebasePerformance` via SPM in `iosApp.xcodeproj`). Instead we
 * measure wall-clock duration in Kotlin and emit structured `AppLog` entries
 * so the same screen-perf signals show up in Xcode console / device logs.
 */
actual class PerformanceMonitor actual constructor() {

    private val idGen = AtomicLong(0L)

    actual fun startScreenTrace(name: String): ScreenTraceHandle {
        AppLog.i("Perf", "start $name")
        return ScreenTraceHandle(
            name = name,
            startMs = nowMs(),
            native = idGen.addAndGet(1L),
        )
    }

    actual fun putMetric(handle: ScreenTraceHandle, key: String, value: Long) {
        AppLog.i("Perf", "${handle.name} $key=$value")
    }

    actual fun stopScreenTrace(handle: ScreenTraceHandle, metrics: Map<String, Long>) {
        val extras = if (metrics.isEmpty()) "" else
            metrics.entries.joinToString(prefix = " ", separator = " ") { "${it.key}=${it.value}" }
        AppLog.i("Perf", "stop ${handle.name}$extras")
    }

    actual fun startTrace(name: String): Any? {
        AppLog.i("Perf", "trace.start $name")
        return TraceMark(name, nowMs())
    }

    actual fun stopTrace(handle: Any?) {
        val mark = handle as? TraceMark ?: return
        val ms = nowMs() - mark.startMs
        AppLog.i("Perf", "trace.stop ${mark.name} duration=${ms}ms")
    }

    @OptIn(ExperimentalTime::class)
    private fun nowMs(): Long = Clock.System.now().toEpochMilliseconds()

    private data class TraceMark(val name: String, val startMs: Long)
}

