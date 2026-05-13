package com.example.uniwattelektrik.core.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.uniwattelektrik.core.theme.AppTheme

// ── Dark-navy palette (matches PremiumBrandFooter so the two read as one band).
private val NavBgTop      = Color(0xFF142A56)
private val NavBgBottom   = Color(0xFF0A1633)
private val NavActive     = Color(0xFF60A5FA)   // electric cyan-blue
private val NavInactive   = Color(0xFF8A99B8)   // muted slate

/** One tab descriptor used by [AppBottomNavBar]. */
data class BottomNavItem(
    val key: String,
    val label: String,
    /** Optional emoji fallback (legacy). Prefer [icon]. */
    val emoji: String = "",
    /** Outline material icon — preferred for the minimal nav bar. */
    val icon: ImageVector? = null,
)

/**
 * Minimal bottom-nav — clean white surface, outline icons, blue active highlight
 * with a small dot/underline indicator. No heavy gradient or pill background.
 */
@Composable
fun AppBottomNavBar(
    items: List<BottomNavItem>,
    selectedKey: String,
    onSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    onFabClick: (() -> Unit)? = null,
    fabEmoji: String = "⚡",
) {
    val withFab = onFabClick != null
    if (withFab) {
        require(items.size == 4) { "AppBottomNavBar with FAB expects exactly 4 tabs." }
    } else {
        require(items.size in 3..6) { "AppBottomNavBar expects 3–6 tabs (got ${items.size})." }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(76.dp)
            .background(
                Brush.verticalGradient(
                    listOf(NavBgTop, NavBgBottom),
                ),
            ),
    ) {
        // Cyan hairline at the top — bridges the dark band into the brand footer above.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(Color(0x3360A5FA))
                .align(Alignment.TopCenter),
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(76.dp)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            if (withFab) {
                NavTab(items[0], selectedKey, onSelected, Modifier.weight(1f))
                NavTab(items[1], selectedKey, onSelected, Modifier.weight(1f))
                Spacer(Modifier.weight(1f))   // FAB slot
                NavTab(items[2], selectedKey, onSelected, Modifier.weight(1f))
                NavTab(items[3], selectedKey, onSelected, Modifier.weight(1f))
            } else {
                items.forEach { item ->
                    NavTab(item, selectedKey, onSelected, Modifier.weight(1f))
                }
            }
        }

        if (withFab) {
            Box(
                modifier = Modifier
                    .size(58.dp)
                    .align(Alignment.TopCenter)
                    .offset(y = (-2).dp)
                    .shadow(elevation = 8.dp, shape = CircleShape, spotColor = AppTheme.ShadowMd)
                    .clip(CircleShape)
                    .background(Brush.verticalGradient(listOf(AppTheme.Brand, AppTheme.Brand700)))
                    .clickable(onClick = onFabClick),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = fabEmoji, fontSize = 28.sp)
            }
        }
    }
}

@Composable
private fun NavTab(
    item: BottomNavItem,
    selectedKey: String,
    onSelected: (String) -> Unit,
    modifier: Modifier,
) {
    val isSelected = item.key == selectedKey
    val activeColor   = NavActive
    val inactiveColor = NavInactive
    val tint by animateColorAsState(
        targetValue = if (isSelected) activeColor else inactiveColor,
        animationSpec = tween(durationMillis = 220),
        label = "navTint",
    )
    val dotAlpha by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (isSelected) 1f else 0f,
        animationSpec = tween(durationMillis = 220),
        label = "navDotAlpha",
    )

    Box(
        modifier = modifier
            .fillMaxHeight()
            .clickable { onSelected(item.key) },
        contentAlignment = Alignment.Center,
    ) {
        // Soft cyan radial halo behind the active tab — fades in/out with the
        // same 220ms curve as the tint animation. Sits behind the icon so
        // selection reads as ambient lighting, not a hard fill.
        if (dotAlpha > 0f) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                activeColor.copy(alpha = 0.22f * dotAlpha),
                                Color.Transparent,
                            ),
                        ),
                    ),
            )
        }
        Column(
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (item.icon != null) {
                Icon(
                    imageVector = item.icon,
                    contentDescription = item.label,
                    tint = tint,
                    modifier = Modifier.size(22.dp),
                )
            } else {
                Text(text = item.emoji, fontSize = 18.sp)
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text       = item.label,
                color      = tint,
                fontSize   = 10.sp,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
            )
            Spacer(Modifier.height(4.dp))
            // Tiny underline indicator under the active tab.
            Box(
                modifier = Modifier
                    .width(14.dp)
                    .height(3.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(activeColor.copy(alpha = dotAlpha)),
            )
        }
    }
}
