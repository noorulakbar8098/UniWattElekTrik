package com.example.uniwattelektrik.feature.user.presentation.screens

import com.example.uniwattelektrik.core.performance.TrackScreenPerformance

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Construction
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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

    Box(modifier = Modifier.fillMaxSize()) {
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
    } // end Column

    // ── Under Development overlay ──────────────────────────────────────────
    UnderDevelopmentOverlay()
    } // end outer Box
}

@Composable
private fun UnderDevelopmentOverlay() {
    val transition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by transition.animateFloat(
        initialValue = 0.55f,
        targetValue  = 1f,
        animationSpec = infiniteRepeatable(
            animation  = tween(1600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulseAlpha",
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0A1929).copy(alpha = 0.93f),
                        Color(0xFF0D2137).copy(alpha = 0.96f),
                    ),
                ),
            ),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp),
            modifier = Modifier.padding(horizontal = 40.dp),
        ) {
            // Pulsing icon circle
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .alpha(pulseAlpha)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                Color(0xFF1E73E8).copy(alpha = 0.30f),
                                Color(0xFF1E73E8).copy(alpha = 0.08f),
                            ),
                        ),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(Color(0xFF1E73E8), Color(0xFF0A3D91)),
                            ),
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector        = Icons.Outlined.Construction,
                        contentDescription = null,
                        tint               = Color.White,
                        modifier           = Modifier.size(34.dp),
                    )
                }
            }

            // "COMING SOON" badge
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(Color(0xFF1E73E8).copy(alpha = 0.18f))
                    .padding(horizontal = 16.dp, vertical = 6.dp),
            ) {
                Row(
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF60A5FA).copy(alpha = pulseAlpha)),
                    )
                    Text(
                        "COMING SOON",
                        color         = Color(0xFF60A5FA),
                        fontSize      = 10.sp,
                        fontWeight    = FontWeight.Bold,
                        letterSpacing = 1.8.sp,
                    )
                }
            }

            // Main headline
            Text(
                "Live Map",
                color      = Color.White,
                fontSize   = 28.sp,
                fontWeight = FontWeight.Bold,
                textAlign  = TextAlign.Center,
            )

            // Subtitle
            Text(
                "We're building a real-time field map with live GPS tracking, employee locations, and task overlays.\n\nThis feature will be available in a future update.",
                color      = Color.White.copy(alpha = 0.62f),
                fontSize   = 14.sp,
                textAlign  = TextAlign.Center,
                lineHeight = 22.sp,
            )

            Spacer(Modifier.height(8.dp))

            // Feature preview pills
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                FeaturePill("📍  Live employee locations")
                FeaturePill("🗺  Task site overlays")
                FeaturePill("🔔  Geo-fenced alerts")
            }
        }
    }
}

@Composable
private fun FeaturePill(text: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.07f))
            .padding(horizontal = 18.dp, vertical = 10.dp),
    ) {
        Text(
            text,
            color      = Color.White.copy(alpha = 0.75f),
            fontSize   = 13.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}

