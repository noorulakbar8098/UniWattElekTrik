package com.example.uniwattelektrik.core.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// Enterprise header palette -- deep navy to brand blue (gradient allowed for headers only)
private val HeaderStart = Color(0xFF0F2B6B)
private val HeaderEnd   = Color(0xFF1A6BF5)

/** Status-bar tint matches the top of [PremiumHeaderBackground]. */
val PremiumHeaderStatusBarColor: Color = HeaderStart

/**
 * App-wide screen header background.
 *
 * Clean diagonal gradient (navy to blue) with a single subtle light arc.
 * No radial glows, no heavy decorative rings -- enterprise grade.
 */
@Composable
fun PremiumHeaderBackground(
    modifier: Modifier = Modifier,
    roundedBottom: Boolean = true,
    cornerRadius: Dp = 24.dp,
    content: @Composable BoxScope.() -> Unit,
) {
    val shape = if (roundedBottom)
        RoundedCornerShape(bottomStart = cornerRadius, bottomEnd = cornerRadius)
    else RoundedCornerShape(0.dp)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(
                Brush.linearGradient(
                    colors = listOf(HeaderStart, HeaderEnd),
                    start  = Offset(0f, 0f),
                    end    = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY),
                ),
            )
            .drawBehind {
                // Single soft arc -- top-right atmospheric highlight
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0x18FFFFFF), Color(0x00FFFFFF)),
                        center = Offset(size.width * 0.88f, 0f),
                        radius = size.width * 0.60f,
                    ),
                    center = Offset(size.width * 0.88f, 0f),
                    radius = size.width * 0.60f,
                )
                // One decorative outline ring -- bottom left
                drawCircle(
                    color  = Color(0x10FFFFFF),
                    center = Offset(size.width * 0.10f, size.height * 0.90f),
                    radius = size.width * 0.40f,
                    style  = Stroke(width = 1.dp.toPx()),
                )
            },
        content = content,
    )
}
