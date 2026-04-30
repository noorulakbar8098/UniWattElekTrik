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
}



