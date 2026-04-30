package com.example.uniwattelektrik.feature.admin.presentation.screens

import androidx.compose.animation.core.EaseOutBack
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.uniwattelektrik.core.theme.AppTypography
import com.example.uniwattelektrik.core.theme.SetStatusBar
import com.example.uniwattelektrik.feature.auth.domain.model.User
import com.example.uniwattelektrik.feature.workforce.data.remote.TaskRecord
import com.example.uniwattelektrik.feature.workforce.presentation.WorkforceViewModel
import com.example.uniwattelektrik.platform.minutesOfDay
import com.example.uniwattelektrik.platform.nowEpochMillis

/* ── Premium fintech design tokens ─────────────────────────────────────── */
// Premium soft-blue header — multi-tone, never flat or heavy.
// (top-left)  3A8DFF → (center) 4F7CFF → (subtle fade) 6B8CFF
/* Premium fintech header — diagonal blue with radial light overlays. */
private val HeaderGrad0  = Color(0xFF3A6BFF)   // top-left, lighter blue
private val HeaderGrad1  = Color(0xFF2F5BEA)   // center
private val HeaderGrad2  = Color(0xFF244EDC)   // bottom-right, deeper blue
private val ScreenBg     = Color(0xFFF4F7FB)   // legacy flat fallback (no longer used directly)
// Whole-screen "blue · white · dark-blue" weave — kept VERY light so cards still pop.
private val ScreenBg0 = Color(0xFFF4F6FF)  // very light blue-lavender tint
private val ScreenBg1 = Color(0xFFE3E9FF)  // soft lavender-blue
private val ScreenBg2 = Color(0xFFDDE6F8)  // balanced neutral blend
private val ScreenBg3 = Color(0xFFD2DBF0)  // deeper cool tone
private val ScreenBg4 = Color(0xFFC4CFE6)  // medium depth (for contrast)
//
//private val ScreenBg0 = Color(0xFFE9EEFF)  // light but noticeable tint
//private val ScreenBg1 = Color(0xFFD6DEFF)  // stronger lavender-blue
//private val ScreenBg2 = Color(0xFFC9D4F5)  // balanced mid tone
//private val ScreenBg3 = Color(0xFFB8C6EA)  // deeper soft blue
//private val ScreenBg4 = Color(0xFFA6B6DA)  // medium-dark (depth layer)
// Frosted-glass card surface — a 40 %-grey blend of white + cool slate.
// The visible gradient is far more pronounced than pure white, giving every
// card on the Home screen a calm, premium "smoke" look.
private val CardTop    = Color(0xFFF1F5F9)   // slate-100 (top, soft white-grey)
private val CardMid    = Color(0xFFE7ECF3)   // 40 % grey blend (mid)
private val CardBottom = Color(0xFFDCE3EC)   // slate-200/300 mix (bottom)
private val InkPrimary   = Color(0xFF0F172A)
private val InkSecondary = Color(0xFF64748B)
private val InkMuted     = Color(0xFF94A3B8)
private val Highlight    = Color(0xFF2979FF)
private val HighlightLt  = Color(0xFF6AA9FF)
private val Success      = Color(0xFF22C55E)
private val Warning      = Color(0xFFF59E0B)
private val Danger       = Color(0xFFEF4444)
private val Navy         = Color(0xFF0F172A)
private val NavyMid      = Color(0xFF334155)
// Premium 3-layer shadow system — soft, wide, diffused, never heavy.
private val SoftShadow1   = Color(0x14000000)   // 8 % black · main soft (0 12 30)
private val SoftShadow2   = Color(0x0D000000)   // 5 % black · secondary spread (0 4 10)
private val GlowShadow    = Color(0x0F3A8DFF)   // 6 % brand blue · subtle ambient glow (0 0 40)
private val CardBorder    = Color(0xFFFFFFFF)   // solid white · clearly defines card corners
private val ShadowSpot   = Color(0x40000000)        // legacy — kept only for header bell glass
private val ShadowAmbient = Color(0x14000000)
private val InnerHi      = Color(0xB3FFFFFF)        // 70 % white inner top highlight
private val Glass        = Color(0x33FFFFFF)        // header icon glass

/* ──────────────────────────────────────────────────────────────────────────
 *  Premium card modifier — 3-layer soft shadow + gradient bg + subtle border
 *  + inner top-highlight reflection. Apply to ANY card on the dashboard for
 *  a consistent floating Stripe / Linear / Apple Pay feel.
 *
 *      Box(modifier = Modifier.premiumCard(RoundedCornerShape(20.dp)).padding(...))
 * ────────────────────────────────────────────────────────────────────────── */
private fun Modifier.premiumCard(
    shape: androidx.compose.ui.graphics.Shape,
): Modifier = this
    // Layer 3 — subtle BRAND-BLUE ambient glow (widest, painted first / bottom).
    //          Pure ambient, no spot, simulating a 0/0/40 blur halo.
    .shadow(
        elevation    = 40.dp,
        shape        = shape,
        ambientColor = GlowShadow,
        spotColor    = GlowShadow,
    )
    // Layer 1 — main soft drop (0 12 30, 8 % black). Wide, diffused.
    .shadow(
        elevation    = 30.dp,
        shape        = shape,
        ambientColor = Color.Transparent,
        spotColor    = SoftShadow1,
    )
    // Layer 2 — tighter contact shadow (0 4 10, 5 % black) for grounding.
    .shadow(
        elevation    = 10.dp,
        shape        = shape,
        ambientColor = Color.Transparent,
        spotColor    = SoftShadow2,
    )
    .clip(shape)
    // Frosted-glass surface: cool grey-white → grey-blue gradient (3-stop) gives
    // a soft "blurred glass" interior without needing a real blur.
    .background(Brush.verticalGradient(listOf(CardTop, CardMid, CardBottom)))
    // 2px solid white border to clearly define the card's rounded corners.
    .border(width = 1.dp, color = CardBorder, shape = shape)
    // Inner top-highlight: 70 % white fading to transparent over the top 40 %.
    .drawBehind {
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(InnerHi, Color.Transparent),
                endY   = size.height * 0.40f,
            ),
        )
    }

@Composable
fun AdminHomeScreen(
    user: User,
    workforceVm: WorkforceViewModel,
    onOpenNotifications: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val employees by workforceVm.employees.collectAsStateWithLifecycle()
    val tasks     by workforceVm.tasks.collectAsStateWithLifecycle()

    val totalEmployees = employees.size.coerceAtLeast(0)
    val activeTasks    = tasks.count { it.status != "Done" }
    val completedTasks = tasks.count { it.status == "Done" }
    val pendingHigh    = tasks.count { it.priority == "High" && it.status != "Done" }

    // One-shot scale-in for the floating KPI grid
    var loaded by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { loaded = true }
    val gridScale by animateFloatAsState(
        targetValue = if (loaded) 1f else 0.97f,
        animationSpec = tween(durationMillis = 480, easing = EaseOutBack),
        label = "kpi-scale",
    )
    val gridAlpha by animateFloatAsState(
        targetValue = if (loaded) 1f else 0f,
        animationSpec = tween(durationMillis = 360, easing = EaseOutCubic),
        label = "kpi-alpha",
    )

    // Live wall-clock minute (drives greeting ⏰ recompute around the boundaries)
    var nowMs by remember { mutableStateOf(nowEpochMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            nowMs = nowEpochMillis()
            kotlinx.coroutines.delay(60_000L)
        }
    }
    val (greetingText, greetingEmoji) = greetingFor(nowMs)

    val recentTasks = remember(tasks) {
        tasks.sortedByDescending { it.id.hashCode() }.take(5)
    }

    // Push a status-bar style that matches the gradient header.
    SetStatusBar(
        color = com.example.uniwattelektrik.core.components.PremiumHeaderStatusBarColor,
        darkIcons = false,
    )

    val listState = rememberLazyListState()

    Column(
        modifier = modifier
            .fillMaxSize()
            // Whole-screen "blue · white · dark-blue" weave — VERY light so the
            // cards still float clearly above it. 5 stops give a subtle hue
            // shift across the full screen height.
            .background(
                Brush.verticalGradient(
                    colorStops = arrayOf(
                        0.00f to ScreenBg0,
                        0.25f to ScreenBg1,
                        0.50f to ScreenBg2,
                        0.75f to ScreenBg3,
                        1.00f to ScreenBg4,
                    ),
                ),
            ),
    ) {
        // Unified gradient header — admin name + bell + greeting all in one
        // block (same pattern as the Employees screen). Sits above the
        // LazyColumn so it stays visible while content scrolls underneath.
        AdminHomeHeader(
            adminName     = user.displayName?.takeIf { it.isNotBlank() }
                ?: user.email.substringBefore("@"),
            greetingText  = greetingText,
            greetingEmoji = greetingEmoji,
            onBell     = onOpenNotifications,
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            state = listState,
            contentPadding = PaddingValues(top = 20.dp, bottom = 110.dp),
            verticalArrangement = Arrangement.spacedBy(22.dp),
        ) {
            // 0. Live status strip — at-a-glance system pulse pills.
            item {
                LiveStatusStrip(
                    online    = totalEmployees.coerceAtMost(99),
                    liveTasks = activeTasks,
                    alerts    = pendingHigh,
                )
            }

            // 1. KPI grid — sits below header (not floating)
            item {
            Column(
                modifier = Modifier
                    .padding(horizontal = 20.dp)
                    .scale(gridScale),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    KpiCard(
                        modifier     = Modifier.weight(1f),
                        icon         = Icons.Filled.People,
                        glow         = Highlight,
                        value        = totalEmployees.toString(),
                        label        = "Total Employees",
                        badgeText    = "+8%",
                        badgeBg      = Color(0xFFDCFCE7),
                        badgeFg      = Success,
                        alpha        = gridAlpha,
                    )
                    KpiCard(
                        modifier     = Modifier.weight(1f),
                        icon         = Icons.AutoMirrored.Filled.Assignment,
                        glow         = Warning,
                        value        = activeTasks.toString(),
                        label        = "Active Tasks",
                        badgeText    = "Live",
                        badgeBg      = Color(0xFFFEF3C7),
                        badgeFg      = Warning,
                        livePulse    = true,
                        alpha        = gridAlpha,
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    KpiCard(
                        modifier     = Modifier.weight(1f),
                        icon         = Icons.Filled.CheckCircle,
                        glow         = Success,
                        value        = completedTasks.toString(),
                        label        = "Completed",
                        badgeText    = "+12%",
                        badgeBg      = Color(0xFFDCFCE7),
                        badgeFg      = Success,
                        alpha        = gridAlpha,
                    )
                    KpiCard(
                        modifier     = Modifier.weight(1f),
                        icon         = Icons.Filled.WarningAmber,
                        glow         = Danger,
                        value        = pendingHigh.toString(),
                        label        = "Pending",
                        badgeText    = if (pendingHigh > 0) "Alert" else "OK",
                        badgeBg      = if (pendingHigh > 0) Color(0xFFFEE2E2) else Color(0xFFDCFCE7),
                        badgeFg      = if (pendingHigh > 0) Danger else Success,
                        alpha        = gridAlpha,
                    )
                }
            }
        }

        // 3. Performance chart
        item {
            Column(
                modifier = Modifier.padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text       = "Performance · 7 days",
                            color      = InkPrimary,
                            fontSize   = 17.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-0.2).sp,
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text     = "Tasks assigned vs completed",
                            color    = InkSecondary,
                            fontSize = 12.sp,
                        )
                    }
                    Text(
                        text       = "This week →",
                        color      = Highlight,
                        fontSize   = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }

                PerformanceChartCard(
                    completed = chartSeed(completedTasks, jitter = listOf(0, 2, -1, 4, 1, 5, 2)),
                    assigned  = chartSeed(activeTasks + completedTasks, jitter = listOf(2, 3, 1, 5, 3, 6, 4)),
                )

                // Insight strip — quick at-a-glance trend summary.
                Row(
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(Color(0xFFDCFCE7))
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                    ) {
                        Text(
                            text       = "▲ +12%",
                            color      = Success,
                            fontSize   = 11.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    Text(
                        text       = "improvement vs last week",
                        color      = InkSecondary,
                        fontSize   = 12.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
        }

        // 4. Recent Activity — real-time pulse of the team.
        item {
            Column(
                modifier = Modifier.padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text       = "Recent activity",
                        color      = InkPrimary,
                        fontSize   = 17.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.2).sp,
                        modifier   = Modifier.weight(1f),
                    )
                    // Live pulse pill
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(Color(0xFFDBEAFE))
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        val t = rememberInfiniteTransition(label = "live")
                        val s by t.animateFloat(
                            initialValue = 0.7f,
                            targetValue  = 1.2f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(900, easing = LinearEasing),
                                repeatMode = RepeatMode.Reverse,
                            ),
                            label = "live-scale",
                        )
                        Box(modifier = Modifier
                            .size(6.dp)
                            .scale(s)
                            .clip(CircleShape)
                            .background(Highlight))
                        Text(
                            text       = "Live",
                            color      = Highlight,
                            fontSize   = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }

                RecentActivityCard()
            }
        }

        // 4. Latest tasks list
        item {
            Column(
                modifier = Modifier.padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text       = "Latest tasks",
                        color      = InkPrimary,
                        fontSize   = 17.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.2).sp,
                        modifier   = Modifier.weight(1f),
                    )
                    Text(
                        text       = "View all →",
                        color      = Highlight,
                        fontSize   = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }

                if (recentTasks.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .premiumCard(RoundedCornerShape(18.dp))
                            .padding(20.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            "No tasks yet — assign your first one.",
                            color = InkMuted,
                            fontSize = 13.sp,
                        )
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        recentTasks.forEach { task ->
                            LatestTaskRow(task = task)
                        }
                    }
                }
            }
        }
        }   // LazyColumn
    }       // outer Column
}

/* ──────────────────────────────────────────────────────────────────────────
 *  Unified gradient header — admin name + bell + greeting all in one block,
 *  same scrolling behaviour as the Employees screen header (sits above the
 *  LazyColumn, not sticky / not split). The gradient is painted ONCE so the
 *  header reads as a single cohesive block, no sub-region color mismatch.
 * ────────────────────────────────────────────────────────────────────────── */
@Composable
private fun AdminHomeHeader(
    adminName: String,
    greetingText: String,
    greetingEmoji: String,
    onBell: () -> Unit,
) {
    com.example.uniwattelektrik.core.components.PremiumHeaderBackground(
        modifier = Modifier
            .shadow(
                elevation    = 18.dp,
                shape        = RoundedCornerShape(bottomStart = 30.dp, bottomEnd = 30.dp),
                ambientColor = Color.Transparent,
                spotColor    = Color(0x14000000),
            ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(horizontal = 22.dp)
                .padding(top = 14.dp, bottom = 26.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // Title row: "Admin Console" + "OPS OVERVIEW" subtitle + bell
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "Admin Console", style = AppTypography.HeaderTitle)
                        Text(
                            text = "Operation Overview",
                            style = AppTypography.HeaderSubtitle,
                            color = Color(0xCCFFFFFF),    // 80 % white — lighter opacity
                        )
                    }
                    BellIcon(onClick = onBell, showDot = true)
                }

                // Greeting — slightly brighter white for emphasis.
                Text(
                    text  = "$greetingText, $adminName $greetingEmoji",
                    style = AppTypography.HeaderGreeting,
                    color = Color.White,
                )
            }
        }
    }
}

/**
 * Clean outlined bell with a soft orange/red notification dot.
 * No glass chrome — sits directly on the gradient at 90 % white opacity,
 * matching the premium fintech reference.
 */
@Composable
private fun BellIcon(
    onClick: () -> Unit,
    showDot: Boolean = false,
) {
    Box(
        modifier = Modifier
            .size(46.dp)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        androidx.compose.material3.Icon(
            imageVector = androidx.compose.material.icons.Icons.Outlined.Notifications,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.92f),
            modifier = Modifier.size(28.dp),
        )
        if (showDot) {
            val t = rememberInfiniteTransition(label = "dot-pulse")
            val s by t.animateFloat(
                initialValue = 0.85f,
                targetValue  = 1.18f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 900, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse,
                ),
                label = "dot-pulse-scale",
            )
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .align(Alignment.TopEnd)
                    .offset(x = (-9).dp, y = 9.dp)
                    .scale(s)
                    .clip(CircleShape)
                    // Soft red/orange — matches the reference badge.
                    .background(Color(0xFFFB7C3C))
                    .border(2.dp, Color.White.copy(alpha = 0.9f), CircleShape),
            )
        }
    }
}

@Composable
private fun GlassIcon(
    emoji: String,
    onClick: () -> Unit,
    showDot: Boolean = false,
    bounce: Boolean = false,
) {
    // Optional gentle bounce for the bell when there's a notification
    val bounceScale = if (bounce) {
        val t = rememberInfiniteTransition(label = "bell-bounce")
        t.animateFloat(
            initialValue = 1f,
            targetValue  = 1.08f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 1100, easing = EaseOutCubic),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "bell-bounce-scale",
        ).value
    } else 1f

    Box(modifier = Modifier.size(44.dp).scale(bounceScale)) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                // Soft outer drop — subtle, integrated with header
                .shadow(
                    elevation    = 10.dp,
                    shape        = RoundedCornerShape(16.dp),
                    ambientColor = Color.Transparent,
                    spotColor    = Color(0x33000000),
                )
                .clip(RoundedCornerShape(16.dp))
                // Semi-transparent glass background
                .background(Color(0x26FFFFFF))   // 15 % white
                // 1px translucent white border for the rim
                .border(
                    width = 1.dp,
                    color = Color(0x40FFFFFF),
                    shape = RoundedCornerShape(16.dp),
                )
                // Inner top highlight + subtle bottom inner glow
                .drawBehind {
                    drawRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(Color(0x4DFFFFFF), Color(0x00FFFFFF)),
                            endY = size.height * 0.55f,
                        ),
                    )
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Color(0x33FFFFFF), Color(0x00FFFFFF)),
                            center = Offset(size.width * 0.5f, size.height * 0.5f),
                            radius = size.width * 0.6f,
                        ),
                        center = Offset(size.width * 0.5f, size.height * 0.5f),
                        radius = size.width * 0.6f,
                    )
                }
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = emoji, fontSize = 18.sp, color = Color.White)
        }
        if (showDot) {
            // Pulsing notification dot
            val t = rememberInfiniteTransition(label = "dot-pulse")
            val s by t.animateFloat(
                initialValue = 0.85f,
                targetValue  = 1.15f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 900, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse,
                ),
                label = "dot-pulse-scale",
            )
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .align(Alignment.TopEnd)
                    .offset(x = (-4).dp, y = 4.dp)
                    .scale(s)
                    .clip(CircleShape)
                    .background(Color(0xFFFF5A5F)),
            )
        }
    }
}

/* ──────────────────────────────────────────────────────────────────────────
 *  KPI card — gradient surface, soft shadow, inner top highlight,
 *  rounded gradient icon, status pill (with optional Live pulse)
 * ────────────────────────────────────────────────────────────────────────── */
@Composable
private fun KpiCard(
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    glow: Color,
    value: String,
    label: String,
    badgeText: String,
    badgeBg: Color,
    badgeFg: Color,
    livePulse: Boolean = false,
    showSparkline: Boolean = false,
    sparkLight: Color = Color(0xFFA5C8FF),
    sparkDark: Color = Highlight,
    alpha: Float = 1f,
) {
    val shape = RoundedCornerShape(22.dp)
    Box(
        modifier = modifier
            .premiumCard(shape)
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.scale(0.94f + 0.06f * alpha),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Plain professional icon — no backing box, just a tint that
                // matches the card's glow colour for a subtle premium feel.
                Icon(
                    imageVector        = icon,
                    contentDescription = null,
                    tint               = glow,
                    modifier           = Modifier.size(28.dp),
                )

                Spacer(Modifier.weight(1f))

                StatusPill(
                    text     = badgeText,
                    bg       = badgeBg,
                    fg       = badgeFg,
                    livePulse = livePulse,
                )
            }

            Text(
                text       = value,
                color      = InkPrimary,
                fontSize   = 26.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = (-0.5).sp,
            )
            Text(
                text       = label,
                color      = InkSecondary,
                fontSize   = 11.sp,
                fontWeight = FontWeight.Medium,
            )

            if (showSparkline) {
                MiniBars(
                    values     = listOf(3, 5, 4, 6, 5, 7, 8),
                    barLight   = sparkLight,
                    barDark    = sparkDark,
                )
            }
        }
    }
}

@Composable
private fun StatusPill(text: String, bg: Color, fg: Color, livePulse: Boolean) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(bg)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (livePulse) {
            val t = rememberInfiniteTransition(label = "live-pulse")
            val s by t.animateFloat(
                initialValue = 0.7f,
                targetValue  = 1.2f,
                animationSpec = infiniteRepeatable(
                    animation = tween(900, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse,
                ),
                label = "live-pulse-scale",
            )
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .scale(s)
                    .clip(CircleShape)
                    .background(fg),
            )
        }
        Text(
            text       = text,
            color      = fg,
            fontSize   = 10.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun MiniBars(values: List<Int>, barLight: Color, barDark: Color) {
    val max = (values.maxOrNull() ?: 1).coerceAtLeast(1)
    val lastIndex = values.lastIndex
    Row(
        modifier = Modifier.fillMaxWidth().height(24.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        values.forEachIndexed { i, v ->
            val ratio = v.toFloat() / max
            // Last bar = darkest (highlight); the rest fade from light → dark
            val color = if (i == lastIndex) {
                barDark
            } else {
                val t = i.toFloat() / lastIndex.coerceAtLeast(1)
                lerp(barLight, barDark, t * 0.55f)
            }
            Box(
                modifier = Modifier
                    .width(8.dp)
                    .height((24f * ratio).dp.coerceAtLeast(5.dp))
                    .clip(RoundedCornerShape(4.dp))
                    .background(color),
            )
        }
    }
}

/** Linear-interpolate between two ARGB colors. */
private fun lerp(a: Color, b: Color, t: Float): Color {
    val tt = t.coerceIn(0f, 1f)
    return Color(
        red   = a.red   + (b.red   - a.red)   * tt,
        green = a.green + (b.green - a.green) * tt,
        blue  = a.blue  + (b.blue  - a.blue)  * tt,
        alpha = a.alpha + (b.alpha - a.alpha) * tt,
    )
}

/* ──────────────────────────────────────────────────────────────────────────
 *  Performance chart — gradient bars, soft shadow, animated rise from bottom
 * ────────────────────────────────────────────────────────────────────────── */
@Composable
private fun PerformanceChartCard(
    completed: List<Int>,
    assigned: List<Int>,
) {
    require(completed.size == 7 && assigned.size == 7)
    val days = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")
    val shape = RoundedCornerShape(24.dp)

    // Bars rise from 0 → 1 once
    var animate by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { animate = true }
    val rise by animateFloatAsState(
        targetValue   = if (animate) 1f else 0f,
        animationSpec = tween(durationMillis = 900, easing = EaseOutCubic),
        label = "chart-rise",
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .premiumCard(shape)
            .padding(20.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(20.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                LegendDot(color = Highlight, label = "Completed")
                LegendDot(color = Navy,      label = "Assigned")
            }

            val maxVal: Int = (completed + assigned).maxOrNull()?.coerceAtLeast(1) ?: 1
            val chartHeight: Dp = 150.dp
            val scrollState = rememberScrollState()

            // Pin viewport to the last data point (Sunday) on first layout so the
            // most recent day is always visible — user can swipe back for older days.
            var initialScrolled by remember { mutableStateOf(false) }
            LaunchedEffect(scrollState.maxValue) {
                if (!initialScrolled && scrollState.maxValue > 0) {
                    scrollState.scrollTo(scrollState.maxValue)
                    initialScrolled = true
                }
            }

            // Show only 3 day-groups in the viewport; the rest scroll horizontally.
            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val visibleGroups = 3
                val dayWidth: Dp = maxWidth / visibleGroups

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(scrollState)
                        .height(chartHeight),
                    verticalAlignment = Alignment.Bottom,
                ) {
                    repeat(7) { i ->
                        Column(
                            modifier = Modifier.width(dayWidth),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Row(
                                modifier = Modifier.height(chartHeight - 22.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.Bottom,
                            ) {
                                ChartBar(
                                    ratio = (completed[i].toFloat() / maxVal) * rise,
                                    fill  = Brush.verticalGradient(listOf(HighlightLt, Highlight)),
                                )
                                ChartBar(
                                    ratio = (assigned[i].toFloat() / maxVal) * rise,
                                    fill  = Brush.verticalGradient(listOf(NavyMid, Navy)),
                                )
                            }
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text       = days[i],
                                color      = InkMuted,
                                fontSize   = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines   = 1,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChartBar(ratio: Float, fill: Brush) {
    Box(
        modifier = Modifier
            .width(32.dp)
            .fillMaxHeight(ratio.coerceIn(0.04f, 1f))
            .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
            .background(fill),
    )
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color),
        )
        Text(text = label, color = InkSecondary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}

/** Spread a single live count across 7 days using fixed jitter. */
private fun chartSeed(total: Int, jitter: List<Int>): List<Int> {
    val base = (total / 7).coerceAtLeast(1)
    return jitter.map { (base + it).coerceAtLeast(1) }
}

/* ──────────────────────────────────────────────────────────────────────────
 *  Time-aware greeting helper
 * ────────────────────────────────────────────────────────────────────────── */
/** Returns ("Good morning"/"Good afternoon"/"Good evening", emoji) for [nowMs]. */
private fun greetingFor(nowMs: Long): Pair<String, String> {
    val mins = minutesOfDay(nowMs)
    val hour = mins / 60
    return when {
        hour in 5..11  -> "Good morning"   to "☀️"
        hour in 12..16 -> "Good afternoon" to "🌤️"
        hour in 17..20 -> "Good evening"   to "🌤️"
        else           -> "Good night"     to "🌙"
    }
}

/* ──────────────────────────────────────────────────────────────────────────
 *  Live Status Strip — three soft-glow pulse pills sitting above the KPI grid.
 *  Communicates real-time system health at a glance.
 * ────────────────────────────────────────────────────────────────────────── */
@Composable
private fun LiveStatusStrip(online: Int, liveTasks: Int, alerts: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        StatusPulsePill(
            modifier = Modifier.weight(1f),
            color    = Success,
            label    = "Online",
            value    = online.toString(),
        )
        StatusPulsePill(
            modifier = Modifier.weight(1f),
            color    = Highlight,
            label    = "Live Tasks",
            value    = liveTasks.toString(),
        )
        StatusPulsePill(
            modifier = Modifier.weight(1f),
            color    = if (alerts > 0) Danger else Success,
            label    = if (alerts > 0) "Alerts" else "All Clear",
            value    = alerts.toString().takeIf { alerts > 0 } ?: "✓",
        )
    }
}

@Composable
private fun StatusPulsePill(
    modifier: Modifier = Modifier,
    color: Color,
    label: String,
    value: String,
) {
    val t = rememberInfiniteTransition(label = "pulse-$label")
    val s by t.animateFloat(
        initialValue = 0.8f,
        targetValue  = 1.15f,
        animationSpec = infiniteRepeatable(
            animation  = tween(1100, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulse-scale-$label",
    )
    Row(
        modifier = modifier
            .shadow(
                elevation    = 8.dp,
                shape        = RoundedCornerShape(999.dp),
                ambientColor = Color.Transparent,
                spotColor    = color.copy(alpha = 0.35f),
            )
            .clip(RoundedCornerShape(999.dp))
            .background(Color.White)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .scale(s)
                .clip(CircleShape)
                .background(color),
        )
        Text(
            text       = value,
            color      = InkPrimary,
            fontSize   = 13.sp,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text       = label,
            color      = InkSecondary,
            fontSize   = 11.sp,
            fontWeight = FontWeight.Medium,
            maxLines   = 1,
        )
    }
}

/* ──────────────────────────────────────────────────────────────────────────
 *  Recent Activity card — minimal time-stamped list (check-ins, completions,
 *  alerts). Premium card surface matching the rest of the dashboard.
 * ────────────────────────────────────────────────────────────────────────── */
@Composable
private fun RecentActivityCard() {
    val shape = RoundedCornerShape(20.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .premiumCard(shape)
            .padding(horizontal = 18.dp, vertical = 14.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            ActivityRow(
                emoji = "✅", tint = Success,
                title = "Task completed",
                body  = "Alex closed “Wiring inspection · Block C”",
                time  = "2m ago",
            )
            ActivityDivider()
            ActivityRow(
                emoji = "🟢", tint = Highlight,
                title = "Check-in",
                body  = "Priya checked in at site Atrium 4F",
                time  = "12m ago",
            )
            ActivityDivider()
            ActivityRow(
                emoji = "⚠️", tint = Danger,
                title = "High-priority alert",
                body  = "Generator #2 voltage dip · auto-ticket raised",
                time  = "32m ago",
            )
            ActivityDivider()
            ActivityRow(
                emoji = "📋", tint = Warning,
                title = "Task assigned",
                body  = "“Conduit replacement · Floor 7” → Ravi",
                time  = "1h ago",
            )
        }
    }
}

@Composable
private fun ActivityRow(
    emoji: String,
    tint: Color,
    title: String,
    body: String,
    time: String,
) {
    Row(verticalAlignment = Alignment.Top) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(tint.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center,
        ) { Text(text = emoji, fontSize = 14.sp) }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text       = title,
                color      = InkPrimary,
                fontSize   = 13.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text     = body,
                color    = InkSecondary,
                fontSize = 11.sp,
                maxLines = 1,
            )
        }
        Spacer(Modifier.width(8.dp))
        Text(
            text     = time,
            color    = InkMuted,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
private fun ActivityDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(Color(0xFFE2E8F0)),
    )
}

/* ──────────────────────────────────────────────────────────────────────────
 *  Latest tasks list row (premium card)
 * ────────────────────────────────────────────────────────────────────────── */
@Composable
private fun LatestTaskRow(task: TaskRecord) {
    val shape = RoundedCornerShape(18.dp)
    val (priorityColor, priorityBg) = when (task.priority) {
        "High"   -> Danger  to Color(0xFFFEE2E2)
        "Low"    -> Success to Color(0xFFDCFCE7)
        else     -> Warning to Color(0xFFFEF3C7)
    }
    val (statusColor, statusBg, statusLabel) = when (task.status) {
        "Done"       -> Triple(Success, Color(0xFFDCFCE7), "Completed")
        "InProgress" -> Triple(Highlight, Color(0xFFDBEAFE), "In progress")
        else         -> Triple(InkSecondary, Color(0xFFE2E8F0), "Pending")
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .premiumCard(shape)
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Left coloured rail by priority
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(40.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(priorityColor),
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text       = task.title,
                    color      = InkPrimary,
                    fontSize   = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines   = 1,
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    text     = "📍 ${task.location} · ${task.day} ${task.time}".trim(),
                    color    = InkSecondary,
                    fontSize = 11.sp,
                    maxLines = 1,
                )
            }
            Spacer(Modifier.width(10.dp))
            Column(horizontalAlignment = Alignment.End) {
                // Priority pill
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(priorityBg)
                        .padding(horizontal = 8.dp, vertical = 3.dp),
                ) {
                    Text(
                        text       = task.priority,
                        color      = priorityColor,
                        fontSize   = 9.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Spacer(Modifier.height(4.dp))
                // Status pill
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(statusBg)
                        .padding(horizontal = 8.dp, vertical = 3.dp),
                ) {
                    Text(
                        text       = statusLabel,
                        color      = statusColor,
                        fontSize   = 9.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}

