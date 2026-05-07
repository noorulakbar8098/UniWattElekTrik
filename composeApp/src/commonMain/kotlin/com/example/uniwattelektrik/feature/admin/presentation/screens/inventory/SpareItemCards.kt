package com.example.uniwattelektrik.feature.admin.presentation.screens.inventory

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.uniwattelektrik.core.theme.AppTheme

/**
 * Stock health levels used for color-coded status feedback in cards.
 *
 *  - [OutOfStock]   qty == 0          → red
 *  - [Success]          qty in 1..LowMax  → amber
 *  - [InStock]      qty > LowMax      → green
 */
enum class StockStatus { OutOfStock, Success, InStock }

private const val LOW_STOCK_THRESHOLD = 20

fun SpareItem.stockStatus(lowThreshold: Int = LOW_STOCK_THRESHOLD): StockStatus = when {
    stockQty <= 0           -> StockStatus.OutOfStock
    stockQty < lowThreshold -> StockStatus.Success
    else                    -> StockStatus.InStock
}

@Composable
fun StockStatus.color(): Color = when (this) {
    StockStatus.OutOfStock -> AppTheme.Danger
    StockStatus.Success        -> AppTheme.Warning
    StockStatus.InStock    -> AppTheme.Success
}

@Composable
fun StockStatus.bgColor(): Color = when (this) {
    StockStatus.OutOfStock -> AppTheme.DangerBg
    StockStatus.Success        -> AppTheme.WarningBg
    StockStatus.InStock    -> AppTheme.SuccessBg
}

fun StockStatus.label(): String = when (this) {
    StockStatus.OutOfStock -> "Out of Stock"
    StockStatus.Success        -> "Success Stock"
    StockStatus.InStock    -> "In Stock"
}

fun StockStatus.icon(): ImageVector = when (this) {
    StockStatus.OutOfStock -> Icons.Outlined.ErrorOutline
    StockStatus.Success        -> Icons.Outlined.WarningAmber
    StockStatus.InStock    -> Icons.Outlined.CheckCircle
}

/**
 * Premium minimal card used in BOTH the inventory list (`AdminInventoryScreen`)
 * and the master spare list (`SpareListScreen`). Shows ONLY:
 *
 *  - Item name (primary)
 *  - Category (subtitle)
 *  - Price (₹ highlight)
 *  - Stock qty (color-coded)
 *  - Status icon
 *
 *  All heavy details (vendor, HSN, rating, poles, …) live in the detail screen.
 *
 *  Behavioural states:
 *   - Default: tap → [onClick] (open detail), long-press → [onLongClick]
 *     (enter selection mode).
 *   - When [selectionMode] is true: tap toggles selection via [onLongClick]'s
 *     caller-supplied logic, the status avatar is replaced with a check
 *     bubble, and the card gets a subtle brand-colour border + tint.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MinimalSpareCard(
    item: SpareItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selectionMode: Boolean = false,
    selected: Boolean = false,
    onLongClick: (() -> Unit)? = null,
) {
    val status = item.stockStatus()
    val statusColor = status.color()
    val statusBg = status.bgColor()
    val shape = RoundedCornerShape(20.dp)

    val cardBg = if (selected) AppTheme.Brand50 else Color.White
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .shadow(6.dp, shape, ambientColor = Color.Transparent, spotColor = AppTheme.ShadowSm)
            .clip(shape)
            .background(cardBg)
            .then(
                if (selected)
                    Modifier.border(width = 2.dp, color = AppTheme.Brand, shape = shape)
                else Modifier,
            )
            .combinedClickable(
                interactionSource = interactionSource,
                indication = ripple(),
                onClick = onClick,
                onLongClick = onLongClick,
            )
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Status icon avatar — switches to a "check bubble" when selected.
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (selected) AppTheme.Brand else statusBg),
                contentAlignment = Alignment.Center,
            ) {
                if (selected) {
                    Icon(
                        imageVector = Icons.Outlined.Check,
                        contentDescription = "Selected",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp),
                    )
                } else if (selectionMode) {
                    // Hollow circle hints "tap to select".
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .clip(CircleShape)
                            .border(
                                width = 2.dp,
                                color = AppTheme.Ink300,
                                shape = CircleShape,
                            ),
                    )
                } else {
                    Icon(
                        imageVector = status.icon(),
                        contentDescription = status.label(),
                        tint = statusColor,
                        modifier = Modifier.size(22.dp),
                    )
                }
            }
            Spacer(Modifier.width(14.dp))

            // Name + category subtitle
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = item.name,
                    color = AppTheme.Ink900,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                )
                Text(
                    text = item.category.ifBlank { "Uncategorized" },
                    color = AppTheme.Ink500,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                )
            }

            Spacer(Modifier.width(12.dp))

            // Price + stock pill
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = if (item.price > 0.0) "₹${formatRupees(item.price)}" else "—",
                    color = AppTheme.Ink900,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                )
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(statusBg)
                        .padding(horizontal = 10.dp, vertical = 3.dp),
                ) {
                    Text(
                        text = "${item.stockQty} units",
                        color = statusColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}

internal fun formatRupees(value: Double): String {
    if (value % 1.0 == 0.0) return value.toLong().toString()
    val rounded = kotlin.math.round(value * 100.0) / 100.0
    return rounded.toString()
}

/* ────────────────────────────────────────────────────────────────────────── */
/*  Multi-selection action bar                                                */
/* ────────────────────────────────────────────────────────────────────────── */

/**
 * Floating action bar shown above list screens while the user is in
 * multi-selection mode. Renders the selection count, a "Select all" / "Clear"
 * toggle, and a destructive "Delete" button. All actions are forwarded to
 * the host via lambdas — this composable keeps no state of its own.
 */
@Composable
fun SelectionActionBar(
    selectedCount: Int,
    totalCount: Int,
    onCancel: () -> Unit,
    onSelectAllToggle: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(20.dp)
    val allSelected = selectedCount > 0 && selectedCount >= totalCount
    Row(
        modifier = modifier
            .fillMaxWidth()
            .shadow(10.dp, shape, ambientColor = Color.Transparent, spotColor = AppTheme.ShadowSm)
            .clip(shape)
            .background(AppTheme.Ink900)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // ── Cancel ──
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.12f))
                .premiumPress(onClick = onCancel),
            contentAlignment = Alignment.Center,
        ) {
            Text("✕", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.width(12.dp))

        // ── Count ──
        Column(modifier = Modifier.weight(1f)) {
            Text(
                "$selectedCount selected",
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                "of $totalCount",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 11.sp,
            )
        }

        // ── Select all toggle ──
        Box(
            modifier = Modifier
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.12f))
                .premiumPress(onClick = onSelectAllToggle)
                .padding(horizontal = 12.dp, vertical = 8.dp),
        ) {
            Text(
                if (allSelected) "Clear" else "All",
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
        Spacer(Modifier.width(10.dp))

        // ── Delete ──
        Box(
            modifier = Modifier
                .clip(CircleShape)
                .background(AppTheme.Danger)
                .premiumPress(onClick = onDelete, enabled = selectedCount > 0)
                .padding(horizontal = 14.dp, vertical = 8.dp),
        ) {
            Text(
                "Delete ($selectedCount)",
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

