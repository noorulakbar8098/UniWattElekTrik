package com.example.uniwattelektrik.feature.user.presentation.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.uniwattelektrik.feature.workforce.data.remote.MaterialUsedItem
import com.example.uniwattelektrik.feature.workforce.data.remote.SpareItemRecord
import com.example.uniwattelektrik.feature.workforce.data.remote.TaskRecord
import com.example.uniwattelektrik.feature.workforce.presentation.WorkforceViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val ScreenBg     = Color(0xFFF4F7FB)
private val CardBg       = Color(0xFFFFFFFF)
private val InkPrimary   = Color(0xFF1A2B49)
private val InkSecondary = Color(0xFF6B7A99)
private val InkMuted     = Color(0xFF94A3B8)
private val Brand        = Color(0xFF3B82F6)
private val BrandDeep    = Color(0xFF1D4ED8)
private val Brand50      = Color(0xFFE6F0FE)
private val Success      = Color(0xFF22C55E)
private val SuccessBg    = Color(0xFFDCFCE7)
private val Danger       = Color(0xFFEF4444)
private val DividerSoft  = Color(0xFFE5EAF2)
private val ShadowSoft   = Color(0x14172C50)

private val RCA_OPTIONS = listOf(
    "Power Failure", "Equipment Fault", "Human Error",
    "Maintenance Due", "External Factor", "Other",
)

private data class SpareRow(val spare: SpareItemRecord, val qty: Int = 0)

@Composable
fun CompleteWorkScreen(
    taskId: String,
    adminId: String,
    userId: String,
    workforceVm: WorkforceViewModel,
    onClose: () -> Unit,
    onSubmitted: () -> Unit,
) {
    val allTasks  by workforceVm.tasks.collectAsStateWithLifecycle()
    val allSpares by workforceVm.spareItems.collectAsStateWithLifecycle()
    val task      = allTasks.firstOrNull { it.id == taskId }
    var step by remember { mutableIntStateOf(0) }
    val rows = remember(allSpares) {
        mutableStateListOf<SpareRow>().also { list -> allSpares.forEach { list.add(SpareRow(it, 0)) } }
    }
    var description by remember { mutableStateOf("") }
    var downtime    by remember { mutableStateOf("") }
    var rca         by remember { mutableStateOf("") }
    var submitting  by remember { mutableStateOf(false) }
    var submitError by remember { mutableStateOf<String?>(null) }
    val scope       = rememberCoroutineScope()
    val endTimeMs   = remember { System.currentTimeMillis() }

    AnimatedContent(
        targetState = step,
        transitionSpec = {
            if (targetState > initialState)
                (slideInHorizontally { it } + fadeIn()) togetherWith (slideOutHorizontally { -it } + fadeOut())
            else
                (slideInHorizontally { -it } + fadeIn()) togetherWith (slideOutHorizontally { it } + fadeOut())
        },
    ) { currentStep ->
        when (currentStep) {
            0 -> MaterialStep(task = task, rows = rows, onBack = onClose, onNext = { step = 1 })
            1 -> SignoffStep(
                task = task, description = description, downtime = downtime, rca = rca,
                submitting = submitting, error = submitError,
                onDescChange = { description = it },
                onDowntimeChange = { downtime = it },
                onRcaChange = { rca = it },
                onBack = { step = 0 },
                onSubmit = {
                    submitError = null
                    if (task == null) { submitError = "Task not found"; return@SignoffStep }
                    if (description.isBlank()) { submitError = "Please enter a work description"; return@SignoffStep }
                    if (rca.isBlank()) { submitError = "Please select an RCA option"; return@SignoffStep }
                    submitting = true
                    val used = rows.filter { it.qty > 0 }.map { MaterialUsedItem(it.spare.id, it.spare.name, it.qty) }
                    scope.launch {
                        workforceVm.completeTaskWithSignoff(
                            adminId = adminId, taskId = taskId, assignedUserId = userId,
                            signoffDescription = description, downtimeMinutes = downtime.toIntOrNull() ?: 0,
                            rca = rca, materialsUsed = used,
                            startTimeMs = task.acceptedAt ?: endTimeMs, endTimeMs = endTimeMs,
                        ) { result ->
                            submitting = false
                            result.fold(
                                onSuccess = { step = 2 },
                                onFailure = { submitError = it.message ?: "Submission failed" },
                            )
                        }
                    }
                },
            )
            else -> SuccessStep(onDone = onSubmitted)
        }
    }
}

@Composable
private fun MaterialStep(
    task: TaskRecord?,
    rows: MutableList<SpareRow>,
    onBack: () -> Unit,
    onNext: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize().background(ScreenBg).statusBarsPadding().navigationBarsPadding()) {
        StepHeader(title = "Materials Used", subtitle = "Step 1 of 2", taskTitle = task?.title, step = 0, onBack = onBack)
        if (rows.isEmpty()) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("\uD83D\uDCE6", fontSize = 48.sp)
                    Spacer(Modifier.height(12.dp))
                    Text("No spare items in inventory", fontSize = 15.sp, color = InkSecondary)
                    Text("Tap Next to continue", fontSize = 13.sp, color = InkMuted)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(rows, key = { it.spare.id }) { row ->
                    SpareRowCard(row = row, onQty = { newQty ->
                        val idx = rows.indexOf(row)
                        if (idx >= 0) rows[idx] = row.copy(qty = newQty.coerceAtLeast(0))
                    })
                }
            }
        }
        BottomCta(label = "Next: Sign Off \u2192", onClick = onNext)
    }
}

@Composable
private fun SpareRowCard(row: SpareRow, onQty: (Int) -> Unit) {
    val scaleAnim by animateFloatAsState(
        targetValue = if (row.qty > 0) 1.03f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
    )
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scaleAnim)
            .shadow(if (row.qty > 0) 6.dp else 2.dp, RoundedCornerShape(14.dp), ambientColor = ShadowSoft)
            .clip(RoundedCornerShape(14.dp))
            .background(CardBg)
            .then(if (row.qty > 0) Modifier.border(1.5.dp, Brand.copy(alpha = .4f), RoundedCornerShape(14.dp)) else Modifier)
            .padding(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(row.spare.name, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = InkPrimary)
                Spacer(Modifier.height(2.dp))
                val meta = buildString {
                    if (row.spare.category.isNotBlank()) append(row.spare.category)
                    if (row.spare.unit.isNotBlank()) {
                        if (isNotEmpty()) append(" \u00B7 ")
                        append(row.spare.unit)
                    }
                }
                if (meta.isNotBlank()) Text(meta, fontSize = 12.sp, color = InkMuted)
                Text(
                    "Stock: ${row.spare.stockQty}",
                    fontSize = 11.sp,
                    color = if (row.spare.stockQty > 0) Success else Danger,
                    fontWeight = FontWeight.Medium,
                )
            }
            Spacer(Modifier.width(12.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                StepperBtn(Icons.Default.Remove, enabled = row.qty > 0) { onQty(row.qty - 1) }
                Box(
                    modifier = Modifier.size(40.dp).background(if (row.qty > 0) Brand else Color(0xFFF1F5F9), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "${row.qty}",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (row.qty > 0) Color.White else InkSecondary,
                    )
                }
                StepperBtn(Icons.Default.Add, enabled = row.qty < row.spare.stockQty) { onQty(row.qty + 1) }
            }
        }
    }
}

@Composable
private fun StepperBtn(icon: ImageVector, enabled: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(34.dp)
            .clip(CircleShape)
            .background(if (enabled) Brand50 else Color(0xFFF1F5F9))
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = if (enabled) Brand else InkMuted, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun SignoffStep(
    task: TaskRecord?,
    description: String,
    downtime: String,
    rca: String,
    submitting: Boolean,
    error: String?,
    onDescChange: (String) -> Unit,
    onDowntimeChange: (String) -> Unit,
    onRcaChange: (String) -> Unit,
    onBack: () -> Unit,
    onSubmit: () -> Unit,
) {
    val durationText = remember(task?.acceptedAt) {
        val start = task?.acceptedAt ?: return@remember null
        val ms = System.currentTimeMillis() - start
        val h = ms / 3_600_000
        val m = (ms % 3_600_000) / 60_000
        buildString { if (h > 0) append("${h}h "); append("${m}m") }
    }
    Column(modifier = Modifier.fillMaxSize().background(ScreenBg).statusBarsPadding().navigationBarsPadding()) {
        StepHeader(
            title = "Sign Off", subtitle = "Step 2 of 2",
            taskTitle = task?.title, step = 1,
            onBack = { if (!submitting) onBack() },
        )
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (durationText != null) item {
                Row(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(SuccessBg).padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("\u23F1", fontSize = 20.sp)
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text("Total Work Duration", fontSize = 11.sp, color = Success, fontWeight = FontWeight.SemiBold)
                        Text(durationText, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF166534))
                    }
                }
            }
            item {
                FormLabel("Work Description *")
                Spacer(Modifier.height(6.dp))
                BasicTextField(
                    value = description, onValueChange = onDescChange,
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                        .background(CardBg).border(1.dp, DividerSoft, RoundedCornerShape(12.dp))
                        .padding(14.dp).height(100.dp),
                    textStyle = TextStyle(fontSize = 14.sp, color = InkPrimary),
                    cursorBrush = SolidColor(Brand),
                    decorationBox = { inner ->
                        Box {
                            if (description.isEmpty()) Text("Describe work done and issue resolved\u2026", fontSize = 14.sp, color = InkMuted)
                            inner()
                        }
                    },
                )
            }
            item {
                FormLabel("Downtime (minutes)")
                Spacer(Modifier.height(6.dp))
                BasicTextField(
                    value = downtime,
                    onValueChange = { if (it.all { c -> c.isDigit() }) onDowntimeChange(it) },
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                        .background(CardBg).border(1.dp, DividerSoft, RoundedCornerShape(12.dp))
                        .padding(horizontal = 14.dp, vertical = 14.dp),
                    textStyle = TextStyle(fontSize = 14.sp, color = InkPrimary),
                    cursorBrush = SolidColor(Brand),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    decorationBox = { inner ->
                        Box { if (downtime.isEmpty()) Text("0", fontSize = 14.sp, color = InkMuted); inner() }
                    },
                )
            }
            item {
                FormLabel("Root Cause Analysis *")
                Spacer(Modifier.height(6.dp))
                RcaDropdown(selected = rca, onSelect = onRcaChange)
            }
            if (error != null) item {
                Row(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFFFEF2F2)).padding(12.dp),
                ) {
                    Text("\u26A0\uFE0F  $error", fontSize = 13.sp, color = Danger)
                }
            }
        }
        BottomCta(
            label = if (submitting) "Submitting\u2026" else "Submit & Complete \u2713",
            enabled = !submitting, loading = submitting, onClick = onSubmit,
        )
    }
}

@Composable
private fun RcaDropdown(selected: String, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        Row(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(CardBg)
                .border(1.dp, if (selected.isNotBlank()) Brand.copy(.4f) else DividerSoft, RoundedCornerShape(12.dp))
                .clickable { expanded = true }.padding(horizontal = 14.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                selected.ifBlank { "Select root cause\u2026" },
                fontSize = 14.sp,
                color = if (selected.isBlank()) InkMuted else InkPrimary,
            )
            Text("\u25BE", fontSize = 14.sp, color = InkMuted)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            RCA_OPTIONS.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option, fontSize = 14.sp) },
                    onClick = { onSelect(option); expanded = false },
                )
            }
        }
    }
}

@Composable
private fun SuccessStep(onDone: () -> Unit) {
    val scale = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        scale.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow))
        delay(2600)
        onDone()
    }
    Box(
        modifier = Modifier.fillMaxSize().background(
            Brush.verticalGradient(listOf(Color(0xFF0F172A), Color(0xFF1E3A5F), Color(0xFF0F172A))),
        ),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.scale(scale.value)) {
            Box(
                modifier = Modifier.size(120.dp)
                    .background(Brush.radialGradient(listOf(Success.copy(.2f), Color.Transparent)), CircleShape)
                    .border(2.dp, Success.copy(.5f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Success, modifier = Modifier.size(70.dp))
            }
            Spacer(Modifier.height(28.dp))
            Text("Task Completed!", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color.White, textAlign = TextAlign.Center)
            Spacer(Modifier.height(10.dp))
            Text(
                "Your sign-off has been recorded\nand inventory updated.",
                fontSize = 15.sp, color = Color.White.copy(.65f), textAlign = TextAlign.Center, lineHeight = 22.sp,
            )
            Spacer(Modifier.height(36.dp))
            CircularProgressIndicator(color = Success.copy(.6f), strokeWidth = 2.dp, modifier = Modifier.size(22.dp))
        }
    }
}

@Composable
private fun StepHeader(title: String, subtitle: String, taskTitle: String?, step: Int, onBack: () -> Unit) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth().background(CardBg).padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = InkPrimary)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(subtitle, fontSize = 11.sp, color = InkMuted, fontWeight = FontWeight.Medium)
                Text(title, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = InkPrimary)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                StepDot(active = step == 0)
                StepDot(active = step == 1)
            }
        }
        if (!taskTitle.isNullOrBlank()) {
            Row(
                modifier = Modifier.fillMaxWidth().background(Brand50).padding(horizontal = 20.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Default.Inventory2, contentDescription = null, tint = Brand, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text(taskTitle, fontSize = 13.sp, color = BrandDeep, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun StepDot(active: Boolean) {
    Box(
        modifier = Modifier
            .size(if (active) 10.dp else 8.dp)
            .background(if (active) Brand else InkMuted.copy(.3f), CircleShape),
    )
}

@Composable
private fun FormLabel(text: String) {
    Text(text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = InkSecondary)
}

@Composable
private fun BottomCta(label: String, enabled: Boolean = true, loading: Boolean = false, onClick: () -> Unit) {
    Box(modifier = Modifier.fillMaxWidth().background(CardBg).padding(20.dp)) {
        Box(
            modifier = Modifier.fillMaxWidth().height(52.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(
                    if (enabled) Brush.horizontalGradient(listOf(Brand, BrandDeep))
                    else Brush.horizontalGradient(listOf(InkMuted, InkMuted)),
                )
                .clickable(enabled = enabled && !loading, onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            if (loading) {
                CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(22.dp))
            } else {
                Text(label, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    }
}
