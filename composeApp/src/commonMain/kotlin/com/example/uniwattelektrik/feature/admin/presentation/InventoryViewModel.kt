package com.example.uniwattelektrik.feature.admin.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.uniwattelektrik.core.AppLog
import com.example.uniwattelektrik.feature.admin.presentation.screens.inventory.SpareItem
import com.example.uniwattelektrik.feature.admin.presentation.screens.inventory.Department
import com.example.uniwattelektrik.feature.admin.presentation.screens.inventory.Equipment
import com.example.uniwattelektrik.feature.workforce.data.remote.SpareItemRecord
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
 * Manages spare items for admin inventory. Reads from Firestore real-time.
 */
class InventoryViewModel(
    private val directory: WorkforceDirectory,
) : ViewModel() {

    private val _spareItems = MutableStateFlow<List<SpareItem>>(emptyList())
    val spareItems: StateFlow<List<SpareItem>> = _spareItems.asStateFlow()

    private val _departments = MutableStateFlow<List<Department>>(emptyList())
    val departments: StateFlow<List<Department>> = _departments.asStateFlow()

    private val _equipment = MutableStateFlow<List<Equipment>>(emptyList())
    val equipment: StateFlow<List<Equipment>> = _equipment.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    // ─── Excel import state ──────────────────────────────────────────────────
    /** All parsed sheets held while user navigates from picker → preview screen. */
    var stagedSheets: List<com.example.uniwattelektrik.core.platform.ParsedSheet>? = null

    private val _importInProgress = MutableStateFlow(false)
    val importInProgress: StateFlow<Boolean> = _importInProgress.asStateFlow()

    /** null = idle, otherwise number of rows just successfully uploaded. */
    private val _importResult = MutableStateFlow<Int?>(null)
    val importResult: StateFlow<Int?> = _importResult.asStateFlow()

    fun consumeImportResult() { _importResult.value = null }

    // ─── Bulk delete state ───────────────────────────────────────────────────
    private val _deleteInProgress = MutableStateFlow(false)
    val deleteInProgress: StateFlow<Boolean> = _deleteInProgress.asStateFlow()

    /** null = idle, otherwise number of items just successfully deleted. */
    private val _deleteResult = MutableStateFlow<Int?>(null)
    val deleteResult: StateFlow<Int?> = _deleteResult.asStateFlow()

    fun consumeDeleteResult() { _deleteResult.value = null }

    private var itemsJob: Job? = null
    private var deptJob: Job? = null
    private var equipmentJob: Job? = null

    fun loadForAdmin(adminId: String) {
        itemsJob?.cancel()
        deptJob?.cancel()
        equipmentJob?.cancel()
        _error.value = null
        _loading.value = true

        itemsJob = directory.observeSpareItems(adminId)
            .onEach { records ->
                _spareItems.value = records.map { toSpareItem(it) }
                _loading.value = false
            }
            .catch { err ->
                _error.value = err.message
                _loading.value = false
                AppLog.e("InventoryVM", "Error loading spares: ${err.message}")
            }
            .launchIn(viewModelScope)

        deptJob = directory.observeDepartments(adminId)
            .onEach { records ->
                _departments.value = records.map { Department(id = it.id, name = it.name) }
            }
            .catch { err -> AppLog.e("InventoryVM", "Error loading departments: ${err.message}") }
            .launchIn(viewModelScope)

        equipmentJob = directory.observeEquipment(adminId)
            .onEach { records ->
                _equipment.value = records.map {
                    Equipment(id = it.id, name = it.name, departmentId = it.departmentId)
                }
            }
            .catch { err -> AppLog.e("InventoryVM", "Error loading equipment: ${err.message}") }
            .launchIn(viewModelScope)
    }

    // ─── Departments ─────────────────────────────────────────────────────────

    fun addDepartment(adminId: String, name: String) {
        viewModelScope.launch {
            try { directory.addDepartment(adminId, name) }
            catch (e: Exception) {
                _error.value = e.message
                AppLog.e("InventoryVM", "Error adding department: ${e.message}")
            }
        }
    }

    fun updateDepartment(adminId: String, departmentId: String, name: String) {
        viewModelScope.launch {
            try { directory.updateDepartment(adminId, departmentId, name) }
            catch (e: Exception) {
                _error.value = e.message
                AppLog.e("InventoryVM", "Error updating department: ${e.message}")
            }
        }
    }

    fun deleteDepartment(adminId: String, departmentId: String) {
        viewModelScope.launch {
            try { directory.deleteDepartment(adminId, departmentId) }
            catch (e: Exception) {
                _error.value = e.message
                AppLog.e("InventoryVM", "Error deleting department: ${e.message}")
            }
        }
    }

    // ─── Equipment ───────────────────────────────────────────────────────────

    fun addEquipment(adminId: String, name: String, departmentId: String) {
        viewModelScope.launch {
            try { directory.addEquipment(adminId, name, departmentId) }
            catch (e: Exception) {
                _error.value = e.message
                AppLog.e("InventoryVM", "Error adding equipment: ${e.message}")
            }
        }
    }

    fun updateEquipment(adminId: String, equipmentId: String, name: String, departmentId: String) {
        viewModelScope.launch {
            try { directory.updateEquipment(adminId, equipmentId, name, departmentId) }
            catch (e: Exception) {
                _error.value = e.message
                AppLog.e("InventoryVM", "Error updating equipment: ${e.message}")
            }
        }
    }

    fun deleteEquipment(adminId: String, equipmentId: String) {
        viewModelScope.launch {
            try { directory.deleteEquipment(adminId, equipmentId) }
            catch (e: Exception) {
                _error.value = e.message
                AppLog.e("InventoryVM", "Error deleting equipment: ${e.message}")
            }
        }
    }

    fun addSpareItem(adminId: String, draft: SpareItem) {
        viewModelScope.launch {
            try {
                val record = SpareItemRecord(
                    id             = "",
                    adminId        = adminId,
                    category       = draft.category,
                    name           = draft.name,
                    make           = draft.make,
                    size           = draft.size,
                    core           = draft.core,
                    currentRating  = draft.currentRating,
                    noOfPoles      = draft.noOfPoles,
                    unit           = draft.unit,
                    price          = draft.price,
                    stockQty       = draft.stockQty,
                    hsn            = draft.hsn,
                    vendorName1    = draft.vendorName1,
                    vendorGst1     = draft.vendorGst1,
                    vendorContact1 = draft.vendorContact1,
                    vendorAddress1 = draft.vendorAddress1,
                    vendorName2    = draft.vendorName2,
                    vendorGst2     = draft.vendorGst2,
                    vendorContact2 = draft.vendorContact2,
                    vendorAddress2 = draft.vendorAddress2,
                    vendorLocation = draft.vendorLocation,
                )
                directory.addSpareItem(adminId, record)
                AppLog.i("InventoryVM", "Spare item added: ${draft.name}")
            } catch (e: Exception) {
                _error.value = e.message
                AppLog.e("InventoryVM", "Error adding spare: ${e.message}")
            }
        }
    }

    /**
     * Convenience: persist a draft from [SpareItemFormScreen]. If the draft has a
     * blank id we add a new doc; otherwise we patch the existing one with all
     * editable fields.
     */
    fun saveSpareItem(adminId: String, draft: SpareItem) {
        if (draft.id.isBlank()) {
            addSpareItem(adminId, draft)
        } else {
            updateSpareItem(
                adminId = adminId,
                itemId  = draft.id,
                updates = mapOf(
                    "category"       to draft.category,
                    "name"           to draft.name,
                    "make"           to draft.make,
                    "size"           to draft.size,
                    "core"           to draft.core,
                    "currentRating"  to draft.currentRating,
                    "noOfPoles"      to draft.noOfPoles,
                    "unit"           to draft.unit,
                    "price"          to draft.price,
                    "stockQty"       to draft.stockQty,
                    "hsn"            to draft.hsn,
                    "vendorName1"    to draft.vendorName1,
                    "vendorGst1"     to draft.vendorGst1,
                    "vendorContact1" to draft.vendorContact1,
                    "vendorAddress1" to draft.vendorAddress1,
                    "vendorName2"    to draft.vendorName2,
                    "vendorGst2"     to draft.vendorGst2,
                    "vendorContact2" to draft.vendorContact2,
                    "vendorAddress2" to draft.vendorAddress2,
                    "vendorLocation" to draft.vendorLocation,
                ),
            )
        }
    }

    fun updateSpareItem(adminId: String, itemId: String, updates: Map<String, Any?>) {
        viewModelScope.launch {
            try {
                directory.updateSpareItem(adminId, itemId, updates)
                AppLog.i("InventoryVM", "Spare item updated: $itemId")
            } catch (e: Exception) {
                _error.value = e.message
                AppLog.e("InventoryVM", "Error updating spare: ${e.message}")
            }
        }
    }

    fun deleteSpareItem(adminId: String, itemId: String) {
        viewModelScope.launch {
            try {
                directory.deleteSpareItem(adminId, itemId)
                AppLog.i("InventoryVM", "Spare item deleted: $itemId")
            } catch (e: Exception) {
                _error.value = e.message
                AppLog.e("InventoryVM", "Error deleting spare: ${e.message}")
            }
        }
    }

    /**
     * Bulk-delete the supplied spare-item ids in Firestore-batch chunks.
     * Emits the deleted count on [deleteResult] for snackbar feedback.
     */
    fun bulkDeleteSpareItems(adminId: String, itemIds: List<String>) {
        if (itemIds.isEmpty()) return
        viewModelScope.launch {
            _deleteInProgress.value = true
            try {
                val n = directory.bulkDeleteSpareItems(adminId, itemIds)
                _deleteResult.value = n
                AppLog.i("InventoryVM", "Bulk delete success: $n")
            } catch (e: Exception) {
                _error.value = e.message
                AppLog.e("InventoryVM", "Bulk delete failed: ${e.message}")
            } finally {
                _deleteInProgress.value = false
            }
        }
    }

    fun bulkImportSpareItems(adminId: String, drafts: List<SpareItem>) {
        viewModelScope.launch {
            _importInProgress.value = true
            try {
                val records = drafts.map { d ->
                    SpareItemRecord(
                        id             = "",
                        adminId        = adminId,
                        category       = d.category,
                        name           = d.name,
                        make           = d.make,
                        size           = d.size,
                        core           = d.core,
                        currentRating  = d.currentRating,
                        noOfPoles      = d.noOfPoles,
                        unit           = d.unit,
                        price          = d.price,
                        stockQty       = d.stockQty,
                        hsn            = d.hsn,
                        vendorName1    = d.vendorName1,
                        vendorGst1     = d.vendorGst1,
                        vendorContact1 = d.vendorContact1,
                        vendorAddress1 = d.vendorAddress1,
                        vendorName2    = d.vendorName2,
                        vendorGst2     = d.vendorGst2,
                        vendorContact2 = d.vendorContact2,
                        vendorAddress2 = d.vendorAddress2,
                        vendorLocation = d.vendorLocation,
                    )
                }
                val n = directory.bulkInsertSpareItems(adminId, records)
                _importResult.value = n
                AppLog.i("InventoryVM", "Bulk import success: $n")
            } catch (e: Exception) {
                _error.value = e.message
                AppLog.e("InventoryVM", "Bulk import failed: ${e.message}")
            } finally {
                _importInProgress.value = false
            }
        }
    }

    private fun toSpareItem(record: SpareItemRecord): SpareItem = SpareItem(
        id             = record.id,
        category       = record.category,
        name           = record.name,
        make           = record.make,
        size           = record.size,
        core           = record.core,
        currentRating  = record.currentRating,
        noOfPoles      = record.noOfPoles,
        unit           = record.unit,
        price          = record.price,
        stockQty       = record.stockQty,
        hsn            = record.hsn,
        vendorName1    = record.vendorName1,
        vendorGst1     = record.vendorGst1,
        vendorContact1 = record.vendorContact1,
        vendorAddress1 = record.vendorAddress1,
        vendorName2    = record.vendorName2,
        vendorGst2     = record.vendorGst2,
        vendorContact2 = record.vendorContact2,
        vendorAddress2 = record.vendorAddress2,
        vendorLocation = record.vendorLocation,
    )
}

