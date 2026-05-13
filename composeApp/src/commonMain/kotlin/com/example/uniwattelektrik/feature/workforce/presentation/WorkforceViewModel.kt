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

    // Phase 2 / Case 2 — inventory transaction stream. Powers the reports'
    // "consumption from tasks vs manual" split and any future stock-movement
    // analytics. Admin-scope only; users don't subscribe.
    private val _inventoryTxns = MutableStateFlow<List<com.example.uniwattelektrik.feature.workforce.data.remote.InventoryTransaction>>(emptyList())
    val inventoryTxns: StateFlow<List<com.example.uniwattelektrik.feature.workforce.data.remote.InventoryTransaction>> =
        _inventoryTxns.asStateFlow()

    private var inventoryTxnsJob: Job? = null

    private val _leaveRequests = MutableStateFlow<List<LeaveRecord>>(emptyList())
    val leaveRequests: StateFlow<List<LeaveRecord>> = _leaveRequests.asStateFlow()

    private var leaveRequestsJob: Job? = null

    private val _notifications = MutableStateFlow<List<com.example.uniwattelektrik.feature.workforce.data.remote.NotificationRecord>>(emptyList())
    val notifications: StateFlow<List<com.example.uniwattelektrik.feature.workforce.data.remote.NotificationRecord>> = _notifications.asStateFlow()

    private var notificationsJob: Job? = null

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    /**
     * Generic flag flipped to `true` while a create/update/delete action is
     * in flight. Screens read this to render a modal LoadingOverlay.
     */
    private val _actionInProgress = MutableStateFlow(false)
    val actionInProgress: StateFlow<Boolean> = _actionInProgress.asStateFlow()

    /**
     * Flips to true only after every required Firestore stream has hit its
     * first emission. Use this — not [loading] alone — to gate shimmer on
     * dashboards that compose data from multiple sources, so we don't reveal
     * employee cards while task / attendance counts are still loading.
     *
     * Reset on every [loadForAdmin] / [loadForUser] re-bind.
     */
    private val _streamsReady = MutableStateFlow(false)
    val streamsReady: StateFlow<Boolean> = _streamsReady.asStateFlow()

    // Per-stream first-emission flags. When all required ones are primed for
    // the active role, [_streamsReady] flips to true.
    private var primedEmployees     = false
    private var primedTasks         = false
    private var primedAttendance    = false
    private var primedLeaveRequests = false
    private var primedSpareItems    = false

    private fun maybeMarkAdminStreamsReady() {
        if (primedEmployees && primedTasks && primedAttendance && primedLeaveRequests) {
            _streamsReady.value = true
        }
    }

    private fun maybeMarkUserStreamsReady() {
        // Users only need their own tasks + leave history to render the home.
        if (primedTasks && primedLeaveRequests) {
            _streamsReady.value = true
        }
    }

    private fun resetStreamPriming() {
        primedEmployees     = false
        primedTasks         = false
        primedAttendance    = false
        primedLeaveRequests = false
        primedSpareItems    = false
        _streamsReady.value = false
    }

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    /**
     * Session-level optimistic check-in state. Lives in the ViewModel so it
     * survives tab-switch navigation (Home → Tasks → Home), unlike `remember`
     * state in a Composable which is destroyed when the composable leaves the
     * composition tree.
     *
     *  - `null`  → no override; use the server-derived state from the attendance flow
     *  - `true`  → user just tapped "On Duty" — show as checked-in immediately
     *  - `false` → user just tapped "Off Duty" — show as checked-out immediately
     *
     * Auto-cleared by [clearSessionCheckInIfConfirmed] once the Firestore
     * snapshot listener confirms the state (optimistic and server agree).
     */
    private val _sessionCheckIn = MutableStateFlow<Boolean?>(null)
    val sessionCheckIn: StateFlow<Boolean?> = _sessionCheckIn.asStateFlow()

    /** Called immediately when the user taps the toggle — before Firestore confirms. */
    fun setSessionCheckIn(isCheckedIn: Boolean) {
        _sessionCheckIn.value = isCheckedIn
    }

    /**
     * Called from a [LaunchedEffect] watching [isCheckedInServer]. Once the
     * server state matches the optimistic override, the override is cleared so
     * the UI falls back to pure server-derived state going forward.
     */
    fun clearSessionCheckInIfConfirmed(serverState: Boolean) {
        if (_sessionCheckIn.value == serverState) {
            _sessionCheckIn.value = null
        }
    }

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
        inventoryTxnsJob?.cancel()
        _error.value = null
        _loading.value = true
        resetStreamPriming()

        employeesJob = directory.observeEmployees(adminUid)
            .onEach {
                _employees.value = it
                _loading.value = false
                primedEmployees = true
                maybeMarkAdminStreamsReady()
            }
            .catch {
                setError(it.message)
                _loading.value = false
                primedEmployees = true
                maybeMarkAdminStreamsReady()
            }
            .launchIn(viewModelScope)

        tasksJob = directory.observeTasksForAdmin(adminUid)
            .onEach {
                _tasks.value = it
                primedTasks = true
                maybeMarkAdminStreamsReady()
            }
            .catch {
                setError(it.message)
                primedTasks = true
                maybeMarkAdminStreamsReady()
            }
            .launchIn(viewModelScope)

        attendanceJob = directory.observeAttendance(adminUid)
            .onEach {
                _attendance.value = it
                primedAttendance = true
                maybeMarkAdminStreamsReady()
            }
            .catch {
                setError(it.message)
                primedAttendance = true
                maybeMarkAdminStreamsReady()
            }
            .launchIn(viewModelScope)

        checkinsJob = directory.observeCheckins(adminUid)
            .onEach { _checkins.value = it }
            .catch { setError(it.message) }
            .launchIn(viewModelScope)

        spareItemsJob = directory.observeSpareItems(adminUid)
            .onEach {
                _spareItems.value = it
                primedSpareItems = true
            }
            .catch {
                setError(it.message)
                primedSpareItems = true
            }
            .launchIn(viewModelScope)

        leaveRequestsJob = directory.observeLeaveRequestsForAdmin(adminUid)
            .onEach {
                _leaveRequests.value = it
                primedLeaveRequests = true
                maybeMarkAdminStreamsReady()
            }
            .catch {
                setError(it.message)
                primedLeaveRequests = true
                maybeMarkAdminStreamsReady()
            }
            .launchIn(viewModelScope)

        // Phase 2 / Case 2 — inventory transactions (admin-side only). Used
        // by ReportsScreen to split consumption into "from tasks" vs manual.
        inventoryTxnsJob = directory.observeInventoryTransactions(adminUid)
            .onEach { _inventoryTxns.value = it }
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
        inventoryTxnsJob?.cancel()
        _error.value = null
        _loading.value = true
        resetStreamPriming()

        tasksJob = directory.observeTasksForUser(userUid)
            .onEach {
                _tasks.value = it
                _loading.value = false
                primedTasks = true
                maybeMarkUserStreamsReady()
            }
            .catch {
                setError(it.message)
                _loading.value = false
                primedTasks = true
                maybeMarkUserStreamsReady()
            }
            .launchIn(viewModelScope)

        // Employee attendance: query by userId (not adminId) so Firestore
        // security rules allow the query. Querying by adminId returns ALL
        // employees' records; Firestore rejects that for non-admin callers
        // because some docs have a different userId — causing intermittent
        // "permission denied" failures when logging in on a second device.
        attendanceJob = directory.observeMyAttendance(userUid)
            .onEach { _attendance.value = it }
            .catch { setError(it.message) }
            .launchIn(viewModelScope)

        if (!adminUid.isNullOrBlank()) {
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
            .onEach {
                _leaveRequests.value = it
                primedLeaveRequests = true
                maybeMarkUserStreamsReady()
            }
            .catch {
                setError(it.message)
                primedLeaveRequests = true
                maybeMarkUserStreamsReady()
            }
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
            _actionInProgress.value = true
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
                    _actionInProgress.value = false
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
                    _actionInProgress.value = false
                    onDone(result)
                }
            }
        }
    }

    /**
     * Update an existing employee. The Firebase Auth account is left
     * untouched (email/password changes are out of scope here) — only the
     * Firestore profile mirror is rewritten.
     */
    fun updateEmployee(
        adminUid: String,
        employeeId: String,
        draft: EmployeeDraft,
        onDone: (Result<EmployeeRecord>) -> Unit = {},
    ) {
        viewModelScope.launch {
            _loading.value = true
            _actionInProgress.value = true
            _error.value = null
            val result = runCatching { directory.updateEmployee(adminUid, employeeId, draft) }
            result.onFailure {
                AppLog.w("WorkforceVM", "updateEmployee firestore failed: ${it.message}")
                setError(it.message)
            }
            _loading.value = false
            _actionInProgress.value = false
            onDone(result)
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
        notifyAssignee: Boolean = true,
        assigneeIds: List<String> = emptyList(),
        assigneeNames: List<String> = emptyList(),
        onDone: (Result<Unit>) -> Unit = {},
    ) {
        viewModelScope.launch {
            _actionInProgress.value = true
            val result = runCatching {
                directory.addTask(
                    adminUid, userId, title, location, time, day, priority,
                    description,
                    departmentId, departmentName, equipmentId, equipmentName,
                    checklist, attachments, address, latitude, longitude,
                    dueDate, ownerAdminName, assigneeName, notifyAssignee,
                    assigneeIds, assigneeNames,
                )
            }
            result.onFailure { setError(it.message) }
            _actionInProgress.value = false
            onDone(result.map { })
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
        notifyAssignee: Boolean = true,
        assigneeIds: List<String> = emptyList(),
        assigneeNames: List<String> = emptyList(),
        onDone: (Result<Unit>) -> Unit = {},
    ) {
        viewModelScope.launch {
            _actionInProgress.value = true
            val result = runCatching {
                directory.updateTask(
                    taskId, adminUid, userId, title, location, time, day, priority,
                    description,
                    departmentId, departmentName, equipmentId, equipmentName,
                    checklist, attachments, address, latitude, longitude,
                    dueDate, assigneeName, notifyAssignee,
                    assigneeIds, assigneeNames,
                )
            }
            result.onFailure { setError(it.message) }
            _actionInProgress.value = false
            onDone(result.map { })
        }
    }

    /**
     * Reassign a task to a new set of users (used by the user-side "Reassign"
     * button on InReview tasks). Status is preserved server-side.
     */
    fun reassignTask(
        taskId: String,
        adminUid: String,
        newAssigneeIds: List<String>,
        newAssigneeNames: List<String>,
        onDone: (Result<Unit>) -> Unit = {},
    ) {
        viewModelScope.launch {
            _actionInProgress.value = true
            val result = runCatching {
                directory.reassignTask(taskId, adminUid, newAssigneeIds, newAssigneeNames)
            }
            result.onFailure {
                AppLog.w("WorkforceVM", "reassignTask failed: ${it.message}")
                setError(it.message)
            }
            _actionInProgress.value = false
            onDone(result.map { })
        }
    }

    /**
     * Upload a new profile photo for [employeeId] to Firebase Storage and
     * update the `photoUrl` field on their Firestore record.
     * [contentUri] is a platform URI string from [rememberAttachmentLauncher].
     * The Firestore snapshot listener will push the new [EmployeeRecord.photoUrl]
     * to the UI automatically — no manual state update needed.
     */
    fun updateProfilePhoto(
        adminId: String,
        employeeId: String,
        contentUri: String,
        onDone: (Result<String>) -> Unit = {},
    ) {
        viewModelScope.launch {
            val result = runCatching {
                directory.updateEmployeePhoto(adminId, employeeId, contentUri)
            }
            result.onFailure {
                AppLog.w("WorkforceVM", "updateProfilePhoto failed: ${it.message}")
                setError(it.message)
            }
            onDone(result)
        }
    }

    /** Update an employee's permission level ("" | "viewer" | "field" | "super"). */
    fun updateEmployeePermission(adminId: String, employeeId: String, permission: String, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            runCatching { directory.updateEmployeePermission(adminId, employeeId, permission) }
                .onFailure { AppLog.w("WorkforceVM", "updatePermission failed: ${it.message}"); setError(it.message) }
                .onSuccess { onDone() }
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
            _actionInProgress.value = true
            val result = runCatching {
                directory.completeTaskWithSignoff(
                    adminId, taskId, assignedUserId,
                    signoffDescription, downtimeMinutes, rca,
                    materialsUsed, startTimeMs, endTimeMs,
                )
            }
            result.onFailure { AppLog.w("WorkforceVM", "completeTaskWithSignoff failed: ${it.message}") }
            _actionInProgress.value = false
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
        voiceUrl: String? = null,
        voiceDurationMs: Long? = null,
    ) {
        // Allow blank message only when a voice attachment is present.
        if (message.isBlank() && voiceUrl == null) return
        viewModelScope.launch {
            runCatching {
                directory.addTaskNote(
                    taskId          = taskId,
                    authorId        = authorId,
                    authorName      = authorName,
                    role            = role,
                    message         = message.trim(),
                    voiceUrl        = voiceUrl,
                    voiceDurationMs = voiceDurationMs,
                )
            }.onFailure {
                AppLog.w("WorkforceVM", "addTaskNote failed: ${it.message}")
                setError(it.message)
            }
        }
    }

    /**
     * Upload a recorded voice clip and post it as a chat note on the task.
     * Wraps the two-step (upload → addTaskNote) so callers don't have to
     * juggle suspending state on the UI side.
     */
    fun postVoiceNote(
        adminId      : String,
        taskId       : String,
        authorId     : String,
        authorName   : String,
        role         : String,
        contentUri   : String,
        durationMs   : Long,
        onDone       : (Result<Unit>) -> Unit = {},
    ) {
        viewModelScope.launch {
            val result = runCatching {
                val url = directory.uploadVoiceNote(adminId, contentUri)
                directory.addTaskNote(
                    taskId          = taskId,
                    authorId        = authorId,
                    authorName      = authorName,
                    role            = role,
                    message         = "",
                    voiceUrl        = url,
                    voiceDurationMs = durationMs,
                )
                Unit
            }
            result.onFailure {
                AppLog.w("WorkforceVM", "postVoiceNote failed: ${it.message}")
                setError(it.message)
            }
            onDone(result)
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
            _actionInProgress.value = true
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
            _actionInProgress.value = false
            onDone(result)
        }
    }

    fun approveLeave(leaveId: String, adminId: String, userId: String, onDone: (Result<Unit>) -> Unit = {}) {
        viewModelScope.launch {
            _actionInProgress.value = true
            val r = runCatching { directory.updateLeaveStatus(leaveId, adminId, userId, "approved") }
                .onFailure {
                    AppLog.w("WorkforceVM", "approveLeave failed: ${it.message}")
                    setError(it.message)
                }
            _actionInProgress.value = false
            onDone(r)
        }
    }

    fun rejectLeave(
        leaveId: String,
        adminId: String,
        userId: String,
        rejectionReason: String = "",
        onDone: (Result<Unit>) -> Unit = {},
    ) {
        viewModelScope.launch {
            _actionInProgress.value = true
            val r = runCatching {
                directory.updateLeaveStatus(leaveId, adminId, userId, "rejected", rejectionReason)
            }.onFailure {
                AppLog.w("WorkforceVM", "rejectLeave failed: ${it.message}")
                setError(it.message)
            }
            _actionInProgress.value = false
            onDone(r)
        }
    }


    // ─── Reports — monthly stock snapshot passthrough ────────────────────────
    //
    // Phase 2 / Case 3: the snapshotMonthlyStock Cloud Function writes an
    // opening-stock doc per admin per month. ReportsScreen subscribes to the
    // snapshot for the *previous* month (i.e. the report month's opening) and
    // passes it into ReportsAggregator. We don't cache it on the VM because
    // the selected report month changes as the user paginates — wrapping the
    // directory flow keeps the subscription scoped to that screen.
    fun monthlyStockSnapshotFlow(
        adminId: String, year: Int, month: Int,
    ): kotlinx.coroutines.flow.Flow<com.example.uniwattelektrik.feature.workforce.data.remote.MonthlyStockSnapshot?> =
        directory.observeMonthlyStockSnapshot(adminId, year, month)

    /**
     * Phase 3 / Case 2: combine N adjacent monthly snapshot flows into a
     * single list flow for trend sparklines. [months] is ordered
     * oldest → newest; the returned list mirrors that order and emits
     * `null` for months whose snapshot doc doesn't exist yet.
     */
    fun monthlyStockHistoryFlow(
        adminId: String,
        months : List<Pair<Int, Int>>,   // (year, monthNumber)
    ): kotlinx.coroutines.flow.Flow<List<com.example.uniwattelektrik.feature.workforce.data.remote.MonthlyStockSnapshot?>> {
        if (months.isEmpty() || adminId.isBlank()) {
            return kotlinx.coroutines.flow.flowOf(emptyList())
        }
        val flows = months.map { (y, m) ->
            directory.observeMonthlyStockSnapshot(adminId, y, m)
        }
        return kotlinx.coroutines.flow.combine(flows) { arr -> arr.toList() }
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
