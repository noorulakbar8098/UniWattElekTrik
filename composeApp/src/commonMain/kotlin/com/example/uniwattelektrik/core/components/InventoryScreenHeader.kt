package com.example.uniwattelektrik.core.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Shared header used by every screen in the Inventory section
 * (Inventory Management, Departments, Equipment, Spare List, Price List, …).
 *
 * Layout:
 *   ┌──────────────────────────────────────────────┐
 *   │ [←]   SUBTITLE                               │
 *   │       Title                       [trailing] │
 *   └──────────────────────────────────────────────┘
 *
 * Now backed by [OperationsHeader] so the inventory section shares the
 * same premium dark surface as the rest of the app. The old API
 * (`title` / `subtitle` / `onBack` / `trailing`) is preserved so every
 * inventory screen migrates for free.
 */
@Composable
fun InventoryScreenHeader(
    title: String,
    subtitle: String,
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null,
) {
    OperationsHeader(
        modifier = modifier,
        eyebrow  = subtitle,
        title    = title,
        onBack   = onBack,
        actions  = trailing?.let { content -> { content() } },
    )
}
