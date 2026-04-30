package com.example.uniwattelektrik.core.media

import androidx.compose.runtime.Composable

@Composable
actual fun rememberPhotoPicker(): PhotoPickerHandle =
    PhotoPickerHandle(uri = null, pick = { /* iOS picker not implemented yet */ })

