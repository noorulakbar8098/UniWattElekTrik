package com.example.uniwattelektrik.core.platform

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import com.example.uniwattelektrik.feature.admin.presentation.reports.EmployeeReportRow
import com.example.uniwattelektrik.feature.admin.presentation.reports.MonthlyReport
import com.example.uniwattelektrik.feature.admin.presentation.reports.StockReportSummary
import com.example.uniwattelektrik.feature.admin.presentation.reports.TaskReportSummary
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
actual fun rememberPdfReportExporter(): PdfReportExporter {
    val context = LocalContext.current
    return remember(context) {
        PdfReportExporter { report, filename ->
            runCatching { renderAndShare(context, report, filename) }
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

private const val BRAND    = 0xFF1A6BF5.toInt()
private const val BRAND_50 = 0xFFE8F0FE.toInt()
private const val INK_900  = 0xFF0D1B3E.toInt()
private const val INK_700  = 0xFF253354.toInt()
private const val INK_500  = 0xFF5B6783.toInt()
private const val SURFACE_MUTED = 0xFFF3F5FA.toInt()
private const val SUCCESS  = 0xFF16A34A.toInt()
private const val WARNING  = 0xFFD97706.toInt()
private const val DANGER   = 0xFFDC2626.toInt()

/* ─── Renderer ───────────────────────────────────────────────────────── */

private fun renderAndShare(context: Context, report: MonthlyReport, filename: String) {
    val doc = PdfDocument()
    val draw = ReportDraw(doc)
    draw.startPage(report)
    draw.overviewStrip(report)
    draw.employeesSection(report.employees)
    draw.tasksSection(report.tasks)
    draw.stockSection(report.stock)
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

/**
 * Holds the current page + cursor so each section function can flow without
 * passing canvas/y around. Auto-paginates when content would clash with the
 * footer reserve.
 */
private class ReportDraw(private val doc: PdfDocument) {
    private var pageNo = 0
    private var page: PdfDocument.Page? = null
    private var y = 0f
    private var report: MonthlyReport? = null

    fun startPage(report: MonthlyReport) {
        this.report = report
        newPage(withHeader = true)
    }

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

    /* ── Header ───────────────────────────────────────────────────── */

    private fun drawHeader() {
        val c = page!!.canvas
        val bar = RectF(0f, 0f, PAGE_W.toFloat(), 96f)
        c.drawRect(bar, fillPaint(BRAND))
        // Eyebrow
        c.drawText(
            "UNIWATT ELEKTRIK",
            MARGIN, 32f,
            textPaint(Color.WHITE, 10f, bold = true, letterSpacing = 0.15f),
        )
        // Title
        c.drawText(
            "Monthly Report",
            MARGIN, 60f,
            textPaint(Color.WHITE, 22f, bold = true),
        )
        // Period (right-aligned)
        val period = "${report!!.month.month.name.lowercase()
            .replaceFirstChar { it.uppercase() }} ${report!!.month.year}"
        val periodPaint = textPaint(Color.WHITE, 14f, bold = true)
        val w = periodPaint.measureText(period)
        c.drawText(period, PAGE_W - MARGIN - w, 60f, periodPaint)
        y = 96f + 20f
    }

    /* ── Overview strip ───────────────────────────────────────────── */

    fun overviewStrip(report: MonthlyReport) {
        ensureSpace(80f)
        val c = page!!.canvas
        val gap = 12f
        val tileW = (PAGE_W - MARGIN * 2 - gap * 2) / 3f
        val tileH = 64f
        val items = listOf(
            Triple("Employees", report.employees.size.toString(), BRAND),
            Triple("Tasks done", report.tasks.completed.toString(), WARNING),
            Triple("Units used", report.stock.itemsConsumedQty.toString(), SUCCESS),
        )
        items.forEachIndexed { i, (label, value, tint) ->
            val x = MARGIN + i * (tileW + gap)
            val bg = withAlpha(tint, 0x18)
            c.drawRoundRect(RectF(x, y, x + tileW, y + tileH), 10f, 10f, fillPaint(bg))
            c.drawText(value, x + 12f, y + 32f, textPaint(tint, 22f, bold = true))
            c.drawText(label, x + 12f, y + 52f, textPaint(tint, 10f, bold = true))
        }
        y += tileH + 18f
    }

    /* ── Employees section ────────────────────────────────────────── */

    fun employeesSection(rows: List<EmployeeReportRow>) {
        sectionHeader("Employees", "${rows.size} active")
        if (rows.isEmpty()) {
            emptyHint("No active employees this period.")
            return
        }
        // Column header
        ensureSpace(20f)
        val c = page!!.canvas
        val labelPaint = textPaint(INK_500, 9f, bold = true, letterSpacing = 0.1f)
        c.drawText("EMPLOYEE", MARGIN, y, labelPaint)
        c.drawText("P · L · A", MARGIN + 230f, y, labelPaint)
        c.drawText("TASKS", MARGIN + 330f, y, labelPaint)
        c.drawText("QUALITY", PAGE_W - MARGIN - 50f, y, labelPaint)
        y += 12f

        rows.forEach { row -> employeeRow(row) }
        y += 8f
    }

    private fun employeeRow(row: EmployeeReportRow) {
        ensureSpace(38f)
        val c = page!!.canvas
        c.drawRoundRect(
            RectF(MARGIN, y, PAGE_W - MARGIN, y + 32f),
            8f, 8f, fillPaint(SURFACE_MUTED),
        )
        c.drawText(
            row.name.take(28),
            MARGIN + 12f, y + 14f,
            textPaint(INK_900, 11f, bold = true),
        )
        c.drawText(
            row.role.take(28),
            MARGIN + 12f, y + 26f,
            textPaint(INK_500, 9f),
        )
        c.drawText(
            "${row.daysPresent} · ${row.daysLate} · ${row.daysAbsent}",
            MARGIN + 230f, y + 20f,
            textPaint(INK_700, 11f, bold = true),
        )
        c.drawText(
            "${row.tasksCompleted}/${row.tasksAssigned}",
            MARGIN + 330f, y + 20f,
            textPaint(INK_700, 11f, bold = true),
        )
        val score = row.qualityScore ?: row.punctualityPct
        val tint = when {
            score >= 85 -> SUCCESS
            score >= 65 -> WARNING
            else        -> DANGER
        }
        val scoreLabel = if (row.qualityScore != null) "Q $score" else "$score%"
        val scorePaint = textPaint(tint, 11f, bold = true)
        val w = scorePaint.measureText(scoreLabel)
        c.drawText(scoreLabel, PAGE_W - MARGIN - 12f - w, y + 20f, scorePaint)
        y += 36f
    }

    /* ── Tasks section ────────────────────────────────────────────── */

    fun tasksSection(t: TaskReportSummary) {
        sectionHeader("Tasks", "${t.completed} of ${t.created} completed")
        metricRow(listOf(
            "Created" to t.created.toString(),
            "Done"    to t.completed.toString(),
            "Pending" to t.pending.toString(),
            "Overdue" to t.overdue.toString(),
        ))
        metricRow(listOf(
            "SLA met"        to (t.slaCompliancePct?.let { "$it%" } ?: "—"),
            "Avg resolution" to (t.avgResolutionMinutes?.let { formatDuration(it) } ?: "—"),
            "Downtime"       to "${t.totalDowntimeHours}h",
        ))
        metricRow(listOf(
            "Reopens"     to t.reopensInMonth.toString(),
            "Reopen rate" to (t.reopenRatePct?.let { "$it%" } ?: "—"),
        ))
        if (t.topRcaCauses.isNotEmpty()) {
            subheader("Top RCA causes")
            t.topRcaCauses.forEach { (cause, count) ->
                bulletRow(cause, count.toString())
            }
        }
        y += 6f
    }

    /* ── Stock section ────────────────────────────────────────────── */

    fun stockSection(s: StockReportSummary) {
        sectionHeader("Stock", "${s.itemsConsumedQty} units consumed")
        metricRow(listOf(
            "In stock"     to s.closingStockQty.toString(),
            "Used units"   to s.itemsConsumedQty.toString(),
            "Low stock"    to s.lowStockCount.toString(),
            "Stocked-out"  to s.zeroStockCount.toString(),
        ))
        // Opening → Closing delta if a snapshot exists
        if (s.openingStockQty != null && s.qtyDelta != null) {
            openingClosingPanel(s)
        }
        if (s.consumedFromTasksQty > 0 || s.consumedManualQty > 0) {
            metricRow(listOf(
                "From tasks" to s.consumedFromTasksQty.toString(),
                "Manual"     to s.consumedManualQty.toString(),
            ))
        }
        if (s.topConsumed.isNotEmpty()) {
            subheader("Top consumed items")
            s.topConsumed.forEach { row ->
                bulletRow(row.itemName, "${row.qty} u")
            }
        }
        if (s.consumptionByEquipment.isNotEmpty()) {
            subheader("Consumption by equipment")
            s.consumptionByEquipment.forEach { (equipment, qty) ->
                bulletRow(equipment, "$qty u")
            }
        }
    }

    private fun openingClosingPanel(s: StockReportSummary) {
        val delta = s.qtyDelta ?: return
        val opening = s.openingStockQty ?: return
        val tint = when {
            delta > 0 -> SUCCESS
            delta < 0 -> DANGER
            else      -> INK_700
        }
        val bg = withAlpha(tint, 0x18)
        ensureSpace(58f)
        val c = page!!.canvas
        c.drawRoundRect(
            RectF(MARGIN, y, PAGE_W - MARGIN, y + 48f),
            10f, 10f, fillPaint(bg),
        )
        c.drawText(
            "OPENING → CLOSING",
            MARGIN + 12f, y + 16f,
            textPaint(tint, 9f, bold = true, letterSpacing = 0.15f),
        )
        c.drawText(
            "$opening → ${s.closingStockQty} units",
            MARGIN + 12f, y + 36f,
            textPaint(INK_900, 13f, bold = true),
        )
        val sign = if (delta > 0) "+" else ""
        val deltaLabel = "$sign$delta"
        val deltaPaint = textPaint(tint, 16f, bold = true)
        val w = deltaPaint.measureText(deltaLabel)
        c.drawText(deltaLabel, PAGE_W - MARGIN - 12f - w, y + 30f, deltaPaint)
        y += 56f
    }

    /* ── Section primitives ───────────────────────────────────────── */

    private fun sectionHeader(title: String, subtitle: String) {
        ensureSpace(36f)
        val c = page!!.canvas
        c.drawText(title, MARGIN, y + 12f, textPaint(INK_900, 16f, bold = true))
        val tPaint = textPaint(INK_500, 10f)
        val tw = tPaint.measureText(subtitle)
        c.drawText(subtitle, PAGE_W - MARGIN - tw, y + 12f, tPaint)
        y += 18f
        // separator
        c.drawRect(
            RectF(MARGIN, y, PAGE_W - MARGIN, y + 1f),
            fillPaint(BRAND_50),
        )
        y += 12f
    }

    private fun subheader(text: String) {
        ensureSpace(20f)
        val c = page!!.canvas
        c.drawText(text, MARGIN, y + 10f, textPaint(INK_700, 11f, bold = true))
        y += 18f
    }

    private fun metricRow(items: List<Pair<String, String>>) {
        if (items.isEmpty()) return
        ensureSpace(52f)
        val c = page!!.canvas
        val gap = 8f
        val tileW = (PAGE_W - MARGIN * 2 - gap * (items.size - 1)) / items.size
        val tileH = 44f
        items.forEachIndexed { i, (label, value) ->
            val x = MARGIN + i * (tileW + gap)
            c.drawRoundRect(
                RectF(x, y, x + tileW, y + tileH),
                8f, 8f, fillPaint(SURFACE_MUTED),
            )
            c.drawText(value, x + 10f, y + 22f, textPaint(INK_900, 14f, bold = true))
            c.drawText(label, x + 10f, y + 38f, textPaint(INK_500, 9f))
        }
        y += tileH + 8f
    }

    private fun bulletRow(label: String, value: String) {
        ensureSpace(22f)
        val c = page!!.canvas
        c.drawRoundRect(
            RectF(MARGIN, y, PAGE_W - MARGIN, y + 18f),
            6f, 6f, fillPaint(SURFACE_MUTED),
        )
        c.drawText(label.take(54), MARGIN + 10f, y + 13f, textPaint(INK_700, 10f))
        val vPaint = textPaint(INK_900, 10f, bold = true)
        val vw = vPaint.measureText(value)
        c.drawText(value, PAGE_W - MARGIN - 10f - vw, y + 13f, vPaint)
        y += 22f
    }

    private fun emptyHint(text: String) {
        ensureSpace(36f)
        val c = page!!.canvas
        c.drawRoundRect(
            RectF(MARGIN, y, PAGE_W - MARGIN, y + 28f),
            8f, 8f, fillPaint(SURFACE_MUTED),
        )
        c.drawText(text, MARGIN + 12f, y + 18f, textPaint(INK_500, 10f))
        y += 36f
    }

    /* ── Footer ───────────────────────────────────────────────────── */

    fun footer() {
        val c = page!!.canvas
        val ts = SimpleDateFormat("d MMM yyyy, h:mm a", Locale.getDefault()).format(Date())
        c.drawText(
            "Generated $ts · Page $pageNo",
            MARGIN, (PAGE_H - 20).toFloat(),
            textPaint(INK_500, 9f),
        )
    }

    fun finish() {
        page?.let { doc.finishPage(it) }
        page = null
    }

    /* ── Paint helpers ────────────────────────────────────────────── */

    private fun fillPaint(color: Int) = Paint().apply {
        this.color = color
        isAntiAlias = true
        style = Paint.Style.FILL
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

    private fun withAlpha(color: Int, alpha: Int): Int =
        (color and 0x00FFFFFF) or (alpha shl 24)

    private fun formatDuration(minutes: Long): String {
        if (minutes < 60) return "${minutes}m"
        val h = minutes / 60
        val m = minutes % 60
        return if (m == 0L) "${h}h" else "${h}h ${m}m"
    }
}
