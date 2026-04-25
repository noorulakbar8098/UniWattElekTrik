package com.example.uniwattelektrik.feature.auth.presentation.state

import com.example.uniwattelektrik.feature.auth.domain.model.User

/**
 * Single source of truth for all Auth screens.
 * Navigation is a pure function of this state (see `App.kt`).
 */
sealed interface AuthUiState {
    data object Idle : AuthUiState
    data object Loading : AuthUiState
    data class Verified(val user: User) : AuthUiState

    /** Transient — carries [previous] so the UI can offer Retry without losing context. */
    data class Error(val message: String, val previous: AuthUiState) : AuthUiState
}

