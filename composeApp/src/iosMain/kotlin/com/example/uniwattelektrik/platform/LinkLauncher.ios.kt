package com.example.uniwattelektrik.platform

import platform.Foundation.NSCharacterSet
import platform.Foundation.NSString
import platform.Foundation.NSURL
import platform.Foundation.URLQueryAllowedCharacterSet
import platform.Foundation.create
import platform.Foundation.stringByAddingPercentEncodingWithAllowedCharacters
import platform.UIKit.UIApplication
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
actual class LinkLauncher {
    actual fun openWhatsApp(phoneE164: String, message: String) {
        val allowed: NSCharacterSet = NSCharacterSet.URLQueryAllowedCharacterSet
        val msgNs = NSString.create(string = message)
        val encoded = msgNs.stringByAddingPercentEncodingWithAllowedCharacters(allowed) ?: message
        val cleanPhone = phoneE164.filter { it.isDigit() || it == '+' }.removePrefix("+")
        val urlStr = if (cleanPhone.isNotBlank()) {
            "https://wa.me/$cleanPhone?text=$encoded"
        } else {
            "https://wa.me/?text=$encoded"
        }
        val url = NSURL.URLWithString(urlStr) ?: return
        UIApplication.sharedApplication.openURL(url)
    }

    actual fun openMap(lat: Double, lng: Double, label: String) {
        // Prefer Apple Maps via the `https://maps.apple.com/?ll=…` deep link —
        // iOS routes that URL straight to the native Maps app. If the user
        // doesn't have Apple Maps (rare; it's pre-installed) we fall back to
        // the Google Maps web URL which any iOS browser can open.
        val allowed: NSCharacterSet = NSCharacterSet.URLQueryAllowedCharacterSet
        val labelEncoded = if (label.isNotBlank()) {
            NSString.create(string = label)
                .stringByAddingPercentEncodingWithAllowedCharacters(allowed) ?: ""
        } else ""

        val appleMaps = if (labelEncoded.isNotBlank()) {
            "https://maps.apple.com/?ll=$lat,$lng&q=$labelEncoded"
        } else {
            "https://maps.apple.com/?ll=$lat,$lng&q=$lat,$lng"
        }
        val primary = NSURL.URLWithString(appleMaps)
        if (primary != null && UIApplication.sharedApplication.canOpenURL(primary)) {
            UIApplication.sharedApplication.openURL(primary)
            return
        }
        val fallback = NSURL.URLWithString(
            "https://www.google.com/maps/search/?api=1&query=$lat,$lng",
        ) ?: return
        UIApplication.sharedApplication.openURL(fallback)
    }
}



