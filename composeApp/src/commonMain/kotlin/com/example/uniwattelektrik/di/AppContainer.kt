package com.example.uniwattelektrik.di

import com.example.uniwattelektrik.core.notification.AdminNotificationsCoordinator
import com.example.uniwattelektrik.core.notification.LocalNotifier
import com.example.uniwattelektrik.core.notification.ReminderScheduler
import com.example.uniwattelektrik.core.notification.UserNotificationsCoordinator
import com.example.uniwattelektrik.core.performance.PerformanceMonitor
import com.example.uniwattelektrik.feature.auth.data.remote.AdminDirectory
import com.example.uniwattelektrik.feature.auth.data.remote.AdminDirectoryFactory
import com.example.uniwattelektrik.feature.auth.data.remote.EmailAuthClient
import com.example.uniwattelektrik.feature.auth.data.remote.provideEmailAuthClient
import com.example.uniwattelektrik.feature.auth.data.remote.EmployeeAuthClient
import com.example.uniwattelektrik.feature.auth.data.remote.EmployeeAuthClientFactory
import com.example.uniwattelektrik.feature.auth.data.remote.EmployeeLocator
import com.example.uniwattelektrik.feature.auth.data.remote.EmployeeLocatorFactory
import com.example.uniwattelektrik.feature.auth.data.repository.AuthRepositoryImpl
import com.example.uniwattelektrik.feature.auth.domain.repository.AuthRepository
import com.example.uniwattelektrik.feature.auth.domain.usecase.LogoutUseCase
import com.example.uniwattelektrik.feature.auth.domain.usecase.ObserveSessionUseCase
import com.example.uniwattelektrik.feature.auth.domain.usecase.SignInUseCase
import com.example.uniwattelektrik.feature.auth.domain.usecase.SignUpUseCase
import com.example.uniwattelektrik.feature.auth.presentation.viewmodel.AuthViewModel
import com.example.uniwattelektrik.feature.workforce.data.remote.WorkforceDirectory
import com.example.uniwattelektrik.feature.workforce.data.remote.WorkforceDirectoryFactory
import com.example.uniwattelektrik.feature.workforce.presentation.WorkforceViewModel
import com.example.uniwattelektrik.feature.auth.presentation.viewmodel.PasswordResetViewModel
import com.example.uniwattelektrik.feature.admin.presentation.InventoryViewModel
import com.example.uniwattelektrik.feature.admin.presentation.screens.inventory.SpareItemSeeder
import com.example.uniwattelektrik.platform.LinkLauncher
import com.example.uniwattelektrik.platform.SessionStorage

/**
 * Tiny manual DI container — single source of truth for wiring.
 *
 * The data-layer clients (`EmailAuthClient`, `AdminDirectory`) come from platform
 * factories so Android gets the real Firebase implementations and iOS falls
 * back to no-op stubs until the Firebase iOS SDK is wired.
 *
 * Call [bootstrap] once at app start to hydrate the persisted session.
 */
object AppContainer {

    // --- Platform singletons ---
    private val sessionStorage by lazy { SessionStorage() }
    val linkLauncher: LinkLauncher by lazy { LinkLauncher() }

    /** App-wide performance monitor (Firebase Perf on Android, log-only on iOS). */
    val performanceMonitor: PerformanceMonitor by lazy { PerformanceMonitor() }

    // --- In-app notifications (free path: Firestore listeners + local push) ---
    val localNotifier: LocalNotifier by lazy { LocalNotifier() }

    /**
     * Schedules killed-state-safe local reminders via AlarmManager (Android)
     * / no-op on iOS until UNUserNotificationCenter is wired. Survives swipe
     * and reboot ([BootCompletedReceiver] rehydrates persisted entries).
     */
    val reminderScheduler: ReminderScheduler by lazy { ReminderScheduler() }

    val adminNotificationsCoordinator: AdminNotificationsCoordinator by lazy {
        AdminNotificationsCoordinator(workforceDirectory, localNotifier)
    }
    val userNotificationsCoordinator: UserNotificationsCoordinator by lazy {
        UserNotificationsCoordinator(workforceDirectory, localNotifier)
    }

    // --- Data layer ---
    // Auto-detects Firebase: real client when google-services.json is present,
    // mock client otherwise (so dev/preview keeps working).
    private val emailAuthClient: EmailAuthClient by lazy { provideEmailAuthClient() }

    // Admin directory — Firestore on Android, no-op stub on iOS until wired.
    private val adminDirectory: AdminDirectory by lazy { AdminDirectoryFactory.create() }

    // Workforce directory — Firestore on Android, stub on iOS.
    private val workforceDirectory: WorkforceDirectory by lazy { WorkforceDirectoryFactory.create() }

    private val employeeAuthClient: EmployeeAuthClient by lazy { EmployeeAuthClientFactory.create() }
    private val employeeLocator: EmployeeLocator by lazy { EmployeeLocatorFactory.create() }

    private val authRepositoryImpl by lazy {
        AuthRepositoryImpl(emailAuthClient, sessionStorage, adminDirectory, employeeLocator)
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
        bootstrap = { authRepositoryImpl.bootstrap() },
    )

    fun createWorkforceViewModel(): WorkforceViewModel =
        WorkforceViewModel(
            directory = workforceDirectory,
            employeeAuthClient = employeeAuthClient,
            reminderScheduler = reminderScheduler,
        )

    fun createInventoryViewModel(): InventoryViewModel =
        InventoryViewModel(
            directory = workforceDirectory,
        )

    /** Drives the "Create New Password" sheet on the user login screen. */
    fun createPasswordResetViewModel(): PasswordResetViewModel =
        PasswordResetViewModel(
            employeeAuthClient = employeeAuthClient,
            employeeLocator = employeeLocator,
            workforceDirectory = workforceDirectory,
        )

    suspend fun bootstrap() {
        authRepositoryImpl.bootstrap()
    }

    /**
     * Seed sample spare items into Firestore for this admin if their inventory
     * is empty. Safe to call repeatedly — no-ops once data exists.
     */
    suspend fun seedAdminInventoryIfNeeded(adminId: String) {
        SpareItemSeeder.seedInventoryIfNeeded(adminId, workforceDirectory)
    }
}
