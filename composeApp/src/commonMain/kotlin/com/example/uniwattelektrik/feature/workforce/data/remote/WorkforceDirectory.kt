package com.example.uniwattelektrik.feature.workforce.data.remote

import kotlinx.coroutines.flow.Flow

/**
 * Firestore-backed workforce data scoped by [adminId] (per-admin subcollections).
 *
 * Layout:
 *   /admins/{adminId}/users/{userId}
 *   /admins/{adminId}/tasks/{taskId}
 *   /admins/{adminId}/inventory/{itemId}
 *
 * Reads are exposed as **hot Flows** (Firestore snapshot listeners) so the UI
 * refreshes the moment a doc changes — no manual re-fetch needed. Writes are
 * suspend functions returning the canonical record.
 */
interface WorkforceDirectory {

    // ─── Employees (admin-managed users) ────────────────────────────────────
    /** Live stream of all employees in this admin's subcollection. */
    fun observeEmployees(adminId: String): Flow<List<EmployeeRecord>>

    /**
     * Persists a new employee at `admins/{adminId}/users/{uid}`.
     *
     * [uid] is the Firebase Auth UID returned by [com.example.uniwattelektrik.feature.auth.data.remote.EmployeeAuthClient.createEmployee].
     * Using it as the doc ID lets the employee resolve their own profile via
     * a collection-group query at sign-in time.
     */
    suspend fun addEmployee(
        adminId: String,
        uid: String,
        draft: EmployeeDraft,
    ): EmployeeRecord

    /**
     * Flips `mustChangePassword → false` on the employee's doc once they've
     * rotated their initial admin-given password.
     */
    suspend fun markPasswordChanged(adminId: String, uid: String)

    // ─── Tasks ──────────────────────────────────────────────────────────────
    /** Live stream of all tasks owned by this admin. */
    fun observeTasksForAdmin(adminId: String): Flow<List<TaskRecord>>

    /**
     * Live stream of all tasks assigned to this employee — uses a Firestore
     * collectionGroup query because tasks live under each owning admin.
     */
    fun observeTasksForUser(userId: String): Flow<List<TaskRecord>>

    suspend fun addTask(
        adminId: String,
        userId: String?,
        title: String,
        location: String,
        time: String,
        day: String,
        priority: String,   // "Low" | "Medium" | "High"
    ): TaskRecord

    // ─── Inventory ──────────────────────────────────────────────────────────
    /** Live stream of all inventory items in this admin's subcollection. */
    fun observeInventory(adminId: String): Flow<List<InventoryRecord>>

    suspend fun addInventoryItem(
        adminId: String,
        name: String,
        type: String,        // "equipment" | "material"
        price: Double,
        quantity: Int,
    ): InventoryRecord

    // ─── Attendance ─────────────────────────────────────────────────────────
    /** Live stream of every attendance record in this admin's subcollection. */
    fun observeAttendance(adminId: String): Flow<List<AttendanceRecord>>

    /** Records a check-in for [userId] at the current server time, with
     *  validated location and shift-relative status (ON_TIME / LATE). */
    suspend fun markCheckIn(
        adminId: String,
        userId: String,
        lat: Double?,
        lng: Double?,
        checkInStatus: String,
    ): AttendanceRecord

    /** Updates an existing attendance doc with `checkOut = serverTimestamp`
     *  and the final location of the employee. */
    suspend fun markCheckOut(
        adminId: String,
        userId: String,
        attendanceId: String,
        lat: Double?,
        lng: Double?,
    )

    // ─── Check-ins (location pings) ─────────────────────────────────────────
    suspend fun recordCheckIn(
        adminId: String,
        userId: String,
        latitude: Double,
        longitude: Double,
    ): String

    /** Live stream of GPS check-in pings under the admin scope. */
    fun observeCheckins(adminId: String): Flow<List<CheckinPing>>
}

data class CheckinPing(
    val id: String,
    val userId: String,
    val latitude: Double,
    val longitude: Double,
    val timestampMs: Long,
)

data class EmployeeRecord(
    val id: String,
    val name: String,
    val role: String,
    val phone: String,
    val zone: String,
    val status: String,        // "Active" | "OnLeave" | "Inactive"
    val tasksOpen: Int = 0,
    /* HR fields — all optional so legacy docs without them still load */
    val email: String = "",
    val gender: String = "",            // "Male" | "Female" | "Other" | ""
    val employmentType: String = "",    // "Fulltime" | "Contract" | ""
    val joiningDateMs: Long? = null,    // epoch millis from Firestore Timestamp
    val salary: Double = 0.0,
    val photoUrl: String = "",          // Firebase Storage download URL
    val dateOfBirthMs: Long? = null,
    val address: String = "",
    val department: String = "",
    val reportingTo: String = "",
    val permission: String = "",        // "Viewer" | "Field" | "Super" | ""
    val emergencyName: String = "",
    val emergencyRelation: String = "",
    val emergencyPhone: String = "",
    /** Work shift — "Shift1" (09:00-18:00) or "Shift2" (13:00-23:00). */
    val shift: String = "Shift1",
)

/**
 * Draft used by the "Add employee" form. Exists so the [WorkforceDirectory]
 * contract stays stable even as we keep adding HR fields.
 *
 * [photoUri] is a platform-neutral URI string for a locally-picked image
 * (e.g. an Android `content://...` URI). The Firestore writer uploads it to
 * Firebase Storage and stores the resulting download URL in `photoUrl`.
 */
data class EmployeeDraft(
    val name: String,
    val role: String,                   // job title / designation
    val email: String,
    /**
     * Initial password set by the admin. **Never persisted to Firestore** —
     * only used to create the Firebase Auth account on the secondary app.
     * Defaults to an empty string for back-compat (forms must populate it).
     */
    val password: String = "",
    val phone: String,
    val gender: String,                 // "Male" | "Female" | "Other"
    val employmentType: String,         // "Fulltime" | "Contract"
    val joiningDateMs: Long?,           // epoch millis (UTC) at midnight
    val salary: Double,
    val zone: String = "",
    val status: String = "Active",      // initial status
    val photoUri: String? = null,       // local URI; uploaded by the writer
    /* Extended HR fields */
    val dateOfBirthMs: Long? = null,
    val address: String = "",
    val department: String = "",
    val reportingTo: String = "",
    val permission: String = "Field",   // Viewer | Field | Super
    val emergencyName: String = "",
    val emergencyRelation: String = "",
    val emergencyPhone: String = "",
    /** Work shift — "Shift1" (09:00-18:00) or "Shift2" (13:00-23:00). */
    val shift: String = "Shift1",
)

data class TaskRecord(
    val id: String,
    val adminId: String,
    val userId: String?,
    val title: String,
    val location: String,
    val time: String,
    val day: String,
    val priority: String,      // "Low" | "Medium" | "High"
    val status: String,        // "Todo" | "InProgress" | "Done"
    val assigneeInitials: String = "",
)

data class InventoryRecord(
    val id: String,
    val name: String,
    val type: String,          // "equipment" | "material"
    val price: Double,
    val quantity: Int,
)

data class AttendanceRecord(
    val id: String,
    val userId: String,
    /** Epoch millis from Firestore Timestamp (or 0 if not yet set). */
    val dateMs: Long,
    val checkInMs: Long,
    val checkInLat: Double? = null,
    val checkInLng: Double? = null,
    /** "ON_TIME" or "LATE" — relative to the user's assigned shift. */
    val checkInStatus: String = "ON_TIME",
    val checkOutMs: Long? = null,
    val checkOutLat: Double? = null,
    val checkOutLng: Double? = null,
    /** "CHECKED_IN" or "COMPLETED". */
    val status: String = "CHECKED_IN",
)

