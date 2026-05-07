package com.example.uniwattelektrik.feature.admin.presentation.screens.inventory

import com.example.uniwattelektrik.core.performance.TrackScreenPerformance

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Inventory2
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.uniwattelektrik.core.components.AppPullToRefresh
import com.example.uniwattelektrik.core.components.EmptyState
import com.example.uniwattelektrik.core.components.InventoryScreenHeader
import com.example.uniwattelektrik.core.components.PremiumGradientFab
import com.example.uniwattelektrik.core.components.PremiumHeaderStatusBarColor
import com.example.uniwattelektrik.core.theme.AppTheme
import com.example.uniwattelektrik.core.theme.SetStatusBar
import com.example.uniwattelektrik.core.theme.appScreenBackground
import com.example.uniwattelektrik.feature.admin.presentation.InventoryViewModel
import com.example.uniwattelektrik.platform.nowEpochMillis

@Composable
fun EquipmentScreen(
    onBack: () -> Unit,
    inventoryVm: InventoryViewModel,
    adminId: String,
    modifier: Modifier = Modifier,
) {
    TrackScreenPerformance("EquipmentScreen")
    SetStatusBar(color = PremiumHeaderStatusBarColor, darkIcons = false)

    val departments by inventoryVm.departments.collectAsState()
    val equipment   by inventoryVm.equipment.collectAsState()

    var selectedDept  by remember { mutableStateOf<Department?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }
    var editTarget    by remember { mutableStateOf<Equipment?>(null) }
    var deleteTarget  by remember { mutableStateOf<Equipment?>(null) }

    val displayed = if (selectedDept == null) equipment
                    else equipment.filter { it.departmentId == selectedDept!!.id }

    // ── Add / Edit dialog ────────────────────────────────────────────────────
    if (showAddDialog || editTarget != null) {
        if (departments.isEmpty()) {
            // No departments yet — instruct the admin to create one first.
            AlertDialog(
                onDismissRequest = { showAddDialog = false; editTarget = null },
                title   = { Text("Add a department first", fontWeight = FontWeight.Bold) },
                text    = {
                    Text(
                        "Equipment must belong to a department. Please open the " +
                            "Departments screen and add at least one department before " +
                            "creating equipment.",
                        color = AppTheme.Ink500,
                    )
                },
                confirmButton = {
                    TextButton(onClick = { showAddDialog = false; editTarget = null }) {
                        Text("OK", color = AppTheme.Brand, fontWeight = FontWeight.SemiBold)
                    }
                },
            )
        } else {
            EquipmentFormDialog(
                initialName    = editTarget?.name ?: "",
                initialDeptId  = editTarget?.departmentId ?: (selectedDept?.id ?: departments.first().id),
                departments    = departments,
                title          = if (editTarget != null) "Edit Equipment" else "Add Equipment",
                onDismiss      = { showAddDialog = false; editTarget = null },
                onSave         = { name, deptId ->
                    if (editTarget != null) {
                        inventoryVm.updateEquipment(adminId, editTarget!!.id, name, deptId)
                        editTarget = null
                    } else {
                        inventoryVm.addEquipment(adminId, name, deptId)
                        showAddDialog = false
                    }
                },
            )
        }
    }

    // ── Delete confirm ───────────────────────────────────────────────────────
    deleteTarget?.let { eq ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title   = { Text("Delete equipment?", fontWeight = FontWeight.Bold) },
            text    = { Text("\"${eq.name}\" will be permanently removed.", color = AppTheme.Ink500) },
            confirmButton = {
                TextButton(onClick = {
                    inventoryVm.deleteEquipment(adminId, eq.id)
                    deleteTarget = null
                }) {
                    Text("Delete", color = AppTheme.Danger, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = { TextButton(onClick = { deleteTarget = null }) { Text("Cancel") } },
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = modifier.fillMaxSize().background(appScreenBackground())) {

            InventoryScreenHeader(
                title = "Equipment",
                subtitle = "${displayed.size} ITEMS IN ${selectedDept?.name?.uppercase() ?: "ALL DEPARTMENTS"}",
                onBack = onBack,
            )

            // ── Department filter chips ──────────────────────────────────────────
            LazyRow(
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                item {
                    FilterChip(
                        label = "All",
                        selected = selectedDept == null,
                        onClick = { selectedDept = null }
                    )
                }
                items(departments) { dept ->
                    FilterChip(
                        label = dept.name,
                        selected = selectedDept?.id == dept.id,
                        onClick = { selectedDept = dept }
                    )
                }
            }

            // ── List ──────────────────────────────────────────────────────────────
            AppPullToRefresh(
                onRefresh = { inventoryVm.loadForAdmin(adminId) },
                modifier  = Modifier.fillMaxSize(),
            ) {
                if (displayed.isEmpty()) {
                    EmptyState(
                        emoji = "🔧",
                        title = "No equipment found",
                        body = "Tap the '+' button to add equipment for this department."
                    )
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 120.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        itemsIndexed(displayed, key = { _, e -> e.id }) { index, eq ->
                            StaggeredEntrance(index = index) {
                                val deptName = departments.find { it.id == eq.departmentId }?.name ?: "—"
                                EquipmentCard(
                                    equipment  = eq,
                                    deptName   = deptName,
                                    onEdit     = { editTarget = eq },
                                    onDelete   = { deleteTarget = eq },
                                )
                            }
                        }
                    }
                }
            }
        }

        // ── FAB ──────────────────────────────────────────────────────────────
        PremiumGradientFab(onClick = { showAddDialog = true })
    }
}

@Composable
private fun FilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(12.dp)
    Box(
        modifier = Modifier
            .clip(shape)
            .background(if (selected) AppTheme.Brand else Color.White)
            .then(if (!selected) Modifier.shadow(2.dp, shape, spotColor = AppTheme.ShadowSm) else Modifier)
            .premiumPress(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (selected) Color.White else AppTheme.Ink700,
            fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
        )
    }
}

@Composable
private fun EquipmentCard(
    equipment: Equipment,
    deptName: String,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    val shape = RoundedCornerShape(20.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(6.dp, shape, ambientColor = Color.Transparent, spotColor = AppTheme.ShadowSm)
            .clip(shape)
            .background(Color.White)
            .premiumPress(onClick = {})
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFFF0F4FF)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Inventory2,
                    contentDescription = null,
                    tint = AppTheme.Brand,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = equipment.name,
                    color = AppTheme.Ink900,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = deptName,
                    color = AppTheme.Ink500,
                    fontSize = 12.sp
                )
            }
            
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconAction(icon = Icons.Outlined.Edit, tint = AppTheme.Brand, onClick = onEdit)
                IconAction(icon = Icons.Outlined.Delete, tint = AppTheme.Danger, onClick = onDelete)
            }
        }
    }
}

@Composable
private fun EquipmentFormDialog(
    initialName: String,
    initialDeptId: String,
    departments: List<Department>,
    title: String,
    onDismiss: () -> Unit,
    onSave: (name: String, deptId: String) -> Unit,
) {
    var name         by remember(initialName) { mutableStateOf(initialName) }
    var nameError    by remember { mutableStateOf("") }
    var selectedDept by remember(initialDeptId) {
        mutableStateOf(departments.find { it.id == initialDeptId } ?: departments.first())
    }
    var showDeptMenu by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        title = { Text(title, fontWeight = FontWeight.Bold, color = AppTheme.Ink900, fontSize = 20.sp) },
        text  = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // Equipment Name
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Equipment Name", color = AppTheme.Ink700, fontSize = 14.sp,
                         fontWeight = FontWeight.Medium)
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it; nameError = "" },
                        placeholder   = { Text("e.g. Circuit Breaker", color = AppTheme.Ink300, fontSize = 15.sp) },
                        singleLine    = true,
                        isError       = nameError.isNotEmpty(),
                        supportingText = if (nameError.isNotEmpty()) {{ Text(nameError, color = AppTheme.Danger, fontSize = 12.sp) }} else null,
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Words,
                            imeAction = ImeAction.Done,
                        ),
                        shape  = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor   = AppTheme.Brand,
                            unfocusedBorderColor = AppTheme.Ink100,
                            cursorColor          = AppTheme.Brand,
                        ),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                // Department picker
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Department", color = AppTheme.Ink700, fontSize = 14.sp,
                         fontWeight = FontWeight.Medium)
                    Box {
                        OutlinedTextField(
                            value         = selectedDept.name,
                            onValueChange = {},
                            readOnly      = true,
                            trailingIcon  = {
                                Icon(
                                    imageVector = Icons.Outlined.ExpandMore,
                                    contentDescription = null,
                                    tint = AppTheme.Ink500,
                                    modifier = Modifier.size(24.dp)
                                )
                            },
                            shape  = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor   = AppTheme.Brand,
                                unfocusedBorderColor = AppTheme.Ink100,
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showDeptMenu = true },
                            enabled = false // Click handled by Box
                        )
                        // Transparent overlay to catch clicks since TextField is readOnly
                        Box(modifier = Modifier.matchParentSize().clickable { showDeptMenu = true })
                        
                        DropdownMenu(
                            expanded = showDeptMenu,
                            onDismissRequest = { showDeptMenu = false },
                            modifier = Modifier.background(Color.White).shadow(8.dp, RoundedCornerShape(12.dp))
                        ) {
                            departments.forEach { dept ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = dept.name,
                                            color = if (dept.id == selectedDept.id) AppTheme.Brand else AppTheme.Ink900,
                                            fontWeight = if (dept.id == selectedDept.id) FontWeight.Bold else FontWeight.Normal
                                        )
                                    },
                                    onClick = { selectedDept = dept; showDeptMenu = false },
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(AppTheme.Brand)
                    .clickable {
                        if (name.isBlank()) { nameError = "Name is required"; return@clickable }
                        onSave(name.trim(), selectedDept.id)
                    }
                    .padding(horizontal = 24.dp, vertical = 12.dp),
            ) {
                Text("Save", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = AppTheme.Ink500, fontWeight = FontWeight.SemiBold)
            }
        },
    )
}
