package com.example.uniwattelektrik.feature.auth.data.remote

/**
 * Directory of admin accounts keyed by their generated short ID.
 *
 * This sits **outside** Firebase Auth (Auth only authenticates by email) and is
 * the bridge that lets users sign in with either an email **or** an Admin ID.
 *
 *  - On sign-up: [register] persists `{adminId, email, uid, fullName}`.
 *  - On sign-in by ID: [resolveEmail] returns the email so we can hand it to
 *    Firebase Auth's `signInWithEmailAndPassword`.
 *  - After sign-in: [lookupAdminIdByUid] enriches the [com.example.uniwattelektrik.feature.auth.domain.model.User]
 *    with its Admin ID so the UI can display it.
 *
 * Implementations: Firestore on Android, no-op on iOS until that target is wired.
 */
interface AdminDirectory {
    suspend fun resolveEmail(adminId: String): String?
    suspend fun lookupAdminIdByUid(uid: String): String?
    suspend fun register(uid: String, adminId: String, email: String, fullName: String?)
}

