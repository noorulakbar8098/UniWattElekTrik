package com.example.uniwattelektrik.core.platform

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.uniwattelektrik.AndroidAppContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

/**
 * Loads the source image off the disk, draws annotation strokes onto a
 * mutable copy, saves the flattened JPEG to `cacheDir/annotated/`, and
 * returns a content:// URI via the existing FileProvider — drop-in
 * replacement for the original picked URI.
 */
actual suspend fun saveAnnotatedPhoto(
    sourceUri: String,
    strokes  : List<AnnotationStroke>,
): String = withContext(Dispatchers.IO) {
    val context = AndroidAppContext.application
    if (strokes.isEmpty()) return@withContext sourceUri

    val uri = Uri.parse(sourceUri)
    val source: Bitmap = context.contentResolver.openInputStream(uri).use { stream ->
        if (stream == null) return@withContext sourceUri
        BitmapFactory.decodeStream(stream)
    } ?: return@withContext sourceUri

    // Work on a mutable ARGB_8888 copy so we can paint over it.
    val mutable = source.copy(Bitmap.Config.ARGB_8888, /* isMutable = */ true)
    val canvas = Canvas(mutable)

    // dp → px scale relative to a "typical" 360dp-wide canvas, using the
    // actual bitmap width. Keeps stroke thickness proportional to the
    // image rather than to the original on-screen canvas size.
    val pxPerDp = mutable.width / 360f

    strokes.forEach { stroke ->
        if (stroke.points.size < 2) return@forEach
        val paint = Paint().apply {
            color = stroke.colorArgb.toInt()
            strokeWidth = (stroke.widthDp * pxPerDp).coerceAtLeast(2f)
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
            isAntiAlias = true
        }
        val path = Path().apply {
            val first = stroke.points.first()
            moveTo(first.x * mutable.width, first.y * mutable.height)
            for (i in 1 until stroke.points.size) {
                val p = stroke.points[i]
                lineTo(p.x * mutable.width, p.y * mutable.height)
            }
        }
        canvas.drawPath(path, paint)
    }

    // Reuse the existing FileProvider-mapped "shared/" cache subfolder so
    // the returned URI is shareable without a manifest change.
    val outDir = File(context.cacheDir, "shared").apply { mkdirs() }
    val outFile = File(outDir, "ann_${System.currentTimeMillis()}.jpg")
    FileOutputStream(outFile).use { fos ->
        mutable.compress(Bitmap.CompressFormat.JPEG, 90, fos)
    }
    source.recycle()
    mutable.recycle()

    val authority = "${context.packageName}.fileprovider"
    FileProvider.getUriForFile(context, authority, outFile).toString()
}
