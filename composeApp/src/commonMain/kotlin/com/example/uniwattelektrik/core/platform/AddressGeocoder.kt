package com.example.uniwattelektrik.core.platform

/** Latitude / longitude pair returned from a successful geocode. */
data class GeoLatLng(val latitude: Double, val longitude: Double)

/**
 * Resolves an address string into a [GeoLatLng] using the platform's
 * native geocoder.
 *
 * - Android: backed by `android.location.Geocoder` (offline / Google Play
 *   Services depending on device).
 * - iOS: stub returning `null` until a `CLGeocoder` actual is wired.
 *
 * Implementations must be safe to call from a coroutine on `Dispatchers.IO`.
 */
expect class AddressGeocoder {
    suspend fun geocode(query: String): GeoLatLng?
}

/** Construct an [AddressGeocoder] for the current platform. */
expect fun createAddressGeocoder(): AddressGeocoder

