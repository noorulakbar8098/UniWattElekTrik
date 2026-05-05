package com.example.uniwattelektrik.core.platform

actual class ExcelParser {
    actual suspend fun parse(uri: String): ParsedSheet = ParsedSheet(emptyList(), emptyList())
}

actual fun createExcelParser(): ExcelParser = ExcelParser()

