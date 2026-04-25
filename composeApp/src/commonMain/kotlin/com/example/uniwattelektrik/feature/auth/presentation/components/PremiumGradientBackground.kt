package com.example.uniwattelektrik.feature.auth.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// ── Palette ──────────────────────────────────────────────────────────────────

/** Layer 1 — base vertical gradient stops */
private val BaseTop    = Color(0xFFEAF2FF)   // very light blue
private val BaseMid    = Color(0xFFF5F8FF)   // near-white with blue tint
private val BaseBottom = Color(0xFFFFFFFF)   // pure white

/** Layer 2 — radial glow: #1E73E8 at 20 % opacity → transparent */
private val GlowCenter      = Color(0x331E73E8)  // 0x33 ≈ 20 %
private val GlowTransparent = Color(0x001E73E8)  // fully transparent, same hue

/** Layer 3 — depth overlay: transparent → #0A1F44 at 3 % opacity */
private val DepthStart = Color(0x000A1F44)   // 0 % navy
private val DepthEnd   = Color(0x080A1F44)   // ~3 % navy  (0x08 / 255 ≈ 0.031)

// ── Composable ────────────────────────────────────────────────────────────────

/**
 * Three-layer premium gradient background used by all Auth screens.
 *
 *   Layer 1  Vertical gradient  EAF2FF → F5F8FF → FFFFFF   (soft base)
 *   Layer 2  Radial glow        Top-center blue glow        (fintech depth)
 *   Layer 3  Depth overlay      Transparent → navy 3 %      (premium shadow)
 *
 * Usage:
 * ```
 * PremiumGradientBackground {
 *     // your screen content here
 * }
 * ```
 */
@Composable
fun PremiumGradientBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {

        val w = constraints.maxWidth.toFloat()
        val h = constraints.maxHeight.toFloat()

        // ── Layer 1: Base vertical gradient ──────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colorStops = arrayOf(
                            0.00f to BaseTop,
                            0.38f to BaseMid,
                            1.00f to BaseBottom,
                        ),
                    )
                )
        )

        // ── Layer 2: Top-center radial glow ───────────────────────────────
        // Centre sits just above the visible area so the glow spills downward
        // from the top edge, covering roughly the top 40 % of the screen.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colorStops = arrayOf(
                            0.00f to GlowCenter,
                            0.55f to Color(0x141E73E8),   // ~8 % — mid fade
                            1.00f to GlowTransparent,
                        ),
                        center = Offset(x = w / 2f, y = -h * 0.05f),
                        radius = w * 0.90f,
                    )
                )
        )

        // ── Layer 3: Subtle depth overlay ────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colorStops = arrayOf(
                            0.00f to DepthStart,
                            1.00f to DepthEnd,
                        ),
                    )
                )
        )

        // ── Content ───────────────────────────────────────────────────────
        Box(modifier = Modifier.fillMaxSize(), content = content)
    }
}
