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

    actual suspend fun saveProfile(
        uid: String,
        email: String,
        displayName: String?,
        adminId: String?,
        parentAdminId: String?,
        mustChangePassword: Boolean,
    ) {
        defaults.setObject(uid, KEY_UID)
        defaults.setObject(email, KEY_EMAIL)
        defaults.setObject(displayName, KEY_NAME)
        defaults.setObject(adminId, KEY_ADMIN_ID)
        defaults.setObject(parentAdminId, KEY_PARENT_ADMIN_ID)
        defaults.setBool(mustChangePassword, KEY_MUST_CHANGE_PWD)
    }

    actual suspend fun readProfile(): StoredProfile? {
        val uid = defaults.stringForKey(KEY_UID) ?: return null
        val email = defaults.stringForKey(KEY_EMAIL) ?: return null
        return StoredProfile(
            uid = uid,
            email = email,
            displayName = defaults.stringForKey(KEY_NAME),
            adminId = defaults.stringForKey(KEY_ADMIN_ID),
            parentAdminId = defaults.stringForKey(KEY_PARENT_ADMIN_ID),
            mustChangePassword = defaults.boolForKey(KEY_MUST_CHANGE_PWD),
        )
    }

    actual suspend fun clear() {
        defaults.removeObjectForKey(KEY_TOKEN)
        defaults.removeObjectForKey(KEY_UID)
        defaults.removeObjectForKey(KEY_EMAIL)
        defaults.removeObjectForKey(KEY_NAME)
        defaults.removeObjectForKey(KEY_ADMIN_ID)
        defaults.removeObjectForKey(KEY_PARENT_ADMIN_ID)
        defaults.removeObjectForKey(KEY_MUST_CHANGE_PWD)
    }

    private companion object {
        const val KEY_TOKEN = "id_token"
        const val KEY_UID = "uid"
        const val KEY_EMAIL = "email"
        const val KEY_NAME = "display_name"
        const val KEY_ADMIN_ID = "admin_id"
        const val KEY_PARENT_ADMIN_ID = "parent_admin_id"
        const val KEY_MUST_CHANGE_PWD = "must_change_pwd"
    }
}
