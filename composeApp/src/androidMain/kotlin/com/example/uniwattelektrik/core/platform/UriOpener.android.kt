 package com.example.uniwattelektrik.core.platform

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

@Composable
actual fun rememberUriOpener(): (String) -> Unit {
    val context = LocalContext.current
    return remember(context) {
        { url ->
            runCatching {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            }.onFailure {
                if (it !is ActivityNotFoundException) it.printStackTrace()
            }
        }
    }
}