package com.example.uniwattelektrik.feature.auth.data.remote

import com.example.uniwattelektrik.core.AppError
import com.example.uniwattelektrik.core.Resource

/**
 * iOS placeholder until a secondary `FIRApp` is wired through GitLive.
 * Returns a clear "not configured" failure so the admin can see what's missing.
 */
private class UnconfiguredEmployeeAuthClient : EmployeeAuthClient {
    override suspend fun createEmployee(
        email: String,
        password: String,
        displayName: String?,
    ): Resource<String> = Resource.failure(
        AppError.Unknown("Employee creation is not yet wired on iOS."),
    )

    override suspend fun changeOwnPassword(
        email: String,
        currentPassword: String,
        newPassword: String,
    ): Resource<String> = Resource.failure(
        AppError.Unknown("Password change is not yet wired on iOS."),
    )
}

actual object EmployeeAuthClientFactory {
    actual fun create(): EmployeeAuthClient = UnconfiguredEmployeeAuthClient()
}

