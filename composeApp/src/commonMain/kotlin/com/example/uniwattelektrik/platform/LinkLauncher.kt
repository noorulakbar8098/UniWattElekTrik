package com.example.uniwattelektrik.platform

/**
 * Cross-platform deep-link launcher.
 *
 * The admin app uses [openWhatsApp] to share an employee's freshly-issued
 * email + temporary password through the operator's existing WhatsApp account.
 *
 *  - Android → `Intent(ACTION_VIEW, Uri.parse("https://wa.me/<phone>?text=…"))`
 *  - iOS     → `UIApplication.openURL` on the same `wa.me` URL
 *
 * If [phoneE164] is blank, both platforms fall back to a chooser
 * (`https://wa.me/?text=…`) so the admin can pick any chat.
 */
expect class LinkLauncher() {
    fun openWhatsApp(phoneE164: String, message: String)

    /**
     * Opens the platform's preferred maps app pinned at [lat]/[lng]. The
     * [label] is used as the pin/title where supported (Google Maps q=, Apple
     * Maps q=). Android prefers `geo:` (Google Maps, Waze, etc. — user picks
     * via chooser) and falls back to the Google Maps web URL. iOS prefers
     * the `maps://` scheme (Apple Maps) and falls back to a web URL.
     */
    fun openMap(lat: Double, lng: Double, label: String = "")
}

