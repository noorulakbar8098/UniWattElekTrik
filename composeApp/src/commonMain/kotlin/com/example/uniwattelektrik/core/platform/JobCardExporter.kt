package com.example.uniwattelektrik.core.platform

import androidx.compose.runtime.Composable
import com.example.uniwattelektrik.feature.workforce.data.remote.TaskRecord

/**
 * Renders a completed [TaskRecord] as a single-task PDF — the "job card" /
 * completion certificate that admins hand to customers. Mirrors
 * [PdfReportExporter]: the common expect carries data; each platform owns
 * its own native PDF primitives.
 *
 * [unitPriceByItemId] looks up the unit price for each material in the
 * task's materialsUsed list. Pass an empty map to render the table
 * without prices (qty only).
 */
@Composable
expect fun rememberJobCardExporter(): JobCardExporter

class JobCardExporter internal constructor(
    private val impl: (TaskRecord, Map<String, Double>, String) -> Unit,
) {
    operator fun invoke(
        task            : TaskRecord,
        unitPriceByItemId: Map<String, Double>,
        filename        : String,
    ) = impl(task, unitPriceByItemId, filename)
}
