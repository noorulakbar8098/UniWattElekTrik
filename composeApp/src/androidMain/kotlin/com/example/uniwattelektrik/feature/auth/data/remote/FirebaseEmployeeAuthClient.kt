package com.example.uniwattelektrik.feature.auth.data.remote

import com.example.uniwattelektrik.AndroidAppContext
import com.example.uniwattelektrik.core.AppError
import com.example.uniwattelektrik.core.AppLog
import com.example.uniwattelektrik.core.Resource
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.UserProfileChangeRequest
import kotlinx.coroutines.tasks.await
import kotlin.coroutines.cancellation.CancellationException

/**
 * Android actual for [EmployeeAuthClient].
 *
 * Uses a **secondary** [FirebaseApp] (named `"uniwatt-secondary"`) so that
 * `createUserWithEmailAndPassword` / `signInWithEmailAndPassword` performed
 * here NEVER touch the admin's primary auth session.
 */
internal class FirebaseEmployeeAuthClient : EmployeeAuthClient {

    private val secondaryAuth: FirebaseAuth by lazy {
        val primary = FirebaseApp.getInstance() // requires google-services.json
        val existing = FirebaseApp.getApps(AndroidAppContext.application)
            .firstOrNull { it.name == SECONDARY_NAME }
        val secondaryApp = existing ?: FirebaseApp.initializeApp(
            AndroidAppContext.application,
            primary.options,
            SECONDARY_NAME,
        )
        FirebaseAuth.getInstance(secondaryApp).also {
            // Make sure the secondary app starts signed-out.
            runCatching { it.signOut() }
        }
    }

    override suspend fun createEmployee(
        email: String,
        password: String,
        displayName: String?,
    ): Resource<String> = try {
        AppLog.i("EmpAuth", "createEmployee email=$email (secondary app)")
        val auth = secondaryAuth
        val result = auth.createUserWithEmailAndPassword(email, password).await()
        val user = result.user ?: return Resource.failure(AppError.Unknown("No user returned"))
        if (!displayName.isNullOrBlank()) {
            runCatching {
                user.updateProfile(
                    UserProfileChangeRequest.Builder().setDisplayName(displayName).build(),
                ).await()
            }
        }
        val uid = user.uid
        AppLog.i("EmpAuth", "  ↳ created uid=$uid — signing out secondary app")
        runCatching { auth.signOut() }
        Resource.success(uid)
    } catch (e: CancellationException) {
        throw e
    } catch (e: FirebaseAuthUserCollisionException) {
        AppLog.w("EmpAuth", "createEmployee: email already in use", e)
        Resource.failure(AppError.EmailAlreadyInUse)
    } catch (e: FirebaseAuthWeakPasswordException) {
        Resource.failure(AppError.WeakPassword)
    } catch (e: FirebaseAuthInvalidCredentialsException) {
        Resource.failure(AppError.InvalidCredentials)
    } catch (e: FirebaseTooManyRequestsException) {
        Resource.failure(AppError.QuotaExceeded)
    } catch (e: FirebaseNetworkException) {
        Resource.failure(AppError.Network)
    } catch (e: Throwable) {
        AppLog.e("EmpAuth", "createEmployee failed", e)
        Resource.failure(AppError.Unknown(e.message ?: e::class.simpleName ?: "Unknown error"))
    }

    override suspend fun changeOwnPassword(
        email: String,
        currentPassword: String,
        newPassword: String,
    ): Resource<String> = try {
        AppLog.i("EmpAuth", "changeOwnPassword email=$email (secondary app)")
        val auth = secondaryAuth
        val signIn = auth.signInWithEmailAndPassword(email, currentPassword).await()
        val user = signIn.user ?: return Resource.failure(AppError.Unknown("No user returned"))
        user.updatePassword(newPassword).await()
        val uid = user.uid
        AppLog.i("EmpAuth", "  ↳ password rotated for uid=$uid — signing out secondary app")
        runCatching { auth.signOut() }
        Resource.success(uid)
    } catch (e: CancellationException) {
        throw e
    } catch (e: FirebaseAuthInvalidUserException) {
        Resource.failure(AppError.InvalidCredentials)
    } catch (e: FirebaseAuthInvalidCredentialsException) {
        Resource.failure(AppError.InvalidCredentials)
    } catch (e: FirebaseAuthWeakPasswordException) {
        Resource.failure(AppError.WeakPassword)
    } catch (e: FirebaseTooManyRequestsException) {
        Resource.failure(AppError.QuotaExceeded)
    } catch (e: FirebaseNetworkException) {
        Resource.failure(AppError.Network)
    } catch (e: Throwable) {
        AppLog.e("EmpAuth", "changeOwnPassword failed", e)
        Resource.failure(AppError.Unknown(e.message ?: e::class.simpleName ?: "Unknown error"))
    }

    private companion object {
        const val SECONDARY_NAME = "uniwatt-secondary"
    }
}

actual object EmployeeAuthClientFactory {
    actual fun create(): EmployeeAuthClient = FirebaseEmployeeAuthClient()
}

