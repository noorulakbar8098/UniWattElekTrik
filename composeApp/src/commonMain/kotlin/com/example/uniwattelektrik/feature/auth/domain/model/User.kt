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
)
