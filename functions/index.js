/**
 * UniWatt ElekTrik — Cloud Functions
 *
 * Reads device tokens from `admins/{uid}.fcmTokens[]` and `users/{uid}.fcmTokens[]`
 * and fans out push notifications via FCM Admin SDK whenever a relevant
 * Firestore document is created or updated.
 *
 * Triggers covered:
 *   • tasks/{id}          create  → notify the assigned user
 *   • tasks/{id}          update  → notify owning admin on status change
 *   • leave_requests/{id} create  → notify the owning admin
 *   • leave_requests/{id} update  → notify the requesting user on status change
 */
const { onDocumentCreated, onDocumentUpdated } =
  require("firebase-functions/v2/firestore");
const { onSchedule } = require("firebase-functions/v2/scheduler");
const admin = require("firebase-admin");

admin.initializeApp();
const db = admin.firestore();

// ─── helpers ────────────────────────────────────────────────────────────────

async function tokensForUser(uid) {
  if (!uid) return [];
  const snap = await db.doc(`users/${uid}`).get();
  return snap.data()?.fcmTokens || [];
}

async function tokensForAdmin(adminUid) {
  if (!adminUid) return [];
  const snap = await db.doc(`admins/${adminUid}`).get();
  return snap.data()?.fcmTokens || [];
}

async function send(tokens, title, body, data = {}) {
  const list = (tokens || []).filter(Boolean);
  if (!list.length) return;

  const res = await admin.messaging().sendEachForMulticast({
    tokens: list,
    notification: { title, body },
    data: Object.fromEntries(
      Object.entries(data).map(([k, v]) => [k, String(v)])
    ),
    android: {
      priority: "high",
      notification: { channelId: "uw_default" },
    },
  });

  // Best-effort cleanup of dead tokens
  const stale = res.responses
    .map((r, i) =>
      !r.success &&
      /registration-token-not-registered|invalid-registration-token/i
        .test(r.error?.code || "")
        ? list[i]
        : null,
    )
    .filter(Boolean);

  if (stale.length) {
    const writers = ["admins", "users"].map(async (col) => {
      const owners = await db
        .collection(col)
        .where("fcmTokens", "array-contains-any", stale.slice(0, 10))
        .get();
      const batch = db.batch();
      owners.forEach((doc) =>
        batch.update(doc.ref, {
          fcmTokens: admin.firestore.FieldValue.arrayRemove(...stale),
        }),
      );
      if (!owners.empty) await batch.commit();
    });
    await Promise.allSettled(writers);
  }
}

// ─── tasks ──────────────────────────────────────────────────────────────────

exports.onTaskCreated = onDocumentCreated("tasks/{taskId}", async (event) => {
  const t = event.data.data();
  // Multi-assignee aware: prefer the array, fall back to the legacy single field.
  const ids = Array.isArray(t.assignedUserIds) && t.assignedUserIds.length
    ? t.assignedUserIds
    : (t.assignedUserId ? [t.assignedUserId] : []);
  const tokenLists = await Promise.all(ids.map((uid) => tokensForUser(uid)));
  const tokens = tokenLists.flat();
  await send(
    tokens,
    "New task assigned",
    t.title || "You have a new task",
    { type: "task", taskId: event.params.taskId },
  );
});

exports.onTaskUpdated = onDocumentUpdated("tasks/{taskId}", async (event) => {
  const before = event.data.before.data(),
        after  = event.data.after.data();
  if (before.status === after.status) return;

  const tokens = await tokensForAdmin(after.adminId);
  await send(
    tokens,
    `Task ${after.status}`,
    after.title || "",
    { type: "task", taskId: event.params.taskId },
  );
});

// ─── task notes (chat) ──────────────────────────────────────────────────────
//
// Triggered when a new note is appended to `tasks/{taskId}/notes/{noteId}`.
// Direction-aware fan-out:
//   • author role "admin" → notify the assigned user
//   • author role "user"  → notify the owning admin
// The notification deep-links straight into the task detail screen via the
// "type=task" + "taskId" data payload, matching the existing onTaskCreated
// / onTaskUpdated routes the mobile client already handles.
exports.onTaskNoteCreated = onDocumentCreated(
  "tasks/{taskId}/notes/{noteId}",
  async (event) => {
    const note = event.data?.data() || {};
    const taskId = event.params.taskId;
    const message = (note.message || "").toString();
    if (!message.trim()) return;

    // Look up the parent task to know who the admin + assigned user are.
    const taskSnap = await db.doc(`tasks/${taskId}`).get();
    if (!taskSnap.exists) return;
    const task = taskSnap.data() || {};

    const role = (note.role || "").toLowerCase();
    let tokens = [];
    let title  = "";
    if (role === "admin") {
      // Admin → user(s). Notify every assignee on the task.
      const ids = Array.isArray(task.assignedUserIds) && task.assignedUserIds.length
        ? task.assignedUserIds
        : (task.assignedUserId ? [task.assignedUserId] : []);
      const tokenLists = await Promise.all(ids.map((uid) => tokensForUser(uid)));
      tokens = tokenLists.flat();
      title  = `Admin note · ${task.title || "Task"}`;
    } else {
      // User → admin (default when role is blank or "user")
      tokens = await tokensForAdmin(task.adminId);
      const who = note.authorName || task.assigneeName || "User";
      title  = `${who} · ${task.title || "Task"}`;
    }

    // Truncate the body so long notes don't overflow the system shade.
    const body = message.length > 140 ? message.slice(0, 137) + "…" : message;

    await send(
      tokens,
      title,
      body,
      { type: "task", taskId, noteId: event.params.noteId },
    );
  },
);

// ─── leave requests ─────────────────────────────────────────────────────────

exports.onLeaveCreated =
  onDocumentCreated("leave_requests/{id}", async (event) => {
    const l = event.data.data();
    const tokens = await tokensForAdmin(l.adminId);
    await send(
      tokens,
      "New leave request",
      l.reason || "",
      { type: "leave", leaveId: event.params.id },
    );
  });

exports.onLeaveUpdated =
  onDocumentUpdated("leave_requests/{id}", async (event) => {
    const before = event.data.before.data(),
          after  = event.data.after.data();
    if (before.status === after.status) return;

    const tokens = await tokensForUser(after.userId);
    await send(
      tokens,
      `Leave ${after.status}`,
      after.reason || "",
      { type: "leave", leaveId: event.params.id },
    );
  });

// ─── stock snapshots ────────────────────────────────────────────────────────
//
// snapshotMonthlyStock — scheduled monthly job
//
// Captures every admin's spare-item state at 00:05 on the 1st of each month
// (Asia/Kolkata). The doc represents both:
//   • the CLOSING stock for the previous month, and
//   • the OPENING stock for the new month.
//
// Storage layout:
//   monthly_stock_snapshots/{adminId}/months/{yyyy-mm}
//     {
//       snapshotAt: serverTimestamp,
//       year:       2026,
//       month:      "05",          // string, zero-padded, "yyyy-MM" key
//       items: [
//         { itemId, name, stockQty, price, value },
//         ...
//       ],
//       totalQty:   12345,
//       totalValue: 987654.32,
//     }
//
// The Kotlin client reads this doc via observeMonthlyStockSnapshot(...) and
// shows the opening → closing delta in the monthly report.
//
// IMPORTANT: this function requires the Firebase Blaze (pay-as-you-go) plan
// — Cloud Scheduler is not available on the Spark free tier. Cost: a single
// nightly Firestore read of spare_items + one write per admin per month
// (typically < ₹50/month for small-to-mid orgs).
exports.snapshotMonthlyStock = onSchedule(
  {
    schedule: "5 0 1 * *",       // 00:05 on day-of-month 1
    timeZone: "Asia/Kolkata",    // IST — adjust if you ship to other regions
    retryCount: 3,
  },
  async () => {
    // Compute the "snapshot key" for the month we're CLOSING (i.e. last
    // month). Running on May 1st → snapshot is for April (2026-04).
    const now = new Date();
    const closingMonth = new Date(now.getFullYear(), now.getMonth() - 1, 1);
    const yyyy = closingMonth.getFullYear();
    const mm   = String(closingMonth.getMonth() + 1).padStart(2, "0");
    const key  = `${yyyy}-${mm}`;

    const admins = await db.collection("admins").get();
    let writes = 0;

    for (const adminDoc of admins.docs) {
      const adminId = adminDoc.id;

      const sparesSnap = await db
        .collection("spare_items")
        .where("adminId", "==", adminId)
        .get();

      const items   = [];
      let totalQty   = 0;
      let totalValue = 0;

      for (const s of sparesSnap.docs) {
        const d        = s.data() || {};
        const stockQty = Number(d.stockQty) || 0;
        const price    = Number(d.price)    || 0;
        const value    = stockQty * price;
        totalQty   += stockQty;
        totalValue += value;
        items.push({
          itemId:   s.id,
          name:     d.name || "",
          stockQty,
          price,
          value,
        });
      }

      await db
        .doc(`monthly_stock_snapshots/${adminId}/months/${key}`)
        .set({
          snapshotAt: admin.firestore.FieldValue.serverTimestamp(),
          year:       yyyy,
          month:      mm,
          items,
          totalQty,
          totalValue,
        });
      writes += 1;
    }

    console.log(
      `[snapshotMonthlyStock] wrote ${writes} snapshot(s) for ${key}`,
    );
  },
);

