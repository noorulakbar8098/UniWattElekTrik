package com.example.uniwattelektrik.core.platform

/**
 * Sheet parsed from an .xlsx/.xls file. The first row is treated as headers,
 * remaining rows are data. All cells are normalized to strings (numbers/dates
 * are formatted using the workbook's display format).
 */
data class ParsedSheet(
    val headers: List<String>,
    val rows: List<List<String>>,
)

expect class ExcelParser() {
    /** Reads the first sheet of the workbook at [uri] and returns parsed cells. */
    suspend fun parse(uri: String): ParsedSheet
}

expect fun createExcelParser(): ExcelParser

