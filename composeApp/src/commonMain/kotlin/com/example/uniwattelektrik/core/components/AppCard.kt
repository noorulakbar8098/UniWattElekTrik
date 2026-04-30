package com.example.uniwattelektrik.core.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.uniwattelektrik.core.theme.AppTheme

/**
 * Standard white card surface used everywhere. Elevation defaults to a soft
 * 10 dp navy-tinted shadow which matches the reference Figma's `--shadow`.
 */
@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = AppTheme.RadiusLg,
    elevation: Dp = 10.dp,
    shadowColor: Color = AppTheme.ShadowSpotMedium,
    background: Color = AppTheme.Surface,
    contentPadding: Dp = AppTheme.Sp4,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val shape = RoundedCornerShape(cornerRadius)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .shadow(elevation = elevation, shape = shape, spotColor = shadowColor)
            .clip(shape)
            .background(background)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(contentPadding),
    ) { content() }
}
