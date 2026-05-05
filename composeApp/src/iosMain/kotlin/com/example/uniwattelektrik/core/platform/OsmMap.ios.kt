package com.example.uniwattelektrik.core.platform

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

@Composable
actual fun OsmMap(
    latitude: Double?,
    longitude: Double?,
    modifier: Modifier,
) {
    Box(
        modifier = modifier.background(Color(0xFFDDE7F5)),
        contentAlignment = Alignment.Center,
    ) {
        val text = if (latitude != null && longitude != null) {
            "Map (iOS) • $latitude, $longitude"
        } else {
            "Map (iOS) • address not resolved"
        }
        Text(text, color = Color(0xFF64748B))
    }
}

