package com.example.uniwattelektrik.feature.auth.data.remote

import com.example.uniwattelektrik.core.AppLog
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

internal class FirestoreEmployeeLocator(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
) : EmployeeLocator {

    override suspend fun findByUid(uid: String): EmployeeAccount? {
        return try {
            AppLog.d("EmpLocator", "employee_map/$uid lookup")
            val doc = firestore.collection("employee_map").document(uid).get().await()
            if (!doc.exists()) {
                AppLog.d("EmpLocator", "  ↳ no employee_map doc for uid=$uid")
                return null
            }
            val adminId = doc.getString("adminId")
            if (adminId.isNullOrBlank()) {
                AppLog.w("EmpLocator", "  ↳ employee_map/$uid missing adminId field")
                return null
            }
            val mustChange = doc.getBoolean("mustChangePassword") ?: false
            AppLog.i("EmpLocator", "  ↳ uid=$uid adminId=$adminId mustChangePassword=$mustChange")
            EmployeeAccount(uid, adminId, mustChange)
        } catch (e: Throwable) {
            AppLog.w("EmpLocator", "findByUid failed: ${e.message}")
            null
        }
    }
}

actual object EmployeeLocatorFactory {
    actual fun create(): EmployeeLocator = FirestoreEmployeeLocator()
}
