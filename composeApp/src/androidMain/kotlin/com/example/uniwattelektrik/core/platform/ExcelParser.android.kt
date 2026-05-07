package com.example.uniwattelektrik.core.platform

import android.net.Uri
import com.example.uniwattelektrik.AndroidAppContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.dhatim.fastexcel.reader.ReadableWorkbook
import org.dhatim.fastexcel.reader.Row

/**
 * Reads **all sheets** of an .xlsx workbook using fastexcel-reader.
 * Note: only `.xlsx` is supported (no legacy `.xls`).
 *
 * Two defensive layers handle real-world malformed xlsx files:
 *
 *  1. Per-row fault tolerance — each sheet's row stream is iterated manually
 *     so a single bad row (e.g. a SharedString index that exceeds the SST
 *     size) does not abort the entire import. Broken rows are silently skipped.
 *
 *  2. Safe cell access — `Row.getCell(int)` is documented to throw
 *     `IndexOutOfBoundsException` for out-of-range indices. We use
 *     `Row.getOptionalCell(int)` which returns `Optional.empty()` instead.
 */
actual class ExcelParser {

    actual suspend fun parse(uri: String): List<ParsedSheet> = withContext(Dispatchers.IO) {
        val resolver = AndroidAppContext.application.contentResolver
        val stream   = resolver.openInputStream(Uri.parse(uri))
            ?: error("Could not open Excel file")

        stream.use { input ->
            ReadableWorkbook(input).use { wb ->
                val result = mutableListOf<ParsedSheet>()

                wb.sheets.forEach { sheet ->
                    val sheetName = sheet.name.trim()

                    // ── Row-level fault-tolerant read ──────────────────────
                    val allRows = ArrayList<Row>()
                    try {
                        sheet.openStream().use { rowStream ->
                            val iter = rowStream.iterator()
                            while (iter.hasNext()) {
                                try {
                                    allRows.add(iter.next())
                                } catch (_: Exception) {
                                    // skip malformed row, keep reading
                                }
                            }
                        }
                    } catch (streamEx: Exception) {
                        // Stream open/advance failed — use whatever rows we
                        // collected before the failure; discard empty sheets.
                        if (allRows.isEmpty()) return@forEach
                    }

                    if (allRows.isEmpty()) return@forEach

                    val headerRow = allRows[0]

                    // Skip sheets whose header row is entirely blank
                    // (summary/cover sheets that carry no item data).
                    val width = headerRow.cellCount
                    if (width == 0) return@forEach

                    val headers = (0 until width).map { cellText(headerRow, it) }
                    if (headers.all { it.isBlank() }) return@forEach

                    val data = allRows.drop(1).map { r ->
                        (0 until width).map { cellText(r, it) }
                    }.filter { row -> row.any { it.isNotBlank() } }

                    result += ParsedSheet(
                        headers   = headers,
                        rows      = data,
                        sheetName = sheetName,
                    )
                }

                if (result.isEmpty()) error("No data found in workbook")
                result
            }
        }
    }

    /**
     * Returns the cell content as a trimmed string regardless of its type.
     *
     * Uses [Row.getOptionalCell] (not [Row.getCell]) because `getCell` throws
     * [IndexOutOfBoundsException] for out-of-range indices rather than
     * returning null — the optional variant is always safe.
     */
    private fun cellText(row: Row, idx: Int): String {
        val cell = row.getOptionalCell(idx).orElse(null) ?: return ""
        return (cell.rawValue ?: "").trim()
    }
}

actual fun createExcelParser(): ExcelParser = ExcelParser()
