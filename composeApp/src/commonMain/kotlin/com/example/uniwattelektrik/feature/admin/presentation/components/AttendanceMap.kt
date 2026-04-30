package com.example.uniwattelektrik.feature.admin.presentation.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

data class MapMarker(
    val id: String,
    val title: String,
    val snippet: String,
    val latitude: Double,
    val longitude: Double,
)

/**
 * Live map view showing employee GPS check-in pings.
 * Android backs this with Google Maps Compose; other targets render a
 * placeholder.
 */
@Composable
expect fun AttendanceMap(
    markers: List<MapMarker>,
    modifier: Modifier = Modifier,
)
