package com.example.uniwattelektrik.platform

/**
 * iOS `actual` stub.
 *
 * TODO: implement with `CLLocationManager` (Core Location). For now we return
 * `null` so the UI falls back to its placeholder text. Once the iOS app gains
 * an `Info.plist` `NSLocationWhenInUseUsageDescription` entry and Core Location
 * is wired, replace this stub.
 */
actual class LocationProvider {
    actual suspend fun getCurrentLocation(): LocationData? = null
}
