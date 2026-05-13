package com.example.uniwattelektrik.core.platform

/**
 * Photo-annotation save pipeline — pure data, no Compose.
 *
 * The annotator UI in [com.example.uniwattelektrik.core.components.PhotoAnnotatorOverlay]
 * captures finger-drawn strokes in normalised canvas coords (0..1) so the
 * same stroke definition renders correctly regardless of canvas size.
 *
 * Platform implementation: load the source image, scale each stroke's
 * normalised coords to the bitmap's pixel size, paint the strokes onto a
 * mutable copy, save as JPEG to the cache dir, return a sharable URI
 * suitable for passing to
 * [com.example.uniwattelektrik.feature.workforce.data.remote.WorkforceDirectory.uploadTaskAttachment].
 *
 * When [strokes] is empty the implementation may return [sourceUri] unchanged
 * (no flatten needed).
 */
expect suspend fun saveAnnotatedPhoto(
    sourceUri: String,
    strokes  : List<AnnotationStroke>,
): String

data class AnnotationStroke(
    /** ARGB color packed as Long (e.g. 0xFFFF3B30 for red). */
    val colorArgb: Long,
    /** Normalised canvas coords — each point's x,y in 0..1. */
    val points   : List<NormalizedPoint>,
    /** Pen width in dp at draw time. The Android impl multiplies by the
     *  bitmap's px-density-equivalent so strokes scale with image size. */
    val widthDp  : Float = 4f,
)

data class NormalizedPoint(val x: Float, val y: Float)
