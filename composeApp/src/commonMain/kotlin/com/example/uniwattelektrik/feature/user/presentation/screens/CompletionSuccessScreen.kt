package com.example.uniwattelektrik.feature.user.presentation.screens

import androidx.compose.animation.core.EaseOutBack
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/* Local tokens (mirror CompleteWorkScreen) */
private val ScreenBg     = Color(0xFFF4F7FB)
private val CardBg       = Color(0xFFFFFFFF)
private val InkPrimary   = Color(0xFF1A2B49)
private val InkSecondary = Color(0xFF6B7A99)
private val InkMuted     = Color(0xFF94A3B8)
private val Brand        = Color(0xFF3B82F6)
private val BrandDeep    = Color(0xFF1D4ED8)
private val Success      = Color(0xFF22C55E)
private val SuccessSoft  = Color(0xFFE8FBF1)
private val SuccessDeep  = Color(0xFF15803D)
private val Warning      = Color(0xFFF59E0B)
private val Danger       = Color(0xFFEF4444)
private val ShadowSoft   = Color(0x14172C50)
private val DividerSoft  = Color(0xFFE5EAF2)

@Composable
fun CompletionSuccessScreen(
    taskCode: String,
    grandTotal: Int,
    onShareReceipt: () -> Unit,
    onBackToDashboard: () -> Unit,
) {
    var loaded by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { loaded = true }
    val checkScale by animateFloatAsState(
        targetValue = if (loaded) 1f else 0f,
        animationSpec = tween(540, easing = EaseOutBack),
        label = "check-scale",
    )

    var rating by remember { mutableStateOf(4) }
    var feedbackTags by remember {
        mutableStateOf(setOf<String>())
    }

    Box(modifier = Modifier.fillMaxSize().background(SuccessSoft)) {

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = 32.dp, bottom = 110.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    /* Confetti dots */
                    Box(
                        modifier = Modifier.fillMaxWidth().height(160.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        ConfettiDot(Color(0xFFFBBF24), x = (-110).dp, y = (-60).dp)
                        ConfettiDot(Color(0xFF8B5CF6), x = 100.dp, y = (-72).dp)
                        ConfettiDot(Color(0xFF3B82F6), x = 130.dp, y = 30.dp)
                        ConfettiDot(Color(0xFFEC4899), x = (-130).dp, y = 0.dp)
                        ConfettiDot(Color(0xFF22C55E), x = 140.dp, y = 70.dp)
                        ConfettiDot(Color(0xFFFB923C), x = (-90).dp, y = 70.dp)

                        Box(
                            modifier = Modifier
                                .size(120.dp)
                                .scale(checkScale)
                                .shadow(30.dp, CircleShape, spotColor = Success.copy(alpha = 0.5f))
                                .clip(CircleShape)
                                .background(
                                    Brush.radialGradient(listOf(Color(0xFF34D399), Success)),
                                )
                                .border(8.dp, Color.White, CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(Icons.Filled.Check, null, tint = Color.White,
                                 modifier = Modifier.size(58.dp))
                        }
                    }

                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Task Completed!",
                        color = InkPrimary,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Great work, Rajesh. The submission has been sent for supervisor approval.",
                        color = InkSecondary,
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 28.dp),
                    )
                    Spacer(Modifier.height(16.dp))
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(Color.White)
                            .border(1.dp, DividerSoft, RoundedCornerShape(50))
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("#$taskCode", color = InkPrimary,
                             fontSize = 11.sp, fontWeight = FontWeight.Bold,
                             letterSpacing = 1.sp)
                        Spacer(Modifier.width(10.dp))
                        Box(modifier = Modifier
                            .size(3.dp).clip(CircleShape).background(InkMuted))
                        Spacer(Modifier.width(10.dp))
                        Text("SUBMITTED · 1:14 PM", color = Success,
                             fontSize = 11.sp, fontWeight = FontWeight.Bold,
                             letterSpacing = 1.sp)
                    }
                }
            }

            /* Task snapshot */
            item {
                Column(modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .background(SuccessSoft)) {
                    Text("Task snapshot", color = InkPrimary,
                         fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(10.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(10.dp, RoundedCornerShape(20.dp),
                                    spotColor = ShadowSoft)
                            .clip(RoundedCornerShape(20.dp))
                            .background(CardBg)
                            .padding(16.dp),
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(46.dp).clip(RoundedCornerShape(12.dp))
                                        .background(
                                            Brush.linearGradient(
                                                listOf(Color(0xFFFB7185), Danger),
                                            ),
                                        ),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(Icons.Filled.Bolt, null, tint = Color.White,
                                         modifier = Modifier.size(22.dp))
                                }
                                Spacer(Modifier.width(12.dp))
                                Column {
                                    Text("Transformer fault — HSR Phase 2",
                                         color = InkPrimary, fontSize = 14.sp,
                                         fontWeight = FontWeight.Bold)
                                    Spacer(Modifier.height(2.dp))
                                    Text("PRIORITY · HIGH · COMPLETED",
                                         color = InkSecondary, fontSize = 10.sp,
                                         fontWeight = FontWeight.Bold,
                                         letterSpacing = 1.sp)
                                }
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                MetricCard("TIME", "4h 32m",
                                           valueColor = InkPrimary,
                                           modifier = Modifier.weight(1f))
                                MetricCard("SLA", "-1h 28m",
                                           bg = SuccessSoft,
                                           valueColor = Success,
                                           modifier = Modifier.weight(1f))
                                MetricCard("COST", "₹${formatThousands(grandTotal)}",
                                           bg = Color(0xFFE6F0FE),
                                           valueColor = Brand,
                                           modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }

            /* Earnings */
            item {
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Today's earnings", color = InkPrimary,
                             fontSize = 18.sp, fontWeight = FontWeight.Bold,
                             modifier = Modifier.weight(1f))
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(SuccessSoft)
                                .padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(modifier = Modifier
                                .size(6.dp).clip(CircleShape).background(Success))
                            Spacer(Modifier.width(6.dp))
                            Text("+ bonus", color = SuccessDeep,
                                 fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(10.dp, RoundedCornerShape(20.dp),
                                    spotColor = ShadowSoft)
                            .clip(RoundedCornerShape(20.dp))
                            .background(SuccessSoft)
                            .padding(16.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("TASK PAYOUT", color = InkSecondary,
                                     fontSize = 11.sp, fontWeight = FontWeight.Bold,
                                     letterSpacing = 1.sp)
                                Spacer(Modifier.height(4.dp))
                                Text("₹4,950", color = Success,
                                     fontSize = 28.sp, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.height(2.dp))
                                Text("Lead tech rate · 4.5 hr",
                                     color = InkSecondary, fontSize = 11.sp)
                            }
                            Column(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(Success)
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                horizontalAlignment = Alignment.End,
                            ) {
                                Text("+EFFICIENCY", color = Color.White,
                                     fontSize = 10.sp, fontWeight = FontWeight.Bold,
                                     letterSpacing = 1.sp)
                                Spacer(Modifier.height(2.dp))
                                Text("₹620", color = Color.White,
                                     fontSize = 20.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        Box(modifier = Modifier
                            .fillMaxWidth().height(1.dp)
                            .background(Success.copy(alpha = 0.3f)))
                        Spacer(Modifier.height(12.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Total credited", color = InkPrimary,
                                 fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                                 modifier = Modifier.weight(1f))
                            Text("₹5,570", color = InkPrimary,
                                 fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            /* Feedback */
            item {
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    Text("How was this task?", color = InkPrimary,
                         fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(10.dp))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(10.dp, RoundedCornerShape(20.dp),
                                    spotColor = ShadowSoft)
                            .clip(RoundedCornerShape(20.dp))
                            .background(CardBg)
                            .padding(20.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp,
                                Alignment.CenterHorizontally),
                        ) {
                            (1..5).forEach { i ->
                                Icon(
                                    imageVector =
                                        if (i <= rating) Icons.Filled.Star
                                        else Icons.Filled.StarOutline,
                                    contentDescription = null,
                                    tint = Color(0xFFFBBF24),
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clickable { rating = i },
                                )
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "Tell us how the site was, the brief, the customer. Helps us plan better.",
                            color = InkSecondary, fontSize = 13.sp,
                            lineHeight = 19.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Spacer(Modifier.height(14.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp,
                                Alignment.CenterHorizontally),
                        ) {
                            FeedbackChip("Smooth job", Brand,
                                         feedbackTags.contains("Smooth job")) {
                                feedbackTags = toggle(feedbackTags, "Smooth job")
                            }
                            FeedbackChip("Site access tricky", Warning,
                                         feedbackTags.contains("Site access tricky")) {
                                feedbackTags = toggle(feedbackTags, "Site access tricky")
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        Box(modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center) {
                            FeedbackChip("Customer helpful", Success,
                                         feedbackTags.contains("Customer helpful")) {
                                feedbackTags = toggle(feedbackTags, "Customer helpful")
                            }
                        }
                    }
                }
            }

            /* Up next */
            item {
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Up next", color = InkPrimary,
                             fontSize = 18.sp, fontWeight = FontWeight.Bold,
                             modifier = Modifier.weight(1f))
                        Text("View all", color = Brand, fontSize = 12.sp,
                             fontWeight = FontWeight.SemiBold)
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(8.dp, RoundedCornerShape(18.dp),
                                    spotColor = ShadowSoft)
                            .clip(RoundedCornerShape(18.dp))
                            .background(CardBg)
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp).clip(RoundedCornerShape(12.dp))
                                .background(
                                    Brush.linearGradient(listOf(Brand, BrandDeep)),
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(Icons.Filled.Build, null, tint = Color.White,
                                 modifier = Modifier.size(22.dp))
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Meter calibration — Indiranagar",
                                 color = InkPrimary, fontSize = 14.sp,
                                 fontWeight = FontWeight.Bold,
                                 lineHeight = 18.sp)
                            Spacer(Modifier.height(2.dp))
                            Text("#TASK-2451 · Starts 2:30 PM · 6.2 km",
                                 color = InkSecondary, fontSize = 11.sp,
                                 fontWeight = FontWeight.Medium)
                        }
                        Spacer(Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(Brush.horizontalGradient(listOf(Brand, BrandDeep)))
                                .padding(horizontal = 14.dp, vertical = 10.dp)
                                .clickable {},
                        ) {
                            Text("Start →", color = Color.White,
                                 fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        /* Sticky bottom actions */
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(CardBg)
                .border(1.dp, DividerSoft, RoundedCornerShape(0.dp))
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                modifier = Modifier
                    .weight(1f).height(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp, DividerSoft, RoundedCornerShape(12.dp))
                    .background(CardBg)
                    .clickable(onClick = onShareReceipt),
                contentAlignment = Alignment.Center,
            ) {
                Text("📩  Share receipt", color = InkPrimary,
                     fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
            Box(
                modifier = Modifier
                    .weight(1.2f).height(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Brush.horizontalGradient(listOf(Brand, BrandDeep)))
                    .clickable(onClick = onBackToDashboard),
                contentAlignment = Alignment.Center,
            ) {
                Text("🏠  Back to dashboard", color = Color.White,
                     fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

/* ─────────────────────────────────────────────────────────────────────── *
 *  PIECES
 * ─────────────────────────────────────────────────────────────────────── */

@Composable
private fun ConfettiDot(color: Color, x: androidx.compose.ui.unit.Dp, y: androidx.compose.ui.unit.Dp) {
    Box(
        modifier = Modifier
            .offset(x = x, y = y)
            .size(10.dp)
            .clip(RoundedCornerShape(3.dp))
            .background(color),
    )
}

@Composable
private fun MetricCard(
    label: String,
    value: String,
    valueColor: Color,
    modifier: Modifier = Modifier,
    bg: Color = Color(0xFFF1F5F9),
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(bg)
            .padding(vertical = 12.dp, horizontal = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(label, color = InkSecondary, fontSize = 10.sp,
             fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp)
        Text(value, color = valueColor, fontSize = 18.sp,
             fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun FeedbackChip(
    label: String,
    tint: Color,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(if (selected) tint.copy(alpha = 0.18f) else Color(0xFFF1F5F9))
            .border(
                width = if (selected) 1.5.dp else 0.dp,
                color = if (selected) tint else Color.Transparent,
                shape = RoundedCornerShape(50),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = Modifier
            .size(6.dp).clip(CircleShape).background(tint))
        Spacer(Modifier.width(8.dp))
        Text(label, color = if (selected) tint else InkPrimary,
             fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

private fun formatThousands(n: Int): String {
    if (n < 1000) return n.toString()
    val whole = n / 1000
    val frac = (n % 1000) / 100
    return if (frac == 0) "${whole}k" else "${whole}.${frac}k"
}

private fun toggle(set: Set<String>, value: String): Set<String> =
    if (set.contains(value)) set - value else set + value
