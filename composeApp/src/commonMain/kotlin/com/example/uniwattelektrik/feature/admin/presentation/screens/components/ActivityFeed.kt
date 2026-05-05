package com.example.uniwattelektrik.feature.admin.presentation.screens.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.uniwattelektrik.feature.workforce.data.remote.AttendanceRecord
import com.example.uniwattelektrik.feature.workforce.data.remote.EmployeeRecord
import com.example.uniwattelektrik.feature.admin.presentation.screens.inventory.SpareItem
import com.example.uniwattelektrik.feature.workforce.data.remote.TaskRecord

/* ──────────────────────────────────────────────────────────────────────────
 *  ActivityItem — heterogeneous feed model
 *
 *  Each subtype carries the bare minimum required to render a row AND deep-link
 *  back to the relevant detail screen on tap. The `at` timestamp drives sorting
 *  and the relative-time label.
 * ────────────────────────────────────────────────────────────────────────── */

sealed interface ActivityItem {
    val at: Long
    val title: String
    val subtitle: String
    val emoji: String
    val tint: Color
    /** "tasks" | "people" | "attendance" | "inventory" — drives filter chips. */
    val category: String
    /** Optional avatar URL (employee photo). When non-null, replaces the emoji bubble. */
    val photoUrl: String? get() = null

    data class TaskCompleted(
        override val at: Long,
        override val title: String,
        override val subtitle: String,
        val taskId: String,
        val userId: String? = null,
        override val photoUrl: String? = null,
    ) : ActivityItem {
        override val emoji = "✅"
        override val tint  = Color(0xFF22C55E)
        override val category = "tasks"
    }

    data class TaskAssigned(
        override val at: Long,
        override val title: String,
        override val subtitle: String,
        val taskId: String,
        val userId: String? = null,
        override val photoUrl: String? = null,
    ) : ActivityItem {
        override val emoji = "🆕"
        override val tint  = Color(0xFF3B82F6)
        override val category = "tasks"
    }

    data class TaskOverdue(
        override val at: Long,
        override val title: String,
        override val subtitle: String,
        val taskId: String,
        val userId: String? = null,
        override val photoUrl: String? = null,
    ) : ActivityItem {
        override val emoji = "⏰"
        override val tint  = Color(0xFFEF4444)
        override val category = "tasks"
    }

    data class EmployeeOnboarded(
        override val at: Long,
        override val title: String,
        override val subtitle: String,
        val employeeId: String,
        override val photoUrl: String? = null,
    ) : ActivityItem {
        override val emoji = "👤"
        override val tint  = Color(0xFF8B5CF6)
        override val category = "people"
    }

    data class CheckedIn(
        override val at: Long,
        override val title: String,
        override val subtitle: String,
        val attendanceId: String,
        val late: Boolean,
        val userId: String? = null,
        override val photoUrl: String? = null,
    ) : ActivityItem {
        override val emoji = if (late) "🟠" else "🟢"
        override val tint  = if (late) Color(0xFFF59E0B) else Color(0xFF10B981)
        override val category = "attendance"
    }

    data class CheckedOut(
        override val at: Long,
        override val title: String,
        override val subtitle: String,
        val attendanceId: String,
        val userId: String? = null,
        override val photoUrl: String? = null,
    ) : ActivityItem {
        override val emoji = "🔵"
        override val tint  = Color(0xFF0EA5E9)
        override val category = "attendance"
    }

    data class LowStockAlert(
        override val at: Long,
        override val title: String,
        override val subtitle: String,
        val spareItem: SpareItem,
    ) : ActivityItem {
        override val emoji = "⚠️"
        override val tint  = Color(0xFFEF4444)
        override val category = "inventory"
    }

    /**
     * Smart-aggregation: collapses 3+ similar consecutive events that occurred
     * within a short window into a single summary row. Tap the chevron in
     * [ActivityFeedRow] to navigate to the most-recent underlying [child].
     */
    data class Aggregated(
        override val at: Long,
        override val title: String,
        override val subtitle: String,
        override val tint: Color,
        override val emoji: String,
        override val category: String,
        val children: List<ActivityItem>,
    ) : ActivityItem
}

/** Top-of-feed filter chip values. */
enum class FeedFilter(val label: String, val key: String) {
    All("All", "all"),
    Tasks("Tasks", "tasks"),
    People("People", "people"),
    Attendance("Attendance", "attendance"),
    Inventory("Inventory", "inventory"),
}

/** Apply [filter] to a feed list. [FeedFilter.All] is a no-op. */
fun List<ActivityItem>.applyFilter(filter: FeedFilter): List<ActivityItem> {
    if (filter == FeedFilter.All) return this
    val key = filter.key
    return this.filter { it.category == key }
}

/* ──────────────────────────────────────────────────────────────────────────
 *  Feed builder — pure, easy to unit-test
 * ────────────────────────────────────────────────────────────────────────── */

private const val LOW_STOCK_THRESHOLD     = 20
private const val OVERDUE_GRACE_MS         = 0L
private const val MAX_FEED_ITEMS           = 25
private const val AGG_WINDOW_MS            = 10L * 60L * 1000L     // 10 minutes
private const val AGG_MIN_COUNT            = 3
const val FRESH_INDICATOR_WINDOW_MS        = 60L * 1000L           // 60 seconds
private const val MISSED_CHECKOUT_AFTER_MS = 12L * 60L * 60L * 1000L

/**
 * Merges multiple Firestore-derived lists into a single chronologically sorted
 * activity feed. Caller passes the latest snapshot of each collection plus
 * `now` (for the overdue calculation). Result is newest-first and capped at
 * [MAX_FEED_ITEMS]. Smart-aggregates 3+ consecutive same-type events that
 * occurred within [AGG_WINDOW_MS] of each other.
 */
fun buildActivityFeed(
    tasks: List<TaskRecord>,
    employees: List<EmployeeRecord>,
    attendance: List<AttendanceRecord>,
    spares: List<SpareItem>,
    now: Long,
): List<ActivityItem> {
    val out = ArrayList<ActivityItem>(64)
    val empById: Map<String, EmployeeRecord> = employees.associateBy { it.id }

    fun photo(uid: String?): String? =
        empById[uid.orEmpty()]?.photoUrl?.takeIf { it.isNotBlank() }

    // ── Tasks ──
    for (t in tasks) {
        val uid       = t.userId.orEmpty().takeIf { it.isNotBlank() }
        val assignee  = empById[uid.orEmpty()]?.name ?: "Unassigned"
        val photoUrl  = photo(uid)
        when {
            t.status == "Done" && t.completedAt != null -> {
                out += ActivityItem.TaskCompleted(
                    at       = t.completedAt!!,
                    title    = "Task completed",
                    subtitle = "$assignee → ${t.title}",
                    taskId   = t.id,
                    userId   = uid,
                    photoUrl = photoUrl,
                )
            }

            t.dueDate != null && t.dueDate!! < now - OVERDUE_GRACE_MS && t.status != "Done" -> {
                out += ActivityItem.TaskOverdue(
                    at       = t.dueDate!!,
                    title    = "Task overdue",
                    subtitle = "${t.title} · $assignee",
                    taskId   = t.id,
                    userId   = uid,
                    photoUrl = photoUrl,
                )
            }

            t.scheduledDateMs != null -> {
                out += ActivityItem.TaskAssigned(
                    at       = t.scheduledDateMs!!,
                    title    = "Task assigned",
                    subtitle = "${t.title} → $assignee",
                    taskId   = t.id,
                    userId   = uid,
                    photoUrl = photoUrl,
                )
            }
        }
    }

    // ── Employees ──
    for (e in employees) {
        val joining = e.joiningDateMs ?: e.updatedAt ?: continue
        out += ActivityItem.EmployeeOnboarded(
            at         = joining,
            title      = "Employee onboarded",
            subtitle   = "${e.name} · ${e.role}",
            employeeId = e.id,
            photoUrl   = e.photoUrl.takeIf { it.isNotBlank() },
        )
    }

    // ── Attendance ──
    for (a in attendance) {
        val emp      = empById[a.userId]?.name ?: "Employee"
        val photoUrl = photo(a.userId)
        out += ActivityItem.CheckedIn(
            at           = a.checkInMs,
            title        = if (a.checkInStatus == "LATE") "Late check-in" else "Checked in",
            subtitle     = emp,
            attendanceId = a.id,
            late         = a.checkInStatus == "LATE",
            userId       = a.userId,
            photoUrl     = photoUrl,
        )
        if (a.checkOutMs != null) {
            out += ActivityItem.CheckedOut(
                at           = a.checkOutMs!!,
                title        = "Checked out",
                subtitle     = emp,
                attendanceId = a.id,
                userId       = a.userId,
                photoUrl     = photoUrl,
            )
        }
    }

    // ── Low-stock alerts (treat as "happening now") ──
    val lowStockAt = now
    for (s in spares.filter { it.stockQty in 0..LOW_STOCK_THRESHOLD }) {
        out += ActivityItem.LowStockAlert(
            at        = lowStockAt,
            title     = if (s.stockQty == 0) "Out of stock" else "Low stock",
            subtitle  = "${s.name} · ${s.stockQty} left",
            spareItem = s,
        )
    }

    val sorted = out.sortedByDescending { it.at }.take(MAX_FEED_ITEMS)
    return aggregateRuns(sorted)
}

/**
 * Collapse runs of ≥[AGG_MIN_COUNT] consecutive same-category items whose
 * timestamps fall within [AGG_WINDOW_MS] of each other into a single
 * [ActivityItem.Aggregated] summary row.
 */
private fun aggregateRuns(items: List<ActivityItem>): List<ActivityItem> {
    if (items.size < AGG_MIN_COUNT) return items
    val out = ArrayList<ActivityItem>(items.size)
    var i = 0
    while (i < items.size) {
        val head = items[i]
        var j = i + 1
        // Walk forward while same category AND within window of head.
        while (j < items.size &&
            items[j].category == head.category &&
            head.at - items[j].at <= AGG_WINDOW_MS &&
            items[j] !is ActivityItem.Aggregated
        ) {
            j++
        }
        val run = items.subList(i, j)
        if (run.size >= AGG_MIN_COUNT && run.first() !is ActivityItem.Aggregated) {
            out += ActivityItem.Aggregated(
                at       = head.at,
                title    = "${run.size} ${categoryLabel(head.category)} in the last ${minutesBetween(run.last().at, head.at)} min",
                subtitle = "Tap to expand",
                tint     = head.tint,
                emoji    = head.emoji,
                category = head.category,
                children = run.toList(),
            )
        } else {
            out += run
        }
        i = j
    }
    return out
}

private fun categoryLabel(key: String): String = when (key) {
    "tasks"      -> "task events"
    "people"     -> "team updates"
    "attendance" -> "attendance events"
    "inventory"  -> "inventory alerts"
    else         -> "events"
}

private fun minutesBetween(a: Long, b: Long): Long {
    val diff = kotlin.math.abs(b - a) / 1000L / 60L
    return diff.coerceAtLeast(1L)
}

/* ──────────────────────────────────────────────────────────────────────────
 *  Relative-time formatter — KMP-friendly (Long math only)
 * ────────────────────────────────────────────────────────────────────────── */

fun formatRelative(now: Long, at: Long): String {
    val diff = now - at
    if (diff < 0L) return "soon"
    val sec = diff / 1000L
    val min = sec / 60L
    val hr  = min / 60L
    val day = hr / 24L
    return when {
        sec < 30L  -> "just now"
        min < 1L   -> "${sec}s ago"
        min < 60L  -> "${min}m ago"
        hr  < 24L  -> "${hr}h ago"
        day < 7L   -> "${day}d ago"
        day < 30L  -> "${day / 7L}w ago"
        day < 365L -> "${day / 30L}mo ago"
        else       -> "${day / 365L}y ago"
    }
}

/* ──────────────────────────────────────────────────────────────────────────
 *  Time-bucket grouping for sticky-style headers
 * ────────────────────────────────────────────────────────────────────────── */

enum class FeedBucket(val label: String) {
    Today("Today"),
    Yesterday("Yesterday"),
    ThisWeek("This week"),
    Earlier("Earlier"),
}

private const val DAY_MS = 24L * 60L * 60L * 1000L

fun bucketOf(now: Long, at: Long): FeedBucket {
    val diff = now - at
    return when {
        diff < DAY_MS         -> FeedBucket.Today
        diff < 2L * DAY_MS    -> FeedBucket.Yesterday
        diff < 7L * DAY_MS    -> FeedBucket.ThisWeek
        else                  -> FeedBucket.Earlier
    }
}

/* ──────────────────────────────────────────────────────────────────────────
 *  UI — premium activity-feed card
 * ────────────────────────────────────────────────────────────────────────── */

@Composable
fun ActivityFeedCard(
    items: List<ActivityItem>,
    now: Long,
    onItemClick: (ActivityItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (items.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(Color.White)
                .padding(20.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text     = "Nothing happening yet — activity will appear here.",
                color    = Color(0xFF94A3B8),
                fontSize = 13.sp,
            )
        }
        return
    }

    // Group consecutively by bucket so we render at most a single header per bucket.
    val grouped: List<Pair<FeedBucket, List<ActivityItem>>> = buildList {
        var current: FeedBucket? = null
        var bucketItems = mutableListOf<ActivityItem>()
        items.forEach { item ->
            val b = bucketOf(now, item.at)
            if (b != current) {
                val prev = current
                if (prev != null && bucketItems.isNotEmpty()) add(prev to bucketItems)
                current = b
                bucketItems = mutableListOf()
            }
            bucketItems.add(item)
        }
        val finalBucket = current
        if (finalBucket != null && bucketItems.isNotEmpty()) add(finalBucket to bucketItems)
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        grouped.forEach { (bucket, bucketItems) ->
            FeedBucketHeader(label = bucket.label, count = bucketItems.size)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.White)
                    .padding(horizontal = 4.dp, vertical = 4.dp),
            ) {
                Column {
                    bucketItems.forEachIndexed { idx, item ->
                        ActivityFeedRow(
                            item    = item,
                            now     = now,
                            onClick = { onItemClick(item) },
                        )
                        if (idx != bucketItems.lastIndex) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 56.dp, end = 16.dp)
                                    .height(1.dp)
                                    .background(Color(0xFFEEF2F7)),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FeedBucketHeader(label: String, count: Int) {
    Row(
        modifier = Modifier.padding(start = 4.dp, top = 6.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text       = label,
            color      = Color(0xFF475569),
            fontSize   = 11.sp,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .clip(CircleShape)
                .background(Color(0xFFE2E8F0))
                .padding(horizontal = 7.dp, vertical = 1.dp),
        ) {
            Text(
                text       = count.toString(),
                color      = Color(0xFF334155),
                fontSize   = 9.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun ActivityFeedRow(
    item: ActivityItem,
    now: Long,
    onClick: () -> Unit,
) {
    val isFresh = (now - item.at) in 0..FRESH_INDICATOR_WINDOW_MS
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Box(modifier = Modifier.size(34.dp)) {
            // Avatar — photo if present, else colored emoji bubble.
            val photo = item.photoUrl
            if (!photo.isNullOrBlank()) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(item.tint.copy(alpha = 0.14f))
                        .border(1.5.dp, item.tint.copy(alpha = 0.35f), CircleShape),
                ) {
                    coil3.compose.AsyncImage(
                        model = photo,
                        contentDescription = null,
                        modifier = Modifier.size(32.dp).clip(CircleShape),
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(item.tint.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center,
                ) { Text(item.emoji, fontSize = 14.sp) }
            }
            if (isFresh) {
                FreshPulseDot(
                    tint = Color(0xFF10B981),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(10.dp),
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text       = item.title,
                color      = Color(0xFF0F172A),
                fontSize   = 13.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines   = 1,
            )
            Text(
                text     = item.subtitle,
                color    = Color(0xFF64748B),
                fontSize = 11.sp,
                maxLines = 1,
            )
        }
        Spacer(Modifier.width(8.dp))
        Text(
            text       = if (isFresh) "now" else formatRelative(now, item.at),
            color      = if (isFresh) Color(0xFF10B981) else Color(0xFF94A3B8),
            fontSize   = 10.sp,
            fontWeight = if (isFresh) FontWeight.Bold else FontWeight.Medium,
        )
    }
}

@Composable
private fun FreshPulseDot(tint: Color, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "fresh")
    val s by transition.animateFloat(
        initialValue = 0.7f,
        targetValue  = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "fresh-scale",
    )
    Box(
        modifier = modifier
            .scale(s)
            .clip(CircleShape)
            .background(tint)
            .border(2.dp, Color.White, CircleShape),
    )
}

/* ──────────────────────────────────────────────────────────────────────────
 *  #6 — Pinned "Needs Attention" strip (overdue / high-prio / stock / no-checkout)
 * ────────────────────────────────────────────────────────────────────────── */

sealed interface NeedsAttentionItem {
    val emoji: String
    val title: String
    val subtitle: String
    val tint: Color
    val bg: Color

    data class Overdue(
        override val title: String,
        override val subtitle: String,
        val taskId: String,
    ) : NeedsAttentionItem {
        override val emoji = "⏰"
        override val tint  = Color(0xFFEF4444)
        override val bg    = Color(0xFFFEE2E2)
    }
    data class HighPriorityTodo(
        override val title: String,
        override val subtitle: String,
        val taskId: String,
    ) : NeedsAttentionItem {
        override val emoji = "🟠"
        override val tint  = Color(0xFFF59E0B)
        override val bg    = Color(0xFFFEF3C7)
    }
    data class OutOfStock(
        override val title: String,
        override val subtitle: String,
        val spareItem: SpareItem,
    ) : NeedsAttentionItem {
        override val emoji = "📦"
        override val tint  = Color(0xFFEF4444)
        override val bg    = Color(0xFFFEE2E2)
    }
    data class MissedCheckout(
        override val title: String,
        override val subtitle: String,
        val attendanceId: String,
        val userId: String,
    ) : NeedsAttentionItem {
        override val emoji = "🚪"
        override val tint  = Color(0xFF8B5CF6)
        override val bg    = Color(0xFFEDE9FE)
    }
}

fun buildNeedsAttention(
    tasks: List<TaskRecord>,
    employees: List<EmployeeRecord>,
    attendance: List<AttendanceRecord>,
    spares: List<SpareItem>,
    now: Long,
): List<NeedsAttentionItem> {
    val out = ArrayList<NeedsAttentionItem>()
    val empById = employees.associateBy { it.id }

    // Overdue tasks.
    tasks.filter {
        it.dueDate != null && it.dueDate!! < now && it.status != "Done"
    }.sortedBy { it.dueDate }.forEach { t ->
        val who = empById[t.userId.orEmpty()]?.name ?: "Unassigned"
        out += NeedsAttentionItem.Overdue(
            title    = t.title,
            subtitle = "Overdue · $who",
            taskId   = t.id,
        )
    }

    // High-priority todos (not yet started).
    tasks.filter { it.priority == "High" && it.status == "Todo" }.forEach { t ->
        val who = empById[t.userId.orEmpty()]?.name ?: "Unassigned"
        out += NeedsAttentionItem.HighPriorityTodo(
            title    = t.title,
            subtitle = "High priority · $who",
            taskId   = t.id,
        )
    }

    // Zero-stock spare items.
    spares.filter { it.stockQty == 0 }.forEach { s ->
        out += NeedsAttentionItem.OutOfStock(
            title    = s.name,
            subtitle = "Out of stock · ${s.category.ifBlank { "—" }}",
            spareItem = s,
        )
    }

    // Forgot to check out (>12h since check-in, no check-out).
    attendance.filter {
        it.checkOutMs == null && (now - it.checkInMs) > MISSED_CHECKOUT_AFTER_MS
    }.forEach { a ->
        val name = empById[a.userId]?.name ?: "Employee"
        out += NeedsAttentionItem.MissedCheckout(
            title    = name,
            subtitle = "Missed check-out · ${formatRelative(now, a.checkInMs)}",
            attendanceId = a.id,
            userId   = a.userId,
        )
    }

    return out
}

@Composable
fun NeedsAttentionStrip(
    items: List<NeedsAttentionItem>,
    onClick: (NeedsAttentionItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (items.isEmpty()) return
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Needs attention", color = Color(0xFF0F172A),
                 fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Color(0xFFFEE2E2))
                    .padding(horizontal = 8.dp, vertical = 2.dp),
            ) {
                Text("${items.size}", color = Color(0xFFB91C1C),
                     fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items.forEach { item ->
                NeedsAttentionCard(item = item, onClick = { onClick(item) })
            }
        }
    }
}

@Composable
private fun NeedsAttentionCard(item: NeedsAttentionItem, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White)
            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(item.bg),
            contentAlignment = Alignment.Center,
        ) { Text(item.emoji, fontSize = 14.sp) }
        Spacer(Modifier.width(10.dp))
        Column {
            Text(item.title, color = Color(0xFF0F172A),
                 fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                 maxLines = 1)
            Text(item.subtitle, color = item.tint,
                 fontSize = 10.sp, fontWeight = FontWeight.Medium,
                 maxLines = 1)
        }
    }
}

/* ──────────────────────────────────────────────────────────────────────────
 *  #7 — Filter chip strip
 * ────────────────────────────────────────────────────────────────────────── */

@Composable
fun FeedFilterChips(
    selected: FeedFilter,
    onSelect: (FeedFilter) -> Unit,
    counts: Map<String, Int> = emptyMap(),
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        FeedFilter.entries.forEach { f ->
            val isSelected = f == selected
            val count = if (f == FeedFilter.All) counts.values.sum() else counts[f.key] ?: 0
            Row(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(
                        if (isSelected) Color(0xFF0F172A) else Color.White,
                    )
                    .border(
                        1.dp,
                        if (isSelected) Color(0xFF0F172A) else Color(0xFFE2E8F0),
                        CircleShape,
                    )
                    .clickable { onSelect(f) }
                    .padding(horizontal = 12.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    f.label,
                    color = if (isSelected) Color.White else Color(0xFF334155),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                if (count > 0) {
                    Spacer(Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(
                                if (isSelected) Color.White.copy(alpha = 0.20f)
                                else Color(0xFFEEF2F7),
                            )
                            .padding(horizontal = 6.dp, vertical = 1.dp),
                    ) {
                        Text(
                            count.toString(),
                            color = if (isSelected) Color.White else Color(0xFF475569),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }
    }
}

/* ──────────────────────────────────────────────────────────────────────────
 *  #11 — "Your Day" hero card
 * ────────────────────────────────────────────────────────────────────────── */

data class YourDayStats(
    val activeTasks: Int,
    val dueToday: Int,
    val absentToday: Int,
    val absentName: String?,
    val highPriorityPending: Int,
    val lowStockCount: Int,
)

fun buildYourDay(
    tasks: List<TaskRecord>,
    employees: List<EmployeeRecord>,
    attendance: List<AttendanceRecord>,
    spares: List<SpareItem>,
    now: Long,
): YourDayStats {
    val dayStart = now - (now % DAY_MS)
    val dayEnd   = dayStart + DAY_MS
    val todaysAttendance = attendance.filter { it.checkInMs in dayStart..dayEnd }
    val checkedInIds     = todaysAttendance.map { it.userId }.toSet()
    val absent           = employees.filter { it.status == "Active" && it.id !in checkedInIds }
    return YourDayStats(
        activeTasks         = tasks.count { it.status != "Done" },
        dueToday            = tasks.count { it.status != "Done" && it.dueDate != null && it.dueDate!! in dayStart..dayEnd },
        absentToday         = absent.size,
        absentName          = absent.firstOrNull()?.name,
        highPriorityPending = tasks.count { it.priority == "High" && it.status != "Done" },
        lowStockCount       = spares.count { it.stockQty in 0..LOW_STOCK_THRESHOLD },
    )
}

@Composable
fun YourDayCard(stats: YourDayStats, modifier: Modifier = Modifier) {
    val rows = buildList {
        if (stats.activeTasks > 0) add("🟢" to "${stats.activeTasks} tasks active · ${stats.dueToday} due today")
        if (stats.absentToday > 0) {
            val lbl = stats.absentName?.let { "$it" } ?: "${stats.absentToday} employees"
            add("🔴" to "$lbl absent today")
        }
        if (stats.highPriorityPending > 0) add("🟠" to "${stats.highPriorityPending} high-priority tasks pending")
        if (stats.lowStockCount > 0) add("📦" to "${stats.lowStockCount} spares below threshold")
    }
    if (rows.isEmpty()) return
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White)
            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(20.dp))
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text("Your day", color = Color(0xFF0F172A),
             fontSize = 14.sp, fontWeight = FontWeight.Bold)
        rows.forEach { (emoji, line) ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(emoji, fontSize = 14.sp)
                Spacer(Modifier.width(8.dp))
                Text(line, color = Color(0xFF334155), fontSize = 12.sp,
                     fontWeight = FontWeight.Medium)
            }
        }
    }
}

/* ──────────────────────────────────────────────────────────────────────────
 *  #12 — Top performer / Bottleneck callout
 * ────────────────────────────────────────────────────────────────────────── */

data class PerformanceInsight(
    val topPerformerName: String?,
    val topPerformerCount: Int,
    val bottleneckName: String?,
    val bottleneckCount: Int,
)

fun buildPerformanceInsight(
    tasks: List<TaskRecord>,
    employees: List<EmployeeRecord>,
    now: Long,
): PerformanceInsight {
    val empById = employees.associateBy { it.id }
    val weekStart = now - 7L * DAY_MS

    val topByUser = tasks
        .filter { it.status == "Done" && (it.completedAt ?: 0L) >= weekStart && !it.userId.isNullOrBlank() }
        .groupingBy { it.userId!! }
        .eachCount()
        .maxByOrNull { it.value }
    val overdueByUser = tasks
        .filter { it.status != "Done" && it.dueDate != null && it.dueDate!! < now && !it.userId.isNullOrBlank() }
        .groupingBy { it.userId!! }
        .eachCount()
        .maxByOrNull { it.value }

    return PerformanceInsight(
        topPerformerName  = topByUser?.let { empById[it.key]?.name },
        topPerformerCount = topByUser?.value ?: 0,
        bottleneckName    = overdueByUser?.let { empById[it.key]?.name },
        bottleneckCount   = overdueByUser?.value ?: 0,
    )
}

@Composable
fun PerformanceInsightCard(insight: PerformanceInsight, modifier: Modifier = Modifier) {
    if (insight.topPerformerName == null && insight.bottleneckName == null) return
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Color(0xFFF8FAFC))
            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(18.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        insight.topPerformerName?.let { name ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(28.dp).clip(CircleShape)
                        .background(Color(0xFFDCFCE7)),
                    contentAlignment = Alignment.Center,
                ) { Text("🏆", fontSize = 13.sp) }
                Spacer(Modifier.width(10.dp))
                Column {
                    Text("Top performer", color = Color(0xFF15803D),
                         fontSize = 10.sp, fontWeight = FontWeight.Bold,
                         letterSpacing = 0.6.sp)
                    Text("$name closed ${insight.topPerformerCount} tasks this week",
                         color = Color(0xFF0F172A),
                         fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
        insight.bottleneckName?.let { name ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(28.dp).clip(CircleShape)
                        .background(Color(0xFFFEE2E2)),
                    contentAlignment = Alignment.Center,
                ) { Text("⚠️", fontSize = 13.sp) }
                Spacer(Modifier.width(10.dp))
                Column {
                    Text("Bottleneck", color = Color(0xFFB91C1C),
                         fontSize = 10.sp, fontWeight = FontWeight.Bold,
                         letterSpacing = 0.6.sp)
                    Text("$name has ${insight.bottleneckCount} overdue tasks",
                         color = Color(0xFF0F172A),
                         fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

/* ──────────────────────────────────────────────────────────────────────────
 *  #13 — Sparkline chip (last-7-day completed tasks)
 * ────────────────────────────────────────────────────────────────────────── */

/** Returns 7 ints representing daily counts of `completedAt` events ending today. */
fun sparkline7Day(tasks: List<TaskRecord>, now: Long): List<Int> {
    val dayStart = now - (now % DAY_MS)
    val buckets = IntArray(7)
    tasks.forEach { t ->
        val ts = t.completedAt ?: return@forEach
        val daysAgo = ((dayStart - (ts - (ts % DAY_MS))) / DAY_MS).toInt()
        if (daysAgo in 0..6) buckets[6 - daysAgo]++  // newest = right edge
    }
    return buckets.toList()
}

@Composable
fun Sparkline(
    points: List<Int>,
    tint: Color = Color(0xFF3B82F6),
    width: Dp = 64.dp,
    height: Dp = 18.dp,
    modifier: Modifier = Modifier,
) {
    if (points.isEmpty()) return
    val maxV = (points.max()).coerceAtLeast(1)
    Canvas(modifier = modifier.size(width = width, height = height)) {
        val w = size.width
        val h = size.height
        if (points.size == 1) return@Canvas
        val stepX = w / (points.size - 1).coerceAtLeast(1)
        val path = Path()
        points.forEachIndexed { i, v ->
            val x = stepX * i
            val y = h - (v.toFloat() / maxV) * h
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        // Filled area under the line for premium look.
        val areaPath = Path().apply {
            addPath(path)
            lineTo(w, h)
            lineTo(0f, h)
            close()
        }
        drawPath(areaPath, color = tint.copy(alpha = 0.15f))
        drawPath(
            path  = path,
            color = tint,
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx()),
        )
        // Last-point dot
        val lastX = stepX * (points.size - 1)
        val lastY = h - (points.last().toFloat() / maxV) * h
        drawCircle(color = tint, radius = 2.5.dp.toPx(), center = Offset(lastX, lastY))
    }
}

