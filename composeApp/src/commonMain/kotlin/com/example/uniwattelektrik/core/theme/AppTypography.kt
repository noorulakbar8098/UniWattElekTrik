package com.example.uniwattelektrik.core.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Single source of truth for every text style that appears in a header bar
 * across the app. By centralizing these here, swapping in a custom font
 * (Inter / SF Pro) later is a one-line change.
 *
 * Conventions:
 *  - `HeaderTitle` — the main screen title ("Admin Console", "Tasks").
 *  - `HeaderSubtitle` — the all-caps tracker (admin id, "OPERATIONS").
 *  - `HeaderGreeting` — the small line above a hero title ("Good morning ☀️").
 *  - `HeroTitle` — extra-large display title ("Operations Overview").
 *  - `SectionTitle` — body-section headers ("Latest tasks", "Performance").
 */
object AppTypography {

    val HeaderTitle = TextStyle(
        fontSize      = 20.sp,
        fontWeight    = FontWeight.SemiBold,
        letterSpacing = (-0.2).sp,
        color         = Color.White,
    )

    val HeaderSubtitle = TextStyle(
        fontSize      = 10.sp,
        fontWeight    = FontWeight.SemiBold,
        letterSpacing = 1.4.sp,
        color         = Color.White.copy(alpha = 0.85f),
    )

    val HeaderSubtitleSmall = TextStyle(
        fontSize      = 8.sp,
        fontWeight    = FontWeight.SemiBold,
        letterSpacing = 1.4.sp,
        color         = Color.White.copy(alpha = 0.85f),
    )

    val HeaderGreeting = TextStyle(
        fontSize      = 12.sp,
        fontWeight    = FontWeight.Medium,
        letterSpacing = 0.sp,
        color         = Color.White.copy(alpha = 0.85f),
    )

    val HeroTitle = TextStyle(
        fontSize      = 26.sp,
        fontWeight    = FontWeight.Bold,
        letterSpacing = (-0.5).sp,
        color         = Color.White,
    )

    val SectionTitle = TextStyle(
        fontSize      = 17.sp,
        fontWeight    = FontWeight.Bold,
        letterSpacing = (-0.2).sp,
    )
}

