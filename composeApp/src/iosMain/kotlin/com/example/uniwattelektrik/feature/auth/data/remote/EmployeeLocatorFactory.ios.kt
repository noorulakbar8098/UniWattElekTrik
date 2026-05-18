package com.example.uniwattelektrik.feature.auth.data.remote

import com.example.uniwattelektrik.core.AppLog
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.firestore.firestore

/**
 * iOS implementation of [EmployeeLocator], backed by the GitLive Firestore
 * wrapper. Mirrors the Android `FirestoreEmployeeLocator` — reads from the
 * `employee_map/{uid}` lookup doc + the `users/{uid}` doc for permission.
 */
private class FirestoreEmployeeLocatorIos : EmployeeLocator {

    private val firestore = Firebase.firestore

    override suspend fun findByUid(uid: String): EmployeeAccount? = try {
        AppLog.d("EmpLocator-iOS", "employee_map/$uid lookup")
        val doc = firestore.collection("employee_map").document(uid).get()
        if (!doc.exists) {
            AppLog.d("EmpLocator-iOS", "  ↳ no employee_map doc for uid=$uid")
            null
        } else {
            val adminId = doc.get<String?>("adminId")
            if (adminId.isNullOrBlank()) {
                AppLog.w("EmpLocator-iOS", "  ↳ employee_map/$uid missing adminId")
                null
            } else {
                val mustChange = runCatching { doc.get<Boolean?>("mustChangePassword") }
                    .getOrNull() ?: false
                val permission = runCatching {
                    val userDoc = firestore.collection("users").document(uid).get()
                    if (userDoc.exists) userDoc.get<String?>("permission") ?: "" else ""
                }.getOrDefault("")
                AppLog.i("EmpLocator-iOS", "  ↳ uid=$uid adminId=$adminId mustChange=$mustChange perm=$permission")
                EmployeeAccount(uid, adminId, mustChange, permission)
            }
        }
    } catch (e: Throwable) {
        AppLog.w("EmpLocator-iOS", "findByUid failed: ${e.message}")
        null
    }
}

actual object EmployeeLocatorFactory {
    actual fun create(): EmployeeLocator = FirestoreEmployeeLocatorIos()
}

