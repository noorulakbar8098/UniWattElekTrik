package com.example.uniwattelektrik.feature.auth.domain.model

/**
 * Domain model for an authenticated user. Pure Kotlin, framework-agnostic.
 */
data class User(
    val id: String,
    val email: String,
    val displayName: String? = null,
    /** Auto-generated short identifier for admins (e.g. "ADM-7421"). Null for regular users. */
    val adminId: String? = null,
    /**
     * For employee accounts (created by an admin) this is the **Firebase UID of
     * the owning admin** (= document ID at `admins/{parentAdminId}`). Null for
     * admin accounts (admins use [adminId] instead).
     */
    val parentAdminId: String? = null,
    /**
     * `true` when the admin just created this employee and the user has not yet
     * rotated the temporary password. The login screen's "Create New Password"
     * button only succeeds while this flag is true.
     */
    val mustChangePassword: Boolean = false,
)
