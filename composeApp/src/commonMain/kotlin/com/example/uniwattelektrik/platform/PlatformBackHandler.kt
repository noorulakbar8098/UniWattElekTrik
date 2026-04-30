package com.example.uniwattelektrik.platform

import androidx.compose.runtime.Composable

/**
 * Cross-platform "system back" handler.
 *
 * - Android: delegates to `androidx.activity.compose.BackHandler` so the
 *   hardware/gesture back button is consumed when [enabled] is true.
 * - iOS    : no-op (iOS has no analogous system gesture hook here; the
 *   navigation host can offer an explicit Cancel button instead).
 */
@Composable
expect fun PlatformBackHandler(enabled: Boolean, onBack: () -> Unit)
