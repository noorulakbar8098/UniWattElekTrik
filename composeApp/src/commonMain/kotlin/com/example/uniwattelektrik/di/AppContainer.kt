package com.example.uniwattelektrik.di

import com.example.uniwattelektrik.feature.auth.data.remote.EmailAuthClient
import com.example.uniwattelektrik.feature.auth.data.remote.provideEmailAuthClient
import com.example.uniwattelektrik.feature.auth.data.repository.AuthRepositoryImpl
import com.example.uniwattelektrik.feature.auth.domain.repository.AuthRepository
import com.example.uniwattelektrik.feature.auth.domain.usecase.LogoutUseCase
import com.example.uniwattelektrik.feature.auth.domain.usecase.ObserveSessionUseCase
import com.example.uniwattelektrik.feature.auth.domain.usecase.SignInUseCase
import com.example.uniwattelektrik.feature.auth.domain.usecase.SignUpUseCase
import com.example.uniwattelektrik.feature.auth.presentation.viewmodel.AuthViewModel
import com.example.uniwattelektrik.platform.SessionStorage

/**
 * Tiny manual DI container — single source of truth for wiring.
 *
 * Swap [MockEmailAuthClient] for `FirebaseEmailAuthClient` once
 * `google-services.json` and the Firebase Auth dependency are added on Android.
 *
 * Call [bootstrap] once at app start to hydrate the persisted session.
 */
object AppContainer {

    // --- Platform singletons ---
    private val sessionStorage by lazy { SessionStorage() }

    // --- Data layer ---
    // Auto-detects Firebase: real client when google-services.json is present,
    // mock client otherwise (so dev/preview keeps working).
    private val emailAuthClient: EmailAuthClient by lazy { provideEmailAuthClient() }

    private val authRepositoryImpl by lazy {
        AuthRepositoryImpl(emailAuthClient, sessionStorage)
    }

    val authRepository: AuthRepository get() = authRepositoryImpl

    // --- Domain layer ---
    val signInUseCase by lazy { SignInUseCase(authRepository) }
    val signUpUseCase by lazy { SignUpUseCase(authRepository) }
    val logoutUseCase by lazy { LogoutUseCase(authRepository) }
    val observeSessionUseCase by lazy { ObserveSessionUseCase(authRepository) }

    // --- Presentation factory ---
    fun createAuthViewModel(): AuthViewModel = AuthViewModel(
        signIn = signInUseCase,
        signUp = signUpUseCase,
        logoutUseCase = logoutUseCase,
        observeSession = observeSessionUseCase,
    )

    suspend fun bootstrap() {
        authRepositoryImpl.bootstrap()
    }
}
