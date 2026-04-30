package com.example.uniwattelektrik.feature.auth.data.remote

import com.example.uniwattelektrik.core.AppLog
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout

/**
 * Hard cap on a single Firestore round-trip.
 *
 * The Firestore SDK retries forever on transient errors (NOT_FOUND when the
 * database isn't provisioned, PERMISSION_DENIED with rules misconfig, no
 * network, etc.) — without a timeout the whole sign-up flow hangs and the
 * UI is stuck on a spinner. 15 s is generous for a single set/get.
 */
private const val FIRESTORE_TIMEOUT_MS = 15_000L

private suspend fun <T> bounded(block: suspend () -> T): T = try {
    withTimeout(FIRESTORE_TIMEOUT_MS) { block() }
} catch (e: TimeoutCancellationException) {
    throw IllegalStateException(
        "Firestore did not respond in ${FIRESTORE_TIMEOUT_MS / 1000}s. " +
            "Verify the database is provisioned in the Firebase Console " +
            "(Console → Firestore → Create database) and that security rules " +
            "permit this operation.",
        e,
    )
}

/**
 * Firestore-backed admin directory.
 *
 * Layout:
 *   /admin_ids/{ADM-XXXX}  → { email, uid }      (lookup table for ID-based login)
 *   /admins/{uid}          → { adminId, email, fullName }   (full record)
 *
 * Security rules (set in Firebase Console → Firestore → Rules):
 *
 *   match /admin_ids/{id} {
 *     allow read: if true;                       // needed *before* sign-in
 *     allow write: if request.auth != null;      // only signed-in users can register
 *   }
 *   match /admins/{uid} {
 *     allow read, write: if request.auth.uid == uid;
 *   }
 */
class FirestoreAdminDirectory(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
) : AdminDirectory {

    override suspend fun resolveEmail(adminId: String): String? = bounded {
        firestore.collection(COL_IDS).document(adminId.uppercase()).get().await()
            .getString("email")
    }

    override suspend fun lookupAdminIdByUid(uid: String): String? = bounded {
        firestore.collection(COL_ADMINS).document(uid).get().await()
            .getString("adminId")
    }

    override suspend fun register(
        uid: String,
        adminId: String,
        email: String,
        fullName: String?,
    ) {
        val id = adminId.uppercase()
        AppLog.i("AdminDir", "register START uid=$uid adminId=$id email=$email")
        // 1. Quick-lookup record (used by anonymous reads on the login screen)
        AppLog.d("PATH", "write admin_ids/$id")
        bounded {
            firestore.collection(COL_IDS).document(id).set(mapOf(
                    "email" to email,
                    "uid" to uid,
                    "createdAt" to com.google.firebase.firestore.FieldValue.serverTimestamp(),
                ),
            ).await()
        }
        AppLog.i("AdminDir", "  ↳ admin_ids/$id ✅")

        // 2. Full admin record — matches the canonical schema:
        //    { name, email, role:"admin", createdAt: timestamp }  (+ app extras)
        AppLog.d("PATH", "write admins/$uid")
        bounded {
            firestore.collection(COL_ADMINS).document(uid).set(
                mapOf(
                    "name"      to (fullName ?: ""),
                    "email"     to email,
                    "role"      to "admin",
                    "createdAt" to com.google.firebase.firestore.FieldValue.serverTimestamp(),
                    // app-specific extras kept for in-app convenience:
                    "adminId"   to id,
                ),
            ).await()
        }
        AppLog.i("AdminDir", "  ↳ admins/$uid ✅ register DONE")
    }

    private companion object {
        const val COL_IDS = "admin_ids"
        const val COL_ADMINS = "admins"
    }
}

