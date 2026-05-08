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

    actual suspend fun saveProfile(
        uid: String,
        email: String,
        displayName: String?,
        adminId: String?,
        parentAdminId: String?,
        mustChangePassword: Boolean,
        permission: String,
    ) = withContext(Dispatchers.IO) {
        prefs.edit()
            .putString(KEY_UID, uid)
            .putString(KEY_EMAIL, email)
            .putString(KEY_NAME, displayName)
            .putString(KEY_ADMIN_ID, adminId)
            .putString(KEY_PARENT_ADMIN_ID, parentAdminId)
            .putBoolean(KEY_MUST_CHANGE_PWD, mustChangePassword)
            .putString(KEY_PERMISSION, permission)
            .apply()
    }

    actual suspend fun readProfile(): StoredProfile? = withContext(Dispatchers.IO) {
        val uid = prefs.getString(KEY_UID, null) ?: return@withContext null
        val email = prefs.getString(KEY_EMAIL, null) ?: return@withContext null
        StoredProfile(
            uid = uid,
            email = email,
            displayName = prefs.getString(KEY_NAME, null),
            adminId = prefs.getString(KEY_ADMIN_ID, null),
            parentAdminId = prefs.getString(KEY_PARENT_ADMIN_ID, null),
            mustChangePassword = prefs.getBoolean(KEY_MUST_CHANGE_PWD, false),
            permission = prefs.getString(KEY_PERMISSION, "") ?: "",
        )
    }

    actual suspend fun clear() = withContext(Dispatchers.IO) {
        prefs.edit()
            .remove(KEY_TOKEN)
            .remove(KEY_UID)
            .remove(KEY_EMAIL)
            .remove(KEY_NAME)
            .remove(KEY_ADMIN_ID)
            .remove(KEY_PARENT_ADMIN_ID)
            .remove(KEY_MUST_CHANGE_PWD)
            .remove(KEY_PERMISSION)
            .apply()
    }

    private companion object {
        const val PREFS = "uniwatt_auth_prefs"
        const val KEY_TOKEN = "id_token"
        const val KEY_UID = "uid"
        const val KEY_EMAIL = "email"
        const val KEY_NAME = "display_name"
        const val KEY_ADMIN_ID = "admin_id"
        const val KEY_PARENT_ADMIN_ID = "parent_admin_id"
        const val KEY_MUST_CHANGE_PWD = "must_change_pwd"
        const val KEY_PERMISSION = "permission"
    }
}
