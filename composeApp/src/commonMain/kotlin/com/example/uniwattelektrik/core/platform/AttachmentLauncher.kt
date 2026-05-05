package com.example.uniwattelektrik.core.platform

import androidx.compose.runtime.Composable

/**
 * Cross-platform image-picker abstraction used by Task Creation /
 * Task Detail screens to grab photos for attachments.
 *
 * On Android: gallery uses the system Photo Picker (no permission needed),
 * camera uses ACTION_IMAGE_CAPTURE preview (no permission needed) and the
 * captured bitmap is staged to the cache dir as a JPEG.
 *
 * On iOS: stub — calling [launchGallery] / [launchCamera] is a no-op.
 *
 * Result strings are local URIs (content://… on Android, file:// fallback
 * for camera) suitable for passing to [WorkforceDirectory.uploadTaskAttachment].
 */
expect class AttachmentLauncher {
    fun launchGallery()
    fun launchCamera()
}

/**
 * Remember a launcher bound to the current Compose context. [onPicked] is
 * invoked once per successful selection / capture with a URI string that
 * can be uploaded.
 */
@Composable
expect fun rememberAttachmentLauncher(
    onPicked: (String) -> Unit,
): AttachmentLauncher

