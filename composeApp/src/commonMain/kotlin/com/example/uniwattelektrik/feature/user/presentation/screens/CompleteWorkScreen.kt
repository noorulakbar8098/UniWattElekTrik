package com.example.uniwattelektrik.feature.user.presentation.screens

import com.example.uniwattelektrik.core.theme.appScreenBackground

import androidx.compose.animation.AnimatedContent
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Inventory
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.absoluteValue

/* ── Tokens — same family as the rest of the app ─────────────────────── */
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
private val Danger       = Color(0xFFEF4444)
private val ShadowSoft   = Color(0x14172C50)
private val DividerSoft  = Color(0xFFE5EAF2)

/* ── Sample data structures ──────────────────────────────────────────── */

private data class MaterialItem(
    val name: String, val sku: String, val rate: String,
    val unit: String, var qty: Int, var used: Boolean,
)

private data class EquipmentItem(
    val name: String, val code: String,
    val costNote: String,         // "@ ₹2,400/hr" or "OWNED · NO COST"
    val unit: String,             // "hr" or "pc"
    var qty: Double, var used: Boolean, val rate: Double, // hire rate per unit
)

private data class LabourEntry(
    val initials: String, val tint: Color, val name: String,
    val role: String, val empCode: String, val hours: Double, val cost: Int,
)

@Composable
fun CompleteWorkScreen(
    taskId: String,
    onClose: () -> Unit,
    onSubmitted: () -> Unit,
) {
    /* Internal flow state — 4 form steps, then a success state. */
    var step by remember { mutableStateOf(1) }
    var submitted by remember { mutableStateOf(false) }

    /* Sample data (would normally come from the task + viewmodel) */
    val taskCode = remember { taskId.take(8).uppercase().ifBlank { "TASK-2438" } }
    val materials = remember {
        mutableStateListOf(
            MaterialItem("Transformer Oil", "SKU-TXO-MIN-25", "@ ₹425/L", "L", 15, true),
            MaterialItem("Cooling Fan Motor 220V", "SKU-FAN-220V-01", "@ ₹3,200/pc", "pc", 1, true),
            MaterialItem("Insulation Tape · 10m", "SKU-INS-TP-10M", "NOT USED", "pc", 0, false),
        )
    }
    val equipment = remember {
        mutableStateListOf(
            EquipmentItem("Hydraulic Crane · 4-ton", "EQ-CRN-04T",
                "@ ₹2,400/hr", "hr", 2.0, true, 2400.0),
            EquipmentItem("Insulation Tester · 5kV", "EQ-INS-5KV",
                "OWNED · NO COST", "pc", 1.0, true, 0.0),
            EquipmentItem("Digital Multimeter · Cat IV", "EQ-MMT-CAT4",
                "OWNED · NO COST", "pc", 1.0, true, 0.0),
            EquipmentItem("Thermal Imaging Camera", "EQ-THM-IR",
                "NOT USED", "pc", 0.0, false, 0.0),
        )
    }
    val labour = remember {
        listOf(
            LabourEntry("RK", Color(0xFF60A5FA), "Rajesh Kumar",
                "LEAD", "EMP-1024", 4.5, 4950),
            LabourEntry("AS", Color(0xFF34D399), "Arjun Singh",
                "TECH", "EMP-1108", 4.5, 3600),
            LabourEntry("PM", Color(0xFFFB923C), "Priya Menon",
                "TECH", "EMP-1156", 4.5, 3600),
        )
    }
    var ppeAllowance  by remember { mutableStateOf(900) }
    var refreshments  by remember { mutableStateOf(408) }
    var supervisorSigned by remember { mutableStateOf(false) }

    /* Computed totals */
    val materialsTotal = remember(materials.toList()) {
        materials.filter { it.used }.sumOf { rateValue(it.rate) * it.qty }
    }
    val equipmentTotal = remember(equipment.toList()) {
        equipment.filter { it.used }.sumOf { (it.rate * it.qty).toInt() }
    }
    val labourTotal = labour.sumOf { it.cost }
    val logistics   = ppeAllowance + refreshments
    val grandTotal  = materialsTotal + equipmentTotal + labourTotal + logistics
    val budget      = 32500
    val withinBy    = (budget - grandTotal).coerceAtLeast(0)
    val withinPct   = if (budget > 0) (withinBy * 100 / budget) else 0

    if (submitted) {
        CompletionSuccessScreen(
            taskCode = taskCode,
            grandTotal = grandTotal,
            onShareReceipt = {},
            onBackToDashboard = onSubmitted,
        )
        return
    }

    Box(modifier = Modifier.fillMaxSize().background(appScreenBackground())) {

        Column(modifier = Modifier.fillMaxSize()) {

            CompleteWorkHeader(
                taskCode = taskCode,
                step = step,
                total = 4,
                onBack = { if (step > 1) step-- else onClose() },
                onClose = onClose,
            )

            StepIndicator(current = step)

            AnimatedContent(
                targetState = step,
                transitionSpec = {
                    val dir = if (targetState > initialState) 1 else -1
                    (slideInHorizontally(tween(260)) { full -> dir * full / 4 } +
                        fadeIn(tween(220))) togetherWith
                        (slideOutHorizontally(tween(220)) { full -> -dir * full / 4 } +
                            fadeOut(tween(180)))
                },
                label = "step-anim",
                modifier = Modifier.weight(1f),
            ) { current ->
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(top = 12.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    when (current) {
                        1 -> item { MaterialsStep(materials = materials, total = materialsTotal) }
                        2 -> item { EquipmentStep(equipment = equipment, total = equipmentTotal) }
                        3 -> item {
                            CostStep(
                                labour = labour,
                                ppeAllowance = ppeAllowance,
                                refreshments = refreshments,
                                onPpeChange = { ppeAllowance = it },
                                onRefreshmentsChange = { refreshments = it },
                                materialsTotal = materialsTotal,
                                equipmentTotal = equipmentTotal,
                                labourTotal = labourTotal,
                                logistics = logistics,
                                grandTotal = grandTotal,
                                withinBy = withinBy, withinPct = withinPct,
                            )
                        }
                        4 -> item {
                            SignOffStep(
                                materialsCount = materials.count { it.used },
                                materialsTotal = materialsTotal,
                                equipmentTotal = equipmentTotal,
                                equipmentTime = equipment.filter { it.used && it.unit == "hr" }
                                    .sumOf { it.qty },
                                labour = labour, labourTotal = labourTotal,
                                logistics = logistics, grandTotal = grandTotal,
                                signed = supervisorSigned,
                                onSign = { supervisorSigned = true },
                            )
                        }
                    }
                }
            }

            BottomActions(
                step = step,
                canGoNext = when (step) {
                    1 -> materials.any { it.used }
                    2 -> equipment.any { it.used }
                    3 -> true
                    4 -> supervisorSigned
                    else -> false
                },
                materialsTotal = materialsTotal,
                equipmentTotal = equipmentTotal,
                onSaveDraft = onClose,
                onBack = { if (step > 1) step-- },
                onNext = {
                    if (step < 4) step++ else submitted = true
                },
            )
        }
    }
}

/* ─────────────────────────────────────────────────────────────────────── *
 *  HEADER + STEP INDICATOR
 * ─────────────────────────────────────────────────────────────────────── */

@Composable
private fun CompleteWorkHeader(
    taskCode: String,
    step: Int,
    total: Int,
    onBack: () -> Unit,
    onClose: () -> Unit,
) {
    com.example.uniwattelektrik.core.components.PremiumHeaderBackground(
        roundedBottom = false,
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            com.example.uniwattelektrik.core.components.GlassBackButton(onClick = onBack)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Complete Work", color = Color.White,
                     fontSize = 19.sp, fontWeight = FontWeight.Bold)
                Text("#$taskCode · STEP $step / $total",
                     color = Color.White.copy(alpha = 0.78f),
                     fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
                     letterSpacing = 0.5.sp)
            }
            HeaderIcon(icon = Icons.Filled.Close, onClick = onClose)
        }
    }
}

@Composable
private fun HeaderIcon(icon: ImageVector, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.18f))
            .border(1.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, null, tint = Color.White, modifier = Modifier.size(18.dp)) }
}

@Composable
private fun StepIndicator(current: Int) {
    val labels = listOf("MATERIALS", "EQUIPMENT", "COST", "SIGN-OFF")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(CardBg)
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        labels.forEachIndexed { i, lbl ->
            val n = i + 1
            val state = when {
                n < current -> StepState.Done
                n == current -> StepState.Active
                else        -> StepState.Upcoming
            }
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                StepBadge(number = n, state = state)
                Text(
                    lbl,
                    color = when (state) {
                        StepState.Done     -> Success
                        StepState.Active   -> Brand
                        StepState.Upcoming -> InkMuted
                    },
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.6.sp,
                )
            }
            if (i != labels.lastIndex) {
                Box(
                    modifier = Modifier
                        .height(2.dp)
                        .weight(0.6f)
                        .background(
                            if (n < current) Success
                            else DividerSoft,
                        ),
                )
            }
        }
    }
}

private enum class StepState { Done, Active, Upcoming }

@Composable
private fun StepBadge(number: Int, state: StepState) {
    val (bg, fg, content) = when (state) {
        StepState.Done -> Triple(Success, Color.White, "✓")
        StepState.Active -> Triple(Brand, Color.White, number.toString())
        StepState.Upcoming -> Triple(Color(0xFFEEF2F7), InkMuted, number.toString())
    }
    Box(
        modifier = Modifier
            .size(28.dp).clip(CircleShape).background(bg)
            .then(
                if (state == StepState.Active)
                    Modifier.shadow(8.dp, CircleShape, spotColor = Brand.copy(alpha = 0.5f))
                else Modifier,
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (state == StepState.Done) {
            Icon(Icons.Filled.Check, null, tint = Color.White,
                 modifier = Modifier.size(14.dp))
        } else {
            Text(content, color = fg, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
    }
}

/* ─────────────────────────────────────────────────────────────────────── *
 *  CARD WRAPPER
 * ─────────────────────────────────────────────────────────────────────── */

@Composable
private fun Card(
    title: String? = null,
    subtitle: String? = null,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit,
) {
    Column(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .shadow(10.dp, RoundedCornerShape(20.dp), spotColor = ShadowSoft)
            .clip(RoundedCornerShape(20.dp))
            .background(CardBg)
            .padding(horizontal = 18.dp, vertical = 18.dp),
    ) {
        if (title != null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier
                    .height(14.dp).width(3.dp)
                    .clip(RoundedCornerShape(50)).background(Brand))
                Spacer(Modifier.width(8.dp))
                Text(title.uppercase(), color = Brand,
                     fontSize = 12.sp, fontWeight = FontWeight.Bold,
                     letterSpacing = 1.2.sp)
            }
            Spacer(Modifier.height(if (subtitle == null) 14.dp else 6.dp))
        }
        if (subtitle != null) {
            Text(subtitle, color = InkSecondary, fontSize = 12.sp,
                 lineHeight = 18.sp)
            Spacer(Modifier.height(12.dp))
        }
        content()
    }
}

/* ─────────────────────────────────────────────────────────────────────── *
 *  STEP 1 · MATERIALS
 * ─────────────────────────────────────────────────────────────────────── */

@Composable
private fun MaterialsStep(materials: androidx.compose.runtime.snapshots.SnapshotStateList<MaterialItem>, total: Int) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Card(
            title = "Materials Used",
            subtitle = "Confirm or edit the actual quantities consumed during this task.",
        ) {
            materials.forEachIndexed { i, item ->
                MaterialRow(
                    item = item,
                    onToggle = { materials[i] = item.copy(used = !item.used) },
                    onChangeQty = { materials[i] = item.copy(qty = it) },
                )
                if (i != materials.lastIndex) Spacer(Modifier.height(12.dp))
            }
            Spacer(Modifier.height(12.dp))
            DashedAddButton(label = "Add unplanned material", icon = Icons.Filled.Inventory)
        }
        Card {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Materials subtotal", color = InkPrimary,
                     fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                     modifier = Modifier.weight(1f))
                Text("₹${formatNumber(total)}", color = Brand,
                     fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun MaterialRow(
    item: MaterialItem,
    onToggle: () -> Unit,
    onChangeQty: (Int) -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Checkbox(checked = item.used, onClick = onToggle)
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                item.name, color = if (item.used) InkPrimary else InkMuted,
                fontSize = 14.sp, fontWeight = FontWeight.Bold,
            )
            Text(
                "${item.sku} · ${item.rate}",
                color = InkSecondary, fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
            )
        }
        Spacer(Modifier.width(8.dp))
        if (item.used) {
            QtyStepper(value = item.qty, unit = item.unit, onChange = onChangeQty)
        } else {
            Text("SKIP", color = Warning, fontSize = 11.sp,
                 fontWeight = FontWeight.Bold, letterSpacing = 0.6.sp)
        }
    }
}

/* ─────────────────────────────────────────────────────────────────────── *
 *  STEP 2 · EQUIPMENT
 * ─────────────────────────────────────────────────────────────────────── */

@Composable
private fun EquipmentStep(equipment: androidx.compose.runtime.snapshots.SnapshotStateList<EquipmentItem>, total: Int) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Card(
            title = "Equipment & Tools",
            subtitle = "Log every piece of equipment used on site. Hire-rate items appear with their cost.",
        ) {
            equipment.forEachIndexed { i, item ->
                EquipmentRow(
                    item = item,
                    onToggle = { equipment[i] = item.copy(used = !item.used) },
                    onChangeQty = { equipment[i] = item.copy(qty = it) },
                )
                if (i != equipment.lastIndex) Spacer(Modifier.height(12.dp))
            }
            Spacer(Modifier.height(12.dp))
            DashedAddButton(label = "Add unplanned equipment", icon = Icons.Filled.Build)
        }
        Card {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Equipment subtotal", color = InkPrimary,
                     fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                     modifier = Modifier.weight(1f))
                Text("₹${formatNumber(total)}", color = Brand,
                     fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun EquipmentRow(
    item: EquipmentItem,
    onToggle: () -> Unit,
    onChangeQty: (Double) -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Checkbox(checked = item.used, onClick = onToggle)
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                item.name, color = if (item.used) InkPrimary else InkMuted,
                fontSize = 14.sp, fontWeight = FontWeight.Bold,
            )
            Text(
                "${item.code} · ${item.costNote}",
                color = InkSecondary, fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
            )
        }
        Spacer(Modifier.width(8.dp))
        when {
            item.used && item.rate == 0.0 ->
                Text("USED ✓", color = Success, fontSize = 11.sp,
                     fontWeight = FontWeight.Bold, letterSpacing = 0.6.sp)
            item.used -> DoubleStepper(value = item.qty, unit = item.unit,
                                        onChange = onChangeQty)
            else -> Text("SKIP", color = Warning, fontSize = 11.sp,
                         fontWeight = FontWeight.Bold, letterSpacing = 0.6.sp)
        }
    }
}

/* ─────────────────────────────────────────────────────────────────────── *
 *  STEP 3 · COST
 * ─────────────────────────────────────────────────────────────────────── */

@Composable
private fun CostStep(
    labour: List<LabourEntry>,
    ppeAllowance: Int,
    refreshments: Int,
    onPpeChange: (Int) -> Unit,
    onRefreshmentsChange: (Int) -> Unit,
    materialsTotal: Int,
    equipmentTotal: Int,
    labourTotal: Int,
    logistics: Int,
    grandTotal: Int,
    withinBy: Int,
    withinPct: Int,
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Card(
            title = "Labour Breakdown",
            subtitle = "Auto-filled from clock-in data. Tap to edit billable hours per technician.",
        ) {
            labour.forEachIndexed { i, l ->
                LabourRow(entry = l)
                if (i != labour.lastIndex) Spacer(Modifier.height(10.dp))
            }
        }

        Card(title = "Additional Costs") {
            CostInputRow(
                title = "Tool & PPE allowance",
                sub = "3 techs · standard",
                value = ppeAllowance,
                onChange = onPpeChange,
            )
            Spacer(Modifier.height(10.dp))
            CostInputRow(
                title = "Site refreshments",
                sub = "Lunch · 3 pax",
                value = refreshments,
                onChange = onRefreshmentsChange,
            )
        }

        Card(title = "Cost Roll-Up") {
            CostLine("Materials", materialsTotal)
            CostLine("Equipment", equipmentTotal)
            CostLine("Labour", labourTotal, sub = "${labour.size} techs · ${labour.sumOf { it.hours }} hr")
            CostLine("Logistics", logistics)
            Spacer(Modifier.height(10.dp))
            ThinDivider()
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Total task cost", color = InkPrimary,
                     fontSize = 15.sp, fontWeight = FontWeight.Bold,
                     modifier = Modifier.weight(1f))
                Text("₹${formatNumber(grandTotal)}", color = Brand,
                     fontSize = 22.sp, fontWeight = FontWeight.Bold)
            }
        }

        if (withinBy > 0) {
            Box(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(SuccessBg)
                    .padding(horizontal = 14.dp, vertical = 12.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("💡", fontSize = 16.sp)
                    Spacer(Modifier.width(10.dp))
                    Text(
                        "Within budget by ₹${formatNumber(withinBy)} ($withinPct%). Eligible for efficiency bonus on submission.",
                        color = Color(0xFF166534),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        lineHeight = 17.sp,
                    )
                }
            }
        }
    }
}

@Composable
private fun LabourRow(entry: LabourEntry) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFFF8FAFC))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(36.dp).clip(CircleShape).background(entry.tint),
            contentAlignment = Alignment.Center,
        ) {
            Text(entry.initials, color = Color.White,
                 fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(entry.name, color = InkPrimary,
                 fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Text("${entry.empCode} · ${entry.role}",
                 color = InkSecondary, fontSize = 10.sp,
                 fontWeight = FontWeight.SemiBold, letterSpacing = 0.5.sp)
        }
        Text("${entry.hours}h", color = InkPrimary,
             fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.width(12.dp))
        Text("₹${formatNumber(entry.cost)}", color = Brand,
             fontSize = 14.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun CostInputRow(title: String, sub: String, value: Int, onChange: (Int) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = InkPrimary, fontSize = 13.sp,
                 fontWeight = FontWeight.SemiBold)
            Text(sub, color = InkMuted, fontSize = 11.sp)
        }
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFFF1F5F9))
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("₹", color = InkSecondary, fontSize = 13.sp)
            BasicTextField(
                value = value.toString(),
                onValueChange = { onChange(it.filter { c -> c.isDigit() }.toIntOrNull() ?: 0) },
                textStyle = TextStyle(color = InkPrimary, fontSize = 13.sp,
                                      fontWeight = FontWeight.Bold),
                cursorBrush = SolidColor(Brand),
                modifier = Modifier.width(60.dp),
            )
        }
    }
}

@Composable
private fun CostLine(label: String, value: Int, sub: String? = null) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, color = InkPrimary, fontSize = 14.sp,
                 fontWeight = FontWeight.Medium)
            if (sub != null) {
                Text(sub, color = InkMuted, fontSize = 11.sp)
            }
        }
        Text("₹${formatNumber(value)}", color = InkPrimary,
             fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
    }
}

/* ─────────────────────────────────────────────────────────────────────── *
 *  STEP 4 · SIGN-OFF
 * ─────────────────────────────────────────────────────────────────────── */

@Composable
private fun SignOffStep(
    materialsCount: Int,
    materialsTotal: Int,
    equipmentTotal: Int,
    equipmentTime: Double,
    labour: List<LabourEntry>,
    labourTotal: Int,
    logistics: Int,
    grandTotal: Int,
    signed: Boolean,
    onSign: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Card(title = "Final Summary") {
            SummaryRow("Materials used", "$materialsCount items", materialsTotal)
            ThinDivider()
            SummaryRow("Equipment hire", "Crane · ${equipmentTime} hr", equipmentTotal)
            ThinDivider()
            SummaryRow("Labour", "${labour.size} techs · ${labour.sumOf { it.hours }} hr", labourTotal)
            ThinDivider()
            SummaryRow("Logistics", "Travel + tools", logistics)
            ThinDivider()
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Total task cost", color = InkPrimary,
                     fontSize = 16.sp, fontWeight = FontWeight.Bold,
                     modifier = Modifier.weight(1f))
                Text("₹${formatNumber(grandTotal)}", color = Brand,
                     fontSize = 22.sp, fontWeight = FontWeight.Bold)
            }
        }

        Card(title = "Final Photos · Before / After") {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                PhotoTile(Color(0xFFFFB28A), Modifier.weight(1f))
                PhotoTile(Color(0xFF86EFAC), Modifier.weight(1f))
                PhotoTile(Color(0xFF8FB3FF), Modifier.weight(1f))
            }
            Spacer(Modifier.height(8.dp))
            Text("3 / 5", color = InkMuted, fontSize = 11.sp,
                 fontWeight = FontWeight.SemiBold)
        }

        Card(title = "Customer / Supervisor Sign-Off") {
            Box(
                modifier = Modifier
                    .fillMaxWidth().height(130.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFFF8FAFC))
                    .border(1.5.dp, DividerSoft, RoundedCornerShape(14.dp))
                    .clickable(onClick = onSign),
                contentAlignment = Alignment.Center,
            ) {
                if (!signed) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("✍️", fontSize = 28.sp)
                        Spacer(Modifier.height(6.dp))
                        Text("Tap to sign", color = InkSecondary,
                             fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                } else {
                    Text("Signed ✓", color = Success,
                         fontSize = 22.sp, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.height(12.dp))
            Text("Signed by", color = InkSecondary, fontSize = 11.sp,
                 fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(6.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFF8FAFC))
                    .padding(horizontal = 12.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("👤", fontSize = 14.sp)
                Spacer(Modifier.width(8.dp))
                Text("Sandeep K. (Supervisor)", color = InkPrimary,
                     fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun SummaryRow(label: String, sub: String, amount: Int) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, color = InkPrimary, fontSize = 14.sp,
                 fontWeight = FontWeight.SemiBold)
            Text(sub, color = InkSecondary, fontSize = 11.sp)
        }
        Text("₹${formatNumber(amount)}", color = InkPrimary,
             fontSize = 14.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun PhotoTile(tint: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .height(78.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(tint.copy(alpha = 0.30f)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(Icons.Filled.Camera, null, tint = tint, modifier = Modifier.size(26.dp))
        Box(
            modifier = Modifier
                .padding(6.dp)
                .size(20.dp).clip(CircleShape)
                .background(Color(0xCC000000))
                .align(Alignment.TopEnd),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.Close, null, tint = Color.White,
                 modifier = Modifier.size(12.dp))
        }
    }
}

/* ─────────────────────────────────────────────────────────────────────── *
 *  REUSABLE BITS
 * ─────────────────────────────────────────────────────────────────────── */

@Composable
private fun Checkbox(checked: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(22.dp)
            .clip(RoundedCornerShape(7.dp))
            .background(if (checked) Brand else Color.White)
            .border(
                width = if (checked) 0.dp else 1.5.dp,
                color = if (checked) Brand else DividerSoft,
                shape = RoundedCornerShape(7.dp),
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (checked) {
            Icon(Icons.Filled.Check, null, tint = Color.White,
                 modifier = Modifier.size(14.dp))
        }
    }
}

@Composable
private fun QtyStepper(value: Int, unit: String, onChange: (Int) -> Unit) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFFF1F5F9))
            .padding(horizontal = 6.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StepperBtn("−") { onChange((value - 1).coerceAtLeast(0)) }
        Text("$value", color = InkPrimary, fontSize = 14.sp,
             fontWeight = FontWeight.Bold,
             modifier = Modifier.padding(horizontal = 8.dp))
        StepperBtn("+") { onChange(value + 1) }
        Spacer(Modifier.width(4.dp))
        Text(unit, color = InkSecondary, fontSize = 11.sp,
             fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun DoubleStepper(value: Double, unit: String, onChange: (Double) -> Unit) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFFF1F5F9))
            .padding(horizontal = 6.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StepperBtn("−") { onChange((value - 0.5).coerceAtLeast(0.0)) }
        Text(formatHrs(value), color = InkPrimary, fontSize = 14.sp,
             fontWeight = FontWeight.Bold,
             modifier = Modifier.padding(horizontal = 8.dp))
        StepperBtn("+") { onChange(value + 0.5) }
        Spacer(Modifier.width(4.dp))
        Text(unit, color = InkSecondary, fontSize = 11.sp,
             fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun StepperBtn(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(24.dp).clip(CircleShape)
            .background(Color.White)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = Brand, fontSize = 14.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun DashedAddButton(label: String, icon: ImageVector) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Brand50)
            .border(1.5.dp, Brand.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
            .clickable {}
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("+", color = Brand, fontSize = 16.sp,
                 fontWeight = FontWeight.Bold)
            Spacer(Modifier.width(8.dp))
            Text(label, color = Brand, fontSize = 13.sp,
                 fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun ThinDivider() {
    Box(modifier = Modifier
        .fillMaxWidth().height(1.dp).background(DividerSoft))
}

/* ─────────────────────────────────────────────────────────────────────── *
 *  BOTTOM ACTIONS
 * ─────────────────────────────────────────────────────────────────────── */

@Composable
private fun BottomActions(
    step: Int,
    canGoNext: Boolean,
    materialsTotal: Int,
    equipmentTotal: Int,
    onSaveDraft: () -> Unit,
    onBack: () -> Unit,
    onNext: () -> Unit,
) {
    val nextLabel = when (step) {
        1 -> "Next: Equipment →"
        2 -> "Next: Cost →"
        3 -> "Next: Sign-off →"
        4 -> "✓ Submit completion"
        else -> "Next →"
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(CardBg)
            .border(1.dp, DividerSoft, RoundedCornerShape(0.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                modifier = Modifier
                    .weight(1f).height(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp, DividerSoft, RoundedCornerShape(12.dp))
                    .background(CardBg)
                    .clickable(onClick = if (step == 1) onSaveDraft else onBack),
                contentAlignment = Alignment.Center,
            ) {
                Text(if (step == 1) "Save draft" else "← Back",
                     color = InkSecondary, fontSize = 13.sp,
                     fontWeight = FontWeight.SemiBold)
            }
            Box(
                modifier = Modifier
                    .weight(1.6f).height(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (canGoNext) {
                            if (step == 4)
                                Brush.horizontalGradient(listOf(Success, Color(0xFF15803D)))
                            else
                                Brush.horizontalGradient(listOf(Brand, BrandDeep))
                        } else SolidColor(Color(0xFFB6CDEF)),
                    )
                    .clickable(enabled = canGoNext, onClick = onNext),
                contentAlignment = Alignment.Center,
            ) {
                Text(nextLabel, color = Color.White, fontSize = 13.sp,
                     fontWeight = FontWeight.Bold)
            }
        }
    }
}

/* ─────────────────────────────────────────────────────────────────────── *
 *  HELPERS
 * ─────────────────────────────────────────────────────────────────────── */

private fun formatNumber(n: Int): String {
    val s = n.toString()
    return s.reversed().chunked(3).joinToString(",").reversed()
}

private fun formatHrs(v: Double): String =
    if (v % 1.0 == 0.0) v.toInt().toString() else "%.1f".formatDouble(v)

private fun String.formatDouble(v: Double): String {
    // KMP-friendly: avoid String.format. Round to 1 decimal manually.
    val rounded = (v * 10).toInt()
    val whole = rounded / 10
    val frac = (rounded % 10).absoluteValue
    return "$whole.$frac"
}

private fun rateValue(rate: String): Int {
    // crude: extract first number from "@ ₹425/L" → 425
    val digits = rate.filter { it.isDigit() }
    return digits.toIntOrNull() ?: 0
}
