package com.example.uniwattelektrik.core.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

@Composable
actual fun rememberJobCardExporter(): JobCardExporter = remember {
    JobCardExporter { _, _, _ -> }
}
