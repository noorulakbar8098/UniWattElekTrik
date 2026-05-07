package com.example.uniwattelektrik.feature.workforce.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.uniwattelektrik.core.AppLog
import com.example.uniwattelektrik.core.Resource
import com.example.uniwattelektrik.feature.auth.data.remote.EmployeeAuthClient
import com.example.uniwattelektrik.feature.workforce.data.remote.AttendanceRecord
import com.example.uniwattelektrik.feature.workforce.data.remote.CheckinPing
import com.example.uniwattelektrik.feature.workforce.data.remote.EmployeeDraft
import com.example.uniwattelektrik.feature.workforce.data.remote.EmployeeRecord
import com.example.uniwattelektrik.feature.workforce.data.remote.MaterialUsedItem
import com.example.uniwattelektrik.feature.workforce.data.remote.SpareItemRecord
import com.example.uniwattelektrik.feature.workforce.data.remote.TaskRecord
import com.example.uniwattelektrik.feature.workforce.data.remote.TaskNote
import com.example.uniwattelektrik.feature.workforce.data.remote.LeaveRecord
import com.example.uniwattelektrik.feature.workforce.data.remote.WorkforceDirectory
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

/**
 * Drives admin & user dashboards with **real-time Firestore data** scoped by
 * adminId/userId. Reads are subscribed via Firestore snapshot listeners (Flow),
 * so any add/update/delete reflects in the UI within ~100 ms — no manual refresh.
 *
 * Call once from a `LaunchedEffect`:
 *   - `loadForAdmin(adminUid)` — admin dashboards
 *   - `loadForUser(userUid)`  — employee dashboards
 *
 * Internally cancels any previous subscriptions on a new call.
 */
class WorkforceViewModel(
    private val directory: WorkforceDirectory,
    private val employeeAuthClient: EmployeeAuthClient,
) : ViewModel() {

    /** Keeps the last admin/user UID so resume-triggered refreshes can re-subscribe. */
    private var cachedAdminUid: String? = null
    private var cachedUserUid: String?  = null
    private var cachedUserAdminUid: String? = null

    /**
     * Call on app resume (lifecycle ON_RESUME) to briefly show the shimmer
     * and re-subscribe to Firestore, ensuring fresh data after the app was backgrounded.
     */
    fun refresh() {
        val adminUid = cachedAdminUid
        val userUid  = cachedUserUid
        when {
            adminUid != null -> loadForAdmin(adminUid)
            userUid  != null -> loadForUser(userUid, cachedUserAdminUid)
        }
    }

    private val _employees = MutableStateFlow<List<EmployeeRecord>>(emptyList())
    val employees: StateFlow<List<EmployeeRecord>> = _employees.asStateFlow()

    private val _tasks = MutableStateFlow<List<TaskRecord>>(emptyList())
    val tasks: StateFlow<List<TaskRecord>> = _tasks.asStateFlow()

    /** Live stream of all attendance records for the current admin scope. */
    private val _attendance = MutableStateFlow<List<AttendanceRecord>>(emptyList())
    val attendance: StateFlow<List<AttendanceRecord>> = _attendance.asStateFlow()

    /** Live stream of GPS pings under the admin scope (for the map view). */
    private val _checkins = MutableStateFlow<List<CheckinPing>>(emptyList())
    val checkins: StateFlow<List<CheckinPing>> = _checkins.asStateFlow()

    private val _spareItems = MutableStateFlow<List<SpareItemRecord>>(emptyList())
    val spareItems: StateFlow<List<SpareItemRecord>> = _spareItems.asStateFlow()

    private var spareItemsJob: Job? = null

    private val _leaveRequests = MutableStateFlow<List<LeaveRecord>>(emptyList())
    val leaveRequests: StateFlow<List<LeaveRecord>> = _leaveRequests.asStateFlow()

    private var leaveRequestsJob: Job? = null

    private val _notifications = MutableStateFlow<List<com.example.uniwattelektrik.feature.workforce.data.remote.NotificationRecord>>(emptyList())
    val notifications: StateFlow<List<com.example.uniwattelektrik.feature.workforce.data.remote.NotificationRecord>> = _notifications.asStateFlow()

    private var notificationsJob: Job? = null

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    /** Set an error and auto-clear it after 4 seconds so the banner doesn't stick. */
    private fun setError(message: String?) {
        _error.value = message
        if (message != null) {
            viewModelScope.launch {
                kotlinx.coroutines.delay(4_000L)
                if (_error.value == message) _error.value = null
            }
        }
    }

    fun clearError() { _error.value = null }

    // Track active subscriptions so a re-call cancels the previous listeners.
    private var employeesJob: Job? = null
    private var tasksJob: Job? = null
    private var attendanceJob: Job? = null
    private var checkinsJob: Job? = null

    fun loadForAdmin(adminUid: String) {
        cachedAdminUid  = adminUid
        cachedUserUid   = null
        // Cancel any previous subscriptions before re-binding.
        employeesJob?.cancel()
        tasksJob?.cancel()
        attendanceJob?.cancel()
        spareItemsJob?.cancel()
        leaveRequestsJob?.cancel()
        _error.value = null
        _loading.value = true

        employeesJob = directory.observeEmployees(adminUid)
            .onEach {
                _employees.value = it
                _loading.value = false
            }
            .catch {
                setError(it.message)
                _loading.value = false
            }
            .launchIn(viewModelScope)

        tasksJob = directory.observeTasksForAdmin(adminUid)
            .onEach { _tasks.value = it }
            .catch { setError(it.message) }
            .launchIn(viewModelScope)

        attendanceJob = directory.observeAttendance(adminUid)
            .onEach { _attendance.value = it }
            .catch { setError(it.message) }
            .launchIn(viewModelScope)

        checkinsJob = directory.observeCheckins(adminUid)
            .onEach { _checkins.value = it }
            .catch { setError(it.message) }
            .launchIn(viewModelScope)

        spareItemsJob = directory.observeSpareItems(adminUid)
            .onEach { _spareItems.value = it }
            .catch { setError(it.message) }
            .launchIn(viewModelScope)

        leaveRequestsJob = directory.observeLeaveRequestsForAdmin(adminUid)
            .onEach { _leaveRequests.value = it }
            .catch { setError(it.message) }
            .launchIn(viewModelScope)
    }

    /**
     * Subscribe to data scoped to an employee. [adminUid] is the employee's
     * `parentAdminId` (the owning admin's Firebase UID) — required so the
     * attendance subscription reads from the same `admins/{adminId}/attendance`
     * collection the admin sees.
     */
    fun loadForUser(userUid: String, adminUid: String? = null) {
        cachedUserUid      = userUid
        cachedUserAdminUid = adminUid
        cachedAdminUid     = null
        employeesJob?.cancel()
        tasksJob?.cancel()
        attendanceJob?.cancel()
        spareItemsJob?.cancel()
        leaveRequestsJob?.cancel()
        _error.value = null
        _loading.value = true

        tasksJob = directory.observeTasksForUser(userUid)
            .onEach {
                _tasks.value = it
                _loading.value = false
            }
            .catch {
                setError(it.message)
                _loading.value = false
            }
            .launchIn(viewModelScope)

        if (!adminUid.isNullOrBlank()) {
            attendanceJob = directory.observeAttendance(adminUid)
                .onEach { _attendance.value = it }
                .catch { setError(it.message) }
                .launchIn(viewModelScope)

            employeesJob = directory.observeEmployees(adminUid)
                .onEach { _employees.value = it }
                .catch { setError(it.message) }
                .launchIn(viewModelScope)

            spareItemsJob = directory.observeSpareItems(adminUid)
                .onEach { _spareItems.value = it }
                .catch { setError(it.message) }
                .launchIn(viewModelScope)
        }

        leaveRequestsJob = directory.observeLeaveRequestsForUser(userUid)
            .onEach { _leaveRequests.value = it }
            .catch { setError(it.message) }
            .launchIn(viewModelScope)

        if (!adminUid.isNullOrBlank()) {
            notificationsJob?.cancel()
            notificationsJob = directory.observeNotifications(adminUid, userUid)
                .onEach { _notifications.value = it.sortedByDescending { n -> n.createdAt ?: 0L } }
                .catch { setError(it.message) }
                .launchIn(viewModelScope)
        }
    }

    /**
     * Records a check-in for [userId] under [adminUid]'s attendance collection.
     * Returns the created [AttendanceRecord] via [onDone] so callers can store
     * its id (used later by [markCheckOut]).
     *
     * [lat] / [lng] are the captured GPS coordinates (null when location
     * unavailable). [checkInStatus] is `"ON_TIME"` or `"LATE"` — derived by
     * the caller from the user's shift configuration.
     */
    fun markCheckIn(
        adminUid: String,
        userId: String,
        lat: Double? = null,
        lng: Double? = null,
        checkInStatus: String = "ON_TIME",
        onDone: (Result<AttendanceRecord>) -> Unit = {},
    ) {
        viewModelScope.launch {
            val result = runCatching {
                directory.markCheckIn(adminUid, userId, lat, lng, checkInStatus)
            }
            result.onFailure {
                AppLog.w("WorkforceVM", "markCheckIn failed: ${it.message}")
                setError(it.message)
            }
            onDone(result)
        }
    }

    /** Records a GPS ping for the current user — used when they check in. */
    fun recordCheckinPing(
        adminUid: String,
        userId: String,
        latitude: Double,
        longitude: Double,
    ) {
        viewModelScope.launch {
            runCatching { directory.recordCheckIn(adminUid, userId, latitude, longitude) }
                .onFailure { AppLog.w("WorkforceVM", "recordCheckinPing failed: ${it.message}") }
        }
    }

    /** Updates an existing attendance doc with `checkOut = serverTimestamp`
     *  and the captured check-out coordinates (may be null). */
    fun markCheckOut(
        adminUid: String,
        userId: String,
        attendanceId: String,
        lat: Double? = null,
        lng: Double? = null,
        onDone: (Result<Unit>) -> Unit = {},
    ) {
        viewModelScope.launch {
            val result = runCatching {
                directory.markCheckOut(adminUid, userId, attendanceId, lat, lng)
            }
            result.onFailure {
                AppLog.w("WorkforceVM", "markCheckOut failed: ${it.message}")
                setError(it.message)
            }
            onDone(result)
        }
    }

    /**
     * Two-step write:
     *   1. Create a Firebase Auth account on a *secondary* app so the admin
     *      stays signed in.
     *   2. Persist the Firestore profile under `admins/{adminUid}/users/{uid}`
     *      using the new uid as the doc id.
     *
     * `onDone` returns the created [EmployeeRecord] on success so the UI can
     * show a "Share via WhatsApp" card with the temp credentials.
     */
    fun addEmployee(
        adminUid: String,
        draft: EmployeeDraft,
        onDone: (Result<EmployeeRecord>) -> Unit = {},
    ) {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            val auth = employeeAuthClient.createEmployee(
                email = draft.email,
                password = draft.password,
                displayName = draft.name,
            )
            when (auth) {
                is Resource.Failure -> {
                    AppLog.w("WorkforceVM", "createEmployee auth failed: ${auth.error.message}")
                    _error.value = auth.error.message
                    _loading.value = false
                    onDone(Result.failure(IllegalStateException(auth.error.message)))
                    return@launch
                }
                is Resource.Success -> {
                    val uid = auth.data
                    val result = runCatching { directory.addEmployee(adminUid, uid, draft) }
                    result.onFailure {
                        AppLog.w("WorkforceVM", "addEmployee firestore failed: ${it.message}")
                        setError(it.message)
                    }
                    _loading.value = false
                    onDone(result)
                }
            }
        }
    }

    fun addTask(
        adminUid: String,
        userId: String?,
        title: String,
        location: String,
        time: String,
        day: String,
        priority: String,
        description: String = "",
        departmentId: String = "",
        departmentName: String = "",
        equipmentId: String = "",
        equipmentName: String = "",
        checklist: List<com.example.uniwattelektrik.feature.workforce.data.remote.ChecklistItem> = emptyList(),
        attachments: List<String> = emptyList(),
        address: String = "",
        latitude: Double? = null,
        longitude: Double? = null,
        dueDate: Long? = null,
        ownerAdminName: String = "",
        assigneeName: String = "",
    ) {
        viewModelScope.launch {
            runCatching {
                directory.addTask(
                    adminUid, userId, title, location, time, day, priority,
                    description,
                    departmentId, departmentName, equipmentId, equipmentName,
                    checklist, attachments, address, latitude, longitude,
                    dueDate, ownerAdminName, assigneeName,
                )
            }.onFailure { setError(it.message) }
        }
    }

    /**
     * Update an existing task. Mirrors [addTask] but writes via
     * [WorkforceDirectory.updateTask] which preserves lifecycle fields
     * (createdAt / acceptedAt / completedAt) and rebalances `tasksOpen`
     * counters when the assignee changes.
     */
    fun updateTask(
        taskId: String,
        adminUid: String,
        userId: String?,
        title: String,
        location: String,
        time: String,
        day: String,
        priority: String,
        description: String = "",
        departmentId: String = "",
        departmentName: String = "",
        equipmentId: String = "",
        equipmentName: String = "",
        checklist: List<com.example.uniwattelektrik.feature.workforce.data.remote.ChecklistItem> = emptyList(),
        attachments: List<String> = emptyList(),
        address: String = "",
        latitude: Double? = null,
        longitude: Double? = null,
        dueDate: Long? = null,
        assigneeName: String = "",
    ) {
        viewModelScope.launch {
            runCatching {
                directory.updateTask(
                    taskId, adminUid, userId, title, location, time, day, priority,
                    description,
                    departmentId, departmentName, equipmentId, equipmentName,
                    checklist, attachments, address, latitude, longitude,
                    dueDate, assigneeName,
                )
            }.onFailure { setError(it.message) }
        }
    }

    /** Update an employee's employment status (e.g. "Relieved", "active", "onleave"). */
    fun updateEmployeeStatus(adminId: String, employeeId: String, status: String) {
        viewModelScope.launch {
            runCatching { directory.updateEmployeeStatus(adminId, employeeId, status) }
                .onFailure {
                    AppLog.w("WorkforceVM", "updateEmployeeStatus failed: ${it.message}")
                    setError(it.message)
                }
        }
    }

    /** Mark a task as accepted by the assigned user — flips status to InProgress. */
    fun acceptTask(taskId: String, lat: Double? = null, lon: Double? = null) {
        viewModelScope.launch {
            runCatching { directory.acceptTask(taskId, lat, lon) }
                .onFailure {
                    AppLog.w("WorkforceVM", "acceptTask failed: ${it.message}")
                    setError(it.message)
                }
        }
    }

    /** Admin-facing: flip a task to any explicit workflow status without a full edit round-trip. */
    fun changeTaskStatus(taskId: String, adminUid: String, userId: String?, newStatus: String) {
        viewModelScope.launch {
            runCatching { directory.updateTaskStatus(adminUid, taskId, userId, newStatus) }
                .onFailure {
                    AppLog.w("WorkforceVM", "changeTaskStatus failed: ${it.message}")
                    setError(it.message)
                }
        }
    }

    /** Mark a task as completed (status="Done"). Decrements assignee's tasksOpen. */
    fun completeTask(adminUid: String, taskId: String, assignedUserId: String?) {
        viewModelScope.launch {
            runCatching { directory.updateTaskStatus(adminUid, taskId, assignedUserId, "Done") }
                .onFailure {
                    AppLog.w("WorkforceVM", "completeTask failed: ${it.message}")
                    setError(it.message)
                }
        }
    }

    fun completeTaskWithSignoff(
        adminId: String,
        taskId: String,
        assignedUserId: String,
        signoffDescription: String,
        downtimeMinutes: Int,
        rca: String,
        materialsUsed: List<MaterialUsedItem>,
        startTimeMs: Long,
        endTimeMs: Long,
        onDone: (Result<TaskRecord>) -> Unit = {},
    ) {
        viewModelScope.launch {
            val result = runCatching {
                directory.completeTaskWithSignoff(
                    adminId, taskId, assignedUserId,
                    signoffDescription, downtimeMinutes, rca,
                    materialsUsed, startTimeMs, endTimeMs,
                )
            }
            result.onFailure { AppLog.w("WorkforceVM", "completeTaskWithSignoff failed: ${it.message}") }
            onDone(result)
        }
    }

    // ─── Task notes (chat) ───────────────────────────────────────────────────

    private val noteFlows = mutableMapOf<String, MutableStateFlow<List<TaskNote>>>()
    private val noteJobs  = mutableMapOf<String, Job>()

    /** Returns a hot StateFlow of notes for [taskId]. Subscribes lazily. */
    fun observeTaskNotes(taskId: String): StateFlow<List<TaskNote>> {
        val existing = noteFlows[taskId]
        if (existing != null) return existing.asStateFlow()
        val flow = MutableStateFlow<List<TaskNote>>(emptyList())
        noteFlows[taskId] = flow
        noteJobs[taskId] = directory.observeTaskNotes(taskId)
            .onEach { flow.value = it }
            .catch { AppLog.w("WorkforceVM", "observeTaskNotes($taskId) failed: ${it.message}") }
            .launchIn(viewModelScope)
        return flow.asStateFlow()
    }

    fun addTaskNote(
        taskId: String,
        authorId: String,
        authorName: String,
        role: String,
        message: String,
    ) {
        if (message.isBlank()) return
        viewModelScope.launch {
            runCatching {
                directory.addTaskNote(taskId, authorId, authorName, role, message.trim())
            }.onFailure {
                AppLog.w("WorkforceVM", "addTaskNote failed: ${it.message}")
                setError(it.message)
            }
        }
    }

    /** Toggle a checklist item on the given task and persist (live listener will re-emit). */
    fun toggleChecklistItem(taskId: String, index: Int, done: Boolean) {
        viewModelScope.launch {
            runCatching { directory.setChecklistItemDone(taskId, index, done) }
                .onFailure { setError(it.message) }
        }
    }

    /**
     * Upload a single picked image (content://… URI) to Firebase Storage and
     * return its download URL via [onDone]. Used by the New Task screen so
     * attachments are persistable as plain URL strings on the TaskRecord.
     */
    fun uploadAttachment(
        adminId: String,
        contentUri: String,
        onDone: (Result<String>) -> Unit = {},
    ) {
        viewModelScope.launch {
            val result = runCatching { directory.uploadTaskAttachment(adminId, contentUri) }
            result.onFailure {
                AppLog.w("WorkforceVM", "uploadAttachment failed: ${it.message}")
                setError(it.message)
            }
            onDone(result)
        }
    }

    /** Replace the attachments list on an existing task. */
    fun updateAttachments(taskId: String, urls: List<String>) {
        viewModelScope.launch {
            runCatching { directory.updateTaskAttachments(taskId, urls) }
                .onFailure { setError(it.message) }
        }
    }

    // ─── Leave requests ──────────────────────────────────────────────────────

    fun submitLeaveRequest(
        adminId: String,
        userId: String,
        employeeName: String,
        department: String,
        leaveType: String,
        fromDateMs: Long,
        toDateMs: Long,
        totalDays: Int,
        reason: String,
        onDone: (Result<LeaveRecord>) -> Unit = {},
    ) {
        viewModelScope.launch {
            _loading.value = true
            val result = runCatching {
                directory.submitLeaveRequest(
                    adminId, userId, employeeName, department,
                    leaveType, fromDateMs, toDateMs, totalDays, reason,
                )
            }
            result.onFailure {
                AppLog.w("WorkforceVM", "submitLeaveRequest failed: ${it.message}")
                setError(it.message)
            }
            _loading.value = false
            onDone(result)
        }
    }

    fun approveLeave(leaveId: String, adminId: String, userId: String) {
        viewModelScope.launch {
            runCatching { directory.updateLeaveStatus(leaveId, adminId, userId, "approved") }
                .onFailure {
                    AppLog.w("WorkforceVM", "approveLeave failed: ${it.message}")
                    setError(it.message)
                }
        }
    }

    fun rejectLeave(
        leaveId: String,
        adminId: String,
        userId: String,
        rejectionReason: String = "",
    ) {
        viewModelScope.launch {
            runCatching {
                directory.updateLeaveStatus(leaveId, adminId, userId, "rejected", rejectionReason)
            }.onFailure {
                AppLog.w("WorkforceVM", "rejectLeave failed: ${it.message}")
                setError(it.message)
            }
        }
    }


    // ─── Danger zone ─────────────────────────────────────────────────────────

    private val _deleteAllState = MutableStateFlow<DeleteAllState>(DeleteAllState.Idle)
    val deleteAllState: StateFlow<DeleteAllState> = _deleteAllState.asStateFlow()

    fun deleteAllData(adminId: String) {
        viewModelScope.launch {
            _deleteAllState.value = DeleteAllState.Loading
            runCatching { directory.deleteAllData(adminId) }
                .onSuccess { _deleteAllState.value = DeleteAllState.Success }
                .onFailure { _deleteAllState.value = DeleteAllState.Error(it.message ?: "Delete failed") }
        }
    }

    fun clearDeleteAllState() { _deleteAllState.value = DeleteAllState.Idle }

    // ── Notifications ──────────────────────────────────────────────────────────

    fun markNotificationRead(notificationId: String) {
        viewModelScope.launch {
            runCatching { directory.markNotificationRead(notificationId) }
        }
    }

    fun markAllNotificationsRead() {
        val unread = _notifications.value.filter { !it.isRead }
        unread.forEach { n ->
            viewModelScope.launch {
                runCatching { directory.markNotificationRead(n.id) }
            }
        }
    }
}

sealed interface DeleteAllState {
    data object Idle    : DeleteAllState
    data object Loading : DeleteAllState
    data object Success : DeleteAllState
    data class  Error(val message: String) : DeleteAllState
}
