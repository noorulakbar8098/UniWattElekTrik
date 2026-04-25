package com.example.uniwattelektrik.feature.auth.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/* --- Local palette so this component is self-contained --- */
private val UncheckedBg     = Color(0xFFF3F4F6)
private val UncheckedBorder = Color(0xFFD1D5DB)
private val LabelColor      = Color(0xFF374151)
private val GlowSky         = Color(0xFF4FACFE)
private val GlowDeep        = Color(0xFF0072FF)

/**
 * Premium animated checkbox.
 *
 *  - 22dp rounded square (10dp radius)
 *  - Unchecked: light grey fill + subtle border
 *  - Checked: sky→deep-blue gradient, white check, soft glow, elevation
 *  - Scale punch (0.95 → 1.0) and fade-in checkmark on toggle
 */
@Composable
fun PremiumCheckbox(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val shape = RoundedCornerShape(10.dp)

    val scale by animateFloatAsState(
        targetValue = if (checked) 1f else 0.95f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow,
        ),
        label = "checkbox-scale",
    )


    Box(
        modifier = modifier
            .size(22.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(shape)
            .then(
                if (checked) Modifier.background(
                    Brush.linearGradient(listOf(GlowSky, GlowDeep)),
                )
                else Modifier
                    .background(UncheckedBg)
                    .border(BorderStroke(1.5.dp, UncheckedBorder), shape),
            )
            .clickable(enabled = enabled) { onCheckedChange(!checked) },
        contentAlignment = Alignment.Center,
    ) {
        // Subtle white inner-glow gradient when checked, behind the tick.
        if (checked) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.White.copy(alpha = 0.18f), Color.Transparent),
                        ),
                    ),
            )
        }

        AnimatedVisibility(
            visible = checked,
            enter = fadeIn(tween(180)) + scaleIn(initialScale = 0.6f, animationSpec = tween(180)),
            exit = fadeOut(tween(120)) + scaleOut(targetScale = 0.6f, animationSpec = tween(120)),
        ) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

/**
 * Drop-in row that pairs [PremiumCheckbox] with the **"Keep me signed in"** label,
 * already styled per spec (Inter/SF Pro fallback, #374151, medium weight, 14dp gap).
 *
 * Tap target spans both the box and the label.
 */
@Composable
fun KeepMeSignedInCheckbox(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    label: String = "Keep me signed in",
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(enabled = enabled) { onCheckedChange(!checked) }
            .padding(vertical = 4.dp, horizontal = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PremiumCheckbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled,
        )
        Spacer(Modifier.width(14.dp))
        Text(
            label,
            color = LabelColor,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 0.1.sp,
        )
    }
}

