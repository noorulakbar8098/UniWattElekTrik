package com.example.uniwattelektrik.platform

/**
 * Persistent, platform-specific session store.
 *
 * Persists both the auth token AND the resolved user profile (uid, email,
 * displayName, adminId) so the app can restore the **correct shell** after a
 * cold start without making a Firestore round-trip on the splash screen.
 *
 * `actual` implementations:
 *  - androidMain → SharedPreferences / DataStore
 *  - iosMain     → NSUserDefaults (use Keychain for production secrets)
 */
expect class SessionStorage() {
    suspend fun saveToken(token: String)
    suspend fun readToken(): String?

    /** Persist the resolved profile so role-routing survives cold start. */
    suspend fun saveProfile(
        uid: String,
        email: String,
        displayName: String?,
        adminId: String?,
        parentAdminId: String? = null,
        mustChangePassword: Boolean = false,
    )

    /** Returns null if no profile was ever saved (e.g. fresh install). */
    suspend fun readProfile(): StoredProfile?

    suspend fun clear()
}

data class StoredProfile(
    val uid: String,
    val email: String,
    val displayName: String?,
    val adminId: String?,
    val parentAdminId: String? = null,
    val mustChangePassword: Boolean = false,
)
