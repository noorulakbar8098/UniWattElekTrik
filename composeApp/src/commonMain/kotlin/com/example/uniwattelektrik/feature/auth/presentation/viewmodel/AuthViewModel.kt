package com.example.uniwattelektrik.feature.auth.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.uniwattelektrik.core.Resource
import com.example.uniwattelektrik.feature.auth.domain.usecase.LogoutUseCase
import com.example.uniwattelektrik.feature.auth.domain.usecase.ObserveSessionUseCase
import com.example.uniwattelektrik.feature.auth.domain.usecase.SignInUseCase
import com.example.uniwattelektrik.feature.auth.domain.usecase.SignUpUseCase
import com.example.uniwattelektrik.feature.auth.presentation.state.AuthUiEvent
import com.example.uniwattelektrik.feature.auth.presentation.state.AuthUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
) : ViewModel() {

    private val _state = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
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

    // Last user intent — enables `Retry` semantics.
    private var lastIntent: AuthUiEvent? = null

    init {
        // Auto-promote to Verified when a persisted session is observed at cold start.
        observeSession()
            .onEach { session ->
                if (session != null && _state.value !is AuthUiState.Verified) {
                    _state.value = AuthUiState.Verified(session.user)
                }
            }
            .launchIn(viewModelScope)
    }

    fun onEvent(event: AuthUiEvent) {
        when (event) {
            is AuthUiEvent.EmailChanged           -> _email.value           = event.value.trim()
            is AuthUiEvent.PasswordChanged        -> _password.value        = event.value
            is AuthUiEvent.ConfirmPasswordChanged -> _confirmPassword.value = event.value
            is AuthUiEvent.FullNameChanged        -> _fullName.value        = event.value
            AuthUiEvent.TogglePasswordVisibility  -> _passwordVisible.value = !_passwordVisible.value
            AuthUiEvent.ToggleKeepSignedIn        -> _keepSignedIn.value    = !_keepSignedIn.value
            AuthUiEvent.SignIn                    -> { lastIntent = event; performSignIn() }
            AuthUiEvent.SignUp                    -> { lastIntent = event; performSignUp() }
            AuthUiEvent.Retry                     -> lastIntent?.let { onEvent(it) }
            AuthUiEvent.DismissError              -> (_state.value as? AuthUiState.Error)?.let {
                _state.value = it.previous
            }
            AuthUiEvent.Logout                    -> viewModelScope.launch {
                logoutUseCase()
                clearForm()
                _state.value = AuthUiState.Idle
            }
        }
    }

    /** Resets form fields after logout / on screen switch. Called by the UI when needed. */
    fun clearForm() {
        _email.value = ""
        _password.value = ""
        _confirmPassword.value = ""
        _fullName.value = ""
    }

    private fun performSignIn() {
        val previous = _state.value
        val email = _email.value
        val pwd = _password.value
        _state.value = AuthUiState.Loading
        viewModelScope.launch {
            _state.value = when (val result = signIn(email, pwd)) {
                is Resource.Success -> AuthUiState.Verified(result.data.user)
                is Resource.Failure -> AuthUiState.Error(result.error.message, previous)
            }
        }
    }

    private fun performSignUp() {
        val previous = _state.value
        val name = _fullName.value
        val email = _email.value
        val pwd = _password.value
        val confirm = _confirmPassword.value
        _state.value = AuthUiState.Loading
        viewModelScope.launch {
            _state.value = when (val result = signUp(name, email, pwd, confirm)) {
                is Resource.Success -> AuthUiState.Verified(result.data.user)
                is Resource.Failure -> AuthUiState.Error(result.error.message, previous)
            }
        }
    }
}
