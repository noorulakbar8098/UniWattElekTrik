package com.example.uniwattelektrik.core.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.uniwattelektrik.core.theme.AppTheme

/* ═══════════════════════════════════════════════════════════════════════════
 *  OperationsHeader — premium dark surface header
 * ═══════════════════════════════════════════════════════════════════════════
 *
 *  A reusable enterprise-style page header. Lives alongside (not instead of)
 *  [PremiumHeaderBackground] — the brand-blue gradient is preserved for any
 *  screen that wants the older look.
 *
 *  Two entry points:
 *
 *    [OperationsHeaderSurface]
 *        — Raw dark gradient + status-bar padding. Use when the layout is
 *          bespoke (e.g. ProfileScreen, which needs its own identity block).
 *
 *    [OperationsHeader]
 *        — Opinionated layout used by most main screens:
 *          [back] · [eyebrow] · [title] · [subtitle] · [actions] · [extras]
 *          Pass only what you need; the rest collapses.
 *
 *  Palette is intentionally muted — Ink900 → midnight-navy. No bright brand
 *  blue, no glow rings. Premium ops-dashboard feel.
 * ═══════════════════════════════════════════════════════════════════════════ */

/** Status-bar tint matches the top of [OperationsHeaderSurface]. */
val OperationsHeaderStatusBarColor: Color = AppTheme.Ink900

private val HeaderEnd = Color(0xFF1A2547)   // soft midnight pairing with Ink900

/* ─── Canonical header spacing tokens ─────────────────────────────────────
 *
 *  Single source of truth for ALL screens — so headers never drift in height
 *  or padding between screens. Bespoke callers (Profile / TaskDetail /
 *  EmployeeDetail etc.) also reference these so identity-block headers feel
 *  the same as standard text-only headers.
 */
val HeaderTopPadding         : androidx.compose.ui.unit.Dp = 12.dp
val HeaderBottomPadding      : androidx.compose.ui.unit.Dp = 16.dp
val HeaderHorizontalPadding  : androidx.compose.ui.unit.Dp = 20.dp
/** Space between the top-utility row (back/eyebrow/actions) and the title. */
val HeaderTopRowToTitleGap   : androidx.compose.ui.unit.Dp = 12.dp
/** Space between the title and the optional subtitle. */
val HeaderTitleToSubtitleGap : androidx.compose.ui.unit.Dp = 2.dp
/** Space between the title/subtitle block and the optional extras slot. */
val HeaderBodyToExtrasGap    : androidx.compose.ui.unit.Dp = 12.dp

@Composable
fun OperationsHeaderSurface(
    modifier: Modifier = Modifier,
    statusBarPadding: Boolean = true,
    horizontalPadding: androidx.compose.ui.unit.Dp = HeaderHorizontalPadding,
    bottomPadding: androidx.compose.ui.unit.Dp = HeaderBottomPadding,
    content: @Composable ColumnScope.() -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Brush.linearGradient(
                    colors = listOf(AppTheme.Ink900, HeaderEnd),
                ),
            ),
    ) {
        // ── Atmospheric overlay ──────────────────────────────────────────
        // Pure decoration drawn behind the content slot: a very low-opacity
        // vertical grid (control-panel feel), two horizontal circuit lines,
        // and a soft top-right cyan bloom. Magnitude is intentionally tiny —
        // it should read as ambient depth, not pattern.
        androidx.compose.foundation.Canvas(
            modifier = Modifier.matchParentSize(),
        ) {
            val w = size.width
            val h = size.height
            // Vertical hairline grid — 6 columns at 3% white.
            val cols = 6
            val gridAlpha = 0.03f
            for (i in 1 until cols) {
                val x = w * i / cols
                drawLine(
                    color = Color.White.copy(alpha = gridAlpha),
                    start = androidx.compose.ui.geometry.Offset(x, 0f),
                    end   = androidx.compose.ui.geometry.Offset(x, h),
                    strokeWidth = 0.5f,
                )
            }
            // Two horizontal circuit hairlines.
            drawLine(
                color = Color.White.copy(alpha = 0.045f),
                start = androidx.compose.ui.geometry.Offset(0f, h * 0.32f),
                end   = androidx.compose.ui.geometry.Offset(w, h * 0.32f),
                strokeWidth = 0.6f,
            )
            drawLine(
                color = Color.White.copy(alpha = 0.035f),
                start = androidx.compose.ui.geometry.Offset(0f, h * 0.78f),
                end   = androidx.compose.ui.geometry.Offset(w, h * 0.78f),
                strokeWidth = 0.6f,
            )
            // Top-right ambient cyan bloom — adds cinematic light from the
            // upper corner without competing with the title text.
            val bloomRadius = w * 0.55f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF60A5FA).copy(alpha = 0.12f),
                        Color.Transparent,
                    ),
                    center = androidx.compose.ui.geometry.Offset(w * 0.92f, h * 0.10f),
                    radius = bloomRadius,
                ),
                radius = bloomRadius,
                center = androidx.compose.ui.geometry.Offset(w * 0.92f, h * 0.10f),
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (statusBarPadding) Modifier.windowInsetsPadding(WindowInsets.statusBars) else Modifier)
                .padding(horizontal = horizontalPadding)
                .padding(top = HeaderTopPadding, bottom = bottomPadding),
            content = content,
        )
    }
}

/**
 * Standard main-screen header layout.
 *
 *   ┌──────────────────────────────────────────────┐
 *   │ [←]   WORKSPACE              [edit] [logout] │
 *   │       Operations Console                     │
 *   │                                              │
 *   │   Title                                      │
 *   │   Subtitle                                   │
 *   │                                              │
 *   │   ▾ extras (KPI strip, chips, etc.)         │
 *   └──────────────────────────────────────────────┘
 *
 *  Every slot is optional. Skip [onBack] when the screen is a top-level tab,
 *  skip [eyebrow] when there's no workspace context, skip [actions] when
 *  there's nothing actionable in the header.
 */
@Composable
fun OperationsHeader(
    title          : String,
    modifier       : Modifier = Modifier,
    eyebrow        : String? = null,
    eyebrowDetail  : String? = null,
    subtitle       : String? = null,
    onBack         : (() -> Unit)? = null,
    actions        : (@Composable RowScope.() -> Unit)? = null,
    extras         : (@Composable ColumnScope.() -> Unit)? = null,
) {
    OperationsHeaderSurface(modifier = modifier) {
        // Top utility row — leading back chip / workspace label, trailing actions.
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (onBack != null) {
                    OperationsHeaderIconChip(
                        icon    = Icons.AutoMirrored.Filled.ArrowBack,
                        desc    = "Back",
                        onClick = onBack,
                    )
                    if (eyebrow != null) Spacer(Modifier.width(12.dp))
                }
                if (eyebrow != null) {
                    Column {
                        Text(
                            eyebrow,
                            color = Color.White.copy(alpha = 0.45f),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 1.4.sp,
                        )
                        if (eyebrowDetail != null) {
                            Text(
                                eyebrowDetail,
                                color = Color.White.copy(alpha = 0.9f),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                            )
                        }
                    }
                }
            }
            if (actions != null) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment     = Alignment.CenterVertically,
                    content = actions,
                )
            }
        }

        Spacer(Modifier.height(HeaderTopRowToTitleGap))

        Text(
            title,
            color = Color.White,
            fontSize = 24.sp,
            fontWeight = FontWeight.ExtraBold,
            maxLines = 1,
        )
        if (subtitle != null) {
            Spacer(Modifier.height(HeaderTitleToSubtitleGap))
            Text(
                subtitle,
                color = Color.White.copy(alpha = 0.65f),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
            )
        }
        if (extras != null) {
            Spacer(Modifier.height(HeaderBodyToExtrasGap))
            extras()
        }
    }
}

/**
 * The outlined glass icon chip used inside [OperationsHeader] for back /
 * action buttons. Exposed so screens can put matching chips into [actions].
 */
@Composable
fun OperationsHeaderIconChip(
    icon: ImageVector,
    desc: String,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(Color.White.copy(alpha = 0.08f))
            .border(1.dp, Color.White.copy(alpha = 0.18f), RoundedCornerShape(10.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            icon,
            contentDescription = desc,
            tint = Color.White.copy(alpha = 0.9f),
            modifier = Modifier.size(16.dp),
        )
    }
}
