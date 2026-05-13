package com.example.uniwattelektrik.core.platform

/** Latitude / longitude pair returned from a successful geocode. */
data class GeoLatLng(val latitude: Double, val longitude: Double)

/**
 * One autocomplete suggestion returned by [AddressGeocoder.suggest]. The
 * [label] is a human-readable formatted address (e.g. "MG Road, Bengaluru,
 * Karnataka 560001, India") suitable for showing in a dropdown.
 */
data class AddressSuggestion(
    val label    : String,
    val latitude : Double,
    val longitude: Double,
)

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

    /**
     * Returns up to [limit] suggestions for an incomplete query. Empty list
     * if no matches, the query is too short, or the platform has no
     * autocomplete provider. Implementations should debounce externally —
     * the geocoder itself is called directly per invocation.
     */
    suspend fun suggest(query: String, limit: Int = 5): List<AddressSuggestion>
}

/** Construct an [AddressGeocoder] for the current platform. */
expect fun createAddressGeocoder(): AddressGeocoder

