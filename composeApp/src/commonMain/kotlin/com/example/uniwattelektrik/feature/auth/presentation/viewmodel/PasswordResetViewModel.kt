package com.example.uniwattelektrik.feature.auth.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.uniwattelektrik.core.AppLog
import com.example.uniwattelektrik.core.Resource
import com.example.uniwattelektrik.feature.auth.data.remote.EmployeeAuthClient
import com.example.uniwattelektrik.feature.auth.data.remote.EmployeeLocator
import com.example.uniwattelektrik.feature.workforce.data.remote.WorkforceDirectory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Drives the **first-time password rotation** flow used by the
 * "Create New Password" button on the user login screen.
 *
 * Flow:
 *   1. Sign in on the *secondary* Firebase app with email + the temporary
 *      password the admin shared.
 *   2. Look up the employee record via collectionGroup("users") and verify
 *      `mustChangePassword == true` — otherwise refuse (the user has already
 *      rotated their password and must use "Forgot password" instead).
 *   3. Update the password on Firebase Auth.
 *   4. Flip the Firestore flag to `false`.
 *   5. Sign out the secondary app so the admin's primary session is untouched.
 */
class PasswordResetViewModel(
    private val employeeAuthClient: EmployeeAuthClient,
    private val employeeLocator: EmployeeLocator,
    private val workforceDirectory: WorkforceDirectory,
) : ViewModel() {

    sealed interface State {
        data object Idle : State
        data object Loading : State
        data class Error(val message: String) : State
        data object Success : State
    }

    private val _state = MutableStateFlow<State>(State.Idle)
    val state: StateFlow<State> = _state.asStateFlow()

    fun reset() { _state.value = State.Idle }

    fun submit(email: String, currentPassword: String, newPassword: String) {
        if (_state.value is State.Loading) return
        viewModelScope.launch {
            _state.value = State.Loading
            AppLog.i("PwdReset", "submit email=$email")

            // Step 1+3: change the password on Firebase Auth (uses secondary app).
            val rotated = employeeAuthClient.changeOwnPassword(
                email = email,
                currentPassword = currentPassword,
                newPassword = newPassword,
            )
            if (rotated is Resource.Failure) {
                _state.value = State.Error(rotated.error.message)
                return@launch
            }
            val uid = (rotated as Resource.Success).data

            // Step 2 (after-the-fact): we still need the parent admin id so we
            // know which Firestore doc to flip the flag on. We already proved
            // the user controls this account by signing in with their old pwd.
            val account = runCatching { employeeLocator.findByUid(uid) }
                .onFailure { AppLog.w("PwdReset", "locator failed", it) }
                .getOrNull()

            if (account == null) {
                _state.value = State.Error(
                    "Password updated, but we couldn't find your profile. " +
                        "Please contact your admin.",
                )
                return@launch
            }

            if (!account.mustChangePassword) {
                _state.value = State.Error(
                    "You've already set your own password. Use 'Forgot password' to reset it.",
                )
                return@launch
            }

            // Step 4: flip the flag.
            runCatching {
                workforceDirectory.markPasswordChanged(account.parentAdminId, uid)
            }.onFailure {
                AppLog.w("PwdReset", "markPasswordChanged failed", it)
                _state.value = State.Error(
                    "Password updated, but we couldn't sync your profile. Please try logging in.",
                )
                return@launch
            }

            AppLog.i("PwdReset", "  ↳ rotated + flag cleared for uid=$uid")
            _state.value = State.Success
        }
    }
}

