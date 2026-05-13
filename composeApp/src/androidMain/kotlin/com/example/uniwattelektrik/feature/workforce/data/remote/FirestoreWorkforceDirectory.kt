package com.example.uniwattelektrik.feature.workforce.data.remote

import com.example.uniwattelektrik.core.AppLog
import com.example.uniwattelektrik.core.storage.CloudinaryUploader
import com.google.firebase.FirebaseApp
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout

private const val FIRESTORE_TIMEOUT_MS = 15_000L

private suspend fun <T> bounded(block: suspend () -> T): T = try {
    withTimeout(FIRESTORE_TIMEOUT_MS) { block() }
} catch (e: TimeoutCancellationException) {
    throw IllegalStateException(
        "Firestore did not respond in ${FIRESTORE_TIMEOUT_MS / 1000}s. " +
            "Verify the database is provisioned and security rules permit this operation.",
        e,
    )
}

private suspend fun <T> com.google.android.gms.tasks.Task<T>.awaitBounded(): T =
    bounded { this.await() }

/**
 * Safely reads a field that may be stored as a Firestore [Timestamp] (written
 * by the app via FieldValue.serverTimestamp) OR as a plain [Long] epoch-millis
 * (written by the seed script / external tools). Returns null when absent.
 */
private fun com.google.firebase.firestore.DocumentSnapshot.getMillis(field: String): Long? {
    val raw = get(field) ?: return null
    return when (raw) {
        is com.google.firebase.Timestamp -> raw.toDate().time
        is Long                          -> raw
        is Double                        -> raw.toLong()   // Firestore may deserialise integers as Double
        is Number                        -> raw.toLong()
        else                             -> null
    }
}

/**
 * Firestore-backed workforce directory using **flat top-level collections**.
 *
 * Collection layout:
 *   /admins/{adminId}
 *   /users/{userId}                   — adminId field
 *   /tasks/{taskId}                   — adminId + assignedUserId fields
 *   /attendance_logs/{attendanceId}   — adminId + userId fields
 *   /inventory_items/{itemId}         — adminId field
 *   /inventory_transactions/{txnId}   — adminId + userId fields
 *   /checkins/{checkinId}             — adminId + userId fields
 *   /notifications/{notificationId}   — adminId + userId fields
 *   /employee_map/{uid}               — auth lookup (unchanged)
 *   /admin_ids/{code}                 — auth lookup (unchanged)
 *
 * Security rules approach (field-based tenant isolation):
 *
 *   match /users/{userId} {
 *     allow read, write: if request.auth.uid == resource.data.adminId
 *                        || request.auth.uid == userId;
 *   }
 *   match /tasks/{taskId} {
 *     allow read:  if request.auth.uid == resource.data.adminId
 *                  || request.auth.uid == resource.data.assignedUserId;
 *     allow write: if request.auth.uid == resource.data.adminId;
 *   }
 *   // same pattern for attendance_logs, inventory_items,
 *   // inventory_transactions, checkins, notifications
 */
class FirestoreWorkforceDirectory(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
) : WorkforceDirectory {

    /** App context for ContentResolver — obtained without touching the constructor. */
    private val appContext get() = FirebaseApp.getInstance().applicationContext

    // ─── Employees ───────────────────────────────────────────────────────────

    override fun observeEmployees(adminId: String): Flow<List<EmployeeRecord>> = callbackFlow {
        AppLog.d("PATH", "observe $COL_USERS where adminId==$adminId")
        // NOTE: no server-side orderBy(createdAt) — that would require a
        // composite index (adminId ASC, createdAt DESC). We sort client-side
        // instead so the query works out-of-the-box without index deployment.
        val reg = firestore.collection(COL_USERS)
            .whereEqualTo("adminId", adminId)
            .addSnapshotListener { snap, err ->
                if (err != null) {
                    AppLog.e("PATH", "observeEmployees failed: ${err.message}")
                    close(err); return@addSnapshotListener
                }
                val list = snap?.documents.orEmpty().map { d ->
                    val createdAt = d.getMillis("createdAt") ?: 0L
                    createdAt to EmployeeRecord(
                        id                = d.id,
                        name              = d.getString("name") ?: "—",
                        role              = d.getString("roleTitle") ?: "Engineer",
                        phone             = d.getString("phone") ?: "",
                        zone              = d.getString("zone") ?: "—",
                        status            = normalizeEmployeeStatus(d.getString("status") ?: "active"),
                        tasksOpen         = (d.getLong("tasksOpen") ?: 0L).toInt(),
                        email             = d.getString("email") ?: "",
                        gender            = d.getString("gender") ?: "",
                        employmentType    = d.getString("employmentType") ?: "",
                        joiningDateMs     = d.getMillis("joiningDate"),
                        salary            = d.getDouble("salary") ?: 0.0,
                        photoUrl          = d.getString("photoUrl") ?: "",
                        dateOfBirthMs     = d.getMillis("dateOfBirth"),
                        address           = d.getString("address") ?: "",
                        department        = d.getString("department") ?: "",
                        reportingTo       = d.getString("reportingTo") ?: "",
                        permission        = d.getString("permission") ?: "",
                        emergencyName     = d.getString("emergencyName") ?: "",
                        emergencyRelation = d.getString("emergencyRelation") ?: "",
                        emergencyPhone    = d.getString("emergencyPhone") ?: "",
                        shift             = d.getString("shift") ?: "Shift1",
                        updatedAt         = d.getMillis("updatedAt"),
                        deletedAt         = d.getMillis("deletedAt"),
                        fcmToken          = d.getString("fcmToken") ?: "",
                    )
                }.sortedByDescending { it.first }.map { it.second }
                trySend(list)
            }
        awaitClose { reg.remove() }
    }

    override suspend fun addEmployee(
        adminId: String,
        uid: String,
        draft: EmployeeDraft,
    ): EmployeeRecord {
        val ref = firestore.collection(COL_USERS).document(uid)
        AppLog.i("PATH", "write $COL_USERS/$uid")

        val uploadedPhotoUrl: String? = draft.photoUri?.takeIf { it.isNotBlank() }?.let { uri ->
            runCatching { uploadEmployeePhoto(adminId, uid, uri) }
                .onFailure { AppLog.w("PATH", "photo upload failed: ${it.message}") }
                .getOrNull()
        }

        val data = mutableMapOf<String, Any?>(
            "uid"                to uid,
            "adminId"            to adminId,
            "createdBy"          to adminId,
            "mustChangePassword" to true,
            "name"               to draft.name,
            "phone"              to draft.phone,
            "email"              to draft.email,
            "gender"             to draft.gender,
            "employmentType"     to draft.employmentType,
            "salary"             to draft.salary,
            "role"               to "user",
            "roleTitle"          to draft.role,
            "zone"               to draft.zone,
            "status"             to draft.status.lowercase(),
            "tasksOpen"          to 0L,
            "address"            to draft.address,
            "department"         to draft.department,
            "reportingTo"        to draft.reportingTo,
            "permission"         to draft.permission,
            "emergencyName"      to draft.emergencyName,
            "emergencyRelation"  to draft.emergencyRelation,
            "emergencyPhone"     to draft.emergencyPhone,
            "shift"              to draft.shift,
            "createdAt"          to FieldValue.serverTimestamp(),
            "updatedAt"          to FieldValue.serverTimestamp(),
        )
        draft.joiningDateMs?.let { data["joiningDate"] = Timestamp(java.util.Date(it)) }
        draft.dateOfBirthMs?.let { data["dateOfBirth"] = Timestamp(java.util.Date(it)) }
        uploadedPhotoUrl?.let  { data["photoUrl"]     = it }

        ref.set(data).awaitBounded()
        AppLog.i("PATH", "  ↳ $COL_USERS/$uid saved (${data.size} fields)")

        runCatching {
            firestore.collection(COL_EMPLOYEE_MAP).document(uid)
                .set(mapOf("adminId" to adminId, "mustChangePassword" to true))
                .awaitBounded()
        }.onFailure {
            AppLog.w("PATH", "employee_map write failed (non-fatal): ${it.message}")
        }

        return EmployeeRecord(
            id                = uid,
            name              = draft.name,
            role              = draft.role,
            phone             = draft.phone,
            zone              = draft.zone,
            status            = normalizeEmployeeStatus(draft.status),
            email             = draft.email,
            gender            = draft.gender,
            employmentType    = draft.employmentType,
            joiningDateMs     = draft.joiningDateMs,
            salary            = draft.salary,
            photoUrl          = uploadedPhotoUrl ?: "",
            dateOfBirthMs     = draft.dateOfBirthMs,
            address           = draft.address,
            department        = draft.department,
            reportingTo       = draft.reportingTo,
            permission        = draft.permission,
            emergencyName     = draft.emergencyName,
            emergencyRelation = draft.emergencyRelation,
            emergencyPhone    = draft.emergencyPhone,
            shift             = draft.shift,
        )
    }

    override suspend fun updateEmployee(
        adminId: String,
        employeeId: String,
        draft: EmployeeDraft,
    ): EmployeeRecord {
        val ref = firestore.collection(COL_USERS).document(employeeId)
        AppLog.i("PATH", "update $COL_USERS/$employeeId")

        // Optional new photo upload — only swap when a fresh URI was supplied.
        val newPhotoUrl: String? = draft.photoUri?.takeIf { it.isNotBlank() }?.let { uri ->
            runCatching { uploadEmployeePhoto(adminId, employeeId, uri) }
                .onFailure { AppLog.w("PATH", "photo upload failed: ${it.message}") }
                .getOrNull()
        }

        // Mutable employee fields. Notably DOES NOT include: uid, adminId,
        // createdBy, createdAt, role ("user"/"admin" rank), mustChangePassword.
        val data = mutableMapOf<String, Any?>(
            "name"               to draft.name,
            "phone"              to draft.phone,
            "email"              to draft.email,
            "gender"             to draft.gender,
            "employmentType"     to draft.employmentType,
            "salary"             to draft.salary,
            "roleTitle"          to draft.role,
            "zone"               to draft.zone,
            "status"             to draft.status.lowercase(),
            "address"            to draft.address,
            "department"         to draft.department,
            "reportingTo"        to draft.reportingTo,
            "permission"         to draft.permission,
            "emergencyName"      to draft.emergencyName,
            "emergencyRelation"  to draft.emergencyRelation,
            "emergencyPhone"     to draft.emergencyPhone,
            "shift"              to draft.shift,
            "updatedAt"          to FieldValue.serverTimestamp(),
        )
        draft.joiningDateMs?.let { data["joiningDate"] = Timestamp(java.util.Date(it)) }
        draft.dateOfBirthMs?.let { data["dateOfBirth"] = Timestamp(java.util.Date(it)) }
        newPhotoUrl?.let          { data["photoUrl"]    = it }

        ref.update(data).awaitBounded()
        AppLog.i("PATH", "  ↳ $COL_USERS/$employeeId updated (${data.size} fields)")

        return EmployeeRecord(
            id                = employeeId,
            name              = draft.name,
            role              = draft.role,
            phone             = draft.phone,
            zone              = draft.zone,
            status            = normalizeEmployeeStatus(draft.status),
            email             = draft.email,
            gender            = draft.gender,
            employmentType    = draft.employmentType,
            joiningDateMs     = draft.joiningDateMs,
            salary            = draft.salary,
            photoUrl          = newPhotoUrl ?: "",
            dateOfBirthMs     = draft.dateOfBirthMs,
            address           = draft.address,
            department        = draft.department,
            reportingTo       = draft.reportingTo,
            permission        = draft.permission,
            emergencyName     = draft.emergencyName,
            emergencyRelation = draft.emergencyRelation,
            emergencyPhone    = draft.emergencyPhone,
            shift             = draft.shift,
        )
    }

    override suspend fun markPasswordChanged(adminId: String, uid: String) {
        AppLog.i("PATH", "update $COL_USERS/$uid mustChangePassword=false")
        firestore.collection(COL_USERS).document(uid)
            .update(mapOf(
                "mustChangePassword" to false,
                "passwordChangedAt"  to FieldValue.serverTimestamp(),
                "updatedAt"          to FieldValue.serverTimestamp(),
            ))
            .awaitBounded()
        // Sync the flag in employee_map — the login flow reads this doc on every sign-in.
        runCatching {
            firestore.collection(COL_EMPLOYEE_MAP).document(uid)
                .update("mustChangePassword", false)
                .awaitBounded()
        }.onFailure {
            AppLog.w("PATH", "employee_map mustChangePassword sync failed (non-fatal): ${it.message}")
        }
    }

    override suspend fun updateEmployeePermission(adminId: String, employeeId: String, permission: String) {
        AppLog.i("PATH", "update $COL_USERS/$employeeId permission=$permission")
        bounded {
            firestore.collection(COL_USERS).document(employeeId)
                .update(mapOf(
                    "permission" to permission,
                    "updatedAt"  to FieldValue.serverTimestamp(),
                ))
                .await()
        }
    }

    override suspend fun updateEmployeeStatus(adminId: String, employeeId: String, status: String) {
        AppLog.i("PATH", "update $COL_USERS/$employeeId status=$status")
        bounded {
            firestore.collection(COL_USERS).document(employeeId)
                .update(mapOf(
                    "status"    to status.lowercase(),
                    "updatedAt" to FieldValue.serverTimestamp(),
                ))
                .await()
        }
    }

    override suspend fun saveFcmToken(adminId: String, uid: String, token: String) {
        AppLog.i("PATH", "update $COL_USERS/$uid fcmToken")
        firestore.collection(COL_USERS).document(uid)
            .update(mapOf(
                "fcmToken"  to token,
                "updatedAt" to FieldValue.serverTimestamp(),
            ))
            .awaitBounded()
    }

    /** Public override — replaces the employee's photo and updates Firestore. */
    override suspend fun updateEmployeePhoto(
        adminId: String,
        employeeId: String,
        contentUri: String,
    ): String {
        val url = uploadEmployeePhoto(adminId, employeeId, contentUri)
        bounded {
            firestore.collection(COL_USERS).document(employeeId)
                .update(mapOf(
                    "photoUrl"  to url,
                    "updatedAt" to FieldValue.serverTimestamp(),
                ))
                .await()
        }
        AppLog.i("PATH", "updateEmployeePhoto $employeeId → $url")
        return url
    }

    private suspend fun uploadEmployeePhoto(
        adminId: String,
        userId: String,
        contentUri: String,
    ): String = CloudinaryUploader.upload(
        context    = appContext,
        contentUri = contentUri,
        folder     = "employee-photos/$adminId",
        publicId   = userId,
    )

    // ─── Tasks ───────────────────────────────────────────────────────────────

    override fun observeTasksForAdmin(adminId: String): Flow<List<TaskRecord>> = callbackFlow {
        AppLog.d("PATH", "observe $COL_TASKS where adminId==$adminId")
        // No server-side orderBy → no composite index required. Sort below.
        val reg = firestore.collection(COL_TASKS)
            .whereEqualTo("adminId", adminId)
            .addSnapshotListener { snap, err ->
                if (err != null) {
                    AppLog.e("PATH", "observeTasksForAdmin failed: ${err.message}")
                    close(err); return@addSnapshotListener
                }
                val list = snap?.documents.orEmpty()
                    .map { d -> (d.getMillis("createdAt") ?: 0L) to toTask(d) }
                    .sortedByDescending { it.first }
                    .map { it.second }
                trySend(list)
            }
        awaitClose { reg.remove() }
    }

    override fun observeTasksForUser(userId: String): Flow<List<TaskRecord>> = callbackFlow {
        AppLog.d("PATH", "observe $COL_TASKS where assignedUserId==$userId OR assignedUserIds contains $userId")
        // Two parallel snapshot listeners — one for the legacy single-assignee
        // field (`assignedUserId == userId`) and one for the new multi-
        // assignee array (`assignedUserIds array-contains userId`). Results
        // are merged & de-duplicated by doc id so the UI sees a single list.
        //
        // Firestore does not support OR queries across different fields with
        // a snapshot listener (`Filter.or` requires v24.4+ and composite
        // index registration). Running two listeners is functionally
        // equivalent and works on any Firestore version.
        val legacyList = java.util.concurrent.ConcurrentHashMap<String, TaskRecord>()
        val arrayList  = java.util.concurrent.ConcurrentHashMap<String, TaskRecord>()

        fun emit() {
            // Array doc takes precedence (it's the canonical shape going
            // forward). Legacy entries fill the gap for old single-assignee
            // docs that never got migrated.
            val merged = (legacyList + arrayList).values
                .sortedByDescending { it.createdAtMs ?: 0L }
            trySend(merged.toList())
        }

        val regLegacy = firestore.collection(COL_TASKS)
            .whereEqualTo("assignedUserId", userId)
            .addSnapshotListener { snap, err ->
                if (err != null) {
                    AppLog.e("PATH", "observeTasksForUser(legacy) failed: ${err.message}")
                    close(err); return@addSnapshotListener
                }
                legacyList.clear()
                snap?.documents.orEmpty().forEach { d ->
                    val t = toTask(d); legacyList[t.id] = t
                }
                emit()
            }
        val regArray = firestore.collection(COL_TASKS)
            .whereArrayContains("assignedUserIds", userId)
            .addSnapshotListener { snap, err ->
                if (err != null) {
                    AppLog.e("PATH", "observeTasksForUser(array) failed: ${err.message}")
                    // Don't close — legacy listener may still be feeding data.
                    return@addSnapshotListener
                }
                arrayList.clear()
                snap?.documents.orEmpty().forEach { d ->
                    val t = toTask(d); arrayList[t.id] = t
                }
                emit()
            }
        awaitClose { regLegacy.remove(); regArray.remove() }
    }

    override suspend fun addTask(
        adminId: String,
        userId: String?,
        title: String,
        location: String,
        time: String,
        day: String,
        priority: String,
        description: String,
        departmentId: String,
        departmentName: String,
        equipmentId: String,
        equipmentName: String,
        checklist: List<ChecklistItem>,
        attachments: List<String>,
        address: String,
        latitude: Double?,
        longitude: Double?,
        dueDate: Long?,
        ownerAdminName: String,
        assigneeName: String,
        notifyAssignee: Boolean,
        assigneeIds: List<String>,
        assigneeNames: List<String>,
    ): TaskRecord {
        // Reconcile single + multi inputs. Multi-assignee callers pass non-
        // empty arrays; legacy callers pass just userId/assigneeName.
        val effectiveIds: List<String> = (
            if (assigneeIds.isNotEmpty()) assigneeIds
            else listOfNotNull(userId?.takeIf { it.isNotBlank() })
        ).distinct()
        val effectiveNames: List<String> = when {
            assigneeNames.isNotEmpty() -> assigneeNames
            assigneeName.isNotBlank()  -> listOf(assigneeName)
            else                       -> emptyList()
        }
        val legacyUid  = effectiveIds.firstOrNull()
        val legacyName = effectiveNames.firstOrNull().orEmpty()

        val ref = firestore.collection(COL_TASKS).document()
        AppLog.i("PATH", "write $COL_TASKS/${ref.id}")
        ref.set(mapOf(
            "adminId"         to adminId,
            "ownerAdminId"    to adminId,
            "ownerAdminName"  to ownerAdminName,
            // Legacy single-assignee mirror — old clients read these.
            "assignedUserId"  to legacyUid,
            "assigneeName"    to legacyName,
            // Canonical multi-assignee arrays — new clients query via these.
            "assignedUserIds" to effectiveIds,
            "assigneeNames"   to effectiveNames,
            "title"           to title,
            "description"     to description,
            "priority"        to priority.lowercase(),
            "status"          to "pending",
            "location"        to location,
            "time"            to time,
            "day"             to day,
            "scheduledDate"   to todayMidnightUtc(),
            "dueDate"         to dueDate?.let { Timestamp(java.util.Date(it)) },
            "completedAt"     to null,
            "acceptedAt"      to null,
            "departmentId"    to departmentId,
            "departmentName"  to departmentName,
            "equipmentId"     to equipmentId,
            "equipmentName"   to equipmentName,
            "checklist"       to checklist.map { mapOf("text" to it.text, "done" to it.done) },
            "attachments"     to attachments,
            "address"         to address,
            "latitude"        to latitude,
            "longitude"       to longitude,
            "notifyAssignee"  to notifyAssignee,
            "createdAt"       to FieldValue.serverTimestamp(),
            "updatedAt"       to FieldValue.serverTimestamp(),
        )).awaitBounded()

        // Increment tasksOpen for every assignee (each person carries one
        // open-task counter independent of how many co-assignees exist).
        effectiveIds.forEach { uid ->
            runCatching {
                firestore.collection(COL_USERS).document(uid)
                    .update("tasksOpen", FieldValue.increment(1))
                    .awaitBounded()
            }.onFailure {
                AppLog.w("PATH", "tasksOpen++ failed for $uid (non-fatal): ${it.message}")
            }
        }

        return TaskRecord(
            id              = ref.id,
            adminId         = adminId,
            userId          = legacyUid,
            title           = title,
            description     = description,
            location        = location,
            time            = time,
            day             = day,
            priority        = priority,
            status          = "Todo",
            assigneeName    = legacyName,
            assignedUserIds = effectiveIds,
            assigneeNames   = effectiveNames,
            ownerAdminId    = adminId,
            ownerAdminName  = ownerAdminName,
            dueDate         = dueDate,
            departmentId    = departmentId,
            departmentName  = departmentName,
            equipmentId     = equipmentId,
            equipmentName   = equipmentName,
            checklist       = checklist,
            attachments     = attachments,
            address         = address,
            latitude        = latitude,
            longitude       = longitude,
            notifyAssignee  = notifyAssignee,
        )
    }

    override suspend fun acceptTask(
        taskId: String,
        lat: Double?,
        lon: Double?,
    ): TaskRecord {
        AppLog.i("PATH", "update $COL_TASKS/$taskId acceptTask lat=$lat lon=$lon")
        val ref = firestore.collection(COL_TASKS).document(taskId)
        val updates = mutableMapOf<String, Any?>(
            "status"     to "in_progress",
            "acceptedAt" to FieldValue.serverTimestamp(),
            "updatedAt"  to FieldValue.serverTimestamp(),
        )
        lat?.let { updates["acceptedLat"] = it }
        lon?.let { updates["acceptedLon"] = it }
        ref.update(updates).awaitBounded()
        return toTask(ref.get().awaitBounded())
    }

    override suspend fun updateTask(
        taskId: String,
        adminId: String,
        userId: String?,
        title: String,
        location: String,
        time: String,
        day: String,
        priority: String,
        description: String,
        departmentId: String,
        departmentName: String,
        equipmentId: String,
        equipmentName: String,
        checklist: List<ChecklistItem>,
        attachments: List<String>,
        address: String,
        latitude: Double?,
        longitude: Double?,
        dueDate: Long?,
        assigneeName: String,
        notifyAssignee: Boolean,
        assigneeIds: List<String>,
        assigneeNames: List<String>,
    ): TaskRecord {
        val ref = firestore.collection(COL_TASKS).document(taskId)
        AppLog.i("PATH", "update $COL_TASKS/$taskId")

        // Read existing doc to detect assignee changes for tasksOpen rebalance.
        val before = runCatching { ref.get().awaitBounded() }.getOrNull()
        val prevUserId = before?.getString("assignedUserId")
        @Suppress("UNCHECKED_CAST")
        val prevAssigneeIds = (before?.get("assignedUserIds") as? List<String>)
            ?: listOfNotNull(prevUserId?.takeIf { it.isNotBlank() })
        val prevStatus = before?.getString("status") ?: "pending"
        val isOpen = prevStatus.lowercase() !in setOf("completed", "done")

        // Reconcile single + multi inputs (mirrors addTask).
        val effectiveIds: List<String> = (
            if (assigneeIds.isNotEmpty()) assigneeIds
            else listOfNotNull(userId?.takeIf { it.isNotBlank() })
        ).distinct()
        val effectiveNames: List<String> = when {
            assigneeNames.isNotEmpty() -> assigneeNames
            assigneeName.isNotBlank()  -> listOf(assigneeName)
            else                       -> emptyList()
        }
        val legacyUid  = effectiveIds.firstOrNull()
        val legacyName = effectiveNames.firstOrNull().orEmpty()

        ref.update(mapOf(
            "assignedUserId"  to legacyUid,
            "assigneeName"    to legacyName,
            "assignedUserIds" to effectiveIds,
            "assigneeNames"   to effectiveNames,
            "title"           to title,
            "description"     to description,
            "priority"        to priority.lowercase(),
            "location"        to location,
            "time"            to time,
            "day"             to day,
            "dueDate"         to dueDate?.let { Timestamp(java.util.Date(it)) },
            "departmentId"    to departmentId,
            "departmentName"  to departmentName,
            "equipmentId"     to equipmentId,
            "equipmentName"   to equipmentName,
            "checklist"       to checklist.map { mapOf("text" to it.text, "done" to it.done) },
            "attachments"     to attachments,
            "address"         to address,
            "latitude"        to latitude,
            "longitude"       to longitude,
            "notifyAssignee"  to notifyAssignee,
            "updatedAt"       to FieldValue.serverTimestamp(),
        )).awaitBounded()

        // Rebalance tasksOpen against the assignee-set delta when the task
        // is still open. Each unique uid that joined gets +1, each uid that
        // left gets -1.
        if (isOpen) {
            val prevSet = prevAssigneeIds.toSet()
            val nextSet = effectiveIds.toSet()
            val added   = nextSet - prevSet
            val removed = prevSet - nextSet
            added.forEach { uid ->
                runCatching {
                    firestore.collection(COL_USERS).document(uid)
                        .update("tasksOpen", FieldValue.increment(1)).awaitBounded()
                }.onFailure { AppLog.w("PATH", "tasksOpen++ failed: ${it.message}") }
            }
            removed.forEach { uid ->
                runCatching {
                    firestore.collection(COL_USERS).document(uid)
                        .update("tasksOpen", FieldValue.increment(-1)).awaitBounded()
                }.onFailure { AppLog.w("PATH", "tasksOpen-- failed: ${it.message}") }
            }
        }

        return toTask(ref.get().awaitBounded())
    }

    override suspend fun reassignTask(
        taskId: String,
        adminId: String,
        newAssigneeIds: List<String>,
        newAssigneeNames: List<String>,
    ): TaskRecord {
        val ref = firestore.collection(COL_TASKS).document(taskId)
        AppLog.i("PATH", "reassign $COL_TASKS/$taskId → $newAssigneeIds")

        val before = runCatching { ref.get().awaitBounded() }.getOrNull()
        val prevUserId = before?.getString("assignedUserId")
        @Suppress("UNCHECKED_CAST")
        val prevAssigneeIds = (before?.get("assignedUserIds") as? List<String>)
            ?: listOfNotNull(prevUserId?.takeIf { it.isNotBlank() })
        val prevStatus = before?.getString("status") ?: "pending"
        val isOpen = prevStatus.lowercase() !in setOf("completed", "done")

        val effectiveIds = newAssigneeIds.filter { it.isNotBlank() }.distinct()
        val effectiveNames = newAssigneeNames
        val legacyUid  = effectiveIds.firstOrNull()
        val legacyName = effectiveNames.firstOrNull().orEmpty()

        ref.update(mapOf(
            "assignedUserId"       to legacyUid,
            "assigneeName"         to legacyName,
            "assignedUserIds"      to effectiveIds,
            "assigneeNames"        to effectiveNames,
            "reassignedAt"         to FieldValue.serverTimestamp(),
            "reassignmentHistory"  to FieldValue.arrayUnion(
                mapOf(
                    "fromUserIds" to prevAssigneeIds,
                    "toUserIds"   to effectiveIds,
                    "atMs"        to System.currentTimeMillis(),
                )
            ),
            "updatedAt"            to FieldValue.serverTimestamp(),
        )).awaitBounded()

        if (isOpen) {
            val prevSet = prevAssigneeIds.toSet()
            val nextSet = effectiveIds.toSet()
            (nextSet - prevSet).forEach { uid ->
                runCatching {
                    firestore.collection(COL_USERS).document(uid)
                        .update("tasksOpen", FieldValue.increment(1)).awaitBounded()
                }.onFailure { AppLog.w("PATH", "tasksOpen++ failed: ${it.message}") }
            }
            (prevSet - nextSet).forEach { uid ->
                runCatching {
                    firestore.collection(COL_USERS).document(uid)
                        .update("tasksOpen", FieldValue.increment(-1)).awaitBounded()
                }.onFailure { AppLog.w("PATH", "tasksOpen-- failed: ${it.message}") }
            }
        }

        return toTask(ref.get().awaitBounded())
    }

    override fun observeTaskNotes(taskId: String): Flow<List<TaskNote>> = callbackFlow {
        AppLog.d("PATH", "observe $COL_TASKS/$taskId/notes")
        val reg = firestore.collection(COL_TASKS).document(taskId)
            .collection("notes")
            .addSnapshotListener { snap, err ->
                if (err != null) {
                    AppLog.e("PATH", "observeTaskNotes failed: ${err.message}")
                    close(err); return@addSnapshotListener
                }
                val list = snap?.documents.orEmpty().map { d ->
                    val ts = d.getMillis("createdAt") ?: 0L
                    ts to TaskNote(
                        id              = d.id,
                        taskId          = taskId,
                        authorId        = d.getString("authorId") ?: "",
                        authorName      = d.getString("authorName") ?: "",
                        role            = d.getString("role") ?: "user",
                        message         = d.getString("message") ?: "",
                        createdAtMs     = ts.takeIf { it > 0 },
                        voiceUrl        = d.getString("voiceUrl"),
                        voiceDurationMs = d.getLong("voiceDurationMs"),
                    )
                }.sortedBy { it.first }.map { it.second }
                trySend(list)
            }
        awaitClose { reg.remove() }
    }

    override suspend fun addTaskNote(
        taskId: String,
        authorId: String,
        authorName: String,
        role: String,
        message: String,
        voiceUrl: String?,
        voiceDurationMs: Long?,
    ): TaskNote {
        val ref = firestore.collection(COL_TASKS).document(taskId)
            .collection("notes").document()
        AppLog.i("PATH", "write $COL_TASKS/$taskId/notes/${ref.id}")
        val payload = buildMap {
            put("authorId",   authorId)
            put("authorName", authorName)
            put("role",       role)
            put("message",    message)
            put("createdAt",  FieldValue.serverTimestamp())
            if (voiceUrl != null) {
                put("voiceUrl",        voiceUrl)
                put("voiceDurationMs", voiceDurationMs ?: 0L)
            }
        }
        ref.set(payload).awaitBounded()
        // Also bump the task's updatedAt so listeners refresh.
        runCatching {
            firestore.collection(COL_TASKS).document(taskId)
                .update("updatedAt", FieldValue.serverTimestamp())
                .awaitBounded()
        }
        return TaskNote(
            id              = ref.id,
            taskId          = taskId,
            authorId        = authorId,
            authorName      = authorName,
            role            = role,
            message         = message,
            createdAtMs     = System.currentTimeMillis(),
            voiceUrl        = voiceUrl,
            voiceDurationMs = voiceDurationMs,
        )
    }

    override suspend fun updateTaskStatus(
        adminId: String,
        taskId: String,
        assignedUserId: String?,
        newStatus: String,
    ): TaskRecord {
        val firestoreStatus = when (newStatus.lowercase()) {
            "todo"                                  -> "pending"
            "inprogress"                            -> "in_progress"
            "inreview", "review", "in_review"       -> "in_review"
            "done"                                  -> "completed"
            else                                    -> newStatus.lowercase()
        }
        val ref = firestore.collection(COL_TASKS).document(taskId)

        // Read the previous status so we can detect "completed → active"
        // (a reopen) and keep counters / reopen log consistent.
        val before = runCatching { ref.get().awaitBounded() }.getOrNull()
        val prevStatus = before?.getString("status")?.lowercase() ?: "pending"
        val wasCompleted   = prevStatus == "completed"
        val nowCompleted   = firestoreStatus == "completed"
        val isReopen       = wasCompleted && !nowCompleted

        // Resolve the full assignee set (multi-assignee aware) so counters
        // rebalance correctly for tasks with co-assignees.
        @Suppress("UNCHECKED_CAST")
        val arrayIds = (before?.get("assignedUserIds") as? List<String>) ?: emptyList()
        val allAssigneeUids: List<String> = (
            arrayIds + listOfNotNull(
                before?.getString("assignedUserId")?.takeIf { it.isNotBlank() }
            ) + listOfNotNull(assignedUserId?.takeIf { it.isNotBlank() })
        ).filter { it.isNotBlank() }.distinct()

        val updates = mutableMapOf<String, Any?>(
            "status"    to firestoreStatus,
            "updatedAt" to FieldValue.serverTimestamp(),
        )
        if (nowCompleted) {
            updates["completedAt"] = FieldValue.serverTimestamp()
            allAssigneeUids.forEach { uid ->
                runCatching {
                    firestore.collection(COL_USERS).document(uid)
                        .update("tasksOpen", FieldValue.increment(-1))
                        .awaitBounded()
                }.onFailure {
                    AppLog.w("PATH", "tasksOpen-- failed for $uid (non-fatal): ${it.message}")
                }
            }
        } else if (isReopen) {
            // Reopen: append a server-side timestamp to the audit log and
            // wipe the stale completedAt so the task no longer looks done.
            updates["reopens"]     = FieldValue.arrayUnion(System.currentTimeMillis())
            updates["completedAt"] = null
            allAssigneeUids.forEach { uid ->
                runCatching {
                    firestore.collection(COL_USERS).document(uid)
                        .update("tasksOpen", FieldValue.increment(1))
                        .awaitBounded()
                }.onFailure {
                    AppLog.w("PATH", "tasksOpen re-increment failed for $uid (non-fatal): ${it.message}")
                }
            }
        }
        ref.update(updates).awaitBounded()
        return toTask(ref.get().awaitBounded())
    }

    override suspend fun completeTaskWithSignoff(
        adminId: String,
        taskId: String,
        assignedUserId: String?,
        signoffDescription: String,
        downtimeMinutes: Int,
        rca: String,
        materialsUsed: List<MaterialUsedItem>,
        startTimeMs: Long?,
        endTimeMs: Long,
    ): TaskRecord {
        val totalWorkDurationMs = if (startTimeMs != null && startTimeMs > 0L)
            (endTimeMs - startTimeMs).coerceAtLeast(0L) else null

        val ref = firestore.collection(COL_TASKS).document(taskId)
        val updates = mutableMapOf<String, Any?>(
            "status"               to "completed",
            "completedAt"          to FieldValue.serverTimestamp(),
            "updatedAt"            to FieldValue.serverTimestamp(),
            "signoffDescription"   to signoffDescription,
            "downtimeMinutes"      to downtimeMinutes,
            "rca"                  to rca,
            "totalWorkDurationMs"  to totalWorkDurationMs,
            "materialsUsed"        to materialsUsed.map {
                mapOf("itemId" to it.itemId, "itemName" to it.itemName, "quantity" to it.quantity)
            },
        )
        ref.update(updates).awaitBounded()

        // Deduct inventory and record transactions (non-fatal — don't block completion)
        materialsUsed.filter { it.quantity > 0 }.forEach { used ->
            runCatching {
                firestore.collection(COL_SPARE_ITEMS).document(used.itemId)
                    .update("stockQty", FieldValue.increment(-used.quantity.toLong()))
                    .awaitBounded()
                // Log the transaction so the admin can see what was consumed
                val txRef = firestore.collection(COL_INVENTORY_TRANSACTIONS).document()
                txRef.set(mapOf(
                    "adminId"   to adminId,
                    "userId"    to (assignedUserId ?: ""),
                    "itemId"    to used.itemId,
                    "itemName"  to used.itemName,
                    "type"      to "issue",
                    "quantity"  to used.quantity,
                    "taskId"    to taskId,
                    "createdAt" to FieldValue.serverTimestamp(),
                )).awaitBounded()
            }.onFailure {
                AppLog.w("PATH", "inventory deduction failed for ${used.itemId}: ${it.message}")
            }
        }

        // Decrement tasksOpen counter
        if (!assignedUserId.isNullOrBlank()) {
            runCatching {
                firestore.collection(COL_USERS).document(assignedUserId)
                    .update("tasksOpen", FieldValue.increment(-1))
                    .awaitBounded()
            }.onFailure {
                AppLog.w("PATH", "tasksOpen decrement failed (non-fatal): ${it.message}")
            }
        }

        return toTask(ref.get().awaitBounded())
    }

    override suspend fun setChecklistItemDone(
        taskId: String,
        index: Int,
        done: Boolean,
    ) {
        val ref = firestore.collection(COL_TASKS).document(taskId)
        AppLog.i("PATH", "update $COL_TASKS/$taskId checklist[$index].done=$done")
        // Read-modify-write so we can preserve siblings; Firestore can't update
        // a single nested array element by index without rewriting the array.
        val snap = ref.get().awaitBounded()
        @Suppress("UNCHECKED_CAST")
        val raw = (snap.get("checklist") as? List<Map<String, Any?>>) ?: emptyList()
        if (index !in raw.indices) return
        val updated = raw.toMutableList().also {
            val item = it[index].toMutableMap()
            item["done"] = done
            it[index] = item
        }
        ref.update(
            mapOf(
                "checklist" to updated,
                "updatedAt" to FieldValue.serverTimestamp(),
            )
        ).awaitBounded()
    }

    override suspend fun uploadTaskAttachment(adminId: String, contentUri: String): String =
        CloudinaryUploader.upload(
            context    = appContext,
            contentUri = contentUri,
            folder     = "task-attachments/$adminId",
            publicId   = "att_${System.currentTimeMillis()}",
        )

    override suspend fun uploadVoiceNote(adminId: String, contentUri: String): String =
        // Cloudinary stores audio under the "video" resource type — uploading
        // an .m4a to the /image/upload endpoint fails silently (HTTP 400).
        // resourceType/mimeType/extension are explicit to keep this surviving
        // any future automated reformat passes.
        CloudinaryUploader.upload(
            context      = appContext,
            contentUri   = contentUri,
            folder       = "voice-notes/$adminId",
            publicId     = "vn_${System.currentTimeMillis()}",
            resourceType = "video",
            mimeType     = "audio/mp4",
            extension    = "m4a",
        )

    override suspend fun updateTaskAttachments(taskId: String, urls: List<String>) {
        AppLog.i("PATH", "update $COL_TASKS/$taskId attachments=${urls.size}")
        firestore.collection(COL_TASKS).document(taskId)
            .update(mapOf(
                "attachments" to urls,
                "updatedAt"   to FieldValue.serverTimestamp(),
            ))
            .awaitBounded()
    }

    private fun toTask(d: com.google.firebase.firestore.DocumentSnapshot): TaskRecord {
        @Suppress("UNCHECKED_CAST")
        val rawChecklist = (d.get("checklist") as? List<Map<String, Any?>>) ?: emptyList()
        @Suppress("UNCHECKED_CAST")
        val rawAttachments = (d.get("attachments") as? List<String>) ?: emptyList()
        return TaskRecord(
            id               = d.id,
            adminId          = d.getString("adminId") ?: "",
            userId           = d.getString("assignedUserId"),
            title            = d.getString("title") ?: "—",
            description      = d.getString("description") ?: "",
            location         = d.getString("location") ?: "—",
            time             = d.getString("time") ?: "",
            day              = d.getString("day") ?: "Today",
            priority         = normalizePriority(d.getString("priority") ?: "medium"),
            status           = normalizeTaskStatus(d.getString("status") ?: "pending"),
            assigneeInitials = (d.getString("assigneeInitials") ?: "").take(2),
            assigneeName     = d.getString("assigneeName") ?: "",
            assignedUserIds  = @Suppress("UNCHECKED_CAST")
                (d.get("assignedUserIds") as? List<String>)
                    ?.filter { it.isNotBlank() }
                    ?: emptyList(),
            assigneeNames    = @Suppress("UNCHECKED_CAST")
                (d.get("assigneeNames") as? List<String>)
                    ?: emptyList(),
            ownerAdminId     = d.getString("ownerAdminId") ?: (d.getString("adminId") ?: ""),
            ownerAdminName   = d.getString("ownerAdminName") ?: "",
            scheduledDateMs  = d.getMillis("scheduledDate"),
            dueDate          = d.getMillis("dueDate"),
            createdAtMs      = d.getMillis("createdAt"),
            acceptedAt       = d.getMillis("acceptedAt"),
            acceptedLat      = d.getDouble("acceptedLat"),
            acceptedLon      = d.getDouble("acceptedLon"),
            completedAt      = d.getMillis("completedAt"),
            updatedAt        = d.getMillis("updatedAt"),
            departmentId     = d.getString("departmentId") ?: "",
            departmentName   = d.getString("departmentName") ?: "",
            equipmentId      = d.getString("equipmentId") ?: "",
            equipmentName    = d.getString("equipmentName") ?: "",
            checklist        = rawChecklist.map {
                ChecklistItem(
                    text = it["text"] as? String ?: "",
                    done = it["done"] as? Boolean ?: false,
                )
            },
            attachments      = rawAttachments,
            address          = d.getString("address") ?: "",
            latitude         = d.getDouble("latitude"),
            longitude        = d.getDouble("longitude"),
            signoffDescription  = d.getString("signoffDescription") ?: "",
            downtimeMinutes     = (d.getLong("downtimeMinutes") ?: 0L).toInt(),
            rca                 = d.getString("rca") ?: "",
            totalWorkDurationMs = d.getLong("totalWorkDurationMs"),
            materialsUsed       = @Suppress("UNCHECKED_CAST")
                (d.get("materialsUsed") as? List<Map<String, Any?>>)
                    ?.map { m ->
                        MaterialUsedItem(
                            itemId   = m["itemId"]   as? String ?: "",
                            itemName = m["itemName"] as? String ?: "",
                            quantity = (m["quantity"] as? Long)?.toInt() ?: 0,
                        )
                    } ?: emptyList(),
            // Legacy tasks won't have this field set — default to true so
            // notifications keep working until the admin opts a task out.
            notifyAssignee      = d.getBoolean("notifyAssignee") ?: true,
            // Audit log of Done → !Done transitions. Firestore stores them
            // as a Long array; legacy tasks have no field → empty list.
            reopens             = @Suppress("UNCHECKED_CAST")
                (d.get("reopens") as? List<Number>)
                    ?.map { it.toLong() }
                    ?: emptyList(),
        )
    }

    // ─── Inventory ───────────────────────────────────────────────────────────

    override fun observeInventory(adminId: String): Flow<List<InventoryRecord>> = callbackFlow {
        AppLog.d("PATH", "observe $COL_INVENTORY_ITEMS where adminId==$adminId")
        val reg = firestore.collection(COL_INVENTORY_ITEMS)
            .whereEqualTo("adminId", adminId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snap, err ->
                if (err != null) { close(err); return@addSnapshotListener }
                val list = snap?.documents.orEmpty().map { d ->
                    InventoryRecord(
                        id       = d.id,
                        name     = d.getString("name") ?: "—",
                        type     = d.getString("type") ?: "material",
                        price    = d.getDouble("price") ?: 0.0,
                        quantity = (d.getLong("quantity") ?: 0L).toInt(),
                    )
                }
                trySend(list)
            }
        awaitClose { reg.remove() }
    }

    override suspend fun addInventoryItem(
        adminId: String,
        name: String,
        type: String,
        price: Double,
        quantity: Int,
    ): InventoryRecord {
        val ref = firestore.collection(COL_INVENTORY_ITEMS).document()
        AppLog.i("PATH", "write $COL_INVENTORY_ITEMS/${ref.id}")
        ref.set(mapOf(
            "adminId"   to adminId,
            "name"      to name,
            "type"      to type,
            "price"     to price,
            "quantity"  to quantity,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp(),
        )).awaitBounded()
        return InventoryRecord(ref.id, name, type, price, quantity)
    }

    override fun observeInventoryTransactions(adminId: String): Flow<List<InventoryTransaction>> = callbackFlow {
        AppLog.d("PATH", "observe $COL_INVENTORY_TRANSACTIONS where adminId==$adminId")
        val reg = firestore.collection(COL_INVENTORY_TRANSACTIONS)
            .whereEqualTo("adminId", adminId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snap, err ->
                if (err != null) { close(err); return@addSnapshotListener }
                val list = snap?.documents.orEmpty().map { d ->
                    InventoryTransaction(
                        id        = d.id,
                        adminId   = d.getString("adminId") ?: "",
                        userId    = d.getString("userId") ?: "",
                        itemId    = d.getString("itemId") ?: "",
                        itemName  = d.getString("itemName") ?: "",
                        type      = d.getString("type") ?: "",
                        quantity  = (d.getLong("quantity") ?: 0L).toInt(),
                        createdAt = d.getMillis("createdAt"),
                        // Linkage to source task (populated by completeTaskWithSignoff);
                        // null for manual deductions / pre-Phase-2 transactions.
                        taskId    = d.getString("taskId"),
                    )
                }
                trySend(list)
            }
        awaitClose { reg.remove() }
    }

    override fun observeMonthlyStockSnapshot(
        adminId: String,
        year: Int,
        month: Int,
    ): Flow<MonthlyStockSnapshot?> = callbackFlow {
        val key = "$year-${month.toString().padStart(2, '0')}"
        val path = "monthly_stock_snapshots/$adminId/months/$key"
        AppLog.d("PATH", "observe $path")
        val reg = firestore.document(path)
            .addSnapshotListener { doc, err ->
                if (err != null) {
                    AppLog.w("PATH", "observeMonthlyStockSnapshot failed: ${err.message}")
                    trySend(null); return@addSnapshotListener
                }
                if (doc == null || !doc.exists()) {
                    trySend(null); return@addSnapshotListener
                }
                @Suppress("UNCHECKED_CAST")
                val rawItems = (doc.get("items") as? List<Map<String, Any?>>) ?: emptyList()
                val parsed = MonthlyStockSnapshot(
                    year       = (doc.getLong("year")  ?: year.toLong()).toInt(),
                    month      = (doc.getString("month")?.toIntOrNull()
                        ?: doc.getLong("month")?.toInt()
                        ?: month),
                    items      = rawItems.map { m ->
                        SnapshotItem(
                            itemId   = m["itemId"]   as? String ?: "",
                            name     = m["name"]     as? String ?: "",
                            stockQty = (m["stockQty"] as? Number)?.toInt() ?: 0,
                            price    = (m["price"]    as? Number)?.toDouble() ?: 0.0,
                            value    = (m["value"]    as? Number)?.toDouble() ?: 0.0,
                        )
                    },
                    totalQty    = (doc.getLong("totalQty") ?: 0L).toInt(),
                    totalValue  = doc.getDouble("totalValue") ?: 0.0,
                    snapshotAtMs = doc.getMillis("snapshotAt"),
                )
                trySend(parsed)
            }
        awaitClose { reg.remove() }
    }

    override suspend fun addInventoryTransaction(
        adminId: String,
        userId: String,
        itemId: String,
        itemName: String,
        type: String,
        quantity: Int,
    ): InventoryTransaction {
        val ref = firestore.collection(COL_INVENTORY_TRANSACTIONS).document()
        AppLog.i("PATH", "write $COL_INVENTORY_TRANSACTIONS/${ref.id}")
        ref.set(mapOf(
            "adminId"   to adminId,
            "userId"    to userId,
            "itemId"    to itemId,
            "itemName"  to itemName,
            "type"      to type,
            "quantity"  to quantity,
            "createdAt" to FieldValue.serverTimestamp(),
        )).awaitBounded()
        return InventoryTransaction(ref.id, adminId, userId, itemId, itemName, type, quantity)
    }

    // ─── Attendance ──────────────────────────────────────────────────────────

    override fun observeAttendance(adminId: String): Flow<List<AttendanceRecord>> = callbackFlow {
        AppLog.d("PATH", "observe $COL_ATTENDANCE_LOGS where adminId==$adminId")
        // Flat collection — simple equality filter, no collectionGroup needed.
        val reg = firestore.collection(COL_ATTENDANCE_LOGS)
            .whereEqualTo("adminId", adminId)
            .orderBy("date", Query.Direction.DESCENDING)
            .addSnapshotListener { snap, err ->
                if (err != null) { close(err); return@addSnapshotListener }
                val list = snap?.documents.orEmpty().map { d ->
                    AttendanceRecord(
                        id            = d.id,
                        userId        = d.getString("userId") ?: "",
                        dateMs        = d.getMillis("date") ?: 0L,
                        checkInMs     = d.getMillis("checkIn") ?: 0L,
                        checkInLat    = d.getDouble("checkInLat"),
                        checkInLng    = d.getDouble("checkInLng"),
                        checkInStatus = d.getString("checkInStatus") ?: "ON_TIME",
                        checkOutMs    = d.getMillis("checkOut"),
                        checkOutLat   = d.getDouble("checkOutLat"),
                        checkOutLng   = d.getDouble("checkOutLng"),
                        status        = d.getString("attendanceStatus")
                            ?: if (d.getMillis("checkOut") != null) "COMPLETED" else "CHECKED_IN",
                    )
                }
                trySend(list)
            }
        awaitClose { reg.remove() }
    }

    /**
     * Employee-scoped attendance query — filters by `userId` so Firestore only
     * returns this employee's own records. The `whereEqualTo("adminId", adminId)`
     * query would return ALL employees under that admin; Firestore then rejects it
     * for non-admin callers because some returned docs have a different `userId`,
     * causing intermittent "permission denied" failures on secondary devices.
     */
    override fun observeMyAttendance(userId: String): Flow<List<AttendanceRecord>> = callbackFlow {
        AppLog.d("PATH", "observe $COL_ATTENDANCE_LOGS where userId==$userId")
        val reg = firestore.collection(COL_ATTENDANCE_LOGS)
            .whereEqualTo("userId", userId)
            .orderBy("date", Query.Direction.DESCENDING)
            .addSnapshotListener { snap, err ->
                if (err != null) { close(err); return@addSnapshotListener }
                val list = snap?.documents.orEmpty().map { d ->
                    AttendanceRecord(
                        id            = d.id,
                        userId        = d.getString("userId") ?: "",
                        dateMs        = d.getMillis("date") ?: 0L,
                        checkInMs     = d.getMillis("checkIn") ?: 0L,
                        checkInLat    = d.getDouble("checkInLat"),
                        checkInLng    = d.getDouble("checkInLng"),
                        checkInStatus = d.getString("checkInStatus") ?: "ON_TIME",
                        checkOutMs    = d.getMillis("checkOut"),
                        checkOutLat   = d.getDouble("checkOutLat"),
                        checkOutLng   = d.getDouble("checkOutLng"),
                        status        = d.getString("attendanceStatus")
                            ?: if (d.getMillis("checkOut") != null) "COMPLETED" else "CHECKED_IN",
                    )
                }
                trySend(list)
            }
        awaitClose { reg.remove() }
    }

    override suspend fun markCheckIn(
        adminId: String,
        userId: String,
        lat: Double?,
        lng: Double?,
        checkInStatus: String,
    ): AttendanceRecord {
        val ref = firestore.collection(COL_ATTENDANCE_LOGS).document()
        AppLog.i("PATH", "write $COL_ATTENDANCE_LOGS/${ref.id}")
        val data = mutableMapOf<String, Any?>(
            "adminId"          to adminId,
            "userId"           to userId,
            "date"             to todayMidnightUtc(),
            "checkIn"          to FieldValue.serverTimestamp(),
            "checkInStatus"    to checkInStatus,
            "attendanceStatus" to "CHECKED_IN",
            "checkOut"         to null,
        )
        lat?.let { data["checkInLat"] = it }
        lng?.let { data["checkInLng"] = it }
        ref.set(data).awaitBounded()
        return AttendanceRecord(
            id            = ref.id,
            userId        = userId,
            dateMs        = System.currentTimeMillis(),
            checkInMs     = System.currentTimeMillis(),
            checkInLat    = lat,
            checkInLng    = lng,
            checkInStatus = checkInStatus,
            status        = "CHECKED_IN",
        )
    }

    override suspend fun markCheckOut(
        adminId: String,
        userId: String,
        attendanceId: String,
        lat: Double?,
        lng: Double?,
    ) {
        AppLog.i("PATH", "update $COL_ATTENDANCE_LOGS/$attendanceId checkOut")
        val updates = mutableMapOf<String, Any?>(
            "checkOut"         to FieldValue.serverTimestamp(),
            "attendanceStatus" to "COMPLETED",
        )
        lat?.let { updates["checkOutLat"] = it }
        lng?.let { updates["checkOutLng"] = it }
        // Flat path — just collection + doc ID, no nested subcollection path.
        firestore.collection(COL_ATTENDANCE_LOGS).document(attendanceId)
            .update(updates)
            .awaitBounded()
    }

    // ─── Check-ins ───────────────────────────────────────────────────────────

    override fun observeCheckins(adminId: String): Flow<List<CheckinPing>> = callbackFlow {
        AppLog.d("PATH", "observe $COL_CHECKINS where adminId==$adminId")
        val reg = firestore.collection(COL_CHECKINS)
            .whereEqualTo("adminId", adminId)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(500)
            .addSnapshotListener { snap, err ->
                if (err != null) { close(err); return@addSnapshotListener }
                val list = snap?.documents.orEmpty().mapNotNull { d ->
                    val lat = d.getDouble("latitude")  ?: return@mapNotNull null
                    val lng = d.getDouble("longitude") ?: return@mapNotNull null
                    CheckinPing(
                        id          = d.id,
                        userId      = d.getString("userId") ?: "",
                        latitude    = lat,
                        longitude   = lng,
                        timestampMs = d.getMillis("timestamp") ?: 0L,
                    )
                }
                trySend(list)
            }
        awaitClose { reg.remove() }
    }

    override suspend fun recordCheckIn(
        adminId: String,
        userId: String,
        latitude: Double,
        longitude: Double,
    ): String {
        val ref = firestore.collection(COL_CHECKINS).document()
        AppLog.i("PATH", "write $COL_CHECKINS/${ref.id}")
        ref.set(mapOf(
            "adminId"   to adminId,
            "userId"    to userId,
            "latitude"  to latitude,
            "longitude" to longitude,
            "timestamp" to FieldValue.serverTimestamp(),
        )).awaitBounded()
        return ref.id
    }

    // ─── Notifications ────────────────────────────────────────────────────────

    override fun observeNotifications(
        adminId: String,
        userId: String,
    ): Flow<List<NotificationRecord>> = callbackFlow {
        AppLog.d("PATH", "observe $COL_NOTIFICATIONS adminId==$adminId userId==$userId")
        val reg = firestore.collection(COL_NOTIFICATIONS)
            .whereEqualTo("adminId", adminId)
            .whereEqualTo("userId", userId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(50)
            .addSnapshotListener { snap, err ->
                if (err != null) { close(err); return@addSnapshotListener }
                val list = snap?.documents.orEmpty().map { d ->
                    NotificationRecord(
                        id        = d.id,
                        adminId   = d.getString("adminId") ?: "",
                        userId    = d.getString("userId") ?: "",
                        title     = d.getString("title") ?: "",
                        body      = d.getString("body") ?: "",
                        type      = d.getString("type") ?: "",
                        isRead    = d.getBoolean("isRead") ?: false,
                        relatedId = d.getString("relatedId"),
                        createdAt = d.getMillis("createdAt"),
                    )
                }
                trySend(list)
            }
        awaitClose { reg.remove() }
    }

    override suspend fun markNotificationRead(notificationId: String) {
        AppLog.i("PATH", "update $COL_NOTIFICATIONS/$notificationId isRead=true")
        firestore.collection(COL_NOTIFICATIONS).document(notificationId)
            .update("isRead", true)
            .awaitBounded()
    }

    override suspend fun addNotification(
        adminId: String,
        userId: String,
        title: String,
        body: String,
        type: String,
        relatedId: String?,
    ): NotificationRecord {
        val ref = firestore.collection(COL_NOTIFICATIONS).document()
        AppLog.i("PATH", "write $COL_NOTIFICATIONS/${ref.id}")
        ref.set(mapOf(
            "adminId"   to adminId,
            "userId"    to userId,
            "title"     to title,
            "body"      to body,
            "type"      to type,
            "isRead"    to false,
            "relatedId" to relatedId,
            "createdAt" to FieldValue.serverTimestamp(),
        )).awaitBounded()
        return NotificationRecord(ref.id, adminId, userId, title, body, type, false, relatedId)
    }

    // ─── Date helpers ─────────────────────────────────────────────────────────

    private fun todayMidnightUtc(): Timestamp {
        val cal = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC"))
        cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
        cal.set(java.util.Calendar.MINUTE, 0)
        cal.set(java.util.Calendar.SECOND, 0)
        cal.set(java.util.Calendar.MILLISECOND, 0)
        return Timestamp(java.util.Date(cal.timeInMillis))
    }

    // ─── Normalizers ──────────────────────────────────────────────────────────

    private fun normalizeEmployeeStatus(raw: String): String = when (raw.lowercase()) {
        "active"              -> "Active"
        "inactive"            -> "Inactive"
        "on_leave", "onleave" -> "OnLeave"
        else                  -> raw.replaceFirstChar { it.uppercase() }
    }

    private fun normalizePriority(raw: String): String = when (raw.lowercase()) {
        "low"    -> "Low"
        "medium" -> "Medium"
        "high"   -> "High"
        else     -> raw.replaceFirstChar { it.uppercase() }
    }

    private fun normalizeTaskStatus(raw: String): String = when (raw.lowercase()) {
        "pending",     "todo"                  -> "Todo"
        "in_progress", "inprogress"            -> "InProgress"
        "in_review",   "inreview",   "review"  -> "InReview"
        "completed",   "done"                  -> "Done"
        else                                   -> raw
    }

    // ─── Spares ────────────────────────────────────────────────────────────────

    override fun observeSpareItems(adminId: String): Flow<List<SpareItemRecord>> = callbackFlow {
        AppLog.d("PATH", "observe $COL_SPARE_ITEMS where adminId == $adminId")
        val reg = firestore.collection(COL_SPARE_ITEMS)
            .whereEqualTo("adminId", adminId)
            .addSnapshotListener { snap, err ->
                if (err != null) { close(err); return@addSnapshotListener }
                val list = snap?.documents.orEmpty().map { d ->
                    SpareItemRecord(
                        id             = d.id,
                        adminId        = adminId,
                        category       = d.getString("category") ?: "",
                        name           = d.getString("name") ?: "—",
                        make           = d.getString("make") ?: "",
                        size           = d.getString("size") ?: "",
                        core           = d.getString("core") ?: "",
                        currentRating  = d.getString("currentRating") ?: "",
                        noOfPoles      = d.getString("noOfPoles") ?: "",
                        unit           = d.getString("unit") ?: "",
                        price          = d.getDouble("price") ?: 0.0,
                        stockQty       = (d.getLong("stockQty") ?: 0L).toInt(),
                        hsn            = d.getString("hsn") ?: "",
                        vendorName1    = d.getString("vendorName1") ?: "",
                        vendorGst1     = d.getString("vendorGst1") ?: "",
                        vendorContact1 = d.getString("vendorContact1") ?: "",
                        vendorAddress1 = d.getString("vendorAddress1") ?: "",
                        vendorName2    = d.getString("vendorName2") ?: "",
                        vendorGst2     = d.getString("vendorGst2") ?: "",
                        vendorContact2 = d.getString("vendorContact2") ?: "",
                        vendorAddress2 = d.getString("vendorAddress2") ?: "",
                        vendorLocation = d.getString("vendorLocation") ?: "",
                        departmentId   = d.getString("departmentId") ?: "",
                        departmentName = d.getString("departmentName") ?: "",
                        equipmentId    = d.getString("equipmentId") ?: "",
                        equipmentName  = d.getString("equipmentName") ?: "",
                        createdAt      = d.getMillis("createdAt"),
                        updatedAt      = d.getMillis("updatedAt"),
                    )
                }
                trySend(list)
            }
        awaitClose { reg.remove() }
    }

    override suspend fun addSpareItem(adminId: String, item: SpareItemRecord): SpareItemRecord {
        val ref = firestore.collection(COL_SPARE_ITEMS).document()
        AppLog.i("PATH", "write $COL_SPARE_ITEMS/${ref.id}")
        val data = mapOf(
            "adminId"        to adminId,
            "category"       to item.category,
            "name"           to item.name,
            "make"           to item.make,
            "size"           to item.size,
            "core"           to item.core,
            "currentRating"  to item.currentRating,
            "noOfPoles"      to item.noOfPoles,
            "unit"           to item.unit,
            "price"          to item.price,
            "stockQty"       to item.stockQty,
            "hsn"            to item.hsn,
            "vendorName1"    to item.vendorName1,
            "vendorGst1"     to item.vendorGst1,
            "vendorContact1" to item.vendorContact1,
            "vendorAddress1" to item.vendorAddress1,
            "vendorName2"    to item.vendorName2,
            "vendorGst2"     to item.vendorGst2,
            "vendorContact2" to item.vendorContact2,
            "vendorAddress2" to item.vendorAddress2,
            "vendorLocation" to item.vendorLocation,
            "departmentId"   to item.departmentId,
            "departmentName" to item.departmentName,
            "equipmentId"    to item.equipmentId,
            "equipmentName"  to item.equipmentName,
            "createdAt"      to FieldValue.serverTimestamp(),
            "updatedAt"      to FieldValue.serverTimestamp(),
        )
        ref.set(data).awaitBounded()
        return item.copy(id = ref.id, createdAt = System.currentTimeMillis(), updatedAt = System.currentTimeMillis())
    }

    override suspend fun bulkInsertSpareItems(adminId: String, items: List<SpareItemRecord>): Int {
        if (items.isEmpty()) return 0
        var written = 0
        items.chunked(500).forEach { chunk ->
            val batch = firestore.batch()
            chunk.forEach { item ->
                val ref = firestore.collection(COL_SPARE_ITEMS).document()
                batch.set(
                    ref,
                    mapOf(
                        "adminId"        to adminId,
                        "category"       to item.category,
                        "name"           to item.name,
                        "make"           to item.make,
                        "size"           to item.size,
                        "core"           to item.core,
                        "currentRating"  to item.currentRating,
                        "noOfPoles"      to item.noOfPoles,
                        "unit"           to item.unit,
                        "price"          to item.price,
                        "stockQty"       to item.stockQty,
                        "hsn"            to item.hsn,
                        "vendorName1"    to item.vendorName1,
                        "vendorGst1"     to item.vendorGst1,
                        "vendorContact1" to item.vendorContact1,
                        "vendorAddress1" to item.vendorAddress1,
                        "vendorName2"    to item.vendorName2,
                        "vendorGst2"     to item.vendorGst2,
                        "vendorContact2" to item.vendorContact2,
                        "vendorAddress2" to item.vendorAddress2,
                        "vendorLocation" to item.vendorLocation,
                        "departmentId"   to item.departmentId,
                        "departmentName" to item.departmentName,
                        "equipmentId"    to item.equipmentId,
                        "equipmentName"  to item.equipmentName,
                        "createdAt"      to FieldValue.serverTimestamp(),
                        "updatedAt"      to FieldValue.serverTimestamp(),
                    ),
                )
            }
            batch.commit().awaitBounded()
            written += chunk.size
            AppLog.i("PATH", "bulk write $COL_SPARE_ITEMS x${chunk.size}")
        }
        return written
    }

    override suspend fun updateSpareItem(adminId: String, itemId: String, updates: Map<String, Any?>): SpareItemRecord {
        AppLog.i("PATH", "update $COL_SPARE_ITEMS/$itemId")
        val data = updates.toMutableMap()
        data["updatedAt"] = FieldValue.serverTimestamp()
        firestore.collection(COL_SPARE_ITEMS).document(itemId)
            .update(data)
            .awaitBounded()
        val doc = firestore.collection(COL_SPARE_ITEMS).document(itemId).get().awaitBounded()
        return SpareItemRecord(
            id             = doc.id,
            adminId        = adminId,
            category       = doc.getString("category") ?: "",
            name           = doc.getString("name") ?: "—",
            make           = doc.getString("make") ?: "",
            size           = doc.getString("size") ?: "",
            core           = doc.getString("core") ?: "",
            currentRating  = doc.getString("currentRating") ?: "",
            noOfPoles      = doc.getString("noOfPoles") ?: "",
            unit           = doc.getString("unit") ?: "",
            price          = doc.getDouble("price") ?: 0.0,
            stockQty       = (doc.getLong("stockQty") ?: 0L).toInt(),
            hsn            = doc.getString("hsn") ?: "",
            vendorName1    = doc.getString("vendorName1") ?: "",
            vendorGst1     = doc.getString("vendorGst1") ?: "",
            vendorContact1 = doc.getString("vendorContact1") ?: "",
            vendorAddress1 = doc.getString("vendorAddress1") ?: "",
            vendorName2    = doc.getString("vendorName2") ?: "",
            vendorGst2     = doc.getString("vendorGst2") ?: "",
            vendorContact2 = doc.getString("vendorContact2") ?: "",
            vendorAddress2 = doc.getString("vendorAddress2") ?: "",
            vendorLocation = doc.getString("vendorLocation") ?: "",
            departmentId   = doc.getString("departmentId") ?: "",
            departmentName = doc.getString("departmentName") ?: "",
            equipmentId    = doc.getString("equipmentId") ?: "",
            equipmentName  = doc.getString("equipmentName") ?: "",
            updatedAt      = doc.getMillis("updatedAt"),
        )
    }

    override suspend fun deleteSpareItem(adminId: String, itemId: String) {
        AppLog.i("PATH", "delete $COL_SPARE_ITEMS/$itemId")
        firestore.collection(COL_SPARE_ITEMS).document(itemId).delete().awaitBounded()
    }

    override suspend fun bulkDeleteSpareItems(adminId: String, itemIds: List<String>): Int {
        if (itemIds.isEmpty()) return 0
        var deleted = 0
        itemIds.filter { it.isNotBlank() }.chunked(500).forEach { chunk ->
            val batch = firestore.batch()
            chunk.forEach { id ->
                batch.delete(firestore.collection(COL_SPARE_ITEMS).document(id))
            }
            batch.commit().awaitBounded()
            deleted += chunk.size
        }
        AppLog.i("PATH", "bulk delete $COL_SPARE_ITEMS x$deleted")
        return deleted
    }

    // ─── Departments ──────────────────────────────────────────────────────────

    override fun observeDepartments(adminId: String): Flow<List<DepartmentRecord>> = callbackFlow {
        AppLog.d("PATH", "observe $COL_DEPARTMENTS where adminId == $adminId")
        val reg = firestore.collection(COL_DEPARTMENTS)
            .whereEqualTo("adminId", adminId)
            .addSnapshotListener { snap, err ->
                if (err != null) { close(err); return@addSnapshotListener }
                val list = snap?.documents.orEmpty().map { d ->
                    DepartmentRecord(
                        id        = d.id,
                        adminId   = adminId,
                        name      = d.getString("name") ?: "—",
                        createdAt = d.getMillis("createdAt"),
                        updatedAt = d.getMillis("updatedAt"),
                    )
                }.sortedBy { it.name.lowercase() }
                trySend(list)
            }
        awaitClose { reg.remove() }
    }

    override suspend fun addDepartment(adminId: String, name: String): DepartmentRecord {
        val ref = firestore.collection(COL_DEPARTMENTS).document()
        AppLog.i("PATH", "write $COL_DEPARTMENTS/${ref.id}")
        ref.set(
            mapOf(
                "adminId"   to adminId,
                "name"      to name,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp(),
            )
        ).awaitBounded()
        val now = System.currentTimeMillis()
        return DepartmentRecord(ref.id, adminId, name, now, now)
    }

    override suspend fun updateDepartment(adminId: String, departmentId: String, name: String): DepartmentRecord {
        AppLog.i("PATH", "update $COL_DEPARTMENTS/$departmentId")
        firestore.collection(COL_DEPARTMENTS).document(departmentId)
            .update(mapOf("name" to name, "updatedAt" to FieldValue.serverTimestamp()))
            .awaitBounded()
        return DepartmentRecord(departmentId, adminId, name, updatedAt = System.currentTimeMillis())
    }

    override suspend fun deleteDepartment(adminId: String, departmentId: String) {
        AppLog.i("PATH", "delete $COL_DEPARTMENTS/$departmentId")
        firestore.collection(COL_DEPARTMENTS).document(departmentId).delete().awaitBounded()
    }

    // ─── Equipment ────────────────────────────────────────────────────────────

    override fun observeEquipment(adminId: String): Flow<List<EquipmentRecord>> = callbackFlow {
        AppLog.d("PATH", "observe $COL_EQUIPMENT where adminId == $adminId")
        val reg = firestore.collection(COL_EQUIPMENT)
            .whereEqualTo("adminId", adminId)
            .addSnapshotListener { snap, err ->
                if (err != null) { close(err); return@addSnapshotListener }
                val list = snap?.documents.orEmpty().map { d ->
                    EquipmentRecord(
                        id           = d.id,
                        adminId      = adminId,
                        name         = d.getString("name") ?: "—",
                        departmentId = d.getString("departmentId") ?: "",
                        createdAt    = d.getMillis("createdAt"),
                        updatedAt    = d.getMillis("updatedAt"),
                    )
                }.sortedBy { it.name.lowercase() }
                trySend(list)
            }
        awaitClose { reg.remove() }
    }

    override suspend fun addEquipment(adminId: String, name: String, departmentId: String): EquipmentRecord {
        val ref = firestore.collection(COL_EQUIPMENT).document()
        AppLog.i("PATH", "write $COL_EQUIPMENT/${ref.id}")
        ref.set(
            mapOf(
                "adminId"      to adminId,
                "name"         to name,
                "departmentId" to departmentId,
                "createdAt"    to FieldValue.serverTimestamp(),
                "updatedAt"    to FieldValue.serverTimestamp(),
            )
        ).awaitBounded()
        val now = System.currentTimeMillis()
        return EquipmentRecord(ref.id, adminId, name, departmentId, now, now)
    }

    override suspend fun updateEquipment(
        adminId: String,
        equipmentId: String,
        name: String,
        departmentId: String,
    ): EquipmentRecord {
        AppLog.i("PATH", "update $COL_EQUIPMENT/$equipmentId")
        firestore.collection(COL_EQUIPMENT).document(equipmentId)
            .update(
                mapOf(
                    "name"         to name,
                    "departmentId" to departmentId,
                    "updatedAt"    to FieldValue.serverTimestamp(),
                )
            )
            .awaitBounded()
        return EquipmentRecord(equipmentId, adminId, name, departmentId, updatedAt = System.currentTimeMillis())
    }

    override suspend fun deleteEquipment(adminId: String, equipmentId: String) {
        AppLog.i("PATH", "delete $COL_EQUIPMENT/$equipmentId")
        firestore.collection(COL_EQUIPMENT).document(equipmentId).delete().awaitBounded()
    }

    // ─── Leave requests ──────────────────────────────────────────────────────

    override fun observeLeaveRequestsForAdmin(adminId: String): Flow<List<LeaveRecord>> =
        callbackFlow {
            val reg = firestore.collection(COL_LEAVE_REQUESTS)
                .whereEqualTo("adminId", adminId)
                .addSnapshotListener { snap, err ->
                    if (err != null) { close(err); return@addSnapshotListener }
                    trySend(snap?.documents.orEmpty().mapNotNull { d ->
                        d.toLeaveRecord()
                    }.sortedByDescending { it.createdAtMs ?: 0L })
                }
            awaitClose { reg.remove() }
        }

    override fun observeLeaveRequestsForUser(userId: String): Flow<List<LeaveRecord>> =
        callbackFlow {
            val reg = firestore.collection(COL_LEAVE_REQUESTS)
                .whereEqualTo("userId", userId)
                .addSnapshotListener { snap, err ->
                    if (err != null) { close(err); return@addSnapshotListener }
                    trySend(snap?.documents.orEmpty().mapNotNull { d ->
                        d.toLeaveRecord()
                    }.sortedByDescending { it.createdAtMs ?: 0L })
                }
            awaitClose { reg.remove() }
        }

    override suspend fun submitLeaveRequest(
        adminId: String,
        userId: String,
        employeeName: String,
        department: String,
        leaveType: String,
        fromDateMs: Long,
        toDateMs: Long,
        totalDays: Int,
        reason: String,
    ): LeaveRecord {
        val ref = firestore.collection(COL_LEAVE_REQUESTS).document()
        AppLog.i("PATH", "write $COL_LEAVE_REQUESTS/${ref.id}")
        bounded {
            ref.set(
                mapOf(
                    "adminId"       to adminId,
                    "userId"        to userId,
                    "employeeName"  to employeeName,
                    "department"    to department,
                    "leaveType"     to leaveType,
                    "fromDateMs"    to fromDateMs,
                    "toDateMs"      to toDateMs,
                    "totalDays"     to totalDays,
                    "reason"        to reason,
                    "status"        to "pending",
                    "rejectionReason" to "",
                    "createdAt"     to FieldValue.serverTimestamp(),
                    "updatedAt"     to FieldValue.serverTimestamp(),
                )
            ).await()
        }
        val nowMs = System.currentTimeMillis()
        return LeaveRecord(
            id = ref.id, userId = userId, adminId = adminId,
            employeeName = employeeName, department = department,
            leaveType = leaveType, fromDateMs = fromDateMs, toDateMs = toDateMs,
            totalDays = totalDays, reason = reason, status = "pending",
            createdAtMs = nowMs, updatedAtMs = nowMs,
        )
    }

    override suspend fun updateLeaveStatus(
        leaveId: String,
        adminId: String,
        userId: String,
        newStatus: String,
        rejectionReason: String,
    ) {
        AppLog.i("PATH", "updateLeaveStatus $COL_LEAVE_REQUESTS/$leaveId → $newStatus")
        val leaveRef = firestore.collection(COL_LEAVE_REQUESTS).document(leaveId)
        val userRef  = firestore.collection(COL_USERS).document(userId)
        bounded {
            firestore.runTransaction { tx ->
                val updates = mutableMapOf<String, Any>(
                    "status"    to newStatus,
                    "updatedAt" to FieldValue.serverTimestamp(),
                )
                if (rejectionReason.isNotBlank()) updates["rejectionReason"] = rejectionReason
                tx.update(leaveRef, updates)
                if (newStatus == "approved") {
                    tx.update(userRef, mapOf("status" to "OnLeave"))
                }
            }.await()
        }
    }

    private fun com.google.firebase.firestore.DocumentSnapshot.toLeaveRecord(): LeaveRecord? {
        return try {
            LeaveRecord(
                id               = id,
                userId           = getString("userId") ?: return null,
                adminId          = getString("adminId") ?: return null,
                employeeName     = getString("employeeName") ?: "—",
                department       = getString("department") ?: "",
                leaveType        = getString("leaveType") ?: "Casual",
                fromDateMs       = getLong("fromDateMs") ?: 0L,
                toDateMs         = getLong("toDateMs") ?: 0L,
                totalDays        = (getLong("totalDays") ?: 1L).toInt(),
                reason           = getString("reason") ?: "",
                status           = getString("status") ?: "pending",
                rejectionReason  = getString("rejectionReason") ?: "",
                createdAtMs      = getMillis("createdAt"),
                updatedAtMs      = getMillis("updatedAt"),
            )
        } catch (e: Exception) {
            AppLog.w("PATH", "toLeaveRecord failed for $id: ${e.message}")
            null
        }
    }

    // ─── Danger zone ─────────────────────────────────────────────────────────

    override suspend fun deleteAllData(adminId: String) {
        val tenantCollections = listOf(
            COL_USERS,
            COL_TASKS,
            COL_ATTENDANCE_LOGS,
            COL_INVENTORY_ITEMS,
            COL_INVENTORY_TRANSACTIONS,
            COL_CHECKINS,
            COL_NOTIFICATIONS,
            COL_SPARE_ITEMS,
            COL_DEPARTMENTS,
            COL_EQUIPMENT,
            COL_LEAVE_REQUESTS,
        )
        for (col in tenantCollections) {
            deleteTenantCollection(col, adminId)
        }
        AppLog.i("PATH", "deleteAllData complete for adminId=$adminId")
    }

    private suspend fun deleteTenantCollection(collection: String, adminId: String) {
        var query = firestore.collection(collection)
            .whereEqualTo("adminId", adminId)
            .limit(400)

        while (true) {
            val snap = bounded { query.get().await() }
            if (snap.isEmpty) break

            val batch = firestore.batch()
            snap.documents.forEach { batch.delete(it.reference) }
            bounded { batch.commit().await() }

            if (snap.documents.size < 400) break
        }
    }

    private companion object {
        const val COL_ADMINS                 = "admins"
        const val COL_EMPLOYEE_MAP           = "employee_map"
        const val COL_USERS                  = "users"
        const val COL_TASKS                  = "tasks"
        const val COL_ATTENDANCE_LOGS        = "attendance_logs"
        const val COL_INVENTORY_ITEMS        = "inventory_items"
        const val COL_INVENTORY_TRANSACTIONS = "inventory_transactions"
        const val COL_CHECKINS               = "checkins"
        const val COL_NOTIFICATIONS          = "notifications"
        const val COL_SPARE_ITEMS            = "spare_items"
        const val COL_DEPARTMENTS            = "departments"
        const val COL_EQUIPMENT              = "equipment"
        const val COL_LEAVE_REQUESTS         = "leave_requests"
    }
}
