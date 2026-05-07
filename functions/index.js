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
  const tokens = await tokensForUser(t.assignedUserId);
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

