package com.example.uniwattelektrik.core.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * The single back-button used in every screen header across the app.
 *
 * Matches the "Task Management" / Kanban screen design:
 *   • 44 × 44 dp rounded square (14 dp corner)
 *   • semi-transparent white fill (18 % alpha)
 *   • 1 dp white border (25 % alpha)
 *   • white auto-mirrored ArrowBack icon
 *
 * Intended to live on top of any **dark / gradient header**. For light-themed
 * headers pass `tint = Color.Black` and a darker `bg` if needed.
 */
@Composable
fun GlassBackButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    bg: Color = Color.White.copy(alpha = 0.18f),
    border: Color = Color.White.copy(alpha = 0.25f),
    tint: Color = Color.White,
) {
    Box(
        modifier = modifier
            .size(44.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(bg)
            .border(1.dp, border, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector        = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = "Back",
            tint               = tint,
            modifier           = Modifier.size(20.dp),
        )
    }
}



