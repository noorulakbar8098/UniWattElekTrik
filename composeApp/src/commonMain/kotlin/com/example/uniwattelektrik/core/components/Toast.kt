package com.example.uniwattelektrik.core.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.uniwattelektrik.core.theme.AppElevation
import com.example.uniwattelektrik.core.theme.AppShapes
import com.example.uniwattelektrik.core.theme.AppTheme
import com.example.uniwattelektrik.core.theme.AppTypography
import kotlin.math.absoluteValue
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/* ═══════════════════════════════════════════════════════════════════════════
 *  Toast model + global controller
 * ═══════════════════════════════════════════════════════════════════════════ */

/** Visual variant — drives the icon tile colour & glyph. */
enum class ToastKind { Success, Info, Error }

/** A single toast notification. [id] is auto-generated; identity is by id. */
data class Toast(
    val id          : Long,
    val kind        : ToastKind,
    val title       : String,
    val body        : String?       = null,
    val actionLabel : String?       = null,
    val onAction    : (() -> Unit)? = null,
)

/**
 * App-wide toast queue. Single source of truth — call [show] / [success] /
 * [info] / [error] from anywhere (ViewModels, callbacks, UI) and a [ToastHost]
 * mounted at the app root will render and auto-dismiss them.
 *
 * No DI needed: this is a Kotlin object, mirroring `DeepLinkBus`.
 */
object ToastController {
    private var nextId = 1L
    private val _queue = MutableStateFlow<List<Toast>>(emptyList())
    val queue: StateFlow<List<Toast>> = _queue.asStateFlow()

    fun show(toast: Toast) {
        _queue.update { it + toast }
    }

    fun dismiss(id: Long) {
        _queue.update { current -> current.filterNot { it.id == id } }
    }

    fun success(title: String, body: String? = null, actionLabel: String? = null, onAction: (() -> Unit)? = null) =
        show(Toast(nextId++, ToastKind.Success, title, body, actionLabel, onAction))

    fun info(title: String, body: String? = null, actionLabel: String? = null, onAction: (() -> Unit)? = null) =
        show(Toast(nextId++, ToastKind.Info, title, body, actionLabel, onAction))

    fun error(title: String, body: String? = null, actionLabel: String? = null, onAction: (() -> Unit)? = null) =
        show(Toast(nextId++, ToastKind.Error, title, body, actionLabel, onAction))
}

/* ═══════════════════════════════════════════════════════════════════════════
 *  ToastHost — mount at app root
 * ═══════════════════════════════════════════════════════════════════════════ */

/**
 * Renders the top-most queued toast at the top of the screen. Auto-dismisses
 * after [autoDismissMs]. Drag horizontally to swipe-dismiss.
 *
 * Place in a top-level Box once per shell, aligned to TopCenter.
 */
@Composable
fun ToastHost(
    modifier      : Modifier = Modifier,
    autoDismissMs : Long     = 4_000,
) {
    val queue by ToastController.queue.collectAsStateWithLifecycle()
    val current = queue.firstOrNull()

    // Auto-dismiss timer keyed to the visible toast's id.
    LaunchedEffect(current?.id) {
        val t = current ?: return@LaunchedEffect
        delay(autoDismissMs)
        ToastController.dismiss(t.id)
    }

    Box(
        modifier         = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(horizontal = AppTheme.SpLg, vertical = AppTheme.SpSm),
        contentAlignment = Alignment.TopCenter,
    ) {
        AnimatedVisibility(
            visible = current != null,
            enter   = slideInVertically { -it } + fadeIn(),
            exit    = slideOutVertically { -it } + fadeOut(),
        ) {
            if (current != null) {
                DsToast(
                    toast     = current,
                    onDismiss = { ToastController.dismiss(current.id) },
                )
            }
        }
    }
}

/* ═══════════════════════════════════════════════════════════════════════════
 *  DsToast — single toast card
 * ═══════════════════════════════════════════════════════════════════════════ */

@Composable
fun DsToast(
    toast    : Toast,
    onDismiss: () -> Unit,
    modifier : Modifier = Modifier,
) {
    val palette = toast.kind.palette()

    // Swipe-to-dismiss: track horizontal offset, dismiss when past threshold.
    var dragOffset by remember { mutableStateOf(0f) }
    val dismissThresholdPx = 220f

    Box(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer { translationX = dragOffset; alpha = 1f - (dragOffset.absoluteValue / 600f).coerceAtMost(0.6f) }
            .pointerInput(toast.id) {
                detectHorizontalDragGestures(
                    onDragEnd = {
                        if (dragOffset.absoluteValue > dismissThresholdPx) onDismiss()
                        else dragOffset = 0f
                    },
                    onHorizontalDrag = { _, delta -> dragOffset += delta },
                )
            }
            // Toast palette-tinted ambient shadow gives each kind (success /
            // info / warning / error) its own soft glow halo. The surface
            // gets a top-light gradient layered over the flat Surface so the
            // toast reads as a frosted glass slab rather than a flat card.
            .shadow(
                elevation    = 10.dp,
                shape        = AppShapes.large,
                ambientColor = palette.iconTint.copy(alpha = 0.20f),
                spotColor    = palette.iconTint.copy(alpha = 0.30f),
            )
            .clip(AppShapes.large)
            .background(AppTheme.Surface)
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.70f),
                        AppTheme.Surface,
                    ),
                ),
            )
            .border(1.dp, palette.border, AppShapes.large)
            .padding(AppTheme.SpMd),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // ── Icon tile ─────────────────────────────────────────────────
            Box(
                modifier         = Modifier
                    .size(40.dp)
                    .clip(AppShapes.medium)
                    .background(palette.tileBg),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector        = toast.kind.icon(),
                    contentDescription = null,
                    tint               = palette.iconTint,
                    modifier           = Modifier.size(AppTheme.IconMd),
                )
            }

            Spacer(Modifier.width(AppTheme.SpMd))

            // ── Title + body ──────────────────────────────────────────────
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text  = toast.title,
                    style = AppTypography.titleSmall.copy(
                        color      = AppTheme.Ink900,
                        fontWeight = FontWeight.Bold,
                    ),
                )
                if (!toast.body.isNullOrBlank()) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text  = toast.body,
                        style = AppTypography.bodySmall.copy(color = AppTheme.Ink500),
                    )
                }
            }

            // ── Optional action ───────────────────────────────────────────
            if (toast.actionLabel != null && toast.onAction != null) {
                Spacer(Modifier.width(AppTheme.SpSm))
                Box(
                    modifier         = Modifier
                        .height(36.dp)
                        .clip(AppShapes.small)
                        .border(1.dp, palette.border, AppShapes.small)
                        .clickable {
                            toast.onAction.invoke()
                            onDismiss()
                        }
                        .padding(horizontal = AppTheme.SpMd),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text  = toast.actionLabel.uppercase(),
                        style = AppTypography.labelSmall.copy(
                            color      = palette.iconTint,
                            fontWeight = FontWeight.Bold,
                        ),
                    )
                }
            }
        }
    }
}

/* ─── Per-kind palette + glyph ─────────────────────────────────────────── */

private data class ToastPalette(
    val tileBg  : Color,
    val iconTint: Color,
    val border  : Color,
)

private fun ToastKind.palette(): ToastPalette = when (this) {
    ToastKind.Success -> ToastPalette(
        tileBg   = AppTheme.SuccessBg,
        iconTint = AppTheme.Success,
        border   = AppTheme.Success.copy(alpha = 0.25f),
    )
    ToastKind.Info -> ToastPalette(
        tileBg   = AppTheme.Brand,
        iconTint = Color.White,
        border   = AppTheme.Brand.copy(alpha = 0.25f),
    )
    ToastKind.Error -> ToastPalette(
        tileBg   = AppTheme.DangerBg,
        iconTint = AppTheme.Danger,
        border   = AppTheme.Danger.copy(alpha = 0.25f),
    )
}

private fun ToastKind.icon() = when (this) {
    ToastKind.Success -> Icons.Filled.Check
    ToastKind.Info    -> Icons.Filled.Info
    ToastKind.Error   -> Icons.Filled.Close
}
