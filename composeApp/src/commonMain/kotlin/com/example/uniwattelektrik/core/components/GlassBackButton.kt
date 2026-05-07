package com.example.uniwattelektrik.core.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.uniwattelektrik.core.theme.AppShapes
import com.example.uniwattelektrik.core.theme.AppTheme

/**
 * Glass-style back button for use inside gradient headers.
 *
 *   • 44 × 44 dp rounded square ([AppShapes.medium] = 12 dp corner)
 *   • Semi-transparent white fill (18 % alpha)
 *   • 1 dp white border (25 % alpha)
 *   • Auto-mirrored ArrowBack icon in [tint]
 *
 * Intended for dark / gradient header backgrounds. For light headers,
 * pass `tint = AppTheme.Ink900` and a matching `bg`.
 */
@Composable
fun GlassBackButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    bg      : Color = Color.White.copy(alpha = 0.18f),
    border  : Color = Color.White.copy(alpha = 0.25f),
    tint    : Color = Color.White,
) {
    Box(
        modifier          = modifier
            .size(44.dp)
            .clip(AppShapes.medium)
            .background(bg)
            .border(1.dp, border, AppShapes.medium)
            .clickable(onClick = onClick),
        contentAlignment  = Alignment.Center,
    ) {
        Icon(
            imageVector        = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = "Back",
            tint               = tint,
            modifier           = Modifier.size(AppTheme.IconMd),
        )
    }
}
