package com.example.uniwattelektrik.core.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * Tints the platform status bar to [color] and toggles whether the system icons
 * (clock, battery, signal) render as dark or light. Call once near the top of
 * each top-level screen so navigating between tabs always pushes a status-bar
 * style that matches the screen's header.
 *
 *  - Android: writes `Window.statusBarColor` + `WindowInsetsController.isAppearanceLightStatusBars`.
 *  - iOS: currently a no-op (the gradient already extends edge-to-edge under the bar).
 */
@Composable
expect fun SetStatusBar(color: Color, darkIcons: Boolean = color.shouldUseDarkIcons())

/**
 * Returns true if [this] color is light enough that dark status-bar icons
 * (black clock/battery) will read better than white ones. Uses a fast
 * perceptual luma approximation — close enough to pick the right icon style.
 */
fun Color.shouldUseDarkIcons(): Boolean {
    val luma = 0.299f * red + 0.587f * green + 0.114f * blue
    return luma > 0.6f
}

