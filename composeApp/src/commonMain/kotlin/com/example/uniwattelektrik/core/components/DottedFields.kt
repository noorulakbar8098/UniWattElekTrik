package com.example.uniwattelektrik.core.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.uniwattelektrik.core.theme.AppTheme
import com.example.uniwattelektrik.core.theme.AppShapes
// ─── Design tokens — backed by enterprise system ────────────────────────────
private val ScreenBg     = AppTheme.Bg
private val CardBg       = AppTheme.Surface
private val InkPrimary   = AppTheme.Ink900
private val InkSecondary = AppTheme.Ink500
private val InkMuted     = AppTheme.Ink300
private val Brand        = AppTheme.Brand
private val BrandDeep    = AppTheme.Brand700
private val Brand50      = AppTheme.Brand50
private val Success      = AppTheme.Success
private val SuccessBg    = AppTheme.SuccessBg
private val Warning      = AppTheme.Warning
private val WarningBg    = AppTheme.WarningBg
private val Danger       = AppTheme.Danger
private val DangerBg     = AppTheme.DangerBg
private val DividerSoft  = AppTheme.Ink100
private val ShadowSoft   = AppTheme.ShadowMd


/* ── Shared design tokens for dotted-style inputs ─────────────────────── */
private val InputBg   = AppTheme.SurfaceMuted
private val DotBorder = AppTheme.Ink100
private val ErrorRed  = AppTheme.Danger
private val FocusBlue = AppTheme.Brand
private val Divider   = AppTheme.Ink100

/**
 * Thin rounded dashed border — matches the Task-creation field style.
 * Used by every [DottedField] / [DottedClickable] / [DottedSelect].
 */
fun Modifier.dottedBorder(
    color: Color = DotBorder,
    cornerRadius: Dp = 12.dp,
    strokeWidth: Dp = 1.5.dp,
): Modifier = drawBehind {
    drawRoundRect(
        color = color,
        cornerRadius = CornerRadius(cornerRadius.toPx(), cornerRadius.toPx()),
        style = Stroke(
            width = strokeWidth.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f),
        ),
    )
}

/**
 * Field caption shown above an input ("Full name", "Email", …).
 * If [required] is true a red asterisk follows the text.
 */
@Composable
fun FieldLabel(
    text: String,
    required: Boolean = false,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier) {
        Text(
            text,
            color = InkPrimary,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
        )
        if (required) {
            Text(
                " *",
                color = ErrorRed,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
    Spacer(Modifier.height(6.dp))
}

/**
 * Editable text field with a leading colored icon tile and a dashed border.
 *
 * Mirrors the input style used across the Task-creation screen.
 */
@Composable
fun DottedField(
    value: String,
    onChange: (String) -> Unit,
    placeholder: String = "",
    leadingIcon: ImageVector? = null,
    leadingTint: Color = FocusBlue,
    leadingBg: Color = Color(0xFFE6F0FE),
    trailing: String? = null,
    keyboard: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Default,
    enabled: Boolean = true,
    minHeight: Dp = 48.dp,
    isError: Boolean = false,
    errorText: String? = null,
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(InputBg)
                .dottedBorder(color = if (isError) ErrorRed else DotBorder)
                .padding(horizontal = 12.dp, vertical = 10.dp)
                .heightIn(min = minHeight),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (leadingIcon != null) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(leadingBg),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = leadingIcon,
                        contentDescription = null,
                        tint = leadingTint,
                        modifier = Modifier.size(18.dp),
                    )
                }
                Spacer(Modifier.width(10.dp))
            }
            Box(modifier = Modifier.weight(1f)) {
                if (value.isEmpty()) {
                    Text(
                        placeholder,
                        color = InkMuted,
                        fontSize = 13.sp,
                    )
                }
                BasicTextField(
                    value = value,
                    onValueChange = onChange,
                    enabled = enabled,
                    textStyle = TextStyle(
                        color = InkPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                    ),
                    cursorBrush = SolidColor(FocusBlue),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = keyboard,
                        imeAction = imeAction,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            if (trailing != null) {
                Spacer(Modifier.width(8.dp))
                Text(
                    trailing,
                    color = InkMuted,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
        if (errorText != null) {
            Spacer(Modifier.height(4.dp))
            Text(
                errorText,
                color = ErrorRed,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(start = 4.dp),
            )
        }
    }
}

/**
 * Read-only chip styled exactly like [DottedField] but tap-able instead
 * of editable. Use for date / time / picker rows.
 */
@Composable
fun DottedClickable(
    value: String,
    leadingIcon: ImageVector,
    leadingTint: Color,
    leadingBg: Color,
    muted: Boolean = false,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(InputBg)
            .dottedBorder()
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(leadingBg),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = leadingIcon,
                contentDescription = null,
                tint = leadingTint,
                modifier = Modifier.size(18.dp),
            )
        }
        Spacer(Modifier.width(10.dp))
        Text(
            value,
            color = if (muted) InkMuted else InkPrimary,
            fontSize = 13.sp,
            fontWeight = if (muted) FontWeight.Medium else FontWeight.SemiBold,
        )
    }
}

/**
 * Static read-only row with a small "auto" hint on the right — matches
 * the Task-creation Task-ID field.
 */
@Composable
fun DottedReadOnly(
    value: String,
    leadingIcon: ImageVector,
    leadingTint: Color,
    leadingBg: Color,
    hint: String = "auto",
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(InputBg)
            .dottedBorder()
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(leadingBg),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = leadingIcon,
                contentDescription = null,
                tint = leadingTint,
                modifier = Modifier.size(18.dp),
            )
        }
        Spacer(Modifier.width(10.dp))
        Text(
            value,
            color = InkSecondary,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f),
        )
        Text(
            hint,
            color = InkMuted,
            fontSize = 11.sp,
        )
    }
}

/**
 * Inline expanding select — shows the current value with a chevron, opens
 * a list of [options] when tapped. Same dashed-border styling as the rest.
 */
@Composable
fun DottedSelect(
    value: String,
    onChange: (String) -> Unit,
    options: List<String>,
    leadingIcon: ImageVector,
    leadingTint: Color = FocusBlue,
    leadingBg: Color = Color(0xFFE6F0FE),
    enabled: Boolean = true,
) {
    var open by remember { mutableStateOf(false) }
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(InputBg)
                .dottedBorder()
                .clickable(enabled = enabled) { open = !open }
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(leadingBg),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    tint = leadingTint,
                    modifier = Modifier.size(18.dp),
                )
            }
            Spacer(Modifier.width(10.dp))
            Text(
                value,
                color = InkPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
            )
            Icon(
                imageVector = Icons.Filled.ExpandMore,
                contentDescription = null,
                tint = InkMuted,
                modifier = Modifier.size(20.dp),
            )
        }
        if (open) {
            Spacer(Modifier.height(4.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(CardBg)
                    .border(1.dp, Divider, RoundedCornerShape(12.dp)),
            ) {
                options.forEach { opt ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onChange(opt)
                                open = false
                            }
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            opt,
                            color = InkPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.weight(1f),
                        )
                        if (value == opt) {
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = null,
                                tint = FocusBlue,
                                modifier = Modifier.size(16.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

