package com.example.uniwattelektrik.feature.auth.data.remote

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

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

    override suspend fun resolveEmail(adminId: String): String? {
        val snap = firestore.collection(COL_IDS).document(adminId.uppercase()).get().await()
        return snap.getString("email")
    }

    override suspend fun lookupAdminIdByUid(uid: String): String? {
        val snap = firestore.collection(COL_ADMINS).document(uid).get().await()
        return snap.getString("adminId")
    }

    override suspend fun register(
        uid: String,
        adminId: String,
        email: String,
        fullName: String?,
    ) {
        val id = adminId.uppercase()
        // 1. Quick-lookup record (used by anonymous reads on the login screen)
        firestore.collection(COL_IDS).document(id).set(
            mapOf("email" to email, "uid" to uid),
        ).await()
        // 2. Full admin record (used after sign-in to enrich the User)
        firestore.collection(COL_ADMINS).document(uid).set(
            mapOf(
                "adminId" to id,
                "email" to email,
                "fullName" to (fullName ?: ""),
            ),
        ).await()
    }

    private companion object {
        const val COL_IDS = "admin_ids"
        const val COL_ADMINS = "admins"
    }
}

