package com.example.uniwattelektrik.feature.admin.presentation.screens.inventory

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.uniwattelektrik.core.theme.AppTheme
import com.example.uniwattelektrik.core.theme.appScreenBackground
import com.example.uniwattelektrik.feature.admin.presentation.InventoryViewModel

/**
 * Hierarchical Excel import preview.
 *
 *  Excel layout (per sheet):
 *  - Column 0 = serial number. Either `\d+` (category marker) or `\d+\.\d+`
 *    (item/variant row).
 *  - Column 1 = name. For category markers it's the category name; for
 *    item rows it's the item name. Multiple item rows can share the same
 *    name → those become variants of the same item.
 *  - Column 2+ = field values mapped via header aliases (Size, Core, HSN,
 *    Make, Price, Vendors, …).
 *
 *  We render a 3-level expandable list: category → item → variants. Storage
 *  remains flat in Firestore (one doc per variant in `spare_items` collection)
 *  to preserve backward-compatibility with task references.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ImportSpareItemsPreviewScreen(
    adminId: String,
    inventoryVm: InventoryViewModel,
    onBack: () -> Unit,
    onSuccess: (Int) -> Unit,
) {
    val sheet = inventoryVm.stagedSheet
    val inProgress by inventoryVm.importInProgress.collectAsStateWithLifecycle()
    val result by inventoryVm.importResult.collectAsStateWithLifecycle()

    LaunchedEffect(result) {
        result?.let { n ->
            inventoryVm.consumeImportResult()
            onSuccess(n)
        }
    }

    if (sheet == null) {
        LaunchedEffect(Unit) { onBack() }
        return
    }

    com.example.uniwattelektrik.core.theme.SetStatusBar(
        color = AppTheme.Bg, darkIcons = true,
    )

    val mapping = remember(sheet.headers, sheet.rows) {
        buildHeaderMapping(sheet.headers, sheet.rows.firstOrNull())
    }
    val drafts = remember(sheet, mapping) {
        parseRows(sheet.rows, mapping)
    }
    val groups = remember(drafts) { groupForDisplay(drafts) }
    val skipped = sheet.rows.size - drafts.size - countCategoryRows(sheet.rows)

    // Category & item expansion state, default-expanded.
    val expandedCats = remember(groups) {
        mutableStateMapOf<String, Boolean>().apply {
            groups.forEach { put(it.category, true) }
        }
    }
    val expandedItems = remember(groups) { mutableStateMapOf<String, Boolean>() }

    Column(modifier = Modifier.fillMaxSize().background(appScreenBackground())) {
        // ── Top bar ─────────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier.size(40.dp).clip(RoundedCornerShape(12.dp))
                    .clickable(onClick = onBack),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Back", tint = AppTheme.Ink900)
            }
            Spacer(Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Preview Import", color = AppTheme.Ink900,
                     fontSize = 18.sp, fontWeight = FontWeight.Bold)
                val totalItems = groups.sumOf { it.items.size }
                Text(
                    "${drafts.size} variants · $totalItems items · ${groups.size} categories" +
                        if (skipped > 0) " · $skipped skipped" else "",
                    color = AppTheme.Ink500, fontSize = 12.sp,
                )
            }
        }

        // ── Empty banner ────────────────────────────────────────────────────
        if (drafts.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxWidth()
                    .padding(horizontal = 12.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(AppTheme.HighBg)
                    .padding(12.dp),
            ) {
                Column {
                    Text("No rows could be imported", color = AppTheme.High,
                         fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Expecting column 0 = serial number ('2', '2.1', …) and column 1 = name. " +
                            "Headers found: ${sheet.headers.joinToString(", ") { it.ifBlank { "—" } }}",
                        color = AppTheme.Ink700, fontSize = 12.sp,
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
        }

        // ── Expandable category → item → variants list ──────────────────────
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            groups.forEach { catGroup ->
                stickyHeader(key = "cat_${catGroup.category}") {
                    CategoryHeader(
                        category = catGroup.category,
                        itemCount = catGroup.items.size,
                        variantCount = catGroup.items.sumOf { it.variants.size },
                        expanded = expandedCats[catGroup.category] == true,
                        onToggle = {
                            expandedCats[catGroup.category] = !(expandedCats[catGroup.category] ?: true)
                        },
                    )
                }
                if (expandedCats[catGroup.category] == true) {
                    catGroup.items.forEach { itemGroup ->
                        val itemKey = "${catGroup.category}::${itemGroup.name}"
                        item(key = "item_$itemKey") {
                            ItemRow(
                                name = itemGroup.name,
                                variantCount = itemGroup.variants.size,
                                expanded = expandedItems[itemKey] == true,
                                onToggle = {
                                    expandedItems[itemKey] = !(expandedItems[itemKey] ?: false)
                                },
                            )
                        }
                        if (expandedItems[itemKey] == true) {
                            itemGroup.variants.forEachIndexed { i, v ->
                                item(key = "var_${itemKey}_$i") { VariantCard(v) }
                            }
                        }
                    }
                }
            }
        }

        // ── Bottom action bar ───────────────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier.weight(1f).height(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(AppTheme.Ink100)
                    .clickable(enabled = !inProgress) {
                        inventoryVm.stagedSheet = null
                        onBack()
                    },
                contentAlignment = Alignment.Center,
            ) {
                Text("Cancel", color = AppTheme.Ink700, fontSize = 14.sp,
                     fontWeight = FontWeight.SemiBold)
            }
            Box(
                modifier = Modifier.weight(1f).height(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (drafts.isEmpty() || inProgress) AppTheme.Brand50 else AppTheme.Brand)
                    .clickable(enabled = drafts.isNotEmpty() && !inProgress) {
                        inventoryVm.bulkImportSpareItems(adminId, drafts)
                    },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = if (drafts.isEmpty()) "Nothing to upload"
                           else "Upload ${drafts.size} variants",
                    color = if (drafts.isEmpty() || inProgress) AppTheme.Brand else Color.White,
                    fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }

    if (inProgress) {
        AlertDialog(
            onDismissRequest = {},
            confirmButton = {},
            title = { Text("Uploading…") },
            text = {
                Column {
                    Text("Pushing rows to Firestore.", fontSize = 13.sp)
                    Spacer(Modifier.height(12.dp))
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
            },
        )
    }
}

/* ────────────────────────────────────────────────────────────────────────── */
/*  UI components                                                             */
/* ────────────────────────────────────────────────────────────────────────── */

@Composable
private fun CategoryHeader(
    category: String,
    itemCount: Int,
    variantCount: Int,
    expanded: Boolean,
    onToggle: () -> Unit,
) {
    val rotation by animateFloatAsState(if (expanded) 0f else -90f, label = "catChev")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(AppTheme.Brand)
            .clickable(onClick = onToggle)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("📦", fontSize = 18.sp)
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(category, color = Color.White, fontSize = 14.sp,
                 fontWeight = FontWeight.Bold)
            Text("$itemCount items · $variantCount variants",
                 color = Color.White.copy(alpha = 0.85f), fontSize = 11.sp)
        }
        Text("▾", color = Color.White, fontSize = 16.sp,
             modifier = Modifier.rotate(rotation))
    }
}

@Composable
private fun ItemRow(
    name: String,
    variantCount: Int,
    expanded: Boolean,
    onToggle: () -> Unit,
) {
    val rotation by animateFloatAsState(if (expanded) 0f else -90f, label = "itemChev")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(AppTheme.Brand50)
            .clickable(onClick = onToggle)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("🔹", fontSize = 14.sp)
        Spacer(Modifier.width(8.dp))
        Text(name, modifier = Modifier.weight(1f),
             color = AppTheme.Ink900, fontSize = 13.sp,
             fontWeight = FontWeight.SemiBold)
        Box(
            modifier = Modifier.clip(CircleShape)
                .background(AppTheme.Brand)
                .padding(horizontal = 8.dp, vertical = 2.dp),
        ) {
            Text("$variantCount", color = Color.White,
                 fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
        }
        Spacer(Modifier.width(8.dp))
        Text("▾", color = AppTheme.Brand, fontSize = 14.sp,
             modifier = Modifier.rotate(rotation))
    }
}

@Composable
private fun VariantCard(item: SpareItem) {
    Row(
        modifier = Modifier.fillMaxWidth()
            .padding(start = 16.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(Color.White)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("→", color = AppTheme.Ink300, fontSize = 14.sp)
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            val specs = listOfNotNull(
                item.size.takeIf { it.isNotBlank() }?.let { "$it sq.mm" },
                item.core.takeIf { it.isNotBlank() }?.let { "$it core" },
                item.currentRating.takeIf { it.isNotBlank() }?.let { "$it A" },
                item.noOfPoles.takeIf { it.isNotBlank() }?.let { "$it pole" },
            ).joinToString(" / ")
            Text(specs.ifBlank { "Default variant" },
                 color = AppTheme.Ink900, fontSize = 13.sp,
                 fontWeight = FontWeight.SemiBold)
            val meta = listOfNotNull(
                item.make.takeIf { it.isNotBlank() },
                item.hsn.takeIf { it.isNotBlank() }?.let { "HSN $it" },
                item.unit.takeIf { it.isNotBlank() },
            ).joinToString(" · ")
            if (meta.isNotBlank()) {
                Text(meta, color = AppTheme.Ink500, fontSize = 11.sp)
            }
            val vendor = listOfNotNull(
                item.vendorName1.takeIf { it.isNotBlank() },
                item.vendorContact1.takeIf { it.isNotBlank() },
            ).joinToString(" · ")
            if (vendor.isNotBlank()) {
                Text(vendor, color = AppTheme.Ink300, fontSize = 11.sp)
            }
        }
        Column(horizontalAlignment = Alignment.End) {
            if (item.price > 0.0) {
                Text("₹${formatPrice(item.price)}",
                     color = AppTheme.Ink900, fontSize = 14.sp,
                     fontWeight = FontWeight.Bold)
            }
            if (item.stockQty > 0) {
                Text("Qty ${item.stockQty}",
                     color = AppTheme.Ink500, fontSize = 11.sp)
            }
        }
    }
}

private fun formatPrice(value: Double): String {
    if (value % 1.0 == 0.0) return value.toLong().toString()
    val rounded = kotlin.math.round(value * 100.0) / 100.0
    return rounded.toString()
}

/* ────────────────────────────────────────────────────────────────────────── */
/*  Display grouping                                                          */
/* ────────────────────────────────────────────────────────────────────────── */

private data class CategoryGroup(
    val category: String,
    val items: List<ItemGroup>,
)

private data class ItemGroup(
    val name: String,
    val variants: List<SpareItem>,
)

private fun groupForDisplay(drafts: List<SpareItem>): List<CategoryGroup> {
    val byCategory = LinkedHashMap<String, LinkedHashMap<String, MutableList<SpareItem>>>()
    drafts.forEach { item ->
        val cat = item.category.ifBlank { "Uncategorized" }
        val nameMap = byCategory.getOrPut(cat) { LinkedHashMap() }
        nameMap.getOrPut(item.name) { mutableListOf() }.add(item)
    }
    return byCategory.map { (cat, nameMap) ->
        CategoryGroup(
            category = cat,
            items = nameMap.map { (name, variants) -> ItemGroup(name, variants) },
        )
    }
}

private fun countCategoryRows(rows: List<List<String>>): Int =
    rows.count { normalizeSerial(it.firstOrNull().orEmpty()).matches(CATEGORY_RX) }

/* ────────────────────────────────────────────────────────────────────────── */
/*  Hierarchical row walker                                                   */
/* ────────────────────────────────────────────────────────────────────────── */

private val CATEGORY_RX = Regex("^\\d+$")
private val ITEM_RX     = Regex("^\\d+(?:\\.\\d+)+$")
private val TRAILING_ZERO_DECIMAL_RX = Regex("\\.0+$")

/** Matches "2 Copper Wire" / "10  Cable Flexible CU" — number + space + label
 *  all in one cell (common when Excel users merge cells or type freely). */
private val INLINE_CATEGORY_RX = Regex("^(\\d+)\\s+(.+)$")

/**
 * Normalises a serial-number cell coming out of fastexcel.
 *
 *  Excel numeric cells lose integer formatting once read as raw values:
 *  a category row whose col 0 is the integer `1` arrives here as `"1.0"`
 *  (or `"2.00"`). Without stripping the trailing zero-fraction, `"1.0"`
 *  would match [ITEM_RX] and be treated as an item, breaking category
 *  detection. We collapse `1.0`, `2.00`, `3.000` → `1`, `2`, `3`.
 *
 *  Real decimal serials like `1.1`, `2.34` are left untouched.
 */
private fun normalizeSerial(raw: String): String {
    val t = raw.trim()
    return TRAILING_ZERO_DECIMAL_RX.replace(t, "")
}

/**
 * Picks the best label for a category-marker row.
 *
 *  Order of preference:
 *   1. Inline category text in col 0 itself (`"2 Copper Wire"` → `"Copper Wire"`).
 *   2. Col 1, if it looks like a real label (non-blank, non-serial,
 *      not a known vendor/spec field column).
 *   3. First subsequent cell that looks like a real label.
 *
 *  Returns `null` if no plausible label is found — caller should keep the
 *  previously-active category in that case.
 */
private fun resolveCategoryLabel(
    row: List<String>,
    rawCol0: String,
    mapping: HeaderMap,
): String? {
    // 1. Inline form: "2 Copper Wire" — extract everything after the number.
    INLINE_CATEGORY_RX.matchEntire(rawCol0.trim())?.let { m ->
        val inline = m.groupValues[2].trim()
        if (inline.isNotBlank() && !inline.matches(SERIAL_LIKE_RX)) return inline
    }

    // Indices we should NEVER pick up as a category — they are field columns
    // (vendor / size / price / hsn / …) detected during header mapping.
    val fieldIndices = setOfNotNull(
        mapping.size.takeIf { it >= 0 },
        mapping.core.takeIf { it >= 0 },
        mapping.currentRating.takeIf { it >= 0 },
        mapping.noOfPoles.takeIf { it >= 0 },
        mapping.unit.takeIf { it >= 0 },
        mapping.price.takeIf { it >= 0 },
        mapping.stockQty.takeIf { it >= 0 },
        mapping.hsn.takeIf { it >= 0 },
        mapping.make.takeIf { it >= 0 },
        mapping.vendorName1.takeIf { it >= 0 },
        mapping.vendorGst1.takeIf { it >= 0 },
        mapping.vendorContact1.takeIf { it >= 0 },
        mapping.vendorAddress1.takeIf { it >= 0 },
        mapping.vendorName2.takeIf { it >= 0 },
        mapping.vendorGst2.takeIf { it >= 0 },
        mapping.vendorContact2.takeIf { it >= 0 },
        mapping.vendorAddress2.takeIf { it >= 0 },
        mapping.vendorLocation.takeIf { it >= 0 },
    )

    // 2. col 1 (the canonical place — most common case).
    val col1 = row.getOrNull(1)?.trim().orEmpty()
    if (col1.isNotBlank() && !col1.matches(SERIAL_LIKE_RX) && 1 !in fieldIndices) {
        return col1
    }

    // 3. Scan forward for the first non-blank, non-serial cell that isn't a
    //    known field column. This rescues sheets where the category label sits
    //    in col 2/3 because col 1 was reserved for "Item Name".
    for (i in row.indices) {
        if (i <= 0 || i in fieldIndices) continue
        val v = row[i].trim()
        if (v.isNotBlank() && !v.matches(SERIAL_LIKE_RX)) return v
    }
    return null
}

/**
 * Walks rows top-to-bottom maintaining `currentCategory`.
 *
 *  - `^\d+$`        in col 0 → category marker; col 1 holds the new category.
 *                              The row itself is NOT emitted as a SpareItem.
 *  - `^\d+\.\d+$`   in col 0 → item/variant row; emits a SpareItem under the
 *                              currently-active category.
 *  - blank col 0    but non-blank col 1 → fallback item under current category.
 *  - everything else is silently skipped.
 */
private fun parseRows(
    rows: List<List<String>>,
    mapping: HeaderMap,
): List<SpareItem> {
    val out = ArrayList<SpareItem>(rows.size)
    // Sanitize the seed: if `mapping.sheetCategory` somehow became "1" / "1.2"
    // (mis-detected sheet), treat it as blank so we wait for a real category row.
    var currentCategory = mapping.sheetCategory.takeUnless { it.matches(SERIAL_LIKE_RX) }
        .orEmpty()

    for (row in rows) {
        val rawCol0 = row.getOrNull(0)?.trim().orEmpty()
        val col0    = normalizeSerial(rawCol0)
        val col1    = row.getOrNull(1)?.trim().orEmpty()
        when {
            // Plain integer in col 0 → category marker.
            col0.matches(CATEGORY_RX) -> {
                resolveCategoryLabel(row, rawCol0, mapping)
                    ?.let { currentCategory = it }
                // category marker — skip emission
            }
            // Inline category form: col 0 = "2 Copper Wire" all in one cell.
            INLINE_CATEGORY_RX.matchEntire(rawCol0) != null && col1.isBlank() -> {
                resolveCategoryLabel(row, rawCol0, mapping)
                    ?.let { currentCategory = it }
            }
            col0.matches(ITEM_RX) -> {
                rowToSpareItem(row, mapping, currentCategory)?.let(out::add)
            }
            col0.isBlank() && col1.isNotBlank() -> {
                // tolerant fallback: row with no serial but with a name
                rowToSpareItem(row, mapping, currentCategory)?.let(out::add)
            }
            // else: discard noise rows
        }
    }
    return out
}

/* ────────────────────────────────────────────────────────────────────────── */
/*  Header → SpareItem field mapping                                          */
/* ────────────────────────────────────────────────────────────────────────── */

private data class HeaderMap(
    /** True when sheet uses hierarchical layout (col 0 = serial, col 1 = name). */
    val hierarchical: Boolean = false,
    /** Sheet-wide category from header[0] when only one category in the sheet. */
    val sheetCategory: String = "",
    val name: Int = -1,
    val category: Int = -1,
    val make: Int = -1,
    val size: Int = -1,
    val core: Int = -1,
    val currentRating: Int = -1,
    val noOfPoles: Int = -1,
    val unit: Int = -1,
    val price: Int = -1,
    val stockQty: Int = -1,
    val hsn: Int = -1,
    val vendorName1: Int = -1,
    val vendorGst1: Int = -1,
    val vendorContact1: Int = -1,
    val vendorAddress1: Int = -1,
    val vendorName2: Int = -1,
    val vendorGst2: Int = -1,
    val vendorContact2: Int = -1,
    val vendorAddress2: Int = -1,
    val vendorLocation: Int = -1,
)

private fun normalize(s: String): String = s.trim().lowercase()
    .replace(Regex("[\\-_().,/\\\\]+"), " ")
    .replace(Regex("\\s+"), " ")
    .trim()

private fun buildHeaderMapping(headers: List<String>, firstDataRow: List<String>?): HeaderMap {
    if (headers.isEmpty()) return HeaderMap()

    val norm = headers.map { normalize(it) }

    fun find(vararg aliases: String): Int {
        val normAliases = aliases.map { normalize(it) }
        for (alias in normAliases) {
            val i = norm.indexOf(alias)
            if (i >= 0) return i
        }
        return -1
    }

    // Detect hierarchical layout: header[0] is a category-like word AND first
    // data row's col 0 looks like a serial. If only ONE category exists in the
    // whole sheet we'll fall back on header[0] as the sheet-wide category.
    val knownFieldAliases = setOf(
        "name", "item", "item name", "description", "spare name", "category", "type",
        "make", "brand", "manufacturer", "size", "price", "rate", "cost", "amount",
        "hsn", "stock", "qty", "quantity", "stock qty", "stock quantity", "unit", "uom",
        "core", "rating", "amps", "poles", "vendor", "gst", "contact", "address", "location",
        "s no", "sno", "sr no", "srno", "sl no", "slno", "sr", "serial", "#", "no",
    )
    val firstHeader = norm.firstOrNull().orEmpty()
    val firstHeaderIsCategoryLike = firstHeader.isNotBlank() &&
        firstHeader !in knownFieldAliases &&
        knownFieldAliases.none { firstHeader.contains(it) }
    val firstData = normalizeSerial(firstDataRow?.firstOrNull().orEmpty())
    val firstDataIsSerial = firstData.matches(SERIAL_LIKE_RX)
    val hierarchical = firstHeaderIsCategoryLike && firstDataIsSerial && headers.size >= 3

    // When fastexcel reads a sheet that has NO field-header row, the very
    // first data row (a category marker like "1 | Wire Flexible CU | …")
    // gets consumed as the "header row". In that case headers[0] is a bare
    // serial like "1" or "1.0", and the real category title is in headers[1].
    //
    // RULE: if headers[0] looks like a serial number → headers[1] is the
    // category, no questions asked. Never substitute it with anything else
    // (including "looks like a field header" heuristics) — those false-rejected
    // legitimate category names like "Cable Locator" or "Vendor Equipment".
    val rawSheetCategory = headers.firstOrNull()?.trim().orEmpty()
    val normalizedFirst = normalizeSerial(rawSheetCategory)
    val resolvedSheetCategory = when {
        !hierarchical -> ""
        normalizedFirst.matches(SERIAL_LIKE_RX) && headers.size > 1 ->
            headers[1].trim().ifBlank { rawSheetCategory }
        else -> rawSheetCategory
    }

    return HeaderMap(
        hierarchical   = hierarchical,
        sheetCategory  = resolvedSheetCategory,
        // Name col always = column 1 in hierarchical mode (col 0 is serial).
        name           = if (hierarchical) 1 else findNameIdx(norm, ::find, headers.size),
        category       = if (hierarchical) -1 else find("category", "type"),
        make           = find("make", "brand", "manufacturer"),
        size           = find("size", "size sq mm", "size sqmm", "sq mm", "sqmm", "size mm"),
        core           = find("core", "cores", "no of cores"),
        currentRating  = find("currentRating", "current rating", "rating", "amps", "amp", "current"),
        noOfPoles      = find("noOfPoles", "poles", "no of poles", "pole"),
        unit           = find("unit", "uom", "units"),
        price          = find("price", "unit price", "rate", "cost", "amount"),
        stockQty       = find("stockQty", "stock qty", "stock quantity", "quantity",
                              "qty", "stock", "in stock"),
        hsn            = find("hsn", "hsn code", "hsn no"),
        vendorName1    = find("vendorName1", "vendor name 1", "vendor name1",
                              "vendor 1", "vendor", "vendor name"),
        vendorContact1 = find("vendorContact1", "vendor contact 1", "vendor contact number 1",
                              "vendor contact number", "vendor contact",
                              "vendor phone 1", "vendor phone", "contact 1", "contact"),
        vendorGst1     = find("vendorGst1", "vendor gst 1", "vendor gst no 1",
                              "vendor gst", "gst 1", "gst"),
        vendorAddress1 = find("vendorAddress1", "vendor address 1",
                              "vendor address", "address 1", "address"),
        vendorName2    = find("vendorName2", "vendor name 2", "vendor name2", "vendor 2"),
        vendorContact2 = find("vendorContact2", "vendor contact 2", "vendor contact number 2",
                              "vendor phone 2", "contact 2"),
        vendorGst2     = find("vendorGst2", "vendor gst 2", "gst 2"),
        vendorAddress2 = find("vendorAddress2", "vendor address 2", "address 2"),
        vendorLocation = find("vendorLocation", "vendor location 1", "vendor location",
                              "location 1", "location"),
    )
}

private fun findNameIdx(
    norm: List<String>,
    find: (Array<out String>) -> Int,
    headerSize: Int,
): Int {
    val nameAliases = arrayOf(
        "name", "item", "item name", "itemname", "description",
        "spare name", "spare", "product", "product name",
        "particulars", "material", "material name",
    )
    val serialAliases = setOf(
        "s no", "sno", "sr no", "srno", "sl no", "slno", "sr",
        "serial", "serial no", "serial number", "#", "no", "number",
        "item no", "item number", "itemno",     // these LOOK item-like but hold serials
    )
    val explicitName  = find(nameAliases)
    val partialName   = norm.indexOfFirst { idx ->
        (idx.contains("name") || idx.contains("item")) && idx !in serialAliases
    }
    val firstNonSerial = norm.indexOfFirst { it.isNotBlank() && it !in serialAliases }
    val resolved = when {
        explicitName  >= 0 -> explicitName
        partialName   >= 0 -> partialName
        firstNonSerial >= 0 -> firstNonSerial
        headerSize >= 2 -> 1
        else -> 0
    }
    // Defensive: never pick col 0 if there's a col 1 — col 0 almost always
    // holds serial numbers in real-world inventory sheets.
    return if (resolved == 0 && headerSize >= 2) 1 else resolved
}

private val SERIAL_LIKE_RX = Regex("^\\d+(?:\\.\\d+)*$")

private fun rowToSpareItem(
    row: List<String>,
    m: HeaderMap,
    currentCategory: String,
): SpareItem? {
    fun get(idx: Int): String = if (idx >= 0 && idx in row.indices) row[idx] else ""

    // Resolve a real item name. The chosen name column might point at a
    // serial-number cell ("1", "1.1") if the header was ambiguous (e.g.
    // "Item No.") — in that case scan forward for the first non-blank,
    // non-serial cell so the user never sees "1" as the item title.
    var name = get(m.name).trim()
    if (name.isBlank() || name.matches(SERIAL_LIKE_RX)) {
        val start = (if (m.name >= 0) m.name else -1) + 1
        for (i in start.coerceAtLeast(0) until row.size) {
            val cell = row[i].trim()
            if (cell.isNotBlank() && !cell.matches(SERIAL_LIKE_RX)) {
                name = cell
                break
            }
        }
    }
    if (name.isBlank() || name.matches(SERIAL_LIKE_RX)) return null

    // Hierarchical sheets always provide the category via the most-recent
    // `1`, `2`, `3` row (handled by `parseRows` → `currentCategory`).
    // The fallbacks below only fire for malformed sheets where an item row
    // appeared with no preceding category marker.
    val rawCategory = currentCategory.ifBlank {
        if (m.category >= 0) get(m.category).ifBlank { "Uncategorized" }
        else m.sheetCategory.ifBlank { "Uncategorized" }
    }
    // Final guard: never let a bare serial number end up as the category title.
    val category = if (rawCategory.matches(SERIAL_LIKE_RX)) "Uncategorized" else rawCategory

    val priceText = get(m.price).replace(",", "").trim()
    val qtyText   = get(m.stockQty).replace(",", "").trim()

    return SpareItem(
        id             = "",
        category       = category,
        name           = name,
        make           = get(m.make),
        size           = get(m.size),
        core           = get(m.core),
        currentRating  = get(m.currentRating),
        noOfPoles      = get(m.noOfPoles),
        unit           = get(m.unit),
        price          = priceText.toDoubleOrNull() ?: 0.0,
        stockQty       = qtyText.toIntOrNull()
            ?: qtyText.toDoubleOrNull()?.toInt()
            ?: 0,
        hsn            = get(m.hsn),
        vendorName1    = get(m.vendorName1),
        vendorGst1     = get(m.vendorGst1),
        vendorContact1 = get(m.vendorContact1),
        vendorAddress1 = get(m.vendorAddress1),
        vendorName2    = get(m.vendorName2),
        vendorGst2     = get(m.vendorGst2),
        vendorContact2 = get(m.vendorContact2),
        vendorAddress2 = get(m.vendorAddress2),
        vendorLocation = get(m.vendorLocation),
    )
}
