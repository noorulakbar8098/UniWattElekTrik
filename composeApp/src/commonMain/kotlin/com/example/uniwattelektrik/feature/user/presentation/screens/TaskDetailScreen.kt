package com.example.uniwattelektrik.feature.user.presentation.screens

import com.example.uniwattelektrik.core.theme.appScreenBackground

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.uniwattelektrik.core.components.EmptyState
import com.example.uniwattelektrik.core.sample.SampleTasks
import com.example.uniwattelektrik.feature.workforce.data.remote.TaskRecord
import com.example.uniwattelektrik.feature.workforce.presentation.WorkforceViewModel
import kotlin.math.absoluteValue

/* ── Design tokens — matched 1:1 with AdminEmployeeDetailScreen ────────── */
private val ScreenBg     = Color(0xFFF4F7FB)
private val CardBg       = Color(0xFFFFFFFF)
private val InkPrimary   = Color(0xFF1A2B49)
private val InkSecondary = Color(0xFF6B7A99)
private val InkMuted     = Color(0xFF94A3B8)
private val Brand        = Color(0xFF3B82F6)
private val BrandDeep    = Color(0xFF1D4ED8)
private val BrandDark    = Color(0xFF0F172A)
private val Brand50      = Color(0xFFE6F0FE)
private val Success      = Color(0xFF22C55E)
private val SuccessBg    = Color(0xFFDCFCE7)
private val Warning      = Color(0xFFF59E0B)
private val WarningBg    = Color(0xFFFEF3C7)
private val Danger       = Color(0xFFEF4444)
private val DangerBg     = Color(0xFFFEE2E2)
private val Purple       = Color(0xFF8B5CF6)
private val ShadowSoft   = Color(0x14172C50)
private val DividerSoft  = Color(0xFFE2E8F0)

/**
 * Premium task detail screen — visual structure matches
 * [com.example.uniwattelektrik.feature.admin.presentation.screens.AdminEmployeeDetailScreen]
 * (gradient header, floating stats card, section-cards with accent stripe).
 */
@Composable
fun TaskDetailScreen(
    taskId: String,
    onBack: () -> Unit,
    onStartWork: (taskId: String) -> Unit,
    modifier: Modifier = Modifier,
    workforceVm: WorkforceViewModel? = null,
) {
    val liveTasks by (workforceVm?.tasks?.collectAsStateWithLifecycle()
        ?: remember { kotlinx.coroutines.flow.MutableStateFlow(emptyList<TaskRecord>()) }
            .collectAsStateWithLifecycle())
    val live = liveTasks.firstOrNull { it.id == taskId }
    val sample = SampleTasks.byId(taskId)
    val task = live?.let(::sampleFromRecord) ?: sample

    com.example.uniwattelektrik.core.theme.SetStatusBar(color = Brand, darkIcons = false)

    if (task == null) {
        EmptyState(emoji = "❓", title = "Task not found",
                   body = "It may have been reassigned or removed.")
        return
    }

    val checklist = remember {
        mutableStateListOf(
            ChecklistItem("Inspect transformer", true),
            ChecklistItem("Replace fuse", true),
            ChecklistItem("Test output voltage", false),
            ChecklistItem("Submit completion report", false),
        )
    }
    val notes = remember {
        mutableStateListOf(
            NoteItem("Priya S.", "PS", Purple, "10:42 AM",
                "Confirmed the fault location. Bringing replacement gaskets."),
            NoteItem("Ravi K.", "RK", Color(0xFFEC8552), "10:55 AM",
                "On site. Power isolated. Starting inspection."),
        )
    }
    var newNote by remember { mutableStateOf("") }
    var descExpanded by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier.fillMaxSize().background(appScreenBackground()),
        contentPadding = PaddingValues(bottom = 110.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        // Gradient header
        item { TaskHeader(task = task, onBack = onBack) }

        // Floating stats card (overlaps header)
        item {
            Box(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .offset(y = (-32).dp),
            ) {
                StatsCard(task = task)
            }
        }

        // Description card
        item {
            Box(modifier = Modifier
                .padding(horizontal = 16.dp)
                .offset(y = (-16).dp)) {
                SectionCard(title = "Description") {
                    val full = task.description
                    val short = if (full.length > 140) full.take(140) + "…" else full
                    Text(
                        if (descExpanded) full else short,
                        color = InkPrimary,
                        fontSize = 14.sp,
                        lineHeight = 22.sp,
                    )
                    if (full.length > 140) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            if (descExpanded) "Show less" else "Read more",
                            color = Brand,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.clickable { descExpanded = !descExpanded },
                        )
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }

        // Activity timeline card
        item {
            Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                SectionCard(title = "Activity") {
                    Spacer(Modifier.height(4.dp))
                    TimelineRow(
                        tint = InkMuted, time = "Yesterday · 17:42",
                        title = "Task created",
                        note = "Created by Operations Admin",
                        isFirst = true, isLast = false,
                    )
                    TimelineRow(
                        tint = Brand, time = "Today · 09:10",
                        title = "Assigned to ${task.assigneeInitials}",
                        note = "Auto-assigned by load balance",
                        isFirst = false, isLast = false,
                    )
                    TimelineRow(
                        tint = Warning, time = "Today · 10:30",
                        title = "In progress",
                        note = "Marked on-site, geo-fence verified",
                        isFirst = false, isLast = false,
                        active = true,
                    )
                    TimelineRow(
                        tint = InkMuted, time = "—",
                        title = "Completion",
                        note = "Awaiting field sign-off",
                        isFirst = false, isLast = true,
                        faded = true,
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
        }

        // Checklist card
        item {
            Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                SectionCard(
                    title = "Checklist",
                    trailingAction = "${checklist.count { it.done }} / ${checklist.size}",
                ) {
                    checklist.forEachIndexed { i, item ->
                        ChecklistRow(
                            item = item,
                            onToggle = { checklist[i] = item.copy(done = !item.done) },
                        )
                        if (i != checklist.lastIndex) Spacer(Modifier.height(4.dp))
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }

        // Attachments card
        item {
            Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                SectionCard(title = "Attachments", trailingAction = "3 files") {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        AttachmentTile(tint = Color(0xFF8FB3FF), modifier = Modifier.weight(1f))
                        AttachmentTile(tint = Color(0xFFFFB28A), modifier = Modifier.weight(1f))
                        AttachmentTile(tint = Color(0xFF86EFAC), modifier = Modifier.weight(1f))
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }

        // Notes card
        item {
            Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                SectionCard(title = "Notes") {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        notes.forEach { NoteRow(it) }
                        ComposerRow(
                            value = newNote,
                            onChange = { newNote = it },
                            onSend = {
                                if (newNote.isNotBlank()) {
                                    notes.add(
                                        NoteItem("You", "YO", Brand, "Now", newNote.trim()),
                                    )
                                    newNote = ""
                                }
                            },
                        )
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }

        // Location card
        item {
            Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                SectionCard(title = "Location", trailingAction = "Open in Maps") {
                    Row(verticalAlignment = Alignment.Top) {
                        Box(
                            modifier = Modifier
                                .size(40.dp).clip(RoundedCornerShape(12.dp))
                                .background(Brand50),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(Icons.Filled.LocationOn, null,
                                 tint = Brand, modifier = Modifier.size(20.dp))
                        }
                        Spacer(Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("ADDRESS", color = InkSecondary, fontSize = 10.sp,
                                 fontWeight = FontWeight.SemiBold, letterSpacing = 0.8.sp)
                            Spacer(Modifier.height(2.dp))
                            Text(task.location, color = InkPrimary,
                                 fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                        Icon(Icons.AutoMirrored.Filled.OpenInNew, null,
                             tint = Brand, modifier = Modifier.size(20.dp))
                    }

                    Spacer(Modifier.height(12.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth().height(140.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFFE8EFFA)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.SpaceBetween,
                        ) {
                            repeat(5) {
                                Box(modifier = Modifier
                                    .fillMaxWidth().height(1.dp)
                                    .background(Color(0xFFCFDBED)))
                            }
                        }
                        Box(
                            modifier = Modifier
                                .size(40.dp).clip(CircleShape)
                                .background(Brand)
                                .border(3.dp, Color.White, CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(Icons.Filled.LocationOn, null,
                                 tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
            Spacer(Modifier.height(20.dp))
        }

        // Bottom action row (3 cards)
        item {
            Row(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                BottomAction(
                    icon = Icons.Filled.Add, label = "Add Note",
                    tint = Brand, bg = Brand50,
                    modifier = Modifier.weight(1f),
                )
                BottomAction(
                    icon = Icons.Filled.Edit, label = "Update",
                    tint = Warning, bg = WarningBg,
                    modifier = Modifier.weight(1f),
                )
                BottomAction(
                    icon = Icons.Filled.Check, label = "Complete",
                    tint = Success, bg = SuccessBg,
                    modifier = Modifier.weight(1f),
                    onClick = { onStartWork(task.id) },
                )
            }
        }
    }
}

/* ─────────────────────────────────────────────────────────────────────── *
 *  HEADER (mirrors AdminEmployeeDetailScreen)
 * ─────────────────────────────────────────────────────────────────────── */

@Composable
private fun TaskHeader(
    task: com.example.uniwattelektrik.core.sample.SampleTask,
    onBack: () -> Unit,
) {
    com.example.uniwattelektrik.core.components.PremiumHeaderBackground(
        roundedBottom = false,
    ) {
        Column(
            modifier = Modifier
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(horizontal = 18.dp)
                .padding(top = 14.dp, bottom = 60.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                com.example.uniwattelektrik.core.components.GlassBackButton(
                    onClick = onBack,
                )
                Spacer(Modifier.width(14.dp))
                Text(
                    "Task Details",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                )
                GlassButton(onClick = {}) {
                    Icon(
                        imageVector = Icons.Filled.MoreVert,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp),
                    )
                }
                Spacer(Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .shadow(8.dp, RoundedCornerShape(14.dp), spotColor = Color(0x33000000))
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color.White)
                        .clickable {},
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Filled.Edit,
                        contentDescription = null,
                        tint = Brand,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }

            Spacer(Modifier.height(22.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                /* Big task icon (bolt) — mirrors the avatar in employee detail */
                val iconGradient = priorityIconGradient(task.priority.label)
                Box(modifier = Modifier.size(108.dp)) {
                    Box(
                        modifier = Modifier
                            .size(108.dp)
                            .shadow(20.dp, RoundedCornerShape(28.dp),
                                    spotColor = Color(0x55000000))
                            .clip(RoundedCornerShape(28.dp))
                            .background(Brush.linearGradient(iconGradient)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Bolt,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(48.dp),
                        )
                    }
                    /* Status indicator — bottom-right (matches employee online dot) */
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .align(Alignment.BottomEnd)
                            .offset(x = (-4).dp, y = (-4).dp)
                            .clip(CircleShape)
                            .background(statusColor(task.status.label))
                            .border(3.dp, Color.White, CircleShape),
                    )
                }

                Spacer(Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        task.title,
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 26.sp,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Field operations · ${task.day} ${task.time}",
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )

                    Spacer(Modifier.height(10.dp))
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(Color.White.copy(alpha = 0.18f))
                            .border(1.dp, Color.White.copy(alpha = 0.25f),
                                    RoundedCornerShape(50))
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            "#${task.id}",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp,
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("•", color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            task.priority.label.uppercase(),
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GlassButton(onClick: () -> Unit, content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White.copy(alpha = 0.18f))
            .border(1.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
        content = { content() },
    )
}

/* ─────────────────────────────────────────────────────────────────────── *
 *  STATS CARD (mirrors employee stats card)
 * ─────────────────────────────────────────────────────────────────────── */

@Composable
private fun StatsCard(task: com.example.uniwattelektrik.core.sample.SampleTask) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .shadow(16.dp, RoundedCornerShape(20.dp), spotColor = ShadowSoft)
            .clip(RoundedCornerShape(20.dp))
            .background(CardBg)
            .padding(vertical = 16.dp),
    ) {
        StatItem(
            value = task.status.label.uppercase().take(6),
            label = "STATUS",
            valueColor = statusColor(task.status.label),
            modifier = Modifier.weight(1f),
        )
        VerticalDivider()
        StatItem(value = "42m", label = "SLA",
                 valueColor = Warning, modifier = Modifier.weight(1f))
        VerticalDivider()
        StatItem(value = task.time, label = "DUE",
                 valueColor = InkPrimary, modifier = Modifier.weight(1f))
        VerticalDivider()
        StatItem(value = task.assigneeInitials.uppercase(), label = "OWNER",
                 valueColor = InkPrimary, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun StatItem(
    value: String,
    label: String,
    valueColor: Color,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            value,
            color = valueColor,
            fontSize = if (value.length <= 4) 22.sp else 16.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            label,
            color = InkSecondary,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.6.sp,
        )
    }
}

@Composable
private fun VerticalDivider() {
    Box(
        modifier = Modifier
            .width(1.dp)
            .fillMaxHeight()
            .padding(vertical = 8.dp)
            .background(DividerSoft),
    )
}

/* ─────────────────────────────────────────────────────────────────────── *
 *  SECTION CARD (matches employee SectionCard with brand-blue accent stripe)
 * ─────────────────────────────────────────────────────────────────────── */

@Composable
private fun SectionCard(
    title: String? = null,
    trailingAction: String? = null,
    onTrailing: () -> Unit = {},
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(10.dp, RoundedCornerShape(20.dp), spotColor = ShadowSoft)
            .clip(RoundedCornerShape(20.dp))
            .background(CardBg)
            .padding(horizontal = 18.dp, vertical = 16.dp),
    ) {
        if (title != null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .height(16.dp).width(3.dp)
                        .clip(RoundedCornerShape(50))
                        .background(Brand),
                )
                Spacer(Modifier.width(8.dp))
                Text(title, color = InkPrimary, fontSize = 16.sp,
                     fontWeight = FontWeight.Bold,
                     modifier = Modifier.weight(1f))
                if (trailingAction != null) {
                    Text(
                        trailingAction,
                        color = Brand,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.clickable(onClick = onTrailing),
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
        }
        content()
    }
}

/* ─────────────────────────────────────────────────────────────────────── *
 *  TIMELINE
 * ─────────────────────────────────────────────────────────────────────── */

@Composable
private fun TimelineRow(
    tint: Color,
    time: String,
    title: String,
    note: String,
    isFirst: Boolean,
    isLast: Boolean,
    active: Boolean = false,
    faded: Boolean = false,
) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top,
            modifier = Modifier.width(28.dp),
        ) {
            Box(modifier = Modifier
                .height(8.dp).width(2.dp)
                .background(if (isFirst) Color.Transparent else DividerSoft))
            Box(
                modifier = Modifier
                    .size(if (active) 14.dp else 10.dp)
                    .clip(CircleShape)
                    .background(if (faded) Color(0xFFE2E8F0) else tint)
                    .then(
                        if (active) Modifier.border(3.dp, tint.copy(alpha = 0.25f), CircleShape)
                        else Modifier,
                    ),
            )
            Box(modifier = Modifier
                .height(58.dp).width(2.dp)
                .background(if (isLast) Color.Transparent else DividerSoft))
        }

        Spacer(Modifier.width(8.dp))

        Column(
            modifier = Modifier
                .padding(top = 4.dp, bottom = 18.dp)
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                time,
                color = if (faded) InkMuted else InkSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.3.sp,
            )
            Text(
                title,
                color = if (faded) InkMuted else InkPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(note, color = InkSecondary, fontSize = 12.sp, lineHeight = 18.sp)
        }
    }
}

/* ─────────────────────────────────────────────────────────────────────── *
 *  CHECKLIST · ATTACHMENTS · NOTES · BOTTOM ACTIONS
 * ─────────────────────────────────────────────────────────────────────── */

private data class ChecklistItem(val text: String, val done: Boolean)

@Composable
private fun ChecklistRow(item: ChecklistItem, onToggle: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onToggle)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(if (item.done) Success else Color.White)
                .border(
                    width = if (item.done) 0.dp else 1.5.dp,
                    color = if (item.done) Success else DividerSoft,
                    shape = RoundedCornerShape(6.dp),
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (item.done) {
                Icon(Icons.Filled.Check, null, tint = Color.White,
                     modifier = Modifier.size(14.dp))
            }
        }
        Spacer(Modifier.width(12.dp))
        Text(
            item.text,
            color = if (item.done) InkMuted else InkPrimary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            textDecoration = if (item.done) TextDecoration.LineThrough else TextDecoration.None,
        )
    }
}

@Composable
private fun AttachmentTile(tint: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .height(82.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(tint.copy(alpha = 0.20f))
            .clickable {},
        contentAlignment = Alignment.Center,
    ) {
        Icon(Icons.Filled.Image, null, tint = tint, modifier = Modifier.size(28.dp))
    }
}

private data class NoteItem(
    val author: String, val initials: String, val tint: Color,
    val timestamp: String, val message: String,
)

@Composable
private fun NoteRow(note: NoteItem) {
    Row {
        Box(
            modifier = Modifier
                .size(36.dp).clip(CircleShape).background(note.tint),
            contentAlignment = Alignment.Center,
        ) {
            Text(note.initials, color = Color.White,
                 fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(note.author, color = InkPrimary,
                     fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.width(8.dp))
                Text(note.timestamp, color = InkMuted,
                     fontSize = 11.sp, fontWeight = FontWeight.Medium)
            }
            Spacer(Modifier.height(2.dp))
            Text(note.message, color = InkSecondary, fontSize = 13.sp,
                 lineHeight = 19.sp)
        }
    }
}

@Composable
private fun ComposerRow(
    value: String,
    onChange: (String) -> Unit,
    onSend: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(ScreenBg)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = Modifier.weight(1f)) {
            if (value.isEmpty()) {
                Text("Add a note…", color = InkMuted, fontSize = 13.sp)
            }
            BasicTextField(
                value = value,
                onValueChange = onChange,
                textStyle = TextStyle(color = InkPrimary, fontSize = 13.sp),
                cursorBrush = SolidColor(Brand),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        Spacer(Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .size(34.dp).clip(CircleShape)
                .background(if (value.isBlank()) InkMuted.copy(alpha = 0.4f) else Brand)
                .clickable(enabled = value.isNotBlank(), onClick = onSend),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.Add, null, tint = Color.White, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun BottomAction(
    icon: ImageVector,
    label: String,
    tint: Color,
    bg: Color,
    modifier: Modifier = Modifier,
    labelColor: Color = InkPrimary,
    onClick: () -> Unit = {},
) {
    Column(
        modifier = modifier
            .height(78.dp)
            .shadow(8.dp, RoundedCornerShape(16.dp), spotColor = ShadowSoft)
            .clip(RoundedCornerShape(16.dp))
            .background(CardBg)
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(bg),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, null, tint = tint, modifier = Modifier.size(18.dp))
        }
        Text(label, color = labelColor, fontSize = 12.sp,
             fontWeight = FontWeight.SemiBold)
    }
}

/* ─────────────────────────────────────────────────────────────────────── *
 *  HELPERS
 * ─────────────────────────────────────────────────────────────────────── */

/** Adapts a real Firestore [TaskRecord] to the SampleTask shape used by this screen. */
private fun sampleFromRecord(t: TaskRecord): com.example.uniwattelektrik.core.sample.SampleTask {
    val priority = when (t.priority.lowercase()) {
        "high"   -> com.example.uniwattelektrik.core.sample.TaskPriority.High
        "low"    -> com.example.uniwattelektrik.core.sample.TaskPriority.Low
        else     -> com.example.uniwattelektrik.core.sample.TaskPriority.Med
    }
    val status = when (t.status) {
        "InProgress" -> com.example.uniwattelektrik.core.sample.TaskStatus.InProgress
        "Done"       -> com.example.uniwattelektrik.core.sample.TaskStatus.Done
        else         -> com.example.uniwattelektrik.core.sample.TaskStatus.Todo
    }
    val code = "TASK-${(t.id.hashCode().absoluteValue % 9000) + 1000}"
    return com.example.uniwattelektrik.core.sample.SampleTask(
        id = code,
        title = t.title,
        description = "Site work scheduled at ${t.location}. Follow standard SLA policy.",
        location = t.location.ifBlank { "—" },
        distanceKm = 0.0,
        time = t.time,
        day = t.day,
        priority = priority,
        status = status,
        assigneeInitials = t.assigneeInitials.ifBlank { "??" },
    )
}

private fun statusColor(status: String): Color = when (status.lowercase()) {
    "completed", "done"      -> Success
    "in progress", "active"  -> Brand
    "to do", "queued"        -> Color(0xFF94A3B8)
    else                      -> Brand
}

private fun priorityIconGradient(priority: String): List<Color> = when (priority.lowercase()) {
    "high"   -> listOf(Color(0xFFFB7185), Danger)
    "low"    -> listOf(Color(0xFF34D399), Success)
    else     -> listOf(Color(0xFFFBBF24), Warning)   // medium
}
