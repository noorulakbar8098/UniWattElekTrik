package com.example.uniwattelektrik.core.platform

actual class AddressGeocoder internal constructor() {
    actual suspend fun geocode(query: String): GeoLatLng? = null
}

actual fun createAddressGeocoder(): AddressGeocoder = AddressGeocoder()

