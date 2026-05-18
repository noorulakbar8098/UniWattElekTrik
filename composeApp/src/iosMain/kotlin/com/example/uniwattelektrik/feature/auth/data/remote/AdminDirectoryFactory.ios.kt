package com.example.uniwattelektrik.feature.auth.data.remote

import com.example.uniwattelektrik.core.AppLog
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.firestore.FieldValue
import dev.gitlive.firebase.firestore.firestore
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout

/**
 * iOS implementation of [AdminDirectory], backed by the GitLive Firestore
 * KMP wrapper around the Firebase Apple SDK.
 *
 * Mirrors the Android `FirestoreAdminDirectory` 1:1 — same collection names,
 * same fields, same bounded-timeout safety net.
 *
 * Pre-requisites in the iosApp Xcode project (one-time setup):
 *   1. Swift Package Manager → add the **FirebaseFirestore** product from
 *      https://github.com/firebase/firebase-ios-sdk (in addition to FirebaseAuth).
 *   2. `GoogleService-Info.plist` must already be in `iosApp/iosApp/`.
 *   3. `FirebaseApp.configure()` must be called in `iOSApp.swift` (already wired).
 */
private const val FIRESTORE_TIMEOUT_MS = 15_000L

private suspend fun <T> bounded(block: suspend () -> T): T = try {
    withTimeout(FIRESTORE_TIMEOUT_MS) { block() }
} catch (e: TimeoutCancellationException) {
    throw IllegalStateException(
        "Firestore did not respond in ${FIRESTORE_TIMEOUT_MS / 1000}s on iOS. " +
            "Verify GoogleService-Info.plist is in the bundle and the database " +
            "is provisioned in the Firebase Console.",
        e,
    )
}

private class FirestoreAdminDirectoryIos : AdminDirectory {

    private val firestore = Firebase.firestore

    override suspend fun resolveEmail(adminId: String): String? = bounded {
        val snap = firestore.collection(COL_IDS).document(adminId.uppercase()).get()
        if (!snap.exists) null else snap.get<String?>("email")
    }

    override suspend fun lookupAdminIdByUid(uid: String): String? = bounded {
        val snap = firestore.collection(COL_ADMINS).document(uid).get()
        if (!snap.exists) null else snap.get<String?>("adminId")
    }

    override suspend fun register(
        uid: String,
        adminId: String,
        email: String,
        fullName: String?,
    ) {
        val id = adminId.uppercase()
        AppLog.i("AdminDir-iOS", "register START uid=$uid adminId=$id email=$email")

        // 1. Quick-lookup record (used by anonymous reads on the login screen)
        bounded {
            firestore.collection(COL_IDS).document(id).set(
                mapOf(
                    "email"     to email,
                    "uid"       to uid,
                    "createdAt" to FieldValue.serverTimestamp,
                ),
            )
        }
        AppLog.i("AdminDir-iOS", "  ↳ admin_ids/$id ✅")

        // 2. Full admin record (matches canonical schema used on Android).
        bounded {
            firestore.collection(COL_ADMINS).document(uid).set(
                mapOf(
                    "name"      to (fullName ?: ""),
                    "email"     to email,
                    "role"      to "admin",
                    "createdAt" to FieldValue.serverTimestamp,
                    "adminId"   to id,
                ),
            )
        }
        AppLog.i("AdminDir-iOS", "  ↳ admins/$uid ✅ register DONE")
    }

    private companion object {
        const val COL_IDS    = "admin_ids"
        const val COL_ADMINS = "admins"
    }
}

actual object AdminDirectoryFactory {
    actual fun create(): AdminDirectory = FirestoreAdminDirectoryIos()
}

