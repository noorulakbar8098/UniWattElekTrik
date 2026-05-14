package com.example.uniwattelektrik.core.theme

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Premium soft drop shadow — neutral, wide, evenly diffused.
 *
 * This is the "modern fintech" shadow look (think Khatabook / Vijay32-style
 * dashboards, Razorpay invoices, modern Indian SaaS apps) — soft cool-grey
 * shadows that sit evenly around every card, NOT colour-tinted halos. The
 * effect should read as "this card is floating above the page" without ever
 * drawing attention to the shadow itself.
 *
 * Recipe — two stacked tiers using a single brand-tinted cool-grey:
 *
 *   ┌────────────────────────────────────┐
 *   │              CARD                  │
 *   └────────────────────────────────────┘
 *    ░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░   ← Tier 2: tight proximity (5dp, soft)
 *    ░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░  ← Tier 1: wide ambient drop (24dp, very soft)
 *
 * Key differences from a "designer flashy" shadow:
 *  · NEUTRAL colour — never status-tinted. Status identity comes through the
 *    accent strip / avatar ring / status chip, not the shadow itself.
 *  · HIGH ambient ratio — shadow spreads omnidirectionally around the card,
 *    not just below it. Reads as ambient light bouncing, not a hard drop.
 *  · LOW saturation overall — the eye should feel the lift without ever
 *    consciously noticing the shadow.
 *
 * The [accentColor] parameter is accepted for backwards-compat with existing
 * call-sites but is INTENTIONALLY ignored — pass anything, the shadow is
 * always rendered in the neutral cool-grey tone.
 *
 * @param shape          Card shape (must match the clipped surface).
 *
 * Tuning knobs — adjust here to retune every elevated surface in the app:
 *
 * @param dropElevation  Wide ambient drop blur. Default 24dp.
 * @param tightElevation Tight proximity shadow blur. Default 5dp.
 * @param dropAlpha      Wide-tier opacity. Default 0.18.
 * @param tightAlpha     Tight-tier opacity. Default 0.10.
 * @param ambientAlpha   Omnidirectional bleed (the "even all around" feel).
 *                       Default 0.14.
 */
fun Modifier.premiumLayeredShadow(
    accentColor: Color = SoftShadow,   // kept for source compat — ignored
    shape: Shape,
    dropElevation : Dp = 24.dp,
    tightElevation: Dp = 5.dp,
    dropAlpha     : Float = 0.18f,
    tightAlpha    : Float = 0.10f,
    ambientAlpha  : Float = 0.14f,
): Modifier = this
    // ── Tier 1 — wide ambient drop ────────────────────────────────────
    // Soft cool-grey, generous blur, even ambient ratio. This is the
    // shadow that gives the "card floating on the page" feel.
    .shadow(
        elevation    = dropElevation,
        shape        = shape,
        spotColor    = SoftShadow.copy(alpha = dropAlpha),
        ambientColor = SoftShadow.copy(alpha = ambientAlpha),
    )
    // ── Tier 2 — tight proximity ──────────────────────────────────────
    // Small elevation, low alpha. Seats the card on the surface so the
    // wide drop doesn't read as "floating in space".
    .shadow(
        elevation    = tightElevation,
        shape        = shape,
        spotColor    = SoftShadow.copy(alpha = tightAlpha),
        ambientColor = SoftShadow.copy(alpha = ambientAlpha * 0.55f),
    )

/**
 * Cool brand-tinted grey used by every premium drop shadow.
 *
 * Ink900 (#0D1B3E — deep navy) mixed with 25 % Brand (#1A6BF5 — vivid blue).
 * Result: a soft blueish slate that reads as "neutral" but quietly ties
 * every elevated surface in the app back to the brand palette. Tune the
 * lerp fraction here to retune the warmth of every shadow at once — higher
 * lerp = bluer / more brand-warm, lower lerp = cooler / more inky.
 */
private val SoftShadow: Color = lerp(AppTheme.Ink900, AppTheme.Brand, 0.25f)
