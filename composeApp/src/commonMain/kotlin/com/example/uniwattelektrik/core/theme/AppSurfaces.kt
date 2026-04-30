package com.example.uniwattelektrik.core.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

/**
 * Shared surface tokens + helpers used app-wide so every screen has the same
 * "blue · white · dark-blue" gradient background and the same premium glass
 * card style as the Admin Home dashboard.
 */

/* ── Whole-screen gradient stops (very light blue → lavender → cool depth) ── */
val ScreenBg0 = Color(0xFFF4F6FF)
val ScreenBg1 = Color(0xFFE3E9FF)
val ScreenBg2 = Color(0xFFDDE6F8)
val ScreenBg3 = Color(0xFFD2DBF0)
val ScreenBg4 = Color(0xFFC4CFE6)

/* ── Premium frosted-glass card surface ──────────────────────────────────── */
val CardTop    = Color(0xFFFFFFFF)
val CardMid    = Color(0xFFF7F9FF)
val CardBottom = Color(0xFFEEF2FF)

/* ── Layered soft shadow system + edge highlight ─────────────────────────── */
val SoftShadow1 = Color(0x14000000)   // 8 % black · main soft  (0 12 30)
val SoftShadow2 = Color(0x0D000000)   // 5 % black · contact    (0  4 10)
val GlowShadow  = Color(0x0F3A8DFF)   // 6 % brand blue ambient (0  0 40)
val CardBorder  = Color(0xFFFFFFFF)   // solid white edge
val CardInnerHi = Color(0xB3FFFFFF)   // 70 % white inner top highlight

/** Single source-of-truth screen background brush. Use everywhere instead of
 *  flat `Color(...)` fills so all screens look unified. */
fun appScreenBackground(): Brush = Brush.verticalGradient(
    colorStops = arrayOf(
        0.00f to ScreenBg0,
        0.25f to ScreenBg1,
        0.50f to ScreenBg2,
        0.75f to ScreenBg3,
        1.00f to ScreenBg4,
    ),
)

/**
 * Premium card modifier — 3-layer soft shadow + frosted-glass gradient
 * surface + white edge border + inner top highlight. Apply to ANY card.
 *
 *     Box(modifier = Modifier.premiumCard(RoundedCornerShape(20.dp)).padding(16.dp))
 */
fun Modifier.premiumCard(shape: Shape): Modifier = this
    // Layer 3 — wide brand-blue ambient halo (painted first / bottom)
    .shadow(
        elevation    = 40.dp,
        shape        = shape,
        ambientColor = GlowShadow,
        spotColor    = GlowShadow,
    )
    // Layer 1 — main soft drop
    .shadow(
        elevation    = 30.dp,
        shape        = shape,
        ambientColor = Color.Transparent,
        spotColor    = SoftShadow1,
    )
    // Layer 2 — tighter contact shadow for grounding
    .shadow(
        elevation    = 10.dp,
        shape        = shape,
        ambientColor = Color.Transparent,
        spotColor    = SoftShadow2,
    )
    .clip(shape)
    .background(Brush.verticalGradient(listOf(CardTop, CardMid, CardBottom)))
    .border(width = 1.dp, color = CardBorder, shape = shape)
    .drawBehind {
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(CardInnerHi, Color.Transparent),
                endY   = size.height * 0.40f,
            ),
        )
    }

