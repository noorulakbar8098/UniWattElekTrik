package com.example.uniwattelektrik.feature.auth.presentation.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.uniwattelektrik.feature.auth.presentation.theme.AuthColors

/** Bottom auth-screen footer: divider — text — divider. */
@Composable
fun NeedHelpFooter(modifier: Modifier = Modifier, text: String = "Need help?") {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        HorizontalDivider(modifier = Modifier.weight(1f), color = AuthColors.Divider)
        Text(
            "  $text  ",
            color = AuthColors.TextLight,
            style = MaterialTheme.typography.labelMedium,
        )
        HorizontalDivider(modifier = Modifier.weight(1f), color = AuthColors.Divider)
    }
}

