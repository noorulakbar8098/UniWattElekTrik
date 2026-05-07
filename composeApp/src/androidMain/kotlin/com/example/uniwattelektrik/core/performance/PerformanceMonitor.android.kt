package com.example.uniwattelektrik.core.performance

import com.google.firebase.perf.FirebasePerformance
import com.google.firebase.perf.metrics.Trace
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import kotlinx.datetime.Clock
import kotlin.time.ExperimentalTime

/**
 * Android implementation backed by Firebase Performance Monitoring.
 *
 * Each screen visit creates a custom [Trace] named after the screen. Custom
 * metrics (`composition_ms`, `ttff_ms`, `visit_ms`) are attached via
 * [Trace.putMetric]. Free-form traces are tracked in an internal map keyed by
 * a monotonic id returned to the caller.
 */
actual class PerformanceMonitor actual constructor() {

    private val perf = FirebasePerformance.getInstance()
    private val activeScreenTraces = ConcurrentHashMap<Long, Trace>()
    private val activeFreeTraces = ConcurrentHashMap<Long, Trace>()
    private val idGen = AtomicLong()

    actual fun startScreenTrace(name: String): ScreenTraceHandle {
        val trace = perf.newTrace(sanitize(name)).also { it.start() }
        val id = idGen.incrementAndGet()
        activeScreenTraces[id] = trace
        @OptIn(ExperimentalTime::class)
        return ScreenTraceHandle(
            name = name,
            startMs = Clock.System.now().toEpochMilliseconds(),
            native = id,
        )
    }

    actual fun putMetric(handle: ScreenTraceHandle, key: String, value: Long) {
        val id = handle.native as? Long ?: return
        activeScreenTraces[id]?.putMetric(sanitize(key), value)
    }

    actual fun stopScreenTrace(handle: ScreenTraceHandle, metrics: Map<String, Long>) {
        val id = handle.native as? Long ?: return
        val trace = activeScreenTraces.remove(id) ?: return
        metrics.forEach { (k, v) -> trace.putMetric(sanitize(k), v) }
        trace.stop()
    }

    actual fun startTrace(name: String): Any? {
        val trace = perf.newTrace(sanitize(name)).also { it.start() }
        val id = idGen.incrementAndGet()
        activeFreeTraces[id] = trace
        return id
    }

    actual fun stopTrace(handle: Any?) {
        val id = handle as? Long ?: return
        activeFreeTraces.remove(id)?.stop()
    }

    /** Firebase trace/metric names: max 100 chars, [a-zA-Z0-9_-] only. */
    private fun sanitize(name: String): String =
        name.replace(Regex("[^A-Za-z0-9_-]"), "_").take(100)

    @OptIn(ExperimentalTime::class)
    private fun nowMs(): Long = Clock.System.now().toEpochMilliseconds()
}

