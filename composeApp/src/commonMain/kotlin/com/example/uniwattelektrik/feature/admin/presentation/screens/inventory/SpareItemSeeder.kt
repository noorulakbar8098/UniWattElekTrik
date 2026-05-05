package com.example.uniwattelektrik.feature.admin.presentation.screens.inventory

import com.example.uniwattelektrik.feature.workforce.data.remote.SpareItemRecord
import com.example.uniwattelektrik.feature.workforce.data.remote.WorkforceDirectory
import com.example.uniwattelektrik.core.AppLog
import kotlinx.coroutines.flow.first

/**
 * One-time seeder to populate Firestore with sample inventory data
 * (departments, equipment, spare items) for an admin on first install.
 *
 * Safe to call multiple times — each section skips if data already exists.
 */
object SpareItemSeeder {

    private val sampleSpares = listOf(
        // ── 1. CABLES ──────────────────────────────────────────────────────────
        SpareItemRecord("s1", "", "Cable", "1.5 Sq.mm PVC Wire", make = "Havells", size = "1.5 mm", core = "1C", unit = "Coil", price = 1250.0, stockQty = 15, hsn = "8544"),
        SpareItemRecord("s2", "", "Cable", "2.5 Sq.mm PVC Wire", make = "Havells", size = "2.5 mm", core = "1C", unit = "Coil", price = 1850.0, stockQty = 12, hsn = "8544"),
        SpareItemRecord("s3", "", "Cable", "4 Sq.mm PVC Wire",   make = "Polycab", size = "4 mm",   core = "1C", unit = "Coil", price = 2850.0, stockQty = 8, hsn = "8544"),
        SpareItemRecord("s4", "", "Cable", "6 Sq.mm PVC Wire",   make = "Polycab", size = "6 mm",   core = "1C", unit = "Coil", price = 4200.0, stockQty = 5, hsn = "8544"),
        SpareItemRecord("s5", "", "Cable", "10 Sq.mm PVC Wire",  make = "Anchor",  size = "10 mm",  core = "1C", unit = "Coil", price = 6800.0, stockQty = 3, hsn = "8544"),
        SpareItemRecord("s6", "", "Cable", "3 Core Flat Cable",  make = "Finolex", size = "2.5 mm", core = "3C", unit = "Mtr",  price = 85.0,   stockQty = 100, hsn = "8544"),

        // ── 2. SPARE COMPONENTS ────────────────────────────────────────────────
        SpareItemRecord("s7", "", "Spare Component", "MCB 32A C-Curve", make = "Schneider", currentRating = "32A", noOfPoles = "1", price = 450.0, stockQty = 25, hsn = "8536"),
        SpareItemRecord("s8", "", "Spare Component", "MCB 63A DP",      make = "Legrand",   currentRating = "63A", noOfPoles = "2", price = 1200.0, stockQty = 15, hsn = "8536"),
        SpareItemRecord("s9", "", "Spare Component", "Contactor 22A",   make = "Siemens",   currentRating = "22A", noOfPoles = "3", price = 2800.0, stockQty = 10, hsn = "8536"),
        SpareItemRecord("s10", "", "Spare Component", "Oil Filter",      make = "Atlas Copco", size = "Standard", price = 3500.0, stockQty = 8, hsn = "8421"),
        SpareItemRecord("s11", "", "Spare Component", "Pressure Gauge",  make = "Wika",        size = "0-10 Bar", price = 850.0,  stockQty = 12, hsn = "9026"),
        SpareItemRecord("s12", "", "Spare Component", "Mechanical Seal", make = "Burgmann",    size = "45mm",     price = 18000.0, stockQty = 2, hsn = "8484"),
        SpareItemRecord("s13", "", "Spare Component", "Bearing 6205",    make = "SKF",         size = "25x52x15", price = 350.0,  stockQty = 30, hsn = "8482"),
    )

    /** Seeds departments, equipment, and spare items in order. */
    suspend fun seedInventoryIfNeeded(adminId: String, directory: WorkforceDirectory) {
        val deptIdMap = seedDepartmentsIfNeeded(adminId, directory)
        seedEquipmentIfNeeded(adminId, directory, deptIdMap)
        seedSpareItemsIfNeeded(adminId, directory)
    }

    /** Returns map of sample-departmentId ("1".."9") → newly created Firestore doc id. */
    private suspend fun seedDepartmentsIfNeeded(
        adminId: String,
        directory: WorkforceDirectory,
    ): Map<String, String> {
        return try {
            val existing = directory.observeDepartments(adminId).first()
            if (existing.isNotEmpty()) {
                AppLog.i("InvSeeder", "Departments already exist for admin $adminId (${existing.size}), skipping seed")
                sampleDepartments.associate { sample ->
                    val match = existing.firstOrNull { it.name.equals(sample.name, ignoreCase = true) }
                    sample.id to (match?.id ?: "")
                }.filterValues { it.isNotBlank() }
            } else {
                val map = mutableMapOf<String, String>()
                for (dept in sampleDepartments) {
                    val rec = directory.addDepartment(adminId, dept.name)
                    map[dept.id] = rec.id
                }
                AppLog.i("InvSeeder", "Seeded ${map.size} departments for admin $adminId")
                map
            }
        } catch (e: Exception) {
            AppLog.e("InvSeeder", "Error seeding departments: ${e.message}")
            emptyMap()
        }
    }

    private suspend fun seedEquipmentIfNeeded(
        adminId: String,
        directory: WorkforceDirectory,
        deptIdMap: Map<String, String>,
    ) {
        try {
            val existing = directory.observeEquipment(adminId).first()
            if (existing.isNotEmpty()) {
                AppLog.i("InvSeeder", "Equipment already exists for admin $adminId (${existing.size}), skipping seed")
                return
            }
            if (deptIdMap.isEmpty()) {
                AppLog.e("InvSeeder", "Cannot seed equipment — department map is empty")
                return
            }
            var count = 0
            for (eq in sampleEquipment) {
                val mappedDeptId = deptIdMap[eq.departmentId] ?: continue
                directory.addEquipment(adminId, eq.name, mappedDeptId)
                count++
            }
            AppLog.i("InvSeeder", "Seeded $count equipment items for admin $adminId")
        } catch (e: Exception) {
            AppLog.e("InvSeeder", "Error seeding equipment: ${e.message}")
        }
    }

    suspend fun seedSpareItemsIfNeeded(adminId: String, directory: WorkforceDirectory) {
        try {
            val existing = directory.observeSpareItems(adminId).first()
            if (existing.isNotEmpty()) {
                AppLog.i("InvSeeder", "Spares already exist for admin $adminId (${existing.size}), skipping seed")
                return
            }
            for (spare in sampleSpares) {
                directory.addSpareItem(adminId, spare)
            }
            AppLog.i("InvSeeder", "Successfully seeded ${sampleSpares.size} spares for admin $adminId")
        } catch (e: Exception) {
            AppLog.e("InvSeeder", "Error seeding spares: ${e.message}")
        }
    }
}

