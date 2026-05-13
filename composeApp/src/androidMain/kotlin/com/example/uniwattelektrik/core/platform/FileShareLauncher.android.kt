package com.example.uniwattelektrik.core.platform

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import java.io.File

@Composable
actual fun rememberFileShareLauncher(): FileShareLauncher {
    val context = LocalContext.current
    return remember(context) {
        FileShareLauncher { content, filename, mimeType ->
            shareViaIntent(context, content, filename, mimeType)
        }
    }
}

/**
 * Write [content] to `cacheDir/shared/<filename>`, grab a `content://` URI
 * via the manifest-declared FileProvider, and fire `ACTION_SEND` so the OS
 * picks a target app (Drive / Gmail / Files / etc.).
 *
 * Failures are caught and silently dropped — the caller has already shown
 * any "in progress" UI and doesn't have a sensible recovery path if the
 * device has no share targets installed.
 */
private fun shareViaIntent(
    context : Context,
    content : String,
    filename: String,
    mimeType: String,
) {
    runCatching {
        val shareDir = File(context.cacheDir, "shared").apply { mkdirs() }
        val outFile  = File(shareDir, filename)
        outFile.writeText(content, Charsets.UTF_8)

        val authority = "${context.packageName}.fileprovider"
        val uri = FileProvider.getUriForFile(context, authority, outFile)

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, filename)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val chooser = Intent.createChooser(intent, "Share $filename").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }.onFailure { it.printStackTrace() }
}
