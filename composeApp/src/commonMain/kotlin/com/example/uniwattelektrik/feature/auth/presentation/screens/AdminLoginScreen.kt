package com.example.uniwattelektrik.feature.auth.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.uniwattelektrik.feature.auth.presentation.components.*
import com.example.uniwattelektrik.feature.auth.presentation.state.AuthUiEvent
import com.example.uniwattelektrik.feature.auth.presentation.state.AuthUiState
import com.example.uniwattelektrik.feature.auth.presentation.theme.AuthColors
import com.example.uniwattelektrik.feature.auth.presentation.viewmodel.AuthViewModel

@Composable
fun AdminLoginScreen(
    viewModel: AuthViewModel,
    onSwitchToUser: () -> Unit,
    onSignUp: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val email by viewModel.email.collectAsStateWithLifecycle()
    val password by viewModel.password.collectAsStateWithLifecycle()
    val passwordVisible by viewModel.passwordVisible.collectAsStateWithLifecycle()
    val keepSignedIn by viewModel.keepSignedIn.collectAsStateWithLifecycle()

    val isLoading    = state is AuthUiState.Loading
    val errorMessage = (state as? AuthUiState.Error)?.message
    val canSubmit    = !isLoading && email.isNotBlank() && password.length >= 6

    AuthBackground {
        // ── Brand header + toggle + form card (centred vertically) ────────
        Column(
            modifier            = Modifier.align(Alignment.Center).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            BrandHeader()
            Spacer(Modifier.height(16.dp))

            UserAdminToggleBordered(
                selected   = AuthRole.Admin,
                onSelected = {
                    if (it == AuthRole.User) {
                        viewModel.clearForm()
                        onSwitchToUser()
                    }
                },
                enabled  = !isLoading,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(20.dp))

            // White card containing the form
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(
                        elevation    = 18.dp,
                        shape        = RoundedCornerShape(28.dp),
                        spotColor    = Color(0x280A1F44),
                        ambientColor = Color(0x140A1F44),
                    )
                    .clip(RoundedCornerShape(28.dp))
                    .background(Color.White)
                    .padding(24.dp),
            ) {
                Text(
                    text       = "Admin Access 🔐",
                    style      = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color      = AuthColors.TextDark,
                    modifier   = Modifier.fillMaxWidth(),
                    textAlign  = TextAlign.Start,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text      = "Sign in to manage operations.",
                    style     = MaterialTheme.typography.bodyMedium,
                    color     = AuthColors.TextLight,
                    modifier  = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Start,
                )

                Spacer(Modifier.height(20.dp))

                AuthInputField(
                    value         = email,
                    onValueChange = { viewModel.onEvent(AuthUiEvent.EmailChanged(it)) },
                    placeholder   = "Admin Email",
                    leadingIcon   = Icons.Filled.Email,
                    keyboardType  = KeyboardType.Email,
                    imeAction     = ImeAction.Next,
                    enabled       = !isLoading,
                )
                Spacer(Modifier.height(14.dp))
                AuthInputField(
                    value         = password,
                    onValueChange = { viewModel.onEvent(AuthUiEvent.PasswordChanged(it)) },
                    placeholder   = "Password",
                    leadingIcon   = Icons.Filled.Lock,
                    keyboardType  = KeyboardType.Password,
                    imeAction     = ImeAction.Done,
                    enabled       = !isLoading,
                    visualTransformation =
                        if (passwordVisible) VisualTransformation.None
                        else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(
                            onClick = { viewModel.onEvent(AuthUiEvent.TogglePasswordVisibility) },
                            enabled = !isLoading,
                        ) {
                            Icon(
                                imageVector = if (passwordVisible) Icons.Filled.VisibilityOff
                                              else Icons.Filled.Visibility,
                                contentDescription = null,
                                tint = AuthColors.TextLight,
                            )
                        }
                    },
                )

                Spacer(Modifier.height(8.dp))
                KeepMeRow(
                    checked         = keepSignedIn,
                    onCheckedChange = { viewModel.onEvent(AuthUiEvent.ToggleKeepSignedIn) },
                    enabled         = !isLoading,
                )

                if (errorMessage != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        errorMessage,
                        color    = MaterialTheme.colorScheme.error,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                Spacer(Modifier.height(20.dp))
                GradientButton(
                    text      = "Admin Sign In",
                    isLoading = isLoading,
                    enabled   = canSubmit,
                    onClick   = { viewModel.onEvent(AuthUiEvent.SignInAsAdmin) },
                )
            }
        }

        // ── Bottom: sign-up link ──────────────────────────────────────────
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            TextButton(onClick = onSignUp, enabled = !isLoading) {
                Text(
                    buildAnnotatedString {
                        withStyle(SpanStyle(color = AuthColors.TextLight)) {
                            append("Don't have an admin account? ")
                        }
                        withStyle(
                            SpanStyle(
                                color      = AuthColors.PrimaryBlue,
                                fontWeight = FontWeight.SemiBold,
                            ),
                        ) { append("Sign Up") }
                    },
                )
            }
        }
    }
}
