package com.example.uniwattelektrik.feature.admin.presentation.reports

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.flow.flowOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.uniwattelektrik.core.components.DsAvatarBubble
import com.example.uniwattelektrik.core.components.DsStatusChip
import com.example.uniwattelektrik.core.components.ToastController
import com.example.uniwattelektrik.core.performance.TrackScreenPerformance
import com.example.uniwattelektrik.core.platform.rememberFileShareLauncher
import com.example.uniwattelektrik.core.platform.rememberPdfReportExporter
import com.example.uniwattelektrik.core.theme.AppShapes
import com.example.uniwattelektrik.core.theme.AppTheme
import com.example.uniwattelektrik.core.theme.AppTypography
import com.example.uniwattelektrik.core.theme.SetStatusBar
import com.example.uniwattelektrik.core.theme.appScreenBackground
import com.example.uniwattelektrik.feature.admin.presentation.InventoryViewModel
import com.example.uniwattelektrik.feature.workforce.presentation.WorkforceViewModel
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlinx.datetime.number
import kotlinx.datetime.plus

/* ═══════════════════════════════════════════════════════════════════════════
 *  ReportsScreen — Phase 1
 * ═══════════════════════════════════════════════════════════════════════════
 *
 *  Aggregates the existing workforce / inventory flows into a per-month
 *  report and renders three sections (Employees, Tasks, Stock). Tap row →
 *  drill into the existing detail screens. Tap Export → share a CSV bundle.
 */

/** Sparkline window — number of months including the current one. */
private const val TREND_WINDOW = 6

/**
 * The three section views on the Reports screen. Each tab carries its own
 * accent palette and icon so the active pill mirrors the section it represents.
 */
private enum class ReportTab { Employees, Tasks, Stock }
@Composable
fun ReportsScreen(
    workforceVm  : WorkforceViewModel,
    inventoryVm  : InventoryViewModel,
    adminId      : String,
    onBack       : () -> Unit,
    onEmployeeOpen : (userId: String) -> Unit = {},
    onTaskBoardOpen: () -> Unit = {},
    onSpareListOpen: () -> Unit = {},
    modifier     : Modifier = Modifier,
) {
    TrackScreenPerformance("ReportsScreen")
    SetStatusBar(color = Color.White, darkIcons = true)

    val employees  by workforceVm.employees.collectAsStateWithLifecycle()
    val tasks      by workforceVm.tasks.collectAsStateWithLifecycle()
    val attendance by workforceVm.attendance.collectAsStateWithLifecycle()
    val leaves     by workforceVm.leaveRequests.collectAsStateWithLifecycle()
    val spares     by inventoryVm.spareItems.collectAsStateWithLifecycle()
    val invTxns    by workforceVm.inventoryTxns.collectAsStateWithLifecycle()

    var month by remember { mutableStateOf(ReportMonth.current()) }
    var selectedTab by remember { mutableStateOf(ReportTab.Employees) }

    // Phase 2 / Case 3 — opening stock comes from the snapshot of the month
    // that was CLOSED on the report month's 1st. So for "April 2026" we read
    // the March 2026 snapshot doc (which captured April's opening = March's
    // closing balance). The snapshot is null until the Cloud Function has
    // run at least once for this admin — UI hides the delta in that case.
    val openingKey = remember(month) {
        val first = LocalDate(month.year, month.month, 1).minus(1, DateTimeUnit.MONTH)
        first.year to first.month.number
    }
    val openingSnapshot by remember(adminId, openingKey) {
        if (adminId.isBlank()) flowOf(null)
        else workforceVm.monthlyStockSnapshotFlow(adminId, openingKey.first, openingKey.second)
    }.collectAsState(initial = null)

    // Phase 3 / Case 2 — trailing 6-month stock snapshot history for the
    // value sparkline. The current month uses live closing value (no
    // snapshot exists yet), so we only need the previous 5 docs.
    val trendMonths = remember(month) {
        ReportsTrends.trailingMonths(month, monthsBack = TREND_WINDOW)
    }
    val historyKeys = remember(trendMonths) {
        // Exclude the current month — its snapshot isn't written yet.
        trendMonths.dropLast(1).map { it.year to it.month.number }
    }
    val snapshotHistory by remember(adminId, historyKeys) {
        if (adminId.isBlank()) flowOf(emptyList())
        else workforceVm.monthlyStockHistoryFlow(adminId, historyKeys)
    }.collectAsState(initial = emptyList())

    val report = remember(month, employees, tasks, attendance, leaves, spares, invTxns, openingSnapshot) {
        ReportsAggregator.build(
            month           = month,
            employees       = employees,
            tasks           = tasks,
            attendance      = attendance,
            leaves          = leaves,
            spares          = spares,
            inventoryTxns   = invTxns,
            openingSnapshot = openingSnapshot,
        )
    }

    // Phase 3 / Case 3 — previous-month report drives the anomaly rules.
    // Re-runs the pure aggregator for last month off the same live streams.
    // Stock fields in the previous report use today's spares (no time-travel),
    // but the anomaly rules only read task-side metrics + manual issuance
    // which ARE accurately bucketed by `createdAt` / `completedAt` per month.
    val previousMonth = remember(month) {
        val first = LocalDate(month.year, month.month, 1).minus(1, DateTimeUnit.MONTH)
        ReportMonth(first.year, first.month)
    }
    val previousReport = remember(previousMonth, employees, tasks, attendance, leaves, spares, invTxns) {
        ReportsAggregator.build(
            month         = previousMonth,
            employees     = employees,
            tasks         = tasks,
            attendance    = attendance,
            leaves        = leaves,
            spares        = spares,
            inventoryTxns = invTxns,
            // No opening snapshot for the previous month — anomaly rules
            // that compare value deltas only read current.opening anyway.
            openingSnapshot = null,
        )
    }
    val anomalies = remember(report, previousReport) {
        ReportsAnomalies.detect(current = report, previous = previousReport)
    }

    val tasksTrend = remember(tasks, month) {
        ReportsTrends.tasksCompletedTrend(tasks, month, TREND_WINDOW)
    }
    val reopenTrend = remember(tasks, month) {
        ReportsTrends.reopenRateTrend(tasks, month, TREND_WINDOW)
    }
    val stockTrend = remember(snapshotHistory, month, report.stock.closingStockValue) {
        ReportsTrends.stockValueTrend(
            snapshots    = snapshotHistory,
            currentMonth = month,
            currentValue = report.stock.closingStockValue,
            monthsBack   = TREND_WINDOW,
        )
    }

    val share    = rememberFileShareLauncher()
    val sharePdf = rememberPdfReportExporter()

    Column(modifier = modifier.fillMaxSize().background(appScreenBackground())) {
        // ── Top bar ─────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(AppTheme.Surface)
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            BackChip(onClick = onBack)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Monthly report",
                    style = AppTypography.labelSmall.copy(
                        color         = AppTheme.Ink500,
                        letterSpacing = 1.4.sp,
                    ),
                )
                Text(
                    "Reports",
                    style = AppTypography.titleLarge.copy(
                        color      = AppTheme.Ink900,
                        fontWeight = FontWeight.ExtraBold,
                    ),
                )
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment     = Alignment.CenterVertically,
            ) {
                ExportButton(
                    label = "PDF",
                    onClick = {
                        val filename = "uniwatt-report-${month.year}-${
                            month.month.number.toString().padStart(2, '0')
                        }.pdf"
                        sharePdf(report, filename)
                        ToastController.info(
                            title = "Report exported",
                            body  = "PDF for ${month.label()} ready to share",
                        )
                    },
                )
                ExportButton(
                    label    = "CSV",
                    primary  = false,
                    onClick = {
                        val filename = "uniwatt-report-${month.year}-${
                            month.month.number.toString().padStart(2, '0')
                        }.csv"
                        share(ReportsCsv.combined(report), filename, "text/csv")
                        ToastController.info(
                            title = "Report exported",
                            body  = "CSV bundle for ${month.label()} ready to share",
                        )
                    },
                )
            }
        }

        // ── Body ────────────────────────────────────────────────────────
        LazyColumn(
            modifier            = Modifier.fillMaxSize(),
            contentPadding      = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item { MonthSelector(month = month, onChange = { month = it }) }
            if (anomalies.isNotEmpty()) {
                item { AnomalyStrip(anomalies = anomalies) }
            }
            item { OverviewStrip(report = report) }

            item {
                ReportsTabBar(
                    selected = selectedTab,
                    counts   = mapOf(
                        ReportTab.Employees to report.employees.size,
                        ReportTab.Tasks     to report.tasks.completed,
                        ReportTab.Stock     to report.stock.itemsConsumedQty,
                    ),
                    onSelect = { selectedTab = it },
                )
            }

            item {
                TabSectionHeader(
                    tab      = selectedTab,
                    subtitle = when (selectedTab) {
                        ReportTab.Employees -> "${report.employees.size} active"
                        ReportTab.Tasks     -> "${report.tasks.completed} of ${report.tasks.created} completed"
                        ReportTab.Stock     -> "${report.stock.itemsConsumedQty} units consumed"
                    },
                    onShareCsv = {
                        val mm = month.month.number.toString().padStart(2, '0')
                        when (selectedTab) {
                            ReportTab.Employees -> share(
                                ReportsCsv.employees(report),
                                "uniwatt-employees-${month.year}-$mm.csv",
                                "text/csv",
                            )
                            ReportTab.Tasks -> share(
                                ReportsCsv.tasks(report),
                                "uniwatt-tasks-${month.year}-$mm.csv",
                                "text/csv",
                            )
                            ReportTab.Stock -> share(
                                ReportsCsv.stock(report),
                                "uniwatt-stock-${month.year}-$mm.csv",
                                "text/csv",
                            )
                        }
                    },
                )
            }

            when (selectedTab) {
                ReportTab.Employees -> {
                    if (report.employees.isEmpty()) {
                        item { EmptyHint("No active employees to report on.") }
                    } else {
                        items(report.employees, key = { it.userId }) { row ->
                            EmployeeRow(row, onClick = { onEmployeeOpen(row.userId) })
                        }
                    }
                }
                ReportTab.Tasks -> item {
                    TasksSectionBody(
                        t              = report.tasks,
                        completedTrend = tasksTrend,
                        reopenTrend    = reopenTrend,
                        onOpenBoard    = onTaskBoardOpen,
                    )
                }
                ReportTab.Stock -> item {
                    StockSectionBody(
                        s            = report.stock,
                        valueTrend   = stockTrend,
                        onOpenSpares = onSpareListOpen,
                    )
                }
            }
        }
    }
}

/* ─── Top-bar pieces ──────────────────────────────────────────────────── */

@Composable
private fun BackChip(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(42.dp)
            .shadow(2.dp, RoundedCornerShape(12.dp), spotColor = Color(0x14000000))
            .clip(RoundedCornerShape(12.dp))
            .background(AppTheme.Surface)
            .border(1.dp, AppTheme.Ink100, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back",
             tint = AppTheme.Ink900, modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun ExportButton(
    label  : String = "Export",
    primary: Boolean = true,
    onClick: () -> Unit,
) {
    val bg   = if (primary) AppTheme.Brand else AppTheme.SurfaceMuted
    val tint = if (primary) Color.White    else AppTheme.Ink700
    Row(
        modifier = Modifier
            .clip(AppShapes.pill)
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 9.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(Icons.Filled.IosShare, contentDescription = null,
             tint = tint, modifier = Modifier.size(14.dp))
        Text(label, color = tint, fontSize = 13.sp, fontWeight = FontWeight.Bold)
    }
}

/* ─── Month selector ──────────────────────────────────────────────────── */

@Composable
private fun MonthSelector(month: ReportMonth, onChange: (ReportMonth) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, AppShapes.card, spotColor = Color(0x10000000))
            .clip(AppShapes.card)
            .background(AppTheme.Surface)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        MonthArrow(Icons.Filled.ChevronLeft) {
            val prev = LocalDate(month.year, month.month, 1).minus(1, DateTimeUnit.MONTH)
            onChange(ReportMonth(prev.year, prev.month))
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "Reporting period",
                style = AppTypography.labelSmall.copy(color = AppTheme.Ink500),
            )
            Text(
                "${month.month.fullName()} ${month.year}",
                style = AppTypography.titleLarge.copy(
                    color = AppTheme.Ink900,
                    fontWeight = FontWeight.Bold,
                ),
            )
        }
        MonthArrow(Icons.Filled.ChevronRight) {
            val next = LocalDate(month.year, month.month, 1).plus(1, DateTimeUnit.MONTH)
            onChange(ReportMonth(next.year, next.month))
        }
    }
}

@Composable
private fun MonthArrow(icon: ImageVector, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(AppTheme.SurfaceMuted)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = AppTheme.Ink900, modifier = Modifier.size(18.dp))
    }
}

/* ─── Anomaly strip ───────────────────────────────────────────────────── */

@Composable
private fun AnomalyStrip(anomalies: List<Anomaly>) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Worth a look",
                style = AppTypography.labelLarge.copy(
                    color = AppTheme.Ink700, fontWeight = FontWeight.Bold,
                ),
                modifier = Modifier.weight(1f),
            )
            Text(
                "${anomalies.size} signal${if (anomalies.size == 1) "" else "s"}",
                style = AppTypography.labelSmall.copy(color = AppTheme.Ink500),
            )
        }
        anomalies.forEach { AnomalyCard(it) }
    }
}

@Composable
private fun AnomalyCard(a: Anomaly) {
    val tint = when (a.severity) {
        AnomalySeverity.DANGER  -> AppTheme.Danger
        AnomalySeverity.WARNING -> AppTheme.Warning
        AnomalySeverity.INFO    -> AppTheme.Brand
    }
    val bg = when (a.severity) {
        AnomalySeverity.DANGER  -> AppTheme.DangerBg
        AnomalySeverity.WARNING -> AppTheme.WarningBg
        AnomalySeverity.INFO    -> AppTheme.Brand50
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(2.dp, RoundedCornerShape(14.dp), spotColor = Color(0x0F000000))
            .clip(RoundedCornerShape(14.dp))
            .background(AppTheme.Surface)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(bg),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                a.icon,
                style = AppTypography.titleSmall.copy(
                    color = tint, fontWeight = FontWeight.ExtraBold,
                ),
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                a.title,
                style = AppTypography.titleSmall.copy(
                    color = AppTheme.Ink900, fontWeight = FontWeight.Bold,
                ),
                maxLines = 1,
            )
            Text(
                a.body,
                style = AppTypography.bodySmall.copy(color = AppTheme.Ink500),
            )
        }
    }
}

/* ─── Overview strip ──────────────────────────────────────────────────── */

@Composable
private fun OverviewStrip(report: MonthlyReport) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        OverviewTile(
            label = "Employees",
            value = report.employees.size.toString(),
            tint  = AppTheme.Brand,
            bg    = AppTheme.Brand50,
            modifier = Modifier.weight(1f),
        )
        OverviewTile(
            label = "Tasks done",
            value = report.tasks.completed.toString(),
            tint  = AppTheme.Warning,
            bg    = AppTheme.WarningBg,
            modifier = Modifier.weight(1f),
        )
        OverviewTile(
            label = "Units used",
            value = report.stock.itemsConsumedQty.toString(),
            tint  = AppTheme.Success,
            bg    = AppTheme.SuccessBg,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun OverviewTile(
    label   : String,
    value   : String,
    tint    : Color,
    bg      : Color,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(bg)
            .padding(horizontal = 12.dp, vertical = 12.dp),
    ) {
        Text(
            value,
            style = AppTypography.displayMedium.copy(
                color = tint,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 22.sp,
            ),
        )
        Text(
            label,
            style = AppTypography.labelSmall.copy(
                color = tint.copy(alpha = 0.85f),
                fontWeight = FontWeight.SemiBold,
            ),
        )
    }
}

/* ─── Tab bar + per-tab header ────────────────────────────────────────── */

@Composable
private fun ReportsTabBar(
    selected: ReportTab,
    counts  : Map<ReportTab, Int>,
    onSelect: (ReportTab) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, AppShapes.card, spotColor = Color(0x10000000))
            .clip(AppShapes.card)
            .background(AppTheme.Surface)
            .padding(6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment     = Alignment.CenterVertically,
    ) {
        ReportTab.values().forEach { tab ->
            ReportsTabPill(
                tab      = tab,
                count    = counts[tab] ?: 0,
                active   = tab == selected,
                onClick  = { onSelect(tab) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun ReportsTabPill(
    tab     : ReportTab,
    count   : Int,
    active  : Boolean,
    onClick : () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tint = tabTint(tab)
    val bg   = if (active) tabBg(tab) else Color.Transparent
    val labelColor = if (active) tint else AppTheme.Ink500
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 10.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector       = tabIcon(tab),
            contentDescription = null,
            tint              = labelColor,
            modifier          = Modifier.size(16.dp),
        )
        Spacer(Modifier.width(6.dp))
        Text(
            tab.name,
            style = AppTypography.labelLarge.copy(
                color      = labelColor,
                fontWeight = if (active) FontWeight.Bold else FontWeight.SemiBold,
            ),
            maxLines = 1,
        )
        if (active && count > 0) {
            Spacer(Modifier.width(6.dp))
            Box(
                modifier = Modifier
                    .clip(AppShapes.pill)
                    .background(tint)
                    .padding(horizontal = 6.dp, vertical = 1.dp),
            ) {
                Text(
                    count.toString(),
                    style = AppTypography.labelSmall.copy(
                        color = Color.White, fontWeight = FontWeight.ExtraBold,
                    ),
                )
            }
        }
    }
}

@Composable
private fun TabSectionHeader(
    tab       : ReportTab,
    subtitle  : String,
    onShareCsv: () -> Unit,
) {
    val tint = tabTint(tab)
    val bg   = tabBg(tab)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(bg),
            contentAlignment = Alignment.Center,
        ) {
            Icon(tabIcon(tab), contentDescription = null, tint = tint,
                 modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                tab.name,
                style = AppTypography.titleLarge.copy(
                    color = AppTheme.Ink900, fontWeight = FontWeight.ExtraBold,
                ),
            )
            Text(subtitle, style = AppTypography.bodySmall.copy(color = AppTheme.Ink500))
        }
        Row(
            modifier = Modifier
                .clip(AppShapes.pill)
                .background(AppTheme.SurfaceMuted)
                .clickable(onClick = onShareCsv)
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(Icons.Filled.IosShare, contentDescription = null,
                 tint = AppTheme.Ink700, modifier = Modifier.size(12.dp))
            Text("CSV", color = AppTheme.Ink700, fontSize = 11.sp,
                 fontWeight = FontWeight.Bold)
        }
    }
}

private fun tabTint(tab: ReportTab) = when (tab) {
    ReportTab.Employees -> AppTheme.Brand
    ReportTab.Tasks     -> AppTheme.Warning
    ReportTab.Stock     -> AppTheme.Success
}

private fun tabBg(tab: ReportTab) = when (tab) {
    ReportTab.Employees -> AppTheme.Brand50
    ReportTab.Tasks     -> AppTheme.WarningBg
    ReportTab.Stock     -> AppTheme.SuccessBg
}

private fun tabIcon(tab: ReportTab): ImageVector = when (tab) {
    ReportTab.Employees -> Icons.Filled.People
    ReportTab.Tasks     -> Icons.AutoMirrored.Filled.Assignment
    ReportTab.Stock     -> Icons.Filled.Inventory2
}

@Composable
private fun EmptyHint(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(AppTheme.SurfaceMuted)
            .padding(18.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = AppTypography.bodyMedium.copy(color = AppTheme.Ink500))
    }
}

/* ─── Employee row ────────────────────────────────────────────────────── */

@Composable
private fun EmployeeRow(row: EmployeeReportRow, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(AppTheme.SurfaceMuted)
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        DsAvatarBubble(initials = row.initials, size = 40.dp)
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                row.name,
                style = AppTypography.titleSmall.copy(
                    color = AppTheme.Ink900, fontWeight = FontWeight.Bold,
                ),
                maxLines = 1,
            )
            // First line: attendance + completion ratio.
            Text(
                "${row.daysPresent}P · ${row.daysLate}L · ${row.daysAbsent}A · " +
                        "${row.tasksCompleted}/${row.tasksAssigned} tasks",
                style = AppTypography.bodySmall.copy(color = AppTheme.Ink500),
            )
            // Second line: only shown when there's signal worth flagging
            // (reopens > 0 or punctuality < 90).
            if (row.reopensInMonth > 0) {
                Text(
                    "${row.reopensInMonth} reopen${if (row.reopensInMonth == 1) "" else "s"} · " +
                            "punctuality ${row.punctualityPct}%",
                    style = AppTypography.captionLarge.copy(color = AppTheme.Ink300),
                )
            }
        }

        // Right-side pill: prefer the Quality score (rolls in on-time +
        // punctuality + 1-reopen-rate). Falls back to raw punctuality % for
        // employees with no completed tasks this month.
        val score   = row.qualityScore
        val displayPct = score ?: row.punctualityPct
        val tint = when {
            displayPct >= 85 -> AppTheme.Success
            displayPct >= 65 -> AppTheme.Warning
            else             -> AppTheme.Danger
        }
        val bg = when {
            displayPct >= 85 -> AppTheme.SuccessBg
            displayPct >= 65 -> AppTheme.WarningBg
            else             -> AppTheme.DangerBg
        }
        DsStatusChip(
            label           = if (score != null) "Q ${score}" else "${row.punctualityPct}%",
            tint            = tint,
            background      = bg,
            leadingDotColor = tint,
        )
    }
}

/* ─── Tasks section body ──────────────────────────────────────────────── */

@Composable
private fun TasksSectionBody(
    t              : TaskReportSummary,
    completedTrend : List<TrendPoint>,
    reopenTrend    : List<TrendPoint>,
    onOpenBoard    : () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // 6-month sparkline: tasks completed (oldest → newest, trails the
        // current report month). Hidden if every point is zero (e.g. brand-new
        // workspace) — an empty chart adds noise without information.
        if (completedTrend.any { it.value > 0f }) {
            SparklineCard(
                label  = "Tasks completed (6mo)",
                points = completedTrend,
                tint   = AppTheme.Warning,
                bg     = AppTheme.WarningBg,
                valueFormatter = { it.toInt().toString() },
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MetricChip("Created",  t.created.toString(),  modifier = Modifier.weight(1f))
            MetricChip("Done",     t.completed.toString(), modifier = Modifier.weight(1f))
            MetricChip("Pending",  t.pending.toString(),  modifier = Modifier.weight(1f))
            MetricChip("Overdue",  t.overdue.toString(),  modifier = Modifier.weight(1f))
        }

        // Priority bar chart
        if (t.byPriority.values.any { it > 0 }) {
            PriorityBarChart(byPriority = t.byPriority)
        }

        // SLA + avg resolution row
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MetricChip(
                label    = "SLA met",
                value    = t.slaCompliancePct?.let { "$it%" } ?: "—",
                modifier = Modifier.weight(1f),
            )
            MetricChip(
                label    = "Avg resolution",
                value    = t.avgResolutionMinutes?.let { formatDuration(it) } ?: "—",
                modifier = Modifier.weight(1f),
            )
            MetricChip(
                label    = "Downtime",
                value    = "${t.totalDowntimeHours}h",
                modifier = Modifier.weight(1f),
            )
        }

        // Reopen-quality row — added in Phase 2 / Case 1. Drives the
        // "are we shipping rework?" question that pure SLA can't answer.
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MetricChip(
                label    = "Reopens",
                value    = t.reopensInMonth.toString(),
                modifier = Modifier.weight(1f),
            )
            MetricChip(
                label    = "Reopen rate",
                value    = t.reopenRatePct?.let { "$it%" } ?: "—",
                modifier = Modifier.weight(1f),
            )
        }
        // Reopen rate trend (6mo) — only when at least one month had real
        // rework signal, otherwise the chart is a flat zero line.
        if (reopenTrend.any { it.value > 0f }) {
            SparklineCard(
                label  = "Reopen rate (6mo)",
                points = reopenTrend,
                tint   = AppTheme.Danger,
                bg     = AppTheme.DangerBg,
                valueFormatter = { "${it.toInt()}%" },
            )
        }

        if (t.topRcaCauses.isNotEmpty()) {
            Spacer(Modifier.height(2.dp))
            Text(
                "Top RCA causes",
                style = AppTypography.labelLarge.copy(
                    color = AppTheme.Ink700, fontWeight = FontWeight.SemiBold,
                ),
            )
            t.topRcaCauses.forEach { (cause, count) ->
                RcaRow(cause = cause, count = count, maxCount = t.topRcaCauses.first().second)
            }
        }

        // "Open the full board" link
        Text(
            "Open task board →",
            style    = AppTypography.labelLarge.copy(
                color = AppTheme.Brand, fontWeight = FontWeight.Bold,
            ),
            modifier = Modifier.clickable(onClick = onOpenBoard),
        )
    }
}

@Composable
private fun PriorityBarChart(byPriority: Map<String, Int>) {
    // Show in stable order regardless of map iteration.
    val order = listOf("High", "Medium", "Low")
    val rows  = order.map { it to (byPriority[it] ?: 0) }
    val max   = rows.maxOf { it.second }.coerceAtLeast(1)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        rows.forEach { (label, count) ->
            val tint = when (label) {
                "High"   -> AppTheme.Danger
                "Medium" -> AppTheme.Warning
                else     -> AppTheme.Success
            }
            val bg = when (label) {
                "High"   -> AppTheme.DangerBg
                "Medium" -> AppTheme.WarningBg
                else     -> AppTheme.SuccessBg
            }
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(label, style = AppTypography.bodySmall.copy(
                        color = AppTheme.Ink700, fontWeight = FontWeight.SemiBold,
                    ))
                    Text(count.toString(), style = AppTypography.bodySmall.copy(
                        color = tint, fontWeight = FontWeight.Bold,
                    ))
                }
                Spacer(Modifier.height(4.dp))
                Canvas(
                    modifier = Modifier.fillMaxWidth().height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(bg),
                ) {
                    val w = size.width * (count.toFloat() / max.toFloat())
                    drawRect(
                        color   = tint,
                        topLeft = Offset.Zero,
                        size    = Size(w.coerceAtLeast(0f), size.height),
                    )
                }
            }
        }
    }
}

@Composable
private fun RcaRow(cause: String, count: Int, maxCount: Int) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            cause,
            style    = AppTypography.bodySmall.copy(color = AppTheme.Ink700),
            modifier = Modifier.weight(1f),
        )
        Box(
            modifier = Modifier
                .width(80.dp)
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(AppTheme.Brand50),
            contentAlignment = Alignment.CenterStart,
        ) {
            val frac = count.toFloat() / maxCount.coerceAtLeast(1).toFloat()
            Box(
                modifier = Modifier
                    .height(6.dp)
                    .width((80.dp * frac))
                    .clip(RoundedCornerShape(3.dp))
                    .background(AppTheme.Brand),
            )
        }
        Spacer(Modifier.width(10.dp))
        Text(
            count.toString(),
            style = AppTypography.labelSmall.copy(
                color = AppTheme.Ink700, fontWeight = FontWeight.Bold,
            ),
        )
    }
}

/* ─── Stock section body ──────────────────────────────────────────────── */

@Composable
private fun StockSectionBody(
    s            : StockReportSummary,
    valueTrend   : List<TrendPoint>,
    onOpenSpares : () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Stock value sparkline — needs at least one historical snapshot
        // to be meaningful, otherwise the line is a single dot.
        if (valueTrend.count { it.value > 0f } >= 2) {
            SparklineCard(
                label  = "Stock value (6mo)",
                points = valueTrend,
                tint   = AppTheme.Success,
                bg     = AppTheme.SuccessBg,
                valueFormatter = { formatRupeesShort(it.toDouble()) },
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MetricChip("In stock",   s.closingStockQty.toString(),
                       modifier = Modifier.weight(1f))
            MetricChip("Used units", s.itemsConsumedQty.toString(),
                       modifier = Modifier.weight(1f))
            MetricChip("Low stock",  s.lowStockCount.toString(),
                       modifier = Modifier.weight(1f))
            MetricChip("Stocked-out", s.zeroStockCount.toString(),
                       modifier = Modifier.weight(1f))
        }

        // Phase 2 / Case 3 — Opening → Closing delta. Hidden when the
        // snapshot Cloud Function hasn't produced a doc for this month yet.
        if (s.openingStockQty != null && s.qtyDelta != null) {
            OpeningClosingDelta(s)
        }

        // Phase 2 / Case 2 — consumption source split. Surfaces inventory
        // leakage when manual deductions outpace task-driven issuance.
        if (s.consumedFromTasksQty > 0 || s.consumedManualQty > 0) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MetricChip(
                    label    = "From tasks",
                    value    = s.consumedFromTasksQty.toString(),
                    modifier = Modifier.weight(1f),
                )
                MetricChip(
                    label    = "Manual",
                    value    = s.consumedManualQty.toString(),
                    modifier = Modifier.weight(1f),
                )
            }
        }

        if (s.consumptionByEquipment.isNotEmpty()) {
            Text(
                "Consumption by equipment",
                style = AppTypography.labelLarge.copy(
                    color = AppTheme.Ink700, fontWeight = FontWeight.SemiBold,
                ),
            )
            s.consumptionByEquipment.forEach { (equipment, qty) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(AppTheme.SurfaceMuted)
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        equipment,
                        style    = AppTypography.bodySmall.copy(color = AppTheme.Ink700),
                        maxLines = 1,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        "$qty u",
                        style = AppTypography.labelSmall.copy(
                            color = AppTheme.Ink900, fontWeight = FontWeight.Bold,
                        ),
                    )
                }
            }
        }

        if (s.topConsumed.isEmpty()) {
            EmptyHint("No materials consumed in this period.")
        } else {
            Text(
                "Top consumed items",
                style = AppTypography.labelLarge.copy(
                    color = AppTheme.Ink700, fontWeight = FontWeight.SemiBold,
                ),
            )
            s.topConsumed.forEach { row ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(AppTheme.SurfaceMuted)
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        row.itemName,
                        style    = AppTypography.bodyMedium.copy(
                            color = AppTheme.Ink900, fontWeight = FontWeight.SemiBold,
                        ),
                        maxLines = 1,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        "${row.qty} u",
                        style = AppTypography.labelSmall.copy(
                            color = AppTheme.Ink700, fontWeight = FontWeight.Bold,
                        ),
                    )
                }
            }
        }

        Text(
            "Open inventory →",
            style    = AppTypography.labelLarge.copy(
                color = AppTheme.Brand, fontWeight = FontWeight.Bold,
            ),
            modifier = Modifier.clickable(onClick = onOpenSpares),
        )
    }
}

/* ─── Opening → Closing delta panel ───────────────────────────────────── */

@Composable
private fun OpeningClosingDelta(s: StockReportSummary) {
    val qtyDelta   = s.qtyDelta ?: return
    val valueDelta = s.valueDelta ?: 0.0
    val opening    = s.openingStockQty ?: return
    val closing    = s.closingStockQty
    val tint = when {
        qtyDelta > 0  -> AppTheme.Success
        qtyDelta < 0  -> AppTheme.Danger
        else          -> AppTheme.Ink700
    }
    val bg = when {
        qtyDelta > 0  -> AppTheme.SuccessBg
        qtyDelta < 0  -> AppTheme.DangerBg
        else          -> AppTheme.SurfaceMuted
    }
    val sign = if (qtyDelta > 0) "+" else ""
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(bg)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            "Opening → Closing",
            style = AppTypography.labelSmall.copy(
                color = tint.copy(alpha = 0.85f),
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.8.sp,
            ),
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "$opening → $closing units",
                style = AppTypography.titleSmall.copy(
                    color = AppTheme.Ink900, fontWeight = FontWeight.Bold,
                ),
                modifier = Modifier.weight(1f),
            )
            Text(
                "$sign$qtyDelta",
                style = AppTypography.titleSmall.copy(
                    color = tint, fontWeight = FontWeight.ExtraBold,
                ),
            )
        }
        if (valueDelta != 0.0) {
            val valueSign = if (valueDelta > 0) "+" else ""
            Text(
                "Value change: $valueSign${formatRupees(valueDelta)}",
                style = AppTypography.bodySmall.copy(color = tint),
            )
        }
    }
}

private fun formatRupees(v: Double): String {
    val abs = kotlin.math.abs(v)
    val sign = if (v < 0) "-" else ""
    return when {
        abs >= 100_000 -> "${sign}₹${(abs / 1_000).toInt()}k"
        else           -> "${sign}₹${abs.toInt()}"
    }
}

/* ─── Trend sparkline card ────────────────────────────────────────────── */

@Composable
private fun SparklineCard(
    label         : String,
    points        : List<TrendPoint>,
    tint          : Color,
    bg            : Color,
    valueFormatter: (Float) -> String,
) {
    if (points.size < 2) return
    val latest = points.last().value
    val prev   = points[points.lastIndex - 1].value
    val delta  = latest - prev
    val deltaSign = when {
        delta > 0f && prev > 0f -> "+${((delta / prev) * 100f).toInt()}%"
        delta < 0f && prev > 0f -> "${((delta / prev) * 100f).toInt()}%"
        delta > 0f              -> "↑"
        delta < 0f              -> "↓"
        else                    -> "·"
    }
    val deltaColor = when {
        delta > 0f && label.contains("Reopen") -> AppTheme.Danger
        delta < 0f && label.contains("Reopen") -> AppTheme.Success
        delta > 0f -> AppTheme.Success
        delta < 0f -> AppTheme.Danger
        else       -> AppTheme.Ink500
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                label,
                style = AppTypography.labelSmall.copy(
                    color = tint.copy(alpha = 0.9f),
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.6.sp,
                ),
                modifier = Modifier.weight(1f),
            )
            Text(
                valueFormatter(latest),
                style = AppTypography.titleSmall.copy(
                    color = AppTheme.Ink900, fontWeight = FontWeight.ExtraBold,
                ),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                deltaSign,
                style = AppTypography.labelSmall.copy(
                    color = deltaColor, fontWeight = FontWeight.Bold,
                ),
            )
        }
        Sparkline(
            points = points.map { it.value },
            tint   = tint,
            modifier = Modifier.fillMaxWidth().height(36.dp),
        )
        Row(modifier = Modifier.fillMaxWidth()) {
            points.forEachIndexed { i, p ->
                Text(
                    p.month.month.shortName(),
                    style = AppTypography.labelSmall.copy(
                        color = if (i == points.lastIndex) tint else AppTheme.Ink500,
                        fontWeight = if (i == points.lastIndex) FontWeight.Bold
                                     else FontWeight.Normal,
                    ),
                    textAlign = TextAlign.Center,
                    modifier  = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun Sparkline(
    points  : List<Float>,
    tint    : Color,
    modifier: Modifier = Modifier,
) {
    if (points.size < 2) return
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val pad = 4f
        val min = points.min()
        val max = points.max()
        val span = (max - min).takeIf { it > 0f } ?: 1f
        val stepX = (w - pad * 2) / (points.size - 1)
        val path = Path()
        points.forEachIndexed { i, v ->
            val x = pad + stepX * i
            val y = h - pad - ((v - min) / span) * (h - pad * 2)
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(
            path  = path,
            color = tint,
            style = Stroke(width = 2.5f, cap = StrokeCap.Round),
        )
        // End-point dot — anchors the eye on "now".
        val lastIdx = points.lastIndex
        val lastX = pad + stepX * lastIdx
        val lastY = h - pad - ((points.last() - min) / span) * (h - pad * 2)
        drawCircle(color = tint, radius = 3f, center = Offset(lastX, lastY))
    }
}

private fun formatRupeesShort(v: Double): String {
    val abs = kotlin.math.abs(v)
    return when {
        abs >= 10_000_000 -> "₹${(abs / 1_000_000.0 * 10).toInt() / 10.0}M"
        abs >= 100_000    -> "₹${(abs / 1_000).toInt()}k"
        else              -> "₹${abs.toInt()}"
    }
}

/* ─── Shared bits ─────────────────────────────────────────────────────── */

@Composable
private fun MetricChip(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(AppTheme.SurfaceMuted)
            .padding(horizontal = 8.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            value,
            style = AppTypography.titleSmall.copy(
                color = AppTheme.Ink900, fontWeight = FontWeight.ExtraBold,
            ),
            textAlign = TextAlign.Center,
        )
        Text(
            label,
            style = AppTypography.labelSmall.copy(color = AppTheme.Ink500),
            textAlign = TextAlign.Center,
        )
    }
}

private fun formatDuration(minutes: Long): String {
    if (minutes < 60) return "${minutes}m"
    val h = minutes / 60
    val m = minutes % 60
    return if (m == 0L) "${h}h" else "${h}h ${m}m"
}
