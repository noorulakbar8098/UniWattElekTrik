package com.example.uniwattelektrik.feature.user.presentation.screens

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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.uniwattelektrik.core.components.AppCard
import com.example.uniwattelektrik.core.components.EmptyState
import com.example.uniwattelektrik.core.sample.SampleNotification
import com.example.uniwattelektrik.core.sample.SampleNotifications
import com.example.uniwattelektrik.core.theme.AppTheme

@Composable
fun NotificationsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize().background(appScreenBackground())) {

        com.example.uniwattelektrik.core.theme.SetStatusBar(
            color = AppTheme.Bg, darkIcons = true,
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            com.example.uniwattelektrik.core.components.GlassBackButton(
                onClick = onBack,
                bg      = AppTheme.Surface,
                border  = AppTheme.Ink100,
                tint    = AppTheme.Ink900,
            )
            Text(
                "Notifications",
                style = com.example.uniwattelektrik.core.theme.AppTypography.HeaderTitle
                    .copy(color = AppTheme.Ink900),
            )
            Spacer(Modifier.weight(1f))
            Text("Mark all read", color = AppTheme.Brand, fontSize = 12.sp,
                 fontWeight = FontWeight.SemiBold)
        }

        if (SampleNotifications.all.isEmpty()) {
            EmptyState(emoji = "🔔", title = "All caught up", body = "You'll see new alerts here.")
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 100.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(SampleNotifications.all, key = { it.id }) { n -> NotificationRow(n) }
            }
        }
    }
}

@Composable
private fun NotificationRow(n: SampleNotification) {
    AppCard {
        Row(verticalAlignment = Alignment.Top) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(n.kind.tint.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) { Text(n.kind.emoji, fontSize = 18.sp) }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(n.title, color = AppTheme.Ink900, fontSize = 14.sp,
                         fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                    if (n.unread) {
                        Box(modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(AppTheme.Brand))
                    }
                }
                Text(n.body,  color = AppTheme.Ink500, fontSize = 12.sp, lineHeight = 17.sp)
                Text(n.time,  color = AppTheme.Ink300, fontSize = 11.sp,
                     modifier = Modifier.padding(top = 4.dp))
            }
        }
    }
}
