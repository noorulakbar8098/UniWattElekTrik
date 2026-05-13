package com.example.uniwattelektrik.feature.user.presentation.screens

import com.example.uniwattelektrik.core.components.ToastController
import com.example.uniwattelektrik.core.performance.TrackScreenPerformance

import com.example.uniwattelektrik.core.theme.appScreenBackground
import com.example.uniwattelektrik.core.theme.AppShapes
import com.example.uniwattelektrik.core.theme.AppTheme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.uniwattelektrik.core.components.EmptyState
import com.example.uniwattelektrik.core.sample.SampleTasks
import com.example.uniwattelektrik.core.platform.rememberAttachmentLauncher
import com.example.uniwattelektrik.core.platform.rememberJobCardExporter
import com.example.uniwattelektrik.feature.workforce.data.remote.TaskNote
import com.example.uniwattelektrik.feature.workforce.data.remote.TaskRecord
import com.example.uniwattelektrik.feature.workforce.data.remote.allAssigneeIds
import com.example.uniwattelektrik.feature.workforce.data.remote.allAssigneeNames
import com.example.uniwattelektrik.core.components.DsAvatarBubble
import com.example.uniwattelektrik.core.components.DsAvatarStack
import androidx.compose.animation.animateContentSize
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import com.example.uniwattelektrik.feature.workforce.presentation.WorkforceViewModel
import com.example.uniwattelektrik.platform.LocationProvider
import kotlinx.coroutines.launch
import kotlinx.datetime.toLocalDateTime
import kotlin.math.absoluteValue
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
private val Purple       = AppTheme.Violet
private val PurpleBg     = AppTheme.PriorityUrgentBg


/* ── Design tokens — matched 1:1 with AdminEmployeeDetailScreen ────────── */

/** Distinguishes Admin vs User viewers — drives which actions are visible. */
enum class TaskDetailRole { Admin, User }

/**
 * Premium task detail screen — visual structure matches
 * [com.example.uniwattelektrik.feature.admin.presentation.screens.AdminEmployeeDetailScreen]
 * (gradient header, floating stats card, section-cards with accent stripe).
 */
@Composable
fun TaskDetailScreen(
    taskId: String,
    onBack: () -> Unit,
    onStartWork: (taskId: String) -> Unit,
    modifier: Modifier = Modifier,
    workforceVm: WorkforceViewModel? = null,
    viewerRole: TaskDetailRole = TaskDetailRole.User,
    currentUserId: String = "",
    currentUserName: String = "",
    adminUid: String = "",
    /**
     * Optional edit handler. When supplied **and** [viewerRole] is
     * [TaskDetailRole.Admin], a pencil action is rendered in the gradient
     * header. User-side viewers never see the action even if a handler is
     * passed, keeping the role-restriction enforced in the UI layer too.
     */
    onEdit: ((taskId: String) -> Unit)? = null,
) {
    TrackScreenPerformance("TaskDetailScreen")
    val liveTasks by (workforceVm?.tasks?.collectAsStateWithLifecycle()
        ?: remember { kotlinx.coroutines.flow.MutableStateFlow(emptyList<TaskRecord>()) }
            .collectAsStateWithLifecycle())
    val live = liveTasks.firstOrNull { it.id == taskId }
    val sample = SampleTasks.byId(taskId)
    val task = live?.let(::sampleFromRecord) ?: sample

    com.example.uniwattelektrik.core.theme.SetStatusBar(color = Brand, darkIcons = false)

    if (task == null) {
        EmptyState(emoji = "❓", title = "Task not found",
                   body = "It may have been reassigned or removed.")
        return
    }

    val checklist = live?.checklist ?: emptyList()
    val attachmentUrls = live?.attachments ?: emptyList()
    var fullscreenAttachment by remember { mutableStateOf<String?>(null) }

    // Live notes from Firestore subcollection.
    val notesFlow = remember(taskId, workforceVm) { workforceVm?.observeTaskNotes(taskId) }
    val liveNotes by (notesFlow?.collectAsStateWithLifecycle()
        ?: remember { kotlinx.coroutines.flow.MutableStateFlow(emptyList<TaskNote>()) }
            .collectAsStateWithLifecycle())
    var newNote by remember { mutableStateOf("") }
    var descExpanded by remember { mutableStateOf(false) }
    var assigneesExpanded by remember { mutableStateOf(false) }
    var isUploadingAttachment by remember { mutableStateOf(false) }

    // Voice notes — recorder for capturing new clips, player for inline
    // playback of clips others have posted. Both are device-local; recorded
    // audio is uploaded to Cloudinary and the URL is stored on the TaskNote.
    val voiceRecorder = com.example.uniwattelektrik.core.platform.rememberVoiceRecorder()
    val voicePlayer   = com.example.uniwattelektrik.core.platform.rememberVoicePlayer()
    // Per-second tick so the recording label updates smoothly.
    var recordingTickMs by remember { mutableStateOf(0L) }
    // Tracks the post-stop upload window so the composer can show a small
    // inline spinner the moment the user taps "send voice", cleared as soon
    // as the Firestore listener delivers the actual voice note.
    var isUploadingVoice by remember { mutableStateOf(false) }
    androidx.compose.runtime.LaunchedEffect(voiceRecorder.isRecording) {
        while (voiceRecorder.isRecording) {
            recordingTickMs = voiceRecorder.elapsedMs
            kotlinx.coroutines.delay(80)
        }
        recordingTickMs = 0L
    }

    // Job-card PDF exporter + live spare-items catalog for unit prices.
    val shareJobCard = rememberJobCardExporter()
    val spareItems by (workforceVm?.spareItems?.collectAsStateWithLifecycle()
        ?: remember {
            kotlinx.coroutines.flow.MutableStateFlow(
                emptyList<com.example.uniwattelektrik.feature.workforce.data.remote.SpareItemRecord>()
            )
        }.collectAsStateWithLifecycle())

    // Attachment upload — extracted so the toast RETRY action can re-invoke it
    // with the same URI on failure (network blip, etc.).
    fun uploadAttachmentUri(uri: String) {
        val current = live ?: return
        isUploadingAttachment = true
        workforceVm?.uploadAttachment(
            adminId    = adminUid.ifBlank { current.adminId },
            contentUri = uri,
        ) { result ->
            isUploadingAttachment = false
            result
                .onSuccess { newUrl ->
                    workforceVm.updateAttachments(current.id, current.attachments + newUrl)
                }
                .onFailure { err ->
                    ToastController.error(
                        title       = "Photo upload failed",
                        body        = err.message?.takeIf { it.isNotBlank() }
                            ?: "Network unavailable · tap retry",
                        actionLabel = "Retry",
                        onAction    = { uploadAttachmentUri(uri) },
                    )
                }
        }
    }

    // Photo annotation — when a picker returns a URI, route it through the
    // annotator overlay first so the technician can highlight defects with a
    // coloured pen before the photo is flattened + uploaded. The overlay is
    // dismissable: cancel → no upload, done → upload the annotated copy.
    var pendingAnnotationUri by remember { mutableStateOf<String?>(null) }

    // Attachment picker — Gallery + Camera. The picked URI is staged for
    // annotation; uploadAttachmentUri is only called once the user taps Done.
    val attachmentLauncher = rememberAttachmentLauncher { uri ->
        pendingAnnotationUri = uri
    }

    val scope = rememberCoroutineScope()
    val locationProvider = remember { LocationProvider() }

    LazyColumn(
        modifier = modifier.fillMaxSize().background(appScreenBackground()),
        contentPadding = PaddingValues(bottom = 110.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        // Gradient header
        item {
            TaskHeader(
                task = task,
                ownerName = live?.ownerAdminName.orEmpty(),
                onBack = onBack,
                // Edit action visible only when an Admin viewer AND a handler
                // was provided by the host (currently only the AdminShell wires this).
                onEdit = if (viewerRole == TaskDetailRole.Admin && onEdit != null)
                    { { onEdit(taskId) } } else null,
            )
        }

        // Floating stats card (overlaps header)
        item {
            Box(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .offset(y = (-32).dp),
            ) {
                StatsCard(task = task, ownerLabel = live?.ownerAdminName.orEmpty().initialsOrFallback(task.assigneeInitials))
            }
        }

        // Description card
        item {
            Box(modifier = Modifier
                .padding(horizontal = 16.dp)
                .offset(y = (-16).dp)) {
                SectionCard(title = "Description") {
                    val full = task.description
                    val short = if (full.length > 140) full.take(140) + "…" else full
                    Text(
                        if (descExpanded) full else short,
                        color = InkPrimary,
                        fontSize = 14.sp,
                        lineHeight = 22.sp,
                    )
                    if (full.length > 140) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            if (descExpanded) "Show less" else "Read more",
                            color = Brand,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.clickable { descExpanded = !descExpanded },
                        )
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }

        // Assignees card — shows avatar stack (collapsed) and click to expand
        // the full per-assignee list with name + initials avatar. Hidden when
        // the task has no assignees at all.
        live?.let { rec ->
            val names = rec.allAssigneeNames()
            val ids   = rec.allAssigneeIds()
            if (names.isNotEmpty() || ids.isNotEmpty()) {
                item {
                    Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                        AssigneesCard(
                            names    = names,
                            expanded = assigneesExpanded,
                            onToggle = { assigneesExpanded = !assigneesExpanded },
                        )
                    }
                    Spacer(Modifier.height(16.dp))
                }
            }
        }

        // Activity timeline card — built from live TaskRecord timestamps.
        item {
            Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                SectionCard(title = "Activity") {
                    Spacer(Modifier.height(4.dp))
                    val rows = buildActivityRows(live)
                    if (rows.isEmpty()) {
                        Text("No activity yet.",
                            color = InkSecondary, fontSize = 13.sp)
                    } else {
                        rows.forEachIndexed { idx, row ->
                            TimelineRow(
                                tint    = row.tint,
                                time    = row.time,
                                title   = row.title,
                                note    = row.note,
                                isFirst = idx == 0,
                                isLast  = idx == rows.lastIndex,
                                active  = row.active,
                                faded   = row.faded,
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }

        // Material Details card (department + equipment)
        if (live != null && (live.departmentName.isNotBlank() || live.equipmentName.isNotBlank())) {
            item {
                Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                    SectionCard(title = "Material Details") {
                        if (live.departmentName.isNotBlank()) {
                            MaterialRow(label = "Department", value = live.departmentName)
                        }
                        if (live.equipmentName.isNotBlank()) {
                            if (live.departmentName.isNotBlank()) Spacer(Modifier.height(8.dp))
                            MaterialRow(label = "Equipment", value = live.equipmentName)
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
        }

        // Checklist card
        item {
            Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                SectionCard(
                    title = "Checklist",
                    trailingAction = if (checklist.isEmpty()) null
                                     else "${checklist.count { it.done }} / ${checklist.size}",
                ) {
                    if (checklist.isEmpty()) {
                        Text(
                            "No checklist items.",
                            color = InkSecondary, fontSize = 13.sp,
                        )
                    } else {
                        checklist.forEachIndexed { i, item ->
                            ChecklistRow(
                                item = ChecklistItem(item.text, item.done),
                                onToggle = {
                                    workforceVm?.toggleChecklistItem(taskId, i, !item.done)
                                },
                            )
                            if (i != checklist.lastIndex) Spacer(Modifier.height(4.dp))
                        }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }

        // Attachments card — supports picking from gallery or camera
        item {
            Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                SectionCard(
                    title = "Attachments",
                    trailingAction = if (attachmentUrls.isEmpty()) null
                                     else "${attachmentUrls.size} ${if (attachmentUrls.size == 1) "file" else "files"}",
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        if (attachmentUrls.isNotEmpty()) {
                            AttachmentsGrid(
                                urls    = attachmentUrls,
                                onClick = { fullscreenAttachment = it },
                            )
                        } else if (!isUploadingAttachment) {
                            Text(
                                "No attachments yet.",
                                color = InkSecondary, fontSize = 13.sp,
                            )
                        }
                        // Add-attachment row (hidden when task is Done)
                        if (workforceVm != null && live?.status != "Done") {
                            AttachmentPickerRow(
                                isUploading = isUploadingAttachment,
                                onGallery   = { attachmentLauncher.launchGallery() },
                                onCamera    = { attachmentLauncher.launchCamera() },
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }

        // Notes card — live chat between admin & assigned user.
        item {
            Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                SectionCard(title = "Notes") {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        if (liveNotes.isEmpty()) {
                            Text(
                                "No notes yet. Start the conversation below.",
                                color = InkSecondary, fontSize = 13.sp,
                            )
                        } else {
                            liveNotes.forEach { n ->
                                NoteRow(
                                    note = NoteItem(
                                        author          = n.authorName.ifBlank { "—" },
                                        initials        = noteInitials(n.authorName),
                                        tint            = if (n.role == "admin") Brand else Purple,
                                        timestamp       = formatNoteTime(n.createdAtMs),
                                        message         = n.message,
                                        voiceUrl        = n.voiceUrl,
                                        voiceDurationMs = n.voiceDurationMs,
                                    ),
                                    player = voicePlayer,
                                )
                            }
                        }
                        ComposerRow(
                            value = newNote,
                            onChange = { newNote = it },
                            onSend = {
                                val msg = newNote.trim()
                                if (msg.isNotBlank() && workforceVm != null && currentUserId.isNotBlank()) {
                                    workforceVm.addTaskNote(
                                        taskId     = taskId,
                                        authorId   = currentUserId,
                                        authorName = currentUserName.ifBlank { "—" },
                                        role       = if (viewerRole == TaskDetailRole.Admin) "admin" else "user",
                                        message    = msg,
                                    )
                                    newNote = ""
                                }
                            },
                            isRecording       = voiceRecorder.isRecording,
                            isUploading       = isUploadingVoice,
                            recordingMs       = recordingTickMs,
                            // MediaRecorder.prepare()/start()/stop() are
                            // synchronous native calls that can take hundreds
                            // of ms on budget chipsets. Running them on the
                            // main thread was triggering ANRs ("Waited 5s for
                            // MotionEvent"). The recorder API is now suspend;
                            // we launch onto the screen scope which is Main-
                            // dispatched and the recorder hops to IO inside.
                            onStartRecording  = { scope.launch { voiceRecorder.start() } },
                            onCancelRecording = { voiceRecorder.cancel() },
                            onStopRecording   = {
                                scope.launch {
                                    val rec = voiceRecorder.stop()
                                    if (rec != null && workforceVm != null && currentUserId.isNotBlank()) {
                                        isUploadingVoice = true
                                        workforceVm.postVoiceNote(
                                            adminId    = adminUid.ifBlank { live?.adminId ?: "" },
                                            taskId     = taskId,
                                            authorId   = currentUserId,
                                            authorName = currentUserName.ifBlank { "—" },
                                            role       = if (viewerRole == TaskDetailRole.Admin) "admin" else "user",
                                            contentUri = rec.contentUri,
                                            durationMs = rec.durationMs,
                                        ) { result ->
                                            isUploadingVoice = false
                                            result.fold(
                                                onSuccess = {
                                                    ToastController.success(
                                                        title = "Voice note posted",
                                                        body  = "Audio uploaded and delivered.",
                                                    )
                                                },
                                                onFailure = { err ->
                                                    ToastController.error(
                                                        title = "Voice note failed",
                                                        body  = err.message?.takeIf { it.isNotBlank() }
                                                            ?: "Upload failed · check your connection.",
                                                    )
                                                },
                                            )
                                        }
                                    } else if (rec == null) {
                                        ToastController.error(
                                            title = "Recording failed",
                                            body  = voiceRecorder.lastError
                                                ?: "Please try again.",
                                        )
                                    }
                                }
                            },
                        )
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }

        // Completion Summary card — visible only when task is Done and signoff data exists.
        if (live != null && live.status == "Done" && (
                live.signoffDescription.isNotBlank() ||
                live.rca.isNotBlank() ||
                live.downtimeMinutes > 0 ||
                live.materialsUsed.isNotEmpty()
            )
        ) {
            item {
                Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                    SectionCard(title = "Completion Summary") {
                        if (live.signoffDescription.isNotBlank()) {
                            CompletionDetailRow(
                                label = "Work Done",
                                value = live.signoffDescription,
                            )
                            Spacer(Modifier.height(10.dp))
                        }
                        if (live.rca.isNotBlank()) {
                            CompletionDetailRow(
                                label = "Root Cause",
                                value = live.rca,
                            )
                            Spacer(Modifier.height(10.dp))
                        }
                        if (live.downtimeMinutes > 0) {
                            CompletionDetailRow(
                                label = "Downtime",
                                value = "${live.downtimeMinutes} min",
                            )
                            Spacer(Modifier.height(10.dp))
                        }
                        live.totalWorkDurationMs?.takeIf { it > 0 }?.let { dur ->
                            CompletionDetailRow(
                                label = "Work Duration",
                                value = formatDuration(dur),
                            )
                            Spacer(Modifier.height(10.dp))
                        }
                        if (live.materialsUsed.isNotEmpty()) {
                            Text(
                                "MATERIALS USED",
                                color = InkSecondary,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = 0.8.sp,
                            )
                            Spacer(Modifier.height(6.dp))
                            live.materialsUsed.forEach { m ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(6.dp)
                                                .clip(CircleShape)
                                                .background(Brand),
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Text(
                                            m.itemName.ifBlank { m.itemId },
                                            color = InkPrimary,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Medium,
                                        )
                                    }
                                    Text(
                                        "× ${m.quantity}",
                                        color = InkSecondary,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                }
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        // Job-card PDF export — branded one-page completion
                        // certificate that admins hand to customers. Renders
                        // off the live task + spare-items prices on Android;
                        // iOS is a no-op stub until we ship a native renderer.
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(AppShapes.pill)
                                .background(Brand)
                                .clickable {
                                    val prices = spareItems.associate { it.id to it.price }
                                    val filename = "uniwatt-jobcard-${live.id.take(8)}.pdf"
                                    shareJobCard(live, prices, filename)
                                    ToastController.info(
                                        title = "Job card ready",
                                        body  = "PDF generated for ${live.title.take(40)}",
                                    )
                                }
                                .padding(horizontal = 14.dp, vertical = 11.dp),
                            verticalAlignment     = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                        ) {
                            Icon(
                                Icons.Filled.IosShare,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(15.dp),
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "Share job card (PDF)",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
        }

        // Location card
        item {
            // Resolve coordinates from the live TaskRecord first (most up-to-
            // date), fall back to whatever we mapped onto the SampleTask.
            val mapLat = live?.latitude ?: task.latitude
            val mapLng = live?.longitude ?: task.longitude
            val hasCoords = mapLat != null && mapLng != null
            val mapLauncher = remember { com.example.uniwattelektrik.platform.LinkLauncher() }
            val openMaps: () -> Unit = {
                if (hasCoords) {
                    mapLauncher.openMap(mapLat!!, mapLng!!, label = task.location)
                }
            }
            Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                SectionCard(
                    title          = "Location",
                    trailingAction = if (hasCoords) "Open in Maps" else null,
                    onTrailing     = openMaps,
                ) {
                    Row(
                        verticalAlignment = Alignment.Top,
                        modifier          = Modifier.then(
                            if (hasCoords) Modifier.clickable(onClick = openMaps) else Modifier,
                        ),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp).clip(RoundedCornerShape(12.dp))
                                .background(Brand50),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(Icons.Filled.LocationOn, null,
                                 tint = Brand, modifier = Modifier.size(20.dp))
                        }
                        Spacer(Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("ADDRESS", color = InkSecondary, fontSize = 10.sp,
                                 fontWeight = FontWeight.SemiBold, letterSpacing = 0.8.sp)
                            Spacer(Modifier.height(2.dp))
                            Text(task.location, color = InkPrimary,
                                 fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            if (hasCoords) {
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    "${formatLatLng5(mapLat!!)}, ${formatLatLng5(mapLng!!)}",
                                    color = InkSecondary,
                                    fontSize = 11.sp,
                                )
                            }
                        }
                        Icon(Icons.AutoMirrored.Filled.OpenInNew, null,
                             tint = Brand, modifier = Modifier.size(20.dp))
                    }

                    Spacer(Modifier.height(12.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth().height(140.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFFE8EFFA))
                            .then(
                                if (hasCoords) Modifier.clickable(onClick = openMaps)
                                else Modifier,
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.SpaceBetween,
                        ) {
                            repeat(5) {
                                Box(modifier = Modifier
                                    .fillMaxWidth().height(1.dp)
                                    .background(Color(0xFFCFDBED)))
                            }
                        }
                        Box(
                            modifier = Modifier
                                .size(40.dp).clip(CircleShape)
                                .background(Brand)
                                .border(3.dp, Color.White, CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(Icons.Filled.LocationOn, null,
                                 tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                    }

                    // Explicit primary CTA below the map preview — opens the
                    // platform's preferred maps app at the exact pin.
                    Spacer(Modifier.height(12.dp))
                    if (hasCoords) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Brand)
                                .clickable(onClick = openMaps)
                                .padding(vertical = 12.dp),
                            verticalAlignment     = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                        ) {
                            Icon(
                                Icons.Filled.LocationOn,
                                contentDescription = null,
                                tint     = Color.White,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "Open in Google Maps",
                                color      = Color.White,
                                fontSize   = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    } else {
                        Text(
                            "No coordinates recorded for this site yet.",
                            color    = InkSecondary,
                            fontSize = 12.sp,
                        )
                    }
                }
            }
            Spacer(Modifier.height(20.dp))
        }

        // Bottom action row — User-only. Admin sees no action buttons here.
        if (viewerRole == TaskDetailRole.User && live != null) {
            item {
                Spacer(Modifier.height(4.dp))
                UserTaskActionBar(
                    status     = live.status,
                    taskId     = live.id,
                    adminUid   = adminUid,
                    userId     = currentUserId.takeIf { it.isNotBlank() },
                    workforceVm = workforceVm,
                    locationProvider = locationProvider,
                    scope      = scope,
                    onStartWork = onStartWork,
                    siteLat    = live.latitude,
                    siteLng    = live.longitude,
                    siteLabel  = live.location,
                )
            }
        }
    }

    fullscreenAttachment?.let { url ->
        AttachmentFullscreenDialog(url = url, onDismiss = { fullscreenAttachment = null })
    }

    // Photo annotation overlay — appears full-screen once the picker has
    // returned a URI. Cancel discards; Done uploads the flattened JPEG.
    pendingAnnotationUri?.let { uri ->
        com.example.uniwattelektrik.core.components.PhotoAnnotatorOverlay(
            sourceUri = uri,
            onCancel  = { pendingAnnotationUri = null },
            onDone    = { annotatedUri ->
                pendingAnnotationUri = null
                uploadAttachmentUri(annotatedUri)
            },
        )
    }
}

/* ─────────────────────────────────────────────────────────────────────── *
 *  HEADER (mirrors AdminEmployeeDetailScreen)
 * ─────────────────────────────────────────────────────────────────────── */

@Composable
private fun TaskHeader(
    task: com.example.uniwattelektrik.core.sample.SampleTask,
    ownerName: String,
    onBack: () -> Unit,
    onEdit: (() -> Unit)? = null,
) {
    com.example.uniwattelektrik.core.components.OperationsHeaderSurface(
        horizontalPadding = 18.dp,
        bottomPadding     = 60.dp,
    ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                com.example.uniwattelektrik.core.components.GlassBackButton(
                    onClick = onBack,
                )
                Spacer(Modifier.width(14.dp))
                Text(
                    "Task Details",
                    color = Color.White,
                    fontSize = 24.sp,           // canonical header title size
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.weight(1f),
                )
                // Admin-only edit action — hidden entirely for User viewers.
                if (onEdit != null) {
                    GlassButton(onClick = onEdit) {
                        Icon(
                            imageVector = Icons.Filled.Edit,
                            contentDescription = "Edit task",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            }

            Spacer(Modifier.height(22.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                /* Big task icon (bolt) — mirrors the avatar in employee detail */
                val iconGradient = priorityIconGradient(task.priority.label)
                Box(modifier = Modifier.size(108.dp)) {
                    Box(
                        modifier = Modifier
                            .size(108.dp)
                            .shadow(20.dp, RoundedCornerShape(28.dp),
                                    spotColor = Color(0x55000000))
                            .clip(RoundedCornerShape(28.dp))
                            .background(Brush.linearGradient(iconGradient)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Bolt,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(48.dp),
                        )
                    }
                    /* Status indicator — bottom-right (matches employee online dot) */
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .align(Alignment.BottomEnd)
                            .offset(x = (-4).dp, y = (-4).dp)
                            .clip(CircleShape)
                            .background(statusColor(task.status.label))
                            .border(3.dp, Color.White, CircleShape),
                    )
                }

                Spacer(Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        task.title,
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 26.sp,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Field operations · ${task.day} ${task.time}",
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (ownerName.isNotBlank()) {
                        Spacer(Modifier.height(2.dp))
                        Text(
                            "Owner · $ownerName",
                            color = Color.White.copy(alpha = 0.80f),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }

                    Spacer(Modifier.height(10.dp))
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(Color.White.copy(alpha = 0.18f))
                            .border(1.dp, Color.White.copy(alpha = 0.25f),
                                    RoundedCornerShape(50))
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            "#${task.id}",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp,
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("•", color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            task.priority.label.uppercase(),
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp,
                        )
                    }
                }
            }
        }
}

@Composable
private fun GlassButton(onClick: () -> Unit, content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White.copy(alpha = 0.18f))
            .border(1.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
        content = { content() },
    )
}

/* ─────────────────────────────────────────────────────────────────────── *
 *  STATS CARD (mirrors employee stats card)
 * ─────────────────────────────────────────────────────────────────────── */

@Composable
private fun StatsCard(task: com.example.uniwattelektrik.core.sample.SampleTask, ownerLabel: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .shadow(16.dp, RoundedCornerShape(20.dp), spotColor = ShadowSoft)
            .clip(RoundedCornerShape(20.dp))
            .background(CardBg)
            .padding(vertical = 16.dp),
    ) {
        StatItem(
            // Display the full status (e.g. "IN PROGRESS"); StatItem auto-shrinks the
            // font for longer labels so it never gets truncated to "INPROG".
            value = humanStatusLabel(task.status.label).uppercase(),
            label = "STATUS",
            valueColor = statusColor(task.status.label),
            modifier = Modifier.weight(1f),
        )
        VerticalDivider()
        StatItem(value = task.time, label = "DUE",
                 valueColor = InkPrimary, modifier = Modifier.weight(1f))
        VerticalDivider()
        StatItem(value = ownerLabel.ifBlank { "—" }, label = "OWNER",
                 valueColor = InkPrimary, modifier = Modifier.weight(1f))
    }
}

/** Returns initials for [this] (first letter of up to 2 words). Falls back to [fallback] when blank. */
private fun String.initialsOrFallback(fallback: String): String {
    val parts = trim().split(" ", "_", ".", "-").filter { it.isNotBlank() }
    val initials = parts.take(2)
        .mapNotNull { it.firstOrNull()?.uppercaseChar() }
        .joinToString("")
    return initials.ifBlank { fallback.ifBlank { "—" } }
}

@Composable
private fun StatItem(
    value: String,
    label: String,
    valueColor: Color,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            value,
            color = valueColor,
            // Scale value font down for longer strings so labels like
            // "IN PROGRESS" fit on one line at any column width.
            fontSize = when {
                value.length <= 4  -> 22.sp
                value.length <= 7  -> 16.sp
                value.length <= 11 -> 13.sp
                else               -> 11.sp
            },
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            label,
            color = InkSecondary,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.6.sp,
        )
    }
}

@Composable
private fun VerticalDivider() {
    Box(
        modifier = Modifier
            .width(1.dp)
            .fillMaxHeight()
            .padding(vertical = 8.dp)
            .background(DividerSoft),
    )
}

/* ─────────────────────────────────────────────────────────────────────── *
 *  SECTION CARD (matches employee SectionCard with brand-blue accent stripe)
 * ─────────────────────────────────────────────────────────────────────── */

@Composable
private fun SectionCard(
    title: String? = null,
    trailingAction: String? = null,
    onTrailing: () -> Unit = {},
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(10.dp, RoundedCornerShape(20.dp), spotColor = ShadowSoft)
            .clip(RoundedCornerShape(20.dp))
            .background(CardBg)
            .padding(horizontal = 18.dp, vertical = 16.dp),
    ) {
        if (title != null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .height(16.dp).width(3.dp)
                        .clip(RoundedCornerShape(50))
                        .background(Brand),
                )
                Spacer(Modifier.width(8.dp))
                Text(title, color = InkPrimary, fontSize = 16.sp,
                     fontWeight = FontWeight.Bold,
                     modifier = Modifier.weight(1f))
                if (trailingAction != null) {
                    Text(
                        trailingAction,
                        color = Brand,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.clickable(onClick = onTrailing),
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
        }
        content()
    }
}

/* ─────────────────────────────────────────────────────────────────────── *
 *  ASSIGNEES CARD — avatar stack + collapsible per-assignee list
 * ─────────────────────────────────────────────────────────────────────── */

/**
 * Compact summary row when collapsed: avatar stack + "N assignees" label +
 * chevron. Tapping expands an inline list with each assignee's avatar bubble
 * and full name, animated via [animateContentSize].
 */
@Composable
private fun AssigneesCard(
    names    : List<String>,
    expanded : Boolean,
    onToggle : () -> Unit,
) {
    val initialsList = names.map { initialsFor(it) }
    val count        = names.size
    val label        = when (count) {
        0    -> "Unassigned"
        1    -> names.first()
        else -> "$count assignees"
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(10.dp, RoundedCornerShape(20.dp), spotColor = ShadowSoft)
            .clip(RoundedCornerShape(20.dp))
            .background(CardBg)
            .clickable(onClick = onToggle)
            .animateContentSize()
            .padding(horizontal = 18.dp, vertical = 16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .height(16.dp).width(3.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Brand),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                "Assignees",
                color = InkPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
            )
            if (initialsList.isNotEmpty()) {
                if (initialsList.size == 1) {
                    DsAvatarBubble(initials = initialsList.first(), size = 32.dp)
                } else {
                    DsAvatarStack(
                        initialsList = initialsList,
                        size         = 32.dp,
                        maxVisible   = 4,
                    )
                }
                Spacer(Modifier.width(8.dp))
            }
            Icon(
                imageVector = if (expanded) Icons.Filled.KeyboardArrowUp
                              else Icons.Filled.KeyboardArrowDown,
                contentDescription = if (expanded) "Collapse" else "Expand",
                tint = InkSecondary,
                modifier = Modifier.size(22.dp),
            )
        }

        if (count > 1 || (count == 1 && expanded)) {
            Spacer(Modifier.height(6.dp))
            Text(
                label,
                color = InkSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
            )
        }

        if (expanded && names.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                names.forEachIndexed { i, name ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        DsAvatarBubble(
                            initials = initialsList.getOrElse(i) { "?" },
                            size     = 36.dp,
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                name.ifBlank { "Unnamed" },
                                color = InkPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                if (i == 0) "Primary assignee" else "Co-assignee",
                                color = InkSecondary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                            )
                        }
                    }
                }
            }
        }
    }
}

/** Two-letter uppercase initials for a full name. */
private fun initialsFor(name: String): String {
    val parts = name.trim().split(' ', '\t', '_', '.', '-').filter { it.isNotBlank() }
    return when {
        parts.size >= 2 -> "${parts[0].first()}${parts[1].first()}".uppercase()
        parts.size == 1 -> parts[0].first().uppercase().toString()
        else            -> "?"
    }
}

/* ─────────────────────────────────────────────────────────────────────── *
 *  TIMELINE
 * ─────────────────────────────────────────────────────────────────────── */

@Composable
private fun TimelineRow(
    tint: Color,
    time: String,
    title: String,
    note: String,
    isFirst: Boolean,
    isLast: Boolean,
    active: Boolean = false,
    faded: Boolean = false,
) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top,
            modifier = Modifier.width(28.dp),
        ) {
            Box(modifier = Modifier
                .height(8.dp).width(2.dp)
                .background(if (isFirst) Color.Transparent else DividerSoft))
            Box(
                modifier = Modifier
                    .size(if (active) 14.dp else 10.dp)
                    .clip(CircleShape)
                    .background(if (faded) Color(0xFFE2E8F0) else tint)
                    .then(
                        if (active) Modifier.border(3.dp, tint.copy(alpha = 0.25f), CircleShape)
                        else Modifier,
                    ),
            )
            Box(modifier = Modifier
                .height(58.dp).width(2.dp)
                .background(if (isLast) Color.Transparent else DividerSoft))
        }

        Spacer(Modifier.width(8.dp))

        Column(
            modifier = Modifier
                .padding(top = 4.dp, bottom = 18.dp)
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                time,
                color = if (faded) InkMuted else InkSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.3.sp,
            )
            Text(
                title,
                color = if (faded) InkMuted else InkPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(note, color = InkSecondary, fontSize = 12.sp, lineHeight = 18.sp)
        }
    }
}

/* ─────────────────────────────────────────────────────────────────────── *
 *  CHECKLIST · ATTACHMENTS · NOTES · BOTTOM ACTIONS
 * ─────────────────────────────────────────────────────────────────────── */

private data class ChecklistItem(val text: String, val done: Boolean)

@Composable
private fun ChecklistRow(item: ChecklistItem, onToggle: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onToggle)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        com.example.uniwattelektrik.core.components.DsCheckbox(
            checked  = item.done,
            onChange = { onToggle() },
            size     = 20.dp,
        )
        Spacer(Modifier.width(12.dp))
        Text(
            item.text,
            color = if (item.done) InkMuted else InkPrimary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            textDecoration = if (item.done) TextDecoration.LineThrough else TextDecoration.None,
        )
    }
}

@Composable
private fun AttachmentTile(tint: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .height(82.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(tint.copy(alpha = 0.20f))
            .clickable {},
        contentAlignment = Alignment.Center,
    ) {
        Icon(Icons.Filled.Image, null, tint = tint, modifier = Modifier.size(28.dp))
    }
}

private data class NoteItem(
    val author: String, val initials: String, val tint: Color,
    val timestamp: String, val message: String,
    /** Firebase Storage URL of an attached voice note. Renders a playable
     *  bubble when present; suppresses the [message] text block when blank. */
    val voiceUrl: String? = null,
    val voiceDurationMs: Long? = null,
)

@Composable
private fun NoteRow(
    note: NoteItem,
    player: com.example.uniwattelektrik.core.platform.VoicePlayer? = null,
) {
    Row {
        Box(
            modifier = Modifier
                .size(36.dp).clip(CircleShape).background(note.tint),
            contentAlignment = Alignment.Center,
        ) {
            Text(note.initials, color = Color.White,
                 fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(note.author, color = InkPrimary,
                     fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.width(8.dp))
                Text(note.timestamp, color = InkMuted,
                     fontSize = 11.sp, fontWeight = FontWeight.Medium)
            }
            Spacer(Modifier.height(2.dp))
            if (note.message.isNotBlank()) {
                Text(note.message, color = InkSecondary, fontSize = 13.sp,
                     lineHeight = 19.sp)
            }
            if (!note.voiceUrl.isNullOrBlank() && player != null) {
                if (note.message.isNotBlank()) Spacer(Modifier.height(6.dp))
                VoiceBubble(
                    url        = note.voiceUrl,
                    durationMs = note.voiceDurationMs ?: 0L,
                    tint       = note.tint,
                    player     = player,
                )
            }
        }
    }
}

@Composable
private fun VoiceBubble(
    url       : String,
    durationMs: Long,
    tint      : Color,
    player    : com.example.uniwattelektrik.core.platform.VoicePlayer,
) {
    val isCurrent = player.currentUrl == url
    val isPlaying = isCurrent && player.isPlaying
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(tint.copy(alpha = 0.10f))
            .border(1.dp, tint.copy(alpha = 0.25f), RoundedCornerShape(14.dp))
            .clickable {
                if (isPlaying) player.pause() else player.play(url)
            }
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier = Modifier.size(28.dp).clip(CircleShape).background(tint),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                if (isPlaying) "❚❚" else "▶",
                color = Color.White,
                fontSize = if (isPlaying) 10.sp else 12.sp,
                fontWeight = FontWeight.Bold,
            )
        }
        Text(
            "Voice note",
            color = InkPrimary,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.width(2.dp))
        Text(
            formatVoiceDuration(durationMs),
            color = InkSecondary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}

private fun formatVoiceDuration(ms: Long): String {
    if (ms <= 0L) return "0:00"
    val totalSec = (ms / 1000L).toInt()
    val m = totalSec / 60
    val s = totalSec % 60
    return "$m:${s.toString().padStart(2, '0')}"
}

@Composable
private fun ComposerRow(
    value: String,
    onChange: (String) -> Unit,
    onSend: () -> Unit,
    isRecording: Boolean = false,
    isUploading: Boolean = false,
    recordingMs: Long = 0L,
    onStartRecording: () -> Unit = {},
    onStopRecording: () -> Unit = {},
    onCancelRecording: () -> Unit = {},
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(ScreenBg)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (isRecording) {
            // ── Recording state — replaces the text input with a live timer
            //    + cancel + send-voice controls. Keeps the composer at one
            //    consistent vertical rhythm whether typing or recording.
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(Danger),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                "Recording  ·  ${formatVoiceDuration(recordingMs)}",
                color = Danger,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
            )
            Box(
                modifier = Modifier
                    .size(34.dp).clip(CircleShape)
                    .background(InkMuted.copy(alpha = 0.25f))
                    .clickable(onClick = onCancelRecording),
                contentAlignment = Alignment.Center,
            ) {
                Text("✕", color = InkSecondary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .size(34.dp).clip(CircleShape)
                    .background(Brand)
                    .clickable(onClick = onStopRecording),
                contentAlignment = Alignment.Center,
            ) {
                // Send (▶ rotated as paper-plane proxy)
                Icon(Icons.Filled.Add, null, tint = Color.White, modifier = Modifier.size(18.dp))
            }
        } else if (isUploading) {
            // Brief "uploading voice note" state — sits between Stop and the
            // moment the Firestore listener delivers the new note. A small
            // inline spinner so it feels responsive without blocking the UI.
            CircularProgressIndicator(
                color       = Brand,
                strokeWidth = 2.dp,
                modifier    = Modifier.size(16.dp),
            )
            Spacer(Modifier.width(10.dp))
            Text(
                "Uploading voice note…",
                color = InkSecondary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f),
            )
        } else {
            Box(modifier = Modifier.weight(1f)) {
                if (value.isEmpty()) {
                    Text("Add a note…", color = InkMuted, fontSize = 13.sp)
                }
                BasicTextField(
                    value = value,
                    onValueChange = onChange,
                    textStyle = TextStyle(color = InkPrimary, fontSize = 13.sp),
                    cursorBrush = SolidColor(Brand),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Spacer(Modifier.width(8.dp))
            // Mic button only shows while there's nothing typed — once the
            // user starts typing the mic gives way to the send button. Mirrors
            // common chat patterns (WhatsApp / Telegram).
            if (value.isBlank()) {
                Box(
                    modifier = Modifier
                        .size(34.dp).clip(CircleShape)
                        .background(Brand.copy(alpha = 0.85f))
                        .clickable(onClick = onStartRecording),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Filled.Mic,
                        contentDescription = "Record voice note",
                        tint               = Color.White,
                        modifier           = Modifier.size(18.dp),
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .size(34.dp).clip(CircleShape)
                        .background(Brand)
                        .clickable(onClick = onSend),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Filled.Add, null, tint = Color.White, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@Composable
private fun BottomAction(
    icon: ImageVector,
    label: String,
    tint: Color,
    bg: Color,
    modifier: Modifier = Modifier,
    labelColor: Color = InkPrimary,
    onClick: () -> Unit = {},
) {
    Column(
        modifier = modifier
            .height(78.dp)
            .shadow(8.dp, RoundedCornerShape(16.dp), spotColor = ShadowSoft)
            .clip(RoundedCornerShape(16.dp))
            .background(CardBg)
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(bg),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, null, tint = tint, modifier = Modifier.size(18.dp))
        }
        Text(label, color = labelColor, fontSize = 12.sp,
             fontWeight = FontWeight.SemiBold)
    }
}

/* ─────────────────────────────────────────────────────────────────────── *
 *  PREMIUM USER TASK ACTION BAR
 * ─────────────────────────────────────────────────────────────────────── */

/**
 * Premium gradient action bar shown at the bottom of TaskDetailScreen for
 * User-role viewers. Buttons map directly to Kanban status transitions:
 *
 *  Todo      → "Accept" (→ InProgress) + "Escalate" (→ InReview)
 *  InProgress → "Completed" (→ Done via sign-off flow) + "Escalate" (→ InReview)
 *  InReview  → "Completed" (→ Done via sign-off flow)
 *  Done      → "Reopen" (→ Todo)
 */
@Composable
private fun UserTaskActionBar(
    status: String,
    taskId: String,
    adminUid: String,
    userId: String?,
    workforceVm: WorkforceViewModel?,
    locationProvider: LocationProvider,
    scope: kotlinx.coroutines.CoroutineScope,
    onStartWork: (String) -> Unit,
    siteLat: Double? = null,
    siteLng: Double? = null,
    siteLabel: String = "",
) {
    val mapLauncher = remember { com.example.uniwattelektrik.platform.LinkLauncher() }
    // Per-action busy flag. Set the moment a transition button is tapped and
    // cleared automatically as soon as the live Firestore status flips to a
    // different value — so the spinner naturally disappears the instant the
    // new action bar (e.g. "Mark Completed") replaces this row. Keyed on
    // taskId so navigating between tasks resets cleanly.
    var pendingAction by remember(taskId) { mutableStateOf<String?>(null) }
    // Reassign sheet visibility — separate from pendingAction because the
    // sheet can be opened/cancelled without ever firing a write.
    var showReassignSheet by remember(taskId) { mutableStateOf(false) }
    val employees by (workforceVm?.employees
        ?: remember { kotlinx.coroutines.flow.MutableStateFlow(emptyList()) })
        .collectAsStateWithLifecycle()
    // Whenever the live status changes (snapshot listener fired), clear the
    // busy state — the UI is now reflecting the server truth.
    LaunchedEffect(status, taskId) { pendingAction = null }
    // Safety net: if Firestore takes longer than 8 s (poor network), drop the
    // spinner so the user can retry instead of being stuck.
    LaunchedEffect(pendingAction, taskId) {
        if (pendingAction != null) {
            kotlinx.coroutines.delay(8000)
            pendingAction = null
        }
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        when (status) {
            "Todo" -> {
                // Primary: Accept → InProgress
                PremiumActionButton(
                    label     = "Accept Task",
                    icon      = Icons.Filled.PlayArrow,
                    gradient  = listOf(Color(0xFF3B82F6), Color(0xFF1D4ED8)),
                    modifier  = Modifier.fillMaxWidth(),
                    loading   = pendingAction == "accept",
                    onClick   = {
                        pendingAction = "accept"
                        scope.launch {
                            val loc = runCatching { locationProvider.getCurrentLocation() }.getOrNull()
                            workforceVm?.acceptTask(
                                taskId = taskId,
                                lat    = loc?.latitude,
                                lon    = loc?.longitude,
                            )
                        }
                    },
                )
                // Secondary: Escalate → InReview
                PremiumActionButton(
                    label     = "Escalate to Review",
                    icon      = Icons.Filled.Upload,
                    gradient  = listOf(Color(0xFFF59E0B), Color(0xFFD97706)),
                    modifier  = Modifier.fillMaxWidth(),
                    loading   = pendingAction == "escalate",
                    onClick   = {
                        pendingAction = "escalate"
                        workforceVm?.changeTaskStatus(
                            taskId    = taskId,
                            adminUid  = adminUid,
                            userId    = userId,
                            newStatus = "InReview",
                        )
                    },
                )
            }

            "InProgress" -> {
                // Quick "Navigate to Site" — only shown when we actually have
                // coordinates on this task. Tapping deep-links into the user's
                // preferred maps app (Google Maps on Android, Apple Maps on
                // iOS) pinned at the site so the engineer can drive straight
                // there from the task screen.
                if (siteLat != null && siteLng != null) {
                    PremiumActionButton(
                        label    = "Navigate to Site",
                        icon     = Icons.Filled.LocationOn,
                        gradient = listOf(Color(0xFF06B6D4), Color(0xFF0E7490)),
                        modifier = Modifier.fillMaxWidth(),
                        onClick  = {
                            mapLauncher.openMap(siteLat, siteLng, label = siteLabel)
                        },
                    )
                }
                // Primary: Completed → Done (sign-off flow). No spinner here —
                // this opens the sign-off sheet rather than writing to Firestore
                // directly, so feedback is immediate.
                PremiumActionButton(
                    label    = "Mark Completed",
                    icon     = Icons.Filled.Check,
                    gradient = listOf(Color(0xFF22C55E), Color(0xFF15803D)),
                    modifier = Modifier.fillMaxWidth(),
                    onClick  = { onStartWork(taskId) },
                )
                // Secondary: Escalate → InReview
                PremiumActionButton(
                    label    = "Escalate to Review",
                    icon     = Icons.Filled.Upload,
                    gradient = listOf(Color(0xFFF59E0B), Color(0xFFD97706)),
                    modifier = Modifier.fillMaxWidth(),
                    loading  = pendingAction == "escalate",
                    onClick  = {
                        pendingAction = "escalate"
                        workforceVm?.changeTaskStatus(
                            taskId    = taskId,
                            adminUid  = adminUid,
                            userId    = userId,
                            newStatus = "InReview",
                        )
                    },
                )
            }

            "InReview" -> {
                // Primary: Completed → Done (sign-off flow)
                PremiumActionButton(
                    label    = "Mark Completed",
                    icon     = Icons.Filled.Check,
                    gradient = listOf(Color(0xFF22C55E), Color(0xFF15803D)),
                    modifier = Modifier.fillMaxWidth(),
                    onClick  = { onStartWork(taskId) },
                )
                // Secondary: Reassign to another user — opens a picker sheet.
                PremiumActionButton(
                    label    = "Reassign",
                    icon     = Icons.Filled.Person,
                    gradient = listOf(Color(0xFF6366F1), Color(0xFF4338CA)),
                    modifier = Modifier.fillMaxWidth(),
                    loading  = pendingAction == "reassign",
                    onClick  = { showReassignSheet = true },
                )
            }

            "Done" -> {
                // Reopen → Todo
                PremiumActionButton(
                    label    = "Reopen Task",
                    icon     = Icons.Filled.Refresh,
                    gradient = listOf(Color(0xFF64748B), Color(0xFF334155)),
                    modifier = Modifier.fillMaxWidth(),
                    loading  = pendingAction == "reopen",
                    onClick  = {
                        pendingAction = "reopen"
                        workforceVm?.changeTaskStatus(
                            taskId    = taskId,
                            adminUid  = adminUid,
                            userId    = userId,
                            newStatus = "Todo",
                        )
                    },
                )
            }
        }
        Spacer(Modifier.height(4.dp))
    }

    // ── Reassign bottom sheet ────────────────────────────────────────────
    if (showReassignSheet) {
        ReassignSheet(
            employees       = employees.filter { it.deletedAt == null && it.status != "Inactive" && it.id != userId },
            onDismiss       = { showReassignSheet = false },
            onPick          = { emp ->
                showReassignSheet = false
                pendingAction = "reassign"
                workforceVm?.reassignTask(
                    taskId           = taskId,
                    adminUid         = adminUid,
                    newAssigneeIds   = listOf(emp.id),
                    newAssigneeNames = listOf(emp.name),
                )
            },
        )
    }
}

/**
 * Bottom-sheet user picker used when the assigned engineer wants to hand
 * off an InReview task to a teammate. Single-select; tap to confirm.
 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun ReassignSheet(
    employees: List<com.example.uniwattelektrik.feature.workforce.data.remote.EmployeeRecord>,
    onDismiss: () -> Unit,
    onPick: (com.example.uniwattelektrik.feature.workforce.data.remote.EmployeeRecord) -> Unit,
) {
    androidx.compose.material3.ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor   = Color.White,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
        ) {
            Text(
                "Reassign task",
                color      = InkPrimary,
                fontSize   = 18.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                "Pick a teammate to take over this task.",
                color    = InkSecondary,
                fontSize = 13.sp,
            )
            Spacer(Modifier.height(12.dp))
            if (employees.isEmpty()) {
                Text(
                    "No other active teammates available.",
                    color    = InkSecondary,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(vertical = 16.dp),
                )
            } else {
                employees.forEach { emp ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onPick(emp) }
                            .padding(vertical = 10.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Brand.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                Icons.Filled.Person,
                                contentDescription = null,
                                tint = Brand,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(emp.name, color = InkPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                            if (emp.role.isNotBlank()) {
                                Text(emp.role, color = InkSecondary, fontSize = 12.sp)
                            }
                        }
                        Text(
                            "${emp.tasksOpen} open",
                            color    = InkSecondary,
                            fontSize = 12.sp,
                        )
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

/**
 * Premium full-width gradient button with icon + label, rounded corners,
 * and a multi-layer drop shadow. Used exclusively in [UserTaskActionBar].
 */
@Composable
private fun PremiumActionButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    gradient: List<Color>,
    modifier: Modifier = Modifier,
    loading: Boolean = false,
    onClick: () -> Unit = {},
) {
    Row(
        modifier = modifier
            .height(52.dp)
            .shadow(12.dp, RoundedCornerShape(16.dp), spotColor = gradient.last().copy(alpha = 0.40f))
            .clip(RoundedCornerShape(16.dp))
            .background(Brush.horizontalGradient(gradient))
            .clickable(enabled = !loading, onClick = onClick)
            .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.20f)),
            contentAlignment = Alignment.Center,
        ) {
            if (loading) {
                CircularProgressIndicator(
                    color       = Color.White,
                    strokeWidth = 2.dp,
                    modifier    = Modifier.size(16.dp),
                )
            } else {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
        Spacer(Modifier.width(10.dp))
        Text(
            if (loading) "Please wait…" else label,
            color = Color.White,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.3.sp,
        )
    }
}

/* ─────────────────────────────────────────────────────────────────────── *
 *  HELPERS
 * ─────────────────────────────────────────────────────────────────────── */

/** Adapts a real Firestore [TaskRecord] to the SampleTask shape used by this screen. */
private fun sampleFromRecord(t: TaskRecord): com.example.uniwattelektrik.core.sample.SampleTask {
    val priority = when (t.priority.lowercase()) {
        "high"   -> com.example.uniwattelektrik.core.sample.TaskPriority.Danger
        "low"    -> com.example.uniwattelektrik.core.sample.TaskPriority.Success
        else     -> com.example.uniwattelektrik.core.sample.TaskPriority.Warning
    }
    val status = when (t.status) {
        "InProgress" -> com.example.uniwattelektrik.core.sample.TaskStatus.InProgress
        "InReview"   -> com.example.uniwattelektrik.core.sample.TaskStatus.InReview
        "Done"       -> com.example.uniwattelektrik.core.sample.TaskStatus.Done
        else         -> com.example.uniwattelektrik.core.sample.TaskStatus.Todo
    }
    val code = "TASK-${(t.id.hashCode().absoluteValue % 9000) + 1000}"
    return com.example.uniwattelektrik.core.sample.SampleTask(
        id = code,
        title = t.title,
        // Prefer the admin-authored description when present, fall back to a
        // synthesized line so older records (no description field) still render.
        description = t.description.ifBlank {
            "Site work scheduled at ${t.location}. Follow service schedule."
        },
        location = t.location.ifBlank { "—" },
        distanceKm = 0.0,
        time = t.time,
        day = t.day,
        priority = priority,
        status = status,
        assigneeInitials = t.assigneeInitials.ifBlank { "??" },
        latitude  = t.latitude,
        longitude = t.longitude,
    )
}

private fun statusColor(status: String): Color = when (status.lowercase()) {
    "completed", "done"      -> Success
    "in progress", "active"  -> Brand
    "to do", "queued"        -> Color(0xFF94A3B8)
    else                      -> Brand
}

/**
 * Human-friendly status label for the StatsCard. Splits camel-case codes coming
 * from the data layer (e.g. "InProgress" → "In Progress") so the value displays
 * naturally instead of looking like a constant.
 */
private fun humanStatusLabel(raw: String): String = when (raw.lowercase().replace(" ", "")) {
    "inprogress", "active"  -> "In Progress"
    "todo", "queued"        -> "To Do"
    "completed", "done"     -> "Done"
    else                    -> raw
}

/**
 * Formats a coordinate to 5 decimal places (≈1 m precision) without
 * relying on `String.format` (JVM-only). Manual rounding keeps the
 * implementation multiplatform.
 */
private fun formatLatLng5(v: Double): String {
    val rounded = kotlin.math.round(v * 100_000.0) / 100_000.0
    val s = rounded.toString()
    val dot = s.indexOf('.')
    return if (dot < 0) "$s.00000" else {
        val frac = s.substring(dot + 1)
        when {
            frac.length >= 5 -> s.substring(0, dot + 6)
            else             -> s + "0".repeat(5 - frac.length)
        }
    }
}

/* ─── Activity timeline builder ────────────────────────────────────────── */

private data class ActivityRow(
    val tint: Color,
    val time: String,
    val title: String,
    val note: String,
    val active: Boolean = false,
    val faded: Boolean = false,
)

private fun buildActivityRows(t: TaskRecord?): List<ActivityRow> {
    if (t == null) return emptyList()
    val rows = mutableListOf<ActivityRow>()

    // 1. Created
    rows += ActivityRow(
        tint  = InkMuted,
        time  = formatActivityTime(t.createdAtMs),
        title = "Task created",
        note  = if (t.ownerAdminName.isNotBlank()) "Created by ${t.ownerAdminName}" else "Created",
    )

    // 2. Assigned (always shown if a user is set)
    if (!t.userId.isNullOrBlank()) {
        rows += ActivityRow(
            tint  = Brand,
            time  = formatActivityTime(t.createdAtMs),
            title = "Assigned to ${t.assigneeName.ifBlank { t.assigneeInitials.ifBlank { "user" } }}",
            note  = "Awaiting acceptance from the assigned user",
        )
    }

    // 3. In progress (when accepted)
    val accepted = t.acceptedAt
    if (accepted != null && accepted > 0L) {
        val coords = if (t.acceptedLat != null && t.acceptedLon != null)
            "Location · ${formatLatLng4(t.acceptedLat)}, ${formatLatLng4(t.acceptedLon)}"
        else "Location not captured"
        rows += ActivityRow(
            tint  = Warning,
            time  = formatActivityTime(accepted),
            title = "In progress",
            note  = coords,
            active = t.status == "InProgress",
        )
    }

    // 4. Completion
    val done = t.completedAt
    if (done != null && done > 0L) {
        rows += ActivityRow(
            tint  = Success,
            time  = formatActivityTime(done),
            title = "Completed",
            note  = "Marked done by the assigned user",
        )
    } else {
        rows += ActivityRow(
            tint  = InkMuted,
            time  = "—",
            title = "Completion",
            note  = "Awaiting field sign-off",
            faded = true,
        )
    }
    return rows
}

private fun formatActivityTime(ms: Long?): String {
    if (ms == null || ms <= 0L) return "—"
    val ldt = kotlinx.datetime.Instant.fromEpochMilliseconds(ms)
        .toLocalDateTime(kotlinx.datetime.TimeZone.currentSystemDefault())
    val months = listOf("Jan","Feb","Mar","Apr","May","Jun","Jul","Aug","Sep","Oct","Nov","Dec")
    val day = ldt.dayOfMonth.toString().padStart(2, '0')
    val mon = months[ldt.monthNumber - 1]
    val hh  = ldt.hour.toString().padStart(2, '0')
    val mm  = ldt.minute.toString().padStart(2, '0')
    return "$day $mon · $hh:$mm"
}

private fun formatNoteTime(ms: Long?): String = formatActivityTime(ms)

private fun noteInitials(name: String): String {
    val parts = name.trim().split(" ", "_", ".", "-").filter { it.isNotBlank() }
    val initials = parts.take(2).mapNotNull { it.firstOrNull()?.uppercaseChar() }.joinToString("")
    return initials.ifBlank { "??" }
}

private fun formatLatLng4(value: Double): String {
    val rounded = kotlin.math.round(value * 10000.0) / 10000.0
    val s = rounded.toString()
    val dot = s.indexOf('.')
    return if (dot < 0) "$s.0000"
    else {
        val frac = s.substring(dot + 1)
        val padded = if (frac.length >= 4) frac.substring(0, 4) else frac.padEnd(4, '0')
        s.substring(0, dot) + "." + padded
    }
}

private fun priorityIconGradient(priority: String): List<Color> = when (priority.lowercase()) {
    "high"   -> listOf(Color(0xFFFB7185), Danger)
    "low"    -> listOf(Color(0xFF34D399), Success)
    else     -> listOf(Color(0xFFFBBF24), Warning)   // medium
}

/* ─────────────────────────────────────────────────────────────────────── *
 *  MATERIAL DETAILS + ATTACHMENTS (live data)
 * ─────────────────────────────────────────────────────────────────────── */

@Composable
private fun CompletionDetailRow(label: String, value: String) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            label.uppercase(),
            color = InkSecondary,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.8.sp,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            value,
            color = InkPrimary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            lineHeight = 20.sp,
        )
    }
}

/** Formats a duration in milliseconds to a human-readable string, e.g. "2 h 30 min". */
private fun formatDuration(ms: Long): String {
    val totalMin = ms / 60_000
    val hours = totalMin / 60
    val minutes = totalMin % 60
    return when {
        hours > 0 && minutes > 0 -> "$hours h $minutes min"
        hours > 0                 -> "$hours h"
        else                      -> "$minutes min"
    }
}

@Composable
private fun MaterialRow(label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(32.dp).clip(RoundedCornerShape(10.dp)).background(Brand50),
            contentAlignment = Alignment.Center,
        ) {
            Text("●", color = Brand, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(label.uppercase(), color = InkSecondary, fontSize = 10.sp,
                 fontWeight = FontWeight.SemiBold, letterSpacing = 0.8.sp)
            Spacer(Modifier.height(2.dp))
            Text(value, color = InkPrimary, fontSize = 14.sp,
                 fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun AttachmentsGrid(urls: List<String>, onClick: (String) -> Unit) {
    // Simple wrap into rows of 3
    val rows = urls.chunked(3)
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        rows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { url ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(82.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Brand50)
                            .clickable { onClick(url) },
                    ) {
                        coil3.compose.AsyncImage(
                            model = url,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                        )
                    }
                }
                // Pad remaining cells so the row aligns
                repeat(3 - row.size) {
                    Box(modifier = Modifier.weight(1f).height(82.dp))
                }
            }
        }
    }
}

/**
 * Two-button row that lets the user pick a photo from gallery or take a new
 * one with the camera. Shows a pulsing "Uploading…" strip while a file is
 * in-flight so the user knows the app is working.
 */
@Composable
private fun AttachmentPickerRow(
    isUploading: Boolean,
    onGallery: () -> Unit,
    onCamera: () -> Unit,
) {
    if (isUploading) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Brand50)
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(
                imageVector = Icons.Filled.Upload,
                contentDescription = null,
                tint = Brand,
                modifier = Modifier.size(16.dp),
            )
            Text(
                "Uploading photo…",
                color = Brand,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
    } else {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            // Gallery button
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Brand50)
                    .clickable(onClick = onGallery)
                    .padding(horizontal = 14.dp, vertical = 11.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.Image,
                    contentDescription = null,
                    tint = Brand,
                    modifier = Modifier.size(16.dp),
                )
                Text(
                    "Gallery",
                    color = Brand,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            // Camera button
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(SuccessBg)
                    .clickable(onClick = onCamera)
                    .padding(horizontal = 14.dp, vertical = 11.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = null,
                    tint = Success,
                    modifier = Modifier.size(16.dp),
                )
                Text(
                    "Camera",
                    color = Success,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

@Composable
private fun AttachmentFullscreenDialog(url: String, onDismiss: () -> Unit) {
    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xEE000000))
                .clickable(onClick = onDismiss),
            contentAlignment = Alignment.Center,
        ) {
            coil3.compose.AsyncImage(
                model = url,
                contentDescription = null,
                modifier = Modifier.fillMaxWidth(),
                contentScale = androidx.compose.ui.layout.ContentScale.Fit,
            )
        }
    }
}
