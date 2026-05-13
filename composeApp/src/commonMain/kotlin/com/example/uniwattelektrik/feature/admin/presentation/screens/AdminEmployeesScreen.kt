package com.example.uniwattelektrik.feature.admin.presentation.screens

import com.example.uniwattelektrik.core.performance.TrackScreenPerformance

import com.example.uniwattelektrik.core.components.DottedClickable
import com.example.uniwattelektrik.core.components.DottedField
import com.example.uniwattelektrik.core.components.DottedReadOnly
import com.example.uniwattelektrik.core.components.FieldLabel
import com.example.uniwattelektrik.core.theme.AppTheme
import com.example.uniwattelektrik.core.theme.AppShapes
import com.example.uniwattelektrik.core.theme.appScreenBackground

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Apartment
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.Cake
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Engineering
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Wc
import androidx.compose.material.icons.outlined.Work
import androidx.compose.material3.Icon
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import com.example.uniwattelektrik.core.components.AppPullToRefresh
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.imePadding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import com.example.uniwattelektrik.platform.PlatformBackHandler
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.onFocusEvent
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.em
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.example.uniwattelektrik.core.media.rememberPhotoPicker
import com.example.uniwattelektrik.feature.workforce.data.remote.EmployeeDraft
import com.example.uniwattelektrik.feature.workforce.data.remote.EmployeeRecord
import com.example.uniwattelektrik.feature.workforce.presentation.WorkforceViewModel
import kotlinx.coroutines.delay
import kotlinx.datetime.toLocalDateTime
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
private val GradStart    = AppTheme.Brand
private val GradEnd      = AppTheme.Navy
private val WhiteAlpha20 = Color(0x33FFFFFF)
private val WhiteAlpha70 = Color(0xB3FFFFFF)
private val Leave        = AppTheme.Violet
private val LeaveBg      = AppTheme.PriorityUrgentBg
private val OffDuty      = AppTheme.StatusLeave
private val OffDutyBg    = AppTheme.StatusLeaveBg
private val ChipBg       = AppTheme.SurfaceMuted
// Previously missing — now resolved via design system
private val Highlight    = AppTheme.Brand


/* ── Local design tokens (per pixel-perfect spec) ─────────────────────── */

/* ── Filter category mapping (role text → bucket) ─────────────────────── */
private enum class RoleCategory(val label: String) {
    All("All"), Field("Field"), Office("Office"), Support("Support");
    companion object {
        fun of(role: String): RoleCategory = when {
            role.contains("Office",    ignoreCase = true) ||
                role.contains("Admin",  ignoreCase = true) ||
                role.contains("Manager",ignoreCase = true) -> Office
            role.contains("Support",   ignoreCase = true) ||
                role.contains("Helper", ignoreCase = true) -> Support
            else -> Field   // Engineer / Electrician / Technician / etc.
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminEmployeesScreen(
    adminUid: String,
    workforceVm: WorkforceViewModel,
    onEmployeeClick: (employeeId: String) -> Unit,
    onShowAddChange: (show: Boolean) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    TrackScreenPerformance("AdminEmployeesScreen")
    val employees by workforceVm.employees.collectAsStateWithLifecycle()
    val loading   by workforceVm.loading.collectAsStateWithLifecycle()
    val error     by workforceVm.error.collectAsStateWithLifecycle()

    com.example.uniwattelektrik.core.theme.SetStatusBar(color = GradStart, darkIcons = false)

    var query     by rememberSaveable { mutableStateOf("") }
    var category  by rememberSaveable { mutableStateOf(RoleCategory.All) }
    var showAdd   by remember { mutableStateOf(false) }
    // Restore scroll position when navigating back from EmployeeDetail.
    val listState = rememberSaveable(saver = LazyListState.Saver) { LazyListState() }

    // Notify parent shell to hide nav bar when add form is shown
    LaunchedEffect(showAdd) { onShowAddChange(showAdd) }

    // Track the last successfully-created employee so the Add screen can show
    // a "Share via WhatsApp" card with the temp credentials before close.
    var lastCreatedName     by remember { mutableStateOf<String?>(null) }
    var lastCreatedEmail    by remember { mutableStateOf<String?>(null) }
    var lastCreatedPassword by remember { mutableStateOf<String?>(null) }
    var lastCreatedPhone    by remember { mutableStateOf<String?>(null) }

    // ── Full-screen "Add Employee" page (replaces the list while open) ───
    if (showAdd) {
        AddEmployeeSheet(
            saving   = loading,
            errorMsg = error,
            createdName     = lastCreatedName,
            createdEmail    = lastCreatedEmail,
            createdPassword = lastCreatedPassword,
            createdPhone    = lastCreatedPhone,
            onDoneSharing = {
                lastCreatedName = null; lastCreatedEmail = null
                lastCreatedPassword = null; lastCreatedPhone = null
                showAdd = false
            },
            onCancel = {
                workforceVm.clearError()
                lastCreatedName = null; lastCreatedEmail = null
                lastCreatedPassword = null; lastCreatedPhone = null
                showAdd = false
            },
            onSubmit = { draft ->
                workforceVm.clearError()
                workforceVm.addEmployee(adminUid, draft) { result ->
                    if (result.isSuccess) {
                        lastCreatedName     = draft.name
                        lastCreatedEmail    = draft.email
                        lastCreatedPassword = draft.password
                        lastCreatedPhone    = draft.phone
                    }
                }
            },
            modifier = modifier,
        )
        return
    }

    val activeCount  = employees.count { it.status == "Active" }
    val onLeaveCount = employees.count { it.status == "OnLeave" }

    val filtered = employees.asSequence()
        .filter { category == RoleCategory.All || RoleCategory.of(it.role) == category }
        .filter {
            query.isBlank() ||
                it.name.contains(query, ignoreCase = true) ||
                it.role.contains(query, ignoreCase = true) ||
                it.id.contains(query, ignoreCase = true) ||
                it.zone.contains(query, ignoreCase = true)
        }
        .toList()
        // Newest hires first. Falls back to name when joining date is missing
        // so two new employees with no join date still order deterministically.
        .sortedWith(
            compareByDescending<EmployeeRecord> { it.joiningDateMs ?: 0L }
                .thenBy { it.name.lowercase() }
        )

    Box(modifier = modifier.fillMaxSize().background(appScreenBackground())) {
        Column(modifier = Modifier.fillMaxSize()) {
            // ── Sticky Gradient header (title + counts + search + chips) ─────
            EmployeesGradientHeader(
                activeCount  = activeCount,
                onLeaveCount = onLeaveCount,
                query        = query,
                onQuery      = { query = it },
                category     = category,
                onCategory   = { category = it },
                onAdd        = { showAdd = true },
            )

            // ── Scrollable list content ─────────────────────────────────
            if (loading && employees.isEmpty()) {
                // Header stays visible above; shimmer renders BELOW it.
                com.example.uniwattelektrik.core.components.InlineSkeleton(
                    type = com.example.uniwattelektrik.core.components.SkeletonType.EmployeeList,
                    modifier = Modifier.weight(1f),
                )
            } else {
            AppPullToRefresh(
                onRefresh = { workforceVm.refresh() },
                modifier  = Modifier.weight(1f),
            ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                state = listState,
                contentPadding = PaddingValues(bottom = 120.dp), // clear of FAB + nav
                verticalArrangement = Arrangement.spacedBy(0.dp),
            ) {
                // Optional inline error
                error?.let {
                    item {
                        Text(
                            text = "⚠ $it",
                            color = Danger,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                        )
                    }
                }

                // ── Empty / loading states ─────────────────────────────────
                when {
                    loading && employees.isEmpty() -> item {
                        InfoBlock(emoji = "⏳", title = "Loading", body = "Fetching your team…")
                    }
                    employees.isEmpty() -> item {
                        InfoBlock(
                            emoji = "👥", title = "No employees yet",
                            body = "Tap the + button to add your first employee.",
                        )
                    }
                    filtered.isEmpty() -> item {
                        InfoBlock(emoji = "🔎", title = "No matches", body = "Try a different filter or search term.")
                    }
                    else -> {
                        items(filtered, key = { it.id }) { e ->
                            Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                                EmployeeCard(e, onClick = { onEmployeeClick(e.id) })
                            }
                        }
                    }
                }
            }
            } // AppPullToRefresh
            } // else (loading skeleton branch)
        }     // Column

        // ── Floating action button (sits above the bottom nav, matches Task list FAB) ────
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(end = 24.dp, bottom = 100.dp)
                .size(64.dp)
                .shadow(elevation = 22.dp, shape = CircleShape, spotColor = Highlight.copy(alpha = 0.6f))
                .clip(CircleShape)
                .background(Brush.verticalGradient(listOf(GradStart, GradEnd)))
                .clickable { showAdd = true },
            contentAlignment = Alignment.Center,
        ) {
            Text("+", color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Bold)
        }
    }
}

/* ──────────────────────────────────────────────────────────────────────────
 *  Gradient header: title + counts + search + role chips
 * ────────────────────────────────────────────────────────────────────────── */
@Composable
private fun EmployeesGradientHeader(
    activeCount: Int,
    onLeaveCount: Int,
    query: String,
    onQuery: (String) -> Unit,
    category: RoleCategory,
    onCategory: (RoleCategory) -> Unit,
    onAdd: () -> Unit,
) {
    com.example.uniwattelektrik.core.components.OperationsHeader(
        eyebrow  = "TEAM",
        title    = "Employees",
        subtitle = "$activeCount active · $onLeaveCount on leave",
        extras = {
            // Search bar
            SearchField(query = query, onQuery = onQuery)
            Spacer(Modifier.height(12.dp))
            // Role chips
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                RoleCategory.entries.forEach { c ->
                    RoleChip(
                        label    = c.label,
                        selected = category == c,
                        onClick  = { onCategory(c) },
                    )
                }
            }
        },
    )
}

@Composable
private fun HeaderIconButton(emoji: String, semiTransparent: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(if (semiTransparent) WhiteAlpha20 else Color.White)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = emoji,
            color = if (semiTransparent) Color.White else Highlight,
            fontSize = if (emoji == "+") 22.sp else 24.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun SearchField(query: String, onQuery: (String) -> Unit) {
    var focused by remember { mutableStateOf(false) }

    // Premium glass-morphism effect with animation
    val bgColor by animateColorAsState(
        targetValue = if (focused) WhiteAlpha20.copy(alpha = 0.25f) else WhiteAlpha20,
        animationSpec = tween(200), label = "search-bg"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        bgColor.copy(alpha = bgColor.alpha * 1.1f),
                        bgColor
                    )
                )
            )
            .border(1.5.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(20.dp))
            .shadow(
                elevation = 12.dp,
                shape = RoundedCornerShape(20.dp),
                spotColor = Color.Black.copy(alpha = 0.1f)
            )
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Animated search icon
            val iconScale by animateFloatAsState(
                targetValue = if (focused) 1.1f else 1f,
                animationSpec = tween(200), label = "search-icon"
            )
            Text("🔍", fontSize = 14.sp, modifier = Modifier.scale(iconScale))
            Spacer(Modifier.width(8.dp))
            Box(modifier = Modifier.weight(1f)) {
                if (query.isEmpty()) {
                    Text(
                        text  = "Search by name, ID, designation",
                        color = WhiteAlpha70,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }
                BasicTextField(
                    value         = query,
                    onValueChange = onQuery,
                    singleLine    = true,
                    textStyle     = TextStyle(
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    ),
                    cursorBrush   = SolidColor(Color.White),
                    modifier = Modifier.onFocusEvent { focused = it.isFocused },
                )
            }
            if (query.isNotEmpty()) {
                Spacer(Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color.White.copy(alpha = 0.2f))
                        .clickable { onQuery("") },
                    contentAlignment = Alignment.Center,
                ) {
                    Text("✕", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun RoleChip(label: String, selected: Boolean, onClick: () -> Unit) {
    // Premium chip with smooth animations
    val bgBrush = if (selected) {
        Brush.horizontalGradient(listOf(Color(0xFF4A90E2), Highlight))
    } else {
        Brush.linearGradient(listOf(WhiteAlpha20, WhiteAlpha20))
    }

    val elevation by animateDpAsState(
        targetValue = if (selected) 8.dp else 0.dp,
        animationSpec = tween(200), label = "chip-elevation"
    )

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .shadow(elevation = elevation, shape = RoundedCornerShape(999.dp))
            .background(bgBrush)
            .border(
                width = 1.5.dp,
                color = if (selected) Color.White.copy(alpha = 0.4f) else Color.Transparent,
                shape = RoundedCornerShape(999.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
    ) {
        Text(
            text       = label,
            color      = Color.White,
            fontSize   = 12.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            letterSpacing = 0.3.sp,
        )
    }
}

/* ──────────────────────────────────────────────────────────────────────────
 *  Employee card
 * ────────────────────────────────────────────────────────────────────────── */
private val avatarPalette = listOf(
    listOf(Color(0xFFFFB28A), Color(0xFFEC8552)),
    listOf(Color(0xFFA5C8FF), Color(0xFF1E73E8)),
    listOf(Color(0xFFB5F0C0), Color(0xFF10B981)),
    listOf(Color(0xFFBFD7FF), Color(0xFF1A6BF5)),
    listOf(Color(0xFFFFC8E0), Color(0xFFEF4444)),
    listOf(Color(0xFFFFD6A5), Color(0xFFF59E0B)),
)

private data class StatusVisual(
    val label: String,
    val color: Color,
    val bg: Color,
    val secondary: String? = null,
)

private fun EmployeeRecord.statusVisual(): StatusVisual = when (status) {
    "Active"   -> StatusVisual("Active",   Success, SuccessBg)
    "OnLeave"  -> StatusVisual("Leave",    Leave,   LeaveBg, secondary = "Until 28 Apr")
    "Inactive" -> StatusVisual("Off-duty", OffDuty, OffDutyBg, secondary = "Last 8:42")
    else       -> StatusVisual(status,     OffDuty, OffDutyBg)
}

@Composable
private fun EmployeeCard(e: EmployeeRecord, onClick: () -> Unit) {
    val gradient = avatarPalette[(e.id.hashCode() and 0x7FFFFFFF) % avatarPalette.size]
    val visual   = e.statusVisual()
    val shape    = RoundedCornerShape(20.dp)
    var isPressed by remember { mutableStateOf(false) }

    // Premium elevation animation on press
    val elevation by animateDpAsState(
        targetValue = if (isPressed) 16.dp else 10.dp,
        animationSpec = tween(150), label = "card-elevation"
    )

    // Premium card color transition
    val cardColor by animateColorAsState(
        targetValue = if (isPressed) Color(0xFFF8FAFC) else CardBg,
        animationSpec = tween(150), label = "card-color"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 96.dp)
            .shadow(elevation = elevation, shape = shape, spotColor = ShadowSoft)
            .clip(shape)
            .background(cardColor)
            .border(
                width = 1.dp,
                color = if (isPressed) Highlight.copy(alpha = 0.2f) else Color.Transparent,
                shape = shape
            )
            .clickable(onClick = onClick)
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        isPressed = event.type == PointerEventType.Press
                    }
                }
            }
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Premium Avatar with glow effect
            Box(modifier = Modifier.size(48.dp)) {
                // Glow layer behind avatar
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    gradient[0].copy(alpha = 0.3f),
                                    Color.Transparent
                                )
                            )
                        )
                )
                // Avatar
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Brush.linearGradient(gradient)),
                    contentAlignment = Alignment.Center,
                ) {
                    if (e.photoUrl.isNotBlank()) {
                        AsyncImage(
                            model = e.photoUrl,
                            contentDescription = e.name,
                            modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(14.dp)),
                            contentScale = ContentScale.Crop,
                        )
                    } else {
                        Text(
                            text = e.name.split(" ").take(2)
                                .mapNotNull { it.firstOrNull()?.uppercaseChar() }
                                .joinToString("")
                                .ifEmpty { e.name.take(2).uppercase() },
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
                // Animated status indicator with pulsing effect
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .align(Alignment.BottomEnd)
                        .offset(x = 2.dp, y = 2.dp)
                        .clip(CircleShape)
                        .background(Color.White),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(visual.color),
                    )
                }
            }
            Spacer(Modifier.width(12.dp))

            // Center: name + meta with enhanced typography
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text       = e.name,
                    color      = InkPrimary,
                    fontSize   = 16.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(4.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text     = "#${employeeIdPretty(e.id)}",
                        color    = Highlight,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        softWrap = false,
                    )
                    if (e.department.isNotBlank()) {
                        Text(
                            text     = "·",
                            color    = InkMuted,
                            fontSize = 11.sp,
                        )
                        Text(
                            text     = e.department,
                            color    = Color(0xFF1A6BF5),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false),
                        )
                    }
                }
                if (e.zone.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "📍",
                            fontSize = 10.sp,
                        )
                        Text(
                            text = e.zone,
                            color = InkMuted,
                            fontSize = 10.sp,
                        )
                        if (e.tasksOpen > 0) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color(0xFFFFE5E5))
                                    .padding(horizontal = 6.dp, vertical = 2.dp),
                            ) {
                                Text(
                                    text = "${e.tasksOpen} task${if (e.tasksOpen > 1) "s" else ""}",
                                    color = Danger,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.SemiBold,
                                )
                            }
                        }
                    }
                }
            }

            // Right: premium status badge
            Spacer(Modifier.width(8.dp))
            Column(
                horizontalAlignment = Alignment.End,
                modifier = Modifier.widthIn(min = 70.dp),
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(visual.bg)
                        .border(1.dp, visual.color.copy(alpha = 0.3f), RoundedCornerShape(999.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        // Animated pulsing dot
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(visual.color),
                        )
                        Text(
                            text       = visual.label,
                            color      = visual.color,
                            fontSize   = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                visual.secondary?.let {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = it,
                        color = InkMuted,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

/** Make the auto-id Firestore key look like the EMP-1042 style in the design. */
private fun employeeIdPretty(id: String): String {
    val digits = id.filter(Char::isDigit).take(4)
    return if (digits.length >= 3) "EMP-$digits"
           else "EMP-${(id.hashCode() and 0xFFF).toString().padStart(4, '0')}"
}

/* ──────────────────────────────────────────────────────────────────────────
 *  Inline empty/loading block (replaces the global EmptyState helper here so
 *  it can sit beneath the gradient header inside the same scroll container).
 * ────────────────────────────────────────────────────────────────────────── */
@Composable
private fun InfoBlock(emoji: String, title: String, body: String) {
    // Premium empty state with entrance animation
    val alphaAnim by rememberInfiniteTransition().animateFloat(
        initialValue = 0.6f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "empty-state-alpha"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 60.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Animated emoji with scale
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF1A6BF5).copy(alpha = 0.1f),
                            Color.Transparent
                        )
                    )
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                emoji,
                fontSize = 48.sp,
                modifier = Modifier.scale(alphaAnim)
            )
        }

        Text(
            title,
            color = InkPrimary,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp,
        )
        Text(
            body,
            color = InkSecondary,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            lineHeight = 1.5.em,
        )
    }
}

/* ──────────────────────────────────────────────────────────────────────────
 *  Add Employee bottom sheet — full HR onboarding form, matches Figma:
 *    • Gradient header
 *    • Profile upload card
 *    • Personal Information section
 *    • Employment section (incl. 3-card permission selector)
 *    • Emergency Contact section
 *    • Sticky bottom action bar (Cancel / Add employee)
 * ────────────────────────────────────────────────────────────────────────── */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AddEmployeeSheet(
    saving: Boolean,
    errorMsg: String?,
    createdName: String?,
    createdEmail: String?,
    createdPassword: String?,
    createdPhone: String?,
    onDoneSharing: () -> Unit,
    onCancel: () -> Unit,
    onSubmit: (EmployeeDraft) -> Unit,
    initial: EmployeeRecord? = null,
    modifier: Modifier = Modifier,
) {
    val isEdit = initial != null
    // Tint the system status bar to match the gradient header (matches the
    // Task creation screen behaviour). Without this, white icons would be
    // invisible on a light status bar background on some devices.
    com.example.uniwattelektrik.core.theme.SetStatusBar(color = Highlight, darkIcons = false)

    /* ── Personal ───────────────────────────────────────────────────── */
    var name by remember(initial) { mutableStateOf(initial?.name ?: "") }
    var dobMs by remember(initial) { mutableStateOf<Long?>(initial?.dateOfBirthMs) }
    var gender by remember(initial) { mutableStateOf(initial?.gender?.takeIf { it.isNotBlank() } ?: "Male") }
    var phone by remember(initial) { mutableStateOf(initial?.phone ?: "") }
    var email by remember(initial) { mutableStateOf(initial?.email ?: "") }
    var password by remember(initial) { mutableStateOf("") }
    var address by remember(initial) { mutableStateOf(initial?.address ?: "") }

    /* ── Employment ─────────────────────────────────────────────────── */
    var role by remember(initial) { mutableStateOf(initial?.role?.takeIf { it.isNotBlank() } ?: "Substation Engineer · L1") }
    var department by remember(initial) { mutableStateOf(initial?.department ?: "") }
    var reportingTo by remember(initial) { mutableStateOf(initial?.reportingTo ?: "") }
    var joiningMs by remember(initial) { mutableStateOf<Long?>(initial?.joiningDateMs) }
    var empType by remember(initial) { mutableStateOf(initial?.employmentType?.takeIf { it.isNotBlank() } ?: "Fulltime") }
    var salaryStr by remember(initial) {
        mutableStateOf(
            initial?.salary?.takeIf { it > 0 }?.let {
                if (it == it.toLong().toDouble()) it.toLong().toString() else it.toString()
            } ?: ""
        )
    }
    var zone by remember(initial) { mutableStateOf(initial?.zone?.takeIf { it != "—" } ?: "") }
    var status by remember(initial) { mutableStateOf(initial?.status?.takeIf { it.isNotBlank() } ?: "Active") }
    var permission by remember(initial) { mutableStateOf(initial?.permission?.takeIf { it.isNotBlank() } ?: "Field") }   // Viewer | Field | Super
    var shift by remember(initial) { mutableStateOf(initial?.shift?.takeIf { it.isNotBlank() } ?: "Shift1") }       // Shift1 | Shift2

    /* ── Emergency contact ──────────────────────────────────────────── */
    var emergencyName by remember(initial) { mutableStateOf(initial?.emergencyName ?: "") }
    var emergencyRelation by remember(initial) { mutableStateOf(initial?.emergencyRelation ?: "") }
    var emergencyPhone by remember(initial) { mutableStateOf(initial?.emergencyPhone ?: "") }

    /* ── Date pickers ───────────────────────────────────────────────── */
    var showJoiningDate by remember { mutableStateOf(false) }
    var showDobDate by remember { mutableStateOf(false) }

    // Photo picker (Android only; no-op iOS)
    val photoPicker = rememberPhotoPicker()
    val photoUri = photoPicker.uri

    // Validation: errors are only shown after the user taps "Add Employee"
    // (so the user isn't yelled at while still typing).
    var submitted by remember { mutableStateOf(false) }

    // System back button on Android cleanly cancels the form.
    PlatformBackHandler(enabled = !saving) { onCancel() }

    // Haptics for high-signal interactions.
    val haptics = LocalHapticFeedback.current
    val emailLooksValid = email.contains("@") && email.contains(".") &&
            email.length >= 5

    // Phone validation:
    //   • 10–15 digits after stripping spaces / dashes / parens
    //   • Indian mobile numbers (10-digit form OR +91 prefix) must start with 6-9
    //   • Reject obvious junk like "0000000000" or all-same digits
    val phoneDigits  = phone.filter { it.isDigit() }
    val phoneLooksValid = run {
        if (phoneDigits.length !in 10..15) return@run false
        if (phoneDigits.toSet().size == 1) return@run false  // 0000000, 1111111…
        // Compute the "subscriber" part for an Indian-style number: drop the
        // optional 91 country-code prefix to get the 10-digit subscriber.
        val subscriber = when {
            phoneDigits.length == 12 && phoneDigits.startsWith("91") -> phoneDigits.drop(2)
            phoneDigits.length == 11 && phoneDigits.startsWith("0")  -> phoneDigits.drop(1)
            else                                                     -> phoneDigits
        }
        if (subscriber.length == 10 && subscriber.first() !in '6'..'9') return@run false
        true
    }

    // DOB validation:
    //   • required
    //   • not in the future
    //   • employee at least 18 years old
    //   • not absurdly old (sanity floor: 1900)
    val nowMs = com.example.uniwattelektrik.platform.nowEpochMillis()
    val approxYearMs = 365L * 24L * 60L * 60L * 1000L
    val dobAgeYears  = dobMs?.let { ((nowMs - it).toDouble() / approxYearMs).toInt() }
    val dobLooksValid = when {
        dobMs == null                 -> false
        dobMs!! >= nowMs              -> false   // future date
        dobAgeYears == null           -> false
        dobAgeYears < 18              -> false
        dobAgeYears > 100             -> false
        else                          -> true
    }

    val nameError = if (submitted && name.isBlank()) "Name is required" else null
    val emailError = when {
        submitted && email.isBlank() -> "Email is required"
        submitted && !emailLooksValid -> "Enter a valid email address"
        else -> null
    }
    val passwordError = when {
        isEdit                          -> null   // password is not editable in edit mode
        submitted && password.isBlank() -> "Password is required"
        submitted && password.length < 6 -> "Min 6 characters"
        else -> null
    }
    val phoneError = when {
        submitted && phone.isBlank()                  -> "Phone number is required"
        submitted && phoneDigits.length !in 10..15    -> "Enter 10–15 digits"
        submitted && phoneDigits.toSet().size == 1    -> "Phone number looks invalid"
        submitted && !phoneLooksValid                 -> "Enter a valid phone number"
        else -> null
    }
    val dobError = when {
        submitted && dobMs == null    -> "Date of birth is required"
        submitted && dobMs!! >= nowMs -> "Date of birth can't be in the future"
        submitted && dobAgeYears != null && dobAgeYears < 18 -> "Employee must be 18 or older"
        submitted && dobAgeYears != null && dobAgeYears > 100 -> "Please re-enter date of birth"
        else -> null
    }
    val salaryError = if (submitted && salaryStr.toDoubleOrNull() == null)
        "Enter a valid salary" else null

    val canSubmit =
        name.isNotBlank() &&
                phoneLooksValid &&
                email.isNotBlank() && emailLooksValid &&
                dobLooksValid &&
                (isEdit || password.length >= 6) &&
                salaryStr.toDoubleOrNull() != null &&
                !saving

    val scrollState = rememberScrollState()
    val navInsets = WindowInsets.navigationBars.asPaddingValues()
    val actionBarHeight = 88.dp

    /* Submit lambda extracted so the header ✓-button and the bottom CTA
       both call the SAME validation + submit path — they can never drift. */
    val submitForm: () -> Unit = {
        submitted = true
        if (canSubmit) {
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            onSubmit(
                EmployeeDraft(
                    name = name.trim(),
                    role = role.trim(),
                    email = email.trim(),
                    password = password,
                    phone = phone.trim(),
                    gender = gender,
                    employmentType = empType,
                    joiningDateMs = joiningMs,
                    salary = salaryStr.toDoubleOrNull() ?: 0.0,
                    zone = zone.trim(),
                    status = status,
                    photoUri = photoUri,
                    dateOfBirthMs = dobMs,
                    address = address.trim(),
                    department = department.trim(),
                    reportingTo = reportingTo.trim(),
                    permission = permission,
                    emergencyName = emergencyName.trim(),
                    emergencyRelation = emergencyRelation.trim(),
                    emergencyPhone = emergencyPhone.trim(),
                    shift = shift,
                )
            )
        }
    }

    // After a successful create, swap the form for a "Share via WhatsApp"
    // success card so the admin can hand the temp creds to the employee
    // before closing the sheet. Only relevant for the create flow.
    if (!isEdit && createdEmail != null && createdPassword != null) {
        ShareCredentialsScreen(
            name = createdName.orEmpty(),
            email = createdEmail,
            password = createdPassword,
            phone = createdPhone.orEmpty(),
            onDone = onDoneSharing,
        )
        return
    }

    Box(modifier = modifier.fillMaxSize().background(appScreenBackground())) {
        Column(modifier = Modifier.fillMaxSize()) {
            // 1. Sticky gradient header — NEVER scrolls
            AddEmployeeHeader(
                onClose     = onCancel,
                onSave      = submitForm,
                saveEnabled = canSubmit,
                saving      = saving,
                title       = if (isEdit) "Edit Employee" else "Add Employee",
                subtitle    = if (isEdit) "UPDATE EMPLOYEE PROFILE" else "NEW HIRE ONBOARDING",
            )

            /* ── Scrollable content ─────────────────────────────────────── */
            Column(
                modifier = Modifier
                    .weight(1f)  // Takes remaining space above the action bar
                    .imePadding() // Push content above the keyboard so the focused field is visible.
                    .verticalScroll(scrollState)
                    .padding(bottom = actionBarHeight + 32.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                // 2. Profile upload card
                Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                    ProfileUploadCard(
                        photoUri = photoUri,
                        onPick = { photoPicker.pick() },
                        enabled = !saving,
                    )
                }

                // 3. Personal Information
                FormSection(
                    index = 0,
                    icon  = Icons.Outlined.Person,
                    title = "Personal Information",
                    tint  = Highlight,
                    bg    = Color(0xFFE6F0FF),
                ) {
                    Column {
                        FieldLabel("Full name", required = true)
                        DottedField(
                            value = name, onChange = { name = it },
                            placeholder = "e.g. Ravi Kumar",
                            leadingIcon = Icons.Outlined.Person,
                            leadingTint = Highlight, leadingBg = Color(0xFFE6F0FF),
                            imeAction = ImeAction.Next,
                            enabled = !saving,
                            isError = nameError != null,
                            errorText = nameError,
                            semanticsLabel = "employee-full-name",
                        )
                    }
                    Column {
                        FieldLabel("Date of birth")
                        DottedClickable(
                            value = dobMs?.let(::formatDate) ?: "Tap to pick a date",
                            muted = dobMs == null,
                            leadingIcon = Icons.Outlined.Cake,
                            leadingTint = Highlight, leadingBg = Color(0xFFE6F0FF),
                            enabled = !saving,
                            onClick = { showDobDate = true },
                        )
                        if (dobError != null) {
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text       = dobError,
                                color      = AppTheme.Danger,
                                fontSize   = 11.sp,
                                fontWeight = FontWeight.Medium,
                            )
                        }
                    }
                    Column {
                        FieldLabel("Gender")
                        GenderSegmented(
                            value = gender,
                            onChange = {
                                gender = it
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            },
                            enabled = !saving,
                        )
                    }
                    Column {
                        FieldLabel("Phone", required = true)
                        DottedField(
                            value = phone, onChange = { phone = it },
                            placeholder = "10-digit mobile",
                            leadingIcon = Icons.Outlined.Phone,
                            leadingTint = Highlight, leadingBg = Color(0xFFE6F0FF),
                            keyboard = KeyboardType.Phone,
                            imeAction = ImeAction.Next,
                            enabled = !saving,
                            isError = phoneError != null,
                            errorText = phoneError,
                            semanticsLabel = "employee-phone-number",
                        )
                    }
                    Column {
                        FieldLabel("Email", required = true)
                        DottedField(
                            value = email, onChange = { email = it },
                            placeholder = "name@company.com",
                            leadingIcon = Icons.Outlined.Email,
                            leadingTint = Highlight, leadingBg = Color(0xFFE6F0FF),
                            keyboard = KeyboardType.Email,
                            imeAction = ImeAction.Next,
                            enabled = !saving,
                            isError = emailError != null,
                            errorText = emailError,
                            semanticsLabel = "employee-email-address",
                        )
                    }
                    if (!isEdit) Column {
                        FieldLabel("Temporary password", required = true)
                        DottedField(
                            value = password, onChange = { password = it },
                            placeholder = "min 6 chars — share with employee",
                            leadingIcon = Icons.Outlined.Key,
                            leadingTint = Highlight, leadingBg = Color(0xFFE6F0FF),
                            imeAction = ImeAction.Next,
                            enabled = !saving,
                            isError = passwordError != null,
                            errorText = passwordError,
                            semanticsLabel = "employee-new-password",
                        )
                        Spacer(Modifier.height(8.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFFEEF2FF))
                                    .clickable(enabled = !saving) {
                                        password = generateTempPassword()
                                    }
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Refresh,
                                    contentDescription = "Generate password",
                                    tint = Highlight,
                                    modifier = Modifier.size(14.dp),
                                )
                                Text(
                                    "Generate", color = Highlight, fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            if (password.isNotBlank()) {
                                CopyPasswordChip(password = password, enabled = !saving)
                            }
                        }
                    }
                    Column {
                        FieldLabel("Address")
                        DottedField(
                            value = address, onChange = { address = it },
                            placeholder = "House, street, city",
                            leadingIcon = Icons.Outlined.Home,
                            leadingTint = Highlight, leadingBg = Color(0xFFE6F0FF),
                            imeAction = ImeAction.Next,
                            enabled = !saving,
                        )
                    }
                }

                // 4. Employment
                FormSection(
                    index = 1,
                    icon  = Icons.Outlined.Work,
                    title = "Employment",
                    tint  = Success,
                    bg    = SuccessBg,
                ) {
                    Column {
                        FieldLabel("Employee ID")
                        DottedReadOnly(
                            value = "Auto-generated on save",
                            leadingIcon = Icons.Outlined.Badge,
                            leadingTint = Success, leadingBg = SuccessBg,
                            hint = "auto",
                        )
                    }
                    Column {
                        FieldLabel("Joining date")
                        DottedClickable(
                            value = joiningMs?.let(::formatDate) ?: "Tap to pick a date",
                            muted = joiningMs == null,
                            leadingIcon = Icons.Outlined.CalendarMonth,
                            leadingTint = Success, leadingBg = SuccessBg,
                            enabled = !saving,
                            onClick = { showJoiningDate = true },
                        )
                    }
                    Column {
                        FieldLabel("Designation")
                        DottedField(
                            value = role, onChange = { role = it },
                            placeholder = "e.g. Substation Engineer · L1",
                            leadingIcon = Icons.Outlined.Work,
                            leadingTint = Success, leadingBg = SuccessBg,
                            imeAction = ImeAction.Next,
                            enabled = !saving,
                        )
                    }
                    Column {
                        FieldLabel("Department")
                        DottedField(
                            value = department, onChange = { department = it },
                            placeholder = "Operations / Maintenance / …",
                            leadingIcon = Icons.Outlined.Apartment,
                            leadingTint = Success, leadingBg = SuccessBg,
                            imeAction = ImeAction.Next,
                            enabled = !saving,
                        )
                    }
                    Column {
                        FieldLabel("Reporting to")
                        DottedField(
                            value = reportingTo, onChange = { reportingTo = it },
                            placeholder = "Manager / Supervisor name",
                            leadingIcon = Icons.Outlined.Person,
                            leadingTint = Success, leadingBg = SuccessBg,
                            imeAction = ImeAction.Next,
                            enabled = !saving,
                        )
                    }
                    Column {
                        FieldLabel("Employment type")
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            StatusPickChip("Full-time", empType == "Fulltime", { empType = "Fulltime" }, Success, SuccessBg)
                            StatusPickChip("Contract", empType == "Contract", { empType = "Contract" }, Highlight, Color(0xFFE6F0FF))
                        }
                    }
                    Column {
                        FieldLabel("Salary (monthly, ₹)")
                        DottedField(
                            value = salaryStr,
                            onChange = { salaryStr = it.filter { c -> c.isDigit() || c == '.' } },
                            placeholder = "e.g. 38500",
                            leadingIcon = Icons.Outlined.Payments,
                            leadingTint = Success, leadingBg = SuccessBg,
                            keyboard = KeyboardType.Decimal,
                            imeAction = ImeAction.Done,
                            enabled = !saving,
                            isError = salaryError != null,
                            errorText = salaryError,
                        )
                    }
                    Column {
                        FieldLabel("Zone (optional)")
                        DottedField(
                            value = zone, onChange = { zone = it },
                            placeholder = "e.g. HSR / Whitefield",
                            leadingIcon = Icons.Outlined.Place,
                            leadingTint = Success, leadingBg = SuccessBg,
                            imeAction = ImeAction.Next,
                            enabled = !saving,
                        )
                    }
                    Column {
                        FieldLabel("Initial status")
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            StatusPickChip("Active", status == "Active", { status = "Active" }, Success, SuccessBg)
                            StatusPickChip("On leave", status == "OnLeave", { status = "OnLeave" }, Leave, LeaveBg)
                            StatusPickChip("Off-duty", status == "Inactive", { status = "Inactive" }, OffDuty, OffDutyBg)
                        }
                    }

                    // Role / Permission card selector — 3 big cards
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "ROLE / PERMISSION",
                        color = InkSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        PermissionCard(
                            emoji = "👁",
                            label = "VIEWER",
                            sub = "Read-only",
                            tint = Success,
                            bg = SuccessBg,
                            selected = permission == "Viewer",
                            onClick = { permission = "Viewer" },
                            modifier = Modifier.weight(1f),
                        )
                        PermissionCard(
                            emoji = "🔧",
                            label = "FIELD",
                            sub = "Standard",
                            tint = Color(0xFFF59E0B),
                            bg = Color(0xFFFFF6E6),
                            selected = permission == "Field",
                            onClick = { permission = "Field" },
                            modifier = Modifier.weight(1f),
                        )
                        PermissionCard(
                            emoji = "👑",
                            label = "SUPER",
                            sub = "Full access",
                            tint = Danger,
                            bg = Color(0xFFFFEBEB),
                            selected = permission == "Super",
                            onClick = { permission = "Super" },
                            modifier = Modifier.weight(1f),
                        )
                    }

                    // Shift selector — 2 cards
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "WORK SHIFT",
                        color = InkSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        PermissionCard(
                            emoji = "☀️",
                            label = "SHIFT 1",
                            sub = "09:00 – 18:00",
                            tint = Color(0xFF1E73E8),
                            bg = Color(0xFFE6F0FE),
                            selected = shift == "Shift1",
                            onClick = { shift = "Shift1" },
                            modifier = Modifier.weight(1f),
                        )
                        PermissionCard(
                            emoji = "🌙",
                            label = "SHIFT 2",
                            sub = "13:00 – 23:00",
                            tint = Color(0xFF1A6BF5),
                            bg = Color(0xFFF3EAFE),
                            selected = shift == "Shift2",
                            onClick = { shift = "Shift2" },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }

                // 5. Emergency Contact
                FormSection(
                    index = 2,
                    icon  = Icons.Outlined.Favorite,
                    title = "Emergency Contact",
                    tint  = Danger,
                    bg    = Color(0xFFFEE2E2),
                ) {
                    Column {
                        FieldLabel("Name")
                        DottedField(
                            value = emergencyName, onChange = { emergencyName = it },
                            placeholder = "Contact's full name",
                            leadingIcon = Icons.Outlined.Person,
                            leadingTint = Danger, leadingBg = Color(0xFFFEE2E2),
                            imeAction = ImeAction.Next,
                            enabled = !saving,
                        )
                    }
                    Column {
                        FieldLabel("Relation")
                        DottedField(
                            value = emergencyRelation, onChange = { emergencyRelation = it },
                            placeholder = "e.g. Spouse / Parent",
                            leadingIcon = Icons.Outlined.Favorite,
                            leadingTint = Danger, leadingBg = Color(0xFFFEE2E2),
                            imeAction = ImeAction.Next,
                            enabled = !saving,
                        )
                    }
                    Column {
                        FieldLabel("Contact number")
                        DottedField(
                            value = emergencyPhone, onChange = { emergencyPhone = it },
                            placeholder = "10-digit mobile",
                            leadingIcon = Icons.Outlined.Phone,
                            leadingTint = Danger, leadingBg = Color(0xFFFEE2E2),
                            keyboard = KeyboardType.Phone,
                            imeAction = ImeAction.Done,
                            enabled = !saving,
                        )
                    }
                }

                if (saving) {
                    Row(
                        modifier = Modifier.padding(horizontal = 20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        Text("Saving to Firestore…", fontSize = 12.sp, color = InkSecondary)
                    }
                }
                errorMsg?.let {
                    Text(
                        text = "⚠ $it", color = Danger, fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 20.dp),
                    )
                }
            }

            /* ── Sticky bottom action bar ───────────────────────────────── */
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CardBg)
                    .shadow(elevation = 18.dp, spotColor = ShadowSoft)
                    .background(CardBg)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .padding(bottom = navInsets.calculateBottomPadding()),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(ChipBg)
                        .clickable(enabled = !saving, onClick = onCancel),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "Cancel", color = InkSecondary, fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Box(
                    modifier = Modifier
                        .weight(1.3f)
                        .height(52.dp)
                        .shadow(
                            elevation = if (canSubmit) 16.dp else 0.dp,
                            shape = RoundedCornerShape(16.dp),
                            spotColor = Highlight.copy(alpha = 0.5f),
                        )
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            if (canSubmit) Brush.horizontalGradient(listOf(GradStart, GradEnd))
                            else Brush.horizontalGradient(listOf(InkMuted, InkMuted)),
                        )
                        .clickable(enabled = !saving, onClick = submitForm),
                    contentAlignment = Alignment.Center,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = if (saving) "Saving…" else if (isEdit) "Save changes" else "Add employee",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        if (!saving) Text(
                            "→", color = Color.White, fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        /* ── Date picker dialogs ────────────────────────────────────────── */
        if (showJoiningDate) {
            val s = rememberDatePickerState(initialSelectedDateMillis = joiningMs)
            DatePickerDialog(
                onDismissRequest = { showJoiningDate = false },
                confirmButton = {
                    TextButton(onClick = {
                        joiningMs = s.selectedDateMillis
                        showJoiningDate = false
                    }) { Text("OK") }
                },
                dismissButton = {
                    TextButton(onClick = { showJoiningDate = false }) { Text("Cancel") }
                },
            ) { DatePicker(state = s) }
        }
        if (showDobDate) {
            val s = rememberDatePickerState(initialSelectedDateMillis = dobMs)
            DatePickerDialog(
                onDismissRequest = { showDobDate = false },
                confirmButton = {
                    TextButton(onClick = {
                        dobMs = s.selectedDateMillis
                        showDobDate = false
                    }) { Text("OK") }
                },
                dismissButton = {
                    TextButton(onClick = { showDobDate = false }) { Text("Cancel") }
                },
            ) { DatePicker(state = s) }
        }
    }
}

    /* ──────────────────────────────────────────────────────────────────────────
 *  Add-employee form sub-components
 * ────────────────────────────────────────────────────────────────────────── */

/** Gradient header at the top of the sheet: back / title / done. */
/**
 * Sticky gradient header for the Add Employee flow.
 *
 *  - Left  : translucent rounded-square back button (←)
 *  - Centre: "Add Employee" + "NEW HIRE ONBOARDING" caps subtitle
 *  - Right : vivid green rounded-square save button (✓), enabled-state aware
 *  - Below : breadcrumb pill — "← From · Employee Management"
 *
 * The save button mirrors the bottom-CTA action — tapping either submits
 * the form. When [saveEnabled] is false (validation pending or saving),
 * the green tile dims and the click is no-op.
 */
@Composable
fun AddEmployeeHeader(
    onClose: () -> Unit,
    onSave: () -> Unit = {},
    saveEnabled: Boolean = true,
    saving: Boolean = false,
    title: String = "Add Employee",
    subtitle: String = "NEW HIRE ONBOARDING",
) {
    com.example.uniwattelektrik.core.components.OperationsHeader(
        eyebrow  = subtitle,
        title    = title,
        subtitle = "From · Employee Management",
        onBack   = onClose,
        actions  = {
            // Save (✓) button — vivid green, dims when disabled.
            val tileBg     = if (saveEnabled) Success else Success.copy(alpha = 0.45f)
            val tileShadow = if (saveEnabled) Color(0x55119F4A) else Color(0x00000000)
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .shadow(
                        elevation = if (saveEnabled) 8.dp else 0.dp,
                        shape     = RoundedCornerShape(12.dp),
                        spotColor = tileShadow,
                    )
                    .clip(RoundedCornerShape(12.dp))
                    .background(tileBg)
                    .clickable(enabled = saveEnabled && !saving, onClick = onSave),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector        = Icons.Filled.Check,
                    contentDescription = "Save",
                    tint               = Color.White,
                    modifier           = Modifier.size(20.dp),
                )
            }
        },
    )
}

/**
 * Profile photo upload card — matches the Figma exactly.
 *
 *  - Card    : soft white→ice-blue diagonal gradient with rounded corners
 *  - Tile    : 84 dp blue gradient square; picked image fills it via crop;
 *              small white camera badge in the bottom-right corner
 *  - Texts   : "Profile photo" + multi-clue subtitle
 *  - CTA     : solid blue gradient pill, "UPLOAD" / "REPLACE" in white
 */
@Composable
fun ProfileUploadCard(photoUri: String?, onPick: () -> Unit, enabled: Boolean) {
    // Soft gradient that matches the reference card background — white at the
    // top-left fading to a very light blue at the bottom-right.
    val cardGradient = Brush.linearGradient(
        colors = listOf(Color(0xFFFFFFFF), Color(0xFFEEF4FF)),
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(elevation = 10.dp, shape = RoundedCornerShape(22.dp), spotColor = ShadowSoft)
            .clip(RoundedCornerShape(22.dp))
            .background(cardGradient)
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {

            /* ── Photo tile ─────────────────────────────────────────────── */
            Box(
                modifier = Modifier
                    .size(84.dp)
                    .clickable(enabled = enabled, onClick = onPick),
            ) {
                // Main rounded square — gradient bg or filled image
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .shadow(8.dp, RoundedCornerShape(20.dp), spotColor = Color(0x33007BFF))
                        .clip(RoundedCornerShape(20.dp))
                        .background(Brush.linearGradient(listOf(GradStart, GradEnd))),
                    contentAlignment = Alignment.Center,
                ) {
                    if (photoUri != null) {
                        // Fills the tile completely (fitXY-equivalent in Compose).
                        AsyncImage(
                            model              = photoUri,
                            contentDescription = "Profile photo",
                            contentScale       = ContentScale.Crop,
                            modifier           = Modifier
                                .matchParentSize()
                                .clip(RoundedCornerShape(20.dp)),
                        )
                    } else {
                        Text(
                            "+",
                            color      = Color.White,
                            fontSize   = 36.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }

                // Small white camera badge in the bottom-right corner — visible
                // even when a photo is picked, so the user knows to tap-to-replace.
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .align(Alignment.BottomEnd)
                        .offset(x = 4.dp, y = 4.dp)
                        .shadow(4.dp, RoundedCornerShape(8.dp), spotColor = Color(0x40000000))
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.White),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("📷", fontSize = 13.sp)
                }
            }

            Spacer(Modifier.width(16.dp))

            /* ── Title + subtitle ───────────────────────────────────────── */
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Profile photo",
                    color      = InkPrimary,
                    fontSize   = 15.sp,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text     = if (photoUri != null)
                        "Tap thumbnail to replace · square crop preferred · max 5 MB"
                    else
                        "Upload a clear face photo · square crop preferred · max 5 MB",
                    color    = InkSecondary,
                    fontSize = 12.sp,
                    lineHeight = 17.sp,
                )
            }

            Spacer(Modifier.width(12.dp))

            /* ── Upload / Replace CTA ───────────────────────────────────── */
            Box(
                modifier = Modifier
                    .shadow(
                        elevation = 8.dp,
                        shape     = RoundedCornerShape(20),
                        spotColor = Color(0x55007BFF),
                    )
                    .clip(RoundedCornerShape(20))
                    .background(Brush.horizontalGradient(listOf(GradStart, GradEnd)))
                    .clickable(enabled = enabled, onClick = onPick)
                    .padding(horizontal = 18.dp, vertical = 5.dp),
            ) {
                Text(
                    text          = if (photoUri != null) "REPLACE" else "UPLOAD",
                    color         = Color.White,
                    fontSize      = 11.sp,
                    fontWeight    = FontWeight.Bold,
                    letterSpacing = 1.4.sp,
                )
            }
        }
    }
}

    /**
     * Section card — matches the Task creation screen's accordion style.
     *
     *  • SECTION N caps caption above a bold title
     *  • 36 dp colored rounded-square icon tile (tint over light bg)
     *  • Hairline divider between header and fields
     *
     * Sections in the Add-Employee flow are always expanded — the
     * collapse/expand chevron from NewTaskScreen is intentionally omitted
     * so admins never have to chase hidden fields during onboarding.
     */
    @Composable
    fun FormSection(
        index: Int,
        icon: ImageVector,
        title: String,
        tint: Color,
        bg: Color,
        content: @Composable ColumnScope.() -> Unit,
    ) {
        Box(modifier = Modifier.padding(horizontal = 16.dp)) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(elevation = 10.dp, shape = RoundedCornerShape(20.dp), spotColor = ShadowSoft)
                    .clip(RoundedCornerShape(20.dp))
                    .background(CardBg)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                /* Header row — colored icon tile + caps caption + title */
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(bg),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = tint,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "SECTION ${index + 1}",
                            color = InkMuted,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.0.sp,
                        )
                        Text(
                            title,
                            color = InkPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }

                /* Hairline divider */
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(Color(0xFFE2E8F0)),
                )

                /* Fields */
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    content()
                }
            }
        }
    }

    /**
     * Premium fintech-style field row.
     *
     * Layout: 36 dp circular emoji icon · 12 sp label on top · 16 sp input below.
     * Idle state    : soft `#F1F5F9` card with a 4 dp drop shadow.
     * Focus state   : white background, 2 dp `#2979FF` border, icon tint flips
     *                 to highlight blue, shadow lifts to 8 dp. Animated 150 ms.
     * Error state   : 2 dp `#EF4444` border + error text below (overrides focus).
     * Success state : 2 dp `#22C55E` border + tiny ✓ next to the label.
     *
     * The whole card listens to descendant focus via [onFocusEvent], so the
     * upgrade applies to every input that lives inside it (text, phone, etc.)
     * without each child having to forward focus state.
     */
    @Composable
    fun FieldRow(
        icon: ImageVector,
        label: String,
        required: Boolean = false,
        onClick: (() -> Unit)? = null,
        errorText: String? = null,
        isSuccess: Boolean = false,
        trailing: @Composable (() -> Unit)? = null,
        content: @Composable () -> Unit,
    ) {
        val shape = RoundedCornerShape(16.dp)
        var focused by remember { mutableStateOf(false) }
        val isError = errorText != null

        val borderColor by animateColorAsState(
            targetValue = when {
                isError -> Danger
                isSuccess -> Success
                focused -> Highlight
                else -> Color.Transparent
            },
            animationSpec = tween(durationMillis = 150),
            label = "field-border",
        )
        val bg by animateColorAsState(
            targetValue = if (focused && !isError) Color.White else Color(0xFFF1F5F9),
            animationSpec = tween(durationMillis = 150),
            label = "field-bg",
        )
        val iconBg by animateColorAsState(
            targetValue = when {
                isError -> Color(0xFFFEE2E2)
                isSuccess -> Color(0xFFDCFCE7)
                focused -> Color(0xFFE0EDFF)
                else -> Color(0xFFE2E8F0)
            },
            animationSpec = tween(durationMillis = 150),
            label = "field-icon-bg",
        )
        val iconTint by animateColorAsState(
            targetValue = when {
                isError -> Danger
                isSuccess -> Success
                focused -> Highlight
                else -> InkSecondary
            },
            animationSpec = tween(durationMillis = 150),
            label = "field-icon-tint",
        )
        val labelColor by animateColorAsState(
            targetValue = when {
                isError -> Danger
                isSuccess -> Success
                focused -> Highlight
                else -> InkSecondary
            },
            animationSpec = tween(durationMillis = 150),
            label = "field-label",
        )
        val shadowElev by animateDpAsState(
            targetValue = if (focused && !isError) 8.dp else 4.dp,
            animationSpec = tween(durationMillis = 150),
            label = "field-shadow",
        )

        Column(modifier = Modifier.fillMaxWidth()) {
            val rowMod = Modifier
                .fillMaxWidth()
                .shadow(
                    elevation = shadowElev,
                    shape = shape,
                    ambientColor = Color.Transparent,
                    spotColor = Color(0x14000000),
                )
                .clip(shape)
                .background(bg)
                .border(width = 2.dp, color = borderColor, shape = shape)
                .onFocusEvent { focused = it.hasFocus }
                .let { if (onClick != null) it.clickable(onClick = onClick) else it }
                .padding(16.dp)

            Row(modifier = rowMod, verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(iconBg),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = label,
                        tint = iconTint,
                        modifier = Modifier.size(18.dp),
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = label,
                            color = labelColor,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                        )
                        if (required) {
                            Spacer(Modifier.width(6.dp))
                            // Subtle red dot replaces the old asterisk — modern fintech feel.
                            Box(
                                modifier = Modifier
                                    .size(5.dp)
                                    .clip(CircleShape)
                                    .background(Danger),
                            )
                        }
                        if (isSuccess) {
                            Spacer(Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .size(5.dp)
                                    .clip(CircleShape)
                                    .background(Success),
                            )
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    content()
                }
                if (trailing != null) {
                    Spacer(Modifier.width(8.dp))
                    trailing()
                }
            }
            // Inline error text — appears beneath the card, scoped to it.
            if (errorText != null) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = errorText,
                    color = Danger,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(start = 4.dp),
                )
            }
        }
    }

    /**
     * Premium 3-segment gender selector.
     *
     * Selected pill: `#E0EDFF` bg, `#2979FF` text, soft shadow.
     * Unselected   : transparent bg, `#64748B` text.
     * Outer track  : `#F1F5F9` with 14 dp radius.
     * The selection slides smoothly via `animateColorAsState`.
     */
    @Composable
    fun GenderSegmented(
        value: String,
        onChange: (String) -> Unit,
        options: List<String> = listOf("Male", "Female", "Other"),
        enabled: Boolean = true,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0xFFF1F5F9))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            options.forEach { option ->
                val selected = option == value
                val pillBg by animateColorAsState(
                    targetValue = if (selected) Color(0xFFE0EDFF) else Color.Transparent,
                    animationSpec = tween(durationMillis = 150),
                    label = "gender-bg-$option",
                )
                val txt by animateColorAsState(
                    targetValue = if (selected) Highlight else InkSecondary,
                    animationSpec = tween(durationMillis = 150),
                    label = "gender-fg-$option",
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(pillBg)
                        .clickable(enabled = enabled) { onChange(option) },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = option,
                        color = txt,
                        fontSize = 14.sp,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                    )
                }
            }
        }
    }

    /**
     * Borderless single-line field used inside [FieldRow]. Looks like a normal
     * text in the row — no Material outline, just a placeholder when empty.
     */
    @Composable fun UnderlinedField(
        value: String,
        onChange: (String) -> Unit,
        placeholder: String,
        enabled: Boolean = true,
        keyboardType: KeyboardType = KeyboardType.Text,
        imeAction: ImeAction = ImeAction.Next,
        onDone: (() -> Unit)? = null,
    ) {
        val focusManager = LocalFocusManager.current
        val keyboardController = LocalSoftwareKeyboardController.current
        Box(modifier = Modifier.fillMaxWidth()) {
            if (value.isEmpty()) {
                Text(placeholder, color = InkMuted, fontSize = 16.sp)
            }
            BasicTextField(
                value = value,
                onValueChange = onChange,
                singleLine = true,
                enabled = enabled,
                modifier = Modifier.fillMaxWidth(),
                textStyle = TextStyle(
                    color = InkPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                ),
                cursorBrush = SolidColor(Highlight),
                keyboardOptions = KeyboardOptions(
                    keyboardType = keyboardType,
                    imeAction = imeAction,
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        keyboardController?.hide()
                        onDone?.invoke()
                    },
                    onNext = { focusManager.moveFocus(FocusDirection.Next) },
                ),
            )
        }
    }

    /** Right-side chevron icon shown on tappable rows (date pickers, etc). */
    @Composable
    private fun TrailingChevron() {
        Icon(
            imageVector = Icons.Outlined.ChevronRight,
            contentDescription = null,
            tint = InkMuted,
            modifier = Modifier.size(20.dp),
        )
    }

    /**
     * "Copy" chip shown next to the Generate button once a temp password exists.
     * Briefly switches to a green "Copied" state on tap.
     */
    @Composable
    private fun CopyPasswordChip(password: String, enabled: Boolean) {
        val clipboard = LocalClipboardManager.current
        val haptics = LocalHapticFeedback.current
        var copied by remember { mutableStateOf(false) }
        LaunchedEffect(copied) {
            if (copied) {
                delay(1500)
                copied = false
            }
        }
        val bg by animateColorAsState(
            targetValue = if (copied) Color(0xFFDCFCE7) else Color(0xFFF1F5F9),
            animationSpec = tween(150), label = "copy-bg",
        )
        val fg by animateColorAsState(
            targetValue = if (copied) Success else InkSecondary,
            animationSpec = tween(150), label = "copy-fg",
        )
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(bg)
                .clickable(enabled = enabled) {
                    clipboard.setText(AnnotatedString(password))
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    copied = true
                }
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(
                imageVector = if (copied) Icons.Outlined.Check else Icons.Outlined.ContentCopy,
                contentDescription = if (copied) "Copied" else "Copy password",
                tint = fg,
                modifier = Modifier.size(14.dp),
            )
            Text(
                text = if (copied) "Copied" else "Copy",
                color = fg,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }

    /** Read-only row content used for date fields and the auto-id row. */
    @Composable
    private fun StaticFieldText(text: String, muted: Boolean) {
        Text(
            text = text,
            color = if (muted) InkMuted else InkPrimary,
            fontSize = 16.sp,
            fontWeight = if (muted) FontWeight.Normal else FontWeight.Medium,
        )
    }

    /** Permission picker card — emoji on top, label/sub below, glow when selected. */
    @Composable
    private fun PermissionCard(
        emoji: String,
        label: String,
        sub: String,
        tint: Color,
        bg: Color,
        selected: Boolean,
        onClick: () -> Unit,
        modifier: Modifier = Modifier,
    ) {
        val borderShape = RoundedCornerShape(16.dp)
        Column(
            modifier = modifier
                .height(112.dp)
                .shadow(
                    elevation = if (selected) 16.dp else 4.dp,
                    shape = borderShape,
                    spotColor = if (selected) tint.copy(alpha = 0.5f) else ShadowSoft,
                )
                .clip(borderShape)
                .background(if (selected) bg else CardBg)
                .clickable(onClick = onClick)
                .padding(vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(if (selected) tint else bg),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = emoji,
                    fontSize = 18.sp,
                    color = if (selected) Color.White else tint,
                )
            }
            Text(
                label, color = if (selected) tint else InkPrimary,
                fontSize = 12.sp, fontWeight = FontWeight.Bold,
                letterSpacing = 0.6.sp
            )
            Text(
                sub, color = InkSecondary, fontSize = 10.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }

/* ──────────────────────────────────────────────────────────────────────────
 *  Share-credentials success screen — shown inside the AddEmployee bottom
 *  sheet immediately after the Firebase Auth account + Firestore profile
 *  have been written successfully. Lets the admin push the temp creds to
 *  the employee through their existing WhatsApp chat.
 * ────────────────────────────────────────────────────────────────────────── */
@Composable
private fun ShareCredentialsScreen(
    name: String,
    email: String,
    password: String,
    phone: String,
    onDone: () -> Unit,
) {
    val launcher = remember { com.example.uniwattelektrik.di.AppContainer.linkLauncher }
    val message = remember(name, email, password) {
        buildString {
            append("Hi ${name.ifBlank { "team" }}, your UniWatt ElekTrik account is ready.\n\n")
            append("Email: $email\n")
            append("Temporary password: $password\n\n")
            append("Open the app, tap *Create New Password (first-time setup)* on the login screen and choose a password of your own.")
        }
    }
    val navInsets = WindowInsets.navigationBars.asPaddingValues()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(ScreenBg)
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
            .padding(bottom = navInsets.calculateBottomPadding() + 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Success badge
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(SuccessBg),
            contentAlignment = Alignment.Center,
        ) { Text("✅", fontSize = 24.sp) }

        Text(
            "Account created",
            color = InkPrimary, fontSize = 22.sp, fontWeight = FontWeight.Bold,
        )
        Text(
            "Share these credentials with ${name.ifBlank { "the employee" }}. They'll only work for the very first sign-in — the employee must rotate the password from the login screen.",
            color = InkSecondary, fontSize = 13.sp,
        )

        // Cred card
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(CardBg)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            CredRow(label = "Email",    value = email)
            CredRow(label = "Password", value = password)
            if (phone.isNotBlank()) CredRow(label = "Phone", value = phone)
        }

        // Share button (WhatsApp green gradient)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .shadow(
                    elevation = 14.dp,
                    shape     = RoundedCornerShape(16.dp),
                    spotColor = Success.copy(alpha = 0.45f),
                )
                .clip(RoundedCornerShape(16.dp))
                .background(Brush.horizontalGradient(listOf(Color(0xFF25D366), Color(0xFF128C7E))))
                .clickable {
                    launcher.openWhatsApp(phoneE164 = phone, message = message)
                },
            contentAlignment = Alignment.Center,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text("💬", fontSize = 16.sp)
                Text(
                    "Share via WhatsApp",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }

        // Done button (closes the sheet)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Brush.horizontalGradient(listOf(GradStart, GradEnd)))
                .clickable(onClick = onDone),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                "Done",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun CredRow(label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            label.uppercase(),
            color = InkSecondary,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.0.sp,
            modifier = Modifier.width(80.dp),
        )
        Text(
            value,
            color = InkPrimary,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f),
        )
    }
}

/**
 * Generates a 10-character temp password mixing upper/lower/digits/symbols.
 * Uses [kotlin.random.Random] which is multi-platform safe.
 */
private fun generateTempPassword(): String {
    val pool = (
        "ABCDEFGHJKLMNPQRSTUVWXYZ" +
        "abcdefghjkmnpqrstuvwxyz" +
        "23456789" +
        "!@#%&*?"
    )
    return buildString {
        repeat(10) { append(pool[kotlin.random.Random.nextInt(pool.length)]) }
    }
}

/** Format epoch millis as "27 Apr 2026" using kotlinx-datetime (KMP-safe). */
private fun formatDate(ms: Long): String {
    val instant = kotlinx.datetime.Instant.fromEpochMilliseconds(ms)
    val ldt = instant.toLocalDateTime(kotlinx.datetime.TimeZone.UTC)
    val month = listOf(
        "Jan", "Feb", "Mar", "Apr", "May", "Jun",
        "Jul", "Aug", "Sep", "Oct", "Nov", "Dec",
    )[ldt.monthNumber - 1]
    return "${ldt.dayOfMonth} $month ${ldt.year}"
}

/** Compact pill chip used inside [FieldRow] for picking enum-ish values. */
@Composable
private fun StatusPickChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    tint: Color,
    bg: Color,
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(if (selected) bg else Color(0xFFF1F5F9))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        Text(
            text       = label,
            color      = if (selected) tint else InkSecondary,
            fontSize   = 12.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
        )
    }
}
