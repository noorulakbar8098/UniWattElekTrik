package com.example.uniwattelektrik.feature.auth.data.remote

import com.example.uniwattelektrik.core.AppError
import com.example.uniwattelektrik.core.Resource
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.FirebaseAuthException
import dev.gitlive.firebase.auth.FirebaseAuthInvalidCredentialsException
import dev.gitlive.firebase.auth.FirebaseAuthInvalidUserException
import dev.gitlive.firebase.auth.FirebaseAuthUserCollisionException
import dev.gitlive.firebase.auth.FirebaseAuthWeakPasswordException
import dev.gitlive.firebase.auth.auth
import kotlin.coroutines.cancellation.CancellationException

/**
 * Real Firebase Email/Password sign-in for iOS, backed by the
 * GitLive Firebase KMP wrapper around the Firebase Apple SDK.
 *
 * Pre-requisites in the iosApp Xcode project (one-time setup):
 *   1. Add the Firebase iOS SDK via Swift Package Manager:
 *      File ▸ Add Package Dependencies… ▸
 *      https://github.com/firebase/firebase-ios-sdk → add product **FirebaseAuth**.
 *   2. Drop **GoogleService-Info.plist** (Firebase Console ▸ Project Settings ▸
 *      Your iOS app) into `iosApp/iosApp/`.
 *   3. In `iOSApp.swift`, call `FirebaseApp.configure()` on launch.
 */
private class FirebaseEmailAuthClientIos : EmailAuthClient {

    private val auth = Firebase.auth

    override suspend fun signIn(
        email: String,
        password: String,
    ): Resource<EmailAuthResult> = try {
        val result = auth.signInWithEmailAndPassword(email, password)
        val user   = result.user
            ?: return Resource.failure(AppError.Unknown("No user returned"))
        val token  = user.getIdToken(forceRefresh = false)
            ?: return Resource.failure(AppError.Unknown("No ID token"))
        Resource.success(
            EmailAuthResult(
                uid         = user.uid,
                email       = user.email ?: email,
                idToken     = token,
                displayName = user.displayName,
            )
        )
    } catch (e: CancellationException) {
        throw e
    } catch (e: FirebaseAuthInvalidUserException) {
        Resource.failure(AppError.InvalidCredentials)
    } catch (e: FirebaseAuthInvalidCredentialsException) {
        Resource.failure(AppError.InvalidCredentials)
    } catch (e: FirebaseAuthException) {
        Resource.failure(AppError.Unknown(e.message ?: "Auth failed"))
    } catch (e: Throwable) {
        Resource.failure(AppError.Unknown(e.message ?: "Unknown error"))
    }

    override suspend fun signUp(
        email: String,
        password: String,
        displayName: String?,
    ): Resource<EmailAuthResult> = try {
        val result = auth.createUserWithEmailAndPassword(email, password)
        val user   = result.user
            ?: return Resource.failure(AppError.Unknown("No user returned"))
        if (!displayName.isNullOrBlank()) {
            user.updateProfile(displayName = displayName)
        }
        val token = user.getIdToken(forceRefresh = false)
            ?: return Resource.failure(AppError.Unknown("No ID token"))
        Resource.success(
            EmailAuthResult(
                uid         = user.uid,
                email       = user.email ?: email,
                idToken     = token,
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
    } catch (e: FirebaseAuthException) {
        Resource.failure(AppError.Unknown(e.message ?: "Sign-up failed"))
    } catch (e: Throwable) {
        Resource.failure(AppError.Unknown(e.message ?: "Unknown error"))
    }
}

actual fun provideEmailAuthClient(): EmailAuthClient = FirebaseEmailAuthClientIos()


