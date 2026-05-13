# UniWatt Push Notifications — Setup notes (Android)

End-to-end FCM integration. Tokens are written by the Android client to
Firestore; pushes are fanned out by these Cloud Functions.

## Token storage

```
admins/{uid}: {
  fcmTokens:   ["abc...", ...],
  fcmTokenMeta: { "abc...": { platform, updatedAt } }
}
users/{uid}:  same shape
```

## Triggers

| Trigger                          | Recipient                  |
| -------------------------------- | -------------------------- |
| `tasks/{id}` create              | the assigned user          |
| `tasks/{id}` status change       | the owning admin           |
| `leave_requests/{id}` create     | the owning admin           |
| `leave_requests/{id}` status     | the requesting user        |

## Deploy

```bash
# from project root
cd functions && npm install && cd ..

firebase login          # if not already
firebase use <project>  # creates .firebaserc

firebase deploy --only functions
```

## Test the device end first

1. Run the app on a real Android 13+ device, log in.
2. Open Firestore → `admins/<your-uid>` (or `users/<uid>`) and confirm
   `fcmTokens` contains a string.
3. Firebase Console → Cloud Messaging → "Send test message" → paste the token.
   The phone should buzz immediately.

Once that works, deploy the functions and try a real flow (create a task,
submit a leave request).

## Logout cleanup

`PushTokenRegistrar.unregisterCurrentDevice()` is invoked from
`MainActivity` when the auth session flows to `null`. It removes the token
from Firestore *and* deletes it from FCM so a different user signing in on
the same device gets a brand-new token.

---

# Monthly stock snapshot — `snapshotMonthlyStock`

A scheduled function that captures each admin's closing stock balance on
the 1st of every month and writes it to Firestore. The Reports screen
reads the previous month's snapshot as the new month's *opening stock* so
admins see `opening → closing` deltas without keeping running totals
client-side.

## Schedule

```
"5 0 1 * *"   Asia/Kolkata
```

00:05 IST on the 1st of every month. Captures the month that *just
ended* — i.e. the snapshot doc keyed `2026-04` is written on 2026-05-01
and represents April's closing balance (= May's opening balance).

## What it writes

```
monthly_stock_snapshots/{adminId}/months/{yyyy-mm}: {
  year:        2026,
  month:       4,            // 1..12, the month being CLOSED
  items:       [ { itemId, name, stockQty, price, value }, ... ],
  totalQty:    1234,
  totalValue:  56789.50,
  snapshotAt:  serverTimestamp(),
}
```

The Kotlin client (`observeMonthlyStockSnapshot(adminId, year, month)`)
reads this doc; `ReportsAggregator.build(..., openingSnapshot = …)`
threads it into the Stock section and computes `qtyDelta` / `valueDelta`.

## Prerequisites

- **Firebase Blaze plan** — scheduled functions require pay-as-you-go.
  Cost for a small org (a few hundred items, ~10 admins) is well under
  ₹50/month: one trigger per month per admin, ~10 KB write each.
- `firebase-functions` v4+ and Node 20+ in `package.json` (already
  configured).

## Deploy

```bash
cd functions && npm install && cd ..
firebase deploy --only functions:snapshotMonthlyStock
```

## Backfill (optional)

If you turn on this function mid-cycle, the opening-stock panel will be
blank for the very first reporting month. Either wait one cycle, or
manually create the previous month's doc:

```js
// Firestore console → manual doc at:
// monthly_stock_snapshots/<adminId>/months/2026-04
{
  year: 2026, month: 4,
  items: [...current spare_items snapshot...],
  totalQty: <sum>, totalValue: <sum>,
  snapshotAt: <today>
}
```

