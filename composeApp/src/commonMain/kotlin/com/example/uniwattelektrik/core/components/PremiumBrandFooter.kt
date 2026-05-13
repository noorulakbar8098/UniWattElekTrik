package com.example.uniwattelektrik.core.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.layout
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.uniwattelektrik.core.theme.AppTheme

/* ═══════════════════════════════════════════════════════════════════════════
 *  PremiumBrandFooter — elegant pill-style branding strip
 * ═══════════════════════════════════════════════════════════════════════════
 *
 *  Visual reference: Vika dashboard footer.
 *
 *    ─────────────────── (thin gradient divider line)
 *      ╭──────────────────────────────────────╮
 *      │  Made with ❤ in Coimbatore · UniWatt  │   ← soft gradient pill
 *      ╰──────────────────────────────────────╯
 *         Smart Industrial Operations             ← muted gray tagline
 *
 *  Pure Compose, sits on the existing light page mesh — no dark band, no
 *  decorative cityscape. Quiet branding that supports the dashboard
 *  content above it instead of competing with it.
 *
 *  Mount as the last item in the home LazyColumn. [breakOutHorizontal] is
 *  retained for backward compat with existing call sites — when > 0 the
 *  composable extends past the parent's horizontal contentPadding.
 * ═══════════════════════════════════════════════════════════════════════════ */

private val PillTop    = Color(0xFFFDF2F8)   // soft rose
private val PillBottom = Color(0xFFEFF6FF)   // soft sky
private val PillBorder = Color(0x14000000)   // 8 % black hairline

private val DividerStart = Color(0xFFC4B5FD)  // soft violet
private val DividerMid   = Color(0xFFFBCFE8)  // soft pink
private val DividerEnd   = Color(0xFFBFDBFE)  // soft blue

@Composable
fun PremiumBrandFooter(
    modifier         : Modifier = Modifier,
    breakOutHorizontal: Dp = 0.dp,
) {
    Column(
        modifier = modifier
            .let { if (breakOutHorizontal > 0.dp) it.extendHorizontalBy(breakOutHorizontal) else it }
            .fillMaxWidth()
            .padding(vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        // ── Thin gradient divider line above the pill ──
        Box(
            modifier = Modifier
                .width(280.dp)
                .height(1.dp)
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            Color.Transparent,
                            DividerStart,
                            DividerMid,
                            DividerEnd,
                            Color.Transparent,
                        ),
                    ),
                ),
        )

        // ── Pill: "Made with ❤ in Coimbatore · UniWatt Elektrik" ──
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(999.dp))
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(PillTop, PillBottom),
                    ),
                )
                .border(0.5.dp, PillBorder, RoundedCornerShape(999.dp))
                .padding(horizontal = 18.dp, vertical = 9.dp),
        ) {
            Text(
                text = buildAnnotatedString {
                    withStyle(SpanStyle(color = AppTheme.Ink700, fontWeight = FontWeight.Medium)) {
                        append("Made with ")
                    }
                    withStyle(SpanStyle(color = Color(0xFFEC4899))) {   // warm pink heart
                        append("❤")
                    }
                    withStyle(SpanStyle(color = AppTheme.Ink700, fontWeight = FontWeight.Medium)) {
                        append("  in Coimbatore  ·  ")
                    }
                    withStyle(SpanStyle(color = AppTheme.Brand, fontWeight = FontWeight.Bold)) {
                        append("UniWatt Elektrik")
                    }
                },
                fontSize = 13.sp,
                letterSpacing = 0.1.sp,
            )
        }

        // ── Tagline below the pill ──
        Text(
            text          = "Smart Industrial Operations, One Task at a Time",
            color         = AppTheme.Ink500,
            fontSize      = 11.sp,
            fontWeight    = FontWeight.Medium,
            letterSpacing = 0.1.sp,
        )

        Spacer(Modifier.height(2.dp))
    }
}

/**
 * Lays the modified composable out wider than its parent constraints by
 * `extra` on each side and shifts it left by `extra`, so a child of a
 * LazyColumn (which has horizontal contentPadding) can still render
 * edge-to-edge of the screen.
 *
 * Retained for backward compatibility with existing call sites; the new
 * light-pill footer doesn't need a full-bleed dark band, but the modifier
 * stays harmless if invoked with `extra > 0`.
 */
private fun Modifier.extendHorizontalBy(extra: Dp): Modifier = this.layout { measurable, constraints ->
    val extraPx = extra.roundToPx()
    val widened = (constraints.maxWidth + extraPx * 2).coerceAtLeast(0)
    val placeable = measurable.measure(
        constraints.copy(minWidth = widened, maxWidth = widened),
    )
    layout(constraints.maxWidth, placeable.height) {
        placeable.place(-extraPx, 0)
    }
}
