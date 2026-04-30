package com.example.uniwattelektrik.platform

/**
 * Snapshot of the device's current location.
 *
 * `coordsText` and `accuracyText` are pre-formatted by the platform `actual`
 * implementation so commonMain code never has to deal with locale-specific
 * number formatting (which is JVM-only on the stdlib).
 */
data class LocationData(
    val latitude: Double,
    val longitude: Double,
    val coordsText: String,    // e.g. "13.0827°N, 77.5877°E"
    val accuracyText: String,  // e.g. "accuracy 4m"
    val placeName: String?,    // e.g. "Hebbal, Bengaluru" — null if reverse-geocode fails
)

/**
 * Platform-specific location source.
 *
 * `actual` implementations:
 *  - androidMain → FusedLocationProviderClient + Geocoder
 *  - iosMain     → CLLocationManager (stub for now)
 *
 * Returns `null` when permission is denied, the device has location off,
 * or the request times out — callers should fall back to last-known data
 * or a placeholder string.
 */
expect class LocationProvider() {
    suspend fun getCurrentLocation(): LocationData?
}
