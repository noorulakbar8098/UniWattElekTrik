package com.example.uniwattelektrik.feature.user.presentation.screens

import com.example.uniwattelektrik.core.performance.TrackScreenPerformance

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.uniwattelektrik.core.platform.OsmMap
import com.example.uniwattelektrik.core.theme.AppTheme
import com.example.uniwattelektrik.feature.workforce.presentation.WorkforceViewModel
import com.example.uniwattelektrik.platform.LocationProvider
import kotlinx.coroutines.launch

@Composable
fun LiveMapScreen(
    workforceVm: WorkforceViewModel? = null,
    userId: String = "",
) {
    TrackScreenPerformance("LiveMapScreen")
    val scope            = rememberCoroutineScope()
    val locationProvider = remember { LocationProvider() }

    // Lat/lng to show on the map — prefers live GPS, falls back to last check-in.
    var mapLat by remember { mutableStateOf<Double?>(null) }
    var mapLng by remember { mutableStateOf<Double?>(null) }
    var locationLabel by remember { mutableStateOf("Acquiring GPS…") }
    var isLive        by remember { mutableStateOf(false) }

    // Pull last check-in coords from attendance records.
    val attendance by (workforceVm?.attendance?.collectAsStateWithLifecycle()
        ?: remember {
            kotlinx.coroutines.flow.MutableStateFlow(
                emptyList<com.example.uniwattelektrik.feature.workforce.data.remote.AttendanceRecord>()
            )
        }.collectAsStateWithLifecycle())

    // Seed the map with last known check-in location while GPS warms up.
    LaunchedEffect(attendance, userId) {
        val last = attendance
            .filter { it.userId == userId && it.checkInLat != null && it.checkInLng != null }
            .maxByOrNull { it.checkInMs }
        if (last != null && mapLat == null) {
            mapLat = last.checkInLat
            mapLng = last.checkInLng
            locationLabel = "Last check-in location"
        }
    }

    // Fetch current GPS on launch.
    LaunchedEffect(Unit) {
        val loc = locationProvider.getCurrentLocation()
        if (loc != null) {
            mapLat = loc.latitude
            mapLng = loc.longitude
            locationLabel = loc.placeName ?: loc.coordsText
            isLive = true
        } else if (mapLat == null) {
            locationLabel = "Location unavailable — check permissions"
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppTheme.Bg)
            .windowInsetsPadding(WindowInsets.statusBars),
    ) {
        // ── Top bar ────────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                Text(
                    "Live Map",
                    color = AppTheme.Ink900,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    if (isLive) "📍 Live GPS" else "Last known location",
                    color = if (isLive) AppTheme.Success else AppTheme.Ink500,
                    fontSize = 12.sp,
                )
            }
            // Refresh button
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(AppTheme.Brand50)
                    .clickable {
                        scope.launch {
                            locationLabel = "Refreshing…"
                            isLive = false
                            val loc = locationProvider.getCurrentLocation()
                            if (loc != null) {
                                mapLat = loc.latitude
                                mapLng = loc.longitude
                                locationLabel = loc.placeName ?: loc.coordsText
                                isLive = true
                            } else {
                                locationLabel = "Location unavailable"
                            }
                        }
                    },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.MyLocation,
                    contentDescription = "Refresh location",
                    tint = AppTheme.Brand,
                    modifier = Modifier.size(20.dp),
                )
            }
        }

        // ── Map ────────────────────────────────────────────────────────────
        OsmMap(
            latitude  = mapLat,
            longitude = mapLng,
            modifier  = Modifier
                .fillMaxWidth()
                .weight(1f),
        )

        // ── Location pill ──────────────────────────────────────────────────
        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .shadow(8.dp, RoundedCornerShape(16.dp), spotColor = AppTheme.ShadowSm)
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(
                imageVector = Icons.Outlined.LocationOn,
                contentDescription = null,
                tint = if (isLive) AppTheme.Success else AppTheme.Brand,
                modifier = Modifier.size(22.dp),
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    locationLabel,
                    color = AppTheme.Ink900,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                val lat = mapLat
                val lng = mapLng
                if (lat != null && lng != null) {
                    Text(
                        "${(lat * 100000).toLong() / 100000.0}° N, ${(lng * 100000).toLong() / 100000.0}° E",
                        color = AppTheme.Ink500,
                        fontSize = 11.sp,
                    )
                }
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}

