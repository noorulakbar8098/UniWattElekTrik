package com.example.uniwattelektrik.core.components

/**
 * DesignSystem.kt — Centralized reusable component library.
 *
 * Every repeated UI pattern in the app lives here as a named composable.
 * Import and use these instead of copy-pasting layout code across screens.
 *
 * Contents (in order):
 *   1.  DsCard             — standard card surface
 *   2.  DsAccentCard       — card with left gradient priority strip
 *   3.  DsSectionHeader    — section title with brand accent stripe
 *   4.  DsStatusChip       — colored status / state badge
 *   5.  DsPriorityChip     — priority indicator with dot + label
 *   6.  DsCodeChip         — item ID chip (e.g. "#TASK-1234")
 *   7.  DsProgressBar      — animated gradient horizontal progress bar
 *   8.  DsAvatarBubble     — gradient avatar circle with shadow
 *   9.  DsFilterChip       — selectable filter pill
 *   10. DsGlassSearchBar   — glassmorphic search text input
 *   11. DsKpiStat          — KPI / metric value + label pair
 *   12. DsKpiCard          — complete dashboard metric card with icon
 *   13. DsActionButton     — full-width gradient CTA button
 *   14. DsGlassButton      — glass-style icon button for gradient headers
 *   15. DsVerticalDivider  — thin vertical divider for stat rows
 *   16. DsTaskCard         — universal premium task card (list + kanban)
 *   17. DsEmptyState       — illustrated empty state (filter / no-match)
 *   17b.DsAllCaughtUpEmptyState — premium card empty state (no items at all)
 *   18. DsLoadingCard      — shimmer skeleton for a task card
 *
 * Design token usage:
 *   Colors    → AppTheme.*
 *   Shapes    → AppShapes.*
 *   Elevation → AppElevation.*
 *   Spacing   → AppTheme.Sp* / AppTheme.SpXs–SpXxl
 *   Typography→ AppTypography.*
 */

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.uniwattelektrik.core.theme.AppElevation
import com.example.uniwattelektrik.core.theme.AppShapes
import com.example.uniwattelektrik.core.theme.AppTheme
import com.example.uniwattelektrik.core.theme.AppTypography

/* ═══════════════════════════════════════════════════════════════════════════
 *  1. DsCard — Standard card surface
 * ═══════════════════════════════════════════════════════════════════════════ */

/**
 * Standard white card with consistent elevation, shape, and shadow.
 * Replaces every `AppCard` call to ensure visual uniformity.
 *
 * @param pressScale when true, a spring scale-down plays on press.
 */
@Composable
fun DsCard(
    modifier      : Modifier = Modifier,
    shape         : RoundedCornerShape = AppShapes.card,
    elevation     : Dp = AppElevation.card,
    background    : Color = AppTheme.Surface,
    contentPadding: Dp = AppTheme.SpLg,
    pressScale    : Boolean = false,
    onClick       : (() -> Unit)? = null,
    content       : @Composable () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue   = if (pressScale && isPressed) 0.97f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label         = "dsCardScale",
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .scale(scale)
            .shadow(elevation, shape, spotColor = AppTheme.ShadowMd, ambientColor = Color.Transparent)
            .clip(shape)
            .background(background)
            .then(
                if (onClick != null) Modifier.clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
                else Modifier,
            )
            .padding(contentPadding),
    ) { content() }
}

/* ═══════════════════════════════════════════════════════════════════════════
 *  2. DsAccentCard — Card with left gradient priority strip
 * ═══════════════════════════════════════════════════════════════════════════ */

/**
 * Card surface with a 4 dp left gradient accent strip for priority/status
 * signalling. Used by task cards, activity rows, and alert panels.
 */
@Composable
fun DsAccentCard(
    accentColor  : Color,
    modifier     : Modifier = Modifier,
    shape        : RoundedCornerShape = AppShapes.card,
    elevation    : Dp = AppElevation.accentCard,
    pressScale   : Boolean = true,
    onClick      : (() -> Unit)? = null,
    content      : @Composable () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue   = if (pressScale && isPressed) 0.97f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label         = "dsAccentCardScale",
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .scale(scale)
            .shadow(elevation, shape, spotColor = accentColor.copy(alpha = 0.14f), ambientColor = Color.Transparent)
            .clip(shape)
            .background(AppTheme.Surface)
            .then(
                if (onClick != null) Modifier.clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
                else Modifier,
            ),
    ) {
        // Diagonal accent wash — subtle depth in top-right corner
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.linearGradient(
                        colors = listOf(accentColor.copy(alpha = 0.04f), Color.Transparent),
                        start  = Offset(Float.POSITIVE_INFINITY, 0f),
                        end    = Offset(0f, Float.POSITIVE_INFINITY),
                    ),
                ),
        )

        Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
            // Left gradient strip
            Box(
                modifier = Modifier
                    .width(AppTheme.AccentStripWidth)
                    .fillMaxHeight()
                    .background(
                        Brush.verticalGradient(listOf(accentColor, accentColor.copy(alpha = 0.50f))),
                    ),
            )
            // Card body
            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = AppTheme.SpMd, end = AppTheme.SpLg, top = AppTheme.SpMd, bottom = AppTheme.SpMd),
            ) { content() }
        }
    }
}

/* ═══════════════════════════════════════════════════════════════════════════
 *  3. DsSectionHeader — Section title with brand accent stripe
 * ═══════════════════════════════════════════════════════════════════════════ */

/**
 * Left-aligned section header with a 3 dp brand-blue accent bar.
 * Used in detail screens and dashboards to open named content sections.
 *
 * @param trailing optional right-aligned action text (e.g. "3 / 5", "View all").
 */
@Composable
fun DsSectionHeader(
    title    : String,
    modifier : Modifier = Modifier,
    trailing : String? = null,
    onTrailing: () -> Unit = {},
) {
    Row(
        modifier          = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .height(AppTheme.SpLg)
                .width(3.dp)
                .clip(AppShapes.pill)
                .background(AppTheme.Brand),
        )
        Spacer(Modifier.width(AppTheme.SpSm))
        Text(
            text     = title,
            style    = AppTypography.titleLarge.copy(color = AppTheme.Ink900),
            modifier = Modifier.weight(1f),
        )
        if (trailing != null) {
            Text(
                text      = trailing,
                style     = AppTypography.labelMedium.copy(color = AppTheme.Brand),
                modifier  = Modifier.clickable(onClick = onTrailing),
            )
        }
    }
}

/* ═══════════════════════════════════════════════════════════════════════════
 *  4. DsStatusChip — Colored status / state badge
 * ═══════════════════════════════════════════════════════════════════════════ */

/**
 * Simple rounded badge displaying a short status string in a tinted pill.
 * Pass `tint` + `background` from AppTheme semantic colors.
 */
@Composable
fun DsStatusChip(
    label         : String,
    tint          : Color,
    background    : Color,
    modifier      : Modifier = Modifier,
    /**
     * When non-null, renders a 6 dp filled dot before the label using this
     * colour. Use for "status with severity" pills (e.g. "5 / 22 days" with a
     * green health dot, or "Live · 12" badges).
     */
    leadingDotColor: Color? = null,
) {
    val shape = if (leadingDotColor != null) AppShapes.pill else AppShapes.small
    // Subtle top-light gradient: slightly brighter at the top, fading into
    // the supplied background. Reads as a soft glass finish without changing
    // the perceived tint at a glance.
    val surfaceBrush = Brush.verticalGradient(
        colors = listOf(
            Color.White.copy(alpha = 0.30f),
            background,
        ),
    )
    Row(
        modifier              = modifier
            .shadow(
                elevation    = 1.dp,
                shape        = shape,
                ambientColor = tint.copy(alpha = 0.12f),
                spotColor    = tint.copy(alpha = 0.18f),
            )
            .clip(shape)
            .background(background)
            .background(surfaceBrush)
            .border(0.5.dp, tint.copy(alpha = 0.20f), shape)
            .padding(horizontal = AppTheme.SpSm, vertical = AppTheme.SpXs),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (leadingDotColor != null) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(leadingDotColor),
            )
        }
        Text(
            text  = label,
            style = AppTypography.labelSmall.copy(color = tint),
        )
    }
}

/* ═══════════════════════════════════════════════════════════════════════════
 *  5. DsPriorityChip — Priority indicator with dot
 * ═══════════════════════════════════════════════════════════════════════════ */

/**
 * Priority chip with a 5 dp filled dot followed by an allcaps label.
 * Used in task cards and kanban columns.
 */
@Composable
fun DsPriorityChip(
    label   : String,
    tint    : Color,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier          = modifier
            .clip(AppShapes.pill)
            .background(tint.copy(alpha = 0.12f))
            .padding(horizontal = AppTheme.SpSm, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(5.dp)
                .clip(CircleShape)
                .background(tint),
        )
        Spacer(Modifier.width(5.dp))
        Text(
            text  = label.uppercase(),
            style = AppTypography.labelSmall.copy(color = tint, letterSpacing = 0.6.sp),
        )
    }
}

/* ═══════════════════════════════════════════════════════════════════════════
 *  5b. DsGlassChip — Translucent pill for gradient header surfaces
 * ═══════════════════════════════════════════════════════════════════════════ */

/**
 * Glassmorphic pill — white-on-brand pattern used inside the gradient
 * `PremiumHeaderBackground` headers (employee detail, task detail, etc.).
 * Drop-in replacement for the inline `Row { clip(pill).background(white@0.18) }`
 * pattern. Supports an optional inline value (e.g. "EMP-1234 · ACTIVE")
 * rendered with a soft dot separator.
 *
 * Use [DsStatusChip] for chips on white card surfaces — this variant exists
 * specifically for coloured gradient backgrounds where contrast comes from
 * white text instead of a tinted background.
 */
@Composable
fun DsGlassChip(
    label      : String,
    modifier   : Modifier = Modifier,
    trailing   : String? = null,
    bgAlpha    : Float = 0.18f,
    borderAlpha: Float = 0.25f,
) {
    Row(
        modifier              = modifier
            .clip(AppShapes.pill)
            .background(Color.White.copy(alpha = bgAlpha))
            .border(1.dp, Color.White.copy(alpha = borderAlpha), AppShapes.pill)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text  = label,
            style = AppTypography.labelSmall.copy(
                color         = Color.White,
                fontWeight    = FontWeight.Bold,
                letterSpacing = 1.2.sp,
            ),
        )
        if (!trailing.isNullOrBlank()) {
            Text(
                text     = "•",
                color    = Color.White.copy(alpha = 0.6f),
                fontSize = 11.sp,
            )
            Text(
                text  = trailing,
                style = AppTypography.labelSmall.copy(
                    color         = Color.White,
                    fontWeight    = FontWeight.Bold,
                    letterSpacing = 1.2.sp,
                ),
            )
        }
    }
}

/* ═══════════════════════════════════════════════════════════════════════════
 *  6. DsCodeChip — Item ID chip
 * ═══════════════════════════════════════════════════════════════════════════ */

/**
 * Monospaced ID chip — e.g. "#TASK-1234", "#EQ-0012".
 * Background is a 10 % alpha tint of [tint].
 */
@Composable
fun DsCodeChip(
    code    : String,
    tint    : Color,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(AppShapes.extraSmall)
            .background(tint.copy(alpha = 0.10f))
            .padding(horizontal = AppTheme.SpSm, vertical = 3.dp),
    ) {
        Text(
            text  = code,
            style = AppTypography.labelSmall.copy(color = tint, letterSpacing = 0.6.sp),
        )
    }
}

/* ═══════════════════════════════════════════════════════════════════════════
 *  6b. DsCheckbox — Rounded-square brand-blue checkbox with white tick
 * ═══════════════════════════════════════════════════════════════════════════ */

/**
 * Branded checkbox replacement for `androidx.compose.material3.Checkbox`.
 *
 * Visual:
 *  • Unchecked → white square (8 dp radius) with a 1.5 dp `Ink100` border
 *  • Checked   → solid brand-blue square with a white tick icon, soft shadow
 *  • State transitions are crossfaded so taps feel responsive without
 *    being jumpy.
 *
 * Use anywhere in the app instead of the Material default — keeps
 * checklists, settings, and form fields visually consistent.
 */
@Composable
fun DsCheckbox(
    checked  : Boolean,
    onChange : (Boolean) -> Unit,
    modifier : Modifier = Modifier,
    size     : Dp = 22.dp,
    enabled  : Boolean = true,
) {
    val shape = RoundedCornerShape(size.value.times(0.30f).dp)
    val bg by animateColorAsState(
        targetValue   = if (checked) AppTheme.Brand else Color.White,
        animationSpec = tween(durationMillis = 160),
        label         = "dsCheckboxBg",
    )
    val borderColor by animateColorAsState(
        targetValue   = if (checked) AppTheme.Brand else AppTheme.Ink100,
        animationSpec = tween(durationMillis = 160),
        label         = "dsCheckboxBorder",
    )
    Box(
        modifier         = modifier
            .size(size)
            .then(
                if (checked)
                    Modifier.shadow(2.dp, shape, spotColor = AppTheme.Brand.copy(alpha = 0.40f))
                else Modifier,
            )
            .clip(shape)
            .background(bg)
            .border(1.5.dp, borderColor, shape)
            .clickable(enabled = enabled) { onChange(!checked) },
        contentAlignment = Alignment.Center,
    ) {
        // Tick icon — fades + scales in when checked.
        val tickAlpha by animateFloatAsState(
            targetValue   = if (checked) 1f else 0f,
            animationSpec = tween(durationMillis = 160),
            label         = "dsCheckboxTickAlpha",
        )
        val tickScale by animateFloatAsState(
            targetValue   = if (checked) 1f else 0.5f,
            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
            label         = "dsCheckboxTickScale",
        )
        Icon(
            imageVector        = Icons.Filled.Check,
            contentDescription = null,
            tint               = Color.White,
            modifier           = Modifier
                .size(size.value.times(0.72f).dp)
                .scale(tickScale)
                .alpha(tickAlpha),
        )
    }
}

/* ═══════════════════════════════════════════════════════════════════════════
 *  7. DsProgressBar — Animated gradient horizontal progress bar
 * ═══════════════════════════════════════════════════════════════════════════ */

/**
 * Animated gradient progress bar. The fill slides from 0 to [targetFraction]
 * via a 900 ms ease-out animation triggered on first composition.
 *
 * @param targetFraction 0f..1f — the desired fill level.
 */
@Composable
fun DsProgressBar(
    targetFraction: Float,
    tint          : Color,
    modifier      : Modifier = Modifier,
    height        : Dp = 5.dp,
) {
    var driven by remember { mutableStateOf(0f) }
    LaunchedEffect(targetFraction) { driven = targetFraction }

    val animated by animateFloatAsState(
        targetValue   = driven,
        animationSpec = tween(durationMillis = 900, delayMillis = 100, easing = FastOutSlowInEasing),
        label         = "dsProgress",
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(AppShapes.pill)
            .background(AppTheme.Ink100),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(animated.coerceIn(0f, 1f))
                .height(height)
                .clip(AppShapes.pill)
                .background(
                    Brush.horizontalGradient(listOf(tint.copy(alpha = 0.70f), tint)),
                ),
        )
    }
}

/* ═══════════════════════════════════════════════════════════════════════════
 *  8. DsAvatarBubble — Gradient avatar circle with shadow
 * ═══════════════════════════════════════════════════════════════════════════ */

/**
 * Circular avatar with a deterministic gradient derived from [initials] and a
 * soft white border. Use wherever an assignee / employee avatar is needed.
 */
@Composable
fun DsAvatarBubble(
    initials: String,
    modifier: Modifier = Modifier,
    size    : Dp = AppTheme.AvatarMd,
) {
    val gradient = dsAvatarGradient(initials)
    Box(
        modifier          = modifier
            .size(size)
            .shadow(AppElevation.tooltip, CircleShape, spotColor = Color(0x30172C50))
            .clip(CircleShape)
            .background(Brush.linearGradient(gradient))
            .border(1.5.dp, Color.White, CircleShape),
        contentAlignment  = Alignment.Center,
    ) {
        Text(
            text  = initials.take(2).uppercase(),
            style = AppTypography.labelSmall.copy(
                color         = Color.White,
                fontSize      = (size.value * 0.33f).sp,
                letterSpacing = 0.4.sp,
            ),
        )
    }
}

/**
 * Horizontally overlapping avatar stack — used wherever a task has multiple
 * assignees. Renders up to [maxVisible] gradient avatars (each derived
 * deterministically via [dsAvatarGradient]) with a 35% horizontal overlap,
 * then a `+N` pill if there are more.
 *
 * @param initialsList per-assignee 2-letter initials, in render order.
 * @param size         avatar diameter (defaults to AvatarSm = 32 dp).
 * @param maxVisible   how many avatars to draw before collapsing to "+N".
 * @param overlap      fraction of the avatar that overlaps (0.0 – 0.6).
 */
@Composable
fun DsAvatarStack(
    initialsList: List<String>,
    modifier    : Modifier = Modifier,
    size        : Dp   = AppTheme.AvatarSm,
    maxVisible  : Int  = 3,
    overlap     : Float = 0.35f,
) {
    if (initialsList.isEmpty()) return
    val visible = initialsList.take(maxVisible)
    val overflow = initialsList.size - visible.size
    val overlapDp = size * overlap

    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        visible.forEachIndexed { i, initials ->
            DsAvatarBubble(
                initials = initials,
                size     = size,
                modifier = if (i == 0) Modifier else Modifier.offset(x = -overlapDp * i),
            )
        }
        if (overflow > 0) {
            // "+N" pill — circular tile with neutral surface, sized to match.
            Box(
                modifier = Modifier
                    .offset(x = -overlapDp * visible.size)
                    .size(size)
                    .clip(CircleShape)
                    .background(AppTheme.Ink50)
                    .border(1.5.dp, Color.White, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text  = "+$overflow",
                    style = AppTypography.labelSmall.copy(
                        color      = AppTheme.Ink700,
                        fontSize   = (size.value * 0.32f).sp,
                        fontWeight = FontWeight.Bold,
                    ),
                )
            }
        }
    }
}

/** Returns a deterministic two-stop gradient palette indexed by [initials]. */
fun dsAvatarGradient(initials: String): List<Color> {
    val palettes = listOf(
        listOf(Color(0xFF60A5FA), Color(0xFF1D4ED8)),
        listOf(Color(0xFFA78BFA), Color(0xFF6D28D9)),
        listOf(Color(0xFFFB923C), Color(0xFFC2410C)),
        listOf(Color(0xFF34D399), Color(0xFF047857)),
        listOf(Color(0xFFF472B6), Color(0xFFBE185D)),
        listOf(Color(0xFF38BDF8), Color(0xFF0284C7)),
        listOf(Color(0xFF86EFAC), Color(0xFF15803D)),
    )
    val idx = (initials.hashCode().let { if (it < 0) -it else it }) % palettes.size
    return palettes[idx]
}

/* ═══════════════════════════════════════════════════════════════════════════
 *  9. DsFilterChip — Selectable filter pill
 * ═══════════════════════════════════════════════════════════════════════════ */

/**
 * Tappable filter chip. Selected state fills with [tint]; unselected is
 * outlined. A small dot appears inside when selected.
 */
@Composable
fun DsFilterChip(
    label   : String,
    selected: Boolean,
    tint    : Color,
    onClick : () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Selected state: subtle vertical gradient (slightly lighter at the top,
    // tint at the bottom) gives the chip the ambient "top-light" finish that
    // matches the rest of the design system. Glow shadow tinted by the chip
    // colour so each filter reads as illuminated, not flat-filled.
    val selectedBrush = if (selected) {
        Brush.verticalGradient(
            colors = listOf(
                tint.copy(alpha = 0.92f),
                tint,
            ),
        )
    } else null
    Box(
        modifier = modifier
            .clip(AppShapes.medium)
            .then(
                if (selected) Modifier.shadow(
                    elevation    = 6.dp,
                    shape        = AppShapes.medium,
                    ambientColor = tint.copy(alpha = 0.30f),
                    spotColor    = tint.copy(alpha = 0.45f),
                ) else Modifier,
            )
            .clip(AppShapes.medium)
            .then(
                if (selectedBrush != null) Modifier.background(selectedBrush)
                else Modifier.background(Color.Transparent)
            )
            .border(1.dp, if (selected) Color.White.copy(alpha = 0.18f) else AppTheme.Ink100, AppShapes.medium)
            .clickable(onClick = onClick)
            .padding(horizontal = AppTheme.SpMd, vertical = AppTheme.SpSm),
    ) {
        Row(
            verticalAlignment    = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            if (selected) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.90f)),
                )
            }
            Text(
                text  = label,
                style = if (selected)
                    AppTypography.labelLarge.copy(color = Color.White)
                else
                    AppTypography.labelLarge.copy(color = AppTheme.Ink500),
            )
        }
    }
}

/* ═══════════════════════════════════════════════════════════════════════════
 *  10. DsGlassSearchBar — Glassmorphic search text input
 * ═══════════════════════════════════════════════════════════════════════════ */

/**
 * Glass-style search bar for use inside gradient headers.
 * On light backgrounds pass `onDark = false` for an opaque white pill.
 */
@Composable
fun DsGlassSearchBar(
    query       : String,
    onQueryChange: (String) -> Unit,
    placeholder : String = "Search…",
    modifier    : Modifier = Modifier,
    onDark      : Boolean = true,
) {
    // Glassmorphic surface — on dark headers, layer a top-light gradient on
    // top of a translucent white fill so the bar reads as frosted glass.
    // On light pages, layer a brand-tinted ambient glow so the search bar
    // still feels lifted off the page mesh.
    val baseBg = if (onDark) Color.White.copy(alpha = 0.14f) else AppTheme.Surface
    val borderColor = if (onDark) Color.White.copy(alpha = 0.22f)
                      else AppTheme.Brand.copy(alpha = 0.18f)
    val iconTint = if (onDark) Color.White.copy(alpha = 0.85f) else AppTheme.Ink500
    val textColor = if (onDark) Color.White else AppTheme.Ink900
    val hintColor = if (onDark) Color.White.copy(alpha = 0.55f) else AppTheme.Ink300

    val surfaceBrush = if (onDark) {
        Brush.verticalGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.20f),
                Color.White.copy(alpha = 0.06f),
            ),
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(
                Color.White,
                Color(0xFFF7FAFE),
            ),
        )
    }

    Row(
        modifier          = modifier
            .fillMaxWidth()
            .height(AppTheme.SearchBarHeight)
            // Brand-tinted glow only on the light variant — keeps the dark
            // header bar from glowing against the dark surface.
            .then(
                if (!onDark) Modifier.shadow(
                    elevation    = 2.dp,
                    shape        = AppShapes.large,
                    ambientColor = AppTheme.Brand.copy(alpha = 0.08f),
                    spotColor    = AppTheme.Brand.copy(alpha = 0.14f),
                ) else Modifier,
            )
            .clip(AppShapes.large)
            .background(baseBg)
            .background(surfaceBrush)
            .border(1.dp, borderColor, AppShapes.large)
            .padding(horizontal = AppTheme.SpLg),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector        = Icons.Filled.Search,
            contentDescription = null,
            tint               = iconTint,
            modifier           = Modifier.size(AppTheme.IconMd),
        )
        Spacer(Modifier.width(AppTheme.SpSm))
        Box(modifier = Modifier.weight(1f)) {
            if (query.isEmpty()) {
                Text(placeholder, style = AppTypography.bodyMedium.copy(color = hintColor))
            }
            BasicTextField(
                value         = query,
                onValueChange = onQueryChange,
                singleLine    = true,
                textStyle     = AppTypography.bodyMedium.copy(color = textColor),
                cursorBrush   = SolidColor(textColor),
                modifier      = Modifier.fillMaxWidth(),
            )
        }
    }
}

/* ═══════════════════════════════════════════════════════════════════════════
 *  11. DsKpiStat — KPI / metric value + label pair (stat cell)
 * ═══════════════════════════════════════════════════════════════════════════ */

/**
 * A single cell in a stats row: large colored value above a muted label.
 * Compose multiples side-by-side inside a `Row` with `DsVerticalDivider`.
 */
@Composable
fun DsKpiStat(
    value     : String,
    label     : String,
    valueColor: Color = AppTheme.Ink900,
    modifier  : Modifier = Modifier,
) {
    Column(
        modifier              = modifier.padding(horizontal = AppTheme.SpXs),
        horizontalAlignment   = Alignment.CenterHorizontally,
        verticalArrangement   = Arrangement.spacedBy(AppTheme.SpXs),
    ) {
        Text(
            text  = value,
            style = AppTypography.titleMedium.copy(
                color      = valueColor,
                fontSize   = when {
                    value.length <= 4  -> 22.sp
                    value.length <= 7  -> 16.sp
                    value.length <= 11 -> 13.sp
                    else               -> 11.sp
                },
            ),
            maxLines  = 1,
            overflow  = TextOverflow.Ellipsis,
        )
        Text(
            text  = label,
            style = AppTypography.captionSmall.copy(
                color         = AppTheme.Ink500,
                letterSpacing = 0.6.sp,
            ),
        )
    }
}

/* ═══════════════════════════════════════════════════════════════════════════
 *  12. DsKpiCard — Complete dashboard metric card with icon
 * ═══════════════════════════════════════════════════════════════════════════ */

/**
 * Self-contained KPI tile used on the admin dashboard.
 * Shows an icon in a tinted circle, a large numeric value, and a label.
 */
@Composable
fun DsKpiCard(
    icon      : ImageVector,
    value     : String,
    label     : String,
    tint      : Color = AppTheme.Brand,
    modifier  : Modifier = Modifier,
    onClick   : (() -> Unit)? = null,
) {
    DsCard(
        modifier       = modifier,
        shape          = AppShapes.large,
        elevation      = AppElevation.card,
        contentPadding = AppTheme.SpLg,
        pressScale     = onClick != null,
        onClick        = onClick,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(AppTheme.SpSm)) {
            Box(
                modifier          = Modifier
                    .size(AppTheme.AvatarSm)
                    .clip(AppShapes.medium)
                    .background(tint.copy(alpha = 0.12f)),
                contentAlignment  = Alignment.Center,
            ) {
                Icon(
                    imageVector        = icon,
                    contentDescription = null,
                    tint               = tint,
                    modifier           = Modifier.size(AppTheme.IconLg),
                )
            }
            Text(
                text  = value,
                style = AppTypography.headlineLarge.copy(color = AppTheme.Ink900),
            )
            Text(
                text  = label,
                style = AppTypography.captionLarge.copy(color = AppTheme.Ink500),
            )
        }
    }
}

/* ═══════════════════════════════════════════════════════════════════════════
 *  13. DsActionButton — Full-width gradient CTA button
 * ═══════════════════════════════════════════════════════════════════════════ */

/**
 * Primary call-to-action button with a brand gradient fill and press animation.
 *
 * @param loading when true shows a `CircularProgressIndicator` instead of the label.
 */
@Composable
fun DsActionButton(
    label   : String,
    onClick : () -> Unit,
    modifier: Modifier = Modifier,
    enabled : Boolean = true,
    loading : Boolean = false,
    icon    : ImageVector? = null,
) {
    val alpha = if (enabled) 1f else 0.50f

    Box(
        modifier          = modifier
            .fillMaxWidth()
            .height(AppTheme.ButtonHeight)
            .clip(AppShapes.large)
            .shadow(AppElevation.sm, AppShapes.large, spotColor = AppTheme.Brand.copy(alpha = 0.30f))
            .background(
                if (enabled)
                    Brush.linearGradient(listOf(AppTheme.Brand, AppTheme.Brand700))
                else
                    Brush.linearGradient(listOf(AppTheme.Ink300, AppTheme.Ink300)),
            )
            .clickable(enabled = enabled && !loading, onClick = onClick),
        contentAlignment  = Alignment.Center,
    ) {
        if (loading) {
            CircularProgressIndicator(
                color       = Color.White,
                strokeWidth = 2.dp,
                modifier    = Modifier.size(AppTheme.IconLg),
            )
        } else {
            Row(
                verticalAlignment    = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AppTheme.SpSm),
            ) {
                if (icon != null) {
                    Icon(
                        imageVector        = icon,
                        contentDescription = null,
                        tint               = Color.White.copy(alpha = alpha),
                        modifier           = Modifier.size(AppTheme.IconMd),
                    )
                }
                Text(
                    text  = label,
                    style = AppTypography.titleSmall.copy(color = Color.White.copy(alpha = alpha)),
                )
            }
        }
    }
}

/* ═══════════════════════════════════════════════════════════════════════════
 *  14. DsGlassButton — Glass-style icon button for gradient headers
 * ═══════════════════════════════════════════════════════════════════════════ */

/**
 * 44 dp semi-transparent square button used inside gradient headers.
 * Accepts any composable content (icon, text, image).
 */
@Composable
fun DsGlassButton(
    onClick : () -> Unit,
    modifier: Modifier = Modifier,
    content : @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier          = modifier
            .size(44.dp)
            .clip(AppShapes.medium)
            .background(Color.White.copy(alpha = 0.18f))
            .border(1.dp, Color.White.copy(alpha = 0.25f), AppShapes.medium)
            .clickable(onClick = onClick),
        contentAlignment  = Alignment.Center,
        content           = content,
    )
}

/* ═══════════════════════════════════════════════════════════════════════════
 *  15. DsVerticalDivider — Thin vertical divider for stat rows
 * ═══════════════════════════════════════════════════════════════════════════ */

@Composable
fun DsVerticalDivider(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .width(1.dp)
            .fillMaxHeight()
            .padding(vertical = AppTheme.SpSm)
            .background(AppTheme.Ink100),
    )
}

/* ═══════════════════════════════════════════════════════════════════════════
 *  16. DsTaskCard — Universal premium task card (list + kanban)
 * ═══════════════════════════════════════════════════════════════════════════ */

/**
 * Premium task card used across both the user task list and admin kanban board.
 *
 * The card is fully decoupled from business models — all data is passed as
 * presentation-ready strings and colors so the design system stays generic.
 *
 * @param taskCode       formatted ID string e.g. "#TASK-1234"
 * @param title          task title
 * @param locationText   location display string e.g. "Yelahanka · 2.4 km"
 * @param dueText        formatted due date/time e.g. "Today  10:30"
 * @param accentColor    priority-derived accent (Danger / Warning / Success)
 * @param priorityLabel  uppercase priority text e.g. "HIGH"
 * @param statusLabel    human-readable status e.g. "In progress"
 * @param statusColor    foreground color for status badge
 * @param statusBg       background color for status badge
 * @param assigneeInitials e.g. "RK"
 * @param showProgress   show the animated progress bar (InProgress tasks only)
 * @param progressFraction 0f..1f progress value
 * @param isCompleted    if true shows a DONE indicator instead of priority chip
 */
@Composable
fun DsTaskCard(
    taskCode         : String,
    title            : String,
    locationText     : String,
    dueText          : String,
    accentColor      : Color,
    priorityLabel    : String,
    statusLabel      : String,
    statusColor      : Color,
    statusBg         : Color,
    assigneeInitials : String,
    onClick          : () -> Unit,
    modifier         : Modifier = Modifier,
    showProgress     : Boolean = false,
    progressFraction : Float = 0f,
    isCompleted      : Boolean = false,
) {
    DsAccentCard(
        accentColor = accentColor,
        modifier    = modifier,
        pressScale  = true,
        onClick     = onClick,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(AppTheme.SpSm)) {

            // ── Row 1: code chip · priority chip · due time ────────────
            Row(verticalAlignment = Alignment.CenterVertically) {
                DsCodeChip(code = taskCode, tint = accentColor)
                Spacer(Modifier.width(AppTheme.SpSm))
                if (isCompleted) {
                    DsStatusChip("DONE", AppTheme.Success, AppTheme.SuccessBg)
                } else {
                    DsPriorityChip(label = priorityLabel, tint = accentColor)
                }
                Spacer(Modifier.weight(1f))
                Text(
                    text  = dueText,
                    style = AppTypography.captionSmall.copy(
                        color      = if (showProgress) accentColor else AppTheme.Ink500,
                        fontWeight = FontWeight.SemiBold,
                    ),
                )
            }

            // ── Row 2: title ───────────────────────────────────────────
            Text(
                text     = title,
                style    = AppTypography.titleSmall.copy(color = AppTheme.Ink900, lineHeight = 20.sp),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )

            // ── Row 3: location ────────────────────────────────────────
            if (locationText.isNotBlank()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector        = Icons.Filled.LocationOn,
                        contentDescription = null,
                        tint               = AppTheme.Ink300,
                        modifier           = Modifier.size(AppTheme.IconXs),
                    )
                    Spacer(Modifier.width(3.dp))
                    Text(
                        text     = locationText,
                        style    = AppTypography.captionSmall.copy(color = AppTheme.Ink300),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            // ── Row 4: progress bar (InProgress only) ─────────────────
            if (showProgress) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    DsProgressBar(
                        targetFraction = progressFraction,
                        tint           = accentColor,
                        modifier       = Modifier.weight(1f),
                    )
                    Spacer(Modifier.width(AppTheme.SpSm))
                    Text(
                        text  = "${(progressFraction * 100).toInt()}%",
                        style = AppTypography.labelSmall.copy(color = accentColor),
                    )
                }
            }

            Spacer(Modifier.height(AppTheme.SpXs))

            // ── Row 5: avatar · assignee · status badge ────────────────
            Row(verticalAlignment = Alignment.CenterVertically) {
                DsAvatarBubble(initials = assigneeInitials, size = AppTheme.AvatarMd)
                Spacer(Modifier.width(AppTheme.SpSm))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text     = assigneeInitials,
                        style    = AppTypography.captionLarge.copy(
                            color      = AppTheme.Ink900,
                            fontWeight = FontWeight.SemiBold,
                        ),
                        maxLines = 1,
                    )
                    Text(
                        text  = "Assignee",
                        style = AppTypography.labelSmall.copy(
                            color         = AppTheme.Ink300,
                            letterSpacing = 0.4.sp,
                        ),
                    )
                }
                DsStatusChip(
                    label      = statusLabel,
                    tint       = statusColor,
                    background = statusBg,
                )
            }
        }
    }
}

/* ═══════════════════════════════════════════════════════════════════════════
 *  17. DsEmptyState — Illustrated empty state
 * ═══════════════════════════════════════════════════════════════════════════ */

/**
 * Centred empty-state screen with an emoji glyph, title, and body text.
 * Drop-in replacement for the old `EmptyState` / `ErrorState` composables.
 */
@Composable
fun DsEmptyState(
    emoji   : String,
    title   : String,
    body    : String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier         = modifier
            .fillMaxSize()
            .padding(AppTheme.SpXxl),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(AppTheme.SpSm),
        ) {
            Box(
                modifier          = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(AppTheme.Brand50),
                contentAlignment  = Alignment.Center,
            ) {
                Text(emoji, fontSize = 32.sp)
            }
            Spacer(Modifier.height(AppTheme.SpXs))
            Text(
                text  = title,
                style = AppTypography.titleLarge.copy(color = AppTheme.Ink900),
            )
            Text(
                text      = body,
                style     = AppTypography.bodySmall.copy(color = AppTheme.Ink500),
                textAlign = TextAlign.Center,
                modifier  = Modifier.fillMaxWidth(),
            )
        }
    }
}

/* ═══════════════════════════════════════════════════════════════════════════
 *  17b. DsAllCaughtUpEmptyState — Premium illustrated empty state card
 * ═══════════════════════════════════════════════════════════════════════════ */

/**
 * Rich, card-style empty state used when a list has *no items at all*
 * (as opposed to a filter producing zero matches — that should still use
 * [DsEmptyState]).
 *
 * Layout matches the design reference:
 *  - "EMPTY" pill chip top-left
 *  - Centred rounded-square icon tile with brand tint
 *  - Bold title with optional celebratory emoji suffix
 *  - Muted body copy
 *  - Optional gradient CTA at bottom (e.g. "View tomorrow")
 *
 * Pass `actionLabel = null` to hide the CTA entirely.
 */
@Composable
fun DsAllCaughtUpEmptyState(
    title       : String,
    body        : String,
    modifier    : Modifier = Modifier,
    pillLabel   : String = "EMPTY",
    actionLabel : String? = null,
    onAction    : (() -> Unit)? = null,
) {
    Box(
        modifier         = modifier
            .fillMaxSize()
            .padding(horizontal = AppTheme.SpLg, vertical = AppTheme.SpXl),
        contentAlignment = Alignment.Center,
    ) {
        DsCard(contentPadding = AppTheme.SpXl) {
            Column(
                modifier            = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // ─── Top-left "✨ EMPTY" pill ────────────────────────────────
                Row(
                    modifier          = Modifier
                        .align(Alignment.Start)
                        .clip(AppShapes.pill)
                        .background(AppTheme.Brand50)
                        .padding(horizontal = AppTheme.SpMd, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("✨", fontSize = 12.sp)
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text  = pillLabel,
                        style = AppTypography.labelSmall.copy(
                            color         = AppTheme.Brand,
                            fontWeight    = FontWeight.SemiBold,
                            letterSpacing = 1.4.sp,
                        ),
                    )
                }

                Spacer(Modifier.height(AppTheme.SpXl))

                // ─── Centred rounded-square icon tile ─────────────────────
                Box(
                    modifier         = Modifier
                        .size(96.dp)
                        .clip(AppShapes.extraLarge)
                        .background(AppTheme.Brand50),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        modifier         = Modifier
                            .size(36.dp)
                            .clip(AppShapes.small)
                            .background(Color.Transparent)
                            .border(2.dp, AppTheme.Brand, AppShapes.small),
                        contentAlignment = Alignment.Center,
                    ) {
                        Box(
                            modifier = Modifier
                                .width(14.dp)
                                .height(2.dp)
                                .clip(AppShapes.extraSmall)
                                .background(AppTheme.Brand),
                        )
                    }
                }

                Spacer(Modifier.height(AppTheme.SpXl))

                // ─── Title ────────────────────────────────────────────────
                Text(
                    text      = title,
                    style     = AppTypography.titleLarge.copy(
                        color      = AppTheme.Ink900,
                        fontWeight = FontWeight.Bold,
                    ),
                    textAlign = TextAlign.Center,
                )

                Spacer(Modifier.height(AppTheme.SpSm))

                // ─── Body ─────────────────────────────────────────────────
                Text(
                    text      = body,
                    style     = AppTypography.bodyMedium.copy(color = AppTheme.Ink500),
                    textAlign = TextAlign.Center,
                    modifier  = Modifier.fillMaxWidth(),
                )

                // ─── Optional CTA ─────────────────────────────────────────
                if (actionLabel != null && onAction != null) {
                    Spacer(Modifier.height(AppTheme.SpXl))
                    DsActionButton(
                        label   = actionLabel,
                        onClick = onAction,
                    )
                }
            }
        }
    }
}

/* ═══════════════════════════════════════════════════════════════════════════
 *  18. DsLoadingCard — Shimmer skeleton placeholder for a task card
 * ═══════════════════════════════════════════════════════════════════════════ */

/**
 * Shimmer skeleton card shown while task data is loading.
 * Matches the height and structure of [DsTaskCard].
 */
@Composable
fun DsLoadingCard(modifier: Modifier = Modifier) {
    DsCard(modifier = modifier, contentPadding = AppTheme.SpMd) {
        Column(verticalArrangement = Arrangement.spacedBy(AppTheme.SpSm)) {
            DsSkeletonBlock(Modifier.fillMaxWidth(0.35f).height(18.dp))
            DsSkeletonBlock(Modifier.fillMaxWidth(0.75f).height(16.dp))
            DsSkeletonBlock(Modifier.fillMaxWidth(0.55f).height(14.dp))
            DsSkeletonBlock(Modifier.fillMaxWidth().height(5.dp))
            Row(
                verticalAlignment    = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AppTheme.SpSm),
            ) {
                DsSkeletonBlock(Modifier.size(AppTheme.AvatarMd).clip(CircleShape))
                DsSkeletonBlock(Modifier.fillMaxWidth(0.40f).height(14.dp))
                Spacer(Modifier.weight(1f))
                DsSkeletonBlock(Modifier.width(60.dp).height(20.dp))
            }
        }
    }
}

@Composable
private fun DsSkeletonBlock(modifier: Modifier) {
    Box(
        modifier = modifier
            .clip(AppShapes.extraSmall)
            .background(AppTheme.Ink100)
            .shimmerOverlay(),
    )
}
