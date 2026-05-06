package com.example.uniwattelektrik.core.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Shared, premium gradient header used by every screen in the Inventory section
 * (Inventory Management, Departments, Equipment, Spare List, Price List, …).
 *
 * Layout:
 *   ┌──────────────────────────────────────────────┐
 *   │ [←]   Title                       [trailing] │
 *   │       SUBTITLE (uppercase tracking)          │
 *   └──────────────────────────────────────────────┘
 *
 * Uses [PremiumHeaderBackground] under the hood, so the look matches the rest
 * of the app (task / employee / dashboard headers).
 */
@Composable
fun InventoryScreenHeader(
    title: String,
    subtitle: String,
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null,
) {
    PremiumHeaderBackground(
        modifier = modifier.shadow(
            elevation = 12.dp,
            shape = RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp),
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(horizontal = 20.dp)
                .padding(top = 14.dp, bottom = 28.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (onBack != null) {
                GlassBackButton(onClick = onBack)
                Spacer(Modifier.width(14.dp))
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.5).sp,
                )
                Text(
                    text = subtitle,
                    color = Color.White.copy(alpha = 0.78f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp,
                )
            }
            if (trailing != null) {
                Spacer(Modifier.width(12.dp))
                Box(contentAlignment = Alignment.Center) { trailing() }
            }
        }
    }
}

