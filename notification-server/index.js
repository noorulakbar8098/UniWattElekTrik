/**
 * UniWatt ElekTrik — Notification Server
 *
 * Drop-in replacement for the previous Firebase Cloud Functions push pipeline.
 * Runs as a single long-lived Node process (Fly.io free tier or any Docker
 * host). Subscribes to Firestore via the Admin SDK and fans out FCM pushes
 * exactly the same way Cloud Functions used to — same payload shape, same
 * channels, same deep-link data.
 *
 * Why this works in killed state:
 *   The trigger source (Cloud Functions / this server / `firebase` CLI)
 *   doesn't affect the on-device delivery path. FCM hands the message to
 *   Google Play Services, which paints the system tray. Whether your app is
 *   running, backgrounded, or force-stopped is irrelevant — Play Services is
 *   a privileged process that's never killed.
 *
 * Handlers (mirrored 1:1 from functions/index.js):
 *   • tasks                — onCreated  → notify assignee(s)
 *   • tasks                — onUpdated  → notify owning admin on status change
 *   • tasks/{id}/notes     — onCreated  → direction-aware (admin↔user) chat push
 *   • leave_requests       — onCreated  → notify admin
 *   • leave_requests       — onUpdated  → notify user on status change
 *   • snapshotMonthlyStock — node-cron at "5 0 1 * *" Asia/Kolkata
 *
 * Setup:
 *   1. `npm install` (firebase-admin + node-cron)
 *   2. set FIREBASE_SERVICE_ACCOUNT env var to the JSON contents of your
 *      Firebase service-account key (Console → Project Settings → Service
 *      Accounts → Generate New Private Key).
 *   3. `npm start` — or deploy to Fly.io with `fly deploy`.
 */

const admin = require("firebase-admin");
const cron  = require("node-cron");

/* ───── Bootstrap ─────────────────────────────────────────────────────── */

const credsJson = process.env.FIREBASE_SERVICE_ACCOUNT;
if (!credsJson) {
  console.error(
    "FATAL: FIREBASE_SERVICE_ACCOUNT env var is missing.\n" +
    "Set it to the full JSON contents of your Firebase service-account key.\n" +
    "  fly secrets set FIREBASE_SERVICE_ACCOUNT=\"$(cat path/to/key.json)\"",
  );
  process.exit(1);
}

admin.initializeApp({
  credential: admin.credential.cert(JSON.parse(credsJson)),
});
const db  = admin.firestore();
const fcm = admin.messaging();

/**
 * Server-start instant. Firestore snapshot listeners deliver every existing
 * doc on their first emission ("added" change-type for everything). We skip
 * those by filtering each query with where("createdAt", ">=", startupTime)
 * — only docs created AFTER the server came online trigger a push.
 */
const startupTime = admin.firestore.Timestamp.now();

/* ───── Helpers (mirrored from functions/index.js) ────────────────────── */

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

/**
 * Multicast send + dead-token cleanup. Pulled straight from the Cloud
 * Functions version so payload shape stays identical — Android client doesn't
 * need to change anything.
 */
async function send(tokens, title, body, data = {}) {
  const list = (tokens || []).filter(Boolean);
  if (!list.length) return;

  const res = await fcm.sendEachForMulticast({
    tokens: list,
    notification: { title, body },
    data: Object.fromEntries(
      Object.entries(data).map(([k, v]) => [k, String(v)]),
    ),
    android: {
      priority: "high",
      notification: { channelId: "uw_default" },
    },
  });

  console.log(
    `[fcm] sent "${title}" to ${list.length} token(s) — ` +
    `success=${res.successCount} failure=${res.failureCount}`,
  );

  // Best-effort cleanup of dead tokens.
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

/* ───── tasks ─────────────────────────────────────────────────────────── */

// onCreated → notify the assignee(s).
db.collection("tasks")
  .where("createdAt", ">=", startupTime)
  .onSnapshot(
    (snap) => {
      snap.docChanges().forEach(async (change) => {
        if (change.type !== "added") return;
        try {
          const t = change.doc.data();
          const ids = Array.isArray(t.assignedUserIds) && t.assignedUserIds.length
            ? t.assignedUserIds
            : t.assignedUserId
              ? [t.assignedUserId]
              : [];
          const tokenLists = await Promise.all(ids.map((uid) => tokensForUser(uid)));
          const tokens = tokenLists.flat();
          await send(
            tokens,
            "New task assigned",
            t.title || "You have a new task",
            { type: "task", taskId: change.doc.id },
          );
        } catch (e) {
          console.error("[tasks.onCreated] error:", e);
        }
      });
    },
    (err) => console.error("[tasks.onSnapshot] listener error:", err),
  );

// onUpdated → notify the owning admin when the status changes. Tracked via
// a small in-memory map of last-known statuses; cheaper than touching
// Firestore on every "modified" event.
const lastTaskStatus = new Map();
db.collection("tasks").onSnapshot(
  (snap) => {
    snap.docChanges().forEach(async (change) => {
      if (change.type !== "modified") {
        // Prime the map on initial load + capture new docs so updates
        // are tracked against a known baseline.
        if (change.doc.exists) {
          lastTaskStatus.set(change.doc.id, change.doc.data().status || "");
        }
        return;
      }
      try {
        const after  = change.doc.data();
        const before = lastTaskStatus.get(change.doc.id);
        lastTaskStatus.set(change.doc.id, after.status || "");

        if (before === undefined) return;          // first sight after restart
        if (before === after.status) return;       // status didn't change

        const tokens = await tokensForAdmin(after.adminId);
        await send(
          tokens,
          `Task ${after.status}`,
          after.title || "",
          { type: "task", taskId: change.doc.id },
        );
      } catch (e) {
        console.error("[tasks.onUpdated] error:", e);
      }
    });
  },
  (err) => console.error("[tasks.status onSnapshot] listener error:", err),
);

/* ───── task notes (chat) ─────────────────────────────────────────────── */

// We can't use a flat collection-group listener here without a Firestore
// index, so we listen per-task using collectionGroup. collectionGroup +
// where on createdAt requires a single-field index that's auto-created.
db.collectionGroup("notes")
  .where("createdAt", ">=", startupTime)
  .onSnapshot(
    (snap) => {
      snap.docChanges().forEach(async (change) => {
        if (change.type !== "added") return;
        try {
          const note   = change.doc.data() || {};
          const taskId = change.doc.ref.parent.parent.id;    // tasks/{taskId}
          const noteId = change.doc.id;
          const message = (note.message || "").toString();
          if (!message.trim()) return;

          const taskSnap = await db.doc(`tasks/${taskId}`).get();
          if (!taskSnap.exists) return;
          const task = taskSnap.data() || {};

          const role = (note.role || "").toLowerCase();
          let tokens = [];
          let title  = "";
          if (role === "admin") {
            const ids = Array.isArray(task.assignedUserIds) && task.assignedUserIds.length
              ? task.assignedUserIds
              : task.assignedUserId
                ? [task.assignedUserId]
                : [];
            const tokenLists = await Promise.all(ids.map((uid) => tokensForUser(uid)));
            tokens = tokenLists.flat();
            title  = `Admin note · ${task.title || "Task"}`;
          } else {
            tokens = await tokensForAdmin(task.adminId);
            const who = note.authorName || task.assigneeName || "User";
            title  = `${who} · ${task.title || "Task"}`;
          }

          const body = message.length > 140 ? message.slice(0, 137) + "…" : message;

          await send(
            tokens,
            title,
            body,
            { type: "task", taskId, noteId },
          );
        } catch (e) {
          console.error("[notes.onCreated] error:", e);
        }
      });
    },
    (err) => console.error("[notes.onSnapshot] listener error:", err),
  );

/* ───── leave requests ────────────────────────────────────────────────── */

db.collection("leave_requests")
  .where("createdAt", ">=", startupTime)
  .onSnapshot(
    (snap) => {
      snap.docChanges().forEach(async (change) => {
        if (change.type !== "added") return;
        try {
          const l = change.doc.data();
          const tokens = await tokensForAdmin(l.adminId);
          await send(
            tokens,
            "New leave request",
            l.reason || "",
            { type: "leave", leaveId: change.doc.id },
          );
        } catch (e) {
          console.error("[leave.onCreated] error:", e);
        }
      });
    },
    (err) => console.error("[leave.onSnapshot] listener error:", err),
  );

const lastLeaveStatus = new Map();
db.collection("leave_requests").onSnapshot(
  (snap) => {
    snap.docChanges().forEach(async (change) => {
      if (change.type !== "modified") {
        if (change.doc.exists) {
          lastLeaveStatus.set(change.doc.id, change.doc.data().status || "");
        }
        return;
      }
      try {
        const after  = change.doc.data();
        const before = lastLeaveStatus.get(change.doc.id);
        lastLeaveStatus.set(change.doc.id, after.status || "");

        if (before === undefined) return;
        if (before === after.status) return;

        const tokens = await tokensForUser(after.userId);
        await send(
          tokens,
          `Leave ${after.status}`,
          after.reason || "",
          { type: "leave", leaveId: change.doc.id },
        );
      } catch (e) {
        console.error("[leave.onUpdated] error:", e);
      }
    });
  },
  (err) => console.error("[leave.status onSnapshot] listener error:", err),
);

/* ───── monthly stock snapshot — cron at 00:05 IST on day 1 ───────────── */

cron.schedule(
  "5 0 1 * *",
  async () => {
    try {
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

      console.log(`[snapshotMonthlyStock] wrote ${writes} snapshot(s) for ${key}`);
    } catch (e) {
      console.error("[snapshotMonthlyStock] error:", e);
    }
  },
  { timezone: "Asia/Kolkata" },
);

/* ───── Boot complete ─────────────────────────────────────────────────── */

console.log("UniWatt notification server up.");
console.log("  startupTime =", startupTime.toDate().toISOString());
console.log("  listeners   = tasks · notes · leave_requests");
console.log("  cron        = snapshotMonthlyStock @ \"5 0 1 * *\" Asia/Kolkata");
