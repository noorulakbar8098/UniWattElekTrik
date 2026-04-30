package com.example.uniwattelektrik.feature.auth.data.remote

import com.example.uniwattelektrik.core.Resource

/**
 * Email/password auth provider contract.
 *
 * Keeping this as an **interface** (instead of `expect class`) means:
 *  - `commonMain` stays free of Firebase imports,
 *  - you can swap Firebase for any other backend (REST, custom JWT) without
 *    changing the ViewModel / UseCase / UI,
 *  - tests can inject a `FakeEmailAuthClient` trivially.
 */
interface EmailAuthClient {
    /** Signs in with [email] + [password]. Returns the user's id token + uid on success. */
    suspend fun signIn(email: String, password: String): Resource<EmailAuthResult>

    /** Creates a new account with [email] + [password] and returns a fresh session. */
    suspend fun signUp(
        email: String,
        password: String,
        displayName: String? = null,
    ): Resource<EmailAuthResult>
}

/** Minimal credential payload returned by the auth backend. */
data class EmailAuthResult(
    val uid: String,
    val email: String,
    val idToken: String,
    val displayName: String? = null,
)

