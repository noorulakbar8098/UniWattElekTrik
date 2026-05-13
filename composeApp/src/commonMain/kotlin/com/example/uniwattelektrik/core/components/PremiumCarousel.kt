package com.example.uniwattelektrik.core.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.ElectricalServices
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.uniwattelektrik.core.theme.AppShapes
import com.example.uniwattelektrik.core.theme.AppTheme
import com.example.uniwattelektrik.core.theme.AppTypography
import kotlin.math.absoluteValue
import kotlinx.coroutines.delay

/* ═══════════════════════════════════════════════════════════════════════════
 *  PremiumCarousel — auto-sliding banner with parallax + glass overlay
 * ═══════════════════════════════════════════════════════════════════════════
 *
 *  Design notes
 *  ────────────
 *  • Pure Compose — no image assets needed. Each slide is built from
 *    layered gradients, a Canvas-painted grid + glow, and the design system's
 *    icon set, so the look stays consistent on every screen size and density.
 *  • Infinite loop: HorizontalPager is given Int.MAX_VALUE pages and the
 *    real slide is `page % slides.size`. Auto-scroll just animates to the
 *    next index every 3 s; the user can still drag in either direction.
 *  • Parallax + scale on neighbouring pages so the centred card always
 *    "pops" forward — a luxury-app feel close to fintech onboarding.
 */

private data class CarouselSlide(
    val title       : String,
    val subtitle    : String,
    val ctaLabel    : String,
    val icon        : ImageVector,
    val gradient    : List<Color>,   // 3-stop diagonal gradient
    val accentColor : Color,         // glow + CTA highlight
)

private val slides = listOf(
    CarouselSlide(
        title       = "Precision Control Panels",
        subtitle    = "Designed for safety, efficiency & industrial performance.",
        ctaLabel    = "Explore Services",
        icon        = Icons.Filled.Bolt,
        gradient    = listOf(Color(0xFF0D1B3E), Color(0xFF14306B), Color(0xFF1A6BF5)),
        accentColor = Color(0xFFF59E0B), // amber lightning
    ),
    CarouselSlide(
        title       = "Smart Industrial Automation",
        subtitle    = "Advanced automation systems engineered for reliability.",
        ctaLabel    = "View Solutions",
        icon        = Icons.Filled.AutoFixHigh,
        gradient    = listOf(Color(0xFF051A4A), Color(0xFF1A6BF5), Color(0xFF38BDF8)),
        accentColor = Color(0xFF38BDF8), // electric cyan
    ),
    CarouselSlide(
        title       = "Powering Reliable Infrastructure",
        subtitle    = "Built with precision. Wired for performance.",
        ctaLabel    = "Learn More",
        icon        = Icons.Filled.ElectricalServices,
        gradient    = listOf(Color(0xFF0B1F4D), Color(0xFF1458CC), Color(0xFF60A5FA)),
        accentColor = Color(0xFF60A5FA), // soft electric blue
    ),
)

@Composable
fun PremiumCarousel(
    modifier      : Modifier = Modifier,
    onSlideAction : (slideIndex: Int) -> Unit = {},
    autoScrollMs  : Long     = 3_000,
) {
    val virtualPageCount = Int.MAX_VALUE
    val startPage = remember { virtualPageCount / 2 - (virtualPageCount / 2) % slides.size }
    val pagerState = rememberPagerState(initialPage = startPage) { virtualPageCount }

    // Auto-scroll. Pause while the user is interacting (settled-but-animating
    // pages count as still-interacting via isScrollInProgress).
    LaunchedEffect(pagerState) {
        while (true) {
            delay(autoScrollMs)
            if (!pagerState.isScrollInProgress) {
                pagerState.animateScrollToPage(
                    page           = pagerState.currentPage + 1,
                    animationSpec  = tween(durationMillis = 700),
                )
            }
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        HorizontalPager(
            state       = pagerState,
            pageSpacing = 12.dp,
            modifier    = Modifier
                .fillMaxWidth()
                .height(208.dp),
        ) { page ->
            val slide = slides[page % slides.size]

            // Parallax + scale based on this page's distance from current.
            val pageOffset = (
                (pagerState.currentPage - page) +
                    pagerState.currentPageOffsetFraction
            ).let { -it }  // current = 0, prev = -1, next = +1

            val absOffset = pageOffset.absoluteValue.coerceAtMost(1f)

            CarouselCard(
                slide    = slide,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        // Subtle scale-down on neighbours: focused = 1.0, edge = 0.92
                        val s = 1f - 0.08f * absOffset
                        scaleX = s
                        scaleY = s
                        alpha  = 0.55f + 0.45f * (1f - absOffset)
                        // Translate slightly for parallax
                        translationX = pageOffset * size.width * 0.10f
                    },
                onCta = { onSlideAction(page % slides.size) },
            )
        }

        Spacer(Modifier.height(14.dp))

        PageIndicator(
            pageCount    = slides.size,
            selectedIndex = pagerState.currentPage % slides.size,
            modifier     = Modifier
                .align(Alignment.CenterHorizontally),
        )
    }
}

/* ─── Single card ─────────────────────────────────────────────────────── */

@Composable
private fun CarouselCard(
    slide   : CarouselSlide,
    onCta   : () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .shadow(
                elevation    = 18.dp,
                shape        = RoundedCornerShape(24.dp),
                spotColor    = slide.gradient.last().copy(alpha = 0.40f),
                ambientColor = Color.Transparent,
            )
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.linearGradient(
                    colors = slide.gradient,
                    start  = Offset(0f, 0f),
                    end    = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY),
                )
            ),
    ) {
        // 1. Decorative grid + radial glow painted onto the gradient.
        Canvas(modifier = Modifier.fillMaxSize()) {
            // Vertical + horizontal grid lines (subtle)
            val step = 28f
            val gridColor = Color.White.copy(alpha = 0.06f)
            var x = 0f
            while (x < size.width) {
                drawLine(
                    color  = gridColor,
                    start  = Offset(x, 0f),
                    end    = Offset(x, size.height),
                    strokeWidth = 1f,
                )
                x += step
            }
            var y = 0f
            while (y < size.height) {
                drawLine(
                    color  = gridColor,
                    start  = Offset(0f, y),
                    end    = Offset(size.width, y),
                    strokeWidth = 1f,
                )
                y += step
            }
            // Diagonal "energy" streak — soft accent gradient line
            val streakBrush = Brush.linearGradient(
                colors = listOf(
                    Color.Transparent,
                    slide.accentColor.copy(alpha = 0.55f),
                    Color.Transparent,
                ),
                start = Offset(size.width * 0.15f, size.height * 0.95f),
                end   = Offset(size.width * 0.95f, size.height * 0.10f),
            )
            drawLine(
                brush       = streakBrush,
                start       = Offset(size.width * 0.15f, size.height * 0.95f),
                end         = Offset(size.width * 0.95f, size.height * 0.10f),
                strokeWidth = 2.5f,
            )
            // Top-right radial glow halo around the icon area.
            drawCircle(
                brush  = Brush.radialGradient(
                    colors = listOf(
                        slide.accentColor.copy(alpha = 0.45f),
                        Color.Transparent,
                    ),
                    center = Offset(size.width * 0.84f, size.height * 0.30f),
                    radius = size.minDimension * 0.55f,
                ),
                radius = size.minDimension * 0.55f,
                center = Offset(size.width * 0.84f, size.height * 0.30f),
            )
        }

        // 2. Glass overlay — soft top-left highlight that reads as glassmorphism.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.10f),
                            Color.Transparent,
                        ),
                        start = Offset(0f, 0f),
                        end   = Offset(0f, Float.POSITIVE_INFINITY),
                    )
                )
                .border(1.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(24.dp)),
        )

        // 3. Floating glow icon (top-right) — the "lightning" focal point.
        Box(
            modifier         = Modifier
                .align(Alignment.TopEnd)
                .padding(end = 22.dp, top = 22.dp)
                .size(56.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            slide.accentColor.copy(alpha = 0.35f),
                            slide.accentColor.copy(alpha = 0.05f),
                        )
                    )
                )
                .border(1.dp, Color.White.copy(alpha = 0.25f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector        = slide.icon,
                contentDescription = null,
                tint               = Color.White,
                modifier           = Modifier.size(28.dp),
            )
        }

        // 4. Foreground content.
        Column(
            modifier            = Modifier
                .fillMaxSize()
                .padding(horizontal = 22.dp, vertical = 22.dp),
            verticalArrangement = Arrangement.Bottom,
        ) {
            // Eyebrow chip — premium "UNIWATT" tag
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier          = Modifier
                    .clip(AppShapes.pill)
                    .background(Color.White.copy(alpha = 0.14f))
                    .border(1.dp, Color.White.copy(alpha = 0.18f), AppShapes.pill)
                    .padding(horizontal = 10.dp, vertical = 4.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(slide.accentColor),
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text  = "UNIWATT • ELEKTRIK",
                    style = AppTypography.labelSmall.copy(
                        color         = Color.White.copy(alpha = 0.85f),
                        fontWeight    = FontWeight.SemiBold,
                        letterSpacing = 1.2.sp,
                    ),
                )
            }

            Spacer(Modifier.height(10.dp))

            Text(
                text     = slide.title,
                style    = AppTypography.titleLarge.copy(
                    color      = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize   = 20.sp,
                ),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )

            Spacer(Modifier.height(4.dp))

            Text(
                text     = slide.subtitle,
                style    = AppTypography.bodySmall.copy(
                    color = Color.White.copy(alpha = 0.78f),
                ),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )

            Spacer(Modifier.height(12.dp))

            // CTA — gradient pill button with arrow
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier          = Modifier
                    .clip(AppShapes.pill)
                    .background(
                        Brush.horizontalGradient(
                            listOf(Color.White, Color(0xFFEAF2FF)),
                        )
                    )
                    .clickable(onClick = onCta)
                    .padding(horizontal = 16.dp, vertical = 9.dp),
            ) {
                Text(
                    text  = slide.ctaLabel,
                    style = AppTypography.labelLarge.copy(
                        color      = AppTheme.Ink900,
                        fontWeight = FontWeight.Bold,
                    ),
                )
                Spacer(Modifier.width(6.dp))
                Icon(
                    imageVector        = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint               = AppTheme.Brand,
                    modifier           = Modifier.size(16.dp),
                )
            }
        }
    }
}

/* ─── Page indicator (active = elongated glowing pill) ─────────────────── */

@Composable
private fun PageIndicator(
    pageCount    : Int,
    selectedIndex: Int,
    modifier     : Modifier = Modifier,
) {
    Row(
        modifier              = modifier,
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        repeat(pageCount) { i ->
            val isActive = i == selectedIndex
            val width by animateDpAsState(
                targetValue = if (isActive) 22.dp else 7.dp,
                animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = 380f),
                label         = "indicatorWidth",
            )
            val alpha by animateFloatAsState(
                targetValue   = if (isActive) 1f else 0.32f,
                animationSpec = tween(220),
                label         = "indicatorAlpha",
            )
            Box(
                modifier = Modifier
                    .size(width = width, height = 7.dp)
                    .clip(CircleShape)
                    .then(
                        if (isActive)
                            Modifier
                                .shadow(8.dp, CircleShape, spotColor = AppTheme.Brand)
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(AppTheme.Brand, AppTheme.Brand700)
                                    )
                                )
                        else
                            Modifier.background(AppTheme.Ink300.copy(alpha = alpha))
                    ),
            )
        }
    }
}
