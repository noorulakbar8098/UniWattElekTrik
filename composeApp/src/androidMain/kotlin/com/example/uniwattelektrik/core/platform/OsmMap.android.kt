package com.example.uniwattelektrik.core.platform

import android.preference.PreferenceManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

@Composable
actual fun OsmMap(
    latitude: Double?,
    longitude: Double?,
    modifier: Modifier,
) {
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        Configuration.getInstance().load(
            context,
            PreferenceManager.getDefaultSharedPreferences(context),
        )
        Configuration.getInstance().userAgentValue = context.packageName
    }

    val mapView = remember {
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            isTilesScaledToDpi = true
            isFlingEnabled = true
            isHorizontalMapRepetitionEnabled = false
            isVerticalMapRepetitionEnabled = false
            zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
            minZoomLevel = 3.0
            maxZoomLevel = 21.0
            controller.setZoom(13.0)
            controller.setCenter(GeoPoint(13.0827, 77.5877)) // Bengaluru fallback
        }
    }

    LaunchedEffect(latitude, longitude) {
        // Drop any previous task-pin Markers
        mapView.overlays.removeAll { it is Marker }
        if (latitude != null && longitude != null) {
            val point = GeoPoint(latitude, longitude)
            val pin = Marker(mapView).apply {
                position = point
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            }
            mapView.overlays.add(pin)
            mapView.controller.animateTo(point)
            mapView.controller.setZoom(16.0)
        }
        mapView.invalidate()
    }

    AndroidView(
        modifier = modifier,
        factory = { mapView },
        update = { mv ->
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
                false
            }
        },
    )
}

