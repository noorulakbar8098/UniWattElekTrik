package com.example.uniwattelektrik.feature.admin.presentation.screens

import com.example.uniwattelektrik.core.performance.TrackScreenPerformance

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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import com.example.uniwattelektrik.core.components.AppPullToRefresh
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.FileUpload
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.uniwattelektrik.core.components.InventoryScreenHeader
import com.example.uniwattelektrik.core.components.PremiumGradientFab
import com.example.uniwattelektrik.core.components.PremiumHeaderStatusBarColor
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
    TrackScreenPerformance("AdminInventoryScreen")
    val items by inventoryVm.spareItems.collectAsStateWithLifecycle()
    val lowStock = items.count { it.stockQty < 20 }

    // Search and category state
    var search by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }
    var sort by remember { mutableStateOf(InventorySort.NameAZ) }
    var stockFilter by remember { mutableStateOf(StockFilter.All) }

    val categories = remember(items) {
        listOf("All") + items.map { it.category.ifBlank { "Other" } }
            .distinct()
            .sortedBy { it.lowercase() }
    }

    val displayed = remember(items, selectedCategory, search, sort, stockFilter) {
        items.asSequence().filter { item ->
            val catOk = selectedCategory == "All" ||
                item.category.ifBlank { "Other" } == selectedCategory
            val q = search.trim()
            val textOk = q.isBlank() ||
                item.name.contains(q, ignoreCase = true) ||
                item.category.contains(q, ignoreCase = true) ||
                item.make.contains(q, ignoreCase = true)
            val stockOk = when (stockFilter) {
                StockFilter.All       -> true
                StockFilter.InStock   -> item.stockQty >= 20
                StockFilter.LowStock  -> item.stockQty in 1..19
                StockFilter.OutOfStock -> item.stockQty <= 0
            }
            catOk && textOk && stockOk
        }.toList().let { list ->
            when (sort) {
                InventorySort.NameAZ        -> list.sortedBy { it.name.lowercase() }
                InventorySort.NameZA        -> list.sortedByDescending { it.name.lowercase() }
                InventorySort.StockLowHigh  -> list.sortedBy { it.stockQty }
                InventorySort.StockHighLow  -> list.sortedByDescending { it.stockQty }
                InventorySort.PriceLowHigh  -> list.sortedBy { it.price }
                InventorySort.PriceHighLow  -> list.sortedByDescending { it.price }
            }
        }
    }

    val filtersActive = stockFilter != StockFilter.All ||
        sort != InventorySort.NameAZ ||
        selectedCategory != "All"

    com.example.uniwattelektrik.core.theme.SetStatusBar(
        color = PremiumHeaderStatusBarColor, darkIcons = false,
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
                    Text("Delete", color = AppTheme.Danger, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text("Cancel") }
            },
        )
    }

    Box(modifier = modifier.fillMaxSize().background(appScreenBackground())) {
        Column(modifier = Modifier.fillMaxSize()) {

            // ── Premium Gradient Header ─────────────────────────────────────
            InventoryScreenHeader(
                title = "Inventory",
                subtitle = "${items.size} SPARES · ${categories.size - 1} CATEGORIES · $lowStock LOW",
                trailing = {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color.White.copy(alpha = 0.18f))
                            .clickable(onClick = onManage),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Settings,
                            contentDescription = "Manage",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                },
            )

            // ── Search bar + Sort + Filter ──────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
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

                InventoryToolbarChip(
                    icon = Icons.Filled.SwapVert,
                    contentDesc = "Sort spares",
                    indicatorOn = sort != InventorySort.NameAZ,
                ) { dismiss ->
                    Text(
                        "Sort by",
                        color = AppTheme.Ink500,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    )
                    InventorySort.entries.forEach { option ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    option.label,
                                    color = AppTheme.Ink700,
                                    fontSize = 13.sp,
                                    fontWeight = if (option == sort) FontWeight.SemiBold else FontWeight.Normal,
                                )
                            },
                            trailingIcon = {
                                if (option == sort) {
                                    Icon(
                                        Icons.Filled.Check,
                                        contentDescription = null,
                                        tint = AppTheme.Brand,
                                        modifier = Modifier.size(16.dp),
                                    )
                                }
                            },
                            onClick = {
                                sort = option
                                dismiss()
                            },
                        )
                    }
                }

                InventoryToolbarChip(
                    icon = Icons.Filled.FilterList,
                    contentDesc = "Filter spares",
                    indicatorOn = stockFilter != StockFilter.All || selectedCategory != "All",
                ) { _ ->
                    Text(
                        "Stock",
                        color = AppTheme.Ink500,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    )
                    StockFilter.entries.forEach { option ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    option.label,
                                    color = AppTheme.Ink700,
                                    fontSize = 13.sp,
                                    fontWeight = if (option == stockFilter) FontWeight.SemiBold else FontWeight.Normal,
                                )
                            },
                            trailingIcon = {
                                if (option == stockFilter) {
                                    Icon(
                                        Icons.Filled.Check,
                                        contentDescription = null,
                                        tint = AppTheme.Brand,
                                        modifier = Modifier.size(16.dp),
                                    )
                                }
                            },
                            onClick = { stockFilter = option },
                        )
                    }
                    HorizontalDivider(
                        color = AppTheme.Ink100,
                        modifier = Modifier.padding(vertical = 4.dp),
                    )
                    Text(
                        "Category",
                        color = AppTheme.Ink500,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    )
                    categories.forEach { cat ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    cat,
                                    color = AppTheme.Ink700,
                                    fontSize = 13.sp,
                                    fontWeight = if (cat == selectedCategory) FontWeight.SemiBold else FontWeight.Normal,
                                )
                            },
                            trailingIcon = {
                                if (cat == selectedCategory) {
                                    Icon(
                                        Icons.Filled.Check,
                                        contentDescription = null,
                                        tint = AppTheme.Brand,
                                        modifier = Modifier.size(16.dp),
                                    )
                                }
                            },
                            onClick = { selectedCategory = cat },
                        )
                    }
                }
            }

            // ── Filters active banner ───────────────────────────────────────
            if (filtersActive) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .padding(bottom = 8.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(AppTheme.Brand50)
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Filled.FilterList,
                        contentDescription = null,
                        tint = AppTheme.Brand,
                        modifier = Modifier.size(14.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "Filters active · ${displayed.size} of ${items.size}",
                        color = AppTheme.Brand,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        "Clear all",
                        color = AppTheme.Brand,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .clickable {
                                sort = InventorySort.NameAZ
                                stockFilter = StockFilter.All
                                selectedCategory = "All"
                            }
                            .padding(horizontal = 8.dp, vertical = 2.dp),
                    )
                }
            }

            // ── Category pills ──────────────────────────────────────────────
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

            Spacer(Modifier.height(12.dp))

            // ── Flat item list ──────────────────────────────────────────────
            AppPullToRefresh(
                onRefresh = { inventoryVm.loadForAdmin(adminId) },
                modifier  = Modifier.weight(1f),
            ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 180.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (displayed.isEmpty()) {
                    item {
                        Box(
                            Modifier.fillMaxWidth().padding(top = 40.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text("No spares found", color = AppTheme.Ink300, fontSize = 14.sp)
                        }
                    }
                } else {
                    items(
                        displayed,
                        key = { it.id.ifBlank { "${it.category}_${it.name}_${it.size}_${it.make}" } },
                    ) { item ->
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
            } // AppPullToRefresh
        }

        // ── FABs (hidden during multi-selection) ─────────────────────────────
        if (!selectionMode) {
            // Import Excel — bottom-LEFT
            PremiumGradientFab(
                onClick = onImportExcel,
                icon = Icons.Outlined.FileUpload,
                contentDescription = "Import Excel",
                alignment = Alignment.BottomStart,
            )
            // Add Spare — bottom-RIGHT
            PremiumGradientFab(
                onClick = onAddSpare,
                icon = Icons.Filled.Add,
                contentDescription = "Add Spare",
                alignment = Alignment.BottomEnd,
            )
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

// ─── Sort / filter model ─────────────────────────────────────────────────────

private enum class InventorySort(val label: String) {
    NameAZ("Name (A → Z)"),
    NameZA("Name (Z → A)"),
    StockLowHigh("Stock (Low → High)"),
    StockHighLow("Stock (High → Low)"),
    PriceLowHigh("Price (Low → High)"),
    PriceHighLow("Price (High → Low)"),
}

private enum class StockFilter(val label: String) {
    All("All stock"),
    InStock("In stock (≥ 20)"),
    LowStock("Low stock (< 20)"),
    OutOfStock("Out of stock"),
}

/** Square chip-style button used for sort/filter; wraps the dropdown anchor. */
@Composable
private fun InventoryToolbarChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDesc: String,
    indicatorOn: Boolean,
    menuContent: @Composable (dismiss: () -> Unit) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(14.dp))
                .shadow(2.dp, RoundedCornerShape(14.dp), spotColor = AppTheme.ShadowSm, ambientColor = Color.Transparent)
                .background(if (indicatorOn) AppTheme.Brand50 else Color.White)
                .clickable { expanded = true },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                icon,
                contentDescription = contentDesc,
                tint = if (indicatorOn) AppTheme.Brand else AppTheme.Ink700,
                modifier = Modifier.size(18.dp),
            )
            if (indicatorOn) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(AppTheme.Brand),
                )
            }
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            containerColor = AppTheme.Surface,
        ) {
            menuContent { expanded = false }
        }
    }
}
