package com.example.uniwattelektrik.feature.admin.presentation.components

import android.preference.PreferenceManager
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Terrain
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.ITileSource
import org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.util.MapTileIndex
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

    // Currently active map style — drives setTileSource. Persists across
    // recompositions but resets on screen leave/re-enter (acceptable; users
    // tend to pick once per session).
    var mapStyle by remember { mutableStateOf(MapStyle.Streets) }

    // Full-screen expand state — drives the modal Dialog below.
    var fullScreen by remember { mutableStateOf(false) }

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

    // Markers we own — kept separately so we never have to scan
    // `mapView.overlays` (which can race with the tile-fetch threads and crash
    // with ConcurrentModificationException when the user taps day chips fast).
    val ownedMarkers = remember { mutableListOf<Marker>() }

    // Lifecycle — OSMDroid tile + GPS providers fire callbacks on background
    // threads and MUST be released when the composable leaves the tree,
    // otherwise the next recomposition pump can dereference a dead MapView.
    DisposableEffect(mapView) {
        onDispose {
            runCatching {
                mapView.overlays.clear()
                mapView.onDetach()
            }
        }
    }

    // ── Apply the selected tile source whenever the user switches style ──
    LaunchedEffect(mapStyle) {
        mapView.setTileSource(mapStyle.tileSource())
        mapView.invalidate()
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
            // Custom avatar marker with the employee's first name baked into
            // the drawable as a pill to the LEFT of the pin. The anchor X
            // varies per name because the left-side pill makes the bitmap
            // asymmetric — buildEmployeeMarkerIcon returns the normalised
            // tail-tip coords so the geo-point stays exactly on the lat/lng.
            val art = buildEmployeeMarkerIcon(
                context  = context,
                name     = m.title,
                initials = initialsForName(m.title),
                accent   = colorForEmployee(m.id),
            )
            val pin = Marker(mapView).apply {
                position = GeoPoint(m.latitude, m.longitude)
                setAnchor(art.anchorX, art.anchorY)
                title = m.title
                snippet = m.snippet
                icon = art.drawable
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

        // ── Map-style switcher (top-right, floating) ────────────────────
        // Three styles, all genuinely free + no API key required:
        //   • Streets    — OpenStreetMap Mapnik (current default)
        //   • Satellite  — ESRI World Imagery (open access for low traffic)
        //   • Terrain    — OpenTopoMap (topographic, worldwide)
        MapStyleSwitcher(
            selected = mapStyle,
            onSelect = { mapStyle = it },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(end = 14.dp, top = 14.dp),
        )

        // ── "View full map" floating pill (bottom-left) ─────────────────
        // Opens a fullscreen Dialog with the same markers + style switcher.
        // Useful when the embedded view is too cramped to see every pin.
        Row(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 14.dp, bottom = 14.dp)
                .shadow(8.dp, RoundedCornerShape(12.dp), spotColor = Color(0x33000000))
                .clip(RoundedCornerShape(12.dp))
                .background(Color.White)
                .border(1.dp, Color(0x14000000), RoundedCornerShape(12.dp))
                .clickable { fullScreen = true }
                .padding(horizontal = 12.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(
                imageVector        = Icons.Filled.Fullscreen,
                contentDescription = null,
                tint               = Color(0xFF1A6BF5),
                modifier           = Modifier.size(16.dp),
            )
            Text(
                text       = "View full map",
                color      = Color(0xFF0D1B3E),
                fontSize   = 12.sp,
                fontWeight = FontWeight.Bold,
            )
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

    // ── Fullscreen map Dialog ───────────────────────────────────────────
    // Reuses the same markers list + spawns its own MapView so it has its
    // own osmdroid lifecycle. Close button (top-left) and a copy of the
    // style switcher (top-right) are inside the Dialog.
    if (fullScreen) {
        Dialog(
            onDismissRequest = { fullScreen = false },
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                decorFitsSystemWindows  = false,
            ),
        ) {
            FullScreenAttendanceMap(
                markers = markers,
                onClose = { fullScreen = false },
            )
        }
    }
}

/* ─── Fullscreen map ─────────────────────────────────────────────────────
 *
 *  Standalone MapView for the expanded "view full map" dialog. Renders the
 *  same employee markers + style switcher, plus a close (X) button at the
 *  top-left that dismisses back to the embedded view.
 */
@Composable
private fun FullScreenAttendanceMap(
    markers: List<MapMarker>,
    onClose: () -> Unit,
) {
    val context = LocalContext.current
    var mapStyle by remember { mutableStateOf(MapStyle.Streets) }

    val mapView = remember {
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            isTilesScaledToDpi = true
            isFlingEnabled = true
            isHorizontalMapRepetitionEnabled = false
            isVerticalMapRepetitionEnabled   = false
            zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
            minZoomLevel = 3.0
            maxZoomLevel = 21.0
            controller.setZoom(13.0)
            controller.setCenter(GeoPoint(13.0827, 77.5877))
        }
    }
    val ownedMarkers = remember { mutableListOf<Marker>() }

    DisposableEffect(mapView) {
        onDispose {
            runCatching {
                mapView.overlays.clear()
                mapView.onDetach()
            }
        }
    }

    LaunchedEffect(mapStyle) {
        mapView.setTileSource(mapStyle.tileSource())
        mapView.invalidate()
    }

    val myLocationOverlay = remember {
        MyLocationNewOverlay(GpsMyLocationProvider(context), mapView).apply {
            enableMyLocation()
        }
    }
    DisposableEffect(myLocationOverlay) {
        if (mapView.overlays.none { it === myLocationOverlay }) {
            mapView.overlays.add(myLocationOverlay)
        }
        myLocationOverlay.runOnFirstFix {
            mapView.post {
                myLocationOverlay.myLocation?.let { gp ->
                    mapView.controller.animateTo(GeoPoint(gp.latitude, gp.longitude))
                    mapView.controller.setZoom(17.0)
                }
            }
        }
        onDispose { myLocationOverlay.disableMyLocation() }
    }

    // Render the same markers with the same avatar+name-pill drawables.
    LaunchedEffect(markers) {
        mapView.post {
            runCatching {
                ownedMarkers.forEach { mapView.overlays.remove(it) }
                ownedMarkers.clear()
                markers.forEach { m ->
                    val art = buildEmployeeMarkerIcon(
                        context  = context,
                        name     = m.title,
                        initials = initialsForName(m.title),
                        accent   = colorForEmployee(m.id),
                    )
                    val pin = Marker(mapView).apply {
                        position = GeoPoint(m.latitude, m.longitude)
                        setAnchor(art.anchorX, art.anchorY)
                        title = m.title
                        snippet = m.snippet
                        icon = art.drawable
                    }
                    mapView.overlays.add(pin)
                    ownedMarkers.add(pin)
                }
                if (myLocationOverlay.myLocation == null) {
                    markers.firstOrNull()?.let {
                        mapView.controller.animateTo(GeoPoint(it.latitude, it.longitude))
                        mapView.controller.setZoom(15.0)
                    }
                }
                mapView.invalidate()
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF0D1B3E))) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory  = { mapView },
        )

        // ── Close (X) button — top-left, above status bar ──
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(start = 14.dp, top = 14.dp)
                .size(46.dp)
                .shadow(8.dp, CircleShape, spotColor = Color(0x33000000))
                .clip(CircleShape)
                .background(Color.White)
                .clickable(onClick = onClose),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector        = Icons.Filled.Close,
                contentDescription = "Close full map",
                tint               = Color(0xFF0D1B3E),
                modifier           = Modifier.size(22.dp),
            )
        }

        // ── Style switcher — top-right, status-bar aware ──
        MapStyleSwitcher(
            selected = mapStyle,
            onSelect = { mapStyle = it },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(end = 14.dp, top = 14.dp),
        )

        // ── My-location button — bottom-right ──
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 18.dp, bottom = 24.dp)
                .size(52.dp)
                .shadow(10.dp, CircleShape, spotColor = Color(0x33000000))
                .clip(CircleShape)
                .background(Color.White)
                .clickable {
                    val fix = myLocationOverlay.myLocation
                    if (fix != null) {
                        mapView.controller.animateTo(GeoPoint(fix.latitude, fix.longitude))
                        mapView.controller.setZoom(17.0)
                    } else {
                        myLocationOverlay.enableMyLocation()
                        myLocationOverlay.runOnFirstFix {
                            mapView.post {
                                myLocationOverlay.myLocation?.let { gp ->
                                    mapView.controller.animateTo(GeoPoint(gp.latitude, gp.longitude))
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
                modifier           = Modifier.size(24.dp),
            )
        }

        // ── Empty-state banner ──
        if (markers.isEmpty()) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(24.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White)
                    .padding(horizontal = 18.dp, vertical = 12.dp),
            ) {
                Text(
                    text = "No GPS check-ins yet",
                    color = Color(0xFF64748B),
                    fontWeight = FontWeight.Medium,
                )
            }
        }
    }
}

/* ─── Map styles ─────────────────────────────────────────────────────────
 *
 *  Three style presets, all free + no API key. Selecting one calls
 *  `mapView.setTileSource(...)` which swaps the entire raster background
 *  without disturbing markers, overlays, or zoom level.
 */

private enum class MapStyle(
    val label: String,
    val icon : ImageVector,
) {
    Streets  ("Streets",   Icons.Filled.Map),
    Satellite("Satellite", Icons.Filled.Public),
    Terrain  ("Terrain",   Icons.Filled.Terrain),
    ;

    fun tileSource(): ITileSource = when (this) {
        Streets   -> TileSourceFactory.MAPNIK
        Satellite -> EsriWorldImagery
        Terrain   -> OpenTopoMap
    }
}

/** ESRI World Imagery — high-res satellite, free for fair-use traffic.
 *  Note: ESRI's terms allow this for development + small-volume production.
 *  If your traffic grows past a few hundred MAUs, switch to a paid tier. */
private val EsriWorldImagery: OnlineTileSourceBase = object : OnlineTileSourceBase(
    "ESRIWorldImagery",
    0, 19, 256, ".png",
    arrayOf(
        "https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/",
    ),
    "© Esri, Maxar, Earthstar Geographics",
) {
    override fun getTileURLString(pMapTileIndex: Long): String =
        baseUrl +
            MapTileIndex.getZoom(pMapTileIndex) + "/" +
            MapTileIndex.getY(pMapTileIndex) + "/" +
            MapTileIndex.getX(pMapTileIndex)
}

/** OpenTopoMap — worldwide topographic style, free, no key.
 *  Built on OpenStreetMap + SRTM data, served from opentopomap.org. */
private val OpenTopoMap: OnlineTileSourceBase = object : OnlineTileSourceBase(
    "OpenTopoMap",
    0, 17, 256, ".png",
    arrayOf(
        "https://a.tile.opentopomap.org/",
        "https://b.tile.opentopomap.org/",
        "https://c.tile.opentopomap.org/",
    ),
    "© OpenTopoMap (CC-BY-SA), © OpenStreetMap contributors",
) {
    override fun getTileURLString(pMapTileIndex: Long): String =
        baseUrl +
            MapTileIndex.getZoom(pMapTileIndex) + "/" +
            MapTileIndex.getX(pMapTileIndex) + "/" +
            MapTileIndex.getY(pMapTileIndex) + ".png"
}

/* ─── Style switcher UI ───────────────────────────────────────────────── */

@Composable
private fun MapStyleSwitcher(
    selected: MapStyle,
    onSelect: (MapStyle) -> Unit,
    modifier: Modifier = Modifier,
) {
    // Floating segmented control: 3 icon buttons in a single pill.
    Row(
        modifier = modifier
            .shadow(8.dp, RoundedCornerShape(12.dp), spotColor = Color(0x33000000))
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White)
            .border(1.dp, Color(0x14000000), RoundedCornerShape(12.dp))
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        MapStyle.values().forEach { style ->
            StyleChip(
                style    = style,
                active   = style == selected,
                onClick  = { onSelect(style) },
            )
        }
    }
}

@Composable
private fun StyleChip(
    style  : MapStyle,
    active : Boolean,
    onClick: () -> Unit,
) {
    val bg   = if (active) Color(0xFF1A6BF5) else Color.Transparent
    val tint = if (active) Color.White else Color(0xFF4E6083)
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(9.dp))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = 9.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector        = style.icon,
            contentDescription = style.label,
            tint               = tint,
            modifier           = Modifier.size(16.dp),
        )
        if (active) {
            Text(
                text = style.label,
                color = tint,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 6.dp),
            )
        }
    }
}

/* ─── Custom marker helpers ───────────────────────────────────────────────
 *
 *  Each employee gets a marker drawable that contains:
 *    • a coloured avatar circle with their initials
 *    • a downward tail that points at the geo-point
 *    • a small white pill BELOW the pin with their first name
 *
 *  This gives every pin a permanent name label (delivery-app style) so the
 *  admin doesn't have to tap each marker to see who's where.
 */

/** Drawable + the normalised anchor (0..1) for the marker. Anchor X varies
 *  per name because the left-side pill widens the bitmap unevenly, so the
 *  geo-point (tail tip) is no longer at horizontal centre. */
private data class MarkerArt(
    val drawable: android.graphics.drawable.Drawable,
    val anchorX : Float,
    val anchorY : Float,
)

private fun buildEmployeeMarkerIcon(
    context : android.content.Context,
    name    : String,
    initials: String,
    accent  : Int,
): MarkerArt {
    val density   = context.resources.displayMetrics.density
    val circleDp  = 40f
    val tailDp    = 6f
    val gapDp     = 5f          // breathing room between pill and circle
    val labelHDp  = 18f         // label pill height
    val labelPadDp = 9f         // horizontal padding inside the pill
    val labelTextDp = 11f       // label font size in dp
    val padDp     = 6f          // bitmap outer padding (for shadows)

    // ── Measure label text width so the pill fits the first name ──
    val firstName = name.trim().split(Regex("\\s+")).firstOrNull()?.takeIf { it.isNotBlank() }
        ?: "—"
    val labelPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
        color    = 0xFF0D1B3E.toInt()       // Ink900
        textSize = labelTextDp * density
        textAlign = android.graphics.Paint.Align.CENTER
        typeface = android.graphics.Typeface.create(
            android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD,
        )
    }
    val labelTextWidth = labelPaint.measureText(firstName)
    val labelWidthPx = labelTextWidth + (labelPadDp * 2f * density)

    // ── Pixel sizes ──
    val padPx    = padDp * density
    val gapPx    = gapDp * density
    val circlePx = circleDp * density
    val tailPx   = tailDp * density
    val labelHPx = labelHDp * density

    // ── Bitmap dimensions ──
    // Layout (horizontally):  [pad] [pill] [gap] [circle] [pad]
    // Layout (vertically):    [pad] [circle + tail] [pad]
    // The pill sits to the LEFT of the circle, vertically centered with it.
    val widthPx  = (padPx + labelWidthPx + gapPx + circlePx + padPx).toInt()
    val heightPx = (padPx + circlePx + tailPx + padPx).toInt()

    val bitmap = android.graphics.Bitmap.createBitmap(
        widthPx, heightPx, android.graphics.Bitmap.Config.ARGB_8888,
    )
    val canvas = android.graphics.Canvas(bitmap)

    // Circle center.
    val cx     = padPx + labelWidthPx + gapPx + circlePx / 2f
    val cy     = padPx + circlePx / 2f
    val radius = circlePx / 2f

    // ── Soft drop shadow behind the avatar ──
    canvas.drawCircle(
        cx, cy + 2 * density, radius + 1 * density,
        android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            color = 0x29000000           // 16 % black
        },
    )

    // ── Tail (small triangle, tip points down to the geo-point) ──
    val tailHalfWidth = 5f * density
    val tailTopY      = cy + radius - 1f
    val tailTipY      = cy + radius + tailPx
    canvas.drawPath(
        android.graphics.Path().apply {
            moveTo(cx - tailHalfWidth, tailTopY)
            lineTo(cx, tailTipY)
            lineTo(cx + tailHalfWidth, tailTopY)
            close()
        },
        android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            color = accent
        },
    )

    // ── Main coloured circle ──
    canvas.drawCircle(
        cx, cy, radius,
        android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            color = accent
        },
    )

    // ── 2 dp white ring for crisp edge on any tile colour ──
    canvas.drawCircle(
        cx, cy, radius - 1f * density,
        android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            style       = android.graphics.Paint.Style.STROKE
            strokeWidth = 2f * density
            color       = android.graphics.Color.WHITE
        },
    )

    // ── Initials, bold white, centered ──
    val initialsPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
        color    = android.graphics.Color.WHITE
        textSize = 13f * density
        textAlign = android.graphics.Paint.Align.CENTER
        typeface = android.graphics.Typeface.create(
            android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD,
        )
    }
    val textVerticalOffset = (initialsPaint.descent() + initialsPaint.ascent()) / 2f
    canvas.drawText(initials.take(2).uppercase(), cx, cy - textVerticalOffset, initialsPaint)

    // ── Name pill to the LEFT of the circle, vertically centered with it ──
    val labelLeftX   = padPx
    val labelRightX  = labelLeftX + labelWidthPx
    val labelTopY    = cy - labelHPx / 2f
    val labelBottomY = cy + labelHPx / 2f
    val labelCornerR = labelHPx / 2f

    // Soft shadow under the pill.
    canvas.drawRoundRect(
        labelLeftX, labelTopY + 1.5f * density,
        labelRightX, labelBottomY + 1.5f * density,
        labelCornerR, labelCornerR,
        android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            color = 0x1F000000           // 12 % black
        },
    )

    // Pill background — white with a faint accent-tinted border.
    canvas.drawRoundRect(
        labelLeftX, labelTopY, labelRightX, labelBottomY,
        labelCornerR, labelCornerR,
        android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.WHITE
        },
    )
    canvas.drawRoundRect(
        labelLeftX, labelTopY, labelRightX, labelBottomY,
        labelCornerR, labelCornerR,
        android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            style       = android.graphics.Paint.Style.STROKE
            strokeWidth = 0.8f * density
            color       = (accent and 0x00FFFFFF) or 0x33000000   // accent @ 20 %
        },
    )

    // Label text — first name in bold Ink900, centered inside the pill.
    val labelCenterX = (labelLeftX + labelRightX) / 2f
    val labelTextDy  = (labelPaint.descent() + labelPaint.ascent()) / 2f
    canvas.drawText(firstName, labelCenterX, cy - labelTextDy, labelPaint)

    // Anchor: the GEO-POINT is at the tail tip. With the pill to the left,
    // the circle (and therefore the tail) is no longer at the bitmap's
    // horizontal centre — compute the actual normalised position so the
    // pin still lands exactly on the lat/lng.
    val anchorX = cx / widthPx.toFloat()
    val anchorY = tailTipY / heightPx.toFloat()

    return MarkerArt(
        drawable = android.graphics.drawable.BitmapDrawable(context.resources, bitmap),
        anchorX  = anchorX,
        anchorY  = anchorY,
    )
}

/** Stable accent colour per employee — hashed from the id. */
private fun colorForEmployee(id: String): Int {
    val palette = intArrayOf(
        0xFF1A6BF5.toInt(),   // brand blue
        0xFFEC4899.toInt(),   // pink
        0xFF14B8A6.toInt(),   // teal
        0xFFF59E0B.toInt(),   // amber
        0xFF7C3AED.toInt(),   // violet
        0xFF22C55E.toInt(),   // green
        0xFF06B6D4.toInt(),   // cyan
        0xFFF97316.toInt(),   // orange
    )
    val index = (id.hashCode() and Int.MAX_VALUE) % palette.size
    return palette[index]
}

/** First letter of first name + first letter of last name, uppercased. */
private fun initialsForName(name: String): String {
    val parts = name.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
    return when {
        parts.isEmpty() -> "??"
        parts.size == 1 -> parts[0].take(2).uppercase()
        else            -> (parts.first().take(1) + parts.last().take(1)).uppercase()
    }
}
