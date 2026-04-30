package com.example.uniwattelektrik.platform

import androidx.compose.runtime.Composable

@Composable
actual fun PlatformBackHandler(enabled: Boolean, onBack: () -> Unit) {
    // iOS has no system back gesture to intercept; this is a no-op.
    // Navigation is handled by the SwiftUI layer or explicit Cancel buttons.
}

