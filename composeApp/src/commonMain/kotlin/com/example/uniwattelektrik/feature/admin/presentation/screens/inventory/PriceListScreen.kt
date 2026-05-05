package com.example.uniwattelektrik.feature.admin.presentation.screens.inventory

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.PriceCheck
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.uniwattelektrik.core.components.EmptyState
import com.example.uniwattelektrik.core.components.PremiumHeaderBackground
import com.example.uniwattelektrik.core.components.PremiumHeaderStatusBarColor
import com.example.uniwattelektrik.core.theme.AppTheme
import com.example.uniwattelektrik.core.theme.SetStatusBar
import com.example.uniwattelektrik.core.theme.appScreenBackground
import com.example.uniwattelektrik.platform.nowEpochMillis

@Composable
fun PriceListScreen(
    onBack: () -> Unit,
    onAdd: () -> Unit,
    onEdit: (SpareItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    SetStatusBar(color = PremiumHeaderStatusBarColor, darkIcons = false)

    val departments = remember { sampleDepartments }
    val allEquipment = remember { sampleEquipment }
    val items = remember {
        mutableStateListOf(
            SpareItem(
                id = "p1",
                category = "Cable",
                name = "2.5 Sq.mm PVC Wire",
                make = "Havells",
                size = "2.5 mm",
                core = "1C",
                price = 185.0,
                stockQty = 200,
                hsn = "8544",
                vendorName1 = "Vendor A",
                vendorGst1 = "27AAAAA0000A1Z5",
                vendorContact1 = "9876543210",
                vendorAddress1 = "Mumbai",
                vendorName2 = "Vendor B",
                vendorLocation = "Mumbai",
            ),
            SpareItem(id = "p2", category = "Cable", name = "4 Sq.mm PVC Wire", make = "Polycab", size = "4 mm", core = "1C", price = 280.0, stockQty = 150, hsn = "8544"),
            SpareItem(id = "p3", category = "Spare Component", name = "MCB 10A", make = "Legrand", size = "10A", core = "SP", currentRating = "10A", noOfPoles = "SP", price = 420.0, stockQty = 80, hsn = "8536"),
            SpareItem(id = "p4", category = "Spare Component", name = "MCB 20A", make = "Schneider", size = "20A", core = "DP", currentRating = "20A", noOfPoles = "DP", price = 650.0, stockQty = 60, hsn = "8536"),
            SpareItem(id = "p5", category = "Spare Component", name = "Hydraulic Oil", make = "Castrol", size = "20L", price = 1200.0, stockQty = 30, hsn = "2710"),
        )
    }

    var filterDept  by remember { mutableStateOf<Department?>(null) }
    var filterEquip by remember { mutableStateOf<Equipment?>(null) }
    var showDeptMenu  by remember { mutableStateOf(false) }
    var showEquipMenu by remember { mutableStateOf(false) }
    var deleteTarget  by remember { mutableStateOf<SpareItem?>(null) }

    val equipForDept = if (filterDept == null) allEquipment
                       else allEquipment.filter { it.departmentId == filterDept!!.id }

    val displayed = items.toList()

    // ── Delete confirm ───────────────────────────────────────────────────────
    deleteTarget?.let { pi ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title   = { Text("Delete item?", fontWeight = FontWeight.Bold) },
            text    = { Text("\"${pi.name}\" will be permanently removed.", color = AppTheme.Ink500) },
            confirmButton = {
                TextButton(onClick = { items.removeAll { it.id == pi.id }; deleteTarget = null }) {
                    Text("Delete", color = AppTheme.High, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = { TextButton(onClick = { deleteTarget = null }) { Text("Cancel") } },
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = modifier.fillMaxSize().background(appScreenBackground())) {

            // ── Premium Gradient Header ─────────────────────────────────────────
            PremiumHeaderBackground(
                modifier = Modifier.shadow(12.dp, RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .windowInsetsPadding(WindowInsets.statusBars)
                        .padding(horizontal = 22.dp)
                        .padding(top = 16.dp, bottom = 32.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    // Back button
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color.White.copy(alpha = 0.15f))
                            .clickable(onClick = onBack),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "Price List",
                            color = Color.White,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-0.5).sp
                        )
                        Text(
                            text = "MASTER PRICING & VENDOR DIRECTORY",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp
                        )
                    }
                }
            }

            // ── Filters ───────────────────────────────────────────────────────────
            LazyRow(
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                item {
                    FilterChip(
                        label    = filterDept?.name ?: "All Departments",
                        selected = filterDept != null,
                        onExpand = { showDeptMenu = true },
                    ) {
                        DropdownMenu(
                            expanded = showDeptMenu,
                            onDismissRequest = { showDeptMenu = false },
                            modifier = Modifier.background(Color.White).shadow(8.dp, RoundedCornerShape(12.dp))
                        ) {
                            DropdownMenuItem(
                                text    = { Text("All Departments", color = AppTheme.Ink500, fontSize = 13.sp) },
                                onClick = {
                                    filterDept  = null
                                    filterEquip = null
                                    showDeptMenu = false
                                },
                            )
                            departments.forEach { d ->
                                DropdownMenuItem(
                                    text = { Text(d.name, color = if (filterDept?.id == d.id) AppTheme.Brand else AppTheme.Ink900, fontSize = 13.sp) },
                                    onClick = {
                                        filterDept  = d
                                        filterEquip = null
                                        showDeptMenu = false
                                    },
                                )
                            }
                        }
                    }
                }
                item {
                    FilterChip(
                        label    = filterEquip?.name ?: "All Equipment",
                        selected = filterEquip != null,
                        onExpand = { showEquipMenu = true },
                    ) {
                        DropdownMenu(
                            expanded = showEquipMenu,
                            onDismissRequest = { showEquipMenu = false },
                            modifier = Modifier.background(Color.White).shadow(8.dp, RoundedCornerShape(12.dp))
                        ) {
                            DropdownMenuItem(
                                text    = { Text("All Equipment", color = AppTheme.Ink500, fontSize = 13.sp) },
                                onClick = { filterEquip = null; showEquipMenu = false },
                            )
                            equipForDept.forEach { e ->
                                DropdownMenuItem(
                                    text = { Text(e.name, color = if (filterEquip?.id == e.id) AppTheme.Brand else AppTheme.Ink900, fontSize = 13.sp) },
                                    onClick = { filterEquip = e; showEquipMenu = false },
                                )
                            }
                        }
                    }
                }
            }

            // ── List ──────────────────────────────────────────────────────────────
            if (displayed.isEmpty()) {
                EmptyState(
                    emoji = "💰",
                    title = "No items found",
                    body = "Adjust filters or tap the '+' button to create a price item."
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 120.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    itemsIndexed(displayed, key = { _, it -> it.id }) { index, item ->
                        StaggeredEntrance(index = index) {
                            val deptName  = item.category.ifBlank { "—" }
                            val equipName = "—"
                            SpareItemCard(
                                item      = item,
                                deptName  = deptName,
                                equipName = equipName,
                                onEdit    = { onEdit(item) },
                                onDelete  = { deleteTarget = item },
                            )
                        }
                    }
                }
            }
        }

        // ── FAB ──────────────────────────────────────────────────────────────
        FloatingActionButton(
            onClick = { onAdd() },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp)
                .padding(bottom = 16.dp),
            containerColor = AppTheme.Brand,
            contentColor = Color.White,
            shape = RoundedCornerShape(16.dp),
            elevation = androidx.compose.material3.FloatingActionButtonDefaults.elevation(8.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.Add, null)
                Text("Add Item", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun SpareItemCard(
    item: SpareItem,
    deptName: String,
    equipName: String,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    val shape = RoundedCornerShape(24.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(8.dp, shape, ambientColor = Color.Transparent, spotColor = AppTheme.ShadowSpotSoft)
            .clip(shape)
            .background(Color.White)
            .padding(20.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            // Title & Actions Row
            Row(verticalAlignment = Alignment.Top) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(AppTheme.LowBg),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.PriceCheck,
                        contentDescription = null,
                        tint = AppTheme.Low,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.name,
                        color = AppTheme.Ink900,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "$equipName • $deptName",
                        color = AppTheme.Ink500,
                        fontSize = 12.sp
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconAction(icon = Icons.Outlined.Edit, tint = AppTheme.Brand, onClick = onEdit)
                    IconAction(icon = Icons.Outlined.Delete, tint = AppTheme.High, onClick = onDelete)
                }
            }

            // Specs Grid
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(AppTheme.Bg)
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                PriceField("Make",  item.make.takeIf { it.isNotBlank() } ?: "—", Modifier.weight(1f))
                PriceField("Size",  item.size,  Modifier.weight(1f))
                PriceField("Core",  item.core,  Modifier.weight(1f))
                PriceField("HSN",   item.hsn,   Modifier.weight(1f))
            }

            // Price & Stock
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("Unit Price", color = AppTheme.Ink300, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    Text(
                        text = "₹ ${item.price.toLong()}",
                        color = AppTheme.Low, // Highlighted Green
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("Current Stock", color = AppTheme.Ink300, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    Text(
                        text = "${item.stockQty} units",
                        color = if (item.stockQty < 20) AppTheme.High else AppTheme.Ink900,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // Vendor Section (Optional)
            if (item.vendorName1.isNotBlank()) {
                Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(AppTheme.Ink100))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.ExpandMore,
                        contentDescription = null,
                        tint = AppTheme.Ink300,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Vendor: ${item.vendorName1} (${item.vendorLocation})",
                        color = AppTheme.Ink500,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
private fun PriceField(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(label, color = AppTheme.Ink300, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
        Text(value, color = AppTheme.Ink900, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
    }
}

@Composable
private fun FilterChip(
    label: String,
    selected: Boolean,
    onExpand: () -> Unit,
    content: @Composable () -> Unit,
) {
    val shape = RoundedCornerShape(12.dp)
    Box {
        Row(
            modifier = Modifier
                .clip(shape)
                .background(if (selected) AppTheme.Brand else Color.White)
                .then(if (!selected) Modifier.shadow(2.dp, shape, spotColor = AppTheme.ShadowSpotSoft) else Modifier)
                .clickable(onClick = onExpand)
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = label,
                color = if (selected) Color.White else AppTheme.Ink700,
                fontSize = 13.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
            )
            Icon(
                imageVector = Icons.Outlined.ExpandMore,
                contentDescription = null,
                tint = if (selected) Color.White else AppTheme.Ink300,
                modifier = Modifier.size(16.dp)
            )
        }
        content()
    }
}
