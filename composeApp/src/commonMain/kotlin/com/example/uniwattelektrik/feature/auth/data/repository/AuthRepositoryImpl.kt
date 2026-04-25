package com.example.uniwattelektrik.feature.auth.data.repository

import com.example.uniwattelektrik.core.Resource
import com.example.uniwattelektrik.feature.auth.data.remote.EmailAuthClient
import com.example.uniwattelektrik.feature.auth.domain.model.AuthSession
import com.example.uniwattelektrik.feature.auth.domain.model.User
import com.example.uniwattelektrik.feature.auth.domain.repository.AuthRepository
import com.example.uniwattelektrik.platform.SessionStorage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Repository implementation for email/password auth.
 */
class AuthRepositoryImpl(
    private val emailAuthClient: EmailAuthClient,
    private val sessionStorage: SessionStorage,
) : AuthRepository {

    private val sessionFlow = MutableStateFlow<AuthSession?>(null)

    suspend fun bootstrap() {
        val token = sessionStorage.readToken() ?: return
        sessionFlow.value = AuthSession(
            token = token,
            user = User(id = token.take(8), email = "unknown"),
        )
    }

    override suspend fun signIn(email: String, password: String): Resource<AuthSession> =
        toSession(emailAuthClient.signIn(email, password))

    override suspend fun signUp(
        email: String,
        password: String,
        displayName: String?,
    ): Resource<AuthSession> =
        toSession(emailAuthClient.signUp(email, password, displayName))

    private suspend fun toSession(result: Resource<com.example.uniwattelektrik.feature.auth.data.remote.EmailAuthResult>): Resource<AuthSession> {
        return when (result) {
            is Resource.Success -> {
                val data = result.data
                sessionStorage.saveToken(data.idToken)
                val session = AuthSession(
                    token = data.idToken,
                    user = User(
                        id = data.uid,
                        email = data.email,
                        displayName = data.displayName,
                    ),
                )
                sessionFlow.value = session
                Resource.success(session)
            }
            is Resource.Failure -> result
        }
    }

    override fun observeSession(): Flow<AuthSession?> = sessionFlow.asStateFlow()

    override suspend fun logout() {
        sessionStorage.clear()
        sessionFlow.value = null
    }
}
