package com.example.uniwattelektrik.feature.auth.domain.repository

import com.example.uniwattelektrik.core.Resource
import com.example.uniwattelektrik.feature.auth.domain.model.AuthSession
import kotlinx.coroutines.flow.Flow

/**
 * Auth contract — the only thing the domain/presentation layers know about auth.
 * Implementation lives in `data/repository/AuthRepositoryImpl`.
 */
interface AuthRepository {
    /**
     * Signs in with [email] + [password] (Firebase Email/Password).
     *
     * @param asAdmin when true, the implementation will ensure the
     *  `admins/{uid}` directory entry exists (self-healing the case where a
     *  previous sign-up created the Firebase Auth user but the Firestore write
     *  failed). The returned [AuthSession.user] will always have a non-null
     *  `adminId` when this flag is true and Firebase Auth succeeded.
     */
    suspend fun signIn(
        email: String,
        password: String,
        asAdmin: Boolean = false,
    ): Resource<AuthSession>

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

