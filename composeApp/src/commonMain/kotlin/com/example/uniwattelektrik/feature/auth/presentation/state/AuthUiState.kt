package com.example.uniwattelektrik.feature.auth.presentation.state

import com.example.uniwattelektrik.feature.auth.domain.model.User

/**
 * Single source of truth for all Auth screens.
 * Navigation is a pure function of this state (see `App.kt`).
 */
sealed interface AuthUiState {
    /** Cold-start placeholder while [com.example.uniwattelektrik.di.AppContainer.bootstrap]
     *  is still restoring the persisted session. The UI shows the splash here
     *  (instead of flashing the Login screen) until we know whether the user
     *  is signed in. */
    data object Bootstrapping : AuthUiState
    data object Idle : AuthUiState
    data object Loading : AuthUiState
    data class Verified(val user: User) : AuthUiState

    /** Transient — carries [previous] so the UI can offer Retry without losing context. */
    data class Error(val message: String, val previous: AuthUiState) : AuthUiState
}

