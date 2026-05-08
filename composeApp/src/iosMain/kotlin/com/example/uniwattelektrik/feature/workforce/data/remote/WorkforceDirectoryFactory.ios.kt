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
    ): TaskRecord = TaskRecord(
        id = "stub", adminId = adminId, userId = userId,
        title = title, description = description,
        location = location, time = time, day = day,
        priority = priority, status = "Todo",
        ownerAdminId = adminId, ownerAdminName = ownerAdminName,
        assigneeName = assigneeName, dueDate = dueDate,
        departmentId = departmentId, departmentName = departmentName,
        equipmentId = equipmentId, equipmentName = equipmentName,
        checklist = checklist, attachments = attachments,
        address = address, latitude = latitude, longitude = longitude,
    )
    override suspend fun acceptTask(taskId: String, lat: Double?, lon: Double?): TaskRecord =
        TaskRecord("stub", "", null, "", "", "", "", "", "InProgress")
    override suspend fun updateTask(
        taskId: String, adminId: String, userId: String?, title: String,
        location: String, time: String, day: String, priority: String,
        description: String,
        departmentId: String, departmentName: String,
        equipmentId: String, equipmentName: String,
        checklist: List<ChecklistItem>, attachments: List<String>,
        address: String, latitude: Double?, longitude: Double?,
        dueDate: Long?, assigneeName: String,
    ): TaskRecord = TaskRecord(
        id = taskId, adminId = adminId, userId = userId,
        title = title, description = description,
        location = location, time = time, day = day,
        priority = priority, status = "Todo",
        assigneeName = assigneeName, dueDate = dueDate,
        departmentId = departmentId, departmentName = departmentName,
        equipmentId = equipmentId, equipmentName = equipmentName,
        checklist = checklist, attachments = attachments,
        address = address, latitude = latitude, longitude = longitude,
    )
    override fun observeTaskNotes(taskId: String): Flow<List<TaskNote>> = flowOf(emptyList())
    override suspend fun completeTaskWithSignoff(
        adminId: String, taskId: String, assignedUserId: String,
        signoffDescription: String, downtimeMinutes: Int, rca: String,
        materialsUsed: List<MaterialUsedItem>, startTimeMs: Long, endTimeMs: Long,
    ): TaskRecord = TaskRecord(taskId, adminId)

    override suspend fun addTaskNote(
        taskId: String, authorId: String, authorName: String, role: String, message: String,
    ): TaskNote = TaskNote("stub", taskId, authorId, authorName, role, message, 0L)
    override suspend fun updateTaskStatus(
        adminId: String, taskId: String, assignedUserId: String?, newStatus: String,
    ): TaskRecord = TaskRecord("stub", adminId, assignedUserId, "", "", "", "", "", "Todo")
    override suspend fun setChecklistItemDone(taskId: String, index: Int, done: Boolean) { /* no-op */ }
    override suspend fun uploadTaskAttachment(adminId: String, contentUri: String): String = ""
    override suspend fun updateTaskAttachments(taskId: String, urls: List<String>) { /* no-op */ }
    override suspend fun updateEmployeePhoto(adminId: String, employeeId: String, contentUri: String): String = ""

    override fun observeInventory(adminId: String): Flow<List<InventoryRecord>> = flowOf(emptyList())
    override suspend fun addInventoryItem(
        adminId: String, name: String, type: String, price: Double, quantity: Int,
    ): InventoryRecord = InventoryRecord("stub", name, type, price, quantity)
    override fun observeInventoryTransactions(adminId: String): Flow<List<InventoryTransaction>> = flowOf(emptyList())
    override suspend fun addInventoryTransaction(
        adminId: String, userId: String, itemId: String, itemName: String, type: String, quantity: Int,
    ): InventoryTransaction = InventoryTransaction("stub", adminId, userId, itemId, itemName, type, quantity)

    override fun observeAttendance(adminId: String): Flow<List<AttendanceRecord>> = flowOf(emptyList())
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
}

actual object WorkforceDirectoryFactory {
    actual fun create(): WorkforceDirectory = StubWorkforceDirectory()
}