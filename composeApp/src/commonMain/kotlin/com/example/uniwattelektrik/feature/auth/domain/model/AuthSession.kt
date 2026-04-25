package com.example.uniwattelektrik.feature.auth.domain.model

/** Represents a persisted auth session (just an id token for now). */
data class AuthSession(
    val token: String,
    val user: User,
)

