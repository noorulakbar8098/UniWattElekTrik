package com.example.uniwattelektrik

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.content.ContextCompat

class MainActivity : ComponentActivity() {

    /**
     * Registered once at activity creation. Result is intentionally ignored —
     * [com.example.uniwattelektrik.platform.LocationProvider] re-checks the
     * permission at every call site and returns `null` if it's missing, so
     * the UI degrades gracefully whether or not the user grants it.
     */
    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { /* no-op */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Switch from the splash theme (blue window background) back to the normal theme
        // before Compose renders, so there is no blue-under-white flash.
        setTheme(android.R.style.Theme_Material_Light_NoActionBar)
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        // Status-bar tint is now published per-screen via `SetStatusBar(...)`,
        // which writes `window.statusBarColor` + `isAppearanceLightStatusBars`
        // on every Compose recomposition. We only enable edge-to-edge here.

        // Initialise the shared AppContext used by `actual` platform classes
        // (e.g. SessionStorage, LocationProvider).
        AndroidAppContext.init(applicationContext)

        // Ask for location at startup. The system dialog is shown only the first
        // time; on subsequent launches this is a silent no-op if already granted.
        if (!hasLocationPermission()) {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                ),
            )
        }

        setContent {
            App()
        }
    }

    private fun hasLocationPermission(): Boolean {
        val fine = ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_FINE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_COARSE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}
