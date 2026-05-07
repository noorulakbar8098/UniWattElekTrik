package com.example.uniwattelektrik.core.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Enterprise typographic scale — single source of truth for every text style.
 *
 * 6-tier hierarchy designed for a field-ops SaaS dashboard.
 * Tight negative letter spacing on large sizes gives a premium "Inter-like"
 * fintech feel; generous line heights on body copy improve readability.
 *
 * ┌─────────────┬──────────┬──────────────┬────────────────────────────────────┐
 * │ Tier        │ Size     │ Weight       │ Intent                             │
 * ├─────────────┼──────────┼──────────────┼────────────────────────────────────┤
 * │ Display     │ 24–34 sp │ ExtraBold    │ Hero numbers, splash, big KPI vals │
 * │ Headline    │ 18–22 sp │ Bold/SemiBold│ Screen titles, section openers     │
 * │ Title       │ 15–17 sp │ SemiBold     │ Card headers, list item titles     │
 * │ Body        │ 13–16 sp │ Normal/Medium│ Descriptions, content copy         │
 * │ Caption     │ 11–12 sp │ Medium       │ Meta, timestamps, locations        │
 * │ Label       │ 10–13 sp │ SemiBold/Bold│ Chips, badges, allcaps tags        │
 * └─────────────┴──────────┴──────────────┴────────────────────────────────────┘
 *
 * Usage example:
 *   Text("Task Board", style = AppTypography.headlineMedium.copy(color = Color.White))
 *
 * Custom font swap:
 *   Add a single `fontFamily = MyFontFamily` override to each style here —
 *   the entire app updates automatically.
 */
object AppTypography {

    /* ── Display — hero numbers, large counters, splash text ──────────── */

    val displayLarge = TextStyle(
        fontSize      = 34.sp,
        fontWeight    = FontWeight.ExtraBold,
        letterSpacing = (-1.0).sp,
        lineHeight    = 42.sp,
    )
    val displayMedium = TextStyle(
        fontSize      = 28.sp,
        fontWeight    = FontWeight.ExtraBold,
        letterSpacing = (-0.5).sp,
        lineHeight    = 36.sp,
    )
    val displaySmall = TextStyle(
        fontSize      = 24.sp,
        fontWeight    = FontWeight.Bold,
        letterSpacing = (-0.3).sp,
        lineHeight    = 32.sp,
    )

    /* ── Headline — screen and section titles ─────────────────────────── */

    val headlineLarge = TextStyle(
        fontSize      = 22.sp,
        fontWeight    = FontWeight.Bold,
        letterSpacing = (-0.2).sp,
        lineHeight    = 30.sp,
    )
    val headlineMedium = TextStyle(
        fontSize      = 20.sp,
        fontWeight    = FontWeight.SemiBold,
        letterSpacing = (-0.2).sp,
        lineHeight    = 28.sp,
    )
    val headlineSmall = TextStyle(
        fontSize      = 18.sp,
        fontWeight    = FontWeight.SemiBold,
        letterSpacing = (-0.1).sp,
        lineHeight    = 26.sp,
    )

    /* ── Title — card headers, list primary text ──────────────────────── */

    val titleLarge = TextStyle(
        fontSize      = 17.sp,
        fontWeight    = FontWeight.Bold,
        letterSpacing = (-0.1).sp,
        lineHeight    = 24.sp,
    )
    val titleMedium = TextStyle(
        fontSize   = 16.sp,
        fontWeight = FontWeight.SemiBold,
        lineHeight = 24.sp,
    )
    val titleSmall = TextStyle(
        fontSize   = 15.sp,
        fontWeight = FontWeight.SemiBold,
        lineHeight = 22.sp,
    )

    /* ── Body — descriptions, activity notes, form content ───────────── */

    val bodyLarge = TextStyle(
        fontSize   = 16.sp,
        fontWeight = FontWeight.Normal,
        lineHeight = 26.sp,
    )
    val bodyMedium = TextStyle(
        fontSize   = 14.sp,
        fontWeight = FontWeight.Normal,
        lineHeight = 22.sp,
    )
    val bodySmall = TextStyle(
        fontSize   = 13.sp,
        fontWeight = FontWeight.Normal,
        lineHeight = 20.sp,
    )

    /* ── Caption — metadata, timestamps, sub-labels ──────────────────── */

    val captionLarge = TextStyle(
        fontSize   = 12.sp,
        fontWeight = FontWeight.Medium,
        lineHeight = 18.sp,
    )
    val captionSmall = TextStyle(
        fontSize   = 11.sp,
        fontWeight = FontWeight.Medium,
        lineHeight = 16.sp,
    )

    /* ── Label — chips, badges, allcaps tags ──────────────────────────── */

    val labelLarge = TextStyle(
        fontSize      = 13.sp,
        fontWeight    = FontWeight.SemiBold,
        letterSpacing = 0.4.sp,
    )
    val labelMedium = TextStyle(
        fontSize      = 12.sp,
        fontWeight    = FontWeight.SemiBold,
        letterSpacing = 0.6.sp,
    )
    val labelSmall = TextStyle(
        fontSize      = 10.sp,
        fontWeight    = FontWeight.Bold,
        letterSpacing = 1.0.sp,
    )

    /* ── Header-specific overrides (white tint for gradient headers) ──── */

    /** Main screen title in the gradient header bar. */
    val HeaderTitle = headlineMedium.copy(color = Color.White)

    /** All-caps eyebrow / sub-counter shown under the header title. */
    val HeaderSubtitle = labelSmall.copy(
        letterSpacing = 1.4.sp,
        color         = Color.White.copy(alpha = 0.85f),
    )

    /** Very small all-caps eyebrow used in compact header rows. */
    val HeaderSubtitleSmall = TextStyle(
        fontSize      = 8.sp,
        fontWeight    = FontWeight.SemiBold,
        letterSpacing = 1.4.sp,
        color         = Color.White.copy(alpha = 0.85f),
    )

    /** Greeting line above a hero title ("Good morning ☀️"). */
    val HeaderGreeting = bodySmall.copy(color = Color.White.copy(alpha = 0.85f))

    /** Extra-large display title in the gradient header ("Operations Overview"). */
    val HeroTitle = displaySmall.copy(color = Color.White)

    /** Body-section sub-header used inside screen cards. */
    val SectionTitle = titleLarge
}
