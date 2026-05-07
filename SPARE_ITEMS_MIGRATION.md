# Spare Items: Hardcoded → Firestore Migration Guide

## Summary
You've successfully migrated spare items from hardcoded data to **real-time Firestore storage**. All spare data is now tenant-isolated (scoped by `adminId`) and syncs in real-time across devices.

## Architecture

### Data Flow
```
Firestore (spare_items collection)
    ↓
WorkforceDirectory.observeSpareItems(adminId)  [Hot Flow]
    ↓
InventoryViewModel.spareItems  [StateFlow]
    ↓
SpareListScreen  [Real-time updates]
```

### Collection Structure
```
spare_items/
├── {itemId1}  (adminId="admin1", category="Cable", name="1.5 Sq.mm PVC Wire", ...)
├── {itemId2}  (adminId="admin1", category="Spare Component", ...)
└── {itemId3}  (adminId="admin2", ...)
```

## Changes Made

### 1. **WorkforceDirectory Interface** (`commonMain`)
Added three new suspend methods and one Flow observer:
```kotlin
fun observeSpareItems(adminId: String): Flow<List<SpareItemRecord>>
suspend fun addSpareItem(adminId: String, item: SpareItemRecord): SpareItemRecord
suspend fun updateSpareItem(adminId: String, itemId: String, updates: Map<String, Any?>): SpareItemRecord
suspend fun deleteSpareItem(adminId: String, itemId: String)
```

Added `SpareItemRecord` data class with full item schema.

### 2. **FirestoreWorkforceDirectory** (`androidMain`)
- Implemented all spare item methods using Firestore snapshot listeners
- Added `COL_SPARE_ITEMS = "spare_items"` collection constant
- Integrated spare collection into `deleteAllData()` danger zone

### 3. **InventoryViewModel** (NEW file, `commonMain`)
- Exposes real-time spare items via `StateFlow<List<SpareItem>>`
- `loadForAdmin(adminId)` subscribes to Firestore listener
- `deleteSpareItem()` triggers Firestore delete
- Handles error propagation

### 4. **SpareListScreen** (UPDATED)
- Now accepts `adminId: String` and `inventoryVm: InventoryViewModel` parameters
- `LaunchedEffect` subscribes to real-time data on mount
- Delete button calls `inventoryVm.deleteSpareItem(adminId, itemId)`
- Removed hardcoded `sampleSpares` list

### 5. **AdminShell** (UPDATED)
- Creates `inventoryVm` via `AppContainer.createInventoryViewModel()`
- Passes both to `SpareListScreen`

### 6. **AppContainer** (UPDATED)
- Added `createInventoryViewModel()` factory method

### 7. **SpareItemSeeder** (NEW file, `commonMain`)
- One-time seeder for initial data population
- Call `SpareItemSeeder.seedSpareItemsIfNeeded(adminId, directory)` on admin app start
- Safely skips if data already exists
- Includes all 13 sample spares (cables + components)

### 8. **iOS Stub** (UPDATED)
- Added no-op stubs for all new methods in `StubWorkforceDirectory`

## Setup: Enable Sample Data

### Option 1: One-Time Seed via App (Recommended)
In `AdminShell.kt`, call seeder on first load:
```kotlin
LaunchedEffect(user.id) {
    workforceVm.loadForAdmin(user.id)
    // Populate spares if this is the first time
    SpareItemSeeder.seedSpareItemsIfNeeded(user.id, workforceDirectory)
}
```

### Option 2: Manual Firestore Import
Use Firebase Console → `spare_items` collection → Import documents from JSON.

### Option 3: Add via App UI
- Navigate to Spare List → tap **"+ Add Spare"**
- Fill form → Save
- Item appears in real-time

## Testing Checklist

- [ ] **Admin opens Spare List** → Real-time data loads (not hardcoded)
- [ ] **Add new spare** → Appears instantly in list
- [ ] **Edit spare** → Changes sync across tabs/devices
- [ ] **Delete spare** → Confirm dialog → Removed from Firestore
- [ ] **Filter by category** → Works with real data
- [ ] **Logout/login** → Spares for correct admin reload
- [ ] **iOS app** → Stub returns empty (expected, no Firebase iOS SDK)

## File Locations

| File | Purpose |
|------|---------|
| `WorkforceDirectory.kt` | Interface + `SpareItemRecord` class |
| `FirestoreWorkforceDirectory.kt` | Firestore implementation |
| `InventoryViewModel.kt` | State management (NEW) |
| `SpareListScreen.kt` | UI (UPDATED) |
| `SpareItemSeeder.kt` | One-time seeder (NEW) |
| `AdminShell.kt` | Navigation wiring (UPDATED) |
| `AppContainer.kt` | DI factory (UPDATED) |
| `WorkforceDirectoryFactory.ios.kt` | iOS stub (UPDATED) |

## Firestore Security Rules

Ensure your `firestore.rules` includes:
```
match /spare_items/{itemId} {
  allow read, write: if request.auth.uid in resource.data.adminId || 
                        request.auth.uid == resource.data.adminId;
}
```

## Next Steps

1. **Call seeder** in `AdminShell` `LaunchedEffect`
2. **Test add/edit/delete** flows
3. **Deploy to production** after QA
4. Spare data is now **fully persistent** and **multi-device synced**

## Rollback

If you need to revert:
1. Restore original `SpareListScreen` (uses `sampleSpares`)
2. Remove `InventoryViewModel` reference from `AdminShell`
3. Data in Firestore persists (safe)

---

**Migration complete!** ✅ Spare items now live in Firestore with real-time sync.

