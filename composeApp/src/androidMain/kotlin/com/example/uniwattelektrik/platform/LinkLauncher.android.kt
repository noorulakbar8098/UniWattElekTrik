package com.example.uniwattelektrik.platform

import android.content.Intent
import android.net.Uri
import com.example.uniwattelektrik.AndroidAppContext
import com.example.uniwattelektrik.core.AppLog
import java.net.URLEncoder

actual class LinkLauncher {
    actual fun openWhatsApp(phoneE164: String, message: String) {
        val encodedText = URLEncoder.encode(message, "UTF-8")
        val cleanPhone = phoneE164.filter { it.isDigit() || it == '+' }.removePrefix("+")
        val url = if (cleanPhone.isNotBlank()) {
            "https://wa.me/$cleanPhone?text=$encodedText"
        } else {
            "https://wa.me/?text=$encodedText"
        }
        AppLog.i("LinkLauncher", "openWhatsApp → $url")
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { AndroidAppContext.application.startActivity(intent) }
            .onFailure { AppLog.w("LinkLauncher", "WhatsApp launch failed: ${it.message}") }
    }
}

