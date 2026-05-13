package com.example.uniwattelektrik.core.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

@Composable
actual fun rememberFileShareLauncher(): FileShareLauncher = remember {
    // iOS: no-op until UIActivityViewController integration lands.
    FileShareLauncher { _, _, _ -> }
}
