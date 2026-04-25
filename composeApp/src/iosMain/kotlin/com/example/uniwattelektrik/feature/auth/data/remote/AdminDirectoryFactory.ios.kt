package com.example.uniwattelektrik.feature.auth.data.remote

/**
 * Placeholder until the Firebase iOS SDK is wired in. Returns an in-memory
 * stub so the iOS build keeps working; admin-ID login will simply fail with
 * a clear "not configured" message until a real implementation is provided.
 */
private class UnconfiguredAdminDirectory : AdminDirectory {
    override suspend fun resolveEmail(adminId: String): String? = null
    override suspend fun lookupAdminIdByUid(uid: String): String? = null
    override suspend fun register(
        uid: String,
        adminId: String,
        email: String,
        fullName: String?,
    ) {
        // no-op: iOS Firestore wiring is pending.
    }
}

actual object AdminDirectoryFactory {
    actual fun create(): AdminDirectory = UnconfiguredAdminDirectory()
}

