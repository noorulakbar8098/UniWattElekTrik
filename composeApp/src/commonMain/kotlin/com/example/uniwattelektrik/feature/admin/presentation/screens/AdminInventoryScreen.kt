package com.example.uniwattelektrik.feature.admin.presentation.screens

import com.example.uniwattelektrik.core.theme.appScreenBackground

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.uniwattelektrik.core.components.AppCard
import com.example.uniwattelektrik.core.sample.SampleInventory
import com.example.uniwattelektrik.core.sample.SampleInventoryItem
import com.example.uniwattelektrik.core.theme.AppTheme

@Composable
fun AdminInventoryScreen(modifier: Modifier = Modifier) {
    val items = SampleInventory.all
    val lowStock = items.count { it.stock < it.reorder }

    com.example.uniwattelektrik.core.theme.SetStatusBar(
        color = AppTheme.Bg, darkIcons = true,
    )

    Column(modifier = modifier.fillMaxSize().background(appScreenBackground())) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(20.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Inventory",
                        style = com.example.uniwattelektrik.core.theme.AppTypography.HeaderTitle
                            .copy(color = AppTheme.Ink900),
                    )
                    Text("${items.size} SKUs · $lowStock below reorder level",
                         color = AppTheme.Ink500, fontSize = 12.sp)
                }
                Box(
                    modifier = Modifier
                        .height(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(AppTheme.Brand)
                        .clickable { /* TODO */ }
                        .padding(horizontal = 14.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("+ Add SKU", color = Color.White, fontSize = 13.sp,
                         fontWeight = FontWeight.SemiBold)
                }
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(items, key = { it.sku }) { sku -> InventoryRow(sku) }
        }
    }
}

@Composable
private fun InventoryRow(item: SampleInventoryItem) {
    val low = item.stock < item.reorder
    AppCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (low) AppTheme.HighBg else AppTheme.Brand50),
                contentAlignment = Alignment.Center,
            ) { Text(if (low) "⚠️" else "📦", fontSize = 20.sp) }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(item.name, color = AppTheme.Ink900, fontSize = 14.sp,
                     fontWeight = FontWeight.SemiBold)
                Text("${item.sku} · ${item.category}", color = AppTheme.Ink500, fontSize = 11.sp)
                Text("Reorder at ${item.reorder}", color = AppTheme.Ink300, fontSize = 11.sp)
            }
            Column(horizontalAlignment = Alignment.End,
                   verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("${item.stock}",
                     color = if (low) AppTheme.High else AppTheme.Ink900,
                     fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(if (low) AppTheme.HighBg else AppTheme.LowBg)
                        .padding(horizontal = 8.dp, vertical = 2.dp),
                ) {
                    Text(if (low) "Low" else "OK",
                         color    = if (low) AppTheme.High else AppTheme.Low,
                         fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}
