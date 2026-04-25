package com.example.uniwattelektrik.platform

/**
 * Persistent, platform-specific token store.
 *
 * `actual` implementations:
 *  - androidMain → SharedPreferences / DataStore
 *  - iosMain     → NSUserDefaults (use Keychain for production secrets)
 */
expect class SessionStorage() {
    suspend fun saveToken(token: String)
    suspend fun readToken(): String?
    suspend fun clear()
}

