# Quick Start: Spare Items Now Use Firestore ✅

## TL;DR

Your hardcoded spare items are now **in Firestore** with real-time sync. No more local data!

## What Changed

| Before | Now |
|--------|-----|
| `SpareListScreen` loaded `sampleSpares` directly | Loads from Firestore via `InventoryViewModel` |
| Deletes removed from local list | Deletes persist to Firestore |
| No sync between devices | Real-time multi-device sync |
| Data lost on app restart | Data persists in Firestore ☁️ |

## How to Test

### 1. **Build & Run**
```bash
./gradlew build
# Run on Android/emulator
```

### 2. **Populate Sample Data** (optional, one-time)
Add to `AdminShell.kt` after `workforceVm.loadForAdmin()`:
```kotlin
SpareItemSeeder.seedSpareItemsIfNeeded(user.id, workforceDirectory)
```

Then run the app once (data is seeded to Firestore).

### 3. **Test the Features**
- Open **Spare List** → See items from Firestore
- Tap **+ Add Spare** → New item syncs to all devices
- Swipe/delete → Item removed from Firestore
- Logout & login → Items reload for your admin

### 4. **Verify in Firebase Console**
- Go to Firestore Database
- Check `spare_items` collection → Your data is there!

## Key Files

| File | What It Does |
|------|-------------|
| `InventoryViewModel.kt` | Manages spare state, connects to Firestore |
| `SpareListScreen.kt` | UI that uses real-time data |
| `FirestoreWorkforceDirectory.kt` | Firestore read/write logic |
| `SpareItemSeeder.kt` | One-time data import (optional) |

## No Breaking Changes ✨

- ✅ Existing `SpareItem` UI components still work
- ✅ All navigation still works
- ✅ Only the data source changed (hardcoded → Firestore)
- ✅ iOS gets no-op stub (returns empty list until Firebase iOS SDK wired)

## Gotchas

⚠️ **Seeder is optional**: You can manually add items via the UI instead

⚠️ **Multi-admin**: Each admin only sees their own spares

⚠️ **Offline**: Spares only load when online (can add local caching later)

## Next: Edit Screen

The add form already works. To make the edit form work with Firestore:

```kotlin
// In SpareItemFormScreen, replace local save with:
inventoryVm.updateSpareItem(adminId, itemId, updates)
```

---

That's it! Your spare items are now in the cloud. 🎉

