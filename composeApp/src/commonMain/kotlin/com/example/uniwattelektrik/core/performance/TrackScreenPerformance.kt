package com.example.uniwattelektrik.core.performance

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.platform.LocalInspectionMode
import com.example.uniwattelektrik.core.AppLog
import com.example.uniwattelektrik.di.AppContainer
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * Drop-in performance probe for any screen. Place as the first call inside the
 * top-level screen composable:
 *
 * ```
 * @Composable
 * fun LoginScreen(...) {
 *     TrackScreenPerformance("LoginScreen")
 *     // ...rest of screen
 * }
 * ```
 *
 * Captures three signals per screen visit:
 * - `composition_ms` — time until Compose finished its first composition pass.
 * - `ttff_ms`        — time-to-first-frame after composition (rendering latency).
 * - `visit_ms`       — total wall-clock time the screen was on-screen.
 *
 * On Android these flow into Firebase Performance under the screen name; on iOS
 * they go to `AppLog` (filterable with the `Perf` tag).
 *
 * Non-invasive: emits side-effects only and contributes no layout node.
 */
@OptIn(ExperimentalTime::class)
@Composable
fun TrackScreenPerformance(screenName: String) {
    // Skip in @Preview / tooling so traces don't pollute Firebase from the IDE.
    if (LocalInspectionMode.current) return

    val monitor = remember { AppContainer.performanceMonitor }
    val startMs = remember { Clock.System.now().toEpochMilliseconds() }
    val handle = remember(screenName) { monitor.startScreenTrace(screenName) }

    // Composition + first-frame timing.
    LaunchedEffect(screenName) {
        val composedAt = Clock.System.now().toEpochMilliseconds()
        val compositionMs = composedAt - startMs
        withFrameNanos { /* await next render frame */ }
        val firstFrameMs = Clock.System.now().toEpochMilliseconds() - startMs
        monitor.putMetric(handle, "composition_ms", compositionMs)
        monitor.putMetric(handle, "ttff_ms", firstFrameMs)
        AppLog.i(
            "Perf",
            "$screenName composition=${compositionMs}ms ttff=${firstFrameMs}ms",
        )
    }

    // Visit duration.
    DisposableEffect(screenName) {
        onDispose {
            val visitMs = Clock.System.now().toEpochMilliseconds() - startMs
            monitor.stopScreenTrace(handle, mapOf("visit_ms" to visitMs))
            AppLog.i("Perf", "$screenName visit=${visitMs}ms")
        }
    }
}



