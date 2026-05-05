package com.example.uniwattelektrik.core.platform

import android.net.Uri
import com.example.uniwattelektrik.AndroidAppContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.dhatim.fastexcel.reader.ReadableWorkbook
import org.dhatim.fastexcel.reader.Row

/**
 * Reads the first sheet of an .xlsx workbook using fastexcel-reader.
 * Note: only `.xlsx` is supported (no legacy `.xls`).
 */
actual class ExcelParser {
    actual suspend fun parse(uri: String): ParsedSheet = withContext(Dispatchers.IO) {
        val resolver = AndroidAppContext.application.contentResolver
        val stream = resolver.openInputStream(Uri.parse(uri))
            ?: error("Could not open Excel file")
        stream.use { input ->
            ReadableWorkbook(input).use { wb ->
                val sheet = wb.firstSheet
                val allRows: List<Row> = sheet.read()
                if (allRows.isEmpty()) return@withContext ParsedSheet(emptyList(), emptyList())
                val headerRow = allRows[0]
                val width = headerRow.cellCount
                val headers = (0 until width).map { cellText(headerRow, it) }
                val data = allRows.drop(1).map { r ->
                    (0 until width).map { cellText(r, it) }
                }.filter { row -> row.any { it.isNotBlank() } }
                ParsedSheet(headers, data)
            }
        }
    }

    /**
     * Returns the cell content as a string regardless of its type.
     * `Row.getCellAsString` throws on NUMBER/BOOLEAN cells, so we read the
     * raw value (the string fastexcel itself stores) and trim it.
     */
    private fun cellText(row: Row, idx: Int): String {
        val cell = row.getCell(idx) ?: return ""
        // rawValue is non-null for any populated cell; for empty cells it's "".
        return (cell.rawValue ?: "").trim()
    }
}

actual fun createExcelParser(): ExcelParser = ExcelParser()
