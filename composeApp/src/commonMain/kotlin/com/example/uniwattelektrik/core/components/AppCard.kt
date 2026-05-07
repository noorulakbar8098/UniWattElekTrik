package com.example.uniwattelektrik.core.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import com.example.uniwattelektrik.core.theme.AppElevation
import com.example.uniwattelektrik.core.theme.AppShapes
import com.example.uniwattelektrik.core.theme.AppTheme

/**
 * Standard card surface — backward-compatible wrapper over design system tokens.
 *
 * For new screens prefer [DsCard] which applies press animation, shape, and
 * elevation tokens automatically. This composable keeps existing call-sites unchanged.
 */
@Composable
fun AppCard(
    modifier      : Modifier = Modifier,
    cornerRadius  : Dp = AppTheme.RadiusXl,         // was AppTheme.RadiusLg
    elevation     : Dp = AppElevation.card,          // 10 dp — unchanged
    shadowColor   : Color = AppTheme.ShadowMd,       // was ShadowSpotMedium
    background    : Color = AppTheme.Surface,
    contentPadding: Dp = AppTheme.SpLg,              // was Sp4 = 16 dp — same value
    onClick       : (() -> Unit)? = null,
    content       : @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .shadow(elevation = elevation, shape = AppShapes.card, spotColor = shadowColor)
            .clip(AppShapes.card)
            .background(background)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(contentPadding),
    ) { content() }
}
