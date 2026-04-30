package com.example.uniwattelektrik.feature.auth.presentation.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.uniwattelektrik.feature.auth.presentation.components.AuthInputField
import com.example.uniwattelektrik.feature.auth.presentation.components.GradientButton
import com.example.uniwattelektrik.feature.auth.presentation.theme.AuthColors
import com.example.uniwattelektrik.feature.auth.presentation.viewmodel.PasswordResetViewModel

/**
 * Bottom-sheet shown when the user taps "Create New Password" on the login
 * screen. Drives [PasswordResetViewModel] which performs the rotation on a
 * secondary Firebase app so it never disturbs the admin's session (if any).
 *
 * Closes itself once [PasswordResetViewModel.State.Success] is reached.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateNewPasswordSheet(
    viewModel: PasswordResetViewModel,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val state by viewModel.state.collectAsStateWithLifecycle()

    var email      by remember { mutableStateOf("") }
    var currentPwd by remember { mutableStateOf("") }
    var newPwd     by remember { mutableStateOf("") }
    var confirmPwd by remember { mutableStateOf("") }

    val isLoading = state is PasswordResetViewModel.State.Loading
    val errorMsg  = (state as? PasswordResetViewModel.State.Error)?.message
    val mismatch  = newPwd.isNotEmpty() && confirmPwd.isNotEmpty() && newPwd != confirmPwd
    val canSubmit = !isLoading
        && email.isNotBlank()
        && currentPwd.length >= 6
        && newPwd.length >= 6
        && newPwd == confirmPwd

    // Auto-dismiss on success.
    LaunchedEffect(state) {
        if (state is PasswordResetViewModel.State.Success) {
            onDismiss()
            viewModel.reset()
        }
    }

    ModalBottomSheet(
        onDismissRequest = { viewModel.reset(); onDismiss() },
        sheetState = sheetState,
        containerColor = Color.White,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 8.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Create New Password",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = AuthColors.TextDark,
            )
            Text(
                text = "First-time setup only. Use the email and temporary password your admin shared with you, then choose a password of your own.",
                style = MaterialTheme.typography.bodySmall,
                color = AuthColors.TextLight,
            )
            Spacer(Modifier.height(4.dp))

            AuthInputField(
                value = email,
                onValueChange = { email = it },
                placeholder = "Work email",
                leadingIcon = Icons.Filled.Email,
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next,
                enabled = !isLoading,
            )
            AuthInputField(
                value = currentPwd,
                onValueChange = { currentPwd = it },
                placeholder = "Temporary password",
                leadingIcon = Icons.Filled.Lock,
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Next,
                enabled = !isLoading,
                visualTransformation = PasswordVisualTransformation(),
            )
            AuthInputField(
                value = newPwd,
                onValueChange = { newPwd = it },
                placeholder = "New password (min 6 characters)",
                leadingIcon = Icons.Filled.Lock,
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Next,
                enabled = !isLoading,
                visualTransformation = PasswordVisualTransformation(),
            )
            AuthInputField(
                value = confirmPwd,
                onValueChange = { confirmPwd = it },
                placeholder = "Confirm new password",
                leadingIcon = Icons.Filled.Lock,
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done,
                enabled = !isLoading,
                visualTransformation = PasswordVisualTransformation(),
            )

            if (mismatch) {
                Text(
                    "Passwords don't match.",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            errorMsg?.let {
                Text(it, color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall)
            }

            Spacer(Modifier.height(4.dp))
            GradientButton(
                text = "Set my password",
                isLoading = isLoading,
                enabled = canSubmit,
                onClick = { viewModel.submit(email.trim(), currentPwd, newPwd) },
            )
        }
    }
}

