package com.example.uniwattelektrik.feature.workforce.data.remote

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

private class StubWorkforceDirectory : WorkforceDirectory {
    override fun observeEmployees(adminId: String): Flow<List<EmployeeRecord>> =
        flowOf(emptyList())
    override suspend fun addEmployee(
        adminId: String, uid: String, draft: EmployeeDraft,
    ): EmployeeRecord = EmployeeRecord(
        id                = uid.ifBlank { "stub" },
        name              = draft.name,
        role              = draft.role,
        phone             = draft.phone,
        zone              = draft.zone,
        status            = "Active",
        tasksOpen         = 0,
        email             = draft.email,
        gender            = draft.gender,
        employmentType    = draft.employmentType,
        joiningDateMs     = draft.joiningDateMs,
        salary            = draft.salary,
        photoUrl          = "",
        dateOfBirthMs     = draft.dateOfBirthMs,
        address           = draft.address,
        department        = draft.department,
        reportingTo       = draft.reportingTo,
        permission        = draft.permission,
        emergencyName     = draft.emergencyName,
        emergencyRelation = draft.emergencyRelation,
        emergencyPhone    = draft.emergencyPhone,
    )

    override suspend fun markPasswordChanged(adminId: String, uid: String) { /* no-op */ }
    override suspend fun saveFcmToken(adminId: String, uid: String, token: String) { /* no-op */ }
    override suspend fun updateEmployee(
        adminId: String, employeeId: String, draft: EmployeeDraft,
    ): EmployeeRecord = EmployeeRecord(
        id                = employeeId,
        name              = draft.name,
        role              = draft.role,
        phone             = draft.phone,
        zone              = draft.zone,
        status            = "Active",
        tasksOpen         = 0,
        email             = draft.email,
        gender            = draft.gender,
        employmentType    = draft.employmentType,
        joiningDateMs     = draft.joiningDateMs,
        salary            = draft.salary,
        photoUrl          = "",
        dateOfBirthMs     = draft.dateOfBirthMs,
        address           = draft.address,
        department        = draft.department,
        reportingTo       = draft.reportingTo,
        permission        = draft.permission,
        emergencyName     = draft.emergencyName,
        emergencyRelation = draft.emergencyRelation,
        emergencyPhone    = draft.emergencyPhone,
    )

    override fun observeTasksForAdmin(adminId: String): Flow<List<TaskRecord>> = flowOf(emptyList())
    override fun observeTasksForUser(userId: String): Flow<List<TaskRecord>> = flowOf(emptyList())
    override suspend fun addTask(
        adminId: String, userId: String?, title: String, location: String,
        time: String, day: String, priority: String,
        description: String,
        departmentId: String, departmentName: String,
        equipmentId: String, equipmentName: String,
        checklist: List<ChecklistItem>, attachments: List<String>,
        address: String, latitude: Double?, longitude: Double?,
        dueDate: Long?, ownerAdminName: String, assigneeName: String,
        notifyAssignee: Boolean,
        assigneeIds: List<String>,
        assigneeNames: List<String>,
    ): TaskRecord = TaskRecord(
        id = "stub", adminId = adminId, userId = userId,
        title = title, description = description,
        location = location, time = time, day = day,
        priority = priority, status = "Todo",
        ownerAdminId = adminId, ownerAdminName = ownerAdminName,
        assigneeName = assigneeName,
        assignedUserIds = assigneeIds,
        assigneeNames = assigneeNames,
        dueDate = dueDate,
        departmentId = departmentId, departmentName = departmentName,
        equipmentId = equipmentId, equipmentName = equipmentName,
        checklist = checklist, attachments = attachments,
        address = address, latitude = latitude, longitude = longitude,
        notifyAssignee = notifyAssignee,
    )
    override suspend fun acceptTask(taskId: String, lat: Double?, lon: Double?): TaskRecord =
        TaskRecord(
            id = "stub", adminId = "", userId = null,
            title = "", location = "", time = "", day = "",
            priority = "", status = "InProgress",
        )
    override suspend fun updateTask(
        taskId: String, adminId: String, userId: String?, title: String,
        location: String, time: String, day: String, priority: String,
        description: String,
        departmentId: String, departmentName: String,
        equipmentId: String, equipmentName: String,
        checklist: List<ChecklistItem>, attachments: List<String>,
        address: String, latitude: Double?, longitude: Double?,
        dueDate: Long?, assigneeName: String,
        notifyAssignee: Boolean,
        assigneeIds: List<String>,
        assigneeNames: List<String>,
    ): TaskRecord = TaskRecord(
        id = taskId, adminId = adminId, userId = userId,
        title = title, description = description,
        location = location, time = time, day = day,
        priority = priority, status = "Todo",
        assigneeName = assigneeName,
        assignedUserIds = assigneeIds,
        assigneeNames = assigneeNames,
        dueDate = dueDate,
        departmentId = departmentId, departmentName = departmentName,
        equipmentId = equipmentId, equipmentName = equipmentName,
        checklist = checklist, attachments = attachments,
        address = address, latitude = latitude, longitude = longitude,
        notifyAssignee = notifyAssignee,
    )
    override suspend fun reassignTask(
        taskId: String, adminId: String,
        newAssigneeIds: List<String>, newAssigneeNames: List<String>,
    ): TaskRecord = TaskRecord(
        id = taskId, adminId = adminId,
        userId = newAssigneeIds.firstOrNull(),
        title = "", location = "", time = "", day = "",
        priority = "", status = "InReview",
        assigneeName = newAssigneeNames.firstOrNull().orEmpty(),
        assignedUserIds = newAssigneeIds,
        assigneeNames = newAssigneeNames,
    )
    override fun observeTaskNotes(taskId: String): Flow<List<TaskNote>> = flowOf(emptyList())
    override suspend fun completeTaskWithSignoff(
        adminId: String, taskId: String, assignedUserId: String?,
        signoffDescription: String, downtimeMinutes: Int, rca: String,
        materialsUsed: List<MaterialUsedItem>, startTimeMs: Long?, endTimeMs: Long,
    ): TaskRecord = TaskRecord(
        id = taskId, adminId = adminId, userId = assignedUserId,
        title = "", location = "", time = "", day = "",
        priority = "", status = "Done",
    )

    override suspend fun addTaskNote(
        taskId: String, authorId: String, authorName: String, role: String, message: String,
        voiceUrl: String?, voiceDurationMs: Long?,
    ): TaskNote = TaskNote(
        id = "stub", taskId = taskId, authorId = authorId, authorName = authorName,
        role = role, message = message, createdAtMs = 0L,
        voiceUrl = voiceUrl, voiceDurationMs = voiceDurationMs,
    )
    override suspend fun updateTaskStatus(
        adminId: String, taskId: String, assignedUserId: String?, newStatus: String,
    ): TaskRecord = TaskRecord(
        id = "stub", adminId = adminId, userId = assignedUserId,
        title = "", location = "", time = "", day = "",
        priority = "", status = newStatus,
    )
    override suspend fun setChecklistItemDone(taskId: String, index: Int, done: Boolean) { /* no-op */ }
    override suspend fun uploadTaskAttachment(adminId: String, contentUri: String): String = ""
    override suspend fun uploadVoiceNote(adminId: String, contentUri: String): String = ""
    override suspend fun updateTaskAttachments(taskId: String, urls: List<String>) { /* no-op */ }
    override suspend fun updateEmployeePhoto(adminId: String, employeeId: String, contentUri: String): String = ""

    override fun observeInventory(adminId: String): Flow<List<InventoryRecord>> = flowOf(emptyList())
    override suspend fun addInventoryItem(
        adminId: String, name: String, type: String, price: Double, quantity: Int,
    ): InventoryRecord = InventoryRecord("stub", name, type, price, quantity)
    override fun observeInventoryTransactions(adminId: String): Flow<List<InventoryTransaction>> = flowOf(emptyList())
    override fun observeMonthlyStockSnapshot(
        adminId: String, year: Int, month: Int,
    ): Flow<MonthlyStockSnapshot?> = flowOf(null)
    override suspend fun addInventoryTransaction(
        adminId: String, userId: String, itemId: String, itemName: String, type: String, quantity: Int,
    ): InventoryTransaction = InventoryTransaction("stub", adminId, userId, itemId, itemName, type, quantity)

    override fun observeAttendance(adminId: String): Flow<List<AttendanceRecord>> = flowOf(emptyList())
    override fun observeMyAttendance(userId: String): Flow<List<AttendanceRecord>> = flowOf(emptyList())
    override suspend fun markCheckIn(
        adminId: String,
        userId: String,
        lat: Double?,
        lng: Double?,
        checkInStatus: String,
    ): AttendanceRecord = AttendanceRecord(
        id = "stub", userId = userId, dateMs = 0L, checkInMs = 0L,
        checkInLat = lat, checkInLng = lng, checkInStatus = checkInStatus,
        checkOutMs = null, status = "CHECKED_IN",
    )
    override suspend fun markCheckOut(
        adminId: String,
        userId: String,
        attendanceId: String,
        lat: Double?,
        lng: Double?,
    ) { /* no-op */ }

    override suspend fun recordCheckIn(
        adminId: String, userId: String, latitude: Double, longitude: Double,
    ): String = "stub"
    override fun observeCheckins(adminId: String): Flow<List<CheckinPing>> = flowOf(emptyList())

    override fun observeNotifications(adminId: String, userId: String): Flow<List<NotificationRecord>> = flowOf(emptyList())
    override suspend fun markNotificationRead(notificationId: String) { /* no-op */ }
    override suspend fun addNotification(
        adminId: String, userId: String, title: String, body: String, type: String, relatedId: String?,
    ): NotificationRecord = NotificationRecord("stub", adminId, userId, title, body, type)

    override fun observeSpareItems(adminId: String): Flow<List<SpareItemRecord>> = flowOf(emptyList())
    override suspend fun addSpareItem(adminId: String, item: SpareItemRecord): SpareItemRecord = item
    override suspend fun updateSpareItem(adminId: String, itemId: String, updates: Map<String, Any?>): SpareItemRecord =
        SpareItemRecord("stub", adminId, "", "", price = 0.0, stockQty = 0, hsn = "")
    override suspend fun deleteSpareItem(adminId: String, itemId: String) { /* no-op */ }
    override suspend fun bulkDeleteSpareItems(adminId: String, itemIds: List<String>): Int = itemIds.size
    override suspend fun bulkInsertSpareItems(adminId: String, items: List<SpareItemRecord>): Int = items.size

    override fun observeDepartments(adminId: String): Flow<List<DepartmentRecord>> = flowOf(emptyList())
    override suspend fun addDepartment(adminId: String, name: String): DepartmentRecord =
        DepartmentRecord("stub", adminId, name)
    override suspend fun updateDepartment(adminId: String, departmentId: String, name: String): DepartmentRecord =
        DepartmentRecord(departmentId, adminId, name)
    override suspend fun deleteDepartment(adminId: String, departmentId: String) { /* no-op */ }

    override fun observeEquipment(adminId: String): Flow<List<EquipmentRecord>> = flowOf(emptyList())
    override suspend fun addEquipment(adminId: String, name: String, departmentId: String): EquipmentRecord =
        EquipmentRecord("stub", adminId, name, departmentId)
    override suspend fun updateEquipment(
        adminId: String, equipmentId: String, name: String, departmentId: String,
    ): EquipmentRecord = EquipmentRecord(equipmentId, adminId, name, departmentId)
    override suspend fun deleteEquipment(adminId: String, equipmentId: String) { /* no-op */ }

    override suspend fun deleteAllData(adminId: String) { /* no-op */ }
    override suspend fun updateEmployeePermission(adminId: String, employeeId: String, permission: String) { /* no-op */ }
    override suspend fun updateEmployeeStatus(adminId: String, employeeId: String, status: String) { /* no-op */ }

    // ─── Leave requests ───────────────────────────────────────────────────
    override fun observeLeaveRequestsForAdmin(adminId: String): Flow<List<LeaveRecord>> = flowOf(emptyList())
    override fun observeLeaveRequestsForUser(userId: String): Flow<List<LeaveRecord>> = flowOf(emptyList())
    override suspend fun submitLeaveRequest(
        adminId: String, userId: String, employeeName: String, department: String,
        leaveType: String, fromDateMs: Long, toDateMs: Long, totalDays: Int, reason: String,
    ): LeaveRecord = LeaveRecord(
        id = "stub", userId = userId, adminId = adminId,
        employeeName = employeeName, department = department,
        leaveType = leaveType, fromDateMs = fromDateMs, toDateMs = toDateMs,
        totalDays = totalDays, reason = reason, status = "pending",
    )
    override suspend fun updateLeaveStatus(
        leaveId: String, adminId: String, userId: String, newStatus: String, rejectionReason: String,
    ) { /* no-op */ }
}

actual object WorkforceDirectoryFactory {
    actual fun create(): WorkforceDirectory = StubWorkforceDirectory()
}