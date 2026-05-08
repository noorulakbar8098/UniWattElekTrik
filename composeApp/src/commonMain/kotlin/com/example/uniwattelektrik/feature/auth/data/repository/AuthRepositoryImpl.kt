package com.example.uniwattelektrik.feature.auth.data.repository

import com.example.uniwattelektrik.core.AppLog
import com.example.uniwattelektrik.core.Resource
import com.example.uniwattelektrik.feature.auth.data.remote.AdminDirectory
import com.example.uniwattelektrik.feature.auth.data.remote.EmailAuthClient
import com.example.uniwattelektrik.feature.auth.data.remote.EmailAuthResult
import com.example.uniwattelektrik.feature.auth.data.remote.EmployeeLocator
import com.example.uniwattelektrik.feature.auth.domain.model.AuthSession
import com.example.uniwattelektrik.feature.auth.domain.model.User
import com.example.uniwattelektrik.feature.auth.domain.repository.AuthRepository
import com.example.uniwattelektrik.platform.SessionStorage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Repository implementation for email/password auth.
 *
 * Also bridges the [AdminDirectory] so [User.adminId] is populated whenever
 * the signed-in account is an admin. The presentation layer uses that field
 * to pick between the User shell and the Admin shell.
 *
 * - **Sign-up** is admin-only in this app: a fresh admin ID (`ADM-XXXX`) is
 *   generated, persisted in the directory, and returned on the [User].
 * - **Sign-in** queries the directory by `uid`; if a record exists, that
 *   admin ID is attached. Plain users get `adminId = null` and stay on the
 *   User shell.
 */
class AuthRepositoryImpl(
    private val emailAuthClient: EmailAuthClient,
    private val sessionStorage: SessionStorage,
    private val adminDirectory: AdminDirectory,
    private val employeeLocator: EmployeeLocator,
) : AuthRepository {

    private val sessionFlow = MutableStateFlow<AuthSession?>(null)

    suspend fun bootstrap() {
        val token = sessionStorage.readToken() ?: return
        val profile = sessionStorage.readProfile()
        // If we have a saved profile, restore the **real** user (with adminId)
        // so role-based routing works on cold start. Otherwise fall back to a
        // placeholder — the next sign-in will overwrite it.
        sessionFlow.value = if (profile != null) {
            AuthSession(
                token = token,
                user = User(
                    id = profile.uid,
                    email = profile.email,
                    displayName = profile.displayName,
                    adminId = profile.adminId,
                    parentAdminId = profile.parentAdminId,
                    mustChangePassword = profile.mustChangePassword,
                    permission = profile.permission,
                ),
            )
        } else {
            AuthSession(
                token = token,
                user = User(id = token.take(8), email = "unknown"),
            )
        }
    }

    override suspend fun signIn(
        email: String,
        password: String,
        asAdmin: Boolean,
    ): Resource<AuthSession> =
        toSession(
            result = emailAuthClient.signIn(email, password),
            isSignUp = false,
            fullName = null,
            asAdmin = asAdmin,
        )

    override suspend fun signUp(
        email: String,
        password: String,
        displayName: String?,
    ): Resource<AuthSession> =
        toSession(
            result = emailAuthClient.signUp(email, password, displayName),
            isSignUp = true,
            fullName = displayName,
            asAdmin = true, // sign-up flow is admin-only in this app
        )

    private suspend fun toSession(
        result: Resource<EmailAuthResult>,
        isSignUp: Boolean,
        fullName: String?,
        asAdmin: Boolean,
    ): Resource<AuthSession> {
        AppLog.d("AuthRepo", "toSession isSignUp=$isSignUp asAdmin=$asAdmin")
        return when (result) {
            is Resource.Success -> {
                val data = result.data
                AppLog.i("AuthRepo", "  ↳ FirebaseAuth OK uid=${data.uid} email=${data.email}")

                // ── Role-guard for sign-in ────────────────────────────────────
                // We always probe the admin directory so that:
                //   • An admin who tries the *user* login is rejected.
                //   • An employee who tries the *admin* login is rejected.
                // Sign-up bypasses this — that flow always provisions an admin.
                val existingAdminId: String? = if (!isSignUp) {
                    runCatching { adminDirectory.lookupAdminIdByUid(data.uid) }
                        .onFailure { AppLog.w("AuthRepo", "  ↳ admin lookup failed", it) }
                        .getOrNull()
                } else null

                if (!isSignUp) {
                    if (asAdmin && existingAdminId == null) {
                        AppLog.w("AuthRepo", "  ↳ REJECT: non-admin tried admin login uid=${data.uid}")
                        return Resource.failure(
                            com.example.uniwattelektrik.core.AppError.Unknown(
                                "This is an employee account. Please use the Employee login.",
                            ),
                        )
                    }
                    if (!asAdmin && existingAdminId != null) {
                        AppLog.w("AuthRepo", "  ↳ REJECT: admin tried user login uid=${data.uid}")
                        return Resource.failure(
                            com.example.uniwattelektrik.core.AppError.Unknown(
                                "This is an admin account. Please use the Admin login.",
                            ),
                        )
                    }
                }

                // Persist the verified token only after the role guard passes,
                // so a rejected attempt leaves no stale credentials behind.
                sessionStorage.saveToken(data.idToken)
                AppLog.d("AuthRepo", "  ↳ token saved (len=${data.idToken.length})")

                val adminId: String? = when {
                    isSignUp -> {
                        // First-time admin → generate + persist their admin ID.
                        val newId = "ADM-${data.uid.take(4).uppercase()}"
                        AppLog.i("AuthRepo", "  ↳ sign-up: registering admins/${data.uid} adminId=$newId")
                        runCatching {
                            adminDirectory.register(
                                uid = data.uid,
                                adminId = newId,
                                email = data.email,
                                fullName = fullName,
                            )
                        }.onFailure {
                            AppLog.e("AuthRepo", "  ↳ register FAILED — admin doc may not exist on server", it)
                        }.onSuccess {
                            AppLog.i("AuthRepo", "  ↳ register OK admins/${data.uid} written")
                        }
                        newId
                    }
                    asAdmin -> existingAdminId  // already validated non-null above
                    else -> null  // employee sign-in — adminId stays null; parentAdminId is resolved below
                }

                // For employee sign-in: resolve owning admin via collection-group lookup.
                val employeeAccount = if (!isSignUp && !asAdmin) {
                    AppLog.d("AuthRepo", "  ↳ employee sign-in: collectionGroup(users) where uid=${data.uid}")
                    runCatching { employeeLocator.findByUid(data.uid) }
                        .onFailure { AppLog.w("AuthRepo", "  ↳ employee locator failed", it) }
                        .getOrNull()
                } else null
                employeeAccount?.let {
                    AppLog.i("AuthRepo", "  ↳ employee resolved parent=${it.parentAdminId} mustChangePwd=${it.mustChangePassword}")
                }

                val session = AuthSession(
                    token = data.idToken,
                    user = User(
                        id = data.uid,
                        email = data.email,
                        displayName = data.displayName ?: fullName,
                        adminId = adminId,
                        parentAdminId = employeeAccount?.parentAdminId,
                        mustChangePassword = employeeAccount?.mustChangePassword ?: false,
                        permission = employeeAccount?.permission ?: "",
                    ),
                )
                sessionStorage.saveProfile(
                    uid = session.user.id,
                    email = session.user.email,
                    displayName = session.user.displayName,
                    adminId = session.user.adminId,
                    parentAdminId = session.user.parentAdminId,
                    mustChangePassword = session.user.mustChangePassword,
                    permission = session.user.permission,
                )
                AppLog.i("AuthRepo", "  ↳ profile persisted uid=${session.user.id} adminId=${session.user.adminId} parent=${session.user.parentAdminId}")
                sessionFlow.value = session
                AppLog.i("AuthRepo", "  ↳ session emitted → ${if (adminId != null) "AdminShell" else "UserShell"}")
                Resource.success(session)
            }
            is Resource.Failure -> {
                AppLog.e("AuthRepo", "  ↳ FirebaseAuth FAILED reason=${result.error.message}")
                result
            }
        }
    }

    override fun observeSession(): Flow<AuthSession?> = sessionFlow.asStateFlow()

    override suspend fun logout() {
        sessionStorage.clear()
        sessionFlow.value = null
    }
}
