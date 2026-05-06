package com.example.uniwattelektrik.feature.admin.presentation.screens.inventory

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
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

@Composable
fun SpareListScreen(
    adminId: String,
    inventoryVm: InventoryViewModel,
    onBack: () -> Unit,
    onAdd: () -> Unit,
    onEdit: (SpareItem) -> Unit,
    onItemClick: (SpareItem) -> Unit = onEdit,
    modifier: Modifier = Modifier,
) {
    SetStatusBar(color = PremiumHeaderStatusBarColor, darkIcons = false)

    LaunchedEffect(adminId) {
        inventoryVm.loadForAdmin(adminId)
    }

    val items by inventoryVm.spareItems.collectAsState()

    // Build the category dropdown dynamically from imported data so users see
    // their REAL categories ("Wire Flexible CU", "Cable Flexible CU", …)
    // rather than the legacy hard-coded pair.
    val categories = remember(items) {
        buildList {
            add("All")
            items.map { it.category.ifBlank { "Uncategorized" } }
                .distinct()
                .sortedBy { it.lowercase() }
                .forEach { add(it) }
        }
    }
    var selectedCategory by remember { mutableStateOf("All") }
    var showCategoryMenu by remember { mutableStateOf(false) }

    val displayed = items.filter { item ->
        selectedCategory == "All" ||
            (item.category.ifBlank { "Uncategorized" } == selectedCategory)
    }

    // ── Multi-selection state ───────────────────────────────────────────────
    val selectedIds = remember { mutableStateMapOf<String, Boolean>() }
    val selectionMode = selectedIds.isNotEmpty()
    var confirmDelete by remember { mutableStateOf(false) }

    // Snap selection back when items change so we don't keep stale ids.
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
        val ids = displayed.mapNotNull { it.id.takeIf(String::isNotBlank) }
        val allSelected = ids.isNotEmpty() && ids.all { selectedIds.containsKey(it) }
        if (allSelected) selectedIds.clear()
        else ids.forEach { selectedIds[it] = true }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete ${selectedIds.size} spares?", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "Selected items will be permanently removed from inventory and any tasks referencing them.",
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


    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = modifier.fillMaxSize().background(appScreenBackground())) {

            InventoryScreenHeader(
                title = "Spare List",
                subtitle = "MASTER SPARES & VENDOR DIRECTORY",
                onBack = onBack,
            )

            // ── Filters ───────────────────────────────────────────────────────────
            LazyRow(
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                item {
                    FilterChip(
                        label    = if (selectedCategory == "All") "All Categories" else selectedCategory,
                        selected = selectedCategory != "All",
                        onExpand = { showCategoryMenu = true },
                    ) {
                        DropdownMenu(
                            expanded = showCategoryMenu,
                            onDismissRequest = { showCategoryMenu = false },
                            modifier = Modifier.background(Color.White).shadow(8.dp, RoundedCornerShape(12.dp))
                        ) {
                            categories.forEach { cat ->
                                DropdownMenuItem(
                                    text = { Text(cat, color = if (selectedCategory == cat) AppTheme.Brand else AppTheme.Ink900, fontSize = 13.sp) },
                                    onClick = {
                                        selectedCategory = cat
                                        showCategoryMenu = false
                                    },
                                )
                            }
                        }
                    }
                }
            }

            // ── List ──────────────────────────────────────────────────────────────
            AppPullToRefresh(
                onRefresh = { inventoryVm.loadForAdmin(adminId) },
                modifier  = Modifier.fillMaxSize(),
            ) {
                if (displayed.isEmpty()) {
                    EmptyState(
                        emoji = "💰",
                        title = "No spares found",
                        body = "Adjust filters or tap the '+' button to create a spare item."
                    )
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 120.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        itemsIndexed(displayed, key = { _, it -> it.id }) { index, item ->
                            StaggeredEntrance(index = index) {
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
            }
        }

        // ── FAB (hidden during multi-selection) ──────────────────────────────
        if (!selectionMode) {
            PremiumGradientFab(onClick = onAdd)
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
                    totalCount = displayed.size,
                    onCancel = { cancelSelection() },
                    onSelectAllToggle = { toggleSelectAll() },
                    onDelete = { confirmDelete = true },
                )
            }
        }
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
