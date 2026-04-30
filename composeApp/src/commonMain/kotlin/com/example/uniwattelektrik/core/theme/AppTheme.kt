package com.example.uniwattelektrik.core.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Single source of truth for all design tokens used across User + Admin modules.
 * Mirrors the CSS custom-properties from the reference Figma so a developer can
 * line up tokens visually.
 */
object AppTheme {

    /* ─── Surface ───────────────────────────────────────────────────── */
    val Bg          = Color(0xFFF7F9FC)   // page background
    val BgSecondary = Color(0xFFEEF2F7)
    val Surface     = Color(0xFFFFFFFF)   // cards
    val SurfaceMuted = Color(0xFFF1F4FA)  // inputs, chips

    /* ─── Brand / Navy ──────────────────────────────────────────────── */
    val Brand     = Color(0xFF007BFF)
    val Brand600  = Color(0xFF0062CC)
    val Brand50   = Color(0xFFE6F2FF)
    val Brand100  = Color(0xFFCCE5FF)
    val Navy      = Color(0xFF0A1F44)
    val Navy700   = Color(0xFF07183A)

    /* ─── Ink (text) ────────────────────────────────────────────────── */
    val Ink900 = Color(0xFF0A1F44)   // headings
    val Ink700 = Color(0xFF2B3A5C)
    val Ink500 = Color(0xFF5B6B8C)   // body
    val Ink300 = Color(0xFF9BA8C4)   // muted
    val Ink100 = Color(0xFFE6ECF5)   // dividers
    val Ink50  = Color(0xFFF1F4FA)

    /* ─── Status (priority/result) ─────────────────────────────────── */
    val Low     = Color(0xFF10B981)   // success / low priority
    val LowBg   = Color(0xFFE6F7F0)
    val Med     = Color(0xFFF59E0B)   // warning / med priority
    val MedBg   = Color(0xFFFEF3E2)
    val High    = Color(0xFFEF4444)   // critical / high priority
    val HighBg  = Color(0xFFFDE8E8)
    val Info    = Color(0xFF1E73E8)
    val Violet  = Color(0xFF8B5CF6)

    /* ─── Shadows (alphas) ─────────────────────────────────────────── */
    val ShadowSpotSoft   = Color(0x0F0A1F44)   // ~6 % navy
    val ShadowSpotMedium = Color(0x140A1F44)   // ~8 %
    val ShadowSpotStrong = Color(0x260A1F44)   // ~15 %
    val ShadowSpotBlue   = Color(0x40007BFF)   // brand-tinted

    /* ─── Radii ─────────────────────────────────────────────────────── */
    val RadiusSm = 10.dp
    val Radius   = 14.dp
    val RadiusLg = 20.dp
    val RadiusXl = 24.dp

    /* ─── Spacing scale (8-pt grid) ─────────────────────────────────── */
    val Sp1 = 4.dp
    val Sp2 = 8.dp
    val Sp3 = 12.dp
    val Sp4 = 16.dp
    val Sp5 = 20.dp
    val Sp6 = 24.dp
    val Sp7 = 32.dp
    val Sp8 = 40.dp
}
