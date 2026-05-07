package com.example.uniwattelektrik.feature.user.presentation.screens

import com.example.uniwattelektrik.core.performance.TrackScreenPerformance

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
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
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.uniwattelektrik.core.components.PremiumHeaderBackground
import com.example.uniwattelektrik.core.media.rememberPhotoPicker
import com.example.uniwattelektrik.core.theme.AppShapes
import com.example.uniwattelektrik.core.theme.AppTheme
import com.example.uniwattelektrik.core.theme.AppTypography
import com.example.uniwattelektrik.feature.workforce.data.remote.MaterialUsedItem
import com.example.uniwattelektrik.feature.workforce.data.remote.SpareItemRecord
import com.example.uniwattelektrik.feature.workforce.data.remote.TaskRecord
import com.example.uniwattelektrik.feature.workforce.presentation.WorkforceViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// ─── Design tokens ────────────────────────────────────────────────────────────
private val ScreenBg     = AppTheme.Bg
private val CardBg       = AppTheme.Surface
private val InkPrimary   = AppTheme.Ink900
private val InkSecondary = AppTheme.Ink500
private val InkMuted     = AppTheme.Ink300
private val Brand        = AppTheme.Brand
private val BrandDeep    = AppTheme.Brand700
private val Brand50      = AppTheme.Brand50
private val Brand100     = AppTheme.Brand100
private val Success      = AppTheme.Success
private val SuccessBg    = AppTheme.SuccessBg
private val Warning      = AppTheme.Warning
private val WarningBg    = AppTheme.WarningBg
private val Danger       = AppTheme.Danger
private val DangerBg     = AppTheme.DangerBg
private val DividerSoft  = AppTheme.Ink100
private val ShadowSoft   = AppTheme.ShadowMd

// ─── RCA options ──────────────────────────────────────────────────────────────
private data class RcaOption(val label: String, val emoji: String)
private val RCA_OPTIONS = listOf(
    RcaOption("Power Failure",   "⚡"),
    RcaOption("Equipment Fault", "🔧"),
    RcaOption("Human Error",     "👤"),
    RcaOption("Maintenance Due", "🗓"),
    RcaOption("External Factor", "🌐"),
    RcaOption("Other",           "📋"),
)

private data class SpareRow(val spare: SpareItemRecord, val qty: Int = 0)

// ─── Screen entry point ───────────────────────────────────────────────────────
@Composable
fun CompleteWorkScreen(
    taskId: String,
    adminId: String,
    userId: String,
    workforceVm: WorkforceViewModel,
    onClose: () -> Unit,
    onSubmitted: () -> Unit,
) {
    TrackScreenPerformance("CompleteWorkScreen")
    val allTasks  by workforceVm.tasks.collectAsStateWithLifecycle()
    val allSpares by workforceVm.spareItems.collectAsStateWithLifecycle()
    val task      = allTasks.firstOrNull { it.id == taskId }

    // ── #1 Department / equipment filter ─────────────────────────────────────
    // Show ONLY spare items present in the inventory (stockQty > 0) whose
    // category matches the task's department or equipment. No fallback —
    // if nothing matches, the list is empty.
    val filteredSpares = remember(allSpares, task?.departmentName, task?.equipmentName) {
        val dept  = task?.departmentName?.lowercase().orEmpty().trim()
        val equip = task?.equipmentName?.lowercase().orEmpty().trim()
        val inStock = allSpares.filter { it.stockQty > 0 }
        if (dept.isEmpty() && equip.isEmpty()) inStock
        else inStock.filter { spare ->
            val cat = spare.category.lowercase().trim()
            if (cat.isEmpty()) false
            else (dept.isNotEmpty() && (cat.contains(dept) || dept.contains(cat))) ||
                 (equip.isNotEmpty() && (cat.contains(equip) || equip.contains(cat)))
        }
    }

    var step by remember { mutableIntStateOf(0) }
    val rows = remember(filteredSpares) {
        mutableStateListOf<SpareRow>().also { list ->
            filteredSpares.forEach { list.add(SpareRow(it, 0)) }
        }
    }
    var description  by remember { mutableStateOf("") }
    var downtime     by remember { mutableStateOf("") }
    var rca          by remember { mutableStateOf("") }
    var photoUri     by remember { mutableStateOf<String?>(null) }
    var submitting   by remember { mutableStateOf(false) }
    var submitError  by remember { mutableStateOf<String?>(null) }
    val scope        = rememberCoroutineScope()
    val endTimeMs    = remember { System.currentTimeMillis() }

    // ── #11 Live remaining stock (UI state) ───────────────────────────────────
    // When a user increments qty, show reduced remaining stock on the card.
    // Actual DB deduction happens only on final submit.
    fun remaining(row: SpareRow) = (row.spare.stockQty - row.qty).coerceAtLeast(0)

    AnimatedContent(
        targetState = step,
        transitionSpec = {
            if (targetState > initialState)
                (slideInHorizontally { it } + fadeIn()) togetherWith (slideOutHorizontally { -it } + fadeOut())
            else
                (slideInHorizontally { -it } + fadeIn()) togetherWith (slideOutHorizontally { it } + fadeOut())
        },
        label = "stepAnim",
    ) { currentStep ->
        when (currentStep) {
            0 -> MaterialStep(
                task      = task,
                rows      = rows,
                remaining = ::remaining,
                onBack    = onClose,
                onNext    = { step = 1 },
            )
            1 -> SignoffStep(
                task             = task,
                rows             = rows,
                description      = description,
                downtime         = downtime,
                rca              = rca,
                photoUri         = photoUri,
                submitting       = submitting,
                error            = submitError,
                onDescChange     = { description = it },
                onDowntimeChange = { downtime = it },
                onRcaChange      = { rca = it },
                onPhotoChange    = { photoUri = it },
                onBack           = { step = 0 },
                onSubmit         = {
                    submitError = null
                    if (task == null)          { submitError = "Task not found"; return@SignoffStep }
                    if (description.isBlank()) { submitError = "Please enter a work description"; return@SignoffStep }
                    if (rca.isBlank())         { submitError = "Please select an RCA option"; return@SignoffStep }
                    submitting = true
                    val used = rows.filter { it.qty > 0 }
                        .map { MaterialUsedItem(it.spare.id, it.spare.name, it.qty) }
                    scope.launch {
                        workforceVm.completeTaskWithSignoff(
                            adminId            = adminId,
                            taskId             = taskId,
                            assignedUserId     = userId,
                            signoffDescription = description,
                            downtimeMinutes    = downtime.toIntOrNull() ?: 0,
                            rca                = rca,
                            materialsUsed      = used,
                            startTimeMs        = task.acceptedAt ?: endTimeMs,
                            endTimeMs          = endTimeMs,
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

// ─────────────────────────────────────────────────────────────────────────────
//  STEP 1 — MATERIALS
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun MaterialStep(
    task: TaskRecord?,
    rows: MutableList<SpareRow>,
    remaining: (SpareRow) -> Int,
    onBack: () -> Unit,
    onNext: () -> Unit,
) {
    val selectedCount = rows.count { it.qty > 0 }
    val ctaLabel = if (selectedCount > 0)
        "Next: Sign Off  ·  $selectedCount item${if (selectedCount > 1) "s" else ""} →"
    else "Next: Sign Off →"

    // ── #10 Checklist check ────────────────────────────────────────────────────
    val uncheckedCount = task?.checklist?.count { !it.done } ?: 0

    // ── Search query for the materials list ────────────────────────────────────
    var query by remember { mutableStateOf("") }
    val visibleRows = remember(rows.toList(), query) {
        val q = query.trim().lowercase()
        if (q.isEmpty()) rows.toList()
        else rows.filter {
            it.spare.name.lowercase().contains(q) ||
            it.spare.category.lowercase().contains(q) ||
            it.spare.unit.lowercase().contains(q)
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(ScreenBg).navigationBarsPadding()) {
        StepHeader(title = "Materials Used", subtitle = "Step 1 of 2", taskTitle = task?.title, step = 0, onBack = onBack)

        // ── Search bar ───────────────────────────────────────────────────────
        if (rows.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(top = 12.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(CardBg)
                    .border(1.dp, DividerSoft, RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(Icons.Default.Search, contentDescription = null, tint = InkMuted, modifier = Modifier.size(18.dp))
                BasicTextField(
                    value = query,
                    onValueChange = { query = it },
                    singleLine = true,
                    textStyle = TextStyle(fontSize = 14.sp, color = InkPrimary),
                    cursorBrush = SolidColor(Brand),
                    modifier = Modifier.weight(1f),
                    decorationBox = { inner ->
                        if (query.isEmpty()) {
                            Text("Search materials…", fontSize = 14.sp, color = InkMuted)
                        }
                        inner()
                    },
                )
                if (query.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickable { query = "" }
                            .padding(4.dp),
                    ) {
                        Text("✕", fontSize = 13.sp, color = InkMuted, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Dept/equip filter chips
        if (!task?.departmentName.isNullOrBlank() || !task?.equipmentName.isNullOrBlank()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(top = 10.dp, bottom = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                listOfNotNull(task?.departmentName, task?.equipmentName).forEach { chip ->
                    Box(
                        modifier = Modifier.clip(RoundedCornerShape(50)).background(Brand50)
                            .padding(horizontal = 10.dp, vertical = 5.dp),
                    ) { Text(chip, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Brand) }
                }
                Text("· filtered", fontSize = 11.sp, color = InkMuted)
            }
        }

        // ── Checklist warning banner ─────────────────────────────────────────
        AnimatedVisibility(visible = uncheckedCount > 0, enter = expandVertically() + fadeIn()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)
                    .clip(RoundedCornerShape(12.dp)).background(WarningBg)
                    .border(1.dp, Warning.copy(.3f), RoundedCornerShape(12.dp)).padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Icon(Icons.Default.WarningAmber, contentDescription = null, tint = Warning, modifier = Modifier.size(18.dp))
                Text(
                    text = "$uncheckedCount checklist item${if (uncheckedCount > 1) "s" else ""} still unchecked. Complete them before signing off.",
                    fontSize = 13.sp, color = Warning, fontWeight = FontWeight.Medium,
                )
            }
        }

        if (rows.isEmpty()) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("📦", fontSize = 48.sp)
                    Spacer(Modifier.height(12.dp))
                    Text("No matching inventory items", fontSize = 15.sp, color = InkSecondary)
                    Text("Tap Next to continue", fontSize = 13.sp, color = InkMuted)
                }
            }
        } else if (visibleRows.isEmpty()) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🔍", fontSize = 40.sp)
                    Spacer(Modifier.height(8.dp))
                    Text("No items match \"$query\"", fontSize = 14.sp, color = InkSecondary)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(visibleRows, key = { it.spare.id }) { row ->
                    SpareRowCard(row = row, remaining = remaining(row), onQty = { newQty ->
                        val idx = rows.indexOfFirst { it.spare.id == row.spare.id }
                        if (idx >= 0) rows[idx] = rows[idx].copy(qty = newQty.coerceAtLeast(0))
                    })
                }
            }
        }
        BottomCta(label = ctaLabel, onClick = onNext)
    }
}

@Composable
private fun SpareRowCard(row: SpareRow, remaining: Int, onQty: (Int) -> Unit) {
    val scaleAnim by animateFloatAsState(
        targetValue   = if (row.qty > 0) 1.03f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label         = "scale",
    )
    // ── #3 Stock warning states ───────────────────────────────────────────────
    val atMax      = row.qty > 0 && row.qty >= row.spare.stockQty
    val outOfStock = row.spare.stockQty <= 0
    val lowStock   = row.spare.stockQty in 1..3

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scaleAnim)
            .shadow(if (row.qty > 0) 4.dp else 1.dp, RoundedCornerShape(14.dp), ambientColor = ShadowSoft)
            .clip(RoundedCornerShape(14.dp))
            .background(CardBg)
            .then(
                when {
                    atMax  -> Modifier.border(1.5.dp, Danger.copy(.4f), RoundedCornerShape(14.dp))
                    row.qty > 0 -> Modifier.border(1.5.dp, Brand.copy(.35f), RoundedCornerShape(14.dp))
                    else   -> Modifier
                }
            )
            .padding(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(row.spare.name, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = InkPrimary)
                Spacer(Modifier.height(2.dp))
                val meta = buildString {
                    if (row.spare.category.isNotBlank()) append(row.spare.category)
                    if (row.spare.unit.isNotBlank()) { if (isNotEmpty()) append(" · "); append(row.spare.unit) }
                }
                if (meta.isNotBlank()) Text(meta, fontSize = 12.sp, color = InkMuted)

                // ── #11 Live remaining stock ──────────────────────────────────
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // Show remaining after selection
                    val stockLabel = when {
                        outOfStock -> "Out of stock"
                        row.qty > 0 -> "Remaining: $remaining"
                        lowStock  -> "Low stock: ${row.spare.stockQty}"
                        else      -> "Stock: ${row.spare.stockQty}"
                    }
                    val stockColor = when {
                        outOfStock || atMax -> Danger
                        lowStock  -> Warning
                        else      -> Success
                    }
                    Text(stockLabel, fontSize = 11.sp, color = stockColor, fontWeight = FontWeight.SemiBold)

                    if (atMax) {
                        Box(
                            modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(DangerBg)
                                .padding(horizontal = 5.dp, vertical = 2.dp),
                        ) { Text("Max", fontSize = 10.sp, color = Danger, fontWeight = FontWeight.Bold) }
                    }
                    if (lowStock && !atMax && row.qty == 0) {
                        Box(
                            modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(WarningBg)
                                .padding(horizontal = 5.dp, vertical = 2.dp),
                        ) { Text("Low", fontSize = 10.sp, color = Warning, fontWeight = FontWeight.Bold) }
                    }
                }
            }
            Spacer(Modifier.width(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                StepperBtn(Icons.Default.Remove, enabled = row.qty > 0) { onQty(row.qty - 1) }
                Box(
                    modifier = Modifier.size(40.dp)
                        .background(
                            when {
                                atMax -> Danger
                                row.qty > 0 -> Brand
                                else -> Color(0xFFF1F5F9)
                            },
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "${row.qty}",
                        fontSize   = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color      = if (row.qty > 0) Color.White else InkSecondary,
                    )
                }
                StepperBtn(Icons.Default.Add, enabled = row.qty < row.spare.stockQty && !outOfStock) {
                    onQty(row.qty + 1)
                }
            }
        }
    }
}

@Composable
private fun StepperBtn(icon: ImageVector, enabled: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier.size(34.dp).clip(CircleShape)
            .background(if (enabled) Brand50 else Color(0xFFF1F5F9))
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = if (enabled) Brand else InkMuted, modifier = Modifier.size(18.dp))
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  STEP 2 — SIGN OFF
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun SignoffStep(
    task: TaskRecord?,
    rows: List<SpareRow>,
    description: String,
    downtime: String,
    rca: String,
    photoUri: String?,
    submitting: Boolean,
    error: String?,
    onDescChange: (String) -> Unit,
    onDowntimeChange: (String) -> Unit,
    onRcaChange: (String) -> Unit,
    onPhotoChange: (String?) -> Unit,
    onBack: () -> Unit,
    onSubmit: () -> Unit,
) {
    // ── #5 Live ticking duration ───────────────────────────────────────────────
    var elapsedMs by remember { mutableLongStateOf(
        System.currentTimeMillis() - (task?.acceptedAt ?: System.currentTimeMillis())
    )}
    LaunchedEffect(task?.acceptedAt) {
        val start = task?.acceptedAt ?: return@LaunchedEffect
        while (true) { elapsedMs = System.currentTimeMillis() - start; delay(1000L) }
    }
    val h = elapsedMs / 3_600_000
    val m = (elapsedMs % 3_600_000) / 60_000
    val s = (elapsedMs % 60_000) / 1_000
    val durationText = buildString { if (h > 0) append("${h}h "); append("${m}m "); append("${s}s") }

    // ── #7 Photo picker ─────────────────────────────────────────────────────
    val photoPicker = rememberPhotoPicker()

    // Derived summary for #2
    val selectedMaterials = rows.filter { it.qty > 0 }

    Column(modifier = Modifier.fillMaxSize().background(ScreenBg).navigationBarsPadding()) {
        StepHeader(title = "Sign Off", subtitle = "Step 2 of 2", taskTitle = task?.title, step = 1,
            onBack = { if (!submitting) onBack() })

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {

            // ── Live duration card ───────────────────────────────────────────
            if (task?.acceptedAt != null) item {
                LiveDurationCard(durationText = durationText)
            }

            // ── Summary preview card ─────────────────────────────────────────
            item {
                SummaryCard(
                    selectedMaterials = selectedMaterials,
                    downtime          = downtime,
                    rca               = rca,
                    photoUri          = photoUri,
                )
            }

            // Work description
            item {
                FormLabel("Work Description *")
                Spacer(Modifier.height(6.dp))
                BasicTextField(
                    value = description, onValueChange = onDescChange,
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                        .background(CardBg).border(1.dp, DividerSoft, RoundedCornerShape(12.dp))
                        .padding(14.dp).height(100.dp),
                    textStyle   = TextStyle(fontSize = 14.sp, color = InkPrimary),
                    cursorBrush = SolidColor(Brand),
                    decorationBox = { inner ->
                        Box {
                            if (description.isEmpty()) Text("Describe work done and issue resolved…",
                                fontSize = 14.sp, color = InkMuted)
                            inner()
                        }
                    },
                )
            }

            // Downtime
            item {
                FormLabel("Downtime (minutes)")
                Spacer(Modifier.height(6.dp))
                BasicTextField(
                    value = downtime,
                    onValueChange = { if (it.all { c -> c.isDigit() }) onDowntimeChange(it) },
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                        .background(CardBg).border(1.dp, DividerSoft, RoundedCornerShape(12.dp))
                        .padding(horizontal = 14.dp, vertical = 14.dp),
                    textStyle       = TextStyle(fontSize = 14.sp, color = InkPrimary),
                    cursorBrush     = SolidColor(Brand),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine      = true,
                    decorationBox   = { inner ->
                        Box { if (downtime.isEmpty()) Text("0", fontSize = 14.sp, color = InkMuted); inner() }
                    },
                )
            }

            // RCA pills
            item {
                FormLabel("Root Cause Analysis *")
                Spacer(Modifier.height(10.dp))
                RcaPillGrid(selected = rca, onSelect = onRcaChange)
            }

            // ── #7 Photo attachment ──────────────────────────────────────────
            item {
                FormLabel("Photo Proof (optional)")
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                        .background(CardBg).border(1.dp, DividerSoft, RoundedCornerShape(12.dp))
                        .clickable { photoPicker.pick() }.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Box(
                        modifier = Modifier.size(40.dp).clip(RoundedCornerShape(10.dp))
                            .background(if (photoUri != null) SuccessBg else Brand50),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = if (photoUri != null) Icons.Default.CheckCircle else Icons.Default.AddAPhoto,
                            contentDescription = null,
                            tint = if (photoUri != null) Success else Brand,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (photoUri != null) "Photo attached" else "Tap to attach photo",
                            fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                            color = if (photoUri != null) InkPrimary else InkSecondary,
                        )
                        if (photoUri != null) {
                            Text(photoUri.substringAfterLast("/"), fontSize = 11.sp, color = InkMuted,
                                maxLines = 1, overflow = TextOverflow.Ellipsis)
                        } else {
                            Text("JPEG / PNG proof of work done", fontSize = 11.sp, color = InkMuted)
                        }
                    }
                    if (photoUri != null) {
                        Icon(Icons.Default.Remove, contentDescription = "Remove",
                            tint = Danger, modifier = Modifier.size(18.dp)
                                .clickable { onPhotoChange(null) })
                    }
                }
                // Sync picker result
                LaunchedEffect(photoPicker.uri) {
                    photoPicker.uri?.let { onPhotoChange(it) }
                }
            }

            // Error banner
            if (error != null) item {
                Row(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFFFEF2F2)).padding(12.dp),
                ) { Text("⚠️  $error", fontSize = 13.sp, color = Danger) }
            }

            // ── #12 Supervisor / sign-off badge ──────────────────────────────
            item { Spacer(Modifier.height(4.dp)) }
            item {
                SignoffBadge(
                    taskTitle    = task?.title,
                    assigneeName = task?.assigneeName,
                    priority     = task?.priority,
                )
            }
        }

        // ── Sign-off CTA bar (premium) ───────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(
                    elevation    = 16.dp,
                    spotColor    = Color.Black.copy(alpha = .08f),
                    ambientColor = Color.Transparent,
                )
                .background(CardBg)
                .padding(horizontal = 20.dp, vertical = 14.dp),
        ) {
            // Disclaimer
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(
                    Icons.Default.Verified,
                    contentDescription = null,
                    tint = InkMuted,
                    modifier = Modifier.size(12.dp),
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    "By submitting you confirm the work is complete and accurate.",
                    fontSize = 11.sp,
                    color = InkMuted,
                    textAlign = TextAlign.Center,
                )
            }
            Spacer(Modifier.height(12.dp))

            // Hero gradient button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp)
                    .shadow(
                        elevation    = if (submitting) 0.dp else 12.dp,
                        shape        = RoundedCornerShape(18.dp),
                        spotColor    = Brand.copy(alpha = .55f),
                        ambientColor = Color.Transparent,
                    )
                    .clip(RoundedCornerShape(18.dp))
                    .background(
                        if (!submitting)
                            Brush.linearGradient(listOf(Brand, BrandDeep, Color(0xFF1F6BFF)))
                        else
                            Brush.horizontalGradient(listOf(InkMuted.copy(.7f), InkMuted.copy(.7f))),
                    )
                    .clickable(enabled = !submitting, onClick = onSubmit),
                contentAlignment = Alignment.Center,
            ) {
                if (submitting) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        CircularProgressIndicator(
                            color       = Color.White,
                            strokeWidth = 2.dp,
                            modifier    = Modifier.size(18.dp),
                        )
                        Text(
                            "Submitting…",
                            fontSize   = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color      = Color.White,
                        )
                    }
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(.22f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                        Text(
                            "Sign Off & Complete Task",
                            fontSize   = 15.5.sp,
                            fontWeight = FontWeight.Bold,
                            color      = Color.White,
                            letterSpacing = .3.sp,
                        )
                    }
                }
            }
        }
    }
}

// ── Live duration card ─────────────────────────────────────────────────────────
@Composable
private fun LiveDurationCard(durationText: String) {
    // Subtle pulse on the clock icon — communicates "live" without being noisy.
    val pulse = rememberInfiniteTransition(label = "durationPulse")
    val pulseAlpha by pulse.animateFloat(
        initialValue = 0.55f,
        targetValue  = 1f,
        animationSpec = infiniteRepeatable(
            animation   = tween(durationMillis = 1100, easing = LinearEasing),
            repeatMode  = RepeatMode.Reverse,
        ),
        label = "durationPulseAlpha",
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(20.dp),
                spotColor = Success.copy(alpha = 0.25f),
                ambientColor = Color.Transparent,
            )
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(Color(0xFFECFDF5), Color(0xFFF0FDF4), Color(0xFFE0F2FE)),
                ),
            )
            .border(1.dp, Success.copy(.20f), RoundedCornerShape(20.dp))
            .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(CircleShape)
                .background(Color.White)
                .border(1.dp, Success.copy(.30f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Default.Schedule,
                contentDescription = null,
                tint = Success.copy(alpha = pulseAlpha),
                modifier = Modifier.size(22.dp),
            )
        }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(Success.copy(alpha = pulseAlpha)),
                )
                Text(
                    "LIVE · TOTAL WORK DURATION",
                    fontSize      = 10.sp,
                    color         = Success,
                    fontWeight    = FontWeight.Bold,
                    letterSpacing = 1.2.sp,
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                durationText,
                fontSize   = 26.sp,
                fontWeight = FontWeight.Black,
                color      = Color(0xFF065F46),
            )
        }
    }
}

// ── Summary preview card ──────────────────────────────────────────────────────
@Composable
private fun SummaryCard(
    selectedMaterials: List<SpareRow>,
    downtime: String,
    rca: String,
    photoUri: String?,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 6.dp,
                shape = RoundedCornerShape(20.dp),
                spotColor = Brand.copy(alpha = 0.10f),
                ambientColor = Color.Transparent,
            )
            .clip(RoundedCornerShape(20.dp))
            .background(CardBg)
            .border(1.dp, DividerSoft, RoundedCornerShape(20.dp))
            .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Brand50),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Default.Verified, contentDescription = null, tint = Brand, modifier = Modifier.size(16.dp))
            }
            Spacer(Modifier.width(10.dp))
            Text(
                "SIGN-OFF SUMMARY",
                fontSize      = 11.sp,
                fontWeight    = FontWeight.Bold,
                color         = InkPrimary,
                letterSpacing = 1.4.sp,
            )
            Spacer(Modifier.weight(1f))
            // Compact "fields filled" pill
            val filled = listOf(
                selectedMaterials.isNotEmpty(),
                downtime.isNotBlank(),
                rca.isNotBlank(),
                photoUri != null,
            ).count { it }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(if (filled == 4) SuccessBg else Brand50)
                    .padding(horizontal = 8.dp, vertical = 3.dp),
            ) {
                Text(
                    "$filled/4",
                    fontSize   = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color      = if (filled == 4) Success else Brand,
                )
            }
        }

        SummaryRow(
            icon  = Icons.Default.Inventory2,
            label = "Materials",
            value = if (selectedMaterials.isEmpty()) "None selected"
                    else selectedMaterials.joinToString(", ") { "${it.qty}× ${it.spare.name}" },
            tint  = Brand,
            valueMuted = selectedMaterials.isEmpty(),
        )
        SummaryDivider()
        SummaryRow(
            icon  = Icons.Default.Schedule,
            label = "Downtime",
            value = if (downtime.isBlank()) "—" else "$downtime min",
            tint  = Warning,
            valueMuted = downtime.isBlank(),
        )
        SummaryDivider()
        SummaryRow(
            icon  = Icons.Default.Search,
            label = "Root Cause",
            value = rca.ifBlank { "Not selected" },
            tint  = Color(0xFF8B5CF6),
            valueMuted = rca.isBlank(),
        )
        SummaryDivider()
        SummaryRow(
            icon  = Icons.Default.AddAPhoto,
            label = "Photo",
            value = if (photoUri != null) "Attached" else "None",
            tint  = if (photoUri != null) Success else InkMuted,
            valueMuted = photoUri == null,
            trailingCheck = photoUri != null,
        )
    }
}

@Composable
private fun SummaryDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(DividerSoft.copy(alpha = .6f)),
    )
}

// ── Summary row helper (premium, icon-driven) ─────────────────────────────────
@Composable
private fun SummaryRow(
    icon: ImageVector,
    label: String,
    value: String,
    tint: Color,
    valueMuted: Boolean = false,
    trailingCheck: Boolean = false,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(tint.copy(alpha = .12f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(16.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(label, fontSize = 11.sp, color = InkMuted, fontWeight = FontWeight.Medium, letterSpacing = .4.sp)
            Spacer(Modifier.height(2.dp))
            Text(
                value,
                fontSize   = 13.5.sp,
                color      = if (valueMuted) InkMuted else InkPrimary,
                fontWeight = FontWeight.SemiBold,
                maxLines   = 2,
                overflow   = TextOverflow.Ellipsis,
            )
        }
        if (trailingCheck) {
            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Success, modifier = Modifier.size(18.dp))
        }
    }
}

// ── RCA pill grid (premium tile chips) ────────────────────────────────────────
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RcaPillGrid(selected: String, onSelect: (String) -> Unit) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement   = Arrangement.spacedBy(10.dp),
        maxItemsInEachRow     = 2,
    ) {
        RCA_OPTIONS.forEach { opt ->
            val isSel = selected == opt.label
            // Subtle elevation lift when selected.
            val elevation by animateFloatAsState(
                targetValue   = if (isSel) 6f else 0f,
                animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                label         = "rcaElev",
            )
            Row(
                modifier = Modifier
                    .weight(1f)
                    .shadow(
                        elevation    = elevation.dp,
                        shape        = RoundedCornerShape(14.dp),
                        spotColor    = Brand.copy(alpha = .35f),
                        ambientColor = Color.Transparent,
                    )
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        if (isSel) Brush.linearGradient(listOf(Brand, BrandDeep))
                        else Brush.linearGradient(listOf(CardBg, CardBg)),
                    )
                    .border(
                        width = if (isSel) 0.dp else 1.dp,
                        color = if (isSel) Color.Transparent else DividerSoft,
                        shape = RoundedCornerShape(14.dp),
                    )
                    .clickable { onSelect(opt.label) }
                    .padding(horizontal = 12.dp, vertical = 12.dp),
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSel) Color.White.copy(.20f) else Brand50),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(opt.emoji, fontSize = 16.sp)
                }
                Text(
                    opt.label,
                    fontSize   = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color      = if (isSel) Color.White else InkPrimary,
                    maxLines   = 1,
                    overflow   = TextOverflow.Ellipsis,
                    modifier   = Modifier.weight(1f),
                )
                if (isSel) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint               = Color.White,
                        modifier           = Modifier.size(16.dp),
                    )
                }
            }
        }
    }
}

// ── Sign-off badge (premium) ──────────────────────────────────────────────────
@Composable
private fun SignoffBadge(taskTitle: String?, assigneeName: String?, priority: String?) {
    val priorityColor = when (priority?.lowercase()) {
        "high", "urgent", "critical", "danger" -> Danger
        "medium", "med"                        -> Warning
        else                                   -> Success
    }
    val initials = (assigneeName ?: "")
        .split(' ', '\t')
        .filter { it.isNotBlank() }
        .take(2)
        .joinToString("") { it.first().uppercase() }
        .ifBlank { "✓" }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation    = 4.dp,
                shape        = RoundedCornerShape(20.dp),
                spotColor    = Brand.copy(alpha = .12f),
                ambientColor = Color.Transparent,
            )
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(CardBg, Brand50.copy(alpha = .35f)),
                ),
            )
            .border(1.dp, Brand.copy(.18f), RoundedCornerShape(20.dp))
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(Brush.linearGradient(listOf(Brand, BrandDeep))),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                initials,
                fontSize   = 16.sp,
                fontWeight = FontWeight.Bold,
                color      = Color.White,
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                "READY TO SIGN OFF",
                fontSize      = 10.sp,
                fontWeight    = FontWeight.Bold,
                color         = Brand,
                letterSpacing = 1.2.sp,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                if (!assigneeName.isNullOrBlank()) "Completing as $assigneeName" else "Completing task",
                fontSize   = 13.5.sp,
                fontWeight = FontWeight.Bold,
                color      = InkPrimary,
                maxLines   = 1,
                overflow   = TextOverflow.Ellipsis,
            )
            if (!taskTitle.isNullOrBlank()) {
                Spacer(Modifier.height(2.dp))
                Text(
                    taskTitle,
                    fontSize = 12.sp,
                    color    = InkSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (!priority.isNullOrBlank()) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(priorityColor.copy(.14f))
                    .border(1.dp, priorityColor.copy(.25f), RoundedCornerShape(50))
                    .padding(horizontal = 10.dp, vertical = 5.dp),
            ) {
                Text(
                    priority.uppercase(),
                    fontSize      = 10.sp,
                    fontWeight    = FontWeight.Bold,
                    color         = priorityColor,
                    letterSpacing = .8.sp,
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  SUCCESS STEP
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun SuccessStep(onDone: () -> Unit) {
    val scale = remember { Animatable(0f) }
    val haloAlpha = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        scale.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow))
        haloAlpha.animateTo(1f, tween(durationMillis = 600))
        delay(2800)
        onDone()
    }
    // Subtle ring pulse around the check mark.
    val pulse = rememberInfiniteTransition(label = "successPulse")
    val ringScale by pulse.animateFloat(
        initialValue = 0.95f,
        targetValue  = 1.12f,
        animationSpec = infiniteRepeatable(
            animation  = tween(durationMillis = 1500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "ringScale",
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF071233), Color(0xFF0D1B3E), Color(0xFF1458CC), Color(0xFF0D1B3E)),
                ),
            ),
        contentAlignment = Alignment.Center,
    ) {

        // Background radial glow
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(Success.copy(alpha = 0.18f * haloAlpha.value), Color.Transparent),
                    ),
                ),
        )

        // Floating confetti emojis (decorative, non-interactive)
        Text("✨", fontSize = 22.sp, modifier = Modifier.align(Alignment.TopStart).padding(start = 40.dp, top = 120.dp))
        Text("🎉", fontSize = 28.sp, modifier = Modifier.align(Alignment.TopEnd).padding(end = 36.dp, top = 100.dp))
        Text("⭐", fontSize = 18.sp, modifier = Modifier.align(Alignment.CenterStart).padding(start = 24.dp))
        Text("✨", fontSize = 20.sp, modifier = Modifier.align(Alignment.CenterEnd).padding(end = 28.dp))
        Text("🎊", fontSize = 24.sp, modifier = Modifier.align(Alignment.BottomStart).padding(start = 48.dp, bottom = 160.dp))
        Text("✨", fontSize = 18.sp, modifier = Modifier.align(Alignment.BottomEnd).padding(end = 60.dp, bottom = 200.dp))

        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.scale(scale.value)) {

            Box(contentAlignment = Alignment.Center) {
                // Outer pulsing ring
                Box(
                    modifier = Modifier
                        .size(160.dp)
                        .scale(ringScale)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(Success.copy(.18f), Color.Transparent),
                            ),
                            CircleShape,
                        ),
                )
                // Static glass ring
                Box(
                    modifier = Modifier
                        .size(140.dp)
                        .background(Color.White.copy(.06f), CircleShape)
                        .border(1.dp, Color.White.copy(.18f), CircleShape),
                )
                // Inner success disc
                Box(
                    modifier = Modifier
                        .size(110.dp)
                        .shadow(20.dp, CircleShape, spotColor = Success)
                        .background(
                            Brush.linearGradient(listOf(Color(0xFF22C55E), Color(0xFF16A34A))),
                            CircleShape,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint               = Color.White,
                        modifier           = Modifier.size(64.dp),
                    )
                }
            }

            Spacer(Modifier.height(32.dp))

            // Premium "TASK COMPLETE" overline
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(Success.copy(.18f))
                    .border(1.dp, Success.copy(.35f), RoundedCornerShape(50))
                    .padding(horizontal = 14.dp, vertical = 6.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(
                        Icons.Default.Bolt,
                        contentDescription = null,
                        tint = Success,
                        modifier = Modifier.size(13.dp),
                    )
                    Text(
                        "SIGN-OFF RECORDED",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Success,
                        letterSpacing = 1.6.sp,
                    )
                }
            }
            Spacer(Modifier.height(16.dp))

            Text(
                "Task Completed!",
                fontSize   = 32.sp,
                fontWeight = FontWeight.Black,
                color      = Color.White,
                textAlign  = TextAlign.Center,
            )
            Spacer(Modifier.height(10.dp))
            Text(
                "Your sign-off has been recorded\nand inventory has been updated.",
                fontSize   = 15.sp,
                color      = Color.White.copy(.72f),
                textAlign  = TextAlign.Center,
                lineHeight = 22.sp,
            )
            Spacer(Modifier.height(40.dp))

            // Premium "returning" indicator
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                CircularProgressIndicator(
                    color       = Success.copy(.8f),
                    strokeWidth = 2.dp,
                    modifier    = Modifier.size(16.dp),
                )
                Text(
                    "Returning to your tasks…",
                    fontSize   = 12.sp,
                    color      = Color.White.copy(.55f),
                    fontWeight = FontWeight.Medium,
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  SHARED
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun StepHeader(title: String, subtitle: String, taskTitle: String?, step: Int, onBack: () -> Unit) {
    // Uses the same PremiumHeaderBackground as the admin screens so the look
    // matches across the app. Adds a back button, overline, title, optional
    // task chip and an animated 2-step progress strip.
    val totalSteps = 2
    val progress by animateFloatAsState(
        targetValue   = ((step + 1).coerceAtMost(totalSteps).toFloat() / totalSteps).coerceIn(0f, 1f),
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label         = "stepProgress",
    )

    PremiumHeaderBackground(roundedBottom = true, cornerRadius = 24.dp) {
        Column(
            modifier = Modifier
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(horizontal = AppTheme.SpLg)
                .padding(top = AppTheme.SpSm, bottom = AppTheme.SpLg),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.18f))
                        .clickable(onClick = onBack),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint               = Color.White,
                        modifier           = Modifier.size(20.dp),
                    )
                }
                Spacer(Modifier.width(AppTheme.SpSm))
                Text(
                    "TASK COMPLETION",
                    style = AppTypography.labelSmall.copy(
                        color         = Color.White.copy(alpha = 0.70f),
                        letterSpacing = 1.8.sp,
                    ),
                )
                Spacer(Modifier.weight(1f))
                Box(
                    modifier = Modifier
                        .clip(AppShapes.pill)
                        .background(Color.White.copy(alpha = 0.18f))
                        .padding(horizontal = AppTheme.SpSm, vertical = 5.dp),
                ) {
                    Text(
                        subtitle,
                        style = AppTypography.captionLarge.copy(
                            color      = Color.White,
                            fontWeight = FontWeight.SemiBold,
                        ),
                    )
                }
            }
            Spacer(Modifier.height(AppTheme.SpMd))
            Text(
                title,
                style = AppTypography.displayMedium.copy(color = Color.White),
            )
            if (!taskTitle.isNullOrBlank()) {
                Spacer(Modifier.height(AppTheme.SpXs))
                Row(
                    modifier = Modifier
                        .clip(AppShapes.pill)
                        .background(Color.White.copy(alpha = 0.16f))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(
                        Icons.Default.Inventory2,
                        contentDescription = null,
                        tint               = Color.White,
                        modifier           = Modifier.size(13.dp),
                    )
                    Text(
                        taskTitle,
                        style    = AppTypography.captionLarge.copy(
                            color      = Color.White,
                            fontWeight = FontWeight.SemiBold,
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Spacer(Modifier.height(AppTheme.SpMd))
            Row(verticalAlignment = Alignment.CenterVertically) {
                BoxWithConstraints(
                    modifier = Modifier
                        .weight(1f)
                        .height(6.dp)
                        .clip(RoundedCornerShape(50))
                        .background(Color.White.copy(alpha = 0.20f)),
                ) {
                    val fillWidth = maxWidth * progress
                    Box(
                        modifier = Modifier
                            .width(fillWidth)
                            .height(6.dp)
                            .clip(RoundedCornerShape(50))
                            .background(
                                Brush.horizontalGradient(
                                    listOf(Color.White, Color.White.copy(alpha = 0.85f)),
                                ),
                            ),
                    )
                }
                Spacer(Modifier.width(10.dp))
                Text(
                    "${(step + 1).coerceAtMost(totalSteps)}/$totalSteps",
                    fontSize   = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color      = Color.White,
                )
            }
        }
    }
}

@Composable
private fun StepDot(active: Boolean, done: Boolean) {
    Box(
        modifier = Modifier.size(if (active) 10.dp else 8.dp)
            .background(when { done -> Success; active -> Brand; else -> InkMuted.copy(.3f) }, CircleShape),
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
            modifier = Modifier.fillMaxWidth().height(52.dp).clip(RoundedCornerShape(14.dp))
                .background(
                    if (enabled) Brush.horizontalGradient(listOf(Brand, BrandDeep))
                    else Brush.horizontalGradient(listOf(InkMuted, InkMuted))
                )
                .clickable(enabled = enabled && !loading, onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            if (loading) CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(22.dp))
            else Text(label, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }
    }
}
