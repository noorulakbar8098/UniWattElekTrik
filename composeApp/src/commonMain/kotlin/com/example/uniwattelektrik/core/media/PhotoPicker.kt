package com.example.uniwattelektrik.core.media

import androidx.compose.runtime.Composable

/**
 * Cross-platform single-image picker.
 *
 *  - Android: backed by `ActivityResultContracts.PickVisualMedia` (no runtime
 *    permission needed).
 *  - iOS: stub returning a no-op handle for now.
 *
 * Returned [PhotoPickerHandle.uri] is a platform-neutral URI string ready to
 * hand to [com.example.uniwattelektrik.feature.workforce.data.remote.EmployeeDraft].
 */
class PhotoPickerHandle(
    val uri: String?,
    val pick: () -> Unit,
)

@Composable
expect fun rememberPhotoPicker(): PhotoPickerHandle

