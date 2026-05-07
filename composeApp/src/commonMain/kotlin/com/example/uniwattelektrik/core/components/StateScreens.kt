package com.example.uniwattelektrik.core.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.uniwattelektrik.core.theme.AppShapes
import com.example.uniwattelektrik.core.theme.AppTheme
import com.example.uniwattelektrik.core.theme.AppTypography

/** Centred indeterminate spinner with optional caption. */
@Composable
fun LoadingState(
    message : String = "Loading…",
    modifier: Modifier = Modifier,
) {
    Box(
        modifier         = modifier.fillMaxSize().padding(AppTheme.SpXl),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(AppTheme.SpMd),
        ) {
            CircularProgressIndicator(color = AppTheme.Brand)
            Text(
                text  = message,
                style = AppTypography.bodySmall.copy(color = AppTheme.Ink500),
            )
        }
    }
}

/** Friendly empty state with an emoji glyph + title + body. */
@Composable
fun EmptyState(
    emoji   : String,
    title   : String,
    body    : String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier         = modifier.fillMaxSize().padding(AppTheme.SpXxl),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(AppTheme.SpSm),
        ) {
            Box(
                modifier          = Modifier
                    .size(72.dp)
                    .clip(AppShapes.circle)
                    .background(AppTheme.Brand50),
                contentAlignment  = Alignment.Center,
            ) {
                Text(emoji, style = AppTypography.displaySmall)
            }
            Text(
                text     = title,
                style    = AppTypography.titleLarge.copy(color = AppTheme.Ink900),
                modifier = Modifier.padding(top = AppTheme.SpSm),
            )
            Text(
                text      = body,
                style     = AppTypography.bodySmall.copy(color = AppTheme.Ink500),
                textAlign = TextAlign.Center,
                modifier  = Modifier.fillMaxWidth(),
            )
        }
    }
}

/** Soft error state — used when a screen fails to load. */
@Composable
fun ErrorState(
    title   : String = "Something went wrong",
    body    : String = "Please pull to refresh or try again later.",
    modifier: Modifier = Modifier,
) = EmptyState(emoji = "⚠️", title = title, body = body, modifier = modifier)
