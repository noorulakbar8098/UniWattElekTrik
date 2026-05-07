package com.example.uniwattelektrik.core.theme

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Centralized elevation scale for consistent perceived depth across all
 * surfaces. Every shadow call in the app should reference a token here
 * rather than raw dp values.
 *
 * Elevation communicates hierarchy:
 *   Screen background (0) → Card (card) → Modal (dialog) → FAB (fab)
 *
 * ┌────────────┬────────┬──────────────────────────────────────────────────┐
 * │ Token      │  dp    │ Surface                                          │
 * ├────────────┼────────┼──────────────────────────────────────────────────┤
 * │ none       │  0     │ Flat backgrounds, dividers                       │
 * │ xs         │  2     │ Chips, small badges                              │
 * │ sm         │  4     │ Input fields, secondary surfaces                 │
 * │ md         │  8     │ Hover / active card state                        │
 * │ card       │ 10     │ Standard content card (default)                  │
 * │ lg         │ 12     │ Accent / priority card                           │
 * │ xl         │ 16     │ Sticky headers, bottom bar                       │
 * │ dialog     │ 24     │ Modal sheets, dialogs                            │
 * │ fab        │ 20     │ Floating action button                           │
 * └────────────┴────────┴──────────────────────────────────────────────────┘
 */
object AppElevation {
    val none    : Dp = 0.dp
    val xs      : Dp = 2.dp
    val sm      : Dp = 4.dp
    val md      : Dp = 8.dp
    val card    : Dp = 10.dp
    val lg      : Dp = 12.dp
    val xl      : Dp = 16.dp
    val dialog  : Dp = 24.dp
    val fab     : Dp = 20.dp

    // Semantic aliases
    val chip      : Dp = xs
    val accentCard: Dp = lg
    val header    : Dp = md
    val bottomBar : Dp = xl
    val tooltip   : Dp = sm
}
