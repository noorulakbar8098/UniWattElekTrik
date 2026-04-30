package com.example.uniwattelektrik.feature.auth.presentation.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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

/**
 * Admin **account creation**. Wires to `SignUpUseCase` → Firebase
 * `createUserWithEmailAndPassword`. The form scrolls because there are 4 fields.
 */
@Composable
fun AdminSignUpScreen(
    viewModel: AuthViewModel,
    onBack: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val fullName by viewModel.fullName.collectAsStateWithLifecycle()
    val email by viewModel.email.collectAsStateWithLifecycle()
    val password by viewModel.password.collectAsStateWithLifecycle()
    val confirmPassword by viewModel.confirmPassword.collectAsStateWithLifecycle()
    val passwordVisible by viewModel.passwordVisible.collectAsStateWithLifecycle()

    val isLoading = state is AuthUiState.Loading
    val errorMessage = (state as? AuthUiState.Error)?.message
    val canSubmit = !isLoading &&
        fullName.length >= 2 &&
        email.isNotBlank() &&
        password.length >= 6 &&
        confirmPassword == password

    AuthBackground {
        // Back button at top-start
        IconButton(
            onClick = onBack,
            enabled = !isLoading,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(top = 24.dp),
        ) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = AuthColors.TextDark,
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(vertical = 64.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            BrandHeader()
            Spacer(Modifier.height(24.dp))

            Text(
                "Create Admin Account",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = AuthColors.TextDark,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Start,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "Set up a new operations account",
                style = MaterialTheme.typography.bodyMedium,
                color = AuthColors.TextLight,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Start,
            )

            Spacer(Modifier.height(20.dp))

            AuthInputField(
                value = fullName,
                onValueChange = { viewModel.onEvent(AuthUiEvent.FullNameChanged(it)) },
                placeholder = "Full Name",
                leadingIcon = Icons.Filled.Badge,
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Next,
                enabled = !isLoading,
            )
            Spacer(Modifier.height(12.dp))
            AuthInputField(
                value = email,
                onValueChange = { viewModel.onEvent(AuthUiEvent.EmailChanged(it)) },
                placeholder = "Admin Email",
                leadingIcon = Icons.Filled.Email,
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next,
                enabled = !isLoading,
            )
            Spacer(Modifier.height(12.dp))
            AuthInputField(
                value = password,
                onValueChange = { viewModel.onEvent(AuthUiEvent.PasswordChanged(it)) },
                placeholder = "Password (min 6 chars)",
                leadingIcon = Icons.Filled.Lock,
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Next,
                enabled = !isLoading,
                visualTransformation =
                    if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
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
            Spacer(Modifier.height(12.dp))
            AuthInputField(
                value = confirmPassword,
                onValueChange = { viewModel.onEvent(AuthUiEvent.ConfirmPasswordChanged(it)) },
                placeholder = "Confirm Password",
                leadingIcon = Icons.Filled.Lock,
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done,
                enabled = !isLoading,
                visualTransformation =
                    if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            )

            if (errorMessage != null) {
                Spacer(Modifier.height(12.dp))
                Text(
                    errorMessage,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.fillMaxWidth(),
                )
            } else if (confirmPassword.isNotEmpty() && confirmPassword != password) {
                Spacer(Modifier.height(12.dp))
                Text(
                    "Passwords do not match.",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Spacer(Modifier.height(20.dp))
            GradientButton(
                text = "Create Admin Account",
                isLoading = isLoading,
                enabled = canSubmit,
                onClick = { viewModel.onEvent(AuthUiEvent.SignUp) },
            )

            Spacer(Modifier.height(16.dp))
            TextButton(onClick = onBack, enabled = !isLoading) {
                Text(
                    buildAnnotatedString {
                        withStyle(SpanStyle(color = AuthColors.TextLight)) {
                            append("Already have an account? ")
                        }
                        withStyle(
                            SpanStyle(
                                color = AuthColors.PrimaryBlue,
                                fontWeight = FontWeight.SemiBold,
                            ),
                        ) { append("Sign In") }
                    },
                )
            }
        }
    }
}

