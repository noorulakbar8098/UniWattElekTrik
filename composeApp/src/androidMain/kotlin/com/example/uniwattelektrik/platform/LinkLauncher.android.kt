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

    actual fun openMap(lat: Double, lng: Double, label: String) {
        // Build a `geo:` URI with an explicit `q=` query so any installed maps
        // app (Google Maps, Waze, OsmAnd…) can resolve it. The lat,lng prefix
        // is required by the geo spec so the chooser can pre-center.
        val encodedLabel = if (label.isNotBlank()) URLEncoder.encode(label, "UTF-8") else ""
        val geoUri = if (encodedLabel.isNotBlank()) {
            "geo:$lat,$lng?q=$lat,$lng($encodedLabel)"
        } else {
            "geo:$lat,$lng?q=$lat,$lng"
        }
        val ctx = AndroidAppContext.application
        AppLog.i("LinkLauncher", "openMap → $geoUri")
        val geoIntent = Intent(Intent.ACTION_VIEW, Uri.parse(geoUri)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val launched = runCatching { ctx.startActivity(geoIntent) }.isSuccess
        if (launched) return

        // Fallback: Google Maps web URL — works on every Android device with a
        // browser even when no maps app is installed.
        val webUrl = "https://www.google.com/maps/search/?api=1&query=$lat,$lng"
        AppLog.w("LinkLauncher", "openMap geo: failed, falling back to web → $webUrl")
        val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse(webUrl)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { ctx.startActivity(webIntent) }
            .onFailure { AppLog.w("LinkLauncher", "openMap web fallback failed: ${it.message}") }
    }
}

