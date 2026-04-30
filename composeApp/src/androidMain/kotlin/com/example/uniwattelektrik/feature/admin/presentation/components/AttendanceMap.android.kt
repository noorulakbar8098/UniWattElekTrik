package com.example.uniwattelektrik.feature.admin.presentation.components

import android.preference.PreferenceManager
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay

/**
 * Free, no-API-key map using OpenStreetMap tiles via osmdroid.
 *
 * - Renders a [MapView] inside Compose
 * - Adds an OS-driven *blue-dot* overlay for the device's live location and
 *   centres + zooms on it (street-level, zoom 17) the moment the first fix
 *   arrives. Falls back to the first employee marker, then to Bengaluru.
 * - Renders one [Marker] per [MapMarker] (employee GPS pings).
 * - Floats a circular **My-location** button bottom-right that re-centres
 *   on the device's current GPS fix at zoom 17.
 */
@Composable
actual fun AttendanceMap(
    markers: List<MapMarker>,
    modifier: Modifier,
) {
    val context = LocalContext.current

    // osmdroid needs a user-agent and storage path before MapView is constructed.
    LaunchedEffect(Unit) {
        Configuration.getInstance().load(
            context,
            PreferenceManager.getDefaultSharedPreferences(context),
        )
        Configuration.getInstance().userAgentValue = context.packageName
    }

    val mapView = remember {
        MapView(context).apply {
            // Default OSM tiles — clean delivery-app look (Uber/Zomato style).
            setTileSource(TileSourceFactory.MAPNIK)

            // Enable buttery pinch-to-zoom + two-finger pan + fling.
            setMultiTouchControls(true)
            isTilesScaledToDpi = true                   // crisp tiles on hi-dpi
            isFlingEnabled = true                       // momentum scroll
            // No world wrap — feels more like a real map app.
            isHorizontalMapRepetitionEnabled = false
            isVerticalMapRepetitionEnabled   = false

            // Hide the on-screen +/- buttons; gestures only.
            zoomController.setVisibility(
                CustomZoomButtonsController.Visibility.NEVER,
            )

            // Allow continuous (fractional) pinch-zoom for smoothness.
            minZoomLevel = 3.0
            maxZoomLevel = 21.0

            // Default to Bengaluru if no fix and no markers yet.
            controller.setZoom(13.0)
            controller.setCenter(GeoPoint(13.0827, 77.5877))
        }
    }

    // ── My-location (blue dot) overlay ────────────────────────────────────
    // The first time the OS pushes a fix to us, animate to that point and
    // zoom in to street level (17). After that the user can pan/zoom freely.
    val myLocationOverlay = remember {
        MyLocationNewOverlay(GpsMyLocationProvider(context), mapView).apply {
            enableMyLocation()
            // Don't auto-follow forever — just centre on the first fix below.
        }
    }
    DisposableEffect(myLocationOverlay) {
        if (mapView.overlays.none { it === myLocationOverlay }) {
            mapView.overlays.add(myLocationOverlay)
        }
        myLocationOverlay.runOnFirstFix {
            // runOnFirstFix runs off the UI thread → hop back via post()
            mapView.post {
                myLocationOverlay.myLocation?.let { gp ->
                    mapView.controller.animateTo(GeoPoint(gp.latitude, gp.longitude))
                    mapView.controller.setZoom(17.0)   // zoomed-in street view
                }
            }
        }
        onDispose {
            myLocationOverlay.disableMyLocation()
        }
    }

    // ── Employee markers ──────────────────────────────────────────────────
    LaunchedEffect(markers) {
        // Drop only employee Marker overlays — keep the my-location overlay.
        mapView.overlays.removeAll { it is Marker }
        markers.forEach { m ->
            val pin = Marker(mapView).apply {
                position = GeoPoint(m.latitude, m.longitude)
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                title = m.title
                snippet = m.snippet
            }
            mapView.overlays.add(pin)
        }
        // If we don't have a my-location fix yet, fall back to centring on the
        // first employee marker so the screen is never empty.
        if (myLocationOverlay.myLocation == null) {
            markers.firstOrNull()?.let {
                mapView.controller.animateTo(GeoPoint(it.latitude, it.longitude))
                mapView.controller.setZoom(15.0)
            }
        }
        mapView.invalidate()
    }

    Box(modifier = modifier) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { mapView },
            update = { mv ->
                // Prevent the parent LazyColumn from stealing the touch
                // gesture while the user is panning / pinch-zooming the map.
                mv.setOnTouchListener { v, event ->
                    when (event.actionMasked) {
                        android.view.MotionEvent.ACTION_DOWN,
                        android.view.MotionEvent.ACTION_POINTER_DOWN,
                        android.view.MotionEvent.ACTION_MOVE ->
                            v.parent?.requestDisallowInterceptTouchEvent(true)
                        android.view.MotionEvent.ACTION_UP,
                        android.view.MotionEvent.ACTION_CANCEL ->
                            v.parent?.requestDisallowInterceptTouchEvent(false)
                    }
                    false  // let the MapView keep handling the event
                }
            },
        )

        if (markers.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0x66FFFFFF)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "No GPS check-ins yet",
                    color = Color(0xFF64748B),
                )
            }
        }

        // ── Floating "my location" button ────────────────────────────────
        // Tapping recentres on the latest GPS fix at zoom 17. If we don't yet
        // have a fix, request one and centre on it as soon as it arrives.
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 14.dp, bottom = 14.dp)
                .size(46.dp)
                .shadow(8.dp, CircleShape, spotColor = Color(0x33000000))
                .clip(CircleShape)
                .background(Color.White)
                .clickable {
                    val fix = myLocationOverlay.myLocation
                    if (fix != null) {
                        mapView.controller.animateTo(GeoPoint(fix.latitude, fix.longitude))
                        mapView.controller.setZoom(17.0)
                    } else {
                        // Re-arm runOnFirstFix in case the overlay missed it
                        myLocationOverlay.enableMyLocation()
                        myLocationOverlay.runOnFirstFix {
                            mapView.post {
                                myLocationOverlay.myLocation?.let { gp ->
                                    mapView.controller.animateTo(
                                        GeoPoint(gp.latitude, gp.longitude),
                                    )
                                    mapView.controller.setZoom(17.0)
                                }
                            }
                        }
                    }
                },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector        = Icons.Filled.MyLocation,
                contentDescription = "Center on my location",
                tint               = Color(0xFF1D4ED8),
                modifier           = Modifier.size(22.dp),
            )
        }
    }
}

