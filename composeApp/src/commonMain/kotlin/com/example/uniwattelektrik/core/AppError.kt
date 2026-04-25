package com.example.uniwattelektrik.core

/**
 * Unified error model for the domain layer.
 * Platform exceptions (Firebase, IO, …) are mapped to one of these in the data layer
 * so the domain/presentation never depend on framework-specific types.
 */
sealed class AppError(val message: String) {
    data object Network : AppError("No internet connection. Please check and try again.")
    data object Timeout : AppError("Request timed out. Please retry.")
    data object InvalidEmail : AppError("Please enter a valid email address.")
    data object InvalidIdentifier : AppError("Please enter your full name.")
    data object InvalidPassword : AppError("Password must be at least 6 characters.")
    data object PasswordMismatch : AppError("Passwords do not match.")
    data object InvalidCredentials : AppError("Incorrect email or password.")
    data object EmailAlreadyInUse : AppError("An account with this email already exists.")
    data object WeakPassword : AppError("Password is too weak. Use at least 6 characters.")
    data object UserDisabled : AppError("This account has been disabled. Contact your admin.")
    data object QuotaExceeded : AppError("Too many attempts. Please try again later.")
    data class Unknown(val raw: String) : AppError(raw)
}

