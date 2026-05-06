package com.example.uniwattelektrik.core.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import platform.Foundation.NSURL
import platform.UIKit.UIApplication

@Composable
actual fun rememberUriOpener(): (String) -> Unit = remember {
    { url ->
        // iOS forwards `geo:` URIs to Apple Maps automatically when no other
        // handler claims the scheme. `UIApplication.openURL:` is deprecated but
        // still functional; switching to the completion-handler variant requires
        // an additional `@OptIn(ExperimentalForeignApi)` boundary.
        val ns = NSURL.URLWithString(url) ?: return@remember
        @Suppress("DEPRECATION")
        UIApplication.sharedApplication.openURL(ns)
    }
}

