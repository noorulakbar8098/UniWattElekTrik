package com.example.uniwattelektrik.core.platform

import androidx.compose.runtime.Composable
import com.example.uniwattelektrik.feature.admin.presentation.reports.MonthlyReport

/**
 * Renders a [MonthlyReport] to a branded one-page (or multi-page) PDF and
 * shares it through the OS share sheet. Mirrors [FileShareLauncher] but
 * operates on the structured report — rendering happens platform-side so
 * each OS can use its native PDF primitives (Android `PdfDocument`,
 * iOS `UIGraphicsPDFRenderer`).
 *
 * Filename is supplied by the caller; mime type is always `application/pdf`.
 */
@Composable
expect fun rememberPdfReportExporter(): PdfReportExporter

class PdfReportExporter internal constructor(
    private val impl: (MonthlyReport, String) -> Unit,
) {
    operator fun invoke(report: MonthlyReport, filename: String) = impl(report, filename)
}
