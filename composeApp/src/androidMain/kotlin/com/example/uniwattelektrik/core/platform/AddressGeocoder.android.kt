package com.example.uniwattelektrik.core.platform

import android.location.Geocoder
import android.os.Build
import com.example.uniwattelektrik.AndroidAppContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.coroutines.resume

actual class AddressGeocoder internal constructor() {

    private val geocoder by lazy {
        Geocoder(AndroidAppContext.application, Locale.getDefault())
    }

    actual suspend fun geocode(query: String): GeoLatLng? {
        if (query.isBlank()) return null
        return runCatching {
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
    }
}

actual fun createAddressGeocoder(): AddressGeocoder = AddressGeocoder()
