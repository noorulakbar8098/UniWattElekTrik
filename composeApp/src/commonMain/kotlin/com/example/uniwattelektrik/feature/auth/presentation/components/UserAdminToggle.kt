package com.example.uniwattelektrik.feature.auth.presentation.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Two roles the auth UI can present. */
enum class AuthRole { User, Admin }

private val ContainerBg  = Color(0xFFECEFF5)
private val ActiveBg     = Color(0xFFFFFFFF)
private val ActiveText   = Color(0xFF0A1F44)
private val InactiveText = Color(0xFF9AA3B5)

/**
 * Figma-style segmented control — rectangular card shape, NOT a pill.
 *
 * Container: soft gray rounded rect (14dp).
 * Active tab: white elevated inset card (10dp) that slides with a spring.
 * Labels: "Employee" / "Admin".
 */
@Composable
fun UserAdminToggle(
    selected: AuthRole,
    onSelected: (AuthRole) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val containerShape = RoundedCornerShape(14.dp)
    val tabShape       = RoundedCornerShape(10.dp)

    BoxWithConstraints(
        modifier = modifier
            .height(46.dp)
            .shadow(
                elevation    = 4.dp,
                shape        = containerShape,
                spotColor    = Color(0x180A1F44),
                ambientColor = Color(0x0C0A1F44),
                clip         = false,
            )
            .clip(containerShape)
            .background(ContainerBg)
            .padding(4.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        val halfWidth = maxWidth / 2

        val offsetX by animateDpAsState(
            targetValue  = if (selected == AuthRole.User) 0.dp else halfWidth,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness    = Spring.StiffnessMediumLow,
            ),
            label = "toggle-slide",
        )

        // Sliding white tab card
        Box(
            modifier = Modifier
                .offset(x = offsetX)
                .width(halfWidth)
                .fillMaxHeight()
                .shadow(
                    elevation    = 6.dp,
                    shape        = tabShape,
                    spotColor    = Color(0x220A1F44),
                    ambientColor = Color(0x120A1F44),
                    clip         = false,
                )
                .clip(tabShape)
                .background(ActiveBg),
        )

        // Labels (drawn on top of slider)
        Row(Modifier.fillMaxSize()) {
            SegmentLabel(
                label    = "Employee",
                selected = selected == AuthRole.User,
                onClick  = { if (enabled) onSelected(AuthRole.User) },
                modifier = Modifier.weight(1f),
            )
            SegmentLabel(
                label    = "Admin",
                selected = selected == AuthRole.Admin,
                onClick  = { if (enabled) onSelected(AuthRole.Admin) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun SegmentLabel(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scale by animateFloatAsState(
        targetValue   = if (selected) 1.02f else 1f,
        animationSpec = tween(160, easing = FastOutSlowInEasing),
        label         = "label-scale",
    )
    Box(
        modifier = modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text          = label,
            color         = if (selected) ActiveText else InactiveText,
            fontSize      = 14.sp,
            fontWeight    = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            letterSpacing = 0.1.sp,
            modifier      = Modifier.graphicsLayer { scaleX = scale; scaleY = scale },
        )
    }
}

/**
 * Drop-in alias used by login screens.
 * Width fills available space — screens should constrain it via their own modifier.
 */
@Composable
fun UserAdminToggleBordered(
    selected: AuthRole,
    onSelected: (AuthRole) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) = UserAdminToggle(
    selected   = selected,
    onSelected = onSelected,
    modifier   = modifier,
    enabled    = enabled,
)
