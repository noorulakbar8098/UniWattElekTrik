package com.example.uniwattelektrik.feature.auth.data.remote

/**
 * Looks up an employee record across **all** admins via Firestore
 * `collectionGroup("users")` so that when an employee signs in we can resolve
 * which admin owns them and whether they still need to rotate their initial
 * temporary password.
 *
 * Returns `null` when the signed-in uid does not appear under any admin
 * (e.g. the account was created outside the admin flow, or this is an
 * admin's own account).
 */
interface EmployeeLocator {
    suspend fun findByUid(uid: String): EmployeeAccount?
}

/** Subset of the employee Firestore doc that the auth layer cares about. */
data class EmployeeAccount(
    val uid: String,
    /** UID of the owning admin (= document ID at `admins/{parentAdminId}`). */
    val parentAdminId: String,
    val mustChangePassword: Boolean,
)

