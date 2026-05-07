/**
 * UniWattElekTrik — Firestore 3-year Dummy Data Seeder
 *
 * SETUP:
 *  1. Go to Firebase Console → Project Settings → Service Accounts
 *  2. Click "Generate new private key" → save as serviceAccountKey.json in this folder
 *  3. Run: node seed.js
 *
 * WHAT IT SEEDS (for test@gmail.com / adminId auto-detected):
 *  • 8 Employees  (various roles, departments, shifts)
 *  • 12 Spare items (cables, components, with varying stock levels)
 *  • ~250 Tasks    (Todo / InProgress / Done) spread across 3 years
 *  • ~800 Attendance records (check-in + check-out per working day, 3 years)
 *  • Leave requests (various statuses)
 *  • Departments + Equipment
 */

const admin = require('firebase-admin');
const path  = require('path');
const fs    = require('fs');

// ── Load service account ────────────────────────────────────────────────────
const keyPath = path.join(__dirname, 'uniwattelectric-firebase-adminsdk-fbsvc-1c7d8831b6.json');
if (!fs.existsSync(keyPath)) {
  console.error('\n❌  serviceAccountKey.json not found in scripts/');
  console.error('   Download it from Firebase Console → Project Settings → Service Accounts\n');
  process.exit(1);
}

admin.initializeApp({
  credential: admin.credential.cert(require(keyPath)),
});

const db = admin.firestore();

// ── Config ───────────────────────────────────────────────────────────────────
const ADMIN_EMAIL = 'test@gmail.com';
const PROJECT_ID  = 'uniwattelectric';

// ── Helpers ──────────────────────────────────────────────────────────────────
const uid   = () => db.collection('_').doc().id;
const now   = Date.now();
const DAY   = 86_400_000;
const HOUR  = 3_600_000;

/** ms from N days ago */
const daysAgo = (n) => now - n * DAY;

/** Random int in [min, max] */
const rnd = (min, max) => Math.floor(Math.random() * (max - min + 1)) + min;

/** Pick random element */
const pick = (arr) => arr[rnd(0, arr.length - 1)];

/** Random past timestamp within 3 years, on a weekday */
function randWorkdayMs(maxDaysBack = 1095) {
  let d;
  do {
    d = new Date(daysAgo(rnd(1, maxDaysBack)));
  } while (d.getDay() === 0 || d.getDay() === 6); // skip Sun/Sat
  return d.getTime();
}

function setTime(ms, h, m) {
  const d = new Date(ms);
  d.setHours(h, m, 0, 0);
  return d.getTime();
}

// ── Master data ───────────────────────────────────────────────────────────────
const EMPLOYEES = [
  { name: 'Ali Hassan',       role: 'Senior Technician',  dept: 'Electrical',   shift: 'Shift1', zone: 'Zone A', gender: 'Male',   phone: '+60111234567' },
  { name: 'Priya Sharma',     role: 'Field Engineer',     dept: 'Mechanical',   shift: 'Shift1', zone: 'Zone B', gender: 'Female', phone: '+60119876543' },
  { name: 'Ahmad Fadzil',     role: 'Technician',         dept: 'Electrical',   shift: 'Shift2', zone: 'Zone A', gender: 'Male',   phone: '+60123456789' },
  { name: 'Nurul Ain',        role: 'Inspector',          dept: 'QA',           shift: 'Shift1', zone: 'Zone C', gender: 'Female', phone: '+60165554321' },
  { name: 'Rajesh Kumar',     role: 'Maintenance Lead',   dept: 'Mechanical',   shift: 'Shift1', zone: 'Zone B', gender: 'Male',   phone: '+60172223344' },
  { name: 'Siti Fatimah',     role: 'Safety Officer',     dept: 'HSE',          shift: 'Shift1', zone: 'Zone D', gender: 'Female', phone: '+60187778899' },
  { name: 'David Lim',        role: 'Electrician',        dept: 'Electrical',   shift: 'Shift2', zone: 'Zone A', gender: 'Male',   phone: '+60196665544' },
  { name: 'Kavitha Nair',     role: 'Assistant Engineer', dept: 'Mechanical',   shift: 'Shift1', zone: 'Zone C', gender: 'Female', phone: '+60112223344' },
];

const SPARE_ITEMS = [
  { category: 'Cable', name: '2.5mm XLPE Cable',    make: 'Prysmian',  size: '2.5mm', core: '3C',  unit: 'metre',  price: 4.50,  stockQty: 250, hsn: '8544' },
  { category: 'Cable', name: '6mm XLPE Cable',      make: 'Nexans',    size: '6mm',   core: '4C',  unit: 'metre',  price: 9.80,  stockQty: 120, hsn: '8544' },
  { category: 'Cable', name: '16mm Armoured Cable', make: 'Prysmian',  size: '16mm',  core: '4C',  unit: 'metre',  price: 22.50, stockQty: 80,  hsn: '8544' },
  { category: 'Cable', name: '95mm HV Cable',       make: 'Belden',    size: '95mm',  core: '1C',  unit: 'metre',  price: 65.00, stockQty: 0,   hsn: '8544' }, // out of stock
  { category: 'Spare Component', name: 'MCB 20A',           make: 'Schneider', unit: 'pcs',   price: 35.00, stockQty: 45,  hsn: '8536', noOfPoles: '2P', currentRating: '20A' },
  { category: 'Spare Component', name: 'MCB 63A',           make: 'ABB',       unit: 'pcs',   price: 72.00, stockQty: 18,  hsn: '8536', noOfPoles: '3P', currentRating: '63A' },
  { category: 'Spare Component', name: 'RCCB 40A 30mA',     make: 'Schneider', unit: 'pcs',   price: 95.00, stockQty: 8,   hsn: '8536', noOfPoles: '2P', currentRating: '40A' },
  { category: 'Spare Component', name: 'Contactor 25A',     make: 'Siemens',   unit: 'pcs',   price: 145.00,stockQty: 12,  hsn: '8536', currentRating: '25A' },
  { category: 'Spare Component', name: 'Terminal Block',    make: 'Phoenix',   unit: 'pcs',   price: 3.50,  stockQty: 200, hsn: '8538' },
  { category: 'Spare Component', name: 'Fuse 32A',          make: 'Hager',     unit: 'pcs',   price: 12.00, stockQty: 5,   hsn: '8536', currentRating: '32A' }, // low stock
  { category: 'Spare Component', name: 'Cable Lug 35mm',    make: 'Cembre',    unit: 'pcs',   price: 2.80,  stockQty: 150, hsn: '8538' },
  { category: 'Spare Component', name: 'Isolator 100A',     make: 'ABB',       unit: 'pcs',   price: 220.00,stockQty: 3,   hsn: '8537', currentRating: '100A' }, // low stock
];

const TASK_TITLES = [
  'Replace faulty MCB in DB-A3',
  'Inspect HV cable run in Block B',
  'Thermographic survey — MV panel',
  'Install new sub-DB in cafeteria',
  'Cable tray extension — Level 3',
  'Lighting retrofit — Warehouse 2',
  'Generator maintenance service',
  'UPS battery replacement',
  'Earth pit resistance test',
  'Power quality audit — Building 5',
  'Conduit installation — Server Room',
  'Panel labelling and documentation',
  'RCCB trip investigation',
  'Surge protection device check',
  'Emergency lighting inspection',
  'Motor winding resistance test',
  'ATS test and commissioning',
  'Distribution transformer oil test',
  'Cable joint installation — Zone C',
  'Fire alarm panel integration',
  'SCADA data logger setup',
  'Energy meter calibration',
  'Load balancing — Main DB',
  'Capacitor bank maintenance',
  'Grounding system upgrade',
];

const LOCATIONS = [
  'Main Substation', 'Block A — Level 2', 'Warehouse 1', 'Server Room',
  'Generator House', 'Block B — Rooftop', 'Cafeteria DB Room',
  'Car Park Level 1', 'Plant Room 3', 'Control Room',
];

const PRIORITIES  = ['Success', 'Medium', 'Danger'];
const TASK_TIMES  = ['08:00', '09:00', '10:30', '13:00', '14:30', '15:00', '16:00'];
const DEPARTMENTS = ['Electrical', 'Mechanical', 'QA', 'HSE'];

// ── Main seed function ────────────────────────────────────────────────────────
async function seed() {
  console.log('\n🌱  UniWattElekTrik — Firestore Seeder');
  console.log('   Project :', PROJECT_ID);
  console.log('   Admin   :', ADMIN_EMAIL, '\n');

  // 1. Resolve adminId from admin_ids or admins collection
  let adminId = null;
  const adminsSnap = await db.collection('admins')
    .where('email', '==', ADMIN_EMAIL).limit(1).get();
  if (!adminsSnap.empty) {
    adminId = adminsSnap.docs[0].id;
    console.log('✅  Found admin uid:', adminId);
  } else {
    // fallback — create minimal admin doc
    adminId = uid();
    await db.collection('admins').doc(adminId).set({
      email: ADMIN_EMAIL, displayName: 'Test Admin', createdAt: now,
    });
    console.log('✅  Created admin doc:', adminId);
  }

  // ── 2. Departments ─────────────────────────────────────────────────────────
  console.log('\n📂  Seeding departments...');
  const deptIds = {};
  for (const deptName of DEPARTMENTS) {
    const id = uid();
    deptIds[deptName] = id;
    await db.collection('departments').doc(id).set({
      adminId, name: deptName, createdAt: now, updatedAt: now,
    });
  }
  console.log(`   ✔  ${DEPARTMENTS.length} departments`);

  // ── 3. Employees ───────────────────────────────────────────────────────────
  console.log('\n👷  Seeding employees...');
  const empIds = [];
  const empNames = [];
  for (const e of EMPLOYEES) {
    const id = uid();
    empIds.push(id);
    empNames.push(e.name);
    const joiningMs = daysAgo(rnd(400, 1095));
    await db.collection('users').doc(id).set({
      adminId,
      name: e.name, role: e.role, phone: e.phone,
      email: `${e.name.toLowerCase().replace(/ /g, '.')}@uniwatt.com`,
      gender: e.gender, zone: e.zone, shift: e.shift,
      department: e.dept, departmentId: deptIds[e.dept] || '',
      employmentType: pick(['Fulltime', 'Contract']),
      status: 'Active',
      joiningDateMs: joiningMs,
      salary: rnd(3500, 8500),
      tasksOpen: 0,
      permission: pick(['Field', 'Super', 'Viewer']),
      photoUrl: '',
      emergencyName: 'Family Member', emergencyRelation: 'Spouse', emergencyPhone: '+601XXXXXXXX',
      createdAt: joiningMs, updatedAt: now,
    });
    // employee_map for auth lookup
    await db.collection('employee_map').doc(id).set({ adminId, userId: id });
  }
  console.log(`   ✔  ${empIds.length} employees`);

  // ── 4. Spare items ─────────────────────────────────────────────────────────
  console.log('\n📦  Seeding spare items...');
  const spareIds = [];
  for (const s of SPARE_ITEMS) {
    const id = uid();
    spareIds.push(id);
    await db.collection('spare_items').doc(id).set({
      adminId, id,
      category: s.category, name: s.name, make: s.make || '',
      size: s.size || '', core: s.core || '',
      currentRating: s.currentRating || '', noOfPoles: s.noOfPoles || '',
      unit: s.unit || 'pcs', price: s.price,
      stockQty: s.stockQty, hsn: s.hsn,
      vendorName1: 'ElectroSupply Sdn Bhd', vendorGst1: 'W12-3456',
      vendorContact1: '+603 7777 8888', vendorAddress1: 'Petaling Jaya, Selangor',
      vendorName2: '', vendorGst2: '', vendorContact2: '', vendorAddress2: '',
      createdAt: daysAgo(rnd(300, 1000)), updatedAt: now,
    });
  }
  console.log(`   ✔  ${spareIds.length} spare items`);

  // ── 5. Tasks — 3 years, all statuses ──────────────────────────────────────
  console.log('\n📋  Seeding tasks...');
  const taskBatches = [];
  let currentBatch = db.batch();
  let batchCount = 0;
  let taskTotal = 0;

  const flush = async () => {
    if (batchCount > 0) { await currentBatch.commit(); currentBatch = db.batch(); batchCount = 0; }
  };

  // Done tasks — spread across 3 years (~160)
  for (let i = 0; i < 160; i++) {
    const taskId   = uid();
    const empIdx   = i % empIds.length;
    const createdAt = randWorkdayMs(1095);
    const dueDate  = createdAt + rnd(1, 7) * DAY;
    const completedAt = dueDate + (Math.random() < 0.7 ? -rnd(0, 12) * HOUR : rnd(1, 24) * HOUR);
    const startMs  = completedAt - rnd(1, 4) * HOUR;

    currentBatch.set(db.collection('tasks').doc(taskId), {
      adminId, id: taskId,
      userId: empIds[empIdx],
      assigneeName: empNames[empIdx],
      title: TASK_TITLES[i % TASK_TITLES.length],
      description: 'Routine maintenance and inspection as per work order.',
      location: LOCATIONS[i % LOCATIONS.length],
      time: pick(TASK_TIMES),
      day: new Date(createdAt).toLocaleDateString('en-US', { weekday: 'long' }),
      priority: pick(PRIORITIES),
      status: 'Done',
      departmentId: deptIds[EMPLOYEES[empIdx].dept] || '',
      departmentName: EMPLOYEES[empIdx].dept,
      scheduledDateMs: createdAt,
      dueDate,
      createdAtMs: createdAt,
      acceptedAt: createdAt + rnd(30, 120) * 60000,
      completedAt,
      updatedAt: completedAt,
      signoffDescription: 'Work completed successfully. System tested and verified operational.',
      downtimeMinutes: rnd(15, 180),
      rca: pick(['Component wear', 'Overload', 'Environmental', 'Improper installation', 'Age-related']),
      totalWorkDurationMs: completedAt - startMs,
      checklist: [
        { text: 'Isolate power supply', done: true },
        { text: 'Inspect and test', done: true },
        { text: 'Replace faulty component', done: true },
        { text: 'Restore and verify', done: true },
      ],
      ownerAdminName: 'Test Admin',
    });
    batchCount++;
    taskTotal++;
    if (batchCount >= 490) await flush();
  }

  // InProgress tasks (~40)
  for (let i = 0; i < 40; i++) {
    const taskId = uid();
    const empIdx = i % empIds.length;
    const createdAt = daysAgo(rnd(1, 30));
    const dueDate = now + rnd(1, 5) * DAY;

    currentBatch.set(db.collection('tasks').doc(taskId), {
      adminId, id: taskId,
      userId: empIds[empIdx],
      assigneeName: empNames[empIdx],
      title: TASK_TITLES[(i + 5) % TASK_TITLES.length],
      description: 'Work order in progress — technician on site.',
      location: LOCATIONS[(i + 3) % LOCATIONS.length],
      time: pick(TASK_TIMES),
      day: new Date(createdAt).toLocaleDateString('en-US', { weekday: 'long' }),
      priority: pick(PRIORITIES),
      status: 'InProgress',
      departmentId: deptIds[EMPLOYEES[empIdx].dept] || '',
      departmentName: EMPLOYEES[empIdx].dept,
      scheduledDateMs: createdAt,
      dueDate,
      createdAtMs: createdAt,
      acceptedAt: createdAt + rnd(10, 60) * 60000,
      updatedAt: now,
      checklist: [
        { text: 'Isolate power supply', done: true },
        { text: 'Inspect and test', done: false },
        { text: 'Replace faulty component', done: false },
        { text: 'Restore and verify', done: false },
      ],
      ownerAdminName: 'Test Admin',
    });
    batchCount++;
    taskTotal++;
    if (batchCount >= 490) await flush();
  }

  // Todo tasks (~50)
  for (let i = 0; i < 50; i++) {
    const taskId = uid();
    const empIdx = i % empIds.length;
    const createdAt = daysAgo(rnd(0, 14));
    const dueDate = now + rnd(1, 10) * DAY;

    currentBatch.set(db.collection('tasks').doc(taskId), {
      adminId, id: taskId,
      userId: i % 4 === 0 ? null : empIds[empIdx],
      assigneeName: i % 4 === 0 ? '' : empNames[empIdx],
      title: TASK_TITLES[(i + 10) % TASK_TITLES.length],
      description: 'Scheduled maintenance task awaiting technician assignment.',
      location: LOCATIONS[(i + 6) % LOCATIONS.length],
      time: pick(TASK_TIMES),
      day: new Date(createdAt).toLocaleDateString('en-US', { weekday: 'long' }),
      priority: i % 5 === 0 ? 'Danger' : pick(['Success', 'Medium']),
      status: 'Todo',
      departmentId: deptIds[DEPARTMENTS[i % DEPARTMENTS.length]] || '',
      departmentName: DEPARTMENTS[i % DEPARTMENTS.length],
      scheduledDateMs: createdAt,
      dueDate,
      createdAtMs: createdAt,
      updatedAt: createdAt,
      checklist: [
        { text: 'Review work order', done: false },
        { text: 'Prepare tools and materials', done: false },
        { text: 'Execute and document', done: false },
      ],
      ownerAdminName: 'Test Admin',
    });
    batchCount++;
    taskTotal++;
    if (batchCount >= 490) await flush();
  }

  await flush();
  console.log(`   ✔  ${taskTotal} tasks (160 Done + 40 InProgress + 50 Todo)`);

  // ── 6. Attendance — 3 years of working days ────────────────────────────────
  console.log('\n🕐  Seeding attendance (3 years × 8 employees)...');
  let attTotal = 0;
  currentBatch = db.batch();
  batchCount = 0;

  // Go through each day in the last 3 years
  for (let daysBack = 1095; daysBack >= 1; daysBack -= 1) {
    const dayMs = daysAgo(daysBack);
    const dayDate = new Date(dayMs);
    const dow = dayDate.getDay(); // 0=Sun, 6=Sat
    if (dow === 0 || dow === 6) continue; // skip weekends

    for (let eIdx = 0; eIdx < empIds.length; eIdx++) {
      // ~85% attendance rate
      if (Math.random() > 0.85) continue;

      const emp = EMPLOYEES[eIdx];
      const shift1 = emp.shift === 'Shift1';

      // Check-in time: Shift1 = 8:45–9:30, Shift2 = 13:00–13:45
      const checkInBase = shift1 ? setTime(dayMs, 9, 0) : setTime(dayMs, 13, 0);
      const checkInMs   = checkInBase + rnd(-15, 45) * 60000;
      const isLate      = shift1
        ? checkInMs > setTime(dayMs, 9, 15)
        : checkInMs > setTime(dayMs, 13, 15);

      // Check-out time: Shift1 = 17:30–19:00, Shift2 = 22:00–23:30
      const checkOutBase = shift1 ? setTime(dayMs, 18, 0) : setTime(dayMs, 22, 30);
      // ~10% chance forgot to check out
      const hasCheckout = Math.random() > 0.10;
      const checkOutMs  = hasCheckout ? checkOutBase + rnd(-30, 60) * 60000 : null;

      const attId = uid();
      const rec = {
        adminId, id: attId,
        userId: empIds[eIdx],
        dateMs: setTime(dayMs, 0, 0),
        checkInMs,
        checkInLat: 3.1390 + (Math.random() - 0.5) * 0.01,
        checkInLng: 101.6869 + (Math.random() - 0.5) * 0.01,
        checkInStatus: isLate ? 'LATE' : 'ON_TIME',
        status: hasCheckout ? 'COMPLETED' : 'CHECKED_IN',
      };
      if (checkOutMs) {
        rec.checkOutMs  = checkOutMs;
        rec.checkOutLat = 3.1390 + (Math.random() - 0.5) * 0.01;
        rec.checkOutLng = 101.6869 + (Math.random() - 0.5) * 0.01;
      }
      currentBatch.set(db.collection('attendance_logs').doc(attId), rec);
      batchCount++;
      attTotal++;

      if (batchCount >= 490) { await currentBatch.commit(); currentBatch = db.batch(); batchCount = 0; }
    }
  }
  if (batchCount > 0) await currentBatch.commit();
  console.log(`   ✔  ${attTotal} attendance records`);

  // ── 7. Leave requests ──────────────────────────────────────────────────────
  console.log('\n🏖  Seeding leave requests...');
  const leaveStatuses = ['pending', 'approved', 'rejected', 'approved', 'approved'];
  let leaveTotal = 0;
  for (let i = 0; i < 30; i++) {
    const eIdx = i % empIds.length;
    const fromMs = daysAgo(rnd(10, 700));
    const days   = rnd(1, 5);
    const toMs   = fromMs + days * DAY;
    const status = leaveStatuses[i % leaveStatuses.length];
    const leaveId = uid();
    await db.collection('leave_requests').doc(leaveId).set({
      adminId, id: leaveId,
      userId: empIds[eIdx],
      employeeName: empNames[eIdx],
      department: EMPLOYEES[eIdx].dept,
      leaveType: pick(['Casual', 'Sick', 'Earned']),
      fromDateMs: fromMs, toDateMs: toMs, totalDays: days,
      reason: pick([
        'Family emergency', 'Medical appointment', 'Personal matters',
        'Annual leave', 'Child sick', 'Home renovation',
      ]),
      status,
      rejectionReason: status === 'rejected' ? 'Insufficient leave balance' : '',
      createdAtMs: fromMs - rnd(1, 5) * DAY,
      updatedAtMs: status !== 'pending' ? fromMs : null,
    });
    leaveTotal++;
  }
  console.log(`   ✔  ${leaveTotal} leave requests`);

  // ── 8. Notifications ──────────────────────────────────────────────────────
  console.log('\n🔔  Seeding notifications...');
  let notifTotal = 0;
  const notifBatch = db.batch();
  const notifTypes = ['task_assigned', 'task_updated', 'check_in', 'leave_approved', 'alert'];
  for (let i = 0; i < 60; i++) {
    const eIdx   = i % empIds.length;
    const notifId = uid();
    const type   = notifTypes[i % notifTypes.length];
    notifBatch.set(db.collection('notifications').doc(notifId), {
      adminId, id: notifId,
      userId: empIds[eIdx],
      title: type === 'task_assigned' ? 'New task assigned' : type === 'alert' ? 'Urgent: Overdue task' : 'Update',
      body: `Notification for ${empNames[eIdx]}`,
      type, isRead: i % 3 !== 0,
      createdAt: daysAgo(rnd(1, 60)),
    });
    notifTotal++;
  }
  await notifBatch.commit();
  console.log(`   ✔  ${notifTotal} notifications`);

  // ── 9. Inventory transactions ─────────────────────────────────────────────
  console.log('\n🔧  Seeding inventory transactions...');
  let txnTotal = 0;
  const txnBatch = db.batch();
  for (let i = 0; i < 80; i++) {
    const txnId  = uid();
    const eIdx   = i % empIds.length;
    const sIdx   = i % spareIds.length;
    txnBatch.set(db.collection('inventory_transactions').doc(txnId), {
      adminId, id: txnId,
      userId: empIds[eIdx],
      itemId: spareIds[sIdx],
      itemName: SPARE_ITEMS[sIdx].name,
      type: pick(['issue', 'return', 'restock']),
      quantity: rnd(1, 10),
      createdAt: daysAgo(rnd(1, 600)),
    });
    txnTotal++;
  }
  await txnBatch.commit();
  console.log(`   ✔  ${txnTotal} inventory transactions`);

  // ── Summary ────────────────────────────────────────────────────────────────
  console.log('\n✅  Seed complete!');
  console.log('───────────────────────────────────');
  console.log(`   Admin ID        : ${adminId}`);
  console.log(`   Employees       : ${empIds.length}`);
  console.log(`   Spare items     : ${SPARE_ITEMS.length}`);
  console.log(`   Tasks           : ${taskTotal}`);
  console.log(`   Attendance logs : ${attTotal}`);
  console.log(`   Leave requests  : ${leaveTotal}`);
  console.log(`   Notifications   : ${notifTotal}`);
  console.log(`   Inv. txn        : ${txnTotal}`);
  console.log('───────────────────────────────────\n');
  process.exit(0);
}

seed().catch((err) => {
  console.error('\n💥  Seed failed:', err.message);
  process.exit(1);
});

