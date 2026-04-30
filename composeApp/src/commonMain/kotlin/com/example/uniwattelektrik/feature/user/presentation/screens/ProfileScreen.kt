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
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.uniwattelektrik.core.components.AppCard
import com.example.uniwattelektrik.core.theme.AppTheme
import com.example.uniwattelektrik.feature.auth.domain.model.User

@Composable
fun ProfileScreen(
    user: User,
    initials: String,
    name: String,
    role: String,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier,
) {
    com.example.uniwattelektrik.core.theme.SetStatusBar(
        color = AppTheme.Bg, darkIcons = true,
    )
    LazyColumn(
        modifier = modifier.fillMaxSize().background(appScreenBackground()),
        contentPadding = PaddingValues(
            start = 20.dp, end = 20.dp,
            top   = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 24.dp,
            bottom = 100.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // ── Identity card ──────────────────────────────────────────────────
        item {
            AppCard(contentPadding = 0.dp) {
                Column {
                    // Brand gradient banner
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(90.dp)
                            .background(
                                Brush.verticalGradient(listOf(
                                    Color(0xFF3B82F6),
                                    Color(0xFF1D4ED8),
                                    Color(0xFF0F172A),
                                ))
                            )
                    )
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        // Avatar — overlaps banner
                        Box(
                            modifier = Modifier
                                .size(76.dp)
                                .padding(bottom = 0.dp)
                                .shadow(12.dp, RoundedCornerShape(20.dp), spotColor = AppTheme.ShadowSpotBlue)
                                .clip(RoundedCornerShape(20.dp))
                                .background(Brush.linearGradient(listOf(Color(0xFFFFB28A), Color(0xFFEC8552))))
                                .padding(8.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(initials, color = Color.White, fontSize = 24.sp,
                                 fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.height(12.dp))
                        Text(name, color = AppTheme.Ink900, fontSize = 20.sp,
                             fontWeight = FontWeight.Bold)
                        Text("// $role", color = AppTheme.Ink500, fontSize = 12.sp,
                             fontFamily = FontFamily.Monospace, letterSpacing = 0.4.sp)
                        Text(user.email, color = AppTheme.Ink300, fontSize = 12.sp)
                    }
                }
            }
        }

        // ── Quick stats ────────────────────────────────────────────────────
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MiniStat("142", "Tasks done",     AppTheme.Brand, Modifier.weight(1f))
                MiniStat("96 %", "On-time rate",  AppTheme.Low,   Modifier.weight(1f))
                MiniStat("18",  "Days leave left",AppTheme.Med,   Modifier.weight(1f))
            }
        }

        // ── Settings sections ──────────────────────────────────────────────
        item {
            ProfileSection(title = "Account", items = listOf(
                "👤" to "Personal information",
                "🛡" to "Security & PIN",
                "🌐" to "Language & region",
            ))
        }
        item {
            ProfileSection(title = "Work", items = listOf(
                "📋" to "Shift roster",
                "🛠" to "Equipment & PPE",
                "📜" to "Compliance & training",
            ))
        }
        item {
            ProfileSection(title = "Support", items = listOf(
                "💬" to "Contact ops",
                "📩" to "Send feedback",
                "ℹ️" to "About UniWatt ElekTrik",
            ))
        }

        // ── Logout ─────────────────────────────────────────────────────────
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(AppTheme.HighBg)
                    .clickable(onClick = onLogout),
                contentAlignment = Alignment.Center,
            ) {
                Text("Sign out", color = AppTheme.High,
                     fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun MiniStat(value: String, label: String, accent: Color, modifier: Modifier) {
    AppCard(modifier = modifier, contentPadding = 14.dp) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(value, color = accent, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text(label, color = AppTheme.Ink500, fontSize = 10.sp,
                 fontWeight = FontWeight.SemiBold, letterSpacing = 0.4.sp)
        }
    }
}

@Composable
private fun ProfileSection(title: String, items: List<Pair<String, String>>) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, color = AppTheme.Ink500, fontSize = 11.sp,
             fontWeight = FontWeight.SemiBold, letterSpacing = 0.7.sp,
             modifier = Modifier.padding(start = 4.dp))
        AppCard(contentPadding = 0.dp) {
            Column {
                items.forEachIndexed { idx, (emoji, label) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { /* TODO */ }
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(emoji, fontSize = 18.sp)
                        Spacer(Modifier.width(14.dp))
                        Text(label, color = AppTheme.Ink900, fontSize = 14.sp,
                             modifier = Modifier.weight(1f))
                        Text("›", color = AppTheme.Ink300, fontSize = 18.sp)
                    }
                    if (idx < items.lastIndex) {
                        Box(modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .padding(start = 48.dp)
                            .background(AppTheme.Ink100))
                    }
                }
            }
        }
    }
}
