package com.example.uniwattelektrik.core.platform

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import java.io.File
import java.io.FileOutputStream

/**
 * Android implementation backed by `ActivityResultContracts`.
 *  - Gallery: system Photo Picker (PickVisualMedia).
 *  - Camera : TakePicturePreview (returns Bitmap → staged to cache file).
 */
actual class AttachmentLauncher internal constructor(
    private val galleryLauncher: () -> Unit,
    private val cameraLauncher: () -> Unit,
) {
    actual fun launchGallery() = galleryLauncher()
    actual fun launchCamera() = cameraLauncher()
}

@Composable
actual fun rememberAttachmentLauncher(
    onPicked: (String) -> Unit,
): AttachmentLauncher {
    val context = LocalContext.current

    val gallery = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
    ) { uri: Uri? ->
        if (uri != null) onPicked(uri.toString())
    }

    val camera = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicturePreview(),
    ) { bitmap: Bitmap? ->
        if (bitmap != null) {
            val staged = stageBitmapToCache(context, bitmap)
            onPicked(staged.toString())
        }
    }

    return remember {
        AttachmentLauncher(
            galleryLauncher = {
                gallery.launch(
                    PickVisualMediaRequest(
                        ActivityResultContracts.PickVisualMedia.ImageOnly,
                    ),
                )
            },
            cameraLauncher = { camera.launch(null) },
        )
    }
}

private fun stageBitmapToCache(context: Context, bitmap: Bitmap): Uri {
    val dir = File(context.cacheDir, "attachments").apply { mkdirs() }
    val file = File(dir, "att_${System.currentTimeMillis()}.jpg")
    FileOutputStream(file).use { os ->
        bitmap.compress(Bitmap.CompressFormat.JPEG, 88, os)
    }
    return Uri.fromFile(file)
}

