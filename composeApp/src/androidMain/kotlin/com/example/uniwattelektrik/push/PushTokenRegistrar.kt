package com.example.uniwattelektrik.push

import com.example.uniwattelektrik.core.AppLog
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.tasks.await

/**
 * Persists this device's FCM token under the **correct profile document** for
 * the currently signed-in user:
 *
 *   • Admin    → `admins/{uid}.fcmTokens` (array)
 *   • Employee → `users/{uid}.fcmTokens`  (array)
 *
 * Cloud Functions read these arrays and fan out pushes via FCM Admin SDK.
 *
 * Stores a parallel metadata map (`fcmTokenMeta.<token>`) holding `platform`
 * and `updatedAt` so we can clean up stale tokens later.
 */
object PushTokenRegistrar {

    private const val TAG = "PushTokenRegistrar"
    private const val PLATFORM = "android"

    /** Call once after a successful sign-in. Safe to call repeatedly. */
    suspend fun registerCurrentDevice() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: run {
            AppLog.w(TAG, "registerCurrentDevice: no signed-in user")
            return
        }
        val token = runCatching { FirebaseMessaging.getInstance().token.await() }
            .onFailure { AppLog.w(TAG, "FCM token fetch failed", it) }
            .getOrNull() ?: return
        upsert(uid, token)
    }

    /** Called from [UniWattMessagingService] when FCM rotates the token. */
    suspend fun onTokenRefreshed(token: String) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        upsert(uid, token)
    }

    /**
     * Removes this device's token from Firestore **and** deletes it from FCM
     * so the device will get a brand-new token on next sign-in. Call this
     * before `FirebaseAuth.signOut()`.
     */
    suspend fun unregisterCurrentDevice() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val token = runCatching { FirebaseMessaging.getInstance().token.await() }.getOrNull()
        if (token != null) {
            removeFromBothProfiles(uid, token)
        }
        runCatching { FirebaseMessaging.getInstance().deleteToken().await() }
            .onFailure { AppLog.w(TAG, "deleteToken failed", it) }
    }

    // ── internals ──────────────────────────────────────────────────────────

    /**
     * Writes the token into whichever profile doc(s) actually exist for this
     * uid. We can't always know the role at this point (token rotation may
     * fire at any time), so we probe both `admins/{uid}` and `users/{uid}`
     * and merge into the one(s) present.
     */
    private suspend fun upsert(uid: String, token: String) {
        val db = FirebaseFirestore.getInstance()

        val adminRef = db.collection("admins").document(uid)
        val userRef  = db.collection("users").document(uid)

        val payload = mapOf(
            "fcmTokens" to FieldValue.arrayUnion(token),
            "fcmTokenMeta" to mapOf(
                token to mapOf(
                    "platform"  to PLATFORM,
                    "updatedAt" to FieldValue.serverTimestamp(),
                ),
            ),
        )

        // admins/{uid}
        runCatching {
            val snap = adminRef.get().await()
            if (snap.exists()) {
                adminRef.set(payload, SetOptions.merge()).await()
                AppLog.i(TAG, "FCM token written → admins/$uid")
            }
        }.onFailure { AppLog.w(TAG, "admins/$uid update failed", it) }

        // users/{uid}
        runCatching {
            val snap = userRef.get().await()
            if (snap.exists()) {
                userRef.set(payload, SetOptions.merge()).await()
                AppLog.i(TAG, "FCM token written → users/$uid")
            }
        }.onFailure { AppLog.w(TAG, "users/$uid update failed", it) }
    }

    private suspend fun removeFromBothProfiles(uid: String, token: String) {
        val db = FirebaseFirestore.getInstance()
        val update = mapOf(
            "fcmTokens" to FieldValue.arrayRemove(token),
            "fcmTokenMeta.$token" to FieldValue.delete(),
        )
        runCatching { db.collection("admins").document(uid).set(update, SetOptions.merge()).await() }
        runCatching { db.collection("users").document(uid).set(update, SetOptions.merge()).await() }
    }
}

