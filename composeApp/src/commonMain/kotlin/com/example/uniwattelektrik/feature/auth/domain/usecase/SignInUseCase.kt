package com.example.uniwattelektrik.feature.auth.domain.usecase

import com.example.uniwattelektrik.core.AppError
import com.example.uniwattelektrik.core.Resource
import com.example.uniwattelektrik.feature.auth.domain.model.AuthSession
import com.example.uniwattelektrik.feature.auth.domain.repository.AuthRepository

/**
 * Signs the user in with **email** + **password** (Firebase Email/Password).
 */
class SignInUseCase(private val repository: AuthRepository) {

    suspend operator fun invoke(
        email: String,
        password: String,
        asAdmin: Boolean = false,
    ): Resource<AuthSession> {
        val normalized = email.trim()
        if (!isValidEmail(normalized)) return Resource.failure(AppError.InvalidEmail)
        if (password.length < MIN_PASSWORD) return Resource.failure(AppError.InvalidPassword)
        return repository.signIn(normalized, password, asAdmin = asAdmin)
    }

    /** Lightweight email check — good enough at the UI boundary; backend has the final word. */
    private fun isValidEmail(value: String): Boolean {
        val at = value.indexOf('@')
        val dot = value.lastIndexOf('.')
        return at in 1..value.length - 3 && dot > at + 1 && dot < value.length - 1
    }

    companion object {
        const val MIN_PASSWORD = 6
    }
}

