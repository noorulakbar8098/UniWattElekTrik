package com.example.uniwattelektrik.feature.auth.presentation.screens

import com.example.uniwattelektrik.core.performance.TrackScreenPerformance

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.uniwattelektrik.di.AppContainer
import com.example.uniwattelektrik.feature.auth.presentation.components.*
import com.example.uniwattelektrik.feature.auth.presentation.state.AuthUiEvent
import com.example.uniwattelektrik.feature.auth.presentation.state.AuthUiState
import com.example.uniwattelektrik.feature.auth.presentation.theme.AuthColors
import com.example.uniwattelektrik.feature.auth.presentation.viewmodel.AuthViewModel

@Composable
fun LoginScreen(
    viewModel: AuthViewModel,
    onSwitchToAdmin: () -> Unit,
) {
    TrackScreenPerformance("LoginScreen")
    val state by viewModel.state.collectAsStateWithLifecycle()
    val email by viewModel.email.collectAsStateWithLifecycle()
    val password by viewModel.password.collectAsStateWithLifecycle()
    val passwordVisible by viewModel.passwordVisible.collectAsStateWithLifecycle()
    val keepSignedIn by viewModel.keepSignedIn.collectAsStateWithLifecycle()

    val isLoading    = state is AuthUiState.Loading
    val errorMessage = (state as? AuthUiState.Error)?.message
    val canSubmit    = !isLoading && email.isNotBlank() && password.length >= 6

    var showCreatePwd by remember { mutableStateOf(false) }
    val resetVm = remember { AppContainer.createPasswordResetViewModel() }

    AuthBackground {
        // ── Brand header + toggle + form card (centred vertically) ────────
        Column(
            modifier             = Modifier.align(Alignment.Center).fillMaxWidth(),
            horizontalAlignment  = Alignment.CenterHorizontally,
        ) {
            BrandHeader()
            Spacer(Modifier.height(16.dp))

            UserAdminToggleBordered(
                selected   = AuthRole.User,
                onSelected = {
                    if (it == AuthRole.Admin) {
                        viewModel.clearForm()
                        onSwitchToAdmin()
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
                        elevation   = 18.dp,
                        shape       = RoundedCornerShape(28.dp),
                        spotColor   = Color(0x280A1F44),
                        ambientColor = Color(0x140A1F44),
                    )
                    .clip(RoundedCornerShape(28.dp))
                    .background(Color.White)
                    .padding(24.dp),
            ) {
                Text(
                    text       = "Welcome back 👋",
                    style      = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color      = AuthColors.TextDark,
                    modifier   = Modifier.fillMaxWidth(),
                    textAlign  = TextAlign.Start,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text  = "Sign in to start your shift.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = AuthColors.TextLight,
                    modifier  = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Start,
                )

                Spacer(Modifier.height(20.dp))

                AuthInputField(
                    value         = email,
                    onValueChange = { viewModel.onEvent(AuthUiEvent.EmailChanged(it)) },
                    placeholder   = "Employee ID / Email",
                    leadingIcon   = Icons.Filled.Person,
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
                    onCreateNewPassword = { showCreatePwd = true },
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
                    text      = "Sign In",
                    isLoading = isLoading,
                    enabled   = canSubmit,
                    onClick   = { viewModel.onEvent(AuthUiEvent.SignIn) },
                )
            }
        }

        NeedHelpFooter(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 24.dp),
        )
    }

    if (showCreatePwd) {
        CreateNewPasswordSheet(
            viewModel = resetVm,
            onDismiss = { showCreatePwd = false },
        )
    }
}

/** Reusable "Keep me signed in" + "Create New Password" + "Forgot password?" row. */
@Composable
internal fun KeepMeRow(
    checked: Boolean,
    onCheckedChange: () -> Unit,
    enabled: Boolean,
    onCreateNewPassword: () -> Unit = {},
    onForgotPassword: () -> Unit = {},
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier          = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            KeepMeSignedInCheckbox(
                checked         = checked,
                onCheckedChange = { onCheckedChange() },
                enabled         = enabled,
            )
            Spacer(Modifier.weight(1f))
            TextButton(
                onClick        = onForgotPassword,
                enabled        = enabled,
                contentPadding = PaddingValues(horizontal = 4.dp),
            ) {
                Text(
                    "Forgot password?",
                    color      = AuthColors.PrimaryBlue,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
        // First-time setup: lets a freshly-onboarded employee turn the temp
        // password their admin shared into a password of their own.
        // Succeeds exactly once per account; afterwards "Forgot password" is
        // the path forward.
        TextButton(
            onClick = onCreateNewPassword,
            enabled = enabled,
            modifier = Modifier.align(Alignment.End),
            contentPadding = PaddingValues(horizontal = 4.dp),
        ) {
            Text(
                "Create New Password (first-time setup)",
                color      = AuthColors.PrimaryBlue,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}
