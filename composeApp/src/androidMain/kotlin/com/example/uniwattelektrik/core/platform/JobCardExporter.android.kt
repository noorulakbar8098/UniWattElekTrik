package com.example.uniwattelektrik.core.platform

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import com.example.uniwattelektrik.feature.workforce.data.remote.ChecklistItem
import com.example.uniwattelektrik.feature.workforce.data.remote.MaterialUsedItem
import com.example.uniwattelektrik.feature.workforce.data.remote.TaskRecord
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
actual fun rememberJobCardExporter(): JobCardExporter {
    val context = LocalContext.current
    return remember(context) {
        JobCardExporter { task, prices, filename ->
            runCatching { renderAndShare(context, task, prices, filename) }
                .onFailure { it.printStackTrace() }
        }
    }
}

/* ─── Page geometry (A4 portrait, points) ─────────────────────────────── */

private const val PAGE_W = 595
private const val PAGE_H = 842
private const val MARGIN = 36f
private const val FOOTER_RESERVE = 56f

/* ─── Brand palette (matches AppTheme) ────────────────────────────────── */

private const val BRAND        = 0xFF1A6BF5.toInt()
private const val INK_900      = 0xFF0D1B3E.toInt()
private const val INK_700      = 0xFF253354.toInt()
private const val INK_500      = 0xFF5B6783.toInt()
private const val SURFACE_MUTED = 0xFFF3F5FA.toInt()
private const val SUCCESS      = 0xFF16A34A.toInt()
private const val DANGER       = 0xFFDC2626.toInt()

/* ─── Renderer ───────────────────────────────────────────────────────── */

private fun renderAndShare(
    context : Context,
    task    : TaskRecord,
    prices  : Map<String, Double>,
    filename: String,
) {
    val doc = PdfDocument()
    val draw = JobCardDraw(doc, task, prices)
    draw.startPage()
    draw.taskHeadline()
    draw.metaGrid()
    draw.locationCard()
    draw.descriptionBlock()
    draw.checklistBlock()
    draw.materialsBlock()
    draw.resolutionBlock()
    draw.signoffBlock()
    draw.footer()
    draw.finish()

    val shareDir = File(context.cacheDir, "shared").apply { mkdirs() }
    val outFile  = File(shareDir, filename)
    outFile.outputStream().use { doc.writeTo(it) }
    doc.close()

    val authority = "${context.packageName}.fileprovider"
    val uri = FileProvider.getUriForFile(context, authority, outFile)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "application/pdf"
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(Intent.EXTRA_SUBJECT, filename)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    val chooser = Intent.createChooser(intent, "Share $filename").apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    context.startActivity(chooser)
}

private class JobCardDraw(
    private val doc      : PdfDocument,
    private val task     : TaskRecord,
    private val priceById: Map<String, Double>,
) {

    private var pageNo = 0
    private var page: PdfDocument.Page? = null
    private var y = 0f

    fun startPage() = newPage(withHeader = true)

    private fun newPage(withHeader: Boolean) {
        page?.let { doc.finishPage(it) }
        pageNo += 1
        val info = PdfDocument.PageInfo.Builder(PAGE_W, PAGE_H, pageNo).create()
        page = doc.startPage(info)
        y = MARGIN
        if (withHeader) drawHeader()
    }

    private fun ensureSpace(needed: Float) {
        if (y + needed > PAGE_H - FOOTER_RESERVE) newPage(withHeader = false)
    }

    /* ── Header bar ───────────────────────────────────────────────── */

    private fun drawHeader() {
        val c = page!!.canvas
        c.drawRect(RectF(0f, 0f, PAGE_W.toFloat(), 96f), fillPaint(BRAND))
        c.drawText(
            "UNIWATT ELEKTRIK · COMPLETION CERTIFICATE",
            MARGIN, 32f,
            textPaint(Color.WHITE, 10f, bold = true, letterSpacing = 0.15f),
        )
        c.drawText(
            "Job Card",
            MARGIN, 60f,
            textPaint(Color.WHITE, 22f, bold = true),
        )
        val docId = "JC-${task.id.take(8).uppercase()}"
        val idPaint = textPaint(Color.WHITE, 12f, bold = true)
        val w = idPaint.measureText(docId)
        c.drawText(docId, PAGE_W - MARGIN - w, 60f, idPaint)
        y = 96f + 18f

        // Status pill, just under the header
        val statusLabel = task.status.uppercase()
        val statusTint = when (task.status.lowercase()) {
            "done" -> SUCCESS
            "todo" -> INK_500
            else   -> BRAND
        }
        val pillPaint = textPaint(Color.WHITE, 10f, bold = true, letterSpacing = 0.15f)
        val pillW = pillPaint.measureText(statusLabel) + 20f
        c.drawRoundRect(RectF(MARGIN, y, MARGIN + pillW, y + 18f), 9f, 9f, fillPaint(statusTint))
        c.drawText(statusLabel, MARGIN + 10f, y + 12.5f, pillPaint)
        y += 26f
    }

    /* ── Task headline ────────────────────────────────────────────── */

    fun taskHeadline() {
        val c = page!!.canvas
        ensureSpace(40f)
        c.drawText(
            task.title.ifBlank { "Untitled task" }.take(64),
            MARGIN, y + 16f,
            textPaint(INK_900, 18f, bold = true),
        )
        y += 22f
        val sub = listOfNotNull(
            task.departmentName.takeIf { it.isNotBlank() },
            task.equipmentName.takeIf { it.isNotBlank() },
        ).joinToString("  ·  ")
        if (sub.isNotBlank()) {
            c.drawText(sub, MARGIN, y + 12f, textPaint(INK_500, 11f))
            y += 18f
        }
        y += 6f
    }

    /* ── Meta grid (two columns) ──────────────────────────────────── */

    fun metaGrid() {
        val rows = listOf(
            "Priority"   to task.priority.replaceFirstChar { it.uppercase() }.ifBlank { "—" },
            "Created"    to formatDate(task.createdAtMs),
            "Accepted"   to formatDate(task.acceptedAt),
            "Completed"  to formatDate(task.completedAt),
            "Assignee"   to task.assigneeName.ifBlank { "—" },
            "Approver"   to task.ownerAdminName.ifBlank { "—" },
            "Duration"   to formatDuration(task.totalWorkDurationMs),
            "Downtime"   to "${task.downtimeMinutes}m",
        )
        ensureSpace(rows.size / 2f * 26f + 24f)
        val c = page!!.canvas
        c.drawRoundRect(
            RectF(MARGIN, y, PAGE_W - MARGIN, y + (rows.size / 2 * 26f + 12f)),
            10f, 10f, fillPaint(SURFACE_MUTED),
        )
        val colW = (PAGE_W - MARGIN * 2 - 24f) / 2f
        rows.chunked(2).forEachIndexed { rowIdx, pair ->
            val rowY = y + 12f + rowIdx * 26f
            pair.forEachIndexed { colIdx, (label, value) ->
                val x = MARGIN + 12f + colIdx * (colW + 12f)
                c.drawText(label.uppercase(), x, rowY + 8f,
                    textPaint(INK_500, 8f, bold = true, letterSpacing = 0.15f))
                c.drawText(value, x, rowY + 22f,
                    textPaint(INK_900, 11f, bold = true))
            }
        }
        y += rows.size / 2 * 26f + 18f
    }

    /* ── Location card ────────────────────────────────────────────── */

    fun locationCard() {
        val parts = listOfNotNull(
            task.location.takeIf { it.isNotBlank() },
            task.address.takeIf { it.isNotBlank() },
        )
        val hasCoords = task.latitude != null && task.longitude != null
        if (parts.isEmpty() && !hasCoords) return
        sectionLabel("SITE")
        ensureSpace(50f)
        val c = page!!.canvas
        val h = 14f + parts.size * 14f + (if (hasCoords) 14f else 0f) + 12f
        c.drawRoundRect(
            RectF(MARGIN, y, PAGE_W - MARGIN, y + h),
            8f, 8f, fillPaint(SURFACE_MUTED),
        )
        var iy = y + 16f
        parts.forEach { line ->
            c.drawText(line.take(96), MARGIN + 12f, iy, textPaint(INK_900, 11f))
            iy += 14f
        }
        if (hasCoords) {
            val coordLabel = "GPS: ${"%.5f".format(task.latitude)}, ${"%.5f".format(task.longitude)}"
            c.drawText(coordLabel, MARGIN + 12f, iy, textPaint(INK_700, 10f))
        }
        y += h + 8f
    }

    /* ── Description ──────────────────────────────────────────────── */

    fun descriptionBlock() {
        val text = task.description.trim()
        if (text.isEmpty()) return
        sectionLabel("SCOPE OF WORK")
        wrappedText(text, fontSize = 11f, color = INK_900)
        y += 6f
    }

    /* ── Checklist ────────────────────────────────────────────────── */

    fun checklistBlock() {
        if (task.checklist.isEmpty()) return
        sectionLabel("CHECKLIST")
        task.checklist.forEach { item -> checklistRow(item) }
        y += 6f
    }

    private fun checklistRow(item: ChecklistItem) {
        ensureSpace(20f)
        val c = page!!.canvas
        val tint = if (item.done) SUCCESS else INK_500
        val box = RectF(MARGIN + 2f, y + 4f, MARGIN + 14f, y + 16f)
        c.drawRoundRect(box, 3f, 3f, strokePaint(tint, 1.2f))
        if (item.done) {
            // Simple check mark
            val checkPaint = strokePaint(tint, 1.6f)
            c.drawLine(box.left + 2f, box.centerY(), box.centerX(), box.bottom - 2f, checkPaint)
            c.drawLine(box.centerX(), box.bottom - 2f, box.right - 1f, box.top + 1f, checkPaint)
        }
        c.drawText(
            item.text.take(86),
            MARGIN + 22f, y + 14f,
            textPaint(if (item.done) INK_900 else INK_500, 11f,
                bold = item.done),
        )
        y += 18f
    }

    /* ── Materials table ──────────────────────────────────────────── */

    fun materialsBlock() {
        if (task.materialsUsed.isEmpty()) return
        sectionLabel("MATERIALS USED")
        // Column header
        ensureSpace(20f)
        val c = page!!.canvas
        val headerPaint = textPaint(INK_500, 8f, bold = true, letterSpacing = 0.15f)
        c.drawText("ITEM",       MARGIN,           y + 10f, headerPaint)
        c.drawText("QTY",        MARGIN + 280f,    y + 10f, headerPaint)
        c.drawText("UNIT PRICE", MARGIN + 340f,    y + 10f, headerPaint)
        val totalLabelX = PAGE_W - MARGIN - 70f
        c.drawText("SUBTOTAL",   totalLabelX,      y + 10f, headerPaint)
        y += 14f

        var grandTotal = 0.0
        task.materialsUsed.forEach { m ->
            val sub = materialRow(m)
            grandTotal += sub
        }
        // Total line
        ensureSpace(22f)
        val c2 = page!!.canvas
        c2.drawRect(
            RectF(MARGIN, y, PAGE_W - MARGIN, y + 1f),
            fillPaint(INK_500 and 0x33FFFFFF.toInt() or (0x33 shl 24)),
        )
        y += 4f
        c2.drawText("Total", MARGIN, y + 12f, textPaint(INK_500, 10f, bold = true))
        val totalText = formatMoney(grandTotal)
        val tp = textPaint(INK_900, 13f, bold = true)
        val w = tp.measureText(totalText)
        c2.drawText(totalText, PAGE_W - MARGIN - w, y + 12f, tp)
        y += 22f
    }

    private fun materialRow(m: MaterialUsedItem): Double {
        ensureSpace(20f)
        val c = page!!.canvas
        val unit = priceById[m.itemId] ?: 0.0
        val subtotal = unit * m.quantity
        c.drawRoundRect(
            RectF(MARGIN, y, PAGE_W - MARGIN, y + 18f),
            6f, 6f, fillPaint(SURFACE_MUTED),
        )
        c.drawText(
            m.itemName.ifBlank { m.itemId }.take(40),
            MARGIN + 10f, y + 13f,
            textPaint(INK_900, 10f),
        )
        c.drawText(m.quantity.toString(), MARGIN + 280f, y + 13f, textPaint(INK_700, 10f))
        if (unit > 0.0) {
            c.drawText(formatMoney(unit), MARGIN + 340f, y + 13f, textPaint(INK_700, 10f))
            val sp = textPaint(INK_900, 10f, bold = true)
            val st = formatMoney(subtotal)
            val w = sp.measureText(st)
            c.drawText(st, PAGE_W - MARGIN - 10f - w, y + 13f, sp)
        } else {
            c.drawText("—", MARGIN + 340f, y + 13f, textPaint(INK_500, 10f))
            val sp = textPaint(INK_500, 10f)
            val st = "—"
            val w = sp.measureText(st)
            c.drawText(st, PAGE_W - MARGIN - 10f - w, y + 13f, sp)
        }
        y += 22f
        return subtotal
    }

    /* ── Resolution + RCA ─────────────────────────────────────────── */

    fun resolutionBlock() {
        val sign = task.signoffDescription.trim()
        val rca  = task.rca.trim()
        if (sign.isEmpty() && rca.isEmpty()) return
        if (sign.isNotEmpty()) {
            sectionLabel("WORK PERFORMED")
            wrappedText(sign, fontSize = 11f, color = INK_900)
            y += 4f
        }
        if (rca.isNotEmpty()) {
            sectionLabel("ROOT CAUSE")
            wrappedText(rca, fontSize = 11f, color = INK_700)
            y += 4f
        }
    }

    /* ── Sign-off block ───────────────────────────────────────────── */

    fun signoffBlock() {
        ensureSpace(80f)
        val c = page!!.canvas
        val h = 70f
        c.drawRoundRect(
            RectF(MARGIN, y, PAGE_W - MARGIN, y + h),
            10f, 10f, strokePaint(INK_500 and 0x33FFFFFF.toInt() or (0x55 shl 24), 1f),
        )
        val colW = (PAGE_W - MARGIN * 2) / 2f
        // Technician
        c.drawText("TECHNICIAN", MARGIN + 14f, y + 16f,
            textPaint(INK_500, 8f, bold = true, letterSpacing = 0.15f))
        c.drawText(
            task.assigneeName.ifBlank { "—" }.take(28),
            MARGIN + 14f, y + 34f,
            textPaint(INK_900, 12f, bold = true),
        )
        c.drawRect(RectF(MARGIN + 14f, y + 50f, MARGIN + colW - 14f, y + 51f),
            fillPaint(INK_500 and 0x33FFFFFF.toInt() or (0x55 shl 24)))
        c.drawText("Signature", MARGIN + 14f, y + 62f, textPaint(INK_500, 8f))

        // Approver
        c.drawText("APPROVED BY", MARGIN + colW + 14f, y + 16f,
            textPaint(INK_500, 8f, bold = true, letterSpacing = 0.15f))
        c.drawText(
            task.ownerAdminName.ifBlank { "—" }.take(28),
            MARGIN + colW + 14f, y + 34f,
            textPaint(INK_900, 12f, bold = true),
        )
        c.drawRect(RectF(MARGIN + colW + 14f, y + 50f, PAGE_W - MARGIN - 14f, y + 51f),
            fillPaint(INK_500 and 0x33FFFFFF.toInt() or (0x55 shl 24)))
        c.drawText("Signature", MARGIN + colW + 14f, y + 62f, textPaint(INK_500, 8f))
        y += h + 8f
    }

    /* ── Footer ───────────────────────────────────────────────────── */

    fun footer() {
        val c = page!!.canvas
        val ts = SimpleDateFormat("d MMM yyyy, h:mm a", Locale.getDefault()).format(Date())
        c.drawText(
            "Generated $ts · UniWatt Elektrik · Page $pageNo",
            MARGIN, (PAGE_H - 20).toFloat(),
            textPaint(INK_500, 9f),
        )
    }

    fun finish() {
        page?.let { doc.finishPage(it) }
        page = null
    }

    /* ── Primitives ───────────────────────────────────────────────── */

    private fun sectionLabel(text: String) {
        ensureSpace(18f)
        val c = page!!.canvas
        c.drawText(text, MARGIN, y + 10f,
            textPaint(INK_500, 9f, bold = true, letterSpacing = 0.15f))
        y += 16f
    }

    /**
     * Word-wraps [text] to the page width, advancing [y] one line at a time.
     * Newlines in the source break paragraphs.
     */
    private fun wrappedText(text: String, fontSize: Float, color: Int) {
        val paint = textPaint(color, fontSize)
        val maxW  = PAGE_W - MARGIN * 2
        text.split('\n').forEach { paragraph ->
            val words = paragraph.split(' ')
            val line = StringBuilder()
            words.forEach { word ->
                val candidate = if (line.isEmpty()) word else "$line $word"
                if (paint.measureText(candidate) <= maxW) {
                    line.clear(); line.append(candidate)
                } else {
                    flushLine(line.toString(), paint, fontSize)
                    line.clear(); line.append(word)
                }
            }
            if (line.isNotEmpty()) flushLine(line.toString(), paint, fontSize)
        }
    }

    private fun flushLine(line: String, paint: Paint, fontSize: Float) {
        val lineH = fontSize * 1.35f
        ensureSpace(lineH)
        page!!.canvas.drawText(line, MARGIN, y + fontSize, paint)
        y += lineH
    }

    private fun fillPaint(color: Int) = Paint().apply {
        this.color = color
        isAntiAlias = true
        style = Paint.Style.FILL
    }

    private fun strokePaint(color: Int, width: Float) = Paint().apply {
        this.color = color
        isAntiAlias = true
        style = Paint.Style.STROKE
        strokeWidth = width
    }

    private fun textPaint(
        color: Int,
        sizeSp: Float,
        bold: Boolean = false,
        letterSpacing: Float = 0f,
    ) = Paint().apply {
        this.color = color
        textSize = sizeSp
        isAntiAlias = true
        typeface = if (bold) Typeface.create(Typeface.DEFAULT, Typeface.BOLD) else Typeface.DEFAULT
        this.letterSpacing = letterSpacing
    }

    private fun formatDate(ms: Long?): String {
        if (ms == null || ms <= 0L) return "—"
        return SimpleDateFormat("d MMM yyyy, h:mm a", Locale.getDefault()).format(Date(ms))
    }

    private fun formatDuration(durationMs: Long?): String {
        if (durationMs == null || durationMs <= 0L) return "—"
        val totalMinutes = durationMs / 60_000
        val h = totalMinutes / 60
        val m = totalMinutes % 60
        return when {
            h > 0 && m > 0 -> "${h}h ${m}m"
            h > 0          -> "${h}h"
            else           -> "${m}m"
        }
    }

    private fun formatMoney(v: Double): String {
        val sign = if (v < 0) "-" else ""
        val abs = kotlin.math.abs(v)
        val rupees = abs.toLong()
        val paise = ((abs - rupees) * 100).toLong()
        return "${sign}₹${rupees}.${paise.toString().padStart(2, '0')}"
    }
}
