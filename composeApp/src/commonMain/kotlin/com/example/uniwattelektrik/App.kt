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
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.uniwattelektrik.di.AppContainer
import com.example.uniwattelektrik.feature.admin.presentation.AdminShell
import com.example.uniwattelektrik.feature.admin.presentation.SeniorManagerShell
import com.example.uniwattelektrik.feature.auth.presentation.screens.AdminLoginScreen
import com.example.uniwattelektrik.feature.auth.presentation.screens.AdminSignUpScreen
import com.example.uniwattelektrik.feature.auth.presentation.screens.LoginScreen
import com.example.uniwattelektrik.feature.auth.presentation.state.AuthRoute
import com.example.uniwattelektrik.feature.auth.presentation.state.AuthUiState
import com.example.uniwattelektrik.feature.user.presentation.UserShell
import com.example.uniwattelektrik.platform.PlatformBackHandler

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

    // No Compose splash. The native Android 12+ system splash
    // (windowSplashScreenBackground = @color/splash_bg = #0D1B3E + adaptive
    // icon) is the *only* splash. We just hold an invisible Ink900 frame
    // while auth bootstrap is resolving so the Login screen doesn't flash
    // for signed-in users on cold start.
    if (state is AuthUiState.Bootstrapping) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0D1B3E)),
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
                        // Role split:
                        //   adminId != null          → full AdminShell
                        //   permission == "super"    → SeniorManagerShell (limited admin)
                        //   else                     → UserShell (regular employee)
                        when {
                            screenState.user.adminId != null ->
                                AdminShell(user = screenState.user, viewModel = viewModel)
                            screenState.user.permission.lowercase() == "super" ->
                                SeniorManagerShell(user = screenState.user, viewModel = viewModel)
                            else ->
                                UserShell(user = screenState.user, viewModel = viewModel)
                        }
                    }
                    else -> Box(
                        modifier = Modifier
                            .fillMaxSize()
                            // Auth screens render inside the safe area too.
                            .windowInsetsPadding(WindowInsets.systemBars),
                    ) {
                        // System back inside the auth flow:
                        //  AdminSignUp → AdminLogin   (matches the visible header arrow)
                        //  AdminLogin  → UserLogin    (default landing)
                        //  UserLogin   → handler disabled, OS finishes the app
                        PlatformBackHandler(enabled = route != AuthRoute.UserLogin) {
                            viewModel.clearForm()
                            route = when (route) {
                                AuthRoute.AdminSignUp -> AuthRoute.AdminLogin
                                else                  -> AuthRoute.UserLogin
                            }
                        }
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

                if (state is AuthUiState.Loading) {
                    // Simple centered spinner for all loading states (login, logout, etc.)
                    // No skeleton/shimmer overlay — keeps sign-out transition clean.
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.18f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(
                            color = Color(0xFF2979FF),
                            strokeWidth = 3.dp,
                        )
                    }
                }
            }
        }
    }
}