package com.example.uniwattelektrik.core.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.example.uniwattelektrik.core.theme.AppTheme

/**
 * Premium 64 dp circular gradient FAB used across the app.
 *
 *   • brand→deep-brand linear gradient
 *   • 3 dp white border, 20 dp brand-tinted shadow
 *   • Aligned to bottom-end with safe-area-aware nav-bar inset + 100 dp bottom
 *     padding so it never sits behind the admin bottom bar.
 *
 * Place inside a Box that fills the screen. By default renders a "+" icon.
 */
@Composable
fun BoxScope.PremiumGradientFab(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Filled.Add,
    contentDescription: String? = "Add",
    bottomPadding: androidx.compose.ui.unit.Dp = 100.dp,
    endPadding: androidx.compose.ui.unit.Dp = 24.dp,
    alignment: Alignment = Alignment.BottomEnd,
) {
    val sidePad =
        if (alignment == Alignment.BottomStart)
            Modifier.padding(start = endPadding, bottom = bottomPadding)
        else
            Modifier.padding(end = endPadding, bottom = bottomPadding)

    Box(
        modifier = modifier
            .align(alignment)
            .windowInsetsPadding(WindowInsets.navigationBars)
            .then(sidePad)
            .size(64.dp)
            .shadow(20.dp, CircleShape, spotColor = AppTheme.Brand.copy(alpha = 0.6f))
            .clip(CircleShape)
            .background(Brush.linearGradient(listOf(AppTheme.Brand, AppTheme.Brand700)))
            .border(3.dp, Color.White, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = Color.White,
            modifier = Modifier.size(28.dp),
        )
    }
}

