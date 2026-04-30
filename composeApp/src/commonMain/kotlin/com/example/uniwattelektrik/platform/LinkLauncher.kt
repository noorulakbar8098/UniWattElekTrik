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
}

