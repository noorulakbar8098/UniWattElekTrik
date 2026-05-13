package com.example.uniwattelektrik.core.platform

actual class AddressGeocoder internal constructor() {
    actual suspend fun geocode(query: String): GeoLatLng? = null
    actual suspend fun suggest(query: String, limit: Int): List<AddressSuggestion> = emptyList()
}

actual fun createAddressGeocoder(): AddressGeocoder = AddressGeocoder()

