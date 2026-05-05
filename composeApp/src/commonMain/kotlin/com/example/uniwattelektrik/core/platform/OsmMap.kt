package com.example.uniwattelektrik.core.platform

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Compact interactive map with a single pin at ([latitude], [longitude]).
 *
 * - Android: backed by OSMDroid / OpenStreetMap tiles (no API key needed).
 * - iOS: renders a styled placeholder card.
 *
 * The map auto-recentres on the pin whenever its coordinates change. If
 * [latitude] and [longitude] are both null, the Android impl falls back to a
 * reasonable default centre (Bengaluru) at low zoom.
 */
@Composable
expect fun OsmMap(
    latitude: Double?,
    longitude: Double?,
    modifier: Modifier = Modifier,
)

