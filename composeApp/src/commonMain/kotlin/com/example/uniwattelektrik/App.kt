package com.example.uniwattelektrik

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.uniwattelektrik.di.AppContainer
import com.example.uniwattelektrik.feature.admin.presentation.AdminShell
import com.example.uniwattelektrik.feature.auth.presentation.screens.AdminLoginScreen
import com.example.uniwattelektrik.feature.auth.presentation.screens.AdminSignUpScreen
import com.example.uniwattelektrik.feature.auth.presentation.screens.LoginScreen
import com.example.uniwattelektrik.feature.auth.presentation.state.AuthRoute
import com.example.uniwattelektrik.feature.auth.presentation.state.AuthUiState
import com.example.uniwattelektrik.feature.user.presentation.UserShell

/**
 * App entry point.
 * Navigation is a **pure function of [AuthUiState]** — no nav graph needed for this module.
 *
 *   Idle / Error(prev=Idle)                 → LoginScreen
 *   Verified                                → HomeScreen
 *   Loading                                 → render the last stable screen with overlay
 */
@Composable
@Preview
fun App() {
    // The ViewModel now owns the bootstrap. It starts in `Bootstrapping`, so
    // the splash stays up until the persisted session is fully restored.
    val viewModel = remember { AppContainer.createAuthViewModel() }
    val state by viewModel.state.collectAsStateWithLifecycle()

    // Hold the splash until either: the minimum splash duration has passed
    // AND the auth state has resolved (Verified or Idle), OR the user is
    // already known. This eliminates the previous Login-screen flash on
    // app relaunch when a session exists.
    var splashMinElapsed by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(900L)   // brief brand moment, not 2 s
        splashMinElapsed = true
    }

    val showSplash = state is AuthUiState.Bootstrapping || !splashMinElapsed
    if (showSplash) {
        // Solid brand-blue bridge that matches the Android system splash.
        // The OS already showed the launcher icon on this exact background;
        // we just hold it for a few hundred ms until auth resolves so the
        // user perceives a single, continuous splash moment.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(androidx.compose.ui.graphics.Color(0xFF2979FF)),
        )
        return
    }

    // Which auth screen to show *while signed out*. Sign-in/sign-up promote to Verified.
    var route by remember { mutableStateOf(AuthRoute.UserLogin) }

    // Keep the last "stable" (non-Loading, non-Error) state so Loading/Error can
    // render as an overlay on top of the correct underlying screen.
    var lastStable by remember { mutableStateOf<AuthUiState>(AuthUiState.Idle) }
    SideEffect {
        if (state !is AuthUiState.Loading && state !is AuthUiState.Error
            && state !is AuthUiState.Bootstrapping) {
            lastStable = state
        }
    }

    val screenState: AuthUiState = when (val s = state) {
        is AuthUiState.Error -> s.previous
        AuthUiState.Loading -> lastStable
        AuthUiState.Bootstrapping -> lastStable
        else -> s
    }

    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.fillMaxSize()) {
                when (screenState) {
                    is AuthUiState.Verified -> {
                        // Role split: admins see the AdminShell, employees see the UserShell.
                        // `User.adminId` is non-null for admins (set during admin sign-up).
                        if (screenState.user.adminId != null) {
                            AdminShell(user = screenState.user, viewModel = viewModel)
                        } else {
                            UserShell(user = screenState.user, viewModel = viewModel)
                        }
                    }
                    else -> Box(
                        modifier = Modifier
                            .fillMaxSize()
                            // Auth screens render inside the safe area too.
                            .windowInsetsPadding(WindowInsets.systemBars),
                    ) {
                        when (route) {
                            AuthRoute.UserLogin -> LoginScreen(
                                viewModel = viewModel,
                                onSwitchToAdmin = { route = AuthRoute.AdminLogin },
                            )
                            AuthRoute.AdminLogin -> AdminLoginScreen(
                                viewModel = viewModel,
                                onSwitchToUser = { route = AuthRoute.UserLogin },
                                onSignUp = { viewModel.clearForm(); route = AuthRoute.AdminSignUp },
                            )
                            AuthRoute.AdminSignUp -> AdminSignUpScreen(
                                viewModel = viewModel,
                                onBack = { viewModel.clearForm(); route = AuthRoute.AdminLogin },
                            )
                        }
                    }
                }
            }
        }
    }
}