package com.example.uniwattelektrik.feature.auth.presentation.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Premium gradient card surface used by the check-in card and other "hero" surfaces.
 *
 * Layered effects:
 *   1. Diagonal linear gradient (top-left bright blue → bottom-right deep navy)
 *   2. Soft radial glow in the top-right corner
 *   3. Subtle white glow at the bottom for lift
 *   4. Tiny dark overlay at the bottom for inner depth
 *   5. Optional translucent glass overlay near the top-left for sheen
 *
 * Use it like a `Surface`/`Box`: pass your card content as [content].
 */
@Composable
fun PremiumCheckInCard(
    modifier: Modifier = Modifier,
    cornerRadius: androidx.compose.ui.unit.Dp = 28.dp,
    gradientStart: Color = Color(0xFF3A8DFF),
    gradientEnd: Color = Color(0xFF0A3D91),
    content: @Composable androidx.compose.foundation.layout.BoxScope.() -> Unit,
) {
    val shape = RoundedCornerShape(cornerRadius)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 22.dp,
                shape = shape,
                spotColor = Color(0x401E73E8),
            )
            .clip(shape)
            // 1. Base diagonal gradient
            .background(Brush.linearGradient(listOf(gradientStart, gradientEnd))),
    ) {
        // 2. Soft radial glow — top-right corner
        Box(
            modifier = Modifier
                .size(260.dp)
                .align(Alignment.TopEnd)
                .offset(x = 90.dp, y = (-90).dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.18f),
                            Color.White.copy(alpha = 0.06f),
                            Color.Transparent,
                        ),
                    ),
                ),
        )

        // 5. Glass sheen — top-left
        Box(
            modifier = Modifier
                .size(180.dp)
                .align(Alignment.TopStart)
                .offset(x = (-50).dp, y = (-50).dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.10f),
                            Color.Transparent,
                        ),
                    ),
                ),
        )

        // 3. Subtle white glow at the bottom
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .size(160.dp)
                .align(Alignment.BottomCenter)
                .offset(y = 60.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.08f),
                            Color.Transparent,
                        ),
                    ),
                ),
        )

        // 4. Inner depth — slight dark overlay at the very bottom
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .size(120.dp)
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.08f)),
                    ),
                ),
        )

        // Caller content draws on top of all decorative layers
        content()
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  STATUS INDICATORS
// ─────────────────────────────────────────────────────────────────────────────

/**
 * A small live-status dot that pulses outward continuously while [active] is true.
 *
 * - Solid centre dot (10dp by default) in [color].
 * - One or two outer rings expand from 1× → ~2.4× while fading to 0, looped.
 * - When [active] = false, only the static dot is drawn (no animation).
 */
@Composable
fun PulsingStatusDot(
    color: Color,
    modifier: Modifier = Modifier,
    active: Boolean = true,
    dotSize: androidx.compose.ui.unit.Dp = 10.dp,
) {
    Box(
        modifier = modifier.size(dotSize * 2.6f),
        contentAlignment = Alignment.Center,
    ) {
        if (active) {
            val transition = rememberInfiniteTransition(label = "pulse")
            // Two staggered rings produce a smoother, premium feel.
            val scale1 by transition.animateFloat(
                initialValue = 1f,
                targetValue = 2.4f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 1400, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart,
                ),
                label = "scale1",
            )
            val alpha1 by transition.animateFloat(
                initialValue = 0.55f,
                targetValue = 0f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 1400, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart,
                ),
                label = "alpha1",
            )
            val scale2 by transition.animateFloat(
                initialValue = 1f,
                targetValue = 2.0f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 1400, easing = LinearEasing, delayMillis = 400),
                    repeatMode = RepeatMode.Restart,
                ),
                label = "scale2",
            )
            val alpha2 by transition.animateFloat(
                initialValue = 0.35f,
                targetValue = 0f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 1400, easing = LinearEasing, delayMillis = 400),
                    repeatMode = RepeatMode.Restart,
                ),
                label = "alpha2",
            )

            Box(
                modifier = Modifier
                    .size(dotSize)
                    .scale(scale1)
                    .clip(CircleShape)
                    .background(color.copy(alpha = alpha1)),
            )
            Box(
                modifier = Modifier
                    .size(dotSize)
                    .scale(scale2)
                    .clip(CircleShape)
                    .background(color.copy(alpha = alpha2)),
            )
        }

        // Solid centre dot — drawn on top of the rings
        Box(
            modifier = Modifier
                .size(dotSize)
                .clip(CircleShape)
                .background(color),
        )
    }
}

/**
 * Drop-in status indicator for the check-in card.
 *
 *  - `isCheckedIn = true`  → green pulsing dot + "Checked-in · Active" (or your custom suffix)
 *  - `isCheckedIn = false` → solid red dot, no animation + "Checked-out"
 *
 * Smoothly cross-fades between states so there's no flicker.
 */
@Composable
fun StatusIndicator(
    isCheckedIn: Boolean,
    modifier: Modifier = Modifier,
    activeText: String = "Checked-in · Active",
    inactiveText: String = "Checked-out",
    textColor: Color = Color.White,
    activeColor: Color = Color(0xFF34D399),
    inactiveColor: Color = Color(0xFFEF4444),
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        AnimatedContent(
            targetState = isCheckedIn,
            transitionSpec = {
                (fadeIn(tween(220)) togetherWith fadeOut(tween(180)))
            },
            label = "status-dot",
        ) { checkedIn ->
            PulsingStatusDot(
                color = if (checkedIn) activeColor else inactiveColor,
                active = checkedIn,
            )
        }

        AnimatedContent(
            targetState = isCheckedIn,
            transitionSpec = {
                (fadeIn(tween(260)) togetherWith fadeOut(tween(180)))
            },
            label = "status-text",
        ) { checkedIn ->
            Text(
                text = if (checkedIn) activeText else inactiveText,
                color = textColor,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

