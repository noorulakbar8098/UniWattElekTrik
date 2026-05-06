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
    for (ch in this@encodeForUri) {
        if (ch.isLetterOrDigit() || ch == '-' || ch == '.' || ch == '_' || ch == '~') {
            append(ch)
        } else if (ch == ' ') {
            append('+')
        } else {
            for (b in ch.toString().toByteArray(Charsets.UTF_8)) {
                append('%')
                append("%02X".format(b.toInt() and 0xFF))
            }
        }
    }
}

