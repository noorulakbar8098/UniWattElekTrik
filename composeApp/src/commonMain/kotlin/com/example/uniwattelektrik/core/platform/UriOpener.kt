package com.example.uniwattelektrik.core.platform

import androidx.compose.runtime.Composable

/**
 * Returns a callable that opens an arbitrary URL/URI in the OS-level default
 * handler app (browser, Maps app, etc).
 *
 * Implemented as a `@Composable` factory rather than a global object so each
 * platform can capture the appropriate context (e.g. Android's LocalContext)
 * the same way [rememberAttachmentLauncher] does — keeps the API consistent
 * and avoids static singletons holding onto Activity references.
 *
 * Failures (no handler installed, malformed URI) are swallowed silently —
 * callers should not rely on side effects.
 */
@Composable
expect fun rememberUriOpener(): (String) -> Unit

/**
 * Build a `tel:` URI suitable for [rememberUriOpener] that opens the OS
 * phone dialer prefilled with [phone]. The dialer launches but no call is
 * placed until the user taps the call button — keeps the integration
 * permission-free on Android (no CALL_PHONE) and friction-free on iOS.
 */
fun buildTelUri(phone: String): String {
    // Strip everything except digits, '+', '*' and '#' which are the chars
    // RFC 3966 / E.164 allow in a telephone-subscriber URI.
    val cleaned = phone.filter { it.isDigit() || it == '+' || it == '*' || it == '#' }
    return "tel:$cleaned"
}

/**
 * Build a `geo:` URI suitable for [rememberUriOpener] that opens the OS map app.
 */
fun buildMapsUri(
    latitude: Double?,
    longitude: Double?,
    label: String,
): String {
    val encoded = label.encodeForUri()
    return if (latitude != null && longitude != null) {
        // 'q=lat,lng(label)' is the cross-app contract honoured by Google Maps,
        // OsmAnd, Maps.me etc. on Android, and CLLocationCoordinate2D on iOS.
        "geo:$latitude,$longitude?q=$latitude,$longitude($encoded)"
    } else {
        "geo:0,0?q=$encoded"
    }
}

/** Minimal, dependency-free percent-encoder for query strings. */
private fun String.encodeForUri(): String = buildString(length) {
    val hex = "0123456789ABCDEF"
    for (ch in this@encodeForUri) {
        if (ch.isLetterOrDigit() || ch == '-' || ch == '.' || ch == '_' || ch == '~') {
            append(ch)
        } else if (ch == ' ') {
            append('+')
        } else {
            // `encodeToByteArray()` is in the common stdlib (UTF-8); JVM-only
            // `String.format` / `Charsets` are unavailable in Kotlin/Native,
            // so we hex-encode by hand to keep the helper KMP-portable.
            for (b in ch.toString().encodeToByteArray()) {
                val v = b.toInt() and 0xFF
                append('%')
                append(hex[v ushr 4])
                append(hex[v and 0x0F])
            }
        }
    }
}

