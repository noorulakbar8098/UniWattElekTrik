package com.example.uniwattelektrik.feature.admin.presentation.screens

import com.example.uniwattelektrik.core.theme.appScreenBackground

import androidx.compose.animation.AnimatedVisibility
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
import com.example.uniwattelektrik.feature.workforce.presentation.WorkforceViewModel
import com.example.uniwattelektrik.platform.nowEpochMillis
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/* ── Local design tokens ─────────────────────────────────────────────── */
private val ScreenBg     = Color(0xFFF4F7FB)
private val CardBg       = Color(0xFFFFFFFF)
private val InputBg      = Color(0xFFFAFBFD)
private val InkPrimary   = Color(0xFF1A2B49)
private val InkSecondary = Color(0xFF6B7A99)
private val InkMuted     = Color(0xFF94A3B8)
private val Brand        = Color(0xFF3B82F6)
private val BrandDeep    = Color(0xFF1D4ED8)
private val BrandDark    = Color(0xFF0F172A)
private val Brand50      = Color(0xFFE6F0FE)
private val Success      = Color(0xFF22C55E)
private val SuccessBg    = Color(0xFFDCFCE7)
private val Warning      = Color(0xFFF59E0B)
private val WarningBg    = Color(0xFFFEF3C7)
private val Danger       = Color(0xFFEF4444)
private val DangerBg     = Color(0xFFFEE2E2)
private val Purple       = Color(0xFF8B5CF6)
private val PurpleBg     = Color(0xFFEDE9FE)
private val ShadowSoft   = Color(0x14172C50)
private val DotBorder    = Color(0xFFCBD5E1)
private val Divider      = Color(0xFFE2E8F0)

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
) {
    val employees by workforceVm.employees.collectAsStateWithLifecycle()
    val departments by inventoryVm.departments.collectAsStateWithLifecycle()
    val equipmentAll by inventoryVm.equipment.collectAsStateWithLifecycle()

    com.example.uniwattelektrik.core.theme.SetStatusBar(color = Brand, darkIcons = false)

    /* ── Section 1: Overview ────────────────────────────────────────── */
    val taskId = remember { autoTaskId() }
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
    val geocoder       = remember { com.example.uniwattelektrik.core.platform.createAddressGeocoder() }

    // Debounced geocode on address change.
    androidx.compose.runtime.LaunchedEffect(location) {
        val query = location.trim()
        if (query.length < 4) {
            resolvedLat = null
            resolvedLng = null
            geocoding   = false
            return@LaunchedEffect
        }
        kotlinx.coroutines.delay(600)
        geocoding = true
        val result = geocoder.geocode(query)
        if (result != null) {
            resolvedLat = result.latitude
            resolvedLng = result.longitude
        } else {
            resolvedLat = null
            resolvedLng = null
        }
        geocoding = false
    }

    /* ── Section 5: Priority ──────────────────────────────────────── */
    var priority by remember { mutableStateOf("Medium") }
    var risk     by remember { mutableStateOf("Low") }

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

    val canSubmit = title.isNotBlank() && location.isNotBlank() &&
                    selectedAssignees.isNotEmpty()

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
                GradientHeader(taskId = taskId, onBack = onBack)
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
                    FieldLabel("Assign to (multi-select)", required = true)
                    if (employees.isEmpty()) {
                        Text("No employees yet — add one in Team first.",
                             color = InkMuted, fontSize = 13.sp)
                    } else {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            items(employees, key = { it.id }) { emp ->
                                AssigneeChip(
                                    employee = emp,
                                    selected = selectedAssignees.contains(emp.id),
                                    onClick  = {
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
                        value = location, onChange = { location = it },
                        leadingIcon = Icons.Filled.LocationOn,
                        leadingTint = Danger, leadingBg = DangerBg,
                        placeholder = "Site address or landmark",
                    )

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
                        PriorityCard("LOW",  "Low priority", Success, SuccessBg, "🟢",
                            priority == "Low",    { priority = "Low"    }, Modifier.weight(1f))
                        PriorityCard("MED",  "Medium",       Warning, WarningBg, "🟠",
                            priority == "Medium", { priority = "Medium" }, Modifier.weight(1f))
                        PriorityCard("HIGH", "Critical",     Danger,  DangerBg,  "🔴",
                            priority == "High",   { priority = "High"   }, Modifier.weight(1f))
                    }

                    Spacer(Modifier.height(14.dp))
                    FieldLabel("Risk level")
                    DottedSelect(
                        value = risk, onChange = { risk = it },
                        options = listOf("Low", "Medium", "High"),
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
                    if (checklist.isNotEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            checklist.forEachIndexed { idx, item ->
                                ChecklistRow(
                                    text = item,
                                    onRemove = { checklist.removeAt(idx) },
                                )
                            }
                        }
                        Spacer(Modifier.height(10.dp))
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
            onSaveDraft = { /* TODO: persist draft locally */ },
            onPreview   = { /* TODO: full-screen preview */ },
            onCreate    = {
                if (canSubmit) {
                    val dueDateMs: Long? = endDateMs?.let { day ->
                        day + (endHour * 3600_000L) + (endMinute * 60_000L)
                    }
                    val assigneeId = selectedAssignees.firstOrNull()
                    val assigneeName = employees.firstOrNull { it.id == assigneeId }?.name.orEmpty()
                    workforceVm.addTask(
                        adminUid       = adminUid,
                        userId         = assigneeId,
                        title          = title.trim(),
                        location       = location.trim(),
                        time           = endTimeStr,
                        day            = endDayLbl,
                        priority       = priority,
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
                    )
                    onCreated()
                }
            },
            modifier = Modifier.align(Alignment.BottomCenter),
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
private fun GradientHeader(taskId: String, onBack: () -> Unit) {
    com.example.uniwattelektrik.core.components.PremiumHeaderBackground(
        roundedBottom = false,
    ) {
        Column(
            modifier = Modifier
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(horizontal = 18.dp, vertical = 16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                com.example.uniwattelektrik.core.components.GlassBackButton(
                    onClick = onBack,
                )

                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("New Task", color = Color.White,
                         fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    Text("ADVANCED · 7 SECTIONS",
                         color = Color(0xCCFFFFFF), fontSize = 11.sp,
                         fontWeight = FontWeight.SemiBold, letterSpacing = 1.2.sp)
                }
            }

            Spacer(Modifier.height(12.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(Color.White.copy(alpha = 0.18f))
                    .border(1.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(50))
                    .padding(horizontal = 14.dp, vertical = 7.dp),
            ) {
                Text(taskId, color = Color.White, fontSize = 12.sp,
                     fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
            }
        }
    }
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
            .height(108.dp)
            .shadow(
                if (selected) 14.dp else 4.dp,
                RoundedCornerShape(16.dp),
                spotColor = if (selected) tint.copy(alpha = 0.5f) else ShadowSoft,
            )
            .clip(RoundedCornerShape(16.dp))
            .background(if (selected) bg else CardBg)
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            modifier = Modifier
                .size(36.dp).clip(CircleShape)
                .background(if (selected) tint else bg),
            contentAlignment = Alignment.Center,
        ) { Text(emoji, fontSize = 18.sp) }
        Text(label, color = if (selected) tint else InkPrimary,
             fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.6.sp)
        Text(sub, color = InkSecondary, fontSize = 10.sp,
             fontWeight = FontWeight.Medium)
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
        Box(
            modifier = Modifier
                .size(20.dp).clip(RoundedCornerShape(6.dp))
                .border(1.5.dp, Success, RoundedCornerShape(6.dp))
                .background(Color.White),
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
            Text("Create →", color = Color.White, fontSize = 14.sp,
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

