package com.example.uniwattelektrik.core.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Enterprise design token system — single source of truth for the entire app.
 *
 * Design principles:
 *  - One primary brand colour (blue), semantic colours only for status
 *  - Clean layered backgrounds — no decorative gradients on surfaces
 *  - Subtle shadows — no coloured glows
 *  - Consistent ink scale for readable type hierarchy
 *  - Accessible contrast ratios throughout
 */
object AppTheme {

    // ─── Backgrounds (layered, light) ────────────────────────────────────
    /** Main page background — very light neutral. */
    val Bg           = Color(0xFFF5F7FA)
    /** Slightly elevated surface (section separators, muted areas). */
    val BgSecondary  = Color(0xFFEBEEF3)
    /** Card / panel surface. */
    val Surface      = Color(0xFFFFFFFF)
    /** Input fields, chips, muted containers. */
    val SurfaceMuted = Color(0xFFF0F3F7)

    // ─── Brand (single primary blue) ─────────────────────────────────────
    /** Primary action colour. */
    val Brand    = Color(0xFF1A6BF5)
    /** Pressed / hover state. */
    val Brand700 = Color(0xFF1458CC)
    /** Tint background behind brand icons / chips. */
    val Brand50  = Color(0xFFE8F0FE)
    /** Subtle brand divider / border. */
    val Brand100 = Color(0xFFD0E2FC)
    /** Dark navy — used only in app header gradient. */
    val Navy     = Color(0xFF0D1B3E)

    // ─── Ink (text) — neutral blue-grey scale ────────────────────────────
    /** Headings, primary text. */
    val Ink900 = Color(0xFF0D1B3E)
    /** Sub-headings. */
    val Ink700 = Color(0xFF253354)
    /** Body text. */
    val Ink500 = Color(0xFF4E6083)
    /** Placeholder, muted labels. */
    val Ink300 = Color(0xFF8E9FC0)
    /** Dividers. */
    val Ink100 = Color(0xFFDDE3EE)
    /** Very light fill. */
    val Ink50  = Color(0xFFF0F3F7)

    // ─── Semantic status colours ──────────────────────────────────────────
    // Task status
    /** Completed / success. */
    val Success   = Color(0xFF16A34A)
    val SuccessBg = Color(0xFFDCFCE7)
    /** Pending / in-progress / medium priority. */
    val Warning   = Color(0xFFD97706)
    val WarningBg = Color(0xFFFEF3C7)
    /** Overdue / critical / high priority. */
    val Danger    = Color(0xFFDC2626)
    val DangerBg  = Color(0xFFFEE2E2)
    /** In-progress. Reuse Brand. */
    val Info      = Brand
    val InfoBg    = Brand50

    // Employee status
    /** Active employee. */
    val StatusActive  = Color(0xFF16A34A)
    val StatusActiveBg = Color(0xFFDCFCE7)
    /** On leave. */
    val StatusLeave   = Color(0xFF6B7280)
    val StatusLeaveBg = Color(0xFFF3F4F6)
    /** Offline. */
    val StatusOffline = Color(0xFF9CA3AF)
    val StatusOfflineBg = Color(0xFFF9FAFB)

    // Priority labels (maps to task priority strings)
    val PriorityLow    = Success;  val PriorityLowBg    = SuccessBg
    val PriorityMed    = Warning;  val PriorityMedBg    = WarningBg
    val PriorityHigh   = Danger;   val PriorityHighBg   = DangerBg
    val PriorityUrgent = Color(0xFF7C3AED); val PriorityUrgentBg = Color(0xFFEDE9FE)

    // ─── Shadows — blue-tinted Ink900 for premium "ambient light" feel ──
    // Tinting shadows toward the brand's dark navy (instead of pure black)
    // makes elevated surfaces look like they're floating above an
    // atmospherically-lit surface rather than sitting on a flat page. The
    // alpha values are unchanged so the perceived elevation is identical to
    // the previous neutral-black system.
    val ShadowSm     = Color(0x0A0D1B3E)   // 4 %  — very subtle
    val ShadowMd     = Color(0x0F0D1B3E)   // 6 %  — standard card
    val ShadowLg     = Color(0x180D1B3E)   // 10 % — elevated modal
    // Keep this for backward compatibility with BottomNavBar FAB
    val ShadowSpotBlue = Color(0x200D1B3E)

    // ─── Shape radii ─────────────────────────────────────────────────────
    val RadiusXs = 6.dp
    val RadiusLg = 18.dp
    val RadiusXl = 24.dp

    // ─── Spacing (8-pt grid) ─────────────────────────────────────────────
    val Sp1 = 4.dp
    val Sp2 = 8.dp
    val Sp3 = 12.dp
    val Sp4 = 16.dp
    val Sp5 = 20.dp
    val Sp6 = 24.dp
    val Sp7 = 32.dp
    val Sp8 = 40.dp

    // ─── Spacing semantic aliases ─────────────────────────────────────────
    /** 4 dp — tight gaps, inner chip padding, icon-to-label spacing */
    val SpXs  = 4.dp
    /** 8 dp — row spacing, chip gaps, icon margins */
    val SpSm  = 8.dp
    /** 12 dp — card internal vertical padding, section sub-gaps */
    val SpMd  = 12.dp
    /** 16 dp — screen horizontal padding, card padding */
    val SpLg  = 16.dp
    /** 24 dp — section spacing, hero padding, dialog padding */
    val SpXl  = 24.dp
    /** 32 dp — large section gaps, screen top padding */
    val SpXxl = 32.dp

    // ─── Component fixed dimensions ───────────────────────────────────────
    val ButtonHeight     = 52.dp    // primary CTA full-width button
    val ButtonHeightSm   = 40.dp    // secondary / compact button
    val IconXs           = 14.dp    // badge / tag icons
    val IconSm           = 16.dp    // inline chip / status dot context
    val IconMd           = 20.dp    // standard action icon
    val IconLg           = 24.dp    // nav bar, section header icon
    val IconXl           = 32.dp    // hero / feature icon
    val AvatarSm         = 32.dp    // compact list item avatar
    val AvatarMd         = 36.dp    // card footer avatar
    val AvatarLg         = 48.dp    // profile section avatar
    val FabSize          = 64.dp    // floating action button
    val BottomNavHeight  = 64.dp    // bottom navigation bar
    val AccentStripWidth = 4.dp     // left gradient strip on content cards
    val ChipHeight       = 28.dp    // status / priority / filter chips
    val SearchBarHeight  = 48.dp    // search input fields

    // ─── Backward-compat aliases for screens not yet refactored ──────────
    // Priority / status color aliases — use semantic names in new code
    val Low    = Success;   val LowBg  = SuccessBg
    val Med    = Warning;   val MedBg  = WarningBg
    val High   = Danger;    val HighBg = DangerBg
    val Violet = Color(0xFF8B5CF6)

    // Shadow aliases — use ShadowSm/Md/Lg in new code
    val ShadowSpotSoft   = ShadowSm
    val ShadowSpotMedium = ShadowMd
    val ShadowSpotStrong = ShadowLg
    val ShadowSpotBlue2  = Color(0x40007BFF)   // kept for gradient FAB only

    // Old radius aliases used by existing screens
    val RadiusSm = RadiusXs      // 6 dp (was 10 dp — close enough for compat)
    val Radius   = RadiusLg      // 18 dp  (was 14 dp)
}
