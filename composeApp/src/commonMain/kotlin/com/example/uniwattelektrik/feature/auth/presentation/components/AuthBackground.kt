package com.example.uniwattelektrik.feature.auth.presentation.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Shared scaffold for all Auth screens.
 *
 * [PremiumGradientBackground] fills the full screen — padding is applied only
 * to the inner content Box so the gradients always bleed edge-to-edge.
 */
@Composable
fun AuthBackground(
    content: @Composable BoxScope.() -> Unit,
) {
    PremiumGradientBackground {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            content = content,
        )
    }
}
