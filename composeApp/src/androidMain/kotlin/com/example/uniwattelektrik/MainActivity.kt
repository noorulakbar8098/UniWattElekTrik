package com.example.uniwattelektrik

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // Switch from the splash theme (blue window background) back to the normal theme
        // before Compose renders, so there is no blue-under-white flash.
        setTheme(android.R.style.Theme_Material_Light_NoActionBar)
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        // Initialise the shared AppContext used by `actual` platform classes
        // (e.g. SessionStorage). A custom Application class would be cleaner
        // but this keeps the template simple.
        AndroidAppContext.init(applicationContext)

        setContent {
            App()
        }
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}