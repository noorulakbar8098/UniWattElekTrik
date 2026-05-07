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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.AlertDialog
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
fun DepartmentScreen(
    onBack: () -> Unit,
    inventoryVm: InventoryViewModel,
    adminId: String,
    modifier: Modifier = Modifier,
) {
    TrackScreenPerformance("DepartmentScreen")
    SetStatusBar(color = PremiumHeaderStatusBarColor, darkIcons = false)

    val departments by inventoryVm.departments.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var showAddDialog    by remember { mutableStateOf(false) }
    var editTarget       by remember { mutableStateOf<Department?>(null) }
    var deleteTarget     by remember { mutableStateOf<Department?>(null) }

    val filteredDepts = if (searchQuery.isBlank()) departments
                       else departments.filter { it.name.contains(searchQuery, ignoreCase = true) }

    // ── Add / Edit dialog ────────────────────────────────────────────────────
    if (showAddDialog || editTarget != null) {
        DepartmentFormDialog(
            initial   = editTarget?.name ?: "",
            title     = if (editTarget != null) "Edit Department" else "Add Department",
            onDismiss = { showAddDialog = false; editTarget = null },
            onSave    = { name ->
                if (editTarget != null) {
                    inventoryVm.updateDepartment(adminId, editTarget!!.id, name)
                    editTarget = null
                } else {
                    inventoryVm.addDepartment(adminId, name)
                    showAddDialog = false
                }
            },
        )
    }

    // ── Delete confirm ───────────────────────────────────────────────────────
    deleteTarget?.let { dept ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title   = { Text("Delete department?", fontWeight = FontWeight.Bold) },
            text    = { Text("\"${dept.name}\" will be permanently removed.", color = AppTheme.Ink500) },
            confirmButton = {
                TextButton(onClick = {
                    inventoryVm.deleteDepartment(adminId, dept.id)
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
                title = "Departments",
                subtitle = "${departments.size} TOTAL DEPARTMENTS",
                onBack = onBack,
            )

            // ── Search Bar ───────────────────────────────────────────────────
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 20.dp),
                placeholder = { Text("Search departments...", color = AppTheme.Ink300) },
                leadingIcon = { Icon(Icons.Outlined.Search, null, tint = AppTheme.Ink300) },
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AppTheme.Brand,
                    unfocusedBorderColor = AppTheme.Ink100,
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                ),
                singleLine = true
            )

            // ── List ──────────────────────────────────────────────────────────────
            AppPullToRefresh(
                onRefresh = { inventoryVm.loadForAdmin(adminId) },
                modifier  = Modifier.fillMaxSize(),
            ) {
                if (filteredDepts.isEmpty()) {
                    EmptyState(
                        emoji = if (searchQuery.isBlank()) "🏢" else "🔍",
                        title = if (searchQuery.isBlank()) "No departments yet" else "No matches found",
                        body = if (searchQuery.isBlank()) "Tap the '+' button to add your first department."
                               else "Try a different search term."
                    )
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 120.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        itemsIndexed(filteredDepts, key = { _, d -> d.id }) { index, dept ->
                            StaggeredEntrance(index = index) {
                                DepartmentCard(
                                    department = dept,
                                    onEdit     = { editTarget = dept },
                                    onDelete   = { deleteTarget = dept },
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
private fun DepartmentCard(
    department: Department,
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
            .premiumPress(onClick = {}) // Ripple + Scale effect
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(AppTheme.Brand50),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Category,
                    contentDescription = null,
                    tint = AppTheme.Brand,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = department.name,
                    color = AppTheme.Ink900,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "System Managed", // Optional placeholder subtext
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
private fun DepartmentFormDialog(
    initial: String,
    title: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
) {
    var name  by remember(initial) { mutableStateOf(initial) }
    var error by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        title = {
            Text(title, fontWeight = FontWeight.Bold, color = AppTheme.Ink900, fontSize = 20.sp)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Department Name", color = AppTheme.Ink700, fontSize = 14.sp,
                     fontWeight = FontWeight.Medium)
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it; error = "" },
                    placeholder = { Text("e.g. Electrical", color = AppTheme.Ink300, fontSize = 15.sp) },
                    singleLine = true,
                    isError = error.isNotEmpty(),
                    supportingText = if (error.isNotEmpty()) {{ Text(error, color = AppTheme.Danger, fontSize = 12.sp) }} else null,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words,
                        imeAction = ImeAction.Done,
                    ),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor   = AppTheme.Brand,
                        unfocusedBorderColor = AppTheme.Ink100,
                        focusedLabelColor    = AppTheme.Brand,
                        cursorColor          = AppTheme.Brand,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(AppTheme.Brand)
                    .clickable {
                        if (name.isBlank()) { error = "Name is required"; return@clickable }
                        onSave(name.trim())
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
