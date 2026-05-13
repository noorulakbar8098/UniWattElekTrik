package com.example.uniwattelektrik.core.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

/**
 * Shared surface tokens + Modifier helpers -- enterprise design system.
 *
 * Rules:
 *  - Flat neutral page background -- no decorative multi-stop gradients
 *  - Cards = plain white + single subtle neutral shadow + faint border
 *  - No coloured glows, no blue-tinted shadows on surfaces
 *  - Gradients reserved for the top header bar only (PremiumHeaderBackground)
 */

// Backward-compat aliases used by screens that import ScreenBg0-4
val ScreenBg0 = AppTheme.Bg
val ScreenBg1 = AppTheme.Bg
val ScreenBg2 = AppTheme.Bg
val ScreenBg3 = AppTheme.Bg
val ScreenBg4 = AppTheme.BgSecondary

// Card surface (plain white)
val CardTop    = AppTheme.Surface
val CardMid    = AppTheme.Surface
val CardBottom = AppTheme.Surface

// Shadow tokens
val SoftShadow1 = AppTheme.ShadowMd       // standard card shadow
val SoftShadow2 = AppTheme.ShadowSm       // contact shadow
val GlowShadow  = Color.Transparent       // no coloured glow

val CardBorder  = AppTheme.Ink100         // subtle grey divider line
val CardInnerHi = Color.Transparent       // no inner highlight

/**
 * Premium screen background — a barely-perceptible 3-stop mesh that gives
 * the canvas atmospheric depth without competing with content. The top of
 * the screen picks up a faint blue cast (subliminal ambient light), the
 * middle warms slightly toward the neutral Bg, and the bottom sits a touch
 * cooler so cards look "lifted" against it.
 *
 * Magnitude is intentionally tiny — under 2 % luminance variance across the
 * full screen — so accent surfaces (cards, headers) keep visual priority.
 */
fun appScreenBackground(): Brush = Brush.verticalGradient(
    colors = listOf(
        Color(0xFFF7F9FD),   // faint blue ambient at the top
        AppTheme.Bg,          // base neutral
        Color(0xFFF1F4FA),   // gentle cool-down at the bottom
    ),
)

/**
 * Clean card -- white surface, 2 dp shadow, 0.5 dp border.
 */
fun Modifier.cleanCard(shape: Shape): Modifier = this
    .shadow(elevation = 2.dp, shape = shape, spotColor = SoftShadow1, ambientColor = SoftShadow2)
    .clip(shape)
    .background(AppTheme.Surface)
    .border(width = 0.5.dp, color = AppTheme.Ink100, shape = shape)

/** Backward compat alias -- delegates to cleanCard. */
fun Modifier.premiumCard(shape: Shape): Modifier = cleanCard(shape)
