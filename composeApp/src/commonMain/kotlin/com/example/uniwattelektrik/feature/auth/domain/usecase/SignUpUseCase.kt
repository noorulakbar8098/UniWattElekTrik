package com.example.uniwattelektrik.feature.auth.domain.usecase

import com.example.uniwattelektrik.core.AppError
import com.example.uniwattelektrik.core.Resource
import com.example.uniwattelektrik.feature.auth.domain.model.AuthSession
import com.example.uniwattelektrik.feature.auth.domain.repository.AuthRepository

/**
 * Creates a new account (typically used by Admin sign-up).
 * Validates inputs domain-side before hitting the auth backend.
 */
class SignUpUseCase(private val repository: AuthRepository) {

    suspend operator fun invoke(
        fullName: String,
        email: String,
        password: String,
        confirmPassword: String,
    ): Resource<AuthSession> {
        val name = fullName.trim()
        val mail = email.trim()
        if (name.length < 2) return Resource.failure(AppError.InvalidIdentifier)
        if (!isValidEmail(mail)) return Resource.failure(AppError.InvalidEmail)
        if (password.length < MIN_PASSWORD) return Resource.failure(AppError.WeakPassword)
        if (password != confirmPassword) return Resource.failure(AppError.PasswordMismatch)
        return repository.signUp(mail, password, displayName = name)
    }

    private fun isValidEmail(value: String): Boolean {
        val at = value.indexOf('@')
        val dot = value.lastIndexOf('.')
        return at in 1..value.length - 3 && dot > at + 1 && dot < value.length - 1
    }

    companion object {
        const val MIN_PASSWORD = 6
    }
}

