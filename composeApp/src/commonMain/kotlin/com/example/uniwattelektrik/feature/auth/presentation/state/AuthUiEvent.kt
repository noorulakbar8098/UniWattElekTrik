package com.example.uniwattelektrik.feature.auth.presentation.state

/** User intents captured by the UI and dispatched to the ViewModel. */
sealed interface AuthUiEvent {
    data class EmailChanged(val value: String) : AuthUiEvent
    data class PasswordChanged(val value: String) : AuthUiEvent
    data class ConfirmPasswordChanged(val value: String) : AuthUiEvent
    data class FullNameChanged(val value: String) : AuthUiEvent
    data object TogglePasswordVisibility : AuthUiEvent
    data object ToggleKeepSignedIn : AuthUiEvent
    data object SignIn : AuthUiEvent
    data object SignInAsAdmin : AuthUiEvent
    data object SignUp : AuthUiEvent
    data object Retry : AuthUiEvent
    data object Logout : AuthUiEvent
    data object DismissError : AuthUiEvent
}

