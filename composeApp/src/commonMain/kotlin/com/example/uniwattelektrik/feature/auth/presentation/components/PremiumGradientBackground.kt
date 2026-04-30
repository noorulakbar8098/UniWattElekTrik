package com.example.uniwattelektrik.feature.auth.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.uniwattelektrik.core.theme.ScreenBg0
import com.example.uniwattelektrik.core.theme.SetStatusBar
import com.example.uniwattelektrik.core.theme.appScreenBackground

/**
 * Auth-screen background. Thin wrapper around the shared [appScreenBackground]
 * weave so every screen — auth, employee dashboard, admin — shares the exact
 * same "blue · white · dark-blue" gradient as the Admin Home dashboard.
 */
@Composable
fun PremiumGradientBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    SetStatusBar(color = ScreenBg0, darkIcons = true)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(appScreenBackground()),
        content = content,
    )
}
