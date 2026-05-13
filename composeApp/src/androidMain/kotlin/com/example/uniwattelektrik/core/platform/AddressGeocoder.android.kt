package com.example.uniwattelektrik.core.platform

import android.location.Geocoder
import android.os.Build
import com.example.uniwattelektrik.AndroidAppContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.Locale
import kotlin.coroutines.resume

actual class AddressGeocoder internal constructor() {

    private val geocoder by lazy {
        Geocoder(AndroidAppContext.application, Locale.getDefault())
    }

    actual suspend fun geocode(query: String): GeoLatLng? {
        if (query.isBlank()) return null
        val native = runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                suspendCancellableCoroutine { cont ->
                    geocoder.getFromLocationName(query, 1) { addresses ->
                        val first = addresses.firstOrNull()
                        cont.resume(
                            first?.let { GeoLatLng(it.latitude, it.longitude) },
                        )
                    }
                }
            } else {
                @Suppress("DEPRECATION")
                withContext(Dispatchers.IO) {
                    val results = geocoder.getFromLocationName(query, 1)
                    results?.firstOrNull()?.let { GeoLatLng(it.latitude, it.longitude) }
                }
            }
        }.getOrNull()
        if (native != null) return native
        // Fallback: Nominatim HTTP search → guarantees a result whenever the
        // device has internet, even on emulators or AOSP builds that ship
        // without Google's Geocoder backend.
        return nominatimSearch(query, limit = 1).firstOrNull()
            ?.let { GeoLatLng(it.latitude, it.longitude) }
    }

    actual suspend fun suggest(query: String, limit: Int): List<AddressSuggestion> {
        if (query.length < 3) return emptyList()
        val capped = limit.coerceIn(1, 10)
        val native = runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                suspendCancellableCoroutine { cont ->
                    geocoder.getFromLocationName(query, capped) { addresses ->
                        cont.resume(addresses.map(::toSuggestion))
                    }
                }
            } else {
                @Suppress("DEPRECATION")
                withContext(Dispatchers.IO) {
                    geocoder.getFromLocationName(query, capped)
                        ?.map(::toSuggestion)
                        ?: emptyList()
                }
            }
        }.getOrDefault(emptyList())
        if (native.isNotEmpty()) return native
        // OS Geocoder returned nothing (typical on emulator / no Play Services).
        // Hit the OSM Nominatim public API so the dropdown always populates.
        return nominatimSearch(query, capped)
    }

    /**
     * Lightweight Nominatim REST call — no extra deps. Returns up to [limit]
     * suggestions or an empty list on any failure (offline, rate-limited,
     * malformed JSON). We keep this best-effort because the UI degrades
     * gracefully when the dropdown is empty.
     */
    private suspend fun nominatimSearch(query: String, limit: Int): List<AddressSuggestion> =
        withContext(Dispatchers.IO) {
            runCatching {
                val q = URLEncoder.encode(query, "UTF-8")
                val url = URL(
                    "https://nominatim.openstreetmap.org/search?q=$q&format=json&limit=$limit&addressdetails=0",
                )
                val conn = (url.openConnection() as HttpURLConnection).apply {
                    connectTimeout = 4_000
                    readTimeout = 4_000
                    requestMethod = "GET"
                    // Nominatim's usage policy requires a descriptive UA.
                    setRequestProperty(
                        "User-Agent",
                        "UniWattElekTrik/1.0 (Android; address-suggest)",
                    )
                    setRequestProperty("Accept", "application/json")
                }
                val text = conn.inputStream.bufferedReader().use { it.readText() }
                parseNominatim(text, limit)
            }.getOrDefault(emptyList())
        }

    /**
     * Tiny hand-rolled JSON parser for the Nominatim array response. We avoid
     * pulling in kotlinx.serialization just for this fallback — the response
     * shape is stable and only three fields (`display_name`, `lat`, `lon`)
     * are needed.
     */
    private fun parseNominatim(json: String, limit: Int): List<AddressSuggestion> {
        val out = mutableListOf<AddressSuggestion>()
        // Split on top-level objects. Nominatim's response is a flat array of
        // `{ … }` entries; this naive split works because none of the values
        // we read contain unescaped `}{` sequences.
        val objs = Regex("\\{[^\\{\\}]*\\}").findAll(json)
        for (m in objs) {
            if (out.size >= limit) break
            val obj = m.value
            val label = extractJsonString(obj, "display_name") ?: continue
            val lat = extractJsonString(obj, "lat")?.toDoubleOrNull() ?: continue
            val lon = extractJsonString(obj, "lon")?.toDoubleOrNull() ?: continue
            out += AddressSuggestion(label = label, latitude = lat, longitude = lon)
        }
        return out
    }

    private fun extractJsonString(obj: String, key: String): String? {
        val pattern = Regex("\"" + Regex.escape(key) + "\"\\s*:\\s*\"([^\"]*)\"")
        return pattern.find(obj)?.groupValues?.get(1)
    }

    /**
     * Build a clean single-line label by concatenating the address lines
     * the Geocoder returned. Skips blanks; falls back to feature name +
     * locality + admin area when the multi-line breakdown is sparse.
     */
    private fun toSuggestion(a: android.location.Address): AddressSuggestion {
        val lines = (0..a.maxAddressLineIndex.coerceAtLeast(-1))
            .mapNotNull { a.getAddressLine(it)?.takeIf { line -> line.isNotBlank() } }
        val label = if (lines.isNotEmpty()) {
            lines.joinToString(", ")
        } else {
            listOfNotNull(a.featureName, a.locality, a.adminArea, a.countryName)
                .filter { it.isNotBlank() }
                .joinToString(", ")
                .ifBlank { "${a.latitude}, ${a.longitude}" }
        }
        return AddressSuggestion(
            label     = label,
            latitude  = a.latitude,
            longitude = a.longitude,
        )
    }
}

actual fun createAddressGeocoder(): AddressGeocoder = AddressGeocoder()
