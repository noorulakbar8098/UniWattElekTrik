package com.example.uniwattelektrik.feature.workforce.data.remote

import kotlinx.coroutines.flow.Flow

/**
 * Firestore-backed workforce data using **flat top-level collections**.
 *
 * Layout:
 *   /admins/{adminId}
 *   /users/{userId}                   — adminId field for tenant isolation
 *   /tasks/{taskId}                   — adminId + assignedUserId fields
 *   /attendance_logs/{attendanceId}   — adminId + userId fields
 *   /inventory_items/{itemId}         — adminId field
 *   /inventory_transactions/{txnId}   — adminId + userId fields
 *   /checkins/{checkinId}             — adminId + userId fields
 *   /notifications/{notificationId}   — adminId + userId fields
 *   /spare_items/{itemId}             — adminId field
 *   /departments/{deptId}             — adminId field
 *   /equipment/{equipmentId}          — adminId + departmentId fields
 *   /employee_map/{uid}               — auth lookup (unchanged)
 *   /admin_ids/{code}                 — auth lookup (unchanged)
 *
 * Reads are exposed as hot Flows (Firestore snapshot listeners). Writes are
 * suspend functions returning the canonical record.
 */
interface WorkforceDirectory {

    // ─── Employees ───────────────────────────────────────────────────────────

    /** Live stream of all active employees for this admin. */
    fun observeEmployees(adminId: String): Flow<List<EmployeeRecord>>

    /** Persists a new employee at `users/{uid}`. Doc ID = Firebase Auth UID. */
    suspend fun addEmployee(
        adminId: String,
        uid: String,
        draft: EmployeeDraft,
    ): EmployeeRecord

    /**
     * Update an existing employee. Mirrors [addEmployee] but uses Firestore
     * `update()` so immutable fields (uid, createdBy, createdAt, role,
     * mustChangePassword) and the auth password are NOT touched. If
     * [draft.photoUri] is non-blank a new photo is uploaded and replaces
     * the previous `photoUrl`.
     */
    suspend fun updateEmployee(
        adminId: String,
        employeeId: String,
        draft: EmployeeDraft,
    ): EmployeeRecord

    /** Flips `mustChangePassword → false` and syncs `employee_map`. */
    suspend fun markPasswordChanged(adminId: String, uid: String)

    /** Updates an employee's employment status (e.g. "active", "Relieved", "onleave"). */
    suspend fun updateEmployeeStatus(adminId: String, employeeId: String, status: String)

    /** Update the `permission` field for an employee ("" | "viewer" | "field" | "super"). */
    suspend fun updateEmployeePermission(adminId: String, employeeId: String, permission: String)

    /** Writes the FCM token. Pass "" to clear on logout. */
    suspend fun saveFcmToken(adminId: String, uid: String, token: String)

    /**
     * Upload a new profile photo for an existing employee and update their
     * `photoUrl` field in Firestore.  [contentUri] is a platform URI string
     * (e.g. `content://…` on Android, `file://…` from camera cache).
     * Returns the Firebase Storage public download URL.
     */
    suspend fun updateEmployeePhoto(
        adminId: String,
        employeeId: String,
        contentUri: String,
    ): String

    // ─── Tasks ───────────────────────────────────────────────────────────────

    /** Live stream of all tasks owned by this admin. */
    fun observeTasksForAdmin(adminId: String): Flow<List<TaskRecord>>

    /** Live stream of all tasks assigned to this employee. Flat query — no collectionGroup. */
    fun observeTasksForUser(userId: String): Flow<List<TaskRecord>>

    suspend fun addTask(
        adminId: String,
        userId: String?,
        title: String,
        location: String,
        time: String,
        day: String,
        priority: String,       // "Success" | "Medium" | "Danger"
        description: String = "",
        departmentId: String = "",
        departmentName: String = "",
        equipmentId: String = "",
        equipmentName: String = "",
        checklist: List<ChecklistItem> = emptyList(),
        attachments: List<String> = emptyList(),
        address: String = "",
        latitude: Double? = null,
        longitude: Double? = null,
        dueDate: Long? = null,
        ownerAdminName: String = "",
        assigneeName: String = "",
    ): TaskRecord

    /**
     * Mark a task as accepted by the assigned user. Writes status="in_progress",
     * acceptedAt = serverTimestamp, and the captured lat/lng (may be null when
     * permission is denied).
     */
    suspend fun acceptTask(
        taskId: String,
        lat: Double? = null,
        lon: Double? = null,
    ): TaskRecord

    /**
     * Update an existing task with the same field-set as [addTask]. Only the
     * fields explicitly passed are written — the doc's `createdAt`,
     * `acceptedAt`, `completedAt` and lifecycle counters are preserved.
     *
     * If [userId] differs from the previously assigned user the `tasksOpen`
     * counter is rebalanced (decrement old, increment new) — only when the
     * task is still open (not in the "Done" state).
     */
    suspend fun updateTask(
        taskId: String,
        adminId: String,
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
        checklist: List<ChecklistItem> = emptyList(),
        attachments: List<String> = emptyList(),
        address: String = "",
        latitude: Double? = null,
        longitude: Double? = null,
        dueDate: Long? = null,
        assigneeName: String = "",
    ): TaskRecord

    /** Live stream of chat notes for a task (subcollection). Sorted ascending. */
    fun observeTaskNotes(taskId: String): Flow<List<TaskNote>>

    /** Append a chat note. Returns the persisted record. */
    suspend fun addTaskNote(
        taskId: String,
        authorId: String,
        authorName: String,
        role: String,           // "admin" | "user"
        message: String,
    ): TaskNote

    /**
     * Updates task status and keeps `tasksOpen` counter consistent.
     * "Done" → sets `completedAt`, decrements assigned employee's counter.
     */
    suspend fun updateTaskStatus(
        adminId: String,
        taskId: String,
        assignedUserId: String?,
        newStatus: String,      // "Todo" | "InProgress" | "Done"
    ): TaskRecord

    /**
     * Completes a task with full signoff data. Atomically:
     *  - marks status = "Done", writes completedAt = serverTimestamp
     *  - stores signoffDescription, downtimeMinutes, rca, totalWorkDurationMs, materialsUsed
     *  - deducts materialsUsed quantities from spare_items stockQty
     *  - records inventory_transactions for each consumed item
     *  - decrements the assignee's tasksOpen counter
     */
    suspend fun completeTaskWithSignoff(
        adminId: String,
        taskId: String,
        assignedUserId: String?,
        signoffDescription: String,
        downtimeMinutes: Int,
        rca: String,
        materialsUsed: List<MaterialUsedItem>,
        startTimeMs: Long?,
        endTimeMs: Long,
    ): TaskRecord

    /** Toggle a single checklist item's done state and persist. */
    suspend fun setChecklistItemDone(
        taskId: String,
        index: Int,
        done: Boolean,
    )

    /**
     * Uploads a local image (content:// or file:// URI as a String) to Cloud
     * Storage under `task-attachments/{adminId}/...` and returns the public
     * download URL that should be appended to a TaskRecord's `attachments`.
     */
    suspend fun uploadTaskAttachment(adminId: String, contentUri: String): String

    /** Replace a task's attachments array with [urls]. */
    suspend fun updateTaskAttachments(taskId: String, urls: List<String>)

    // ─── Inventory ───────────────────────────────────────────────────────────

    /** Live stream of all inventory items for this admin. */
    fun observeInventory(adminId: String): Flow<List<InventoryRecord>>

    suspend fun addInventoryItem(
        adminId: String,
        name: String,
        type: String,           // "equipment" | "material"
        price: Double,
        quantity: Int,
    ): InventoryRecord

    /** Live stream of all inventory transactions (issues / returns / restocks). */
    fun observeInventoryTransactions(adminId: String): Flow<List<InventoryTransaction>>

    suspend fun addInventoryTransaction(
        adminId: String,
        userId: String,         // employee who received or returned the item
        itemId: String,
        itemName: String,       // denormalised for display without extra read
        type: String,           // "issue" | "return" | "restock"
        quantity: Int,
    ): InventoryTransaction

    // ─── Attendance ──────────────────────────────────────────────────────────

    /** Live stream of all attendance logs for this admin. */
    fun observeAttendance(adminId: String): Flow<List<AttendanceRecord>>

    /**
     * Live stream of attendance logs belonging to a single employee.
     * Uses `whereEqualTo("userId", userId)` so Firestore security rules
     * allow the query for non-admin users (employees can only list their
     * own records — querying by adminId would return other employees'
     * records and Firestore would reject the entire query).
     */
    fun observeMyAttendance(userId: String): Flow<List<AttendanceRecord>>

    suspend fun markCheckIn(
        adminId: String,
        userId: String,
        lat: Double?,
        lng: Double?,
        checkInStatus: String,  // "ON_TIME" | "LATE"
    ): AttendanceRecord

    suspend fun markCheckOut(
        adminId: String,
        userId: String,
        attendanceId: String,
        lat: Double?,
        lng: Double?,
    )

    // ─── Check-ins (location pings) ──────────────────────────────────────────

    /** Live stream of recent GPS pings for this admin (capped at 500). */
    fun observeCheckins(adminId: String): Flow<List<CheckinPing>>

    suspend fun recordCheckIn(
        adminId: String,
        userId: String,
        latitude: Double,
        longitude: Double,
    ): String

    // ─── Notifications ────────────────────────────────────────────────────────

    /** Live stream of notifications for a specific user under this admin. */
    fun observeNotifications(adminId: String, userId: String): Flow<List<NotificationRecord>>

    suspend fun markNotificationRead(notificationId: String)

    suspend fun addNotification(
        adminId: String,
        userId: String,
        title: String,
        body: String,
        type: String,           // "task_assigned" | "task_updated" | "check_in" | etc.
        relatedId: String?,     // taskId, attendanceId, etc.
    ): NotificationRecord

    // ─── Spares ────────────────────────────────────────────────────────────────

    /** Live stream of all spare items for this admin. */
    fun observeSpareItems(adminId: String): Flow<List<SpareItemRecord>>

    suspend fun addSpareItem(
        adminId: String,
        item: SpareItemRecord,
    ): SpareItemRecord

    suspend fun updateSpareItem(
        adminId: String,
        itemId: String,
        updates: Map<String, Any?>,
    ): SpareItemRecord

    suspend fun deleteSpareItem(adminId: String, itemId: String)

    /**
     * Bulk-delete spare items in Firestore-batch chunks (≤500 per batch).
     * Returns the number of docs successfully deleted. Used by the
     * multi-selection delete feature on inventory screens.
     */
    suspend fun bulkDeleteSpareItems(adminId: String, itemIds: List<String>): Int

    /**
     * Bulk-insert spare items in Firestore-batch chunks (≤500 per batch).
     * Returns the number of records successfully written. Useful for
     * Excel-imported spare lists.
     */
    suspend fun bulkInsertSpareItems(adminId: String, items: List<SpareItemRecord>): Int

    // ─── Departments ──────────────────────────────────────────────────────────

    fun observeDepartments(adminId: String): Flow<List<DepartmentRecord>>

    suspend fun addDepartment(adminId: String, name: String): DepartmentRecord

    suspend fun updateDepartment(adminId: String, departmentId: String, name: String): DepartmentRecord

    suspend fun deleteDepartment(adminId: String, departmentId: String)

    // ─── Equipment ────────────────────────────────────────────────────────────

    fun observeEquipment(adminId: String): Flow<List<EquipmentRecord>>

    suspend fun addEquipment(adminId: String, name: String, departmentId: String): EquipmentRecord

    suspend fun updateEquipment(
        adminId: String,
        equipmentId: String,
        name: String,
        departmentId: String,
    ): EquipmentRecord

    suspend fun deleteEquipment(adminId: String, equipmentId: String)

    // ─── Leave requests ────────────────────────────────────────────────────────

    /** Live stream of all leave requests belonging to this admin's tenant. */
    fun observeLeaveRequestsForAdmin(adminId: String): Flow<List<LeaveRecord>>

    /** Live stream of leave requests submitted by a specific employee. */
    fun observeLeaveRequestsForUser(userId: String): Flow<List<LeaveRecord>>

    suspend fun submitLeaveRequest(
        adminId: String,
        userId: String,
        employeeName: String,
        department: String,
        leaveType: String,    // "Casual" | "Sick" | "Earned"
        fromDateMs: Long,
        toDateMs: Long,
        totalDays: Int,
        reason: String,
    ): LeaveRecord

    /**
     * Flip a request's status to "approved" or "rejected".
     * Approved: also marks the employee as OnLeave in /users/{userId}.
     */
    suspend fun updateLeaveStatus(
        leaveId: String,
        adminId: String,
        userId: String,
        newStatus: String,            // "approved" | "rejected"
        rejectionReason: String = "",
    )

    // ─── Danger zone ──────────────────────────────────────────────────────────

    /**
     * Permanently deletes every document in all tenant-isolated collections
     * where `adminId == [adminId]`. Irreversible — caller must confirm with user.
     */
    suspend fun deleteAllData(adminId: String)
}

// ─── Data classes ────────────────────────────────────────────────────────────

data class EmployeeRecord(
    val id: String,
    val name: String,
    val role: String,
    val phone: String,
    val zone: String,
    val status: String,             // "Active" | "OnLeave" | "Inactive"
    val tasksOpen: Int = 0,
    val email: String = "",
    val gender: String = "",        // "Male" | "Female" | "Other" | ""
    val employmentType: String = "", // "Fulltime" | "Contract" | ""
    val joiningDateMs: Long? = null,
    val salary: Double = 0.0,
    val photoUrl: String = "",
    val dateOfBirthMs: Long? = null,
    val address: String = "",
    val department: String = "",
    val reportingTo: String = "",
    val permission: String = "",    // "Viewer" | "Field" | "Super" | ""
    val emergencyName: String = "",
    val emergencyRelation: String = "",
    val emergencyPhone: String = "",
    val shift: String = "Shift1",   // "Shift1" (09:00-18:00) | "Shift2" (13:00-23:00)
    val updatedAt: Long? = null,
    val deletedAt: Long? = null,    // non-null = soft-deleted
    val fcmToken: String = "",
)

data class EmployeeDraft(
    val name: String,
    val role: String,
    val email: String,
    val password: String = "",      // used only for Auth account creation, never stored
    val phone: String,
    val gender: String,
    val employmentType: String,
    val joiningDateMs: Long?,
    val salary: Double,
    val zone: String = "",
    val status: String = "Active",
    val photoUri: String? = null,
    val dateOfBirthMs: Long? = null,
    val address: String = "",
    val department: String = "",
    val reportingTo: String = "",
    val permission: String = "Field",
    val emergencyName: String = "",
    val emergencyRelation: String = "",
    val emergencyPhone: String = "",
    val shift: String = "Shift1",
)

data class TaskRecord(
    val id: String,
    val adminId: String,
    val userId: String?,            // assignedUserId in Firestore
    val title: String,
    val description: String = "",   // long-form details captured on the create-task screen
    val location: String,
    val time: String,
    val day: String,
    val priority: String,           // "Success" | "Medium" | "Danger"
    val status: String,             // "Todo" | "InProgress" | "Done"
    val assigneeInitials: String = "",
    val assigneeName: String = "",
    val ownerAdminId: String = "",
    val ownerAdminName: String = "",
    val scheduledDateMs: Long? = null,
    val dueDate: Long? = null,
    val createdAtMs: Long? = null,
    val acceptedAt: Long? = null,
    val acceptedLat: Double? = null,
    val acceptedLon: Double? = null,
    val completedAt: Long? = null,
    val updatedAt: Long? = null,
    // Service Details (Phase B)
    val departmentId: String = "",
    val departmentName: String = "",
    val equipmentId: String = "",
    val equipmentName: String = "",
    // Checklist (Phase B)
    val checklist: List<ChecklistItem> = emptyList(),
    // Attachments (Phase C — populated as URL list)
    val attachments: List<String> = emptyList(),
    // Geocoded address (Phase D — populated when address is resolved)
    val address: String = "",
    val latitude: Double? = null,
    val longitude: Double? = null,
    // ── Task Completion / Signoff (Phase E) ──────────────────────────────
    val signoffDescription: String = "",  // what work was done / issue resolution
    val downtimeMinutes: Int = 0,         // total downtime reported by technician
    val rca: String = "",                 // root cause analysis
    val totalWorkDurationMs: Long? = null, // endTime - acceptedAt (auto-calculated)
    val materialsUsed: List<MaterialUsedItem> = emptyList(), // inventory consumed
)

/** One line-item of material consumed during task completion. */
data class MaterialUsedItem(
    val itemId: String = "",
    val itemName: String = "",
    val quantity: Int = 0,
)

data class TaskNote(
    val id: String,
    val taskId: String,
    val authorId: String,
    val authorName: String,
    val role: String,               // "admin" | "user"
    val message: String,
    val createdAtMs: Long? = null,
)

data class ChecklistItem(
    val text: String,
    val done: Boolean = false,
)

data class InventoryRecord(
    val id: String,
    val name: String,
    val type: String,               // "equipment" | "material"
    val price: Double,
    val quantity: Int,
)

data class InventoryTransaction(
    val id: String,
    val adminId: String,
    val userId: String,
    val itemId: String,
    val itemName: String,
    val type: String,               // "issue" | "return" | "restock"
    val quantity: Int,
    val createdAt: Long? = null,
)

data class AttendanceRecord(
    val id: String,
    val userId: String,
    val dateMs: Long,
    val checkInMs: Long,
    val checkInLat: Double? = null,
    val checkInLng: Double? = null,
    val checkInStatus: String = "ON_TIME",
    val checkOutMs: Long? = null,
    val checkOutLat: Double? = null,
    val checkOutLng: Double? = null,
    val status: String = "CHECKED_IN", // "CHECKED_IN" | "COMPLETED"
)

data class CheckinPing(
    val id: String,
    val userId: String,
    val latitude: Double,
    val longitude: Double,
    val timestampMs: Long,
)

data class NotificationRecord(
    val id: String,
    val adminId: String,
    val userId: String,
    val title: String,
    val body: String,
    val type: String,               // "task_assigned" | "task_updated" | "check_in" | etc.
    val isRead: Boolean = false,
    val relatedId: String? = null,
    val createdAt: Long? = null,
)

data class SpareItemRecord(
    val id: String,
    val adminId: String,
    val category: String,           // "Cable" | "Spare Component"
    val name: String,
    val make: String = "",
    val size: String = "",
    val core: String = "",
    val currentRating: String = "",
    val noOfPoles: String = "",
    val unit: String = "",
    val price: Double,
    val stockQty: Int,
    val hsn: String,
    val vendorName1: String = "",
    val vendorGst1: String = "",
    val vendorContact1: String = "",
    val vendorAddress1: String = "",
    val vendorName2: String = "",
    val vendorGst2: String = "",
    val vendorContact2: String = "",
    val vendorAddress2: String = "",
    val vendorLocation: String = "",
    val createdAt: Long? = null,
    val updatedAt: Long? = null,
)

data class DepartmentRecord(
    val id: String,
    val adminId: String,
    val name: String,
    val createdAt: Long? = null,
    val updatedAt: Long? = null,
)

data class EquipmentRecord(
    val id: String,
    val adminId: String,
    val name: String,
    val departmentId: String,
    val createdAt: Long? = null,
    val updatedAt: Long? = null,
)

data class LeaveRecord(
    val id: String,
    val userId: String,
    val adminId: String,
    val employeeName: String,
    val department: String = "",
    val leaveType: String,           // "Casual" | "Sick" | "Earned"
    val fromDateMs: Long,            // epoch millis
    val toDateMs: Long,              // epoch millis
    val totalDays: Int,
    val reason: String,
    val status: String,              // "pending" | "approved" | "rejected"
    val rejectionReason: String = "",
    val createdAtMs: Long? = null,
    val updatedAtMs: Long? = null,
)

