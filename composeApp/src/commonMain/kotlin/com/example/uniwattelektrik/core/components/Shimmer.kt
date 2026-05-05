package com.example.uniwattelektrik.core.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class SkeletonType {
    Auth,
    Dashboard,
    List,
    Form,
}

/**
 * Subtle global shimmer sweep that can be layered on top of any screen.
 * Keep alpha low so content remains readable.
 */
@Composable
fun Modifier.shimmerOverlay(enabled: Boolean = true): Modifier {
    if (!enabled) return this

    val transition = rememberInfiniteTransition(label = "globalShimmer")
    val progress = transition.animateFloat(
        initialValue = -1f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "globalShimmerProgress",
    )

    return drawWithContent {
        drawContent()

        val width = size.width
        val startX = (progress.value - 1f) * width
        val endX = progress.value * width

        drawRect(
            brush = Brush.linearGradient(
                colors = listOf(
                    Color.Transparent,
                    Color.White.copy(alpha = 0.06f),
                    Color.Transparent,
                ),
                start = Offset(startX, 0f),
                end = Offset(endX, size.height),
            ),
        )
    }
}

@Composable
fun ScreenSkeletonOverlay(
    type: SkeletonType = SkeletonType.List,
    message: String = "Loading...",
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White.copy(alpha = 0.65f)),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            when (type) {
                SkeletonType.Auth -> AuthSkeleton()
                SkeletonType.Dashboard -> DashboardSkeleton()
                SkeletonType.List -> ListSkeleton()
                SkeletonType.Form -> FormSkeleton()
            }
        }

        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            CircularProgressIndicator(color = Color(0xFF2979FF), strokeWidth = 2.5.dp)
            Text(
                text = message,
                color = Color(0xFF334155),
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun AuthSkeleton() {
    SkeletonBlock(modifier = Modifier.fillMaxWidth(0.45f).height(18.dp))
    SkeletonBlock(modifier = Modifier.fillMaxWidth(0.7f).height(14.dp))
    SkeletonBlock(modifier = Modifier.fillMaxWidth().height(56.dp))
    SkeletonBlock(modifier = Modifier.fillMaxWidth().height(56.dp))
    SkeletonBlock(modifier = Modifier.fillMaxWidth().height(52.dp))
}

@Composable
private fun DashboardSkeleton() {
    SkeletonBlock(modifier = Modifier.fillMaxWidth(0.55f).height(22.dp))
    SkeletonBlock(modifier = Modifier.fillMaxWidth().height(110.dp))
    Box(modifier = Modifier.fillMaxWidth()) {
        SkeletonBlock(modifier = Modifier.fillMaxWidth(0.48f).height(84.dp))
        SkeletonBlock(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .fillMaxWidth(0.48f)
                .height(84.dp),
        )
    }
    SkeletonBlock(modifier = Modifier.fillMaxWidth().height(88.dp))
}

@Composable
private fun ListSkeleton() {
    SkeletonBlock(modifier = Modifier.fillMaxWidth(0.35f).height(18.dp))
    SkeletonBlock(modifier = Modifier.fillMaxWidth().height(56.dp))
    SkeletonBlock(modifier = Modifier.fillMaxWidth().height(72.dp))
    SkeletonBlock(modifier = Modifier.fillMaxWidth().height(72.dp))
    SkeletonBlock(modifier = Modifier.fillMaxWidth().height(72.dp))
}

@Composable
private fun FormSkeleton() {
    SkeletonBlock(modifier = Modifier.fillMaxWidth(0.4f).height(18.dp))
    SkeletonBlock(modifier = Modifier.fillMaxWidth().height(52.dp))
    SkeletonBlock(modifier = Modifier.fillMaxWidth().height(52.dp))
    Box(modifier = Modifier.fillMaxWidth()) {
        SkeletonBlock(modifier = Modifier.fillMaxWidth(0.48f).height(52.dp))
        SkeletonBlock(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .fillMaxWidth(0.48f)
                .height(52.dp),
        )
    }
    SkeletonBlock(modifier = Modifier.fillMaxWidth().height(52.dp))
    SkeletonBlock(modifier = Modifier.fillMaxWidth().height(52.dp))
}

@Composable
private fun SkeletonBlock(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFFE2E8F0))
            .shimmerOverlay(),
    )
}

