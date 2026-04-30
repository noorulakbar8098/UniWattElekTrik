package com.example.uniwattelektrik.core.sample

import androidx.compose.ui.graphics.Color
import com.example.uniwattelektrik.core.theme.AppTheme

/* ──────────────────────────────────────────────────────────────────────────
 *  In-memory demo data shared by both User & Admin modules.
 *
 *  This file plays the role of "fake repository" — every screen reads from
 *  here so the entire UI flows with realistic content. When the real backend
 *  is wired up, replace these static lists with repository/ViewModel state.
 * ────────────────────────────────────────────────────────────────────────── */

// ─── Tasks ─────────────────────────────────────────────────────────────────

enum class TaskStatus(val label: String, val color: Color, val bg: Color) {
    Todo      ("To do",       AppTheme.Ink500, AppTheme.Ink50),
    InProgress("In progress", AppTheme.Brand,  AppTheme.Brand50),
    Done      ("Completed",   AppTheme.Low,    AppTheme.LowBg),
}

enum class TaskPriority(val label: String, val color: Color, val bg: Color) {
    Low ("Low",    AppTheme.Low,  AppTheme.LowBg),
    Med ("Medium", AppTheme.Med,  AppTheme.MedBg),
    High("High",   AppTheme.High, AppTheme.HighBg),
}

data class SampleTask(
    val id: String,
    val title: String,
    val description: String,
    val location: String,
    val distanceKm: Double,
    val time: String,            // HH:mm
    val day: String,             // "Today", "Tomorrow", "Mar 14"
    val priority: TaskPriority,
    val status: TaskStatus,
    val assigneeInitials: String,
)

object SampleTasks {
    val all: List<SampleTask> = listOf(
        SampleTask(
            id = "T-001", title = "Transformer oil leak — 11 kV",
            description = "Inspect and seal the oil leak on transformer T3. Carry replacement gaskets and 5 L oil.",
            location = "Yelahanka", distanceKm = 2.4, time = "10:30", day = "Today",
            priority = TaskPriority.High, status = TaskStatus.InProgress, assigneeInitials = "RK",
        ),
        SampleTask(
            id = "T-002", title = "Insulator replacement — 33 kV",
            description = "Replace damaged porcelain insulator unit on Pole 47. Lift required.",
            location = "Whitefield", distanceKm = 5.1, time = "13:00", day = "Today",
            priority = TaskPriority.Med, status = TaskStatus.Todo, assigneeInitials = "RK",
        ),
        SampleTask(
            id = "T-003", title = "Routine breaker inspection",
            description = "Quarterly inspection of substation breakers — record contact resistance.",
            location = "Hebbal Substation", distanceKm = 0.2, time = "15:30", day = "Today",
            priority = TaskPriority.Low, status = TaskStatus.Todo, assigneeInitials = "RK",
        ),
        SampleTask(
            id = "T-004", title = "Cable joint thermal scan",
            description = "Run an IR scan along the underground cable joint near junction 14.",
            location = "Yeshwantpur", distanceKm = 3.7, time = "09:00", day = "Tomorrow",
            priority = TaskPriority.Med, status = TaskStatus.Todo, assigneeInitials = "AM",
        ),
        SampleTask(
            id = "T-005", title = "LT panel cleaning",
            description = "Dust-out and tighten lugs on LT panel B. Use insulation cleaner only.",
            location = "Indiranagar", distanceKm = 6.9, time = "11:30", day = "Tomorrow",
            priority = TaskPriority.Low, status = TaskStatus.Todo, assigneeInitials = "RK",
        ),
        SampleTask(
            id = "T-006", title = "Earthing strip continuity check",
            description = "Verify continuity from main earth pit to all panels. Log resistance values.",
            location = "Hebbal Substation", distanceKm = 0.2, time = "16:00", day = "Mar 14",
            priority = TaskPriority.High, status = TaskStatus.Done, assigneeInitials = "SP",
        ),
    )

    fun byId(id: String): SampleTask? = all.firstOrNull { it.id == id }
}

// ─── Notifications ─────────────────────────────────────────────────────────

enum class NotifKind(val emoji: String, val tint: Color) {
    Task   ("📋", AppTheme.Brand),
    Alert  ("⚠️", AppTheme.High),
    Leave  ("🏖", AppTheme.Med),
    System ("🔔", AppTheme.Violet),
}

data class SampleNotification(
    val id: String,
    val kind: NotifKind,
    val title: String,
    val body: String,
    val time: String,
    val unread: Boolean,
)

object SampleNotifications {
    val all: List<SampleNotification> = listOf(
        SampleNotification("N-1", NotifKind.Alert, "Critical: Transformer T3", "Oil-pressure threshold breached. Open work order T-001.", "2 min", true),
        SampleNotification("N-2", NotifKind.Task,  "New task assigned",        "Insulator replacement at Whitefield (Pole 47).",                "18 min", true),
        SampleNotification("N-3", NotifKind.Leave, "Leave approved",           "Your 14-Mar leave was approved by Vikram Sethi.",                "1 h",    false),
        SampleNotification("N-4", NotifKind.Task,  "Task completed",           "Earthing strip continuity check has been signed off.",          "3 h",    false),
        SampleNotification("N-5", NotifKind.System,"Shift roster updated",     "Next week's roster is now available.",                          "Yesterday", false),
    )
}

// ─── Leave ─────────────────────────────────────────────────────────────────

enum class LeaveStatus(val label: String, val color: Color, val bg: Color) {
    Pending ("Pending",  AppTheme.Med,  AppTheme.MedBg),
    Approved("Approved", AppTheme.Low,  AppTheme.LowBg),
    Rejected("Rejected", AppTheme.High, AppTheme.HighBg),
}

enum class LeaveType(val emoji: String, val label: String) {
    Casual ("☕", "Casual"),
    Sick   ("🤒", "Sick"),
    Earned ("🌴", "Earned"),
}

data class SampleLeave(
    val id: String,
    val type: LeaveType,
    val from: String,
    val to: String,
    val days: Int,
    val reason: String,
    val status: LeaveStatus,
)

object SampleLeaves {
    val balance = listOf(
        Triple(LeaveType.Casual, 6,  12),
        Triple(LeaveType.Sick,   3,  10),
        Triple(LeaveType.Earned, 11, 18),
    )

    val history: List<SampleLeave> = listOf(
        SampleLeave("L-1", LeaveType.Earned, "14 Mar", "16 Mar", 3, "Family wedding",        LeaveStatus.Approved),
        SampleLeave("L-2", LeaveType.Sick,   "02 Mar", "02 Mar", 1, "Fever",                 LeaveStatus.Approved),
        SampleLeave("L-3", LeaveType.Casual, "21 Mar", "21 Mar", 1, "Bank work",             LeaveStatus.Pending),
        SampleLeave("L-4", LeaveType.Casual, "08 Feb", "09 Feb", 2, "Personal",              LeaveStatus.Rejected),
    )
}

// ─── Employees (admin) ─────────────────────────────────────────────────────

enum class EmployeeStatus(val label: String, val color: Color, val bg: Color) {
    Active   ("Active",    AppTheme.Low,   AppTheme.LowBg),
    OnLeave  ("On leave",  AppTheme.Med,   AppTheme.MedBg),
    Inactive ("Inactive",  AppTheme.Ink500, AppTheme.Ink50),
}

data class SampleEmployee(
    val id: String,
    val name: String,
    val role: String,
    val zone: String,
    val tasksOpen: Int,
    val avatarGradient: List<Color>,
    val status: EmployeeStatus,
)

object SampleEmployees {
    val all: List<SampleEmployee> = listOf(
        SampleEmployee("E-1024", "Ravi Kumar",     "Substation Engineer · L2", "Hebbal",      3, listOf(Color(0xFFFFB28A), Color(0xFFEC8552)), EmployeeStatus.Active),
        SampleEmployee("E-1025", "Anita Mehra",    "Line Technician · L1",     "Whitefield",  5, listOf(Color(0xFFA5C8FF), Color(0xFF1E73E8)), EmployeeStatus.Active),
        SampleEmployee("E-1026", "Suresh Pillai",  "Cable Specialist · L3",    "Yeshwantpur", 0, listOf(Color(0xFFB5F0C0), Color(0xFF10B981)), EmployeeStatus.OnLeave),
        SampleEmployee("E-1027", "Vikram Sethi",   "Operations Lead",          "Central",     2, listOf(Color(0xFFD8B5FF), Color(0xFF8B5CF6)), EmployeeStatus.Active),
        SampleEmployee("E-1028", "Priya Rao",      "Apprentice · L0",          "Indiranagar", 1, listOf(Color(0xFFFFC8E0), Color(0xFFEF4444)), EmployeeStatus.Inactive),
    )
}

// ─── Admin KPIs / activity ─────────────────────────────────────────────────

data class KpiPoint(val label: String, val value: String, val delta: String, val positive: Boolean, val accent: Color)

object SampleAdminData {
    val kpis = listOf(
        KpiPoint("Active tasks",      "47",  "+12 %", true,  AppTheme.Brand),
        KpiPoint("Employees on duty", "23",  "+2",    true,  AppTheme.Low),
        KpiPoint("Critical alerts",   "3",   "+1",    false, AppTheme.High),
        KpiPoint("Cost this month",   "₹4.2L","-6 %", true,  AppTheme.Violet),
    )

    data class Activity(val emoji: String, val title: String, val time: String)
    val activity = listOf(
        Activity("✅", "Ravi Kumar completed T-006 Earthing check",  "2 min ago"),
        Activity("📍", "Anita Mehra checked in at Whitefield",       "14 min ago"),
        Activity("⚠️", "Critical alert raised — Transformer T3",     "32 min ago"),
        Activity("📋", "Suresh Pillai requested 3-day leave",        "1 h ago"),
        Activity("🛠", "Inventory low: 11 kV cable lugs (8 left)",   "3 h ago"),
    )
}

// ─── Inventory ─────────────────────────────────────────────────────────────

data class SampleInventoryItem(
    val sku: String,
    val name: String,
    val category: String,
    val stock: Int,
    val reorder: Int,
)

object SampleInventory {
    val all = listOf(
        SampleInventoryItem("SKU-1100", "11 kV cable lugs",            "Cabling",   8,  20),
        SampleInventoryItem("SKU-1101", "Porcelain insulator (33 kV)", "Hardware",  46, 30),
        SampleInventoryItem("SKU-1102", "Transformer oil — 5 L can",   "Fluids",    12, 10),
        SampleInventoryItem("SKU-1103", "MCB 32 A — C-curve",          "Switchgear",78, 40),
        SampleInventoryItem("SKU-1104", "Earthing strip — 25 mm",      "Hardware",  3,  15),
    )
}
