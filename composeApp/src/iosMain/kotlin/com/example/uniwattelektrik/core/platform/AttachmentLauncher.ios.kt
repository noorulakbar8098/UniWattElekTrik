package com.example.uniwattelektrik.core.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

actual class AttachmentLauncher internal constructor() {
    actual fun launchGallery() { /* no-op on iOS */ }
    actual fun launchCamera()  { /* no-op on iOS */ }
}

@Composable
actual fun rememberAttachmentLauncher(
    onPicked: (String) -> Unit,
): AttachmentLauncher = remember { AttachmentLauncher() }

