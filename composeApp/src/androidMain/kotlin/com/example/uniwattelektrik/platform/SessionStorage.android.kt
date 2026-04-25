package com.example.uniwattelektrik.platform

import android.content.Context
import com.example.uniwattelektrik.AndroidAppContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Android `actual` backed by `SharedPreferences`.
 * For production secrets prefer `EncryptedSharedPreferences` or the Keystore.
 */
actual class SessionStorage {
    private val prefs by lazy {
        AndroidAppContext.application.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    }

    actual suspend fun saveToken(token: String) = withContext(Dispatchers.IO) {
        prefs.edit().putString(KEY_TOKEN, token).apply()
    }

    actual suspend fun readToken(): String? = withContext(Dispatchers.IO) {
        prefs.getString(KEY_TOKEN, null)
    }

    actual suspend fun clear() = withContext(Dispatchers.IO) {
        prefs.edit().remove(KEY_TOKEN).apply()
    }

    private companion object {
        const val PREFS = "uniwatt_auth_prefs"
        const val KEY_TOKEN = "id_token"
    }
}

