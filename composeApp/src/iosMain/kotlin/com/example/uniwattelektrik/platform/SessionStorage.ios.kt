package com.example.uniwattelektrik.platform

import platform.Foundation.NSUserDefaults

/**
 * iOS `actual` backed by `NSUserDefaults`.
 * For production, swap to the Keychain via `Security.framework` for secret storage.
 */
actual class SessionStorage {
    private val defaults = NSUserDefaults.standardUserDefaults

    actual suspend fun saveToken(token: String) {
        defaults.setObject(token, KEY_TOKEN)
    }

    actual suspend fun readToken(): String? =
        defaults.stringForKey(KEY_TOKEN)

    actual suspend fun clear() {
        defaults.removeObjectForKey(KEY_TOKEN)
    }

    private companion object {
        const val KEY_TOKEN = "id_token"
    }
}

