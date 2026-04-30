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

/* ── Shared header palette — used by every screen-level header ──────────── */
private val HeaderGradTopLeft     = Color(0xFF3A6BFF)
private val HeaderGradMid         = Color(0xFF2F5BEA)
private val HeaderGradBottomRight = Color(0xFF244EDC)

/** Status-bar tint that exactly matches the top of [PremiumHeaderBackground]. */
val PremiumHeaderStatusBarColor: Color = HeaderGradTopLeft

/**
 * The single source-of-truth header background used across the entire app.
 *
 * Composed of:
 *  • A diagonal 3-stop gradient (top-left → bottom-right)
 *  • A primary "sunlight" radial glow at the top-left
 *  • A secondary soft glow slightly offset
 *  • A subtle radial highlight in the top-right corner
 *  • Three decorative outline circles (atmospheric "vector" rings)
 *  • Optional rounded bottom corners
 *
 * Place this at the top of any screen — pass back-button / title /
 * actions into [content] so each screen keeps its own controls while
 * sharing the same premium fintech look.
 *
 * If [roundedBottom] is `false`, the background paints as a flat strip
 * (used by full-screen flows like the multi-step forms).
 */
@Composable
fun PremiumHeaderBackground(
    modifier: Modifier = Modifier,
    roundedBottom: Boolean = true,
    cornerRadius: Dp = 30.dp,
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
                    colorStops = arrayOf(
                        0.0f  to HeaderGradTopLeft,
                        0.55f to HeaderGradMid,
                        1.0f  to HeaderGradBottomRight,
                    ),
                    start = Offset(0f, 0f),
                    end   = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY),
                ),
            )
            .drawBehind {
                // 1. Primary light glow — top-left "sunlight" source.
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0x33FFFFFF), Color(0x00FFFFFF)),
                        center = Offset(size.width * 0.05f, size.height * 0.05f),
                        radius = size.width * 1.10f,
                    ),
                    center = Offset(size.width * 0.05f, size.height * 0.05f),
                    radius = size.width * 1.10f,
                )
                // 2. Secondary soft glow — slightly offset.
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0x1AFFFFFF), Color(0x00FFFFFF)),
                        center = Offset(size.width * 0.30f, size.height * 0.18f),
                        radius = size.width * 0.65f,
                    ),
                    center = Offset(size.width * 0.30f, size.height * 0.18f),
                    radius = size.width * 0.65f,
                )

                // 3. Decorative atmospheric outline rings.
                drawCircle(
                    color  = Color(0x14FFFFFF),
                    center = Offset(size.width * 0.18f, size.height * 0.45f),
                    radius = size.width * 0.55f,
                    style  = Stroke(width = 1.2.dp.toPx()),
                )
                drawCircle(
                    color  = Color(0x0FFFFFFF),
                    center = Offset(size.width * (-0.05f), size.height * 0.78f),
                    radius = size.width * 0.42f,
                    style  = Stroke(width = 1.dp.toPx()),
                )
                drawCircle(
                    color  = Color(0x14FFFFFF),
                    center = Offset(size.width * 0.92f, size.height * (-0.10f)),
                    radius = size.width * 0.42f,
                    style  = Stroke(width = 1.2.dp.toPx()),
                )

                // 4. Faint top-right radial highlight.
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0x0DFFFFFF), Color(0x00FFFFFF)),
                        center = Offset(size.width * 0.95f, size.height * 0.10f),
                        radius = size.width * 0.55f,
                    ),
                    center = Offset(size.width * 0.95f, size.height * 0.10f),
                    radius = size.width * 0.55f,
                )
            },
        content = content,
    )
}
