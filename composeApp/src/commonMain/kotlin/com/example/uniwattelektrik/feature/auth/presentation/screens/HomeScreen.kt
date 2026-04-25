package com.example.uniwattelektrik.feature.auth.presentation.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.uniwattelektrik.feature.auth.domain.model.User
import com.example.uniwattelektrik.feature.auth.presentation.state.AuthUiEvent
import com.example.uniwattelektrik.feature.auth.presentation.viewmodel.AuthViewModel

@Composable
fun HomeScreen(user: User, viewModel: AuthViewModel) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Welcome 👷", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(8.dp))
        Text(user.displayName ?: user.email, style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(4.dp))
        Text(user.email, style = MaterialTheme.typography.labelSmall)

        Spacer(Modifier.height(32.dp))
        Text(
            "You're signed in. The next features (Tasks, Check-in, Logs, Support) will appear here.",
            style = MaterialTheme.typography.bodyMedium,
        )

        Spacer(Modifier.height(32.dp))
        OutlinedButton(onClick = { viewModel.onEvent(AuthUiEvent.Logout) }) {
            Text("Logout")
        }
    }
}

