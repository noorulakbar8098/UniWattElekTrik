package com.example.uniwattelektrik.core.platform

import androidx.compose.runtime.Composable

/**
 * Returns a launcher lambda — invoke it to open the system file picker filtered
 * to .xlsx/.xls. When the user selects a file, [onPicked] is called with the
 * platform-specific URI string (Android: content:// Uri; iOS: file:// path).
 */
@Composable
expect fun rememberExcelFilePicker(onPicked: (String) -> Unit): () -> Unit

