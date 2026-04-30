package com.example.uniwattelektrik.feature.auth.data.remote

import com.example.uniwattelektrik.core.Resource

/**
 * Specialised auth client used by the **admin app** to create new employee
 * accounts WITHOUT signing the admin out.
 *
 * Implementation note (Android/iOS):
 *   Both Firebase mobile SDKs replace the current user when you call
 *   `createUserWithEmailAndPassword(...)`. To keep the admin's session
 *   intact we route this call through a **secondary `FirebaseApp` instance**
 *   that points at the same project but has its own isolated Auth state.
 *
 * Also exposes [changeOwnPassword] so the freshly-created employee can rotate
 * their initial admin-given password from the user login screen WITHOUT
 * disturbing the admin's primary session either.
 */
interface EmployeeAuthClient {
    /** Creates a Firebase Auth account on a *secondary* app and returns the new uid. */
    suspend fun createEmployee(
        email: String,
        password: String,
        displayName: String?,
    ): Resource<String>

    /**
     * Signs in on a *secondary* app with [email] + [currentPassword], rotates
     * the password to [newPassword], and signs out — all without affecting the
     * primary app session. Returns the user's uid on success.
     */
    suspend fun changeOwnPassword(
        email: String,
        currentPassword: String,
        newPassword: String,
    ): Resource<String>
}

