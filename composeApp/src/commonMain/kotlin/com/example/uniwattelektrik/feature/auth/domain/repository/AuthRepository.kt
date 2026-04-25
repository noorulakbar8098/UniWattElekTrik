package com.example.uniwattelektrik.feature.auth.domain.repository

import com.example.uniwattelektrik.core.Resource
import com.example.uniwattelektrik.feature.auth.domain.model.AuthSession
import kotlinx.coroutines.flow.Flow

/**
 * Auth contract — the only thing the domain/presentation layers know about auth.
 * Implementation lives in `data/repository/AuthRepositoryImpl`.
 */
interface AuthRepository {
    /** Signs in with [email] + [password] (Firebase Email/Password). Returns the new session. */
    suspend fun signIn(email: String, password: String): Resource<AuthSession>

    /** Creates a new account and returns the new session. */
    suspend fun signUp(
        email: String,
        password: String,
        displayName: String? = null,
    ): Resource<AuthSession>

    /** Hot stream: emits the current session or `null` when signed out. */
    fun observeSession(): Flow<AuthSession?>

    /** Clears the persisted session. */
    suspend fun logout()
}

