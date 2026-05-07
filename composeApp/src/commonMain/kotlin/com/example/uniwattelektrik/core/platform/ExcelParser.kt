package com.example.uniwattelektrik.core.platform

/**
 * One sheet parsed from a multi-sheet .xlsx workbook.
 *
 * The first row of each sheet is treated as a header row; the remaining
 * rows are data. All cell values are normalised to strings.
 *
 * [sheetName] is the tab name from the workbook (e.g. "Wires", "MCB") and is
 * used as a category fallback when a sheet's data contains no integer-serial
 * category-marker rows.
 */
data class ParsedSheet(
    val headers  : List<String>,
    val rows     : List<List<String>>,
    val sheetName: String = "",
)

expect class ExcelParser() {
    /**
     * Reads **all** sheets of the workbook at [uri] and returns one
     * [ParsedSheet] per sheet.  Sheets whose first row is entirely blank are
     * skipped.  Each sheet is parsed independently so that different column
     * layouts across sheets are handled correctly.
     */
    suspend fun parse(uri: String): List<ParsedSheet>
}

expect fun createExcelParser(): ExcelParser
