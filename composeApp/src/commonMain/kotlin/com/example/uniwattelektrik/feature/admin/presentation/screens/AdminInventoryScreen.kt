package com.example.uniwattelektrik.feature.admin.presentation.screens

import com.example.uniwattelektrik.core.theme.appScreenBackground

import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.uniwattelektrik.core.components.AppCard
import com.example.uniwattelektrik.core.theme.AppTheme
import com.example.uniwattelektrik.feature.admin.presentation.InventoryViewModel
import com.example.uniwattelektrik.feature.admin.presentation.screens.inventory.MinimalSpareCard
import com.example.uniwattelektrik.feature.admin.presentation.screens.inventory.SelectionActionBar
import com.example.uniwattelektrik.feature.admin.presentation.screens.inventory.SpareItem
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AdminInventoryScreen(
    inventoryVm: InventoryViewModel,
    adminId: String,
    onManage: () -> Unit,
    onAddSpare: () -> Unit,
    onImportExcel: () -> Unit,
    onItemClick: (SpareItem) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val items by inventoryVm.spareItems.collectAsStateWithLifecycle()
    val lowStock = items.count { it.stockQty < 20 } // Threshold for demo

    // Group items by category, preserving the order they were added in.
    val grouped: List<Pair<String, List<SpareItem>>> = remember(items) {
        val map = LinkedHashMap<String, MutableList<SpareItem>>()
        items.forEach { item ->
            val key = item.category.ifBlank { "Uncategorized" }
            map.getOrPut(key) { mutableListOf() }.add(item)
        }
        // Sort categories alphabetically for predictability; keep item order inside each.
        map.toList().sortedBy { it.first.lowercase() }
    }

    com.example.uniwattelektrik.core.theme.SetStatusBar(
        color = AppTheme.Bg, darkIcons = true,
    )

    // ── Multi-selection state ───────────────────────────────────────────────
    val selectedIds = remember { mutableStateMapOf<String, Boolean>() }
    val selectionMode = selectedIds.isNotEmpty()
    var confirmDelete by remember { mutableStateOf(false) }

    LaunchedEffect(items) {
        val live = items.mapNotNull { it.id.takeIf(String::isNotBlank) }.toSet()
        selectedIds.keys.retainAll { it in live }
    }

    fun toggle(id: String) {
        if (id.isBlank()) return
        if (selectedIds.containsKey(id)) selectedIds.remove(id) else selectedIds[id] = true
    }
    fun cancelSelection() = selectedIds.clear()
    fun toggleSelectAll() {
        val ids = items.mapNotNull { it.id.takeIf(String::isNotBlank) }
        val all = ids.isNotEmpty() && ids.all { selectedIds.containsKey(it) }
        if (all) selectedIds.clear() else ids.forEach { selectedIds[it] = true }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete ${selectedIds.size} spares?", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "Selected items will be permanently removed.",
                    color = AppTheme.Ink500,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val ids = selectedIds.keys.toList()
                    confirmDelete = false
                    cancelSelection()
                    inventoryVm.bulkDeleteSpareItems(adminId, ids)
                }) {
                    Text("Delete", color = AppTheme.High, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text("Cancel") }
            },
        )
    }

    Box(modifier = modifier.fillMaxSize().background(appScreenBackground())) {
        Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(20.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Inventory",
                        style = com.example.uniwattelektrik.core.theme.AppTypography.HeaderTitle
                            .copy(color = AppTheme.Ink900),
                    )
                    Text(
                        "${items.size} Spares · ${grouped.size} categories · $lowStock low",
                        color = AppTheme.Ink500, fontSize = 12.sp,
                    )
                }
                androidx.compose.material3.IconButton(onClick = onManage) {
                    androidx.compose.material3.Icon(
                        imageVector = Icons.Outlined.Settings,
                        contentDescription = "Manage",
                        tint = AppTheme.Ink700,
                    )
                }
                Spacer(Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .height(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(AppTheme.Brand50)
                        .clickable(onClick = onImportExcel)
                        .padding(horizontal = 14.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "⬆ Import Excel", color = AppTheme.Brand, fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                Spacer(Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .height(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(AppTheme.Brand)
                        .clickable(onClick = onAddSpare)
                        .padding(horizontal = 14.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "+ Add Spare", color = Color.White, fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            grouped.forEach { (category, catItems) ->
                stickyHeader(key = "h_$category") {
                    InventoryCategoryHeader(
                        category = category,
                        count    = catItems.size,
                        lowCount = catItems.count { it.stockQty < 20 },
                    )
                }
                items(catItems, key = { it.id.ifBlank { "${category}_${it.name}_${it.size}_${it.make}" } }) { item ->
                    MinimalSpareCard(
                        item = item,
                        selectionMode = selectionMode,
                        selected = selectedIds.containsKey(item.id),
                        onClick = {
                            if (selectionMode) toggle(item.id)
                            else onItemClick(item)
                        },
                        onLongClick = { toggle(item.id) },
                    )
                }
            }
        }
        }

        // ── Multi-selection action bar ───────────────────────────────────────
        if (selectionMode) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 24.dp),
            ) {
                SelectionActionBar(
                    selectedCount = selectedIds.size,
                    totalCount = items.size,
                    onCancel = { cancelSelection() },
                    onSelectAllToggle = { toggleSelectAll() },
                    onDelete = { confirmDelete = true },
                )
            }
        }
    }
}

@Composable
private fun InventoryCategoryHeader(category: String, count: Int, lowCount: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(AppTheme.Brand50)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = category,
            modifier = Modifier.weight(1f),
            color = AppTheme.Brand,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
        )
        if (lowCount > 0) {
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(AppTheme.HighBg)
                    .padding(horizontal = 8.dp, vertical = 2.dp),
            ) {
                Text(
                    "$lowCount low",
                    color = AppTheme.High,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Spacer(Modifier.width(6.dp))
        }
        Box(
            modifier = Modifier
                .clip(CircleShape)
                .background(AppTheme.Brand)
                .padding(horizontal = 10.dp, vertical = 3.dp),
        ) {
            Text(
                text = "$count",
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun InventoryRow(item: SpareItem, deptName: String) {
    val low = item.stockQty < 20
    AppCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (low) AppTheme.HighBg else AppTheme.Brand50),
                contentAlignment = Alignment.Center,
            ) { Text(if (low) "⚠️" else "📦", fontSize = 20.sp) }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(item.name, color = AppTheme.Ink900, fontSize = 14.sp,
                     fontWeight = FontWeight.SemiBold)
                Text("${item.make} · $deptName", color = AppTheme.Ink500, fontSize = 11.sp)
                Text("HSN: ${item.hsn}", color = AppTheme.Ink300, fontSize = 11.sp)
            }
            Column(horizontalAlignment = Alignment.End,
                   verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("${item.stockQty}",
                     color = if (low) AppTheme.High else AppTheme.Ink900,
                     fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(if (low) AppTheme.HighBg else AppTheme.LowBg)
                        .padding(horizontal = 8.dp, vertical = 2.dp),
                ) {
                    Text(if (low) "Low" else "OK",
                         color    = if (low) AppTheme.High else AppTheme.Low,
                         fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}
