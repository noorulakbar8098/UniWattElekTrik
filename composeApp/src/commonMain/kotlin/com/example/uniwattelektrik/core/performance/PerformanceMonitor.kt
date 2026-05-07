package com.example.uniwattelektrik.core.performance

/**
 * Cross-platform performance monitor.
 *
 * - Android: backed by Firebase Performance (`FirebasePerformance.newTrace`).
 * - iOS: a logging fallback that still measures wall-clock duration and emits
 *   structured logs through `AppLog` / `NSLog`. (Hook up the Firebase iOS SDK
 *   later via SPM if remote dashboards are required.)
 *
 * Usage from common code:
 * ```
 * val handle = AppContainer.performanceMonitor.startScreenTrace("LoginScreen")
 * ...
 * AppContainer.performanceMonitor.stopScreenTrace(handle, mapOf("ttff_ms" to 120))
 * ```
 *
 * Most screens should not call this directly — prefer the [TrackScreenPerformance]
 * Composable side-effect, which handles start/stop and TTFF automatically.
 */
expect class PerformanceMonitor() {
    fun startScreenTrace(name: String): ScreenTraceHandle
    fun putMetric(handle: ScreenTraceHandle, key: String, value: Long)
    fun stopScreenTrace(handle: ScreenTraceHandle, metrics: Map<String, Long> = emptyMap())

    /** Free-form trace (e.g. data-layer latency). Returns an opaque handle or null. */
    fun startTrace(name: String): Any?
    fun stopTrace(handle: Any?)
}

/**
 * Opaque, common-side handle returned by [PerformanceMonitor.startScreenTrace].
 * Stores the screen name and start timestamp so visit duration can be computed
 * even on platforms where the underlying SDK is a no-op.
 */
data class ScreenTraceHandle(
    val name: String,
    val startMs: Long,
    /** Platform-specific underlying trace (e.g. Firebase `Trace` on Android). */
    val native: Any? = null,
)

