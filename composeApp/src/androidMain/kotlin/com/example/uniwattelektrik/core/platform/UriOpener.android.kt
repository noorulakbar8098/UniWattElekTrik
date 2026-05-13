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
                val parsed = Uri.parse(url)
                // tel: URIs go to the dialer (ACTION_DIAL). This is the
                // permission-free path — the user still has to tap "call",
                // which is the right UX for a contact-list call button and
                // means we don't need the runtime CALL_PHONE permission.
                val action = if ((parsed.scheme ?: "").equals("tel", ignoreCase = true)) {
                    Intent.ACTION_DIAL
                } else {
                    Intent.ACTION_VIEW
                }
                val intent = Intent(action, parsed).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            }.onFailure {
                if (it !is ActivityNotFoundException) it.printStackTrace()
            }
        }
    }
}