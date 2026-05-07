package com.example.uniwattelektrik.core.theme

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

/**
 * Centralized shape system — every corner radius in the app comes from here.
 *
 * ┌─────────────┬──────────┬───────────────────────────────────────────────┐
 * │ Token       │ Radius   │ Usage                                         │
 * ├─────────────┼──────────┼───────────────────────────────────────────────┤
 * │ extraSmall  │  4 dp    │ Dense tags, tooltip corners                   │
 * │ small       │  8 dp    │ Buttons, filter chips, search bars            │
 * │ medium      │ 12 dp    │ Action chips, dropdowns, small panels         │
 * │ large       │ 16 dp    │ Cards, dialog sheets, bottom drawers          │
 * │ card        │ 20 dp    │ Standard screen-level content card            │
 * │ extraLarge  │ 24 dp    │ Hero cards, large modal sheets                │
 * │ headerCard  │ 28 dp    │ Header section with curved bottom             │
 * │ pill        │ 50 dp    │ Capsule buttons, status pills                 │
 * │ circle      │ —        │ Avatars, FABs, status indicator dots          │
 * └─────────────┴──────────┴───────────────────────────────────────────────┘
 *
 * Always import `AppShapes.*` instead of writing `RoundedCornerShape(Ndp)`.
 */
object AppShapes {
    val extraSmall  = RoundedCornerShape(4.dp)
    val small       = RoundedCornerShape(8.dp)
    val medium      = RoundedCornerShape(12.dp)
    val large       = RoundedCornerShape(16.dp)
    val card        = RoundedCornerShape(20.dp)
    val extraLarge  = RoundedCornerShape(24.dp)
    val headerCard  = RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp)
    val pill        = RoundedCornerShape(50.dp)
    val circle      = CircleShape

    // Directional variants used by the left accent strip on cards
    val stripStart  = RoundedCornerShape(topStart = 4.dp, bottomStart = 4.dp)
}
