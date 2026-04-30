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

    override fun observeTasksForAdmin(adminId: String): Flow<List<TaskRecord>> = flowOf(emptyList())
    override fun observeTasksForUser(userId: String): Flow<List<TaskRecord>> = flowOf(emptyList())
    override suspend fun addTask(
        adminId: String, userId: String?, title: String, location: String,
        time: String, day: String, priority: String,
    ): TaskRecord = TaskRecord("stub", adminId, userId, title, location, time, day, priority, "Todo")

    override fun observeInventory(adminId: String): Flow<List<InventoryRecord>> = flowOf(emptyList())
    override suspend fun addInventoryItem(
        adminId: String, name: String, type: String, price: Double, quantity: Int,
    ): InventoryRecord = InventoryRecord("stub", name, type, price, quantity)

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
}

actual object WorkforceDirectoryFactory {
    actual fun create(): WorkforceDirectory = StubWorkforceDirectory()
}

