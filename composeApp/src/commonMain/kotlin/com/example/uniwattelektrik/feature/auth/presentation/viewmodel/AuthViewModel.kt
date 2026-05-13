package com.example.uniwattelektrik.feature.auth.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.uniwattelektrik.core.AppLog
import com.example.uniwattelektrik.core.Resource
import com.example.uniwattelektrik.feature.auth.domain.usecase.LogoutUseCase
import com.example.uniwattelektrik.feature.auth.domain.usecase.ObserveSessionUseCase
import com.example.uniwattelektrik.feature.auth.domain.usecase.SignInUseCase
import com.example.uniwattelektrik.feature.auth.domain.usecase.SignUpUseCase
import com.example.uniwattelektrik.feature.auth.presentation.state.AuthUiEvent
import com.example.uniwattelektrik.feature.auth.presentation.state.AuthUiState
import com.example.uniwattelektrik.platform.nowEpochMillis
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

/**
 * Multiplatform ViewModel driving sign-in **and** sign-up flows for both User and Admin
 * roles. The UI decides which screen to render — this ViewModel holds the form state.
 */
class AuthViewModel(
    private val signIn: SignInUseCase,
    private val signUp: SignUpUseCase,
    private val logoutUseCase: LogoutUseCase,
    observeSession: ObserveSessionUseCase,
    bootstrap: suspend () -> Unit = {},
) : ViewModel() {

    /** Cold-start state is [AuthUiState.Bootstrapping] so the app sits on the
     *  splash until the persisted session is restored. We flip to `Verified`
     *  if a session is found, else `Idle` (login). This avoids the 1-2 s
     *  Login-screen flash on relaunch. */
    private val _state = MutableStateFlow<AuthUiState>(AuthUiState.Bootstrapping)
    val state: StateFlow<AuthUiState> = _state.asStateFlow()

    private val _email = MutableStateFlow("")
    val email: StateFlow<String> = _email.asStateFlow()

    private val _password = MutableStateFlow("")
    val password: StateFlow<String> = _password.asStateFlow()

    private val _confirmPassword = MutableStateFlow("")
    val confirmPassword: StateFlow<String> = _confirmPassword.asStateFlow()

    private val _fullName = MutableStateFlow("")
    val fullName: StateFlow<String> = _fullName.asStateFlow()

    private val _passwordVisible = MutableStateFlow(false)
    val passwordVisible: StateFlow<Boolean> = _passwordVisible.asStateFlow()

    private val _keepSignedIn = MutableStateFlow(true)
    val keepSignedIn: StateFlow<Boolean> = _keepSignedIn.asStateFlow()

    private val _loadingMessage = MutableStateFlow("Please wait...")
    val loadingMessage: StateFlow<String> = _loadingMessage.asStateFlow()

    // Last user intent — enables `Retry` semantics.
    private var lastIntent: AuthUiEvent? = null

    init {
        // Observe session changes (logout, sign-in elsewhere, etc.) and promote
        // to Verified whenever a session appears. Initial Bootstrapping → Idle
        // transition is owned by the `bootstrap` job below so we never flash
        // the Login screen while the persisted session is still being read.
        val sessionFlow = observeSession()
        sessionFlow
            .onEach { session ->
                if (session != null && _state.value !is AuthUiState.Verified) {
                    _state.value = AuthUiState.Verified(session.user)
                }
            }
            .launchIn(viewModelScope)

        // Restore the persisted session on cold start. Once the suspend call
        // returns, we *know* whether the user is signed in — flip out of the
        // Bootstrapping state. We peek the current session from the StateFlow
        // (it's backed by a StateFlow in the repository, so `.first()` is
        // synchronous-ish and returns the current value) to avoid a race
        // where bootstrap completes → state=Idle → observer emits Verified.
        // That race caused a brief "Login screen flash" right after the
        // splash on relaunch for signed-in users.
        viewModelScope.launch {
            try { bootstrap() } catch (_: Throwable) { /* fall through */ }
            if (_state.value is AuthUiState.Bootstrapping) {
                val currentSession = runCatching { sessionFlow.first() }.getOrNull()
                _state.value = if (currentSession != null) {
                    AuthUiState.Verified(currentSession.user)
                } else {
                    AuthUiState.Idle
                }
            }
        }
    }

    fun onEvent(event: AuthUiEvent) {
        when (event) {
            is AuthUiEvent.EmailChanged           -> _email.value           = event.value.trim()
            is AuthUiEvent.PasswordChanged        -> _password.value        = event.value
            is AuthUiEvent.ConfirmPasswordChanged -> _confirmPassword.value = event.value
            is AuthUiEvent.FullNameChanged        -> _fullName.value        = event.value
            AuthUiEvent.TogglePasswordVisibility  -> _passwordVisible.value = !_passwordVisible.value
            AuthUiEvent.ToggleKeepSignedIn        -> _keepSignedIn.value    = !_keepSignedIn.value
            AuthUiEvent.SignIn                    -> { lastIntent = event; performSignIn(asAdmin = false) }
            AuthUiEvent.SignInAsAdmin             -> { lastIntent = event; performSignIn(asAdmin = true) }
            AuthUiEvent.SignUp                    -> { lastIntent = event; performSignUp() }
            AuthUiEvent.Retry                     -> lastIntent?.let { onEvent(it) }
            AuthUiEvent.DismissError              -> (_state.value as? AuthUiState.Error)?.let {
                _state.value = it.previous
            }
            AuthUiEvent.Logout                    -> performLogout()
        }
    }

    /** Resets form fields after logout / on screen switch. Called by the UI when needed. */
    fun clearForm() {
        _email.value = ""
        _password.value = ""
        _confirmPassword.value = ""
        _fullName.value = ""
    }

    private fun performSignIn(asAdmin: Boolean = false) {
        val previous = _state.value
        val email = _email.value
        val pwd = _password.value
        val startedAt = nowEpochMillis()
        _loadingMessage.value = if (asAdmin) "Signing in as admin..." else "Signing in..."
        _state.value = AuthUiState.Loading
        viewModelScope.launch {
            val nextState = when (val result = signIn(email, pwd, asAdmin = asAdmin)) {
                is Resource.Success -> {
                    val user = result.data.user
                    val accountIsAdmin = user.adminId != null
                    when {
                        // User account tried to log in via Admin login screen
                        asAdmin && !accountIsAdmin -> AuthUiState.Error(
                            "This is an employee account. Please use the Employee login.",
                            previous,
                        )
                        // Admin account tried to log in via User login screen
                        !asAdmin && accountIsAdmin -> AuthUiState.Error(
                            "This is an admin account. Please use the Admin login.",
                            previous,
                        )
                        else -> AuthUiState.Verified(user)
                    }
                }
                is Resource.Failure -> AuthUiState.Error(result.error.message, previous)
            }
            holdLoadingForMinimum(startedAt)
            _state.value = nextState
        }
    }

    private fun performSignUp() {
        val previous = _state.value
        val name = _fullName.value
        val email = _email.value
        val pwd = _password.value
        val confirm = _confirmPassword.value
        val startedAt = nowEpochMillis()
        AppLog.i("SignUp", "▶ start  email=$email name='$name' pwdLen=${pwd.length} confirmLen=${confirm.length}")
        _loadingMessage.value = "Creating account..."
        _state.value = AuthUiState.Loading
        viewModelScope.launch {
            val result = signUp(name, email, pwd, confirm)
            val nextState = when (result) {
                is Resource.Success -> {
                    val u = result.data.user
                    AppLog.i("SignUp", "✅ success uid=${u.id} adminId=${u.adminId} email=${u.email}")
                    AuthUiState.Verified(u)
                }
                is Resource.Failure -> {
                    AppLog.e("SignUp", "❌ failed reason=${result.error.message}")
                    AuthUiState.Error(result.error.message, previous)
                }
            }
            holdLoadingForMinimum(startedAt)
            _state.value = nextState
        }
    }

    private fun performLogout() {
        val previous = _state.value
        _loadingMessage.value = "Signing out..."
        _state.value = AuthUiState.Loading
        viewModelScope.launch {
            val startedAt = nowEpochMillis()
            val result = runCatching { logoutUseCase() }
            holdLoadingForMinimum(startedAt)
            result
                .onSuccess {
                    clearForm()
                    _state.value = AuthUiState.Idle
                }
                .onFailure {
                    _state.value = AuthUiState.Error(it.message ?: "Sign out failed", previous)
                }
        }
    }

    private suspend fun holdLoadingForMinimum(startedAt: Long, minDurationMs: Long = 800L) {
        val elapsed = nowEpochMillis() - startedAt
        if (elapsed < minDurationMs) delay(minDurationMs - elapsed)
    }
}
