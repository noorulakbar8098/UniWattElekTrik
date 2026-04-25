package com.example.uniwattelektrik.feature.auth.data.remote

import com.example.uniwattelektrik.core.AppError
import com.example.uniwattelektrik.core.Resource

/**
 * Placeholder until Firebase iOS SDK is wired in. Always returns a clear error so
 * an unconfigured iOS build fails loudly rather than silently faking sign-in.
 */
private class UnconfiguredEmailAuthClient : EmailAuthClient {
    override suspend fun signIn(email: String, password: String): Resource<EmailAuthResult> =
        Resource.failure(AppError.Unknown("Firebase Auth is not yet configured for iOS."))

    override suspend fun signUp(
        email: String,
        password: String,
        displayName: String?,
    ): Resource<EmailAuthResult> =
        Resource.failure(AppError.Unknown("Firebase Auth is not yet configured for iOS."))
}

actual fun provideEmailAuthClient(): EmailAuthClient = UnconfiguredEmailAuthClient()

