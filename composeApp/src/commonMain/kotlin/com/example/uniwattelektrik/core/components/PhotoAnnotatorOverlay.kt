package com.example.uniwattelektrik.core.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.example.uniwattelektrik.core.platform.AnnotationStroke
import com.example.uniwattelektrik.core.platform.NormalizedPoint
import com.example.uniwattelektrik.core.platform.saveAnnotatedPhoto
import com.example.uniwattelektrik.core.theme.AppTheme
import kotlinx.coroutines.launch

/* ═══════════════════════════════════════════════════════════════════════════
 *  PhotoAnnotatorOverlay
 * ═══════════════════════════════════════════════════════════════════════════
 *
 *  Full-screen dark overlay that lets the user finger-draw annotations on a
 *  picked photo before it's uploaded. Use case: a technician highlights a
 *  loose wire, a wet connection, or marks a part number with a coloured pen.
 *
 *  Stroke coords are tracked in normalised (0..1) form so the resulting
 *  [AnnotationStroke] list re-renders correctly at any bitmap resolution
 *  when the platform [saveAnnotatedPhoto] flattens them into a JPEG.
 *
 *  UX rules:
 *    • Source image is centered (Fit) so aspect ratio is preserved.
 *    • Drawing canvas matches the image's bounding box, not the screen, so
 *      coords stay accurate when the user crops near image edges.
 *    • Top bar: cancel · undo · done.
 *    • Bottom bar: 4 colour swatches (red / amber / cyan / white) + a
 *      "Clear all" affordance.
 *
 *  When done, [onDone] is invoked on the main thread with the URI of the
 *  flattened JPEG (Android) or the original URI (iOS stub).
 */
@Composable
fun PhotoAnnotatorOverlay(
    sourceUri: String,
    onDone   : (String) -> Unit,
    onCancel : () -> Unit,
    modifier : Modifier = Modifier,
) {
    // ── Committed strokes ─ never mutated mid-drag, so the existing list
    //   stays stable and Compose doesn't re-walk it 60+ times/second.
    var committed by remember { mutableStateOf<List<AnnotationStroke>>(emptyList()) }
    // ── In-progress stroke ─ a single immutable list that grows as the user
    //   drags. Replaced wholesale per tick (cheap because we drop near-
    //   duplicate points before appending).
    var currentPoints by remember { mutableStateOf<List<NormalizedPoint>>(emptyList()) }
    // Snapshot of the colour used for the in-progress stroke so changing
    // the pen colour mid-drag doesn't repaint earlier segments.
    var currentArgb by remember { mutableStateOf(PenColor.Red.argbLong) }
    var penColor by remember { mutableStateOf(PenColor.Red) }
    var isExporting by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0A0F1F)),     // dark editor canvas
    ) {
        // ── Image + drawing canvas ────────────────────────────────────────
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 64.dp, bottom = 96.dp),
            contentAlignment = Alignment.Center,
        ) {
            val maxW = maxWidth
            val maxH = maxHeight
            Box(
                modifier = Modifier
                    .size(width = maxW, height = maxH)
                    .clip(RoundedCornerShape(20.dp)),
                contentAlignment = Alignment.Center,
            ) {
                AsyncImage(
                    model              = sourceUri,
                    contentDescription = "Photo to annotate",
                    contentScale       = ContentScale.Fit,
                    modifier           = Modifier.fillMaxSize(),
                )

                // Overlay drawing canvas — matches the same Box so the
                // coordinate system aligns with what the user sees. Coords
                // are normalised to canvas size so they re-scale correctly
                // when the platform flattens them onto the source bitmap.
                //
                // Perf rules:
                //  1. `committed` only mutates on drag-end → no work mid-stroke.
                //  2. `currentPoints` filters near-duplicate points so a fast
                //     finger doesn't queue 200 redundant points/sec.
                //  3. Path objects are built per draw call but reused via
                //     `rewind` — far cheaper than `Path()` allocation in the
                //     hot path.
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectDragGestures(
                                onDragStart = { offset ->
                                    currentArgb = penColor.argbLong
                                    currentPoints = listOf(
                                        offset.toNormalized(this.size.width.toFloat(), this.size.height.toFloat())
                                    )
                                },
                                onDrag = { change, _ ->
                                    val p = change.position.toNormalized(
                                        this.size.width.toFloat(),
                                        this.size.height.toFloat(),
                                    )
                                    val last = currentPoints.lastOrNull()
                                    // Skip points within 0.2 % of the canvas
                                    // in both axes — visually identical,
                                    // saves draw-list size.
                                    if (last != null &&
                                        kotlin.math.abs(last.x - p.x) < 0.002f &&
                                        kotlin.math.abs(last.y - p.y) < 0.002f
                                    ) return@detectDragGestures
                                    currentPoints = currentPoints + p
                                },
                                onDragEnd = {
                                    if (currentPoints.size >= 2) {
                                        committed = committed + AnnotationStroke(
                                            colorArgb = currentArgb,
                                            points    = currentPoints,
                                        )
                                    }
                                    currentPoints = emptyList()
                                },
                                onDragCancel = { currentPoints = emptyList() },
                            )
                        },
                ) {
                    val w = size.width
                    val h = size.height
                    val strokeStyle = Stroke(
                        width = 5.dp.toPx(),
                        cap   = StrokeCap.Round,
                        join  = StrokeJoin.Round,
                    )
                    val scratch = Path()
                    // Committed strokes — stable until the next drag-end.
                    committed.forEach { s ->
                        if (s.points.size < 2) return@forEach
                        scratch.rewind()
                        val first = s.points.first()
                        scratch.moveTo(first.x * w, first.y * h)
                        for (i in 1 until s.points.size) {
                            val p = s.points[i]
                            scratch.lineTo(p.x * w, p.y * h)
                        }
                        drawPath(
                            // Color(Int) interprets the bits as ARGB. The
                            // ULong constructor expects an internal packed
                            // representation including a color-space index;
                            // passing an ARGB long there crashed with
                            // ArrayIndexOutOfBoundsException at index 58.
                            path  = scratch,
                            color = Color(s.colorArgb.toInt()),
                            style = strokeStyle,
                        )
                    }
                    // In-progress stroke — only this re-evaluates per tick.
                    if (currentPoints.size >= 2) {
                        scratch.rewind()
                        val first = currentPoints.first()
                        scratch.moveTo(first.x * w, first.y * h)
                        for (i in 1 until currentPoints.size) {
                            val p = currentPoints[i]
                            scratch.lineTo(p.x * w, p.y * h)
                        }
                        drawPath(
                            path  = scratch,
                            color = Color(currentArgb.toInt()),
                            style = strokeStyle,
                        )
                    }
                }
            }
        }

        // ── Top bar — cancel · undo · done ────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            EditorChip(label = "Cancel", onClick = onCancel)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                EditorChip(
                    label    = "Undo",
                    enabled  = committed.isNotEmpty(),
                    onClick  = { if (committed.isNotEmpty()) committed = committed.dropLast(1) },
                )
                EditorChip(
                    label    = if (isExporting) "Saving…" else "Done",
                    primary  = true,
                    enabled  = !isExporting,
                    onClick  = {
                        if (isExporting) return@EditorChip
                        isExporting = true
                        val finalStrokes = committed
                        scope.launch {
                            val out = runCatching { saveAnnotatedPhoto(sourceUri, finalStrokes) }
                                .getOrElse { sourceUri }
                            onDone(out)
                        }
                    },
                )
            }
        }

        // ── Bottom toolbar — colour swatches + clear ──────────────────────
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 24.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Spacer(Modifier.weight(1f))
            PenColor.values().forEach { c ->
                SwatchDot(
                    color    = c.swatch,
                    selected = c == penColor,
                    onClick  = { penColor = c },
                )
            }
            Spacer(Modifier.width(8.dp))
            // Clear-all — kept text-only, no destructive colour, so it
            // doesn't visually shout louder than the colour picker.
            EditorChip(
                label   = "Clear",
                enabled = committed.isNotEmpty() || currentPoints.isNotEmpty(),
                onClick = {
                    committed = emptyList()
                    currentPoints = emptyList()
                },
            )
            Spacer(Modifier.weight(1f))
        }
    }
}

private fun Offset.toNormalized(w: Float, h: Float) = NormalizedPoint(
    x = (x / w).coerceIn(0f, 1f),
    y = (y / h).coerceIn(0f, 1f),
)

/* ─── Editor primitives ──────────────────────────────────────────────── */

@Composable
private fun EditorChip(
    label   : String,
    onClick : () -> Unit,
    primary : Boolean = false,
    enabled : Boolean = true,
) {
    val bg = when {
        primary && enabled -> AppTheme.Brand
        primary            -> AppTheme.Brand.copy(alpha = 0.40f)
        else               -> Color.White.copy(alpha = 0.12f)
    }
    val text = if (primary) Color.White
               else Color.White.copy(alpha = if (enabled) 0.92f else 0.40f)
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(bg)
            .border(1.dp, Color.White.copy(alpha = if (primary) 0.18f else 0.12f), RoundedCornerShape(10.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = text, fontSize = 13.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun SwatchDot(color: Color, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(color)
            .border(
                width = if (selected) 3.dp else 1.dp,
                color = if (selected) Color.White else Color.White.copy(alpha = 0.25f),
                shape = CircleShape,
            )
            .clickable(onClick = onClick),
    )
}

private enum class PenColor(val swatch: Color, val argbLong: Long) {
    Red   (Color(0xFFFF453A), 0xFFFF453AL),
    Amber (Color(0xFFFFB02E), 0xFFFFB02EL),
    Cyan  (Color(0xFF32D6FF), 0xFF32D6FFL),
    White (Color(0xFFFFFFFF), 0xFFFFFFFFL),
}

