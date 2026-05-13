package com.example.uniwattelektrik.core.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.layout
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/* ═══════════════════════════════════════════════════════════════════════════
 *  PremiumBrandFooter — atmospheric industrial-tech branding band
 * ═══════════════════════════════════════════════════════════════════════════
 *
 *  Pure Compose, no image assets. Stripped of the heavy decorative cityscape
 *  / tower / cabinet artwork that the earlier version drew — that read as a
 *  poster, not a footer. The new band is intentionally quiet: a deep-navy
 *  vertical gradient, a thin cyan glow at the top edge to separate it from
 *  the BottomNav, two barely-there grid hairlines, one soft radial glow, and
 *  a centred wordmark + two tagline rows. Height dropped ~40 % so the
 *  dashboard's content stays the visual priority.
 *
 *  Mount as the last item in the home LazyColumn. Pass [breakOutHorizontal]
 *  equal to the parent's horizontal contentPadding so the dark band runs
 *  edge-to-edge of the screen.
 * ═══════════════════════════════════════════════════════════════════════════ */

private val FooterTop    = Color(0xFF0B1633)   // deep midnight navy
private val FooterMid    = Color(0xFF0A1430)
private val FooterBottom = Color(0xFF080F25)

private val ElectricCyan = Color(0xFF60A5FA)
private val SoftWhite    = Color(0xFFE7EEFB)

@Composable
fun PremiumBrandFooter(
    modifier         : Modifier = Modifier,
    breakOutHorizontal: Dp = 0.dp,
) {
    // Single very slow pulse — drives the top-edge glow + corner glow only.
    // Anything more is visual noise at footer scale.
    val transition = androidx.compose.animation.core.rememberInfiniteTransition(label = "footerPulse")
    val pulse by transition.animateFloat(
        initialValue = 0f,
        targetValue  = 1f,
        animationSpec = infiniteRepeatable(
            animation  = tween(durationMillis = 4_200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulse",
    )

    Box(
        modifier = modifier
            .let { if (breakOutHorizontal > 0.dp) it.extendHorizontalBy(breakOutHorizontal) else it }
            .fillMaxWidth()
            .height(148.dp)
            .background(
                Brush.verticalGradient(listOf(FooterTop, FooterMid, FooterBottom)),
            ),
    ) {
        // 1. Thin cyan glow line at the top edge — separates the footer from
        //    the BottomNav / content above without a hard divider.
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp)
                .align(Alignment.TopCenter),
        ) {
            val midX = size.width / 2f
            val glowAlpha = 0.35f + 0.25f * pulse
            // Horizontal line that fades to transparent at the screen edges.
            val brush = Brush.horizontalGradient(
                colors = listOf(
                    Color.Transparent,
                    ElectricCyan.copy(alpha = glowAlpha),
                    ElectricCyan.copy(alpha = glowAlpha),
                    Color.Transparent,
                ),
                startX = 0f,
                endX   = size.width,
            )
            drawLine(
                brush       = brush,
                start       = Offset(0f, size.height / 2f),
                end         = Offset(size.width, size.height / 2f),
                strokeWidth = 1f,
            )
            // Centre highlight bead — barely perceptible focal point.
            drawCircle(
                color  = ElectricCyan.copy(alpha = 0.55f * pulse),
                radius = 2.2f,
                center = Offset(midX, size.height / 2f),
            )
        }

        // 2. Atmospheric backdrop — 2 hairline grid lines + one soft radial.
        AtmosphericBackdrop(pulse = pulse, modifier = Modifier.fillMaxSize())

        // 3. Centred wordmark + taglines.
        Column(
            modifier            = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            // ── Wordmark — SemiBold, no `#`, cyan accent on "Watt" ──────────
            Text(
                text = buildAnnotatedString {
                    withStyle(SpanStyle(color = Color.White)) { append("Uni") }
                    withStyle(SpanStyle(color = ElectricCyan)) { append("Watt") }
                },
                fontSize      = 26.sp,
                fontWeight    = FontWeight.SemiBold,
                letterSpacing = (-0.4).sp,
            )

            // ── ── ELEKTRIK ── ──
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Box(
                    modifier = Modifier
                        .width(28.dp)
                        .height(1.dp)
                        .background(ElectricCyan.copy(alpha = 0.55f)),
                )
                Text(
                    text  = "ELEKTRIK",
                    color = SoftWhite.copy(alpha = 0.85f),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 3.2.sp,
                )
                Box(
                    modifier = Modifier
                        .width(28.dp)
                        .height(1.dp)
                        .background(ElectricCyan.copy(alpha = 0.55f)),
                )
            }

            Spacer(Modifier.height(2.dp))

            // ── Taglines — compact, two lines, electric-cyan highlights ────
            Text(
                text  = buildAnnotatedString {
                    withStyle(SpanStyle(color = Color.White.copy(alpha = 0.55f))) {
                        append("⚡  ")
                    }
                    withStyle(SpanStyle(color = ElectricCyan)) {
                        append("Smart Industrial")
                    }
                    withStyle(SpanStyle(color = Color.White.copy(alpha = 0.55f))) {
                        append(" Operations")
                    }
                },
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.2.sp,
            )
            Text(
                text = buildAnnotatedString {
                    withStyle(SpanStyle(color = Color.White.copy(alpha = 0.55f))) {
                        append("❤  Built in  ")
                    }
                    withStyle(SpanStyle(color = ElectricCyan)) {
                        append("Coimbatore")
                    }
                },
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.2.sp,
            )
        }
    }
}

/**
 * Lays the modified composable out wider than its parent constraints by
 * `extra` on each side and shifts it left by `extra`, so a child of a
 * LazyColumn (which has horizontal contentPadding) can still render
 * edge-to-edge of the screen.
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

/* ─── Atmospheric backdrop ──────────────────────────────────────────────── */

@Composable
private fun AtmosphericBackdrop(pulse: Float, modifier: Modifier = Modifier) {
    val dash = PathEffect.dashPathEffect(floatArrayOf(3f, 8f), 0f)
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // Two ultra-thin dashed grid lines — barely visible, just enough to
        // give the surface texture without reading as decoration.
        drawLine(
            color = SoftWhite.copy(alpha = 0.05f),
            start = Offset(0f, h * 0.35f),
            end   = Offset(w, h * 0.35f),
            strokeWidth = 0.6f,
            pathEffect  = dash,
        )
        drawLine(
            color = SoftWhite.copy(alpha = 0.04f),
            start = Offset(0f, h * 0.72f),
            end   = Offset(w, h * 0.72f),
            strokeWidth = 0.6f,
            pathEffect  = dash,
        )

        // Soft radial glow in the bottom-right — single ambient highlight.
        val glowSize = w * 0.55f
        drawOval(
            brush   = Brush.radialGradient(
                colors = listOf(
                    ElectricCyan.copy(alpha = 0.12f + 0.06f * pulse),
                    Color.Transparent,
                ),
                center = Offset(w * 0.92f, h * 0.85f),
                radius = glowSize,
            ),
            topLeft = Offset(w * 0.92f - glowSize, h * 0.85f - glowSize),
            size    = Size(glowSize * 2f, glowSize * 2f),
        )
    }
}
