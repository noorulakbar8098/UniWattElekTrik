package com.example.uniwattelektrik.feature.user.presentation.screens

import com.example.uniwattelektrik.core.theme.appScreenBackground

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Assignment
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.AdminPanelSettings
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material.icons.outlined.PlaylistAdd
import androidx.compose.material.icons.outlined.Policy
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material.icons.outlined.Warehouse
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.uniwattelektrik.core.components.AppCard
import com.example.uniwattelektrik.core.components.PremiumHeaderBackground
import com.example.uniwattelektrik.core.components.PremiumHeaderStatusBarColor
import com.example.uniwattelektrik.core.navigation.AdminRoute
import com.example.uniwattelektrik.core.theme.AppTheme
import com.example.uniwattelektrik.core.theme.SetStatusBar
import com.example.uniwattelektrik.feature.auth.domain.model.User
import com.example.uniwattelektrik.feature.workforce.presentation.DeleteAllState
import com.example.uniwattelektrik.feature.workforce.presentation.WorkforceViewModel
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProfileScreen(
    user: User,
    initials: String,
    name: String,
    role: String,
    onLogout: () -> Unit,
    workforceVm: WorkforceViewModel? = null,
    onNavigate: (AdminRoute) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    SetStatusBar(color = PremiumHeaderStatusBarColor, darkIcons = false)

    // ── Live data for KPIs ──────────────────────────────────────────────────
    val employees by workforceVm?.employees?.collectAsState() ?: remember { mutableStateOf(emptyList()) }
    val tasks     by workforceVm?.tasks?.collectAsState()     ?: remember { mutableStateOf(emptyList()) }
    val attendance by workforceVm?.attendance?.collectAsState() ?: remember { mutableStateOf(emptyList()) }

    val doneTasks   = tasks.count { it.status == "Done" }
    val activeTasks = tasks.count { it.status != "Done" }
    val teamSize    = employees.size
    val efficiency  = if (tasks.isNotEmpty())
        ((doneTasks.toFloat() / tasks.size) * 100).roundToInt() else 0

    // ── Delete all state ────────────────────────────────────────────────────
    val deleteAllState by workforceVm?.deleteAllState?.collectAsState()
        ?: remember { mutableStateOf(DeleteAllState.Idle) }.let { s ->
            s.value.let { remember(it) { mutableStateOf(it) } }
        }

    var showConfirmDialog by remember { mutableStateOf(false) }
    var showResultDialog  by remember { mutableStateOf<String?>(null) }
    var showLogoutDialog  by remember { mutableStateOf(false) }

    LaunchedEffect(deleteAllState) {
        when (val s = deleteAllState) {
            is DeleteAllState.Success -> {
                showResultDialog = "All data deleted successfully."
                workforceVm?.clearDeleteAllState()
            }
            is DeleteAllState.Error -> {
                showResultDialog = "Delete failed: ${s.message}"
                workforceVm?.clearDeleteAllState()
            }
            else -> Unit
        }
    }

    // ── Dialogs ─────────────────────────────────────────────────────────────
    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            title = { Text("Delete all data?", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "This will permanently delete all employees, tasks, attendance logs, " +
                        "inventory, check-ins and notifications for your account.\n\n" +
                        "This action cannot be undone.",
                    color = AppTheme.Ink500,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showConfirmDialog = false
                    workforceVm?.deleteAllData(user.id)
                }) { Text("Delete", color = AppTheme.High, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDialog = false }) { Text("Cancel") }
            },
        )
    }

    showResultDialog?.let { msg ->
        AlertDialog(
            onDismissRequest = { showResultDialog = null },
            title = { Text("Done") },
            text = { Text(msg) },
            confirmButton = {
                TextButton(onClick = { showResultDialog = null }) { Text("OK") }
            },
        )
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Sign out now?", fontWeight = FontWeight.Bold) },
            text = { Text("You will be returned to the login screen.", color = AppTheme.Ink500) },
            confirmButton = {
                TextButton(onClick = {
                    showLogoutDialog = false
                    onLogout()
                }) { Text("OK", fontWeight = FontWeight.SemiBold) }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) { Text("Cancel") }
            },
        )
    }

    // ── Main layout ─────────────────────────────────────────────────────────
    LazyColumn(
        modifier = modifier.fillMaxSize().background(appScreenBackground()),
        contentPadding = PaddingValues(bottom = 120.dp),
    ) {

        // ═══════════════════════════════════════════════════════════════════
        // 1. PREMIUM GRADIENT HEADER
        // ═══════════════════════════════════════════════════════════════════
        item {
            PremiumHeaderBackground {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .windowInsetsPadding(WindowInsets.statusBars)
                        .padding(horizontal = 20.dp)
                        .padding(top = 18.dp, bottom = 32.dp),
                ) {
                    // Top row: edit + logout
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        // Admin badge
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.White.copy(alpha = 0.15f))
                                .border(1.dp, Color.White.copy(alpha = 0.20f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 10.dp, vertical = 5.dp),
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                Icon(Icons.Outlined.AdminPanelSettings, null,
                                     tint = Color.White.copy(alpha = 0.9f), modifier = Modifier.size(14.dp))
                                Text("ADMIN", color = Color.White.copy(alpha = 0.9f),
                                     fontSize = 10.sp, fontWeight = FontWeight.Bold,
                                     letterSpacing = 1.sp)
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            GlassIconButton(Icons.Outlined.Edit, "Edit") { /* TODO */ }
                            GlassIconButton(Icons.AutoMirrored.Outlined.Logout, "Logout") {
                                showLogoutDialog = true
                            }
                        }
                    }

                    Spacer(Modifier.height(20.dp))

                    // Avatar + name
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .shadow(16.dp, CircleShape, spotColor = Color.Black.copy(alpha = 0.3f))
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(listOf(Color(0xFFFFB28A), Color(0xFFEC8552)))
                                )
                                .border(2.dp, Color.White.copy(alpha = 0.3f), CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(initials, color = Color.White, fontSize = 22.sp,
                                 fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.width(16.dp))
                        Column {
                            Text(name, color = Color.White, fontSize = 20.sp,
                                 fontWeight = FontWeight.Bold)
                            Text(role, color = Color.White.copy(alpha = 0.7f),
                                 fontSize = 12.sp, fontFamily = FontFamily.Monospace,
                                 letterSpacing = 0.3.sp)
                            Text(user.email, color = Color.White.copy(alpha = 0.5f),
                                 fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        // ═══════════════════════════════════════════════════════════════════
        // 2. KPI SECTION (4 cards)
        // ═══════════════════════════════════════════════════════════════════
        item {
            Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
                SectionLabel("PERFORMANCE")
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    KpiCard(
                        icon = Icons.Outlined.TaskAlt,
                        value = doneTasks,
                        label = "Completed",
                        accent = AppTheme.Low,
                        accentBg = AppTheme.LowBg,
                        modifier = Modifier.weight(1f),
                    )
                    KpiCard(
                        icon = Icons.Outlined.Speed,
                        value = activeTasks,
                        label = "Active",
                        accent = AppTheme.Brand,
                        accentBg = AppTheme.Brand50,
                        modifier = Modifier.weight(1f),
                    )
                }
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    KpiCard(
                        icon = Icons.Outlined.People,
                        value = teamSize,
                        label = "Team Size",
                        accent = AppTheme.Violet,
                        accentBg = Color(0xFFF0F0FF),
                        modifier = Modifier.weight(1f),
                    )
                    KpiCard(
                        icon = Icons.Outlined.CheckCircle,
                        value = efficiency,
                        label = "Efficiency %",
                        accent = AppTheme.Med,
                        accentBg = AppTheme.MedBg,
                        modifier = Modifier.weight(1f),
                        suffix = "%",
                    )
                }
            }
        }

        // ═══════════════════════════════════════════════════════════════════
        // 3. QUICK ACTIONS BAR
        // ═══════════════════════════════════════════════════════════════════
        item {
            Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                SectionLabel("QUICK ACTIONS")
                Spacer(Modifier.height(10.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    item {
                        QuickActionChip(
                            icon = Icons.Outlined.PlaylistAdd,
                            label = "Add Task",
                            bg = AppTheme.Brand,
                        ) { onNavigate(AdminRoute.NewTask()) }
                    }
                    item {
                        QuickActionChip(
                            icon = Icons.Outlined.PersonAdd,
                            label = "Add Employee",
                            bg = AppTheme.Violet,
                        ) { onNavigate(AdminRoute.Employees) }
                    }
                    item {
                        QuickActionChip(
                            icon = Icons.Outlined.Warehouse,
                            label = "Add Inventory",
                            bg = AppTheme.Low,
                        ) { onNavigate(AdminRoute.InventoryManagement) }
                    }
                }
            }
        }

        // ═══════════════════════════════════════════════════════════════════
        // 4. MANAGEMENT MODULES (2-col grid)
        // ═══════════════════════════════════════════════════════════════════
        item {
            Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
                SectionLabel("MANAGEMENT")
                Spacer(Modifier.height(10.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    maxItemsInEachRow = 2,
                ) {
                    ModuleCard(
                        icon = Icons.Outlined.Inventory2,
                        title = "Inventory",
                        subtitle = "Stock & pricing",
                        iconBg = AppTheme.LowBg,
                        iconTint = AppTheme.Low,
                        modifier = Modifier.weight(1f),
                    ) { onNavigate(AdminRoute.InventoryManagement) }

                    ModuleCard(
                        icon = Icons.AutoMirrored.Outlined.Assignment,
                        title = "Tasks",
                        subtitle = "$activeTasks active",
                        iconBg = AppTheme.Brand50,
                        iconTint = AppTheme.Brand,
                        modifier = Modifier.weight(1f),
                    ) { onNavigate(AdminRoute.Tasks) }

                    ModuleCard(
                        icon = Icons.Outlined.People,
                        title = "Employees",
                        subtitle = "$teamSize members",
                        iconBg = Color(0xFFF0F0FF),
                        iconTint = AppTheme.Violet,
                        modifier = Modifier.weight(1f),
                    ) { onNavigate(AdminRoute.Employees) }

                    ModuleCard(
                        icon = Icons.Outlined.LocationOn,
                        title = "Attendance",
                        subtitle = "Today's log",
                        iconBg = AppTheme.MedBg,
                        iconTint = AppTheme.Med,
                        modifier = Modifier.weight(1f),
                    ) { onNavigate(AdminRoute.Attendance) }

                    ModuleCard(
                        icon = Icons.Outlined.Policy,
                        title = "Compliance",
                        subtitle = "Reports",
                        iconBg = AppTheme.HighBg,
                        iconTint = AppTheme.High,
                        modifier = Modifier.weight(1f),
                    ) { /* TODO */ }

                    ModuleCard(
                        icon = Icons.Outlined.Notifications,
                        title = "Alerts",
                        subtitle = "Notifications",
                        iconBg = AppTheme.Brand100,
                        iconTint = AppTheme.Brand600,
                        modifier = Modifier.weight(1f),
                    ) { onNavigate(AdminRoute.Notifications) }
                }
            }
        }

        // ═══════════════════════════════════════════════════════════════════
        // 5. RECENT ACTIVITY
        // ═══════════════════════════════════════════════════════════════════
        item {
            Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)) {
                SectionLabel("RECENT ACTIVITY")
                Spacer(Modifier.height(10.dp))
                AppCard(cornerRadius = 20.dp, contentPadding = 0.dp) {
                    Column {
                        val lastTask = tasks.maxByOrNull { it.updatedAt ?: 0L }
                        val lastEmployee = employees.firstOrNull()

                        ActivityRow(
                            emoji = "📋",
                            title = lastTask?.title ?: "No tasks yet",
                            detail = lastTask?.let { "Status: ${it.status}" } ?: "Create your first task",
                        )
                        Divider()
                        ActivityRow(
                            emoji = "👤",
                            title = lastEmployee?.name ?: "No employees yet",
                            detail = lastEmployee?.let { "${it.role} · ${it.status}" } ?: "Add your first team member",
                        )
                        Divider()
                        ActivityRow(
                            emoji = "📦",
                            title = "Inventory Updated",
                            detail = "Tap to manage stock & spare items",
                        )
                    }
                }
            }
        }

        // ═══════════════════════════════════════════════════════════════════
        // 6. SETTINGS (minimal)
        // ═══════════════════════════════════════════════════════════════════
        item {
            Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
                SectionLabel("SETTINGS")
                Spacer(Modifier.height(10.dp))
                AppCard(cornerRadius = 20.dp, contentPadding = 0.dp) {
                    Column {
                        SettingsRow(Icons.Outlined.Person, "Personal Information") { }
                        Divider()
                        SettingsRow(Icons.Outlined.Shield, "Security & PIN") { }
                        Divider()
                        SettingsRow(Icons.Outlined.Language, "Language & Region") { }
                    }
                }
            }
        }

        // ═══════════════════════════════════════════════════════════════════
        // 7. DANGER ZONE + LOGOUT
        // ═══════════════════════════════════════════════════════════════════
        if (workforceVm != null) {
            item {
                Column(
                    modifier = Modifier.padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    SectionLabel("DANGER ZONE")
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(AppTheme.HighBg)
                            .clickable(
                                enabled = deleteAllState !is DeleteAllState.Loading,
                                onClick = { showConfirmDialog = true },
                            )
                            .padding(horizontal = 20.dp, vertical = 16.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Column {
                                Text("Delete all data", color = AppTheme.High,
                                     fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                                Text("Removes all employees, tasks, attendance & inventory",
                                     color = AppTheme.High.copy(alpha = 0.65f), fontSize = 11.sp)
                            }
                            if (deleteAllState is DeleteAllState.Loading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = AppTheme.High, strokeWidth = 2.dp,
                                )
                            }
                        }
                    }
                }
            }
        }

        item {
            Box(
                modifier = Modifier
                    .padding(horizontal = 20.dp, vertical = 12.dp)
                    .fillMaxWidth()
                    .height(50.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(AppTheme.HighBg)
                    .clickable(onClick = { showLogoutDialog = true }),
                contentAlignment = Alignment.Center,
            ) {
                Text("Sign out", color = AppTheme.High,
                     fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// PRIVATE COMPONENTS
// ═════════════════════════════════════════════════════════════════════════════

@Composable
private fun GlassIconButton(icon: ImageVector, desc: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(Color.White.copy(alpha = 0.15f))
            .border(1.dp, Color.White.copy(alpha = 0.20f), RoundedCornerShape(10.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = desc, tint = Color.White, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        color = AppTheme.Ink500,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.2.sp,
        modifier = Modifier.padding(start = 4.dp),
    )
}

/** Animated KPI card with count-up number. */
@Composable
private fun KpiCard(
    icon: ImageVector,
    value: Int,
    label: String,
    accent: Color,
    accentBg: Color,
    modifier: Modifier = Modifier,
    suffix: String = "",
) {
    // Animated count-up
    val anim = remember { Animatable(0f) }
    LaunchedEffect(value) {
        anim.snapTo(0f)
        anim.animateTo(
            targetValue = value.toFloat(),
            animationSpec = tween(durationMillis = 900, easing = FastOutSlowInEasing),
        )
    }

    val shape = RoundedCornerShape(20.dp)
    Box(
        modifier = modifier
            .shadow(8.dp, shape, spotColor = AppTheme.ShadowSpotSoft)
            .clip(shape)
            .background(Color.White)
            .padding(16.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(accentBg),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, null, tint = accent, modifier = Modifier.size(22.dp))
            }
            Text(
                text = "${anim.value.roundToInt()}$suffix",
                color = AppTheme.Ink900,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(label, color = AppTheme.Ink500, fontSize = 11.sp,
                 fontWeight = FontWeight.SemiBold, letterSpacing = 0.3.sp)
        }
    }
}

/** Horizontal quick-action pill. */
@Composable
private fun QuickActionChip(
    icon: ImageVector,
    label: String,
    bg: Color,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.95f else 1f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 800f),
        label = "chipScale",
    )

    Box(
        modifier = Modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(RoundedCornerShape(14.dp))
            .background(bg)
            .clickable(interactionSource = interactionSource, indication = ripple(), onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(icon, null, tint = Color.White, modifier = Modifier.size(18.dp))
            Text(label, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

/** 2-col grid module card with press animation. */
@Composable
private fun ModuleCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    iconBg: Color,
    iconTint: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.95f else 1f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 800f),
        label = "moduleScale",
    )

    val shape = RoundedCornerShape(20.dp)
    Box(
        modifier = modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .shadow(6.dp, shape, spotColor = AppTheme.ShadowSpotSoft)
            .clip(shape)
            .background(Color.White)
            .clickable(interactionSource = interactionSource, indication = ripple(), onClick = onClick)
            .padding(16.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(13.dp))
                    .background(iconBg),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, null, tint = iconTint, modifier = Modifier.size(24.dp))
            }
            Text(title, color = AppTheme.Ink900, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Text(subtitle, color = AppTheme.Ink500, fontSize = 11.sp)
        }
    }
}

@Composable
private fun ActivityRow(emoji: String, title: String, detail: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(emoji, fontSize = 22.sp)
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, color = AppTheme.Ink900, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                 maxLines = 1)
            Text(detail, color = AppTheme.Ink500, fontSize = 11.sp, maxLines = 1)
        }
    }
}

@Composable
private fun SettingsRow(icon: ImageVector, label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(AppTheme.Ink50),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, null, tint = AppTheme.Ink700, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(14.dp))
        Text(label, color = AppTheme.Ink900, fontSize = 14.sp, modifier = Modifier.weight(1f))
        Text("›", color = AppTheme.Ink300, fontSize = 18.sp)
    }
}

@Composable
private fun Divider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .padding(start = 52.dp)
            .background(AppTheme.Ink100),
    )
}
