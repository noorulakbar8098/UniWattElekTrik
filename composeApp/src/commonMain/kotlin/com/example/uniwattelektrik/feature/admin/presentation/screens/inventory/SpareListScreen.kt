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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.text.TextStyle
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
    TrackScreenPerformance("SpareListScreen")
    SetStatusBar(color = PremiumHeaderStatusBarColor, darkIcons = false)

    LaunchedEffect(adminId) {
        inventoryVm.loadForAdmin(adminId)
    }

    val items by inventoryVm.spareItems.collectAsState()

    // Build category list dynamically
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
    var search by remember { mutableStateOf("") }

    val displayed = items.filter { item ->
        val catOk = selectedCategory == "All" ||
            (item.category.ifBlank { "Uncategorized" } == selectedCategory)
        val q = search.trim()
        val textOk = q.isBlank() ||
            item.name.contains(q, ignoreCase = true) ||
            item.category.contains(q, ignoreCase = true) ||
            item.make.contains(q, ignoreCase = true)
        catOk && textOk
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
                    Text("Delete", color = AppTheme.Danger, fontWeight = FontWeight.SemiBold)
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

            // ── Search bar ────────────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .shadow(2.dp, RoundedCornerShape(14.dp), spotColor = AppTheme.ShadowSm, ambientColor = Color.Transparent)
                    .background(Color.White)
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Search,
                    contentDescription = null,
                    tint = AppTheme.Ink300,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(8.dp))
                BasicTextField(
                    value = search,
                    onValueChange = { search = it },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    textStyle = TextStyle(
                        color = AppTheme.Ink900,
                        fontSize = 13.sp,
                    ),
                    decorationBox = { inner ->
                        if (search.isEmpty()) {
                            Text("Search spares…", color = AppTheme.Ink300, fontSize = 13.sp)
                        }
                        inner()
                    },
                )
                if (search.isNotEmpty()) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = null,
                        tint = AppTheme.Ink300,
                        modifier = Modifier.size(16.dp).clickable { search = "" },
                    )
                }
            }

            // ── Category pills ────────────────────────────────────────────────
            LazyRow(
                contentPadding = PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(categories) { cat ->
                    val selected = cat == selectedCategory
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .then(
                                if (!selected)
                                    Modifier.shadow(2.dp, RoundedCornerShape(999.dp), spotColor = AppTheme.ShadowSm)
                                else
                                    Modifier
                            )
                            .background(if (selected) AppTheme.Brand else Color.White)
                            .clickable { selectedCategory = cat }
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                    ) {
                        Text(
                            cat,
                            color = if (selected) Color.White else AppTheme.Ink700,
                            fontSize = 13.sp,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

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
