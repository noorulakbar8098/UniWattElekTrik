package com.example.uniwattelektrik.feature.workforce.data.remote

import com.example.uniwattelektrik.core.AppLog
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import android.net.Uri
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout

/**
 * 15 s cap on a single Firestore round-trip — without this the SDK retries
 * forever on NOT_FOUND / PERMISSION_DENIED / no-network and the UI hangs.
 */
private const val FIRESTORE_TIMEOUT_MS = 15_000L

private suspend fun <T> bounded(block: suspend () -> T): T = try {
    withTimeout(FIRESTORE_TIMEOUT_MS) { block() }
} catch (e: TimeoutCancellationException) {
    throw IllegalStateException(
        "Firestore did not respond in ${FIRESTORE_TIMEOUT_MS / 1000}s. " +
            "Provision the database in the Firebase Console and verify rules.",
        e,
    )
}

/** Replaces `.await()` at every Firestore call site — bounds each call. */
private suspend fun <T> com.google.android.gms.tasks.Task<T>.awaitBounded(): T =
    bounded { this.await() }

/**
 * Firestore-backed workforce directory using **per-admin subcollections**.
 *
 * Layout:
 *   /admins/{adminId}/users/{userId}
 *   /admins/{adminId}/tasks/{taskId}
 *   /admins/{adminId}/inventory/{itemId}
 *   /admins/{adminId}/attendance/{attendanceId}
 *   /admins/{adminId}/checkins/{checkinId}
 *
 * No more `adminId` field on each doc — the parent path *is* the admin scope.
 *
 * Security rules (one block covers every subcollection):
 *
 *   match /admins/{adminId} {
 *     allow read, write: if request.auth.uid == adminId;
 *     match /{document=**} {
 *       allow read, write: if request.auth.uid == adminId;
 *     }
 *   }
 */
class FirestoreWorkforceDirectory(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val storage:   FirebaseStorage   = FirebaseStorage.getInstance(),
) : WorkforceDirectory {

    /** All admin-scoped paths flow through here. */
    private fun adminRoot(adminId: String) =
        firestore.collection(COL_ADMINS).document(adminId)

    // ─── Employees ──────────────────────────────────────────────────────────

    override fun observeEmployees(adminId: String): Flow<List<EmployeeRecord>> = callbackFlow {
        AppLog.d("PATH", "observe admins/$adminId/$SUB_USERS")
        val reg = adminRoot(adminId).collection(SUB_USERS)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snap, err ->
                if (err != null) { close(err); return@addSnapshotListener }
                val list = snap?.documents.orEmpty().map { d ->
                    val rawStatus = d.getString("status") ?: "active"
                    EmployeeRecord(
                        id                = d.id,
                        name              = d.getString("name") ?: "—",
                        role              = d.getString("role_title") ?: "Engineer",
                        phone             = d.getString("phone") ?: "",
                        zone              = d.getString("zone") ?: "—",
                        status            = normalizeEmployeeStatus(rawStatus),
                        tasksOpen         = (d.getLong("tasksOpen") ?: 0L).toInt(),
                        email             = d.getString("email") ?: "",
                        gender            = d.getString("gender") ?: "",
                        employmentType    = d.getString("employmentType") ?: "",
                        joiningDateMs     = d.getTimestamp("joiningDate")?.toDate()?.time,
                        salary            = d.getDouble("salary") ?: 0.0,
                        photoUrl          = d.getString("photoUrl") ?: "",
                        dateOfBirthMs     = d.getTimestamp("dateOfBirth")?.toDate()?.time,
                        address           = d.getString("address") ?: "",
                        department        = d.getString("department") ?: "",
                        reportingTo       = d.getString("reportingTo") ?: "",
                        permission        = d.getString("permission") ?: "",
                        emergencyName     = d.getString("emergencyName") ?: "",
                        emergencyRelation = d.getString("emergencyRelation") ?: "",
                        emergencyPhone    = d.getString("emergencyPhone") ?: "",
                        shift             = d.getString("shift") ?: "Shift1",
                    )
                }
                trySend(list)
            }
        awaitClose { reg.remove() }
    }

    override suspend fun addEmployee(
        adminId: String,
        uid: String,
        draft: EmployeeDraft,
    ): EmployeeRecord {
        // Use the Firebase Auth UID as the Firestore doc ID so the employee
        // can resolve their own profile via a collectionGroup("users") query
        // on sign-in.
        val ref = adminRoot(adminId).collection(SUB_USERS).document(uid)
        AppLog.i("PATH", "write admins/$adminId/$SUB_USERS/${ref.id}")

        // 1. Upload photo to Firebase Storage first (if any) so we can write
        //    the resulting public download URL into the Firestore doc.
        val uploadedPhotoUrl: String? = draft.photoUri?.takeIf { it.isNotBlank() }?.let { uri ->
            runCatching { uploadEmployeePhoto(adminId, ref.id, uri) }
                .onFailure { AppLog.w("PATH", "photo upload failed: ${it.message}") }
                .getOrNull()
        }
        // 2. Build the canonical doc.
        val data = mutableMapOf<String, Any?>(
            "uid"               to uid,
            "adminId"           to adminId,                 // back-reference (denormalised)
            "createdBy"         to adminId,
            "mustChangePassword" to true,                  // first-login forced rotation
            "name"              to draft.name,
            "phone"             to draft.phone,
            "email"             to draft.email,
            "gender"            to draft.gender,
            "employmentType"    to draft.employmentType,
            "salary"            to draft.salary,
            "role"              to "user",
            "status"            to draft.status.lowercase(),
            "createdAt"         to FieldValue.serverTimestamp(),
            // app-specific extras:
            "role_title"        to draft.role,
            "zone"              to draft.zone,
            "tasksOpen"         to 0L,
            // Extended HR fields (Figma "Add employee"):
            "address"           to draft.address,
            "department"        to draft.department,
            "reportingTo"       to draft.reportingTo,
            "permission"        to draft.permission,
            "emergencyName"     to draft.emergencyName,
            "emergencyRelation" to draft.emergencyRelation,
            "emergencyPhone"    to draft.emergencyPhone,
            "shift"             to draft.shift,
        )
        draft.joiningDateMs?.let { data["joiningDate"] = Timestamp(java.util.Date(it)) }
        draft.dateOfBirthMs?.let { data["dateOfBirth"] = Timestamp(java.util.Date(it)) }
        uploadedPhotoUrl?.let { data["photoUrl"] = it }

        ref.set(data).awaitBounded()
        AppLog.i("PATH", "  ↳ profile saved with ${data.size} fields")

        // Write a top-level lookup doc so the employee can resolve their adminId
        // on login without a fragile collection-group query. This is best-effort;
        // a failure here (e.g. transient rules issue) must NOT roll back the
        // employee profile that was just persisted above.
        runCatching {
            firestore.collection(COL_EMPLOYEE_MAP).document(uid)
                .set(mapOf("adminId" to adminId, "mustChangePassword" to true))
                .awaitBounded()
        }.onFailure {
            AppLog.w("PATH", "employee_map write failed (non-fatal): ${it.message}")
        }

        return EmployeeRecord(
            id                = ref.id,
            name              = draft.name,
            role              = draft.role,
            phone             = draft.phone,
            zone              = draft.zone,
            status            = normalizeEmployeeStatus(draft.status),
            tasksOpen         = 0,
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
        )
    }

    override suspend fun markPasswordChanged(adminId: String, uid: String) {
        AppLog.i("PATH", "update admins/$adminId/$SUB_USERS/$uid mustChangePassword=false")
        adminRoot(adminId).collection(SUB_USERS).document(uid)
            .update(
                mapOf(
                    "mustChangePassword" to false,
                    "passwordChangedAt"  to FieldValue.serverTimestamp(),
                ),
            )
            .awaitBounded()
    }

    /**
     * Uploads the picked image to `employee-photos/{adminId}/{userId}.jpg` in
     * Firebase Storage and returns its public download URL.
     */
    private suspend fun uploadEmployeePhoto(
        adminId: String,
        userId: String,
        contentUri: String,
    ): String {
        val path = "employee-photos/$adminId/$userId.jpg"
        AppLog.i("PATH", "upload $path  ← $contentUri")
        val ref = storage.reference.child(path)
        ref.putFile(Uri.parse(contentUri)).awaitBounded()
        return ref.downloadUrl.awaitBounded().toString()
    }

    // ─── Tasks ──────────────────────────────────────────────────────────────

    override fun observeTasksForAdmin(adminId: String): Flow<List<TaskRecord>> = callbackFlow {
        AppLog.d("PATH", "observe admins/$adminId/$SUB_TASKS")
        val reg = adminRoot(adminId).collection(SUB_TASKS)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snap, err ->
                if (err != null) { close(err); return@addSnapshotListener }
                val list = snap?.documents.orEmpty().map { toTask(it, adminId) }
                trySend(list)
            }
        awaitClose { reg.remove() }
    }

    override fun observeTasksForUser(userId: String): Flow<List<TaskRecord>> = callbackFlow {
        AppLog.d("PATH", "observe collectionGroup($SUB_TASKS) where assignedTo == $userId")
        // Cross-admin search via collectionGroup — finds every "tasks"
        // subcollection in the project where assignedTo == userId.
        // NOTE: requires single-field collectionGroup index on `assignedTo`
        // (Firestore prompts a creation link in logcat on first run).
        val reg = firestore.collectionGroup(SUB_TASKS)
            .whereEqualTo("assignedTo", userId)
            .addSnapshotListener { snap, err ->
                if (err != null) { close(err); return@addSnapshotListener }
                val list = snap?.documents.orEmpty().map { d ->
                    val owningAdmin = d.reference.parent.parent?.id ?: ""
                    toTask(d, owningAdmin)
                }.sortedByDescending { it.id }
                trySend(list)
            }
        awaitClose { reg.remove() }
    }

    override suspend fun addTask(
        adminId: String,
        userId: String?,
        title: String,
        location: String,
        time: String,
        day: String,
        priority: String,
    ): TaskRecord {
        val ref = adminRoot(adminId).collection(SUB_TASKS).document()
        AppLog.i("PATH", "write admins/$adminId/$SUB_TASKS/${ref.id}")
        // Canonical schema:
        //   { title, description, priority, status, assignedTo, createdAt }
        // App extras kept for the dashboards: location, time, day.
        val data = mapOf(
            "title"       to title,
            "description" to "",
            "priority"    to priority.lowercase(),
            "status"      to "pending",
            "assignedTo"  to userId,
            "createdAt"   to FieldValue.serverTimestamp(),
            // app-specific extras:
            "location"    to location,
            "time"        to time,
            "day"         to day,
        )
        ref.set(data).awaitBounded()
        return TaskRecord(
            id = ref.id, adminId = adminId, userId = userId,
            title = title, location = location, time = time, day = day,
            priority = priority, status = "Todo",
        )
    }

    // ─── Helpers ────────────────────────────────────────────────────────────

    private fun toTask(
        d: com.google.firebase.firestore.DocumentSnapshot,
        adminId: String,
    ): TaskRecord = TaskRecord(
        id        = d.id,
        adminId   = adminId, // derived from path, no longer a field
        userId    = d.getString("assignedTo"),
        title     = d.getString("title") ?: "—",
        location  = d.getString("location") ?: "—",
        time      = d.getString("time") ?: "",
        day       = d.getString("day") ?: "Today",
        priority  = normalizePriority(d.getString("priority") ?: "medium"),
        status    = normalizeTaskStatus(d.getString("status") ?: "pending"),
        assigneeInitials = (d.getString("assigneeInitials") ?: "").take(2),
    )

    // ─── Inventory ──────────────────────────────────────────────────────────

    override fun observeInventory(adminId: String): Flow<List<InventoryRecord>> = callbackFlow {
        AppLog.d("PATH", "observe admins/$adminId/$SUB_INVENTORY")
        val reg = adminRoot(adminId).collection(SUB_INVENTORY)
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
        val ref = adminRoot(adminId).collection(SUB_INVENTORY).document()
        AppLog.i("PATH", "write admins/$adminId/$SUB_INVENTORY/${ref.id}")
        ref.set(
            mapOf(
                "name"     to name,
                "type"     to type,
                "price"    to price,
                "quantity" to quantity,
                "createdAt" to FieldValue.serverTimestamp(),
            ),
        ).awaitBounded()
        return InventoryRecord(ref.id, name, type, price, quantity)
    }

    // ─── Attendance ─────────────────────────────────────────────────────────

    override fun observeAttendance(adminId: String): Flow<List<AttendanceRecord>> = callbackFlow {
        // Attendance now lives under each user. Use a collection-group query
        // filtered by adminId so the admin sees every employee's records.
        AppLog.d("PATH", "observe collectionGroup($SUB_ATTENDANCE) where adminId == $adminId")
        val reg = firestore.collectionGroup(SUB_ATTENDANCE)
            .whereEqualTo("adminId", adminId)
            .orderBy("date", Query.Direction.DESCENDING)
            .addSnapshotListener { snap, err ->
                if (err != null) { close(err); return@addSnapshotListener }
                val list = snap?.documents.orEmpty().map { d ->
                    AttendanceRecord(
                        id            = d.id,
                        userId        = d.getString("userId") ?: "",
                        dateMs        = d.getTimestamp("date")?.toDate()?.time ?: 0L,
                        checkInMs     = d.getTimestamp("checkIn")?.toDate()?.time ?: 0L,
                        checkInLat    = d.getDouble("checkInLat"),
                        checkInLng    = d.getDouble("checkInLng"),
                        checkInStatus = d.getString("checkInStatus") ?: "ON_TIME",
                        checkOutMs    = d.getTimestamp("checkOut")?.toDate()?.time,
                        checkOutLat   = d.getDouble("checkOutLat"),
                        checkOutLng   = d.getDouble("checkOutLng"),
                        status        = d.getString("attendanceStatus")
                            ?: if (d.getTimestamp("checkOut") != null) "COMPLETED" else "CHECKED_IN",
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
        val ref = adminRoot(adminId).collection(SUB_USERS).document(userId)
            .collection(SUB_ATTENDANCE).document()
        AppLog.i("PATH", "write admins/$adminId/$SUB_USERS/$userId/$SUB_ATTENDANCE/${ref.id}")
        val data = mutableMapOf<String, Any?>(
            "adminId"          to adminId,
            "userId"           to userId,
            "date"             to FieldValue.serverTimestamp(),
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
            checkOutMs    = null,
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
        AppLog.i("PATH", "update admins/$adminId/$SUB_USERS/$userId/$SUB_ATTENDANCE/$attendanceId checkOut")
        val updates = mutableMapOf<String, Any?>(
            "checkOut"         to FieldValue.serverTimestamp(),
            "attendanceStatus" to "COMPLETED",
        )
        lat?.let { updates["checkOutLat"] = it }
        lng?.let { updates["checkOutLng"] = it }
        adminRoot(adminId).collection(SUB_USERS).document(userId)
            .collection(SUB_ATTENDANCE).document(attendanceId)
            .update(updates)
            .awaitBounded()
    }

    // ─── Check-ins (location pings) ─────────────────────────────────────────

    override fun observeCheckins(adminId: String): Flow<List<CheckinPing>> = callbackFlow {
        AppLog.d("PATH", "observe admins/$adminId/$SUB_CHECKINS")
        val reg = adminRoot(adminId).collection(SUB_CHECKINS)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snap, err ->
                if (err != null) { close(err); return@addSnapshotListener }
                val list = snap?.documents.orEmpty().mapNotNull { d ->
                    val lat = d.getDouble("latitude") ?: return@mapNotNull null
                    val lng = d.getDouble("longitude") ?: return@mapNotNull null
                    CheckinPing(
                        id = d.id,
                        userId = d.getString("userId") ?: "",
                        latitude = lat,
                        longitude = lng,
                        timestampMs = d.getTimestamp("timestamp")?.toDate()?.time ?: 0L,
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
        val ref = adminRoot(adminId).collection(SUB_CHECKINS).document()
        AppLog.i("PATH", "write admins/$adminId/$SUB_CHECKINS/${ref.id}")
        ref.set(
            mapOf(
                "userId"    to userId,
                "latitude"  to latitude,
                "longitude" to longitude,
                "timestamp" to FieldValue.serverTimestamp(),
            ),
        ).awaitBounded()
        return ref.id
    }

    // ── Mappers between canonical schema values and the UI's display values ──

    private fun normalizeEmployeeStatus(raw: String): String = when (raw.lowercase()) {
        "active"    -> "Active"
        "inactive"  -> "Inactive"
        "on_leave",
        "onleave"   -> "OnLeave"
        else        -> raw.replaceFirstChar { it.uppercase() }
    }

    private fun normalizePriority(raw: String): String = when (raw.lowercase()) {
        "low"    -> "Low"
        "medium" -> "Medium"
        "high"   -> "High"
        else     -> raw.replaceFirstChar { it.uppercase() }
    }

    private fun normalizeTaskStatus(raw: String): String = when (raw.lowercase()) {
        "pending"      -> "Todo"
        "in_progress"  -> "InProgress"
        "completed"    -> "Done"
        // accept legacy values too
        "todo"         -> "Todo"
        "inprogress"   -> "InProgress"
        "done"         -> "Done"
        else           -> raw
    }

    private companion object {
        const val COL_ADMINS      = "admins"
        const val COL_EMPLOYEE_MAP = "employee_map"
        const val SUB_USERS       = "users"
        const val SUB_TASKS       = "tasks"
        const val SUB_INVENTORY   = "inventory"
        const val SUB_ATTENDANCE  = "attendance"
        const val SUB_CHECKINS    = "checkins"
    }
}



