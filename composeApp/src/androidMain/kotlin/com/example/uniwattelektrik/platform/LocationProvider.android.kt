package com.example.uniwattelektrik.platform

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.os.Build
import androidx.core.content.ContextCompat
import com.example.uniwattelektrik.AndroidAppContext
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Android `actual` for [LocationProvider].
 *
 * Uses Google Play Services FusedLocationProviderClient to get a fresh location
 * (preferred over `lastLocation` which can be stale), then attempts a reverse
 * geocode to resolve a human-readable place name.
 *
 * Returns `null` if permission is missing, location services are off, or the
 * request times out (10 s). Callers display a fallback string in that case.
 */
actual class LocationProvider {

    private val context: Context get() = AndroidAppContext.application
    private val client by lazy { LocationServices.getFusedLocationProviderClient(context) }

    @SuppressLint("MissingPermission") // we check explicitly below
    actual suspend fun getCurrentLocation(): LocationData? {
        if (!hasLocationPermission()) return null

        val location: Location = withTimeoutOrNull(10_000) {
            val token = CancellationTokenSource()
            try {
                client.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, token.token).await()
                    ?: client.lastLocation.await()
            } catch (t: Throwable) {
                null
            } finally {
                token.cancel()
            }
        } ?: return null

        val placeName = reverseGeocode(location.latitude, location.longitude)

        return LocationData(
            latitude     = location.latitude,
            longitude    = location.longitude,
            coordsText   = formatCoords(location.latitude, location.longitude),
            accuracyText = "accuracy ${location.accuracy.roundToInt()}m",
            placeName    = placeName,
        )
    }

    private fun hasLocationPermission(): Boolean {
        val fine = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_COARSE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }

    /**
     * Reverse-geocode a lat/lng to a "Locality, Sublocality" style string.
     *
     * On API 33+ we use the async callback API; on older versions we fall back
     * to the deprecated synchronous call. Returns `null` on any failure — the
     * caller will just show the coordinates instead.
     */
    private suspend fun reverseGeocode(lat: Double, lng: Double): String? = withContext(Dispatchers.IO) {
        if (!Geocoder.isPresent()) return@withContext null
        val geocoder = Geocoder(context)

        val address = try {
            if (Build.VERSION.SDK_INT >= 33) {
                suspendCancellableCoroutine { cont ->
                    geocoder.getFromLocation(lat, lng, 1) { results ->
                        cont.resume(results.firstOrNull())
                    }
                }
            } else {
                @Suppress("DEPRECATION")
                geocoder.getFromLocation(lat, lng, 1)?.firstOrNull()
            }
        } catch (_: Throwable) {
            null
        } ?: return@withContext null

        // Build a concise "Sublocality, Locality" label, falling back as fields are missing.
        listOfNotNull(
            address.subLocality?.takeIf { it.isNotBlank() } ?: address.featureName?.takeIf { it.isNotBlank() },
            address.locality?.takeIf { it.isNotBlank() } ?: address.adminArea?.takeIf { it.isNotBlank() },
        ).distinct().joinToString(", ").takeIf { it.isNotBlank() }
    }

    private fun formatCoords(lat: Double, lng: Double): String {
        val latStr = "${formatTo4(abs(lat))}°${if (lat >= 0) "N" else "S"}"
        val lngStr = "${formatTo4(abs(lng))}°${if (lng >= 0) "E" else "W"}"
        return "$latStr, $lngStr"
    }

    private fun formatTo4(value: Double): String {
        // Android (JVM) — String.format is available, gives consistent 4-decimal output.
        return "%.4f".format(value)
    }
}
