package com.example.uniwattelektrik.feature.admin.presentation.screens

import com.example.uniwattelektrik.core.components.ToastController
import com.example.uniwattelektrik.core.components.LoadingOverlay
import com.example.uniwattelektrik.core.performance.TrackScreenPerformance

import com.example.uniwattelektrik.core.theme.appScreenBackground
import com.example.uniwattelektrik.core.theme.AppShapes
import com.example.uniwattelektrik.core.theme.AppTheme

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.uniwattelektrik.feature.workforce.data.remote.EmployeeRecord
import com.example.uniwattelektrik.feature.workforce.data.remote.allAssigneeIds
import com.example.uniwattelektrik.feature.workforce.presentation.WorkforceViewModel
import com.example.uniwattelektrik.platform.nowEpochMillis
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
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
private val InputBg      = AppTheme.SurfaceMuted
private val DotBorder    = AppTheme.Ink300
private val Divider      = AppTheme.Ink100
private val Purple       = AppTheme.Violet
private val PurpleBg     = AppTheme.PriorityUrgentBg


/* ── Local design tokens ─────────────────────────────────────────────── */

/**
 * Advanced multi-section task creation form (8 collapsible sections).
 *
 * Sections: Overview · Assignment · Service Scheduling ·
 *           Location · Priority · Attachments & Checklist ·
 *           Notifications.
 *
 * Bottom bar: Save Draft · Preview · Create Task.
 *
 * Only the core fields (title, location, priority, day, time, assignee)
 * write to Firestore today via [WorkforceViewModel.addTask]. The advanced
 * fields live in local state — the UI is ready for a richer backend
 * schema when one lands.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewTaskScreen(
    workforceVm: WorkforceViewModel,
    inventoryVm: com.example.uniwattelektrik.feature.admin.presentation.InventoryViewModel,
    adminUid: String,
    onBack: () -> Unit,
    onCreated: () -> Unit,
    adminDisplayName: String = "",
    /**
     * When non-null, the form prefills from the matching live [TaskRecord]
     * and saves via `updateTask` instead of `addTask`. The bottom action
     * label flips to "Update →" automatically.
     */
    editTaskId: String? = null,
) {
    TrackScreenPerformance("NewTaskScreen")
    val employees by workforceVm.employees.collectAsStateWithLifecycle()
    val departments by inventoryVm.departments.collectAsStateWithLifecycle()
    val equipmentAll by inventoryVm.equipment.collectAsStateWithLifecycle()
    val liveTasks by workforceVm.tasks.collectAsStateWithLifecycle()
    val actionInProgress by workforceVm.actionInProgress.collectAsStateWithLifecycle()
    val isEditing = editTaskId != null
    val editing = remember(editTaskId, liveTasks) {
        editTaskId?.let { id -> liveTasks.firstOrNull { it.id == id } }
    }

    com.example.uniwattelektrik.core.theme.SetStatusBar(color = Brand, darkIcons = false)

    /* ── Section 1: Overview ────────────────────────────────────────── */
    val taskId = remember(editing?.id) {
        editing?.let { "TASK-${(it.id.hashCode() and 0x7FFFFFFF).toString().padStart(5, '0').take(5)}" }
            ?: autoTaskId()
    }
    var title       by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    /* ── Section 2: Assignment ─────────────────────────────────────── */
    val selectedAssignees = remember { mutableStateListOf<String>() }
    var roleFilter        by remember { mutableStateOf("Any") }
    var teamFilter        by remember { mutableStateOf("All teams") }
    var autoAssign        by remember { mutableStateOf(false) }

    /* ── Section 3: Service Scheduling ─────────────────────────────── */
    var slaHours      by remember { mutableStateOf("6") }
    var slaMinutes    by remember { mutableStateOf("0") }
    var startDateMs   by remember { mutableStateOf<Long?>(null) }
    var startHour     by remember { mutableStateOf(9) }
    var startMinute   by remember { mutableStateOf(0) }
    var endDateMs     by remember { mutableStateOf<Long?>(null) }
    var endHour       by remember { mutableStateOf(18) }
    var endMinute     by remember { mutableStateOf(0) }

    /* ── Section 4: Location ──────────────────────────────────────── */
    var location       by remember { mutableStateOf("") }
    var resolvedLat    by remember { mutableStateOf<Double?>(null) }
    var resolvedLng    by remember { mutableStateOf<Double?>(null) }
    var geocoding      by remember { mutableStateOf(false) }
    var addressSuggestions by remember {
        mutableStateOf<List<com.example.uniwattelektrik.core.platform.AddressSuggestion>>(emptyList())
    }
    // Suppressed once the user picks a suggestion — prevents the dropdown
    // re-opening as we write the chosen label back into the field.
    var suggestionsSuppressed by remember { mutableStateOf(false) }
    val geocoder       = remember { com.example.uniwattelektrik.core.platform.createAddressGeocoder() }

    // Debounced geocode + suggestions on address change.
    androidx.compose.runtime.LaunchedEffect(location) {
        val query = location.trim()
        if (query.length < 3) {
            resolvedLat = null
            resolvedLng = null
            geocoding   = false
            addressSuggestions = emptyList()
            return@LaunchedEffect
        }
        kotlinx.coroutines.delay(400)
        geocoding = true
        // Native Geocoder.getFromLocationName is fast enough that running
        // suggest + geocode sequentially is cheap and avoids the extra
        // coroutineScope/async imports.
        val suggestions = geocoder.suggest(query, limit = 5)
        addressSuggestions = if (suggestionsSuppressed) emptyList() else suggestions
        // Prefer the first suggestion's lat/lng when we have one — saves a
        // second geocode round trip. Fall back to an explicit geocode only
        // when no suggestions came back.
        val first = suggestions.firstOrNull()
        if (first != null) {
            resolvedLat = first.latitude
            resolvedLng = first.longitude
        } else {
            val result = if (query.length >= 4) geocoder.geocode(query) else null
            resolvedLat = result?.latitude
            resolvedLng = result?.longitude
        }
        geocoding = false
    }

    /* ── Section 5: Priority ──────────────────────────────────────── */
    var priority by remember { mutableStateOf("Medium") }
    var risk     by remember { mutableStateOf("Success") }

    /* ── Service details (Department + Equipment) ─────────────────── */
    var selectedDeptId   by remember { mutableStateOf<String?>(null) }
    var selectedEquipId  by remember { mutableStateOf<String?>(null) }
    var showDeptSheet    by remember { mutableStateOf(false) }
    var showEquipSheet   by remember { mutableStateOf(false) }
    val selectedDept     = departments.firstOrNull { it.id == selectedDeptId }
    val equipmentInDept  = remember(equipmentAll, selectedDeptId) {
        if (selectedDeptId == null) emptyList()
        else equipmentAll.filter { it.departmentId == selectedDeptId }
    }
    val selectedEquip    = equipmentInDept.firstOrNull { it.id == selectedEquipId }

    /* ── Section 7: Attachments & Checklist ───────────────────────── */
    val attachments = remember { mutableStateListOf<String>() }
    val checklist   = remember { mutableStateListOf<String>() }
    var newChecklist by remember { mutableStateOf("") }
    var showAttachmentPicker by remember { mutableStateOf(false) }
    var uploadingAttachment by remember { mutableStateOf(false) }
    val attachmentLauncher = com.example.uniwattelektrik.core.platform.rememberAttachmentLauncher { uri ->
        uploadingAttachment = true
        workforceVm.uploadAttachment(adminUid, uri) { result ->
            uploadingAttachment = false
            result.getOrNull()?.takeIf { it.isNotBlank() }?.let { url ->
                attachments.add(url)
            }
        }
    }

    /* ── Section 8: Notifications ─────────────────────────────────── */
    var notifyAssignee   by remember { mutableStateOf(true) }
    var notifySupervisor by remember { mutableStateOf(false) }
    var reminderHours    by remember { mutableStateOf("2") }

    /* ── Pickers ──────────────────────────────────────────────────── */
    var showStartDate by remember { mutableStateOf(false) }
    var showStartTime by remember { mutableStateOf(false) }
    var showEndDate   by remember { mutableStateOf(false) }
    var showEndTime   by remember { mutableStateOf(false) }

    /* ── Accordion expansion state ────────────────────────────────── */
    val expanded = remember { androidx.compose.runtime.mutableStateMapOf<Int, Boolean>(0 to true) }

    /* ── Edit-mode prefill ─────────────────────────────────────────── *
     * Runs once per [editing.id]: copies every field from the existing
     * [TaskRecord] into the local form state. Subsequent edits to a
     * field are user-driven — we never overwrite again.
     */
    var prefilled by remember(editing?.id) { mutableStateOf(false) }
    androidx.compose.runtime.LaunchedEffect(editing?.id) {
        val src = editing ?: return@LaunchedEffect
        if (prefilled) return@LaunchedEffect
        title       = src.title
        description = src.description
        priority    = when (src.priority.lowercase()) {
            "low"  -> "Success"
            "high" -> "Danger"
            else   -> "Medium"
        }
        location    = src.address.ifBlank { src.location }
        resolvedLat = src.latitude
        resolvedLng = src.longitude
        selectedDeptId  = src.departmentId.takeIf { it.isNotBlank() }
        selectedEquipId = src.equipmentId.takeIf { it.isNotBlank() }
        // Prefill the multi-assignee selection from whichever shape the task
        // was written in (legacy single field or the new array). Helper
        // [allAssigneeIds] merges both for us.
        val prefillIds = src.allAssigneeIds()
        if (prefillIds.isNotEmpty()) {
            selectedAssignees.clear()
            selectedAssignees.addAll(prefillIds)
        }
        checklist.clear()
        checklist.addAll(src.checklist.map { it.text })
        attachments.clear()
        attachments.addAll(src.attachments)
        endDateMs = src.dueDate
        // Best-effort time decode from the stored "HH:mm" field.
        src.time.split(":").let { parts ->
            parts.getOrNull(0)?.toIntOrNull()?.let { endHour = it }
            parts.getOrNull(1)?.toIntOrNull()?.let { endMinute = it }
        }
        prefilled = true
    }

    // Auto-assign provides its own assignee at submit time, so a manual chip
    // selection isn't required when that mode is on.
    val canSubmit = title.isNotBlank() && location.isNotBlank() &&
                    (selectedAssignees.isNotEmpty() || autoAssign)

    val startTimeStr = formatHHmm(startHour, startMinute)
    val endTimeStr   = formatHHmm(endHour, endMinute)
    val startDayLbl  = startDateMs?.let(::formatShortDate) ?: "Today"
    val endDayLbl    = endDateMs?.let(::formatShortDate) ?: "Today"

    Box(modifier = Modifier.fillMaxSize().background(appScreenBackground())) {

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 130.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                GradientHeader(taskId = taskId, onBack = onBack, isEditing = isEditing)
                Spacer(Modifier.height(12.dp))
            }

            /* 1. Overview */
            item {
                AccordionSection(
                    index = 0, title = "Task Overview",
                    icon = Icons.Filled.Description,
                    tint = Brand, bg = Brand50,
                    expandedMap = expanded,
                ) {
                    FieldLabel("Task ID")
                    DottedReadOnly(value = taskId, leadingIcon = Icons.Filled.Bolt,
                                   leadingTint = Warning, leadingBg = WarningBg)

                    Spacer(Modifier.height(12.dp))
                    FieldLabel("Task title", required = true)
                    DottedField(
                        value = title, onChange = { title = it },
                        leadingIcon = Icons.Filled.Bolt,
                        leadingTint = Warning, leadingBg = WarningBg,
                        placeholder = "e.g. Replace LT cable — Koramangala",
                    )

                    Spacer(Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.Bottom) {
                        FieldLabel("Description", modifier = Modifier.weight(1f))
                        Text("${description.length} / 500",
                             color = InkMuted, fontSize = 11.sp)
                    }
                    DottedField(
                        value = description,
                        onChange = { if (it.length <= 500) description = it },
                        leadingIcon = Icons.Filled.Description,
                        leadingTint = Brand, leadingBg = Brand50,
                        placeholder = "Add work scope, precautions, materials…",
                        minHeight = 80.dp,
                    )
                }
            }

            /* 2. Assignment Logic */
            item {
                AccordionSection(
                    index = 1, title = "Assignment Logic",
                    icon = Icons.Filled.People,
                    tint = Purple, bg = PurpleBg,
                    expandedMap = expanded,
                ) {
                    // Lock the assignee chooser entirely when editing a
                    // completed task — admins shouldn't be silently re-routing
                    // history. The rest of the form (notes, attachments,
                    // checklist) remains editable.
                    val assigneeLocked = isEditing && editing?.status == "Done"
                    // Auto-assign toggle — when on, the form picks the employee
                    // with the fewest active tasks at submit time, optionally
                    // honouring the Role / Team filters below as a candidate
                    // pool. Manual chips are greyed out so the admin sees
                    // the system is making the call.
                    ToggleRow(
                        title    = "Auto-assign (load-balanced)",
                        subtitle = "Pick the available employee with the fewest active tasks",
                        checked  = autoAssign,
                        onChange = {
                            if (assigneeLocked) return@ToggleRow
                            autoAssign = it; if (it) selectedAssignees.clear()
                        },
                        tint     = Purple,
                    )
                    Spacer(Modifier.height(10.dp))

                    FieldLabel("Assign to (multi-select)", required = !autoAssign)
                    if (assigneeLocked) {
                        Text(
                            "Assignees are locked because this task is already completed.",
                            color = InkMuted, fontSize = 12.sp,
                        )
                        Spacer(Modifier.height(8.dp))
                    }
                    if (employees.isEmpty()) {
                        Text("No employees yet — add one in Team first.",
                             color = InkMuted, fontSize = 13.sp)
                    } else {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            items(employees, key = { it.id }) { emp ->
                                AssigneeChip(
                                    employee = emp,
                                    selected = !autoAssign && selectedAssignees.contains(emp.id),
                                    onClick  = {
                                        if (assigneeLocked || autoAssign) return@AssigneeChip
                                        if (selectedAssignees.contains(emp.id))
                                            selectedAssignees.remove(emp.id)
                                        else selectedAssignees.add(emp.id)
                                    },
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(14.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Column(modifier = Modifier.weight(1f)) {
                            FieldLabel("Role-based")
                            DottedSelect(
                                value = roleFilter, onChange = { roleFilter = it },
                                options = listOf("Any", "Engineer", "Supervisor", "Technician"),
                                leadingIcon = Icons.Filled.People,
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            FieldLabel("Team")
                            DottedSelect(
                                value = teamFilter, onChange = { teamFilter = it },
                                options = listOf("All teams", "North zone", "South zone", "Central"),
                                leadingIcon = Icons.Filled.People,
                            )
                        }
                    }

                    Spacer(Modifier.height(12.dp))
                    ToggleRow(
                        title = "Auto-assign by load balance",
                        subtitle = "Pick the user with fewest open tasks in the matching role/team",
                        checked = autoAssign, onChange = { autoAssign = it },
                        tint = Purple,
                    )
                }
            }

            /* 3. Service Scheduling */
            item {
                AccordionSection(
                    index = 2, title = "Service Details",
                    icon = Icons.Filled.Category,
                    tint = Brand, bg = Brand50,
                    expandedMap = expanded,
                ) {
                    FieldLabel("Department")
                    DottedClickable(
                        leadingIcon = Icons.Filled.Category,
                        leadingTint = Brand, leadingBg = Brand50,
                        value = selectedDept?.name ?: "Select a department",
                        muted = selectedDept == null,
                        onClick = { showDeptSheet = true },
                    )

                    Spacer(Modifier.height(12.dp))
                    FieldLabel("Equipment")
                    DottedClickable(
                        leadingIcon = Icons.Filled.Category,
                        leadingTint = Brand, leadingBg = Brand50,
                        value = when {
                            selectedDept == null      -> "Pick a department first"
                            equipmentInDept.isEmpty() -> "No equipment in this department"
                            selectedEquip == null     -> "Select equipment"
                            else                      -> selectedEquip.name
                        },
                        muted = selectedEquip == null,
                        onClick = {
                            if (selectedDept != null && equipmentInDept.isNotEmpty()) {
                                showEquipSheet = true
                            }
                        },
                    )
                }
            }

            /* 4. Service Scheduling */
            item {
                AccordionSection(
                    index = 3, title = "Service Scheduling",
                    icon = Icons.Filled.Schedule,
                    tint = Warning, bg = WarningBg,
                    expandedMap = expanded,
                ) {
                    FieldLabel("SLA")
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Column(modifier = Modifier.weight(1f)) {
                            DottedField(
                                value = slaHours,
                                onChange = { slaHours = it.filter { c -> c.isDigit() }.take(3) },
                                leadingIcon = Icons.Filled.HourglassEmpty,
                                leadingTint = Warning, leadingBg = WarningBg,
                                trailing = "hrs", keyboard = KeyboardType.Number,
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            DottedField(
                                value = slaMinutes,
                                onChange = { slaMinutes = it.filter { c -> c.isDigit() }.take(2) },
                                leadingIcon = Icons.Filled.HourglassEmpty,
                                leadingTint = Warning, leadingBg = WarningBg,
                                trailing = "min", keyboard = KeyboardType.Number,
                            )
                        }
                    }

                    Spacer(Modifier.height(14.dp))
                    FieldLabel("Start")
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Column(modifier = Modifier.weight(1f)) {
                            DottedClickable(
                                leadingIcon = Icons.Outlined.CalendarMonth,
                                leadingTint = Brand, leadingBg = Brand50,
                                value = startDayLbl, muted = startDateMs == null,
                                onClick = { showStartDate = true },
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            DottedClickable(
                                leadingIcon = Icons.Filled.Schedule,
                                leadingTint = Brand, leadingBg = Brand50,
                                value = startTimeStr, muted = false,
                                onClick = { showStartTime = true },
                            )
                        }
                    }

                    Spacer(Modifier.height(12.dp))
                    FieldLabel("End / Deadline")
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Column(modifier = Modifier.weight(1f)) {
                            DottedClickable(
                                leadingIcon = Icons.Outlined.CalendarMonth,
                                leadingTint = Danger, leadingBg = DangerBg,
                                value = endDayLbl, muted = endDateMs == null,
                                onClick = { showEndDate = true },
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            DottedClickable(
                                leadingIcon = Icons.Filled.Schedule,
                                leadingTint = Danger, leadingBg = DangerBg,
                                value = endTimeStr, muted = false,
                                onClick = { showEndTime = true },
                            )
                        }
                    }
                }
            }

            /* 4. Location Intelligence */
            item {
                AccordionSection(
                    index = 4, title = "Location Intelligence",
                    icon = Icons.Filled.LocationOn,
                    tint = Danger, bg = DangerBg,
                    expandedMap = expanded,
                ) {
                    FieldLabel("Address", required = true)
                    DottedField(
                        value = location,
                        onChange = {
                            location = it
                            // User is typing again — re-open suggestions if any.
                            suggestionsSuppressed = false
                        },
                        leadingIcon = Icons.Filled.LocationOn,
                        leadingTint = Danger, leadingBg = DangerBg,
                        placeholder = "Site address or landmark",
                    )

                    // Suggestions dropdown — appears below the field while the
                    // user is typing. Tapping a row fills the field, pins the
                    // lat/lng, and dismisses further suggestions.
                    AnimatedVisibility(
                        visible = addressSuggestions.isNotEmpty(),
                        enter   = expandVertically() + fadeIn(),
                        exit    = shrinkVertically() + fadeOut(),
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 6.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(CardBg)
                                .border(1.dp, DividerSoft, RoundedCornerShape(14.dp)),
                        ) {
                            addressSuggestions.forEachIndexed { i, s ->
                                if (i > 0) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(1.dp)
                                            .background(DividerSoft),
                                    )
                                }
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            // Apply suggestion + suppress
                                            // further dropdown until the user
                                            // types again.
                                            location              = s.label
                                            resolvedLat           = s.latitude
                                            resolvedLng           = s.longitude
                                            suggestionsSuppressed = true
                                            addressSuggestions    = emptyList()
                                        }
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Icon(
                                        Icons.Filled.LocationOn,
                                        contentDescription = null,
                                        tint = Danger,
                                        modifier = Modifier.size(16.dp),
                                    )
                                    Spacer(Modifier.width(10.dp))
                                    Text(
                                        s.label,
                                        color    = InkPrimary,
                                        fontSize = 13.sp,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(8.dp))
                    val statusText = when {
                        location.trim().length < 4               -> "Type at least 4 characters to locate on map."
                        geocoding                                -> "Locating address…"
                        resolvedLat != null && resolvedLng != null -> "Pinned at ${formatLatLng(resolvedLat!!)}, ${formatLatLng(resolvedLng!!)}"
                        else                                     -> "Couldn't resolve address. Try a more specific query."
                    }
                    Text(
                        statusText,
                        color = if (resolvedLat != null) Success else InkSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                    )

                    Spacer(Modifier.height(12.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFFE8EFFA)),
                    ) {
                        com.example.uniwattelektrik.core.platform.OsmMap(
                            latitude  = resolvedLat,
                            longitude = resolvedLng,
                            modifier  = Modifier.fillMaxSize(),
                        )
                    }
                }
            }

            /* 5. Priority */
            item {
                AccordionSection(
                    index = 5, title = "Priority",
                    icon = Icons.Filled.Flag,
                    tint = Danger, bg = DangerBg,
                    expandedMap = expanded,
                ) {
                    FieldLabel("Priority", required = true)
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        PriorityCard("LOW",  "Success priority", Success, SuccessBg, "🟢",
                            priority == "Success",    { priority = "Success"    }, Modifier.weight(1f))
                        PriorityCard("MED",  "Medium",       Warning, WarningBg, "🟠",
                            priority == "Medium", { priority = "Medium" }, Modifier.weight(1f))
                        PriorityCard("HIGH", "Critical",     Danger,  DangerBg,  "🔴",
                            priority == "Danger",   { priority = "Danger"   }, Modifier.weight(1f))
                    }

                    Spacer(Modifier.height(14.dp))
                    FieldLabel("Risk level")
                    DottedSelect(
                        value = risk, onChange = { risk = it },
                        options = listOf("Success", "Medium", "Danger"),
                        leadingIcon = Icons.Filled.Flag,
                    )
                }
            }

            /* 7. Attachments & Checklist */
            item {
                AccordionSection(
                    index = 6, title = "Attachments & Checklist",
                    icon = Icons.Filled.AttachFile,
                    tint = Success, bg = SuccessBg,
                    expandedMap = expanded,
                ) {
                    FieldLabel("Attachments")
                    if (attachments.isNotEmpty()) {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(attachments) { url ->
                                AttachmentThumb(url = url, onRemove = { attachments.remove(url) })
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                    }
                    DottedClickable(
                        leadingIcon = Icons.Filled.AttachFile,
                        leadingTint = Success, leadingBg = SuccessBg,
                        value = if (uploadingAttachment) "Uploading…" else "Tap to add photo (camera / gallery)",
                        muted = true,
                        onClick = { if (!uploadingAttachment) showAttachmentPicker = true },
                    )

                    Spacer(Modifier.height(16.dp))
                    FieldLabel("Checklist")
                    // Smoothly expand / collapse the items column so adding the
                    // first item or removing the last one doesn't jump.
                    AnimatedVisibility(
                        visible = checklist.isNotEmpty(),
                        enter   = expandVertically() + fadeIn(),
                        exit    = shrinkVertically() + fadeOut(),
                    ) {
                        Column(
                            modifier            = Modifier.animateContentSize(),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            checklist.forEachIndexed { idx, item ->
                                ChecklistRow(
                                    text = item,
                                    onRemove = { checklist.removeAt(idx) },
                                )
                            }
                            Spacer(Modifier.height(10.dp))
                        }
                    }
                    AddItemRow(
                        value = newChecklist,
                        placeholder = "Add checklist item…",
                        onChange = { newChecklist = it },
                        onAdd = {
                            if (newChecklist.isNotBlank()) {
                                checklist.add(newChecklist.trim())
                                newChecklist = ""
                            }
                        },
                        tint = Success, bg = SuccessBg,
                    )
                }
            }

            /* 8. Notifications */
            item {
                AccordionSection(
                    index = 6, title = "Notifications",
                    icon = Icons.Filled.Notifications,
                    tint = Brand, bg = Brand50,
                    expandedMap = expanded,
                ) {
                    ToggleRow(
                        title = "Notify assigned user",
                        subtitle = "Send a push when this task is assigned",
                        checked = notifyAssignee, onChange = { notifyAssignee = it },
                        tint = Brand,
                    )
                    Spacer(Modifier.height(8.dp))
                    ToggleRow(
                        title = "Notify supervisor",
                        subtitle = "Cc the supervisor on assignment + breach events",
                        checked = notifySupervisor, onChange = { notifySupervisor = it },
                        tint = Brand,
                    )

                    Spacer(Modifier.height(12.dp))
                    FieldLabel("Reminder before deadline")
                    DottedField(
                        value = reminderHours,
                        onChange = { reminderHours = it.filter { c -> c.isDigit() }.take(3) },
                        leadingIcon = Icons.Filled.Schedule,
                        leadingTint = Brand, leadingBg = Brand50,
                        trailing = "hrs", keyboard = KeyboardType.Number,
                    )
                }
            }
        }

        /* ── Sticky bottom action bar ───────────────────────────────── */
        BottomActions(
            canSubmit = canSubmit,
            isEditing = isEditing,
            onSaveDraft = { /* TODO: persist draft locally */ },
            onPreview   = { /* TODO: full-screen preview */ },
            onCreate    = {
                if (canSubmit) {
                    val dueDateMs: Long? = endDateMs?.let { day ->
                        day + (endHour * 3600_000L) + (endMinute * 60_000L)
                    }
                    // ── Auto-assign: pick the employee with the fewest active
                    // tasks. Ties broken by zone match (preferring the task's
                    // team filter), then alphabetical name for determinism. ──
                    val resolvedAssigneeId: String? = if (autoAssign) {
                        val activeEmps = employees.filter { it.deletedAt == null && it.status != "Inactive" }
                        val pool = activeEmps.filter { emp ->
                            (roleFilter == "Any" || emp.role.contains(roleFilter, ignoreCase = true))
                                && (teamFilter == "All teams" || emp.zone.equals(teamFilter, ignoreCase = true))
                        }.ifEmpty { activeEmps }   // fall back to all-active if filters wipe the pool
                        val openCountByUser = liveTasks
                            .filter { it.status != "Done" }
                            .flatMap { it.allAssigneeIds() }
                            .groupingBy { it }
                            .eachCount()
                        pool.minWithOrNull(
                            compareBy<com.example.uniwattelektrik.feature.workforce.data.remote.EmployeeRecord> {
                                openCountByUser[it.id] ?: 0
                            }.thenBy {
                                // Same-zone tiebreak when team filter is set explicitly.
                                if (teamFilter != "All teams" && it.zone.equals(teamFilter, ignoreCase = true)) 0 else 1
                            }.thenBy { it.name.lowercase() }
                        )?.id
                    } else {
                        selectedAssignees.firstOrNull()
                    }
                    val assigneeId   = resolvedAssigneeId
                    val assigneeName = employees.firstOrNull { it.id == assigneeId }?.name.orEmpty()
                    // Multi-assignee fan-out: when the admin manually picked
                    // people we send the whole list; for auto-assign we send
                    // just the resolved single id. Names align 1:1.
                    val multiAssigneeIds: List<String> = when {
                        autoAssign -> listOfNotNull(assigneeId)
                        else       -> selectedAssignees.toList()
                    }
                    val multiAssigneeNames: List<String> = multiAssigneeIds.map { uid ->
                        employees.firstOrNull { it.id == uid }?.name.orEmpty()
                    }
                    val tName = title.trim()
                    val toName = assigneeName.takeIf { it.isNotBlank() }
                    val afterAction: (Result<Unit>) -> Unit = { result ->
                        if (result.isSuccess) {
                            if (isEditing) {
                                ToastController.success(
                                    title = "Task updated",
                                    body  = if (toName != null) "$tName · Assigned to $toName" else tName,
                                )
                            } else {
                                ToastController.success(
                                    title = "Task created",
                                    body  = if (toName != null) "Assigned to $toName · Due $endDayLbl $endTimeStr" else tName,
                                )
                            }
                            onCreated()
                        }
                    }
                    if (isEditing && editTaskId != null) {
                        workforceVm.updateTask(
                            taskId         = editTaskId,
                            adminUid       = adminUid,
                            userId         = assigneeId,
                            title          = title.trim(),
                            location       = location.trim(),
                            time           = endTimeStr,
                            day            = endDayLbl,
                            priority       = priority,
                            description    = description.trim(),
                            departmentId   = selectedDeptId.orEmpty(),
                            departmentName = selectedDept?.name.orEmpty(),
                            equipmentId    = selectedEquipId.orEmpty(),
                            equipmentName  = selectedEquip?.name.orEmpty(),
                            checklist      = checklist.map {
                                com.example.uniwattelektrik.feature.workforce.data.remote.ChecklistItem(text = it)
                            },
                            attachments    = attachments.toList(),
                            address        = location.trim(),
                            latitude       = resolvedLat,
                            longitude      = resolvedLng,
                            dueDate        = dueDateMs,
                            assigneeName   = assigneeName,
                            notifyAssignee = notifyAssignee,
                            assigneeIds    = multiAssigneeIds,
                            assigneeNames  = multiAssigneeNames,
                            onDone         = afterAction,
                        )
                    } else {
                        workforceVm.addTask(
                            adminUid       = adminUid,
                            userId         = assigneeId,
                            title          = title.trim(),
                            location       = location.trim(),
                            time           = endTimeStr,
                            day            = endDayLbl,
                            priority       = priority,
                            description    = description.trim(),
                            departmentId   = selectedDeptId.orEmpty(),
                            departmentName = selectedDept?.name.orEmpty(),
                            equipmentId    = selectedEquipId.orEmpty(),
                            equipmentName  = selectedEquip?.name.orEmpty(),
                            checklist      = checklist.map {
                                com.example.uniwattelektrik.feature.workforce.data.remote.ChecklistItem(text = it)
                            },
                            attachments    = attachments.toList(),
                            address        = location.trim(),
                            latitude       = resolvedLat,
                            longitude      = resolvedLng,
                            dueDate        = dueDateMs,
                            ownerAdminName = adminDisplayName,
                            assigneeName   = assigneeName,
                            notifyAssignee = notifyAssignee,
                            assigneeIds    = multiAssigneeIds,
                            assigneeNames  = multiAssigneeNames,
                            onDone         = afterAction,
                        )
                    }
                }
            },
            modifier = Modifier.align(Alignment.BottomCenter),
        )

        // Modal loader while addTask / updateTask is in flight
        LoadingOverlay(
            visible = actionInProgress,
            message = if (isEditing) "Updating task…" else "Creating task…",
        )
    }

    /* ── Date / time pickers ──────────────────────────────────────── */
    if (showStartDate) {
        val s = rememberDatePickerState(initialSelectedDateMillis = startDateMs)
        DatePickerDialog(
            onDismissRequest = { showStartDate = false },
            confirmButton = {
                TextButton(onClick = { startDateMs = s.selectedDateMillis; showStartDate = false }) {
                    Text("OK")
                }
            },
            dismissButton = { TextButton(onClick = { showStartDate = false }) { Text("Cancel") } },
        ) { DatePicker(state = s) }
    }
    if (showStartTime) {
        val tps = rememberTimePickerState(initialHour = startHour, initialMinute = startMinute)
        DatePickerDialog(
            onDismissRequest = { showStartTime = false },
            confirmButton = {
                TextButton(onClick = {
                    startHour = tps.hour; startMinute = tps.minute; showStartTime = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showStartTime = false }) { Text("Cancel") } },
        ) {
            Box(modifier = Modifier.fillMaxWidth().padding(24.dp),
                contentAlignment = Alignment.Center) { TimePicker(state = tps) }
        }
    }
    if (showEndDate) {
        val s = rememberDatePickerState(initialSelectedDateMillis = endDateMs)
        DatePickerDialog(
            onDismissRequest = { showEndDate = false },
            confirmButton = {
                TextButton(onClick = { endDateMs = s.selectedDateMillis; showEndDate = false }) {
                    Text("OK")
                }
            },
            dismissButton = { TextButton(onClick = { showEndDate = false }) { Text("Cancel") } },
        ) { DatePicker(state = s) }
    }
    if (showEndTime) {
        val tps = rememberTimePickerState(initialHour = endHour, initialMinute = endMinute)
        DatePickerDialog(
            onDismissRequest = { showEndTime = false },
            confirmButton = {
                TextButton(onClick = {
                    endHour = tps.hour; endMinute = tps.minute; showEndTime = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showEndTime = false }) { Text("Cancel") } },
        ) {
            Box(modifier = Modifier.fillMaxWidth().padding(24.dp),
                contentAlignment = Alignment.Center) { TimePicker(state = tps) }
        }
    }

    /* ── Department / Equipment modal selectors ──────────────────── */
    if (showDeptSheet) {
        com.example.uniwattelektrik.core.components.ModalSelectSheet(
            title          = "Select department",
            items          = departments,
            selectedId     = selectedDeptId,
            itemId         = { it.id },
            itemTitle      = { it.name },
            searchPlaceholder = "Search departments…",
            emptyText      = "No departments yet — add one in Inventory ▸ Departments.",
            onDismiss      = { showDeptSheet = false },
            onSelect       = { dept ->
                if (dept.id != selectedDeptId) {
                    // Reset equipment when department changes
                    selectedEquipId = null
                }
                selectedDeptId = dept.id
                showDeptSheet  = false
            },
        )
    }
    if (showEquipSheet) {
        com.example.uniwattelektrik.core.components.ModalSelectSheet(
            title          = "Select equipment",
            items          = equipmentInDept,
            selectedId     = selectedEquipId,
            itemId         = { it.id },
            itemTitle      = { it.name },
            searchPlaceholder = "Search equipment…",
            emptyText      = "No equipment in this department.",
            onDismiss      = { showEquipSheet = false },
            onSelect       = { eq ->
                selectedEquipId = eq.id
                showEquipSheet  = false
            },
        )
    }

    if (showAttachmentPicker) {
        AttachmentPickerSheet(
            onDismiss  = { showAttachmentPicker = false },
            onCamera   = {
                showAttachmentPicker = false
                attachmentLauncher.launchCamera()
            },
            onGallery  = {
                showAttachmentPicker = false
                attachmentLauncher.launchGallery()
            },
        )
    }
}

/* ─────────────────────────────────────────────────────────────────────── *
 *  HEADER
 * ─────────────────────────────────────────────────────────────────────── */

@Composable
private fun GradientHeader(taskId: String, onBack: () -> Unit, isEditing: Boolean = false) {
    com.example.uniwattelektrik.core.components.OperationsHeader(
        eyebrow  = if (isEditing) "UPDATE EXISTING TASK" else "ADVANCED · 7 SECTIONS",
        title    = if (isEditing) "Edit Task" else "New Task",
        onBack   = onBack,
        extras   = {
            com.example.uniwattelektrik.core.components.DsGlassChip(label = taskId)
        },
    )
}

/* ─────────────────────────────────────────────────────────────────────── *
 *  ACCORDION
 * ─────────────────────────────────────────────────────────────────────── */

@Composable
private fun AccordionSection(
    index: Int,
    title: String,
    icon: ImageVector,
    tint: Color,
    bg: Color,
    expandedMap: androidx.compose.runtime.snapshots.SnapshotStateMap<Int, Boolean>,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit,
) {
    val open = expandedMap[index] ?: false
    val rotation by animateFloatAsState(
        targetValue = if (open) 180f else 0f,
        animationSpec = tween(220),
        label = "chev",
    )
    Column(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .shadow(10.dp, RoundedCornerShape(20.dp), spotColor = ShadowSoft)
            .clip(RoundedCornerShape(20.dp))
            .background(CardBg),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expandedMap[index] = !open }
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(bg),
                contentAlignment = Alignment.Center,
            ) { Icon(icon, null, tint = tint, modifier = Modifier.size(20.dp)) }

            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("SECTION ${index + 1}",
                     color = InkMuted, fontSize = 10.sp,
                     fontWeight = FontWeight.Bold, letterSpacing = 1.0.sp)
                Text(title, color = InkPrimary, fontSize = 15.sp,
                     fontWeight = FontWeight.Bold)
            }

            Icon(
                imageVector = Icons.Filled.ExpandMore,
                contentDescription = null,
                tint = InkSecondary,
                modifier = Modifier.size(22.dp).rotate(rotation),
            )
        }

        AnimatedVisibility(
            visible = open,
            enter = fadeIn(tween(180)) + expandVertically(tween(220)),
            exit  = fadeOut(tween(120)) + shrinkVertically(tween(180)),
        ) {
            Column(modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)) {
                Box(modifier = Modifier
                    .fillMaxWidth().height(1.dp).background(Divider))
                Spacer(Modifier.height(14.dp))
                content()
            }
        }
    }
}

/* ─────────────────────────────────────────────────────────────────────── *
 *  FORM PRIMITIVES
 * ─────────────────────────────────────────────────────────────────────── */

@Composable
private fun FieldLabel(text: String, required: Boolean = false, modifier: Modifier = Modifier) {
    Row(modifier = modifier) {
        Text(text, color = InkPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        if (required) Text(" *", color = Danger, fontSize = 13.sp, fontWeight = FontWeight.Bold)
    }
    Spacer(Modifier.height(6.dp))
}

private fun Modifier.dottedBorder(
    color: Color = DotBorder,
    cornerRadius: androidx.compose.ui.unit.Dp = 12.dp,
    strokeWidth: androidx.compose.ui.unit.Dp = 1.5.dp,
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

@Composable
private fun DottedField(
    value: String,
    onChange: (String) -> Unit,
    placeholder: String = "",
    leadingIcon: ImageVector? = null,
    leadingTint: Color = Brand,
    leadingBg: Color = Brand50,
    trailing: String? = null,
    keyboard: KeyboardType = KeyboardType.Text,
    minHeight: androidx.compose.ui.unit.Dp = 48.dp,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(InputBg)
            .dottedBorder()
            .padding(horizontal = 12.dp, vertical = 10.dp)
            .heightIn(min = minHeight),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leadingIcon != null) {
            Box(
                modifier = Modifier
                    .size(32.dp).clip(RoundedCornerShape(10.dp)).background(leadingBg),
                contentAlignment = Alignment.Center,
            ) { Icon(leadingIcon, null, tint = leadingTint, modifier = Modifier.size(18.dp)) }
            Spacer(Modifier.width(10.dp))
        }
        Box(modifier = Modifier.weight(1f)) {
            if (value.isEmpty()) Text(placeholder, color = InkMuted, fontSize = 13.sp)
            BasicTextField(
                value = value, onValueChange = onChange,
                textStyle = TextStyle(color = InkPrimary, fontSize = 13.sp,
                                      fontWeight = FontWeight.Medium),
                cursorBrush = SolidColor(Brand),
                keyboardOptions = KeyboardOptions(keyboardType = keyboard),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (trailing != null) {
            Spacer(Modifier.width(8.dp))
            Text(trailing, color = InkMuted, fontSize = 12.sp,
                 fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun DottedReadOnly(
    value: String,
    leadingIcon: ImageVector,
    leadingTint: Color,
    leadingBg: Color,
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
                .size(32.dp).clip(RoundedCornerShape(10.dp)).background(leadingBg),
            contentAlignment = Alignment.Center,
        ) { Icon(leadingIcon, null, tint = leadingTint, modifier = Modifier.size(18.dp)) }
        Spacer(Modifier.width(10.dp))
        Text(value, color = InkSecondary, fontSize = 13.sp,
             fontWeight = FontWeight.SemiBold,
             modifier = Modifier.weight(1f))
        Text("auto", color = InkMuted, fontSize = 11.sp)
    }
}

@Composable
private fun DottedSelect(
    value: String,
    onChange: (String) -> Unit,
    options: List<String>,
    leadingIcon: ImageVector,
) {
    var open by remember { mutableStateOf(false) }
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(InputBg)
                .dottedBorder()
                .clickable { open = !open }
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp).clip(RoundedCornerShape(10.dp)).background(Brand50),
                contentAlignment = Alignment.Center,
            ) { Icon(leadingIcon, null, tint = Brand, modifier = Modifier.size(18.dp)) }
            Spacer(Modifier.width(10.dp))
            Text(value, color = InkPrimary, fontSize = 13.sp,
                 fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            Icon(
                imageVector = Icons.Filled.ExpandMore,
                contentDescription = null, tint = InkMuted,
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
                            .clickable { onChange(opt); open = false }
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(opt, color = InkPrimary, fontSize = 13.sp,
                             fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                        if (value == opt) {
                            Icon(Icons.Filled.Check, null,
                                 tint = Brand, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DottedClickable(
    leadingIcon: ImageVector,
    leadingTint: Color,
    leadingBg: Color,
    value: String,
    muted: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(InputBg)
            .dottedBorder()
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(32.dp).clip(RoundedCornerShape(10.dp)).background(leadingBg),
            contentAlignment = Alignment.Center,
        ) { Icon(leadingIcon, null, tint = leadingTint, modifier = Modifier.size(18.dp)) }
        Spacer(Modifier.width(10.dp))
        Text(
            value,
            color = if (muted) InkMuted else InkPrimary,
            fontSize = 13.sp,
            fontWeight = if (muted) FontWeight.Medium else FontWeight.SemiBold,
        )
    }
}

@Composable
private fun ToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
    tint: Color,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(InputBg)
            .clickable { onChange(!checked) }
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = InkPrimary, fontSize = 14.sp,
                 fontWeight = FontWeight.SemiBold)
            Text(subtitle, color = InkSecondary, fontSize = 11.sp,
                 fontWeight = FontWeight.Medium, maxLines = 2,
                 overflow = TextOverflow.Ellipsis)
        }
        Spacer(Modifier.width(10.dp))
        Box(
            modifier = Modifier
                .width(46.dp).height(26.dp)
                .clip(RoundedCornerShape(50))
                .background(if (checked) tint else Color(0xFFE2E8F0)),
            contentAlignment = if (checked) Alignment.CenterEnd else Alignment.CenterStart,
        ) {
            Box(
                modifier = Modifier
                    .padding(2.dp).size(22.dp)
                    .clip(CircleShape).background(Color.White),
            )
        }
    }
}

@Composable
private fun StatusPickChip(
    label: String, selected: Boolean, onClick: () -> Unit,
    tint: Color, bg: Color,
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(if (selected) bg else InputBg)
            .border(
                width = if (selected) 1.5.dp else 1.dp,
                color = if (selected) tint else Divider,
                shape = RoundedCornerShape(50),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.size(7.dp).clip(CircleShape).background(tint),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            label, color = if (selected) tint else InkSecondary,
            fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun AssigneeChip(
    employee: EmployeeRecord,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(if (selected) Brand50 else InputBg)
            .then(
                if (selected) Modifier.border(2.dp, Brand, RoundedCornerShape(14.dp))
                else Modifier.dottedBorder(cornerRadius = 14.dp),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(32.dp).clip(CircleShape)
                .background(Brush.linearGradient(listOf(Brand, BrandDeep))),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                employee.name.take(2).uppercase().ifBlank { "??" },
                color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold,
            )
        }
        Spacer(Modifier.width(10.dp))
        Column {
            Text(
                employee.name.ifBlank { "Unnamed" },
                color = InkPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
            Text(
                employee.role.ifBlank { "Engineer" },
                color = InkSecondary, fontSize = 11.sp,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
        }
        if (selected) {
            Spacer(Modifier.width(8.dp))
            Icon(Icons.Filled.CheckCircle, null,
                 tint = Brand, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun PriorityCard(
    label: String, sub: String, tint: Color, bg: Color, emoji: String,
    selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .height(126.dp)
            .shadow(
                if (selected) 14.dp else 4.dp,
                RoundedCornerShape(16.dp),
                spotColor = if (selected) tint.copy(alpha = 0.5f) else ShadowSoft,
            )
            .clip(RoundedCornerShape(16.dp))
            .background(if (selected) bg else CardBg)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Box(
            modifier = Modifier
                .size(34.dp).clip(CircleShape)
                .background(if (selected) tint else bg),
            contentAlignment = Alignment.Center,
        ) { Text(emoji, fontSize = 17.sp) }
        Text(
            text       = label,
            color      = if (selected) tint else InkPrimary,
            fontSize   = 12.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.6.sp,
            maxLines   = 1,
        )
        Text(
            text       = sub,
            color      = InkSecondary,
            fontSize   = 10.sp,
            fontWeight = FontWeight.Medium,
            maxLines   = 1,
            textAlign  = androidx.compose.ui.text.style.TextAlign.Center,
        )
    }
}

@Composable
private fun MaterialChip(label: String, onRemove: () -> Unit) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(Brand50)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(Brand))
        Spacer(Modifier.width(8.dp))
        Text(label, color = Brand, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .size(18.dp).clip(CircleShape).background(Color(0x22000000))
                .clickable(onClick = onRemove),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.Close, null, tint = Brand, modifier = Modifier.size(11.dp))
        }
    }
}

@Composable
private fun ChecklistRow(text: String, onRemove: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(InputBg)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Visual placeholder — admins build the checklist; the assignee
        // ticks each item later in the user app via the same DsCheckbox.
        com.example.uniwattelektrik.core.components.DsCheckbox(
            checked  = false,
            onChange = {},
            enabled  = false,
            size     = 20.dp,
        )
        Spacer(Modifier.width(10.dp))
        Text(text, color = InkPrimary, fontSize = 13.sp,
             fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
        Box(
            modifier = Modifier
                .size(22.dp).clip(CircleShape).background(Color(0x11000000))
                .clickable(onClick = onRemove),
            contentAlignment = Alignment.Center,
        ) { Icon(Icons.Filled.Close, null, tint = InkSecondary, modifier = Modifier.size(13.dp)) }
    }
}

@Composable
private fun AddItemRow(
    value: String,
    placeholder: String,
    onChange: (String) -> Unit,
    onAdd: () -> Unit,
    tint: Color,
    bg: Color,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(InputBg)
            .dottedBorder()
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(28.dp).clip(CircleShape).background(bg)
                .clickable(onClick = onAdd),
            contentAlignment = Alignment.Center,
        ) {
            Text("+", color = tint, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.width(10.dp))
        Box(modifier = Modifier.weight(1f)) {
            if (value.isEmpty()) Text(placeholder, color = InkMuted, fontSize = 13.sp)
            BasicTextField(
                value = value, onValueChange = onChange,
                textStyle = TextStyle(color = InkPrimary, fontSize = 13.sp),
                cursorBrush = SolidColor(tint),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun MiniMapPlaceholder() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFFE8EFFA))
            .dottedBorder(cornerRadius = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            repeat(5) {
                Box(modifier = Modifier
                    .fillMaxWidth().height(1.dp).background(Color(0xFFCFDBED)))
            }
        }
        Box(
            modifier = Modifier
                .size(44.dp).clip(CircleShape).background(Brand)
                .border(3.dp, Color.White, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.LocationOn, null, tint = Color.White,
                 modifier = Modifier.size(22.dp))
        }
    }
}

/* ─────────────────────────────────────────────────────────────────────── *
 *  BOTTOM ACTION BAR
 * ─────────────────────────────────────────────────────────────────────── */

@Composable
private fun BottomActions(
    canSubmit: Boolean,
    onSaveDraft: () -> Unit,
    onPreview: () -> Unit,
    onCreate: () -> Unit,
    modifier: Modifier = Modifier,
    isEditing: Boolean = false,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(CardBg)
            .shadow(20.dp, spotColor = ShadowSoft)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        OutlinedAction(label = "Save Draft", onClick = onSaveDraft,
                       modifier = Modifier.weight(1f))
        OutlinedAction(label = "Preview",    onClick = onPreview,
                       modifier = Modifier.weight(1f))
        Box(
            modifier = Modifier
                .weight(1.4f).height(48.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(
                    if (canSubmit)
                        Brush.horizontalGradient(listOf(Brand, BrandDeep))
                    else SolidColor(Color(0xFFB6CDEF)),
                )
                .clickable(enabled = canSubmit, onClick = onCreate),
            contentAlignment = Alignment.Center,
        ) {
            Text(if (isEditing) "Update →" else "Create →",
                 color = Color.White, fontSize = 14.sp,
                 fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun OutlinedAction(label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .height(48.dp)
            .clip(RoundedCornerShape(14.dp))
            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(14.dp))
            .background(CardBg)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = InkSecondary, fontSize = 13.sp,
             fontWeight = FontWeight.SemiBold)
    }
}

/* ─────────────────────────────────────────────────────────────────────── *
 *  HELPERS
 * ─────────────────────────────────────────────────────────────────────── */

private fun autoTaskId(): String {
    val seed = (nowEpochMillis() / 1000) % 100000
    return "TASK-${seed.toString().padStart(5, '0')}"
}

private fun formatLatLng(value: Double): String {
    // 4-decimal-place formatter that works on all KMP targets (no String.format).
    val rounded = kotlin.math.round(value * 10000.0) / 10000.0
    val s = rounded.toString()
    val dot = s.indexOf('.')
    return if (dot < 0) "$s.0000"
    else {
        val frac = s.substring(dot + 1)
        val padded = if (frac.length >= 4) frac.substring(0, 4)
                     else frac.padEnd(4, '0')
        s.substring(0, dot) + "." + padded
    }
}

private fun formatHHmm(h: Int, m: Int) =
    h.toString().padStart(2, '0') + ":" + m.toString().padStart(2, '0')

private fun formatShortDate(epochMs: Long): String {
    val ldt = Instant.fromEpochMilliseconds(epochMs).toLocalDateTime(TimeZone.UTC)
    val months = listOf("Jan","Feb","Mar","Apr","May","Jun",
                        "Jul","Aug","Sep","Oct","Nov","Dec")
    return "${ldt.dayOfMonth.toString().padStart(2,'0')} ${months[ldt.monthNumber - 1]} ${ldt.year}"
}

/* ─────────────────────────────────────────────────────────────────────── *
 *  ATTACHMENTS
 * ─────────────────────────────────────────────────────────────────────── */

@Composable
private fun AttachmentThumb(url: String, onRemove: () -> Unit) {
    Box(
        modifier = Modifier
            .size(72.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Brand50),
    ) {
        coil3.compose.AsyncImage(
            model = url,
            contentDescription = null,
            modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(12.dp)),
            contentScale = androidx.compose.ui.layout.ContentScale.Crop,
        )
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(4.dp)
                .size(20.dp)
                .clip(CircleShape)
                .background(Color(0xCC000000))
                .clickable(onClick = onRemove),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.Close, null, tint = Color.White, modifier = Modifier.size(12.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AttachmentPickerSheet(
    onDismiss: () -> Unit,
    onCamera:  () -> Unit,
    onGallery: () -> Unit,
) {
    val sheetState = androidx.compose.material3.rememberModalBottomSheetState(skipPartiallyExpanded = true)
    androidx.compose.material3.ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                "Add attachment",
                color = InkPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                "Pick a source for this photo.",
                color = InkSecondary,
                fontSize = 13.sp,
            )
            Spacer(Modifier.height(4.dp))
            PickerOption(
                icon  = Icons.Filled.Schedule, // generic; replace if needed
                label = "Camera",
                sub   = "Capture a new photo",
                tint  = Brand,
                bg    = Brand50,
                onClick = onCamera,
            )
            PickerOption(
                icon  = Icons.Filled.AttachFile,
                label = "Gallery",
                sub   = "Choose from your photos",
                tint  = Success,
                bg    = SuccessBg,
                onClick = onGallery,
            )
        }
    }
}

@Composable
private fun PickerOption(
    icon: ImageVector,
    label: String,
    sub: String,
    tint: Color,
    bg: Color,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(InputBg)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp).clip(RoundedCornerShape(12.dp)).background(bg),
            contentAlignment = Alignment.Center,
        ) { Icon(icon, null, tint = tint, modifier = Modifier.size(20.dp)) }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(label, color = InkPrimary, fontSize = 14.sp,
                 fontWeight = FontWeight.SemiBold)
            Text(sub, color = InkSecondary, fontSize = 12.sp)
        }
        Icon(Icons.Filled.ChevronRight, null, tint = InkMuted, modifier = Modifier.size(20.dp))
    }
}

