package com.example.uniwattelektrik.feature.auth.data.remote

import com.example.uniwattelektrik.core.AppError
import com.example.uniwattelektrik.core.Resource
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
 * Real Firebase Email/Password sign-in for Android.
 * Maps Firebase exceptions to the framework-agnostic [AppError] catalogue so domain
 * and presentation never see Firebase types.
 */
class FirebaseEmailAuthClient(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
) : EmailAuthClient {

    override suspend fun signIn(email: String, password: String): Resource<EmailAuthResult> = try {
        val authResult = auth.signInWithEmailAndPassword(email, password).await()
        val user = authResult.user ?: return Resource.failure(AppError.Unknown("No user returned"))
        val token = user.getIdToken(false).await().token
            ?: return Resource.failure(AppError.Unknown("No ID token"))
        Resource.success(
            EmailAuthResult(
                uid = user.uid,
                email = user.email ?: email,
                idToken = token,
                displayName = user.displayName,
            )
        )
    } catch (e: CancellationException) {
        throw e
    } catch (e: FirebaseAuthInvalidCredentialsException) {
        Resource.failure(AppError.InvalidCredentials)
    } catch (e: FirebaseAuthInvalidUserException) {
        // user-not-found, user-disabled, etc.
        if (e.errorCode == "ERROR_USER_DISABLED") Resource.failure(AppError.UserDisabled)
        else Resource.failure(AppError.InvalidCredentials)
    } catch (e: FirebaseTooManyRequestsException) {
        Resource.failure(AppError.QuotaExceeded)
    } catch (e: FirebaseNetworkException) {
        Resource.failure(AppError.Network)
    } catch (e: Throwable) {
        Resource.failure(AppError.Unknown(e.message ?: e::class.simpleName ?: "Unknown error"))
    }

    override suspend fun signUp(
        email: String,
        password: String,
        displayName: String?,
    ): Resource<EmailAuthResult> = try {
        val authResult = auth.createUserWithEmailAndPassword(email, password).await()
        val user = authResult.user ?: return Resource.failure(AppError.Unknown("No user returned"))
        if (!displayName.isNullOrBlank()) {
            user.updateProfile(
                UserProfileChangeRequest.Builder().setDisplayName(displayName).build()
            ).await()
        }
        val token = user.getIdToken(false).await().token
            ?: return Resource.failure(AppError.Unknown("No ID token"))
        Resource.success(
            EmailAuthResult(
                uid = user.uid,
                email = user.email ?: email,
                idToken = token,
                displayName = displayName ?: user.displayName,
            )
        )
    } catch (e: CancellationException) {
        throw e
    } catch (e: FirebaseAuthUserCollisionException) {
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
        Resource.failure(AppError.Unknown(e.message ?: e::class.simpleName ?: "Unknown error"))
    }
}

/**
 * Production wiring: always returns the real [FirebaseEmailAuthClient].
 *
 * Requires `google-services.json` at `composeApp/google-services.json`. If the file
 * is missing, `FirebaseAuth.getInstance()` will throw on first use and the user will
 * see an [AppError.Unknown] with a clear message — that's intentional so the
 * misconfiguration is caught immediately instead of silently using a mock.
 */
actual fun provideEmailAuthClient(): EmailAuthClient = FirebaseEmailAuthClient()

