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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.uniwattelektrik.core.components.AppCard
import com.example.uniwattelektrik.core.sample.LeaveType
import com.example.uniwattelektrik.core.sample.SampleLeave
import com.example.uniwattelektrik.core.sample.SampleLeaves
import com.example.uniwattelektrik.core.theme.AppTheme

private enum class LeaveTab { Apply, History, Balance }

@Composable
fun LeaveScreen(modifier: Modifier = Modifier) {
    var tab by remember { mutableStateOf(LeaveTab.Apply) }

    com.example.uniwattelektrik.core.theme.SetStatusBar(
        color = AppTheme.Bg, darkIcons = true,
    )

    Column(modifier = modifier.fillMaxSize().background(appScreenBackground())) {

        // Header
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(20.dp),
        ) {
            Text(
                "Leave",
                style = com.example.uniwattelektrik.core.theme.AppTypography.HeaderTitle
                    .copy(color = AppTheme.Ink900),
            )
            Spacer(Modifier.height(12.dp))

            // Tab pills
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(AppTheme.Ink50)
                    .padding(4.dp),
            ) {
                TabPill("Apply",   tab == LeaveTab.Apply,   { tab = LeaveTab.Apply   }, Modifier.weight(1f))
                TabPill("History", tab == LeaveTab.History, { tab = LeaveTab.History }, Modifier.weight(1f))
                TabPill("Balance", tab == LeaveTab.Balance, { tab = LeaveTab.Balance }, Modifier.weight(1f))
            }
        }

        when (tab) {
            LeaveTab.Apply   -> LeaveApply()
            LeaveTab.History -> LeaveHistory()
            LeaveTab.Balance -> LeaveBalance()
        }
    }
}

@Composable
private fun TabPill(label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) AppTheme.Surface else Color.Transparent)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(label,
             color      = if (selected) AppTheme.Ink900 else AppTheme.Ink500,
             fontSize   = 13.sp,
             fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium)
    }
}

@Composable
private fun LeaveApply() {
    var selectedType by remember { mutableStateOf(LeaveType.Casual) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            AppCard {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Leave type", color = AppTheme.Ink900, fontSize = 13.sp,
                         fontWeight = FontWeight.SemiBold, letterSpacing = 0.5.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        LeaveType.entries.forEach { type ->
                            LeaveTypeTile(
                                type     = type,
                                selected = type == selectedType,
                                onClick  = { selectedType = type },
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            }
        }
        item {
            AppCard {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Date range", color = AppTheme.Ink900, fontSize = 13.sp,
                         fontWeight = FontWeight.SemiBold, letterSpacing = 0.5.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        DateField(label = "From", value = "21 Mar 2026", Modifier.weight(1f))
                        DateField(label = "To",   value = "21 Mar 2026", Modifier.weight(1f))
                    }
                }
            }
        }
        item {
            AppCard {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Reason", color = AppTheme.Ink900, fontSize = 13.sp,
                         fontWeight = FontWeight.SemiBold, letterSpacing = 0.5.sp)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(96.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(AppTheme.SurfaceMuted)
                            .padding(12.dp),
                    ) {
                        Text("Add reason for leave…", color = AppTheme.Ink300, fontSize = 13.sp)
                    }
                }
            }
        }
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(AppTheme.Brand)
                    .clickable { /* submit */ },
                contentAlignment = Alignment.Center,
            ) {
                Text("Submit request", color = Color.White,
                     fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun LeaveTypeTile(
    type: LeaveType, selected: Boolean, onClick: () -> Unit, modifier: Modifier,
) {
    Column(
        modifier = modifier
            .height(82.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) AppTheme.Brand50 else AppTheme.SurfaceMuted)
            .clickable(onClick = onClick)
            .padding(8.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(type.emoji, fontSize = 26.sp)
        Spacer(Modifier.height(4.dp))
        Text(type.label,
             color      = if (selected) AppTheme.Brand else AppTheme.Ink700,
             fontSize   = 12.sp,
             fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium)
    }
}

@Composable
private fun DateField(label: String, value: String, modifier: Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, color = AppTheme.Ink500, fontSize = 11.sp,
             fontWeight = FontWeight.SemiBold, letterSpacing = 0.5.sp)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(46.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(AppTheme.SurfaceMuted)
                .padding(horizontal = 14.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            Text("📅  $value", color = AppTheme.Ink900, fontSize = 13.sp)
        }
    }
}

@Composable
private fun LeaveHistory() {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        items(SampleLeaves.history, key = { it.id }) { LeaveHistoryRow(it) }
    }
}

@Composable
private fun LeaveHistoryRow(leave: SampleLeave) {
    AppCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .padding(end = 12.dp)
                    .clip(CircleShape)
                    .background(AppTheme.Brand50)
                    .padding(10.dp),
            ) { Text(leave.type.emoji, fontSize = 20.sp) }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("${leave.type.label} · ${leave.days} day${if (leave.days > 1) "s" else ""}",
                     color = AppTheme.Ink900, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Text("${leave.from} → ${leave.to}", color = AppTheme.Ink500, fontSize = 12.sp)
                Text(leave.reason, color = AppTheme.Ink300, fontSize = 11.sp)
            }
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(leave.status.bg)
                    .padding(horizontal = 10.dp, vertical = 4.dp),
            ) {
                Text(leave.status.label, color = leave.status.color, fontSize = 11.sp,
                     fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun LeaveBalance() {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(SampleLeaves.balance) { (type, used, total) ->
            AppCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(type.emoji, fontSize = 26.sp)
                    Spacer(Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(type.label, color = AppTheme.Ink900, fontSize = 14.sp,
                             fontWeight = FontWeight.SemiBold)
                        Text("$used of $total used", color = AppTheme.Ink500, fontSize = 12.sp)
                    }
                    Text("${total - used}", color = AppTheme.Brand,
                         fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.width(4.dp))
                    Text("left", color = AppTheme.Ink500, fontSize = 11.sp)
                }
            }
        }
    }
}
