package com.example.uniwattelektrik.feature.admin.presentation.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.RemoveCircleOutline
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SupervisedUserCircle
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.WorkOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.uniwattelektrik.core.theme.AppTheme
import com.example.uniwattelektrik.feature.workforce.data.remote.EmployeeRecord
import com.example.uniwattelektrik.feature.workforce.presentation.WorkforceViewModel

// ─── Permission tier metadata ─────────────────────────────────────────────────

private data class PermissionTier(
    val key: String,
    val label: String,
    val description: String,
    val icon: ImageVector,
    val color: Color,
    val bgColor: Color,
)

private val TIERS = listOf(
    PermissionTier(
        key = "super",
        label = "Senior Manager",
        description = "Create tasks · Manage inventory · View attendance",
        icon = Icons.Filled.AdminPanelSettings,
        color = Color(0xFF6C3CE1),
        bgColor = Color(0xFFF0EBFF),
    ),
    PermissionTier(
        key = "field",
        label = "Field Officer",
        description = "Accept & complete tasks · Check in/out",
        icon = Icons.Filled.WorkOutline,
        color = Color(0xFF0288D1),
        bgColor = Color(0xFFE1F5FE),
    ),
    PermissionTier(
        key = "viewer",
        label = "Viewer",
        description = "Read-only access to tasks & reports",
        icon = Icons.Filled.Visibility,
        color = Color(0xFF43A047),
        bgColor = Color(0xFFE8F5E9),
    ),
    PermissionTier(
        key = "",
        label = "Standard",
        description = "Default employee — no elevated access",
        icon = Icons.Filled.Person,
        color = Color(0xFF757575),
        bgColor = Color(0xFFF5F5F5),
    ),
)

private fun tierFor(key: String) =
    TIERS.firstOrNull { it.key == key.lowercase() } ?: TIERS.last()

// ─── Screen ───────────────────────────────────────────────────────────────────

@Composable
fun LinkManagerScreen(
    adminId: String,
    workforceVm: WorkforceViewModel,
    onBack: () -> Unit,
) {
    val employees by workforceVm.employees.collectAsStateWithLifecycle()
    val activeEmployees = employees.filter { it.deletedAt == null && it.status != "Inactive" }

    var editTarget by remember { mutableStateOf<EmployeeRecord?>(null) }

    // Group: super first, then field, then viewer, then standard
    val grouped = remember(activeEmployees) {
        val order = listOf("super", "field", "viewer", "")
        order.map { key ->
            key to activeEmployees.filter { it.permission.lowercase() == key }
        }.filter { it.second.isNotEmpty() }
    }

    Scaffold(
        containerColor = Color(0xFFF7F8FC),
        topBar = {
            LinkManagerTopBar(onBack = onBack)
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(bottom = 32.dp),
        ) {
            // ── Hero header ────────────────────────────────────────────────
            item {
                LinkManagerHero(totalCount = activeEmployees.size)
            }

            // ── Permission tier legend ─────────────────────────────────────
            item {
                TierLegendRow()
            }

            // ── Employees grouped by tier ─────────────────────────────────
            grouped.forEach { (tierKey, list) ->
                val tier = tierFor(tierKey)
                item(key = "header_$tierKey") {
                    TierSectionHeader(tier = tier, count = list.size)
                }
                items(list, key = { it.id }) { emp ->
                    EmployeePermissionCard(
                        employee = emp,
                        tier = tierFor(emp.permission),
                        onClick = { editTarget = emp },
                    )
                }
            }

            // Empty state
            if (activeEmployees.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(60.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Filled.SupervisedUserCircle, null, modifier = Modifier.size(56.dp), tint = Color(0xFFBDBDBD))
                            Spacer(Modifier.height(12.dp))
                            Text("No employees yet", color = Color(0xFF9E9E9E), fontWeight = FontWeight.Medium)
                            Text("Add employees from the Team tab", color = Color(0xFFBDBDBD), fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }

    // ── Permission change dialog ───────────────────────────────────────────
    editTarget?.let { emp ->
        PermissionEditDialog(
            employee = emp,
            currentTier = tierFor(emp.permission),
            onDismiss = { editTarget = null },
            onSelect = { newPermission ->
                workforceVm.updateEmployeePermission(
                    adminId    = adminId,
                    employeeId = emp.id,
                    permission = newPermission,
                )
                editTarget = null
            },
        )
    }
}

// ─── Top bar ─────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LinkManagerTopBar(onBack: () -> Unit) {
    TopAppBar(
        title = {
            Column {
                Text("Link Manager", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text("Manage employee access levels", fontSize = 12.sp, color = Color(0xFF9E9E9E))
            }
        },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.White,
            titleContentColor = Color(0xFF1A1A2E),
        ),
    )
}

// ─── Hero ─────────────────────────────────────────────────────────────────────

@Composable
private fun LinkManagerHero(totalCount: Int) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.linearGradient(
                    listOf(Color(0xFF6C3CE1), Color(0xFF2979FF)),
                )
            )
            .padding(24.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.Shield, null, tint = Color.White, modifier = Modifier.size(28.dp))
            }
            Spacer(Modifier.width(16.dp))
            Column {
                Text(
                    "Access Control",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                )
                Text(
                    "$totalCount employees · tap any card to change role",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 13.sp,
                )
            }
        }
    }
}

// ─── Tier legend ─────────────────────────────────────────────────────────────

@Composable
private fun TierLegendRow() {
    Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)) {
        Text(
            "PERMISSION TIERS",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF9E9E9E),
            letterSpacing = 1.sp,
        )
        Spacer(Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            TIERS.forEach { tier ->
                TierChip(tier = tier, modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun TierChip(tier: PermissionTier, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(tier.bgColor)
            .padding(horizontal = 8.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(tier.icon, null, tint = tier.color, modifier = Modifier.size(18.dp))
        Spacer(Modifier.height(4.dp))
        Text(
            tier.label,
            fontSize = 9.sp,
            fontWeight = FontWeight.SemiBold,
            color = tier.color,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

// ─── Section header ───────────────────────────────────────────────────────────

@Composable
private fun TierSectionHeader(tier: PermissionTier, count: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(tier.bgColor),
                contentAlignment = Alignment.Center,
            ) {
                Icon(tier.icon, null, tint = tier.color, modifier = Modifier.size(15.dp))
            }
            Text(
                tier.label.uppercase(),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = tier.color,
                letterSpacing = 0.8.sp,
            )
        }
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(tier.bgColor)
                .padding(horizontal = 10.dp, vertical = 4.dp),
        ) {
            Text("$count", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = tier.color)
        }
    }
}

// ─── Employee card ────────────────────────────────────────────────────────────

@Composable
private fun EmployeePermissionCard(
    employee: EmployeeRecord,
    tier: PermissionTier,
    onClick: () -> Unit,
) {
    val bgColor by animateColorAsState(tier.bgColor.copy(alpha = 0.4f), tween(300), label = "bg")

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .border(1.dp, Color(0xFFEEEEEE), RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Avatar
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(bgColor),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                employee.name.take(2).uppercase(),
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = tier.color,
            )
        }

        Spacer(Modifier.width(14.dp))

        // Name + role
        Column(modifier = Modifier.weight(1f)) {
            Text(
                employee.name,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
                color = Color(0xFF1A1A2E),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                employee.role.takeIf { it.isNotBlank() } ?: "Employee",
                fontSize = 12.sp,
                color = Color(0xFF757575),
            )
        }

        // Permission badge
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(tier.bgColor)
                .padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(tier.icon, null, tint = tier.color, modifier = Modifier.size(13.dp))
            Text(tier.label, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = tier.color)
        }
    }
}

// ─── Permission edit dialog ───────────────────────────────────────────────────

@Composable
private fun PermissionEditDialog(
    employee: EmployeeRecord,
    currentTier: PermissionTier,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(Color.White)
                .padding(24.dp),
        ) {
            // Header
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(currentTier.bgColor),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        employee.name.take(2).uppercase(),
                        fontWeight = FontWeight.Bold,
                        color = currentTier.color,
                    )
                }
                Column {
                    Text(employee.name, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF1A1A2E))
                    Text("Change access level", fontSize = 12.sp, color = Color(0xFF9E9E9E))
                }
            }

            Spacer(Modifier.height(20.dp))
            Divider(color = Color(0xFFF0F0F0))
            Spacer(Modifier.height(16.dp))

            // Tier options
            TIERS.forEach { tier ->
                val isSelected = tier.key == currentTier.key
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (isSelected) tier.bgColor else Color.Transparent)
                        .border(
                            width = if (isSelected) 1.5.dp else 1.dp,
                            color = if (isSelected) tier.color.copy(alpha = 0.4f) else Color(0xFFF0F0F0),
                            shape = RoundedCornerShape(14.dp),
                        )
                        .clickable { onSelect(tier.key) }
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(tier.bgColor),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(tier.icon, null, tint = tier.color, modifier = Modifier.size(18.dp))
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(tier.label, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = Color(0xFF1A1A2E))
                        Text(tier.description, fontSize = 11.sp, color = Color(0xFF9E9E9E), maxLines = 2)
                    }
                    if (isSelected) {
                        Box(
                            modifier = Modifier
                                .size(22.dp)
                                .clip(CircleShape)
                                .background(tier.color),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(Icons.Filled.Check, null, tint = Color.White, modifier = Modifier.size(13.dp))
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
            }

            Spacer(Modifier.height(4.dp))

            // Cancel
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.align(Alignment.End),
            ) {
                Text("Cancel", color = Color(0xFF9E9E9E))
            }
        }
    }
}
