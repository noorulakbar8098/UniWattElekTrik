package com.example.uniwattelektrik.core.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.uniwattelektrik.core.theme.AppElevation
import com.example.uniwattelektrik.core.theme.AppShapes
import com.example.uniwattelektrik.core.theme.AppTheme

/**
 * Standard card surface — backward-compatible wrapper over design system tokens.
 *
 * Premium polish pass: the card is no longer a flat white rectangle. It now
 * sits on a vertical "top-light" gradient (a hairline lighter at the top
 * fading into the base surface) so each card picks up a subtle highlight
 * along its upper edge, with a hairline ink-100 border to keep the silhouette
 * crisp against the new mesh page background. Shadows are tinted Ink-navy
 * (see AppTheme.ShadowMd) so elevated cards feel ambient-lit rather than
 * stamped onto a flat page.
 *
 * For new screens prefer [DsCard] which applies press animation, shape, and
 * elevation tokens automatically. This composable keeps existing call-sites unchanged.
 */
@Composable
fun AppCard(
    modifier      : Modifier = Modifier,
    cornerRadius  : Dp = AppTheme.RadiusXl,         // unchanged API
    elevation     : Dp = AppElevation.card,
    shadowColor   : Color = AppTheme.ShadowMd,
    background    : Color = AppTheme.Surface,
    contentPadding: Dp = AppTheme.SpLg,
    onClick       : (() -> Unit)? = null,
    content       : @Composable () -> Unit,
) {
    // Subtle top-light surface: a near-white highlight on the top edge fades
    // into the supplied background. When the caller passes a tinted surface
    // (e.g. AppTheme.Brand50), the highlight still sits naturally on top.
    val surfaceBrush = Brush.verticalGradient(
        colors = listOf(
            Color.White.copy(alpha = if (background == AppTheme.Surface) 0.6f else 0.18f),
            background,
        ),
    )
    Box(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation    = elevation,
                shape        = AppShapes.card,
                ambientColor = shadowColor,
                spotColor    = shadowColor,
            )
            .clip(AppShapes.card)
            .background(background)            // base fill (in case the brush below is translucent)
            .background(surfaceBrush)          // top-light overlay
            .border(0.5.dp, AppTheme.Ink100, AppShapes.card)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(contentPadding),
    ) { content() }
}
