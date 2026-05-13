package com.example.uniwattelektrik.feature.admin.presentation.screens.components

import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.uniwattelektrik.core.theme.AppTheme
import com.example.uniwattelektrik.feature.workforce.data.remote.EmployeeRecord
import com.example.uniwattelektrik.feature.workforce.data.remote.TaskRecord
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.math.roundToInt

/* ─────────────────────────────────────────────────────────────────────────
 *  Design palette — employee accent colours, anchored on the brand blue
 *  and harmonised with the AppTheme semantic palette so the chart feels
 *  native to the rest of the dashboard.
 * ───────────────────────────────────────────────────────────────────────── */
val EMPLOYEE_CHART_COLORS = listOf(
    Color(0xFF1A6BF5), // Brand
    Color(0xFF14B8A6), // Teal
    Color(0xFF7C3AED), // Violet
    Color(0xFFF59E0B), // Amber
    Color(0xFF06B6D4), // Cyan
    Color(0xFFEC4899), // Pink
    Color(0xFF22C55E), // Green
    Color(0xFFF97316), // Orange
)

/* ─────────────────────────────────────────────────────────────────────────
 *  Period & chart-type enums
 * ───────────────────────────────────────────────────────────────────────── */
enum class ChartPeriod(val label: String) {
    Week("Week"),
    Month("Month"),
    Year("Year"),
}

enum class ChartType {
    Bar, Line
}

/* ─────────────────────────────────────────────────────────────────────────
 *  Data models
 * ───────────────────────────────────────────────────────────────────────── */
/** One time-bucket (e.g. "Mon", "Jan") with per-employee completion counts. */
data class ChartBucket(
    val label: String,
    /** employeeId → count */
    val counts: Map<String, Int>,
) {
    val total: Int get() = counts.values.sum()
}

data class ChartData(
    val buckets: List<ChartBucket>,
    /** Ordered list of (employeeId, name, color) — drives legend + coloring. */
    val employees: List<Triple<String, String, Color>>,
)

/* ─────────────────────────────────────────────────────────────────────────
 *  Data builder
 * ───────────────────────────────────────────────────────────────────────── */
private val DAY_MS = 24L * 60L * 60L * 1_000L

fun buildChartData(
    tasks: List<TaskRecord>,
    employees: List<EmployeeRecord>,
    now: Long,
    period: ChartPeriod,
): ChartData {
    val tz      = TimeZone.currentSystemDefault()
    val empById = employees.associateBy { it.id }

    // Only completed tasks with a timestamp
    val done = tasks.filter { it.status == "Done" && it.completedAt != null }

    // Determine buckets + which tasks fall into each bucket
    val buckets: List<ChartBucket> = when (period) {

        ChartPeriod.Week -> {
            // Last 7 calendar days, Mon → today
            val nowLocal = Instant.fromEpochMilliseconds(now).toLocalDateTime(tz)
            val todayStart = now - ((nowLocal.hour * 3600L + nowLocal.minute * 60L + nowLocal.second) * 1000L)
            val dayLabels = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
            (6 downTo 0).map { daysAgo ->
                val bucketStart = todayStart - daysAgo * DAY_MS
                val bucketEnd   = bucketStart + DAY_MS
                val bucketLocal = Instant.fromEpochMilliseconds(bucketStart).toLocalDateTime(tz)
                // Use day-of-week label derived from actual date
                val label = dayLabels[(bucketLocal.dayOfWeek.ordinal)]  // Mon=0..Sun=6
                val counts = mutableMapOf<String, Int>()
                done.filter { it.completedAt!! in bucketStart until bucketEnd }.forEach { t ->
                    val uid = t.userId?.takeIf { it.isNotBlank() } ?: "unknown"
                    counts[uid] = (counts[uid] ?: 0) + 1
                }
                ChartBucket(label = label, counts = counts)
            }
        }

        ChartPeriod.Month -> {
            // Last 4 calendar weeks (W1..W4)
            val nowLocal = Instant.fromEpochMilliseconds(now).toLocalDateTime(tz)
            val todayStart = now - ((nowLocal.hour * 3600L + nowLocal.minute * 60L + nowLocal.second) * 1000L)
            (3 downTo 0).mapIndexed { idx, weeksAgo ->
                val weekEnd   = todayStart + DAY_MS - weeksAgo * 7 * DAY_MS
                val weekStart = weekEnd - 7 * DAY_MS
                val label = "W${4 - weeksAgo}"
                val counts = mutableMapOf<String, Int>()
                done.filter { it.completedAt!! in weekStart until weekEnd }.forEach { t ->
                    val uid = t.userId?.takeIf { it.isNotBlank() } ?: "unknown"
                    counts[uid] = (counts[uid] ?: 0) + 1
                }
                ChartBucket(label = label, counts = counts)
            }
        }

        ChartPeriod.Year -> {
            // 12 calendar months Jan–Dec for the current year
            val nowLocal = Instant.fromEpochMilliseconds(now).toLocalDateTime(tz)
            val year     = nowLocal.year
            val monthLabels = listOf("Jan","Feb","Mar","Apr","May","Jun","Jul","Aug","Sep","Oct","Nov","Dec")
            (0..11).map { monthIdx ->
                val label = monthLabels[monthIdx]
                val counts = mutableMapOf<String, Int>()
                done.filter { t ->
                    val local = Instant.fromEpochMilliseconds(t.completedAt!!).toLocalDateTime(tz)
                    local.year == year && (local.monthNumber - 1) == monthIdx
                }.forEach { t ->
                    val uid = t.userId?.takeIf { it.isNotBlank() } ?: "unknown"
                    counts[uid] = (counts[uid] ?: 0) + 1
                }
                ChartBucket(label = label, counts = counts)
            }
        }
    }

    // Collect all unique employee IDs that appear in any bucket, sort by total desc
    val empTotals = mutableMapOf<String, Int>()
    buckets.forEach { b -> b.counts.forEach { (uid, cnt) -> empTotals[uid] = (empTotals[uid] ?: 0) + cnt } }
    val sortedIds = empTotals.entries.sortedByDescending { it.value }.map { it.key }

    val empTriples = sortedIds.take(EMPLOYEE_CHART_COLORS.size).mapIndexed { i, uid ->
        val name = empById[uid]?.name?.takeIf { it.isNotBlank() } ?: "Unknown"
        Triple(uid, name, EMPLOYEE_CHART_COLORS[i % EMPLOYEE_CHART_COLORS.size])
    }

    return ChartData(buckets = buckets, employees = empTriples)
}

/* ─────────────────────────────────────────────────────────────────────────
 *  Main composable
 * ───────────────────────────────────────────────────────────────────────── */
@Composable
fun PerformanceChartSection(
    tasks: List<TaskRecord>,
    employees: List<EmployeeRecord>,
    nowMs: Long,
    modifier: Modifier = Modifier,
) {
    var period    by remember { mutableStateOf(ChartPeriod.Week) }
    var chartType by remember { mutableStateOf(ChartType.Bar) }

    val chartData = remember(tasks, employees, nowMs, period) {
        buildChartData(tasks, employees, nowMs, period)
    }

    // Animate bars rising on data change
    var animate by remember(chartData) { mutableStateOf(false) }
    LaunchedEffect(chartData) { animate = true }
    val rise by animateFloatAsState(
        targetValue   = if (animate) 1f else 0f,
        animationSpec = tween(700, easing = EaseOutCubic),
        label         = "chart-rise",
    )

    // Selected bucket index for tooltip
    var selectedBucket by remember { mutableStateOf<Int?>(null) }

    // Summary metrics derived from buckets
    val total      = remember(chartData) { chartData.buckets.sumOf { it.total } }
    val peakBucket = remember(chartData) { chartData.buckets.maxByOrNull { it.total } }
    val avgPerStep = remember(chartData) {
        if (chartData.buckets.isEmpty()) 0.0
        else total.toDouble() / chartData.buckets.size
    }

    val cardShape = RoundedCornerShape(28.dp)
    Box(
        modifier = modifier
            .fillMaxWidth()
            // Ambient cyan-tinted spot shadow gives the card a subtle "lift",
            // matching the premium analytics-widget feel in the brief.
            .shadow(
                elevation    = 12.dp,
                shape        = cardShape,
                ambientColor = AppTheme.Brand.copy(alpha = 0.08f),
                spotColor    = AppTheme.Brand.copy(alpha = 0.14f),
            )
            .clip(cardShape)
            // Very soft blue-gray mesh gradient — replaces flat white so the
            // surface picks up light differently across its body.
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        Color(0xFFFCFDFF),
                        Color(0xFFF5F8FE),
                        Color(0xFFFAFBFF),
                    ),
                ),
            )
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        AppTheme.Brand.copy(alpha = 0.18f),
                        AppTheme.Ink100,
                        AppTheme.Brand.copy(alpha = 0.10f),
                    ),
                ),
                shape = cardShape,
            )
            .padding(20.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {

            // ── Header row ──────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Brand-tinted leading badge for an executive feel
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(AppTheme.Brand, AppTheme.Brand700),
                            ),
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector        = Icons.Filled.BarChart,
                        contentDescription = null,
                        tint               = Color.White,
                        modifier           = Modifier.size(20.dp),
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text       = "Task Completions",
                        color      = AppTheme.Ink900,
                        fontSize   = 16.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.3).sp,
                    )
                    Text(
                        text     = "By employee · ${period.label}",
                        color    = AppTheme.Ink500,
                        fontSize = 12.sp,
                    )
                }

                // Chart-type toggle (Bar / Line)
                ChartTypeToggle(
                    selected  = chartType,
                    onSelect  = { chartType = it; selectedBucket = null },
                )
            }

            // ── Summary stats strip ─────────────────────────────────────
            if (chartData.employees.isNotEmpty()) {
                SummaryStatsRow(
                    total       = total,
                    avg         = avgPerStep,
                    peakLabel   = peakBucket?.label.orEmpty(),
                    peakValue   = peakBucket?.total ?: 0,
                    periodLabel = period.label,
                    sparkline   = chartData.buckets.map { it.total },
                )
            }

            // ── Period selector ─────────────────────────────────────────
            PeriodSelector(
                selected = period,
                onSelect = { period = it; selectedBucket = null; animate = false },
            )

            // ── Chart body ──────────────────────────────────────────────
            if (chartData.employees.isEmpty()) {
                EmptyChartState()
            } else {
                when (chartType) {
                    ChartType.Bar  -> BarChartBody(
                        data            = chartData,
                        rise            = rise,
                        selectedBucket  = selectedBucket,
                        onBucketSelect  = { selectedBucket = if (selectedBucket == it) null else it },
                    )
                    ChartType.Line -> LineChartBody(
                        data           = chartData,
                        rise           = rise,
                        selectedBucket = selectedBucket,
                        onBucketSelect = { selectedBucket = if (selectedBucket == it) null else it },
                    )
                }

                // ── Tooltip ───────────────────────────────────────────
                val sel = selectedBucket
                if (sel != null && sel < chartData.buckets.size) {
                    BucketTooltip(
                        bucket    = chartData.buckets[sel],
                        employees = chartData.employees,
                        onDismiss = { selectedBucket = null },
                    )
                }

                // ── Legend ────────────────────────────────────────────
                EmployeeLegend(employees = chartData.employees)
            }
        }
    }
}

/* ─────────────────────────────────────────────────────────────────────────
 *  Period selector — premium animated sliding pill
 * ───────────────────────────────────────────────────────────────────────── */
@Composable
private fun PeriodSelector(
    selected: ChartPeriod,
    onSelect: (ChartPeriod) -> Unit,
) {
    val periods = ChartPeriod.entries
    val selectedIndex = periods.indexOf(selected)

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(AppTheme.SurfaceMuted)
            .border(1.dp, AppTheme.Ink100, RoundedCornerShape(14.dp))
            .padding(4.dp),
    ) {
        val pillWidth = (maxWidth - 8.dp) / periods.size

        // Animated sliding pill offset
        val pillOffsetX by animateDpAsState(
            targetValue   = pillWidth * selectedIndex,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness    = Spring.StiffnessMedium,
            ),
            label = "period-pill",
        )

        // Sliding gradient pill background — anchored to brand
        Box(
            modifier = Modifier
                .offset(x = pillOffsetX)
                .width(pillWidth)
                .height(36.dp)
                .shadow(
                    elevation    = 4.dp,
                    shape        = RoundedCornerShape(10.dp),
                    ambientColor = AppTheme.ShadowMd,
                    spotColor    = AppTheme.ShadowMd,
                )
                .clip(RoundedCornerShape(10.dp))
                .background(
                    Brush.linearGradient(
                        colors = listOf(AppTheme.Brand, AppTheme.Brand700),
                    ),
                ),
        )

        // Labels row on top of pill
        Row(modifier = Modifier.fillMaxWidth()) {
            periods.forEach { p ->
                val isSelected = p == selected
                Box(
                    modifier = Modifier
                        .width(pillWidth)
                        .height(36.dp)
                        .clickable { onSelect(p) },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text       = p.label,
                        color      = if (isSelected) Color.White else AppTheme.Ink500,
                        fontSize   = 13.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        letterSpacing = if (isSelected) 0.3.sp else 0.sp,
                    )
                }
            }
        }
    }
}

/* ─────────────────────────────────────────────────────────────────────────
 *  Chart type toggle (Bar / Line icons)
 * ───────────────────────────────────────────────────────────────────────── */
@Composable
private fun ChartTypeToggle(
    selected: ChartType,
    onSelect: (ChartType) -> Unit,
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(AppTheme.SurfaceMuted)
            .border(1.dp, AppTheme.Ink100, RoundedCornerShape(10.dp))
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        listOf(ChartType.Bar to Icons.Filled.BarChart, ChartType.Line to Icons.AutoMirrored.Filled.ShowChart)
            .forEach { (type, icon) ->
                val isSelected = type == selected
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) AppTheme.Brand else Color.Transparent)
                        .clickable { onSelect(type) }
                        .padding(8.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector        = icon,
                        contentDescription = null,
                        tint               = if (isSelected) Color.White else AppTheme.Ink500,
                        modifier           = Modifier.size(18.dp),
                    )
                }
            }
    }
}

/* ─────────────────────────────────────────────────────────────────────────
 *  Bar chart body — premium grouped bars with gridlines, gradient fills,
 *  rounded tops and a soft Y-axis scale on the left.
 * ───────────────────────────────────────────────────────────────────────── */
@Composable
private fun BarChartBody(
    data: ChartData,
    rise: Float,
    selectedBucket: Int?,
    onBucketSelect: (Int) -> Unit,
) {
    val chartHeight = 180.dp
    val scrollState = rememberScrollState()

    // Auto-scroll to end
    var scrolled by remember { mutableStateOf(false) }
    LaunchedEffect(scrollState.maxValue) {
        if (!scrolled && scrollState.maxValue > 0) {
            scrollState.scrollTo(scrollState.maxValue)
            scrolled = true
        }
    }

    val maxVal     = data.buckets.flatMap { it.counts.values }.maxOrNull()?.coerceAtLeast(1) ?: 1
    val niceMax    = niceCeil(maxVal)
    val numEmps    = data.employees.size.coerceAtLeast(1)
    val numBuckets = data.buckets.size.coerceAtLeast(1)

    // Minimum sizes — used as the floor when there isn't enough width and the
    // chart needs to scroll horizontally. Bars are intentionally chunky so the
    // graph reads as a premium dashboard, not a sparkline.
    val minBarWidth   = if (numEmps <= 2) 34.dp else if (numEmps <= 4) 24.dp else 18.dp
    val maxBarWidth   = if (numEmps <= 2) 56.dp else if (numEmps <= 4) 42.dp else 32.dp
    val barGap        = 6.dp
    val groupHPadding = 8.dp   // 4dp on each side inside a group column
    val minGroupWidth = (minBarWidth.value * numEmps +
                         barGap.value * (numEmps - 1) +
                         groupHPadding.value * 2 + 12).dp

    Row(
        modifier = Modifier.fillMaxWidth().height(chartHeight),
        verticalAlignment = Alignment.Top,
    ) {
        // Y-axis scale
        YAxisScale(
            niceMax  = niceMax,
            modifier = Modifier
                .width(28.dp)
                .fillMaxHeight()
                .padding(bottom = 24.dp, top = 6.dp),
        )

        // Plot area — use BoxWithConstraints so we know how much horizontal
        // room is available and can either (a) stretch each group to fill
        // the chart when there's room, or (b) fall back to the per-group
        // minimum and let horizontal scrolling kick in.
        BoxWithConstraints(modifier = Modifier.weight(1f).fillMaxHeight()) {
            val available    = maxWidth                                  // plot width
            val groupGapTotal = barGap * (numBuckets - 1).coerceAtLeast(0)
            val fitGroup     = ((available - groupGapTotal) / numBuckets)
                .coerceAtLeast(minGroupWidth)
            val groupWidth   = fitGroup
            // Stretch bars proportionally when there's spare room; bars can
            // grow up to 36 dp wide which keeps them legible on tablets.
            val innerSpace   = (groupWidth - groupHPadding * 2 -
                                barGap * (numEmps - 1).coerceAtLeast(0))
                .coerceAtLeast(minBarWidth * numEmps)
            // When there's only ONE employee (i.e. only one bar per day-group),
            // let the bar grow to fill the whole day slot — no upper cap. With
            // multiple employees we keep the cap so the bars don't go absurdly
            // wide on tablets.
            val rawBarWidth  = innerSpace / numEmps
            val barWidth     = if (numEmps == 1)
                rawBarWidth.coerceAtLeast(minBarWidth)
            else
                rawBarWidth.coerceIn(minBarWidth, maxBarWidth)

            // Gridlines behind everything
            GridLines(
                divisions = 4,
                modifier  = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .padding(bottom = 24.dp, top = 6.dp),
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(scrollState)
                    .fillMaxHeight(),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(barGap),
            ) {
                data.buckets.forEachIndexed { bi, bucket ->
                    val isSelected = selectedBucket == bi
                    Column(
                        modifier = Modifier
                            .width(groupWidth)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSelected) AppTheme.Brand50 else Color.Transparent)
                            .clickable { onBucketSelect(bi) }
                            .padding(horizontal = 4.dp, vertical = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Bottom,
                    ) {
                        // Total badge above the tallest bar when selected
                        if (isSelected && bucket.total > 0) {
                            Text(
                                text       = bucket.total.toString(),
                                color      = AppTheme.Brand,
                                fontSize   = 11.sp,
                                fontWeight = FontWeight.Bold,
                            )
                            Spacer(Modifier.height(2.dp))
                        }

                        // Bars row
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(barGap, Alignment.CenterHorizontally),
                            verticalAlignment = Alignment.Bottom,
                        ) {
                            data.employees.forEach { (uid, _, color) ->
                                val cnt   = bucket.counts[uid] ?: 0
                                val ratio = (cnt.toFloat() / niceMax) * rise
                                val alpha = if (selectedBucket == null || isSelected) 1f else 0.35f
                                Box(
                                    modifier = Modifier
                                        .width(barWidth)
                                        .fillMaxHeight(ratio.coerceIn(if (cnt > 0) 0.025f else 0f, 1f))
                                        .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                                        .background(
                                            Brush.verticalGradient(
                                                colors = listOf(
                                                    color.copy(alpha = alpha),
                                                    color.copy(alpha = alpha * 0.65f),
                                                ),
                                            ),
                                        ),
                                )
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text       = bucket.label,
                            color      = if (isSelected) AppTheme.Brand else AppTheme.Ink300,
                            fontSize   = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            maxLines   = 1,
                        )
                    }
                }
            }
        }
    }
}

/* ─────────────────────────────────────────────────────────────────────────
 *  Line chart body — smooth cubic-Bezier curves, soft gradient area fill,
 *  themed gridlines and crosshair on the selected bucket.
 * ───────────────────────────────────────────────────────────────────────── */
@Composable
private fun LineChartBody(
    data: ChartData,
    rise: Float,
    selectedBucket: Int?,
    onBucketSelect: (Int) -> Unit,
) {
    val chartHeight = 180.dp
    val labelHeight = 24.dp
    val maxVal      = data.buckets.flatMap { it.counts.values }.maxOrNull()?.coerceAtLeast(1) ?: 1
    val niceMax     = niceCeil(maxVal)
    val numBuckets  = data.buckets.size.coerceAtLeast(2)
    val density     = LocalDensity.current

    Row(modifier = Modifier.fillMaxWidth().height(chartHeight)) {
        // Y-axis scale on the left
        YAxisScale(
            niceMax  = niceMax,
            modifier = Modifier
                .width(28.dp)
                .fillMaxHeight()
                .padding(bottom = labelHeight, top = 6.dp),
        )

        Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
                    // ── Touch: map tap X → nearest bucket index ──────────
                    .pointerInput(numBuckets) {
                        detectTapGestures { offset ->
                            val stepX = size.width.toFloat() / (numBuckets - 1).coerceAtLeast(1)
                            val bucketIdx = (offset.x / stepX).toInt().coerceIn(0, numBuckets - 1)
                            onBucketSelect(bucketIdx)
                        }
                    },
            ) {
                val labelPx = with(density) { labelHeight.toPx() }
                val topPad  = with(density) { 6.dp.toPx() }
                val w       = size.width
                val h       = size.height - labelPx - topPad
                val stepX   = w / (numBuckets - 1).toFloat().coerceAtLeast(1f)
                val gridColor = AppTheme.Ink100

                // Horizontal gridlines (4 divisions)
                val gridCount = 4
                repeat(gridCount + 1) { gi ->
                    val y = topPad + h * (1f - gi.toFloat() / gridCount)
                    drawLine(
                        color       = gridColor,
                        start       = Offset(0f, y),
                        end         = Offset(w, y),
                        strokeWidth = 1.dp.toPx(),
                    )
                }

                // Vertical selected-bucket crosshair
                val sel = selectedBucket
                if (sel != null && sel in 0 until numBuckets) {
                    val x = stepX * sel
                    drawLine(
                        color       = AppTheme.Brand.copy(alpha = 0.20f),
                        start       = Offset(x, topPad),
                        end         = Offset(x, topPad + h),
                        strokeWidth = 2.dp.toPx(),
                    )
                }

                // One smooth curve + filled area per employee
                data.employees.forEach { (uid, _, color) ->
                    val faded = selectedBucket != null
                    val points = data.buckets.mapIndexed { bi, bucket ->
                        val cnt   = bucket.counts[uid] ?: 0
                        val ratio = (cnt.toFloat() / niceMax) * rise
                        Offset(stepX * bi, topPad + h * (1f - ratio.coerceIn(0f, 1f)))
                    }

                    if (points.size >= 2) {
                        val curve = buildSmoothPath(points)

                        // Filled area under the curve
                        val fillPath = Path().apply {
                            addPath(curve)
                            lineTo(points.last().x, topPad + h)
                            lineTo(points.first().x, topPad + h)
                            close()
                        }
                        drawPath(
                            fillPath,
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    color.copy(alpha = if (faded) 0.10f else 0.22f),
                                    Color.Transparent,
                                ),
                                startY = topPad,
                                endY   = topPad + h,
                            ),
                        )

                        // Smooth stroke
                        drawPath(
                            curve,
                            color = color.copy(alpha = if (faded) 0.45f else 1f),
                            style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round),
                        )

                        // Dots
                        points.forEachIndexed { bi, pt ->
                            val isSelected = sel == bi
                            val outerR = if (isSelected) 7.dp.toPx() else 4.dp.toPx()
                            val innerR = if (isSelected) 4.dp.toPx() else 2.dp.toPx()
                            if (isSelected) {
                                drawCircle(
                                    color  = color.copy(alpha = 0.18f),
                                    radius = 12.dp.toPx(),
                                    center = pt,
                                )
                            }
                            drawCircle(color = Color.White, radius = outerR, center = pt)
                            drawCircle(
                                color  = color.copy(alpha = if (faded && !isSelected) 0.45f else 1f),
                                radius = innerR,
                                center = pt,
                            )
                        }
                    }
                }
            }

            // X-axis labels
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                data.buckets.forEachIndexed { bi, bucket ->
                    val isSelected = selectedBucket == bi
                    Text(
                        text       = bucket.label,
                        color      = if (isSelected) AppTheme.Brand else AppTheme.Ink300,
                        fontSize   = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        modifier   = Modifier.weight(1f),
                        maxLines   = 1,
                    )
                }
            }
        }
    }
}

/** Catmull-Rom-style smoothing → cubic Bezier path through the given points. */
private fun buildSmoothPath(points: List<Offset>): Path {
    val path = Path()
    if (points.isEmpty()) return path
    path.moveTo(points.first().x, points.first().y)
    if (points.size < 3) {
        points.drop(1).forEach { path.lineTo(it.x, it.y) }
        return path
    }
    val tension = 0.18f
    for (i in 0 until points.size - 1) {
        val p0 = if (i == 0) points[i] else points[i - 1]
        val p1 = points[i]
        val p2 = points[i + 1]
        val p3 = if (i + 2 < points.size) points[i + 2] else p2
        val c1x = p1.x + (p2.x - p0.x) * tension
        val c1y = p1.y + (p2.y - p0.y) * tension
        val c2x = p2.x - (p3.x - p1.x) * tension
        val c2y = p2.y - (p3.y - p1.y) * tension
        path.cubicTo(c1x, c1y, c2x, c2y, p2.x, p2.y)
    }
    return path
}

/* ─────────────────────────────────────────────────────────────────────────
 *  Tooltip card shown when a bucket is selected
 * ───────────────────────────────────────────────────────────────────────── */
@Composable
private fun BucketTooltip(
    bucket: ChartBucket,
    employees: List<Triple<String, String, Color>>,
    onDismiss: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation    = 8.dp,
                shape        = RoundedCornerShape(16.dp),
                ambientColor = AppTheme.ShadowLg,
                spotColor    = AppTheme.ShadowLg,
            )
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(AppTheme.Navy, Color(0xFF1B2A52)),
                ),
            )
            .clickable { onDismiss() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(AppTheme.Brand),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text       = bucket.label,
                    color      = Color.White,
                    fontSize   = 13.sp,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text       = "${bucket.total} completed",
                    color      = Color(0xFFB8C5E0),
                    fontSize   = 12.sp,
                    fontWeight = FontWeight.Medium,
                    modifier   = Modifier.weight(1f),
                )
                Text(
                    text     = "✕",
                    color    = Color(0xFF8E9FC0),
                    fontSize = 13.sp,
                )
            }
            if (bucket.counts.isEmpty()) {
                Text("No completions", color = Color(0xFF8E9FC0), fontSize = 12.sp)
            } else {
                val maxInBucket = employees.maxOf { (u, _, _) -> bucket.counts[u] ?: 0 }.coerceAtLeast(1)
                employees.filter { (uid, _, _) -> (bucket.counts[uid] ?: 0) > 0 }
                    .forEach { (uid, name, color) ->
                        val cnt = bucket.counts[uid] ?: 0
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(color),
                            )
                            Text(
                                text       = name,
                                color      = Color(0xFFE2E8F0),
                                fontSize   = 12.sp,
                                fontWeight = FontWeight.Medium,
                                modifier   = Modifier.weight(1f),
                                maxLines   = 1,
                            )
                            // Mini bar
                            val ratio = cnt.toFloat() / maxInBucket
                            Box(
                                modifier = Modifier
                                    .height(6.dp)
                                    .width((80 * ratio).dp.coerceAtLeast(8.dp))
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(
                                        Brush.horizontalGradient(
                                            colors = listOf(color.copy(alpha = 0.55f), color),
                                        ),
                                    ),
                            )
                            Text(
                                text       = cnt.toString(),
                                color      = color,
                                fontSize   = 12.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
            }
        }
    }
}

/* ─────────────────────────────────────────────────────────────────────────
 *  Employee legend — chip style, wraps cleanly
 * ───────────────────────────────────────────────────────────────────────── */
@Composable
private fun EmployeeLegend(employees: List<Triple<String, String, Color>>) {
    if (employees.isEmpty()) return
    // Compact chips — show top 4 by default, expand to all on tap. Keeps the
    // analytics card from sprawling vertically when the team is large.
    val collapsedCap = 4
    var expanded by remember(employees) { mutableStateOf(false) }
    val needsExpand = employees.size > collapsedCap
    val visible = if (expanded || !needsExpand) employees else employees.take(collapsedCap)

    androidx.compose.foundation.layout.FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement   = Arrangement.spacedBy(6.dp),
    ) {
        visible.forEach { (_, name, color) ->
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(color.copy(alpha = 0.08f))
                    .border(1.dp, color.copy(alpha = 0.22f), RoundedCornerShape(10.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(color),
                )
                Text(
                    text       = name.split(" ").first(),
                    color      = AppTheme.Ink700,
                    fontSize   = 10.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines   = 1,
                )
            }
        }
        if (needsExpand) {
            val remaining = employees.size - collapsedCap
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(AppTheme.SurfaceMuted)
                    .border(1.dp, AppTheme.Ink100, RoundedCornerShape(10.dp))
                    .clickable { expanded = !expanded }
                    .padding(horizontal = 9.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text       = if (expanded) "Show less" else "+$remaining more",
                    color      = AppTheme.Brand,
                    fontSize   = 10.5.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

/* ─────────────────────────────────────────────────────────────────────────
 *  Summary stats strip — Total · Average · Peak
 * ───────────────────────────────────────────────────────────────────────── */
@Composable
private fun SummaryStatsRow(
    total: Int,
    avg: Double,
    peakLabel: String,
    peakValue: Int,
    periodLabel: String,
    sparkline: List<Int>,
) {
    val avgText = if (avg >= 10) avg.roundToInt().toString()
                  else ((avg * 10).roundToInt() / 10.0).toString()
    // Average trend caption — compare second-half vs first-half so the user
    // sees momentum. Quiet, deliberate — no fake precision.
    val trendCaption = remember(sparkline) {
        if (sparkline.size < 2) ""
        else {
            val half = sparkline.size / 2
            val first = sparkline.take(half).sum()
            val second = sparkline.drop(half).sum()
            when {
                second > first  -> "↑ trending up"
                second < first  -> "↓ trending down"
                else            -> "stable"
            }
        }
    }
    Row(
        // IntrinsicSize.Max measures the tallest child first, then stretches the
        // shorter ones to match — so Total / Average / Peak always share one
        // common height regardless of which decoration (sparkline, trend caption,
        // none) each tile renders.
        modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Max),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        StatTile(
            modifier  = Modifier.weight(1f).fillMaxHeight(),
            label     = "Total",
            value     = total.toString(),
            sublabel  = "This ${periodLabel.lowercase()}",
            accent    = AppTheme.Brand,
            tintBg    = AppTheme.Brand50,
            sparkline = sparkline,
        )
        StatTile(
            modifier  = Modifier.weight(1f).fillMaxHeight(),
            label     = "Average",
            value     = avgText,
            sublabel  = "${stepLabel(periodLabel)} avg",
            accent    = AppTheme.Success,
            tintBg    = AppTheme.SuccessBg,
            trendCaption = trendCaption,
        )
        StatTile(
            modifier  = Modifier.weight(1f).fillMaxHeight(),
            label     = "Peak",
            value     = peakValue.toString(),
            sublabel  = peakLabel.ifBlank { "—" },
            accent    = AppTheme.Warning,
            tintBg    = AppTheme.WarningBg,
            topBadge  = "HIGHEST",
        )
    }
}

private fun stepLabel(period: String): String = when (period) {
    "Week"  -> "Daily"
    "Month" -> "Weekly"
    "Year"  -> "Monthly"
    else    -> "Step"
}

@Composable
private fun StatTile(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    sublabel: String,
    accent: Color,
    tintBg: Color,
    sparkline: List<Int>? = null,
    trendCaption: String? = null,
    topBadge: String? = null,
) {
    Column(
        modifier = modifier
            .shadow(
                elevation    = 1.dp,
                shape        = RoundedCornerShape(20.dp),
                ambientColor = accent.copy(alpha = 0.10f),
                spotColor    = accent.copy(alpha = 0.10f),
            )
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        tintBg.copy(alpha = 0.85f),
                        Color.White.copy(alpha = 0.65f),
                    ),
                ),
            )
            .border(1.dp, accent.copy(alpha = 0.18f), RoundedCornerShape(20.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        // Top row: uppercase label + optional badge.
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text       = label.uppercase(),
                color      = accent,
                fontSize   = 9.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.0.sp,
                modifier = Modifier.weight(1f),
            )
            if (topBadge != null) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(accent.copy(alpha = 0.18f))
                        .padding(horizontal = 5.dp, vertical = 1.dp),
                ) {
                    Text(
                        text       = topBadge,
                        color      = accent,
                        fontSize   = 7.5.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp,
                    )
                }
            }
        }
        // Big number — the dominant visual.
        Text(
            text       = value,
            color      = AppTheme.Ink900,
            fontSize   = 24.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = (-0.6).sp,
        )
        Text(
            text     = sublabel,
            color    = AppTheme.Ink500,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
        )
        // Optional decoration row — sparkline OR trend caption (mutually
        // exclusive at the call site so tiles stay the same height).
        when {
            sparkline != null && sparkline.size >= 2 -> {
                Spacer(Modifier.height(2.dp))
                StatSparkline(
                    values = sparkline.map { it.toFloat() },
                    tint   = accent,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(18.dp),
                )
            }
            !trendCaption.isNullOrBlank() -> {
                Spacer(Modifier.height(2.dp))
                // "Trending down" must read as a warning — override the tile's
                // (green) accent with the brand Danger red so a regression is
                // unmistakable. Up / stable keep the accent colour.
                val captionColor = if (trendCaption.contains('↓')) {
                    AppTheme.Danger
                } else {
                    accent.copy(alpha = 0.9f)
                }
                Text(
                    text     = trendCaption,
                    color    = captionColor,
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.2.sp,
                )
            }
        }
    }
}

/** Inline sparkline — strokes + soft gradient fill underneath. */
@Composable
private fun StatSparkline(
    values  : List<Float>,
    tint    : Color,
    modifier: Modifier = Modifier,
) {
    if (values.size < 2) return
    Canvas(modifier = modifier) {
        val pad = 2f
        val w   = size.width
        val h   = size.height
        val min = values.min()
        val max = values.max()
        val span = (max - min).takeIf { it > 0f } ?: 1f
        val stepX = (w - pad * 2) / (values.size - 1)
        val points = values.mapIndexed { i, v ->
            val x = pad + stepX * i
            val y = h - pad - ((v - min) / span) * (h - pad * 2)
            Offset(x, y)
        }
        // Soft fill area beneath the line.
        val fillPath = Path().apply {
            moveTo(points.first().x, h)
            points.forEach { lineTo(it.x, it.y) }
            lineTo(points.last().x, h)
            close()
        }
        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(tint.copy(alpha = 0.30f), tint.copy(alpha = 0.0f)),
            ),
        )
        // Stroke line on top.
        val strokePath = Path().apply {
            moveTo(points.first().x, points.first().y)
            for (i in 1 until points.size) lineTo(points[i].x, points[i].y)
        }
        drawPath(
            path  = strokePath,
            color = tint,
            style = Stroke(width = 1.6f, cap = StrokeCap.Round),
        )
        // End-point dot.
        drawCircle(color = tint, radius = 2.4f, center = points.last())
    }
}

/* ─────────────────────────────────────────────────────────────────────────
 *  Y-axis numeric scale (5 ticks, 0..niceMax)
 * ───────────────────────────────────────────────────────────────────────── */
@Composable
private fun YAxisScale(niceMax: Int, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.End,
    ) {
        val ticks = 5
        for (i in 0 until ticks) {
            val v = niceMax - (niceMax * i / (ticks - 1))
            Text(
                text     = v.toString(),
                color    = AppTheme.Ink300,
                fontSize = 9.sp,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

/* ─────────────────────────────────────────────────────────────────────────
 *  Background gridlines drawn behind chart bars
 * ───────────────────────────────────────────────────────────────────────── */
@Composable
private fun GridLines(divisions: Int, modifier: Modifier = Modifier) {
    val gridColor = AppTheme.Ink100
    Canvas(modifier = modifier) {
        repeat(divisions + 1) { gi ->
            val y = size.height * (1f - gi.toFloat() / divisions)
            drawLine(
                color       = gridColor,
                start       = Offset(0f, y),
                end         = Offset(size.width, y),
                strokeWidth = 1.dp.toPx(),
            )
        }
    }
}

/* ─────────────────────────────────────────────────────────────────────────
 *  Empty state
 * ───────────────────────────────────────────────────────────────────────── */
@Composable
private fun EmptyChartState() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(AppTheme.SurfaceMuted),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(AppTheme.Brand50),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector        = Icons.Filled.BarChart,
                    contentDescription = null,
                    tint               = AppTheme.Brand,
                    modifier           = Modifier.size(22.dp),
                )
            }
            Text(
                "No completed tasks yet",
                color      = AppTheme.Ink700,
                fontSize   = 13.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                "Completed tasks will appear here.",
                color    = AppTheme.Ink500,
                fontSize = 11.sp,
            )
        }
    }
}

/** Round a max value up to a friendly tick (1, 2, 5, 10, 20, 50…). */
private fun niceCeil(v: Int): Int {
    if (v <= 1) return 1
    val mag = generateSequence(1) { it * 10 }.first { it * 10 > v }
    return when {
        v <= mag       -> mag
        v <= mag * 2   -> mag * 2
        v <= mag * 5   -> mag * 5
        else           -> mag * 10
    }
}

