package com.example.uniwattelektrik.core.platform

/**
 * iOS no-op — returns the original URI unchanged. A native impl using
 * UIGraphicsImageRenderer can be added later when the iOS flow ships.
 */
actual suspend fun saveAnnotatedPhoto(
    sourceUri: String,
    strokes  : List<AnnotationStroke>,
): String = sourceUri
