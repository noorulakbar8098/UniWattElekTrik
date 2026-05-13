package com.example.uniwattelektrik.core.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import platform.Foundation.NSURL
import platform.UIKit.UIApplication

@Composable
actual fun rememberUriOpener(): (String) -> Unit = remember {
    open@{ url: String ->
        // iOS forwards `geo:` URIs to Apple Maps automatically when no other
        // handler claims the scheme. `UIApplication.openURL:` is deprecated but
        // still functional; switching to the completion-handler variant requires
        // an additional `@OptIn(ExperimentalForeignApi)` boundary.
        //
        // NOTE: the early-return label must point at *this* lambda — not at
        // the enclosing `remember { }` block, which has already returned by
        // the time this callable is invoked. Using `return@remember` here
        // crashes at runtime with a non-local-return error.
        val ns = NSURL.URLWithString(url) ?: return@open
        @Suppress("DEPRECATION")
        UIApplication.sharedApplication.openURL(ns)
    }
}

