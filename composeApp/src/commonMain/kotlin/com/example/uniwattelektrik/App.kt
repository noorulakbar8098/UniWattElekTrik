package com.example.uniwattelektrik

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
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
import com.example.uniwattelektrik.feature.auth.presentation.screens.AdminLoginScreen
import com.example.uniwattelektrik.feature.auth.presentation.screens.AdminSignUpScreen
import com.example.uniwattelektrik.feature.auth.presentation.screens.HomeScreen
import com.example.uniwattelektrik.feature.auth.presentation.screens.LoginScreen
import com.example.uniwattelektrik.feature.auth.presentation.screens.SplashScreen
import com.example.uniwattelektrik.feature.auth.presentation.state.AuthRoute
import com.example.uniwattelektrik.feature.auth.presentation.state.AuthUiState

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
    var showSplash by remember { mutableStateOf(true) }

    if (showSplash) {
        SplashScreen(onFinished = { showSplash = false })
        return
    }

    val viewModel = remember { AppContainer.createAuthViewModel() }
    LaunchedEffect(Unit) { AppContainer.bootstrap() }

    val state by viewModel.state.collectAsStateWithLifecycle()

    // Which auth screen to show *while signed out*. Sign-in/sign-up promote to Verified.
    var route by remember { mutableStateOf(AuthRoute.UserLogin) }

    // Keep the last "stable" (non-Loading, non-Error) state so Loading/Error can
    // render as an overlay on top of the correct underlying screen.
    var lastStable by remember { mutableStateOf<AuthUiState>(AuthUiState.Idle) }
    SideEffect {
        if (state !is AuthUiState.Loading && state !is AuthUiState.Error) {
            lastStable = state
        }
    }

    val screenState: AuthUiState = when (val s = state) {
        is AuthUiState.Error -> s.previous
        AuthUiState.Loading -> lastStable
        else -> s
    }

    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.fillMaxSize()) {
                when (screenState) {
                    is AuthUiState.Verified -> HomeScreen(screenState.user, viewModel)
                    else -> when (route) {
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