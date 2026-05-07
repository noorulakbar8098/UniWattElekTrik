package com.example.uniwattelektrik.feature.admin.presentation.screens.inventory

import com.example.uniwattelektrik.core.performance.TrackScreenPerformance

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.material.icons.outlined.AddBusiness
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Receipt
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material.icons.outlined.SyncAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.uniwattelektrik.core.theme.AppTheme
import com.example.uniwattelektrik.core.theme.SetStatusBar
import com.example.uniwattelektrik.core.theme.appScreenBackground

/**
 * Premium master-detail screen for a single [SpareItem].
 *
 *  Sections:
 *   1. Gradient header — name, category chip, status chip, edit/delete actions.
 *   2. Price & Stock — large highlighted figures with color feedback.
 *   3. Specifications — 2-column grid (make / size / core / rating / poles / HSN).
 *   4. Vendor — collapsible section, primary + optional secondary vendor.
 *   5. Actions — update stock / edit / add vendor (all delegate via callbacks).
 *
 *  This screen is **pure UI** — no Firestore / backend logic. All mutations are
 *  forwarded to caller-supplied lambdas so the host (`AdminShell`) can route to
 *  the existing form / VM.
 */
@Composable
fun SpareItemDetailScreen(
    item: SpareItem,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onUpdateStock: () -> Unit = onEdit,
    onAddVendor: () -> Unit = onEdit,
    modifier: Modifier = Modifier,
) {
    TrackScreenPerformance("SpareItemDetailScreen")
    SetStatusBar(color = AppTheme.Brand, darkIcons = false)

    val status = item.stockStatus()
    var deleteOpen by remember { mutableStateOf(false) }
    var vendorExpanded by remember { mutableStateOf(true) }

    if (deleteOpen) {
        AlertDialog(
            onDismissRequest = { deleteOpen = false },
            title = { Text("Delete spare?", fontWeight = FontWeight.Bold) },
            text = { Text("\"${item.name}\" will be permanently removed.", color = AppTheme.Ink500) },
            confirmButton = {
                TextButton(onClick = {
                    deleteOpen = false
                    onDelete()
                }) { Text("Delete", color = AppTheme.Danger, fontWeight = FontWeight.SemiBold) }
            },
            dismissButton = { TextButton(onClick = { deleteOpen = false }) { Text("Cancel") } },
        )
    }

    Column(modifier = modifier.fillMaxSize().background(appScreenBackground())) {
        DetailGradientHeader(
            item = item,
            status = status,
            onBack = onBack,
            onEdit = onEdit,
            onDelete = { deleteOpen = true },
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item { PriceStockSection(item, status) }
            item { SpecificationsSection(item) }
            item {
                VendorSection(
                    item = item,
                    expanded = vendorExpanded,
                    onToggle = { vendorExpanded = !vendorExpanded },
                )
            }
            item {
                ActionsSection(
                    onUpdateStock = onUpdateStock,
                    onEdit = onEdit,
                    onAddVendor = onAddVendor,
                )
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

/* ── Section 1 — Gradient header ────────────────────────────────────────── */

@Composable
private fun DetailGradientHeader(
    item: SpareItem,
    status: StockStatus,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(12.dp, RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))
            .clip(RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(AppTheme.Brand, Color(0xFF0A4FAA)),
                ),
            ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(horizontal = 20.dp)
                .padding(top = 12.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            // Top action row
            Row(verticalAlignment = Alignment.CenterVertically) {
                HeaderIcon(Icons.AutoMirrored.Outlined.ArrowBack, onClick = onBack)
                Spacer(Modifier.weight(1f))
                HeaderIcon(Icons.Outlined.Edit, onClick = onEdit)
                Spacer(Modifier.width(10.dp))
                HeaderIcon(Icons.Outlined.Delete, onClick = onDelete)
            }

            // Title block
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = item.name,
                    color = Color.White,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.5).sp,
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    HeaderChip(
                        text = item.category.ifBlank { "Uncategorized" },
                        bg = Color.White.copy(alpha = 0.18f),
                        fg = Color.White,
                    )
                    HeaderChip(
                        text = status.label(),
                        bg = Color.White,
                        fg = status.color(),
                    )
                }
            }
        }
    }
}

@Composable
private fun HeaderIcon(icon: ImageVector, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.15f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, null, tint = Color.White, modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun HeaderChip(text: String, bg: Color, fg: Color) {
    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(bg)
            .padding(horizontal = 12.dp, vertical = 5.dp),
    ) {
        Text(text, color = fg, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}

/* ── Section 2 — Price & Stock ──────────────────────────────────────────── */

@Composable
private fun PriceStockSection(item: SpareItem, status: StockStatus) {
    SoftCard {
        Row(modifier = Modifier.fillMaxWidth()) {
            HighlightTile(
                label = "Unit Price",
                value = if (item.price > 0.0) "₹${formatRupees(item.price)}" else "—",
                accent = AppTheme.Brand,
                bg = AppTheme.Brand50,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(12.dp))
            HighlightTile(
                label = "In Stock",
                value = "${item.stockQty}",
                suffix = "units",
                accent = status.color(),
                bg = status.bgColor(),
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun HighlightTile(
    label: String,
    value: String,
    accent: Color,
    bg: Color,
    modifier: Modifier = Modifier,
    suffix: String? = null,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(bg)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            label.uppercase(),
            color = accent.copy(alpha = 0.75f),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
        )
        Row(verticalAlignment = Alignment.Bottom) {
            Text(value, color = accent, fontSize = 26.sp, fontWeight = FontWeight.Bold)
            if (suffix != null) {
                Spacer(Modifier.width(4.dp))
                Text(
                    suffix,
                    color = accent.copy(alpha = 0.85f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(bottom = 4.dp),
                )
            }
        }
    }
}

/* ── Section 3 — Specifications grid ────────────────────────────────────── */

@Composable
private fun SpecificationsSection(item: SpareItem) {
    val specs = listOfNotNull(
        ("Make"   to item.make).takeIfValue(),
        ("Size"   to item.size).takeIfValue(),
        ("Core"   to item.core).takeIfValue(),
        ("Rating" to item.currentRating).takeIfValue(),
        ("Poles"  to item.noOfPoles).takeIfValue(),
        ("HSN"    to item.hsn).takeIfValue(),
        ("Unit"   to item.unit).takeIfValue(),
    )
    SoftCard {
        SectionTitle(icon = Icons.Outlined.Receipt, title = "Specifications")
        Spacer(Modifier.height(14.dp))
        if (specs.isEmpty()) {
            Text(
                "No specifications recorded.",
                color = AppTheme.Ink500, fontSize = 12.sp,
            )
        } else {
            // 2-column grid emulated with chunked rows
            specs.chunked(2).forEachIndexed { idx, pair ->
                if (idx > 0) Spacer(Modifier.height(10.dp))
                Row(modifier = Modifier.fillMaxWidth()) {
                    SpecCell(pair[0].first, pair[0].second, Modifier.weight(1f))
                    Spacer(Modifier.width(10.dp))
                    if (pair.size > 1) {
                        SpecCell(pair[1].first, pair[1].second, Modifier.weight(1f))
                    } else {
                        Box(Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun SpecCell(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(AppTheme.Bg)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            label.uppercase(),
            color = AppTheme.Ink300,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.8.sp,
        )
        Text(value, color = AppTheme.Ink900, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}

private fun Pair<String, String>.takeIfValue(): Pair<String, String>? =
    if (second.isNotBlank()) this else null

/* ── Section 4 — Vendor (expandable) ────────────────────────────────────── */

@Composable
private fun VendorSection(
    item: SpareItem,
    expanded: Boolean,
    onToggle: () -> Unit,
) {
    val hasV1 = item.vendorName1.isNotBlank() ||
        item.vendorContact1.isNotBlank() ||
        item.vendorAddress1.isNotBlank()
    val hasV2 = item.vendorName2.isNotBlank() ||
        item.vendorContact2.isNotBlank() ||
        item.vendorAddress2.isNotBlank()
    val rotation by animateFloatAsState(if (expanded) 0f else -90f, label = "vendChev")

    SoftCard {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onToggle),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Outlined.Storefront,
                null,
                tint = AppTheme.Brand,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Vendor Information",
                    color = AppTheme.Ink900,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    when {
                        hasV1 && hasV2 -> "2 vendors"
                        hasV1          -> "1 vendor"
                        else           -> "No vendor added"
                    },
                    color = AppTheme.Ink500, fontSize = 11.sp,
                )
            }
            Icon(
                Icons.Outlined.ExpandMore, null, tint = AppTheme.Ink500,
                modifier = Modifier.rotate(rotation),
            )
        }

        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut(),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Spacer(Modifier.height(12.dp))
                if (!hasV1 && !hasV2) {
                    Text(
                        "Tap “Add vendor” below to record a supplier.",
                        color = AppTheme.Ink500,
                        fontSize = 12.sp,
                    )
                } else {
                    if (hasV1) VendorCard(
                        index = 1,
                        name = item.vendorName1,
                        contact = item.vendorContact1,
                        address = item.vendorAddress1,
                        gst = item.vendorGst1,
                        location = item.vendorLocation,
                    )
                    if (hasV2) VendorCard(
                        index = 2,
                        name = item.vendorName2,
                        contact = item.vendorContact2,
                        address = item.vendorAddress2,
                        gst = item.vendorGst2,
                        location = "",
                    )
                }
            }
        }
    }
}

@Composable
private fun VendorCard(
    index: Int,
    name: String,
    contact: String,
    address: String,
    gst: String,
    location: String,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(AppTheme.Bg)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(AppTheme.Brand),
                contentAlignment = Alignment.Center,
            ) {
                Text("$index", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.width(10.dp))
            Text(
                name.ifBlank { "Vendor $index" },
                color = AppTheme.Ink900,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
            )
        }
        if (contact.isNotBlank()) VendorRow(Icons.Outlined.Call, contact)
        if (address.isNotBlank()) VendorRow(Icons.Outlined.LocationOn, address)
        if (location.isNotBlank()) VendorRow(Icons.Outlined.LocationOn, location)
        if (gst.isNotBlank()) VendorRow(Icons.Outlined.Receipt, "GST $gst")
    }
}

@Composable
private fun VendorRow(icon: ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = AppTheme.Ink500, modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(8.dp))
        Text(text, color = AppTheme.Ink700, fontSize = 12.sp)
    }
}

/* ── Section 5 — Actions ────────────────────────────────────────────────── */

@Composable
private fun ActionsSection(
    onUpdateStock: () -> Unit,
    onEdit: () -> Unit,
    onAddVendor: () -> Unit,
) {
    SoftCard {
        SectionTitle(icon = Icons.Outlined.Inventory2, title = "Quick Actions")
        Spacer(Modifier.height(12.dp))
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            ActionRow(
                icon = Icons.Outlined.SyncAlt,
                title = "Update Stock",
                subtitle = "Adjust quantity on hand",
                accent = AppTheme.Brand,
                onClick = onUpdateStock,
            )
            ActionRow(
                icon = Icons.Outlined.Edit,
                title = "Edit Item",
                subtitle = "Modify specs, price, HSN…",
                accent = AppTheme.Warning,
                onClick = onEdit,
            )
            ActionRow(
                icon = Icons.Outlined.AddBusiness,
                title = "Add Vendor",
                subtitle = "Record a supplier or contact",
                accent = AppTheme.Success,
                onClick = onAddVendor,
            )
        }
    }
}

@Composable
private fun ActionRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    accent: Color,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(AppTheme.Bg)
            .premiumPress(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(accent.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, null, tint = accent, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = AppTheme.Ink900, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Text(subtitle, color = AppTheme.Ink500, fontSize = 11.sp)
        }
        Text("›", color = AppTheme.Ink300, fontSize = 22.sp, fontWeight = FontWeight.Bold)
    }
}

/* ── Shared building blocks ─────────────────────────────────────────────── */

@Composable
private fun SoftCard(content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    val shape = RoundedCornerShape(20.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(8.dp, shape, ambientColor = Color.Transparent, spotColor = AppTheme.ShadowSm)
            .clip(shape)
            .background(Color.White)
            .padding(18.dp),
        content = content,
    )
}

@Composable
private fun SectionTitle(icon: ImageVector, title: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = AppTheme.Brand, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(
            title,
            color = AppTheme.Ink900,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

