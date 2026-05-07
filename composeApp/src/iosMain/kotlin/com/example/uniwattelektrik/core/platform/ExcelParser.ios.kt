package com.example.uniwattelektrik.core.platform

actual class ExcelParser {
    actual suspend fun parse(uri: String): List<ParsedSheet> = emptyList()
}

actual fun createExcelParser(): ExcelParser = ExcelParser()
