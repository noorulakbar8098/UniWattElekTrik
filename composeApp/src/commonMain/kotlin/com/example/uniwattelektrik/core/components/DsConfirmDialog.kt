package com.example.uniwattelektrik.core.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.uniwattelektrik.core.theme.AppTheme
import com.example.uniwattelektrik.core.theme.AppTypography

/* ═══════════════════════════════════════════════════════════════════════════
 *  DsConfirmDialog — premium destructive / informational confirmation
 * ═══════════════════════════════════════════════════════════════════════════
 *
 *  Modal dialog with:
 *    • A translucent dark scrim (acts as the "blur" backdrop)
 *    • White card with a halo'd icon at the top
 *    • A rotating dashed ring around the icon (drives the "in-progress" feel
 *      the user asked for — circular dotted line in continuous motion)
 *    • Bold title + soft body (the body supports an emphasised subject name
 *      so the recipient stands out, e.g. **Ravi Kumar** will lose access…)
 *    • Stacked CTA buttons (destructive primary + neutral cancel)
 */

enum class DsConfirmKind { Destructive, Info, Success }

@Composable
fun DsConfirmDialog(
    visible       : Boolean,
    title         : String,
    /**
     * Emphasised subject (e.g. employee name). Rendered in bold ink at the
     * start of the body sentence.
     */
    subject       : String? = null,
    /**
     * Body text. If [subject] is provided, the host is responsible for using
     * "$subject " as the leading phrase or just letting the dialog prepend
     * it automatically (we prepend only when subject is non-null).
     */
    body          : String,
    confirmLabel  : String,
    onConfirm     : () -> Unit,
    onDismiss     : () -> Unit,
    cancelLabel   : String = "Cancel",
    kind          : DsConfirmKind = DsConfirmKind.Destructive,
    icon          : ImageVector = Icons.Filled.Block,
) {
    if (!visible) return

    val (iconTint, iconBg) = when (kind) {
        DsConfirmKind.Destructive -> AppTheme.Danger to AppTheme.DangerBg
        DsConfirmKind.Info        -> AppTheme.Brand  to AppTheme.Brand50
        DsConfirmKind.Success     -> AppTheme.Success to AppTheme.SuccessBg
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties       = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        // Translucent scrim — covers the screen behind the card and dismisses
        // the dialog on tap outside.
        Box(
            modifier         = Modifier
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.55f))
                .clickable(onClick = onDismiss)
                .padding(horizontal = 24.dp),
            contentAlignment = Alignment.Center,
        ) {
            // The card itself — block click propagation so taps inside don't
            // dismiss.
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(20.dp, RoundedCornerShape(24.dp), spotColor = Color.Black.copy(alpha = 0.30f))
                    .clip(RoundedCornerShape(24.dp))
                    .background(AppTheme.Surface)
                    .clickable(enabled = false, onClick = {})
                    .padding(horizontal = 24.dp, vertical = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {

                // ── Icon halo with rotating dashed ring ─────────────────
                IconHaloWithRotatingRing(
                    icon     = icon,
                    iconTint = iconTint,
                    iconBg   = iconBg,
                )

                Spacer(Modifier.height(20.dp))

                // ── Title ────────────────────────────────────────────────
                Text(
                    text  = title,
                    style = AppTypography.titleLarge.copy(
                        color      = AppTheme.Ink900,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize   = 22.sp,
                    ),
                    textAlign = TextAlign.Center,
                )

                Spacer(Modifier.height(10.dp))

                // ── Body (optional bold subject prefix) ─────────────────
                Text(
                    text  = buildAnnotatedString {
                        if (!subject.isNullOrBlank()) {
                            withStyle(SpanStyle(color = AppTheme.Ink900, fontWeight = FontWeight.Bold)) {
                                append(subject)
                            }
                            append(" ")
                        }
                        withStyle(SpanStyle(color = AppTheme.Ink500)) { append(body) }
                    },
                    style     = AppTypography.bodyMedium,
                    textAlign = TextAlign.Center,
                    modifier  = Modifier.fillMaxWidth(),
                )

                Spacer(Modifier.height(24.dp))

                // ── Primary destructive CTA ──────────────────────────────
                val primaryGradient = when (kind) {
                    DsConfirmKind.Destructive -> listOf(Color(0xFFF87171), Color(0xFFDC2626))
                    DsConfirmKind.Info        -> listOf(AppTheme.Brand, AppTheme.Brand700)
                    DsConfirmKind.Success     -> listOf(Color(0xFF34D399), Color(0xFF059669))
                }
                Box(
                    modifier         = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .shadow(10.dp, RoundedCornerShape(14.dp), spotColor = primaryGradient.last().copy(alpha = 0.55f))
                        .clip(RoundedCornerShape(14.dp))
                        .background(Brush.horizontalGradient(primaryGradient))
                        .clickable(onClick = onConfirm),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text  = confirmLabel,
                        style = AppTypography.titleSmall.copy(
                            color      = Color.White,
                            fontWeight = FontWeight.Bold,
                        ),
                    )
                }

                Spacer(Modifier.height(10.dp))

                // ── Cancel ───────────────────────────────────────────────
                Box(
                    modifier         = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(AppTheme.SurfaceMuted)
                        .border(1.dp, AppTheme.Ink100, RoundedCornerShape(14.dp))
                        .clickable(onClick = onDismiss),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text  = cancelLabel,
                        style = AppTypography.titleSmall.copy(
                            color      = AppTheme.Ink900,
                            fontWeight = FontWeight.Bold,
                        ),
                    )
                }
            }
        }
    }
}

/**
 * Static icon tile + an animated dashed ring that rotates around it
 * continuously — the "circle dot line" the design called for.
 */
@Composable
private fun IconHaloWithRotatingRing(
    icon    : ImageVector,
    iconTint: Color,
    iconBg  : Color,
) {
    val rotation by rememberInfiniteTransition(label = "ringRotation").animateFloat(
        initialValue  = 0f,
        targetValue   = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 6_000, easing = LinearEasing),
        ),
        label = "rotation",
    )

    Box(
        modifier         = Modifier.size(112.dp),
        contentAlignment = Alignment.Center,
    ) {
        // Rotating dashed ring
        Canvas(
            modifier = Modifier
                .size(112.dp)
                .rotate(rotation),
        ) {
            val dash = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
            val inset = 4f
            drawArc(
                color     = iconTint.copy(alpha = 0.55f),
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter  = false,
                topLeft    = Offset(inset, inset),
                size       = Size(size.width - inset * 2, size.height - inset * 2),
                style      = Stroke(width = 2.5f, pathEffect = dash),
            )
        }

        // Static icon tile
        Box(
            modifier         = Modifier
                .size(76.dp)
                .clip(CircleShape)
                .background(iconBg),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector        = icon,
                contentDescription = null,
                tint               = iconTint,
                modifier           = Modifier.size(38.dp),
            )
        }
    }
}

/** Convenience overload that takes a custom AnnotatedString body. */
@Composable
fun DsConfirmDialog(
    visible      : Boolean,
    title        : String,
    body         : AnnotatedString,
    confirmLabel : String,
    onConfirm    : () -> Unit,
    onDismiss    : () -> Unit,
    cancelLabel  : String = "Cancel",
    kind         : DsConfirmKind = DsConfirmKind.Destructive,
    icon         : ImageVector = Icons.Filled.Block,
) {
    if (!visible) return
    Dialog(
        onDismissRequest = onDismiss,
        properties       = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier         = Modifier
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.55f))
                .clickable(onClick = onDismiss)
                .padding(horizontal = 24.dp),
            contentAlignment = Alignment.Center,
        ) {
            val (iconTint, iconBg) = when (kind) {
                DsConfirmKind.Destructive -> AppTheme.Danger to AppTheme.DangerBg
                DsConfirmKind.Info        -> AppTheme.Brand  to AppTheme.Brand50
                DsConfirmKind.Success     -> AppTheme.Success to AppTheme.SuccessBg
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(20.dp, RoundedCornerShape(24.dp), spotColor = Color.Black.copy(alpha = 0.30f))
                    .clip(RoundedCornerShape(24.dp))
                    .background(AppTheme.Surface)
                    .clickable(enabled = false, onClick = {})
                    .padding(horizontal = 24.dp, vertical = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                IconHaloWithRotatingRing(icon = icon, iconTint = iconTint, iconBg = iconBg)
                Spacer(Modifier.height(20.dp))
                Text(
                    title,
                    style = AppTypography.titleLarge.copy(
                        color      = AppTheme.Ink900,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize   = 22.sp,
                    ),
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    text      = body,
                    style     = AppTypography.bodyMedium.copy(color = AppTheme.Ink500),
                    textAlign = TextAlign.Center,
                    modifier  = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(24.dp))
                val primaryGradient = when (kind) {
                    DsConfirmKind.Destructive -> listOf(Color(0xFFF87171), Color(0xFFDC2626))
                    DsConfirmKind.Info        -> listOf(AppTheme.Brand, AppTheme.Brand700)
                    DsConfirmKind.Success     -> listOf(Color(0xFF34D399), Color(0xFF059669))
                }
                Box(
                    modifier         = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .shadow(10.dp, RoundedCornerShape(14.dp), spotColor = primaryGradient.last().copy(alpha = 0.55f))
                        .clip(RoundedCornerShape(14.dp))
                        .background(Brush.horizontalGradient(primaryGradient))
                        .clickable(onClick = onConfirm),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        confirmLabel,
                        style = AppTypography.titleSmall.copy(color = Color.White, fontWeight = FontWeight.Bold),
                    )
                }
                Spacer(Modifier.height(10.dp))
                Box(
                    modifier         = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(AppTheme.SurfaceMuted)
                        .border(1.dp, AppTheme.Ink100, RoundedCornerShape(14.dp))
                        .clickable(onClick = onDismiss),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        cancelLabel,
                        style = AppTypography.titleSmall.copy(color = AppTheme.Ink900, fontWeight = FontWeight.Bold),
                    )
                }
            }
        }
    }
}
