package com.example.uniwattelektrik.feature.admin.presentation.screens.inventory

import com.example.uniwattelektrik.core.performance.TrackScreenPerformance

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForwardIos
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.PriceCheck
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.uniwattelektrik.core.components.InventoryScreenHeader
import com.example.uniwattelektrik.core.components.PremiumHeaderStatusBarColor
import com.example.uniwattelektrik.core.theme.AppTheme
import com.example.uniwattelektrik.core.theme.SetStatusBar
import com.example.uniwattelektrik.core.theme.appScreenBackground

@Composable
fun InventoryManagementScreen(
    onBack: () -> Unit,
    onDepartments: () -> Unit,
    onEquipment: () -> Unit,
    onPriceList: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TrackScreenPerformance("InventoryManagementScreen")
    SetStatusBar(color = PremiumHeaderStatusBarColor, darkIcons = false)

    val modules = listOf(
        InventoryModule(
            icon = Icons.Outlined.Category,
            iconBg = AppTheme.Brand50,
            iconTint = AppTheme.Brand,
            title = "Department",
            description = "Create and manage departments for your organisation",
            onClick = onDepartments
        ),
        InventoryModule(
            icon = Icons.Outlined.Inventory2,
            iconBg = Color(0xFFF0F4FF),
            iconTint = AppTheme.Brand,
            title = "Equipment",
            description = "Add and organise equipment grouped by department",
            onClick = onEquipment
        ),
        InventoryModule(
            icon = Icons.Outlined.PriceCheck,
            iconBg = AppTheme.SuccessBg,
            iconTint = AppTheme.Success,
            title = "Spare List",
            description = "Define items with size, core, price, stock & HSN",
            onClick = onPriceList
        )
    )

    Column(modifier = modifier.fillMaxSize().background(appScreenBackground())) {
        InventoryScreenHeader(
            title = "Inventory Management",
            subtitle = "MANAGE DEPARTMENTS, EQUIPMENT & PRICING",
            onBack = onBack,
        )

        LazyColumn(
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item { SectionLabel("CORE MODULES") }
            
            itemsIndexed(modules) { index, module ->
                StaggeredEntrance(index = index) {
                    InventoryModuleCard(
                        icon        = module.icon,
                        iconBg      = module.iconBg,
                        iconTint    = module.iconTint,
                        title       = module.title,
                        description = module.description,
                        onClick     = module.onClick,
                    )
                }
            }

            item { Spacer(Modifier.height(100.dp)) }
        }
    }
}

private data class InventoryModule(
    val icon: ImageVector,
    val iconBg: Color,
    val iconTint: Color,
    val title: String,
    val description: String,
    val onClick: () -> Unit
)

@Composable
private fun InventoryModuleCard(
    icon: ImageVector,
    iconBg: Color,
    iconTint: Color,
    title: String,
    description: String,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(20.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(8.dp, shape, ambientColor = Color.Transparent, spotColor = AppTheme.ShadowMd)
            .clip(shape)
            .background(Color.White)
            .premiumPress(onClick = onClick)
            .padding(20.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(iconBg),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, null, tint = iconTint, modifier = Modifier.size(28.dp))
            }
            Spacer(Modifier.width(18.dp))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, color = AppTheme.Ink900, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                Text(description, color = AppTheme.Ink500, fontSize = 13.sp, lineHeight = 18.sp)
            }
            Spacer(Modifier.width(12.dp))
            Icon(Icons.AutoMirrored.Outlined.ArrowForwardIos, null, tint = AppTheme.Ink300, modifier = Modifier.size(16.dp))
        }
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
        modifier = Modifier.padding(start = 4.dp, bottom = 4.dp),
    )
}
