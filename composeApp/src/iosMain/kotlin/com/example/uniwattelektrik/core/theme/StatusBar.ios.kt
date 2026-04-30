package com.example.uniwattelektrik.core.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * iOS no-op for now. The Compose UIViewController already extends edge-to-edge
 * so the screen's gradient header paints under the status bar; the icons
 * default to light glyphs which suit the dark gradient on every screen.
 *
 * Future improvement: bridge to `UIViewController.preferredStatusBarStyle`
 * via `setNeedsStatusBarAppearanceUpdate()`.
 */
@Composable
actual fun SetStatusBar(color: Color, darkIcons: Boolean) {
    // intentional no-op
}

