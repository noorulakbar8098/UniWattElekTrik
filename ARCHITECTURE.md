# UniWatt ElekTrik — System Architecture

A production-ready, scalable architecture for the **UniWatt ElekTrik** platform, consisting of a **Technician User App** (Android + iOS) and an **Admin Panel** (Web), powered by a **Kotlin Multiplatform (KMP) shared module** and a backend (Firebase or custom API).

This document is **architecture-only** — no implementation code, UI, or backend setup steps.

---

## 1. Overall System Architecture

```
                ┌─────────────────────────────────────────────────────────┐
                │                    UniWatt ElekTrik                     │
                └─────────────────────────────────────────────────────────┘

   ┌────────────────────────────┐                    ┌────────────────────────────┐
   │      TECHNICIAN APP        │                    │        ADMIN PANEL         │
   │   (Android + iOS via KMP)  │                    │    (Web – React/Compose    │
   │                            │                    │     for Web, or Next.js)   │
   │  ┌──────────────────────┐  │                    │  ┌──────────────────────┐  │
   │  │  Presentation (MVVM) │  │                    │  │   Presentation (MVVM)│  │
   │  ├──────────────────────┤  │                    │  ├──────────────────────┤  │
   │  │       Domain         │◄─┼── shared KMP ──────┼─►│        Domain        │  │
   │  ├──────────────────────┤  │   (commonMain)     │  ├──────────────────────┤  │
   │  │         Data         │  │                    │  │         Data         │  │
   │  └──────────┬───────────┘  │                    │  └──────────┬───────────┘  │
   └─────────────┼──────────────┘                    └─────────────┼──────────────┘
                 │                HTTPS / WebSocket               │
                 │                                                │
                 ▼                                                ▼
        ┌──────────────────────────────────────────────────────────────────┐
        │                     BACKEND  (API Gateway)                       │
        │      Firebase  OR  Node.js / Spring Boot REST + WebSocket        │
        ├──────────────────────────────────────────────────────────────────┤
        │ Auth Service │ User Svc │ Task Svc │ Attendance │ Logs │ Support │
        ├──────────────────────────────────────────────────────────────────┤
        │         Firestore / PostgreSQL   │   Cloud Storage (files)       │
        │                    FCM (Push)    │   Analytics / Crashlytics     │
        └──────────────────────────────────────────────────────────────────┘
```

**Key idea:** business rules live in the KMP **shared module**; both apps consume it. The backend is the single source of truth.

---

## 2. KMP Shared Module Design

### Source Set Responsibilities

| Source Set | Contains | Examples |
|------------|----------|----------|
| **commonMain** | Pure Kotlin: domain models, use cases, repository **interfaces**, ViewModels, DTOs, mappers, networking (Ktor), serialization, validation | `Task`, `AuthRepository`, `CheckInUseCase`, `TaskViewModel` |
| **androidMain** | Android `actual` implementations: `Context`, FusedLocationProvider, DataStore, WorkManager, FCM integration | `AndroidLocationProvider`, `AndroidSecureStorage` |
| **iosMain** | iOS `actual` implementations using Apple APIs | `CLLocationManager` wrapper, `KeychainStorage` |
| **jsMain / wasmJsMain** *(optional – if Admin Panel uses Compose for Web / KMP Web)* | Browser-specific `actual`s: `fetch`, `localStorage` | `WebSecureStorage` |
| **commonTest** | Multiplatform unit tests for use cases/ViewModels | `TaskViewModelTest` |

### Separation of Concerns

- **`commonMain` is framework-agnostic** — no Android/iOS imports.
- Platform-specific concerns (location, storage, notifications, biometrics) are exposed via `expect`/`actual` or injected interfaces.
- **ViewModels live in `commonMain`** (using `kotlinx.coroutines` + `StateFlow`) and are reused on Android (Compose) and iOS (SwiftUI via `SKIE` or manual bridging).

---

## 3. Project Structure

```
UniWattElekTrik/
├── shared/                         # KMP shared module (core of the system)
│   └── src/
│       ├── commonMain/kotlin/com/uniwatt/
│       │   ├── core/               # DI, Result, Dispatchers, Logger, Network client
│       │   ├── feature/
│       │   │   ├── auth/           # data / domain / presentation
│       │   │   ├── checkin/
│       │   │   ├── tasks/
│       │   │   ├── logs/
│       │   │   └── support/
│       │   └── platform/           # expect declarations (Location, Storage, Push)
│       ├── androidMain/            # actuals for Android
│       ├── iosMain/                # actuals for iOS
│       ├── jsMain/                 # (optional) actuals for Web admin
│       └── commonTest/
│
├── androidUserApp/                 # Android technician app (Compose UI)
│   └── src/main/kotlin/...         # screens, navigation, DI wiring, Activity
│
├── iosUserApp/                     # iOS technician app (SwiftUI)
│   ├── iosUserApp.xcodeproj
│   └── Sources/                    # SwiftUI views, bridging to shared framework
│
├── adminWebApp/                    # Admin Panel (Web)
│   └── src/                        # React/Next.js  OR  Compose for Web
│       └── features/...            # if KMP Web → consumes shared jsMain
│
├── backend/                        # (Optional – if custom API)
│   ├── src/                        # Node.js (NestJS) or Spring Boot
│   ├── modules/ (auth, users, tasks, attendance, logs, support)
│   └── infra/ (db migrations, docker, ci)
│
└── docs/                           # Architecture, ADRs, diagrams
```

**Module boundaries:**
- `shared` has **no dependency** on any app module.
- App modules depend **only on `shared`** and their platform SDKs.
- `backend` is independent and communicates via contract (OpenAPI / Firestore schema).

---

## 4. Layered Architecture (Clean Architecture)

Each feature inside `shared/commonMain` follows the same three layers:

```
┌───────────────────────────────────────────────────────────────┐
│ PRESENTATION       ViewModels, UiState, UiEvent, Navigation   │  ← Depends on Domain
├───────────────────────────────────────────────────────────────┤
│ DOMAIN             Entities, UseCases, Repository Interfaces  │  ← Pure Kotlin, no deps
├───────────────────────────────────────────────────────────────┤
│ DATA               Repository Impls, Remote/Local Sources,    │  ← Depends on Domain
│                    DTOs, Mappers                              │
└───────────────────────────────────────────────────────────────┘
            ▲                                         ▼
   Platform UI (Compose / SwiftUI / React)    Backend / DB / Device APIs
```

### Layer Responsibilities

| Layer | Responsibility | Rules |
|-------|----------------|-------|
| **Domain** | Business rules, use cases, entity models, repository contracts | No framework, no I/O, no Android/iOS imports |
| **Data** | Implements repositories; handles API calls (Ktor), caching (SQLDelight), mapping DTO↔Entity | Depends on Domain interfaces only |
| **Presentation** | Multiplatform ViewModels expose `StateFlow<UiState>`; handle `UiEvent`; orchestrate use cases | No direct data access |
| **Platform UI** | Compose / SwiftUI / React renders `UiState` and dispatches `UiEvent` | Thin; no business logic |

**Dependency rule:** Presentation → Domain ← Data.  Domain has **zero** outward dependencies.

---

## 5. Feature Module Breakdown

Each feature is self-contained under `shared/commonMain/kotlin/com/uniwatt/feature/<name>/`:

```
feature/<name>/
├── data/
│   ├── remote/   (ApiService, DTOs)
│   ├── local/    (DAO, entities)
│   └── repository/ (RepositoryImpl)
├── domain/
│   ├── model/
│   ├── repository/ (interfaces)
│   └── usecase/
└── presentation/
    ├── state/    (UiState, UiEvent)
    └── viewmodel/
```

### 5.1 Auth
- **Domain:** `User`, `AuthRepository`, `LoginUseCase`, `LogoutUseCase`, `ObserveSessionUseCase`
- **Data:** Firebase Auth / `/auth/login` endpoint; secure token storage via platform Keychain/Keystore
- **Presentation:** `LoginViewModel` (`StateFlow<LoginUiState>`)

### 5.2 Check-In / Check-Out
- **Domain:** `Attendance`, `GeoPoint`, `CheckInUseCase`, `CheckOutUseCase`
- **Platform:** `expect class LocationProvider` (FusedLocation on Android, CLLocation on iOS)
- **Data:** Offline queue (SQLDelight) → sync when online
- **Presentation:** `CheckInViewModel` — handles permissions state, current location, in/out status

### 5.3 Tasks
- **Domain:** `Task`, `TaskStatus { NOT_STARTED, IN_PROGRESS, COMPLETED }`, `GetAssignedTasksUseCase`, `UpdateTaskStatusUseCase`
- **Data:** Realtime updates via Firestore listener / WebSocket; local cache for offline read
- **Presentation:** `TaskListViewModel`, `TaskDetailViewModel`

### 5.4 Work Logs
- **Domain:** `WorkLog`, `Severity { LOW, MEDIUM, HIGH }`, `CreateWorkLogUseCase`, `GetLogsUseCase`
- **Data:** POST to backend; attachments via Cloud Storage pre-signed URL
- **Presentation:** `LogEntryViewModel`, `LogHistoryViewModel`

### 5.5 Support / Issue Reporting
- **Domain:** `SupportTicket`, `TicketStatus`, `CreateTicketUseCase`, `ObserveTicketsUseCase`
- **Data:** Bi-directional (technician creates, admin responds) via backend
- **Presentation:** `SupportViewModel`, `TicketThreadViewModel`

### 5.6 Dashboard (derived, not a new data source)
- Aggregates read-models from Tasks + Attendance + Logs use cases.

---

## 6. Data Flow (End-to-End)

### Admin assigns a task → Technician sees it → Admin monitors completion

```
 ┌────────────┐  create task    ┌────────────┐  persist    ┌────────────┐
 │  Admin UI  │────────────────►│  Backend   │────────────►│  Database  │
 └────────────┘                 │  (API)     │             └────────────┘
       ▲                        └─────┬──────┘
       │  live update                 │  push (FCM / WebSocket)
       │  (WebSocket/listener)        ▼
 ┌─────┴──────┐    pull/stream  ┌────────────────────┐
 │ Admin VM   │◄────────────────│ Technician Device  │
 │ (shared)   │                 │                    │
 └────────────┘                 │  1. FCM wakes app  │
                                │  2. TaskRepo sync  │
                                │  3. Room/SQLDelight│
                                │  4. StateFlow emit │
                                │  5. UI re-renders  │
                                └─────────┬──────────┘
                                          │ status updates (PATCH)
                                          ▼
                                  ┌────────────┐
                                  │  Backend   │──► updates admin live view
                                  └────────────┘
```

### Check-in flow (offline-first)

```
Tech taps "Check In"
   → CheckInViewModel.onEvent(CheckIn)
   → CheckInUseCase()
      → LocationProvider.current()     [platform actual]
      → AttendanceRepository.checkIn(point)
           → LocalDataSource.insert()  (source of truth for UI)
           → SyncWorker enqueue()
                → RemoteDataSource.push()
                → mark as synced
   → StateFlow emits Success
   → UI re-renders; Admin dashboard receives realtime update
```

---

## 7. Backend Architecture (High-Level)

Two recommended options — pick one per product stage:

### Option A — Firebase (fastest to production)

```
┌─────────────────────────── Firebase Project ────────────────────────────┐
│  Firebase Auth      → users, roles (custom claims: TECHNICIAN/ADMIN)    │
│  Firestore          → /users /tasks /attendance /logs /tickets          │
│  Cloud Storage      → task attachments, log photos                      │
│  Cloud Functions    → assignments, notifications, report generation     │
│  FCM                → push notifications to technicians                 │
│  Security Rules     → role-based doc access                             │
│  Crashlytics / GA   → observability                                     │
└─────────────────────────────────────────────────────────────────────────┘
```
- **Pros:** realtime out of the box, low ops.
- **Data strategy:** denormalize read models (`/dashboard/{adminId}`) updated by Cloud Functions.

### Option B — Custom API (Node.js NestJS / Spring Boot)

```
                    ┌──────────── API Gateway (HTTPS) ───────────┐
                    │        Auth (JWT + Refresh)                │
                    └────┬────────┬────────┬────────┬────────────┘
                         │        │        │        │
                    ┌────▼──┐ ┌───▼───┐ ┌──▼────┐ ┌─▼──────┐
                    │ Auth  │ │ Tasks │ │Attend.│ │Support │ ... micro-modules
                    └────┬──┘ └───┬───┘ └──┬────┘ └─┬──────┘
                         └────────┴────┬───┴────────┘
                                       ▼
                               ┌──────────────┐
                               │ PostgreSQL   │  (primary store)
                               │ Redis        │  (cache, pub/sub)
                               │ S3           │  (files)
                               │ WebSocket hub│  (live updates)
                               │ FCM/APNs     │  (push)
                               └──────────────┘
```
- **Data strategy:** CQRS-lite — write via REST, read via WebSocket/SSE for live panels.
- **Contract:** OpenAPI spec consumed by the KMP Ktor client (generated DTOs).

### Cross-cutting backend concerns
- **AuthN/Z:** JWT + role-based claims (`TECHNICIAN`, `ADMIN`, `SUPER_ADMIN`).
- **Observability:** structured logs, metrics (Prometheus/Grafana), tracing (OpenTelemetry).
- **CI/CD:** GitHub Actions → containerized deploy (Cloud Run / ECS).

---

## 8. State Management

### In the shared module (commonMain)
- **`StateFlow<UiState>`** — single immutable UI state per screen.
- **`SharedFlow<UiEffect>`** — one-shot events (navigation, snackbars).
- **`Channel` / `Flow`** for repository streams.
- **`CoroutineScope`** tied to platform lifecycle (Android `ViewModel`, iOS `@StateObject` wrapper).

```
UiEvent  ──►  ViewModel.onEvent()  ──►  UseCase  ──►  Repository
                        │
                        ▼
                  StateFlow<UiState>  ──►  Compose / SwiftUI / React re-render
                        │
                        └─►  SharedFlow<UiEffect>  ──►  Navigation / Toast
```

### Platform binding
| Platform | Binding |
|----------|---------|
| Android (Compose) | `collectAsStateWithLifecycle()` |
| iOS (SwiftUI) | `SKIE` Flow→AsyncSequence, or `ObservableObject` wrapper |
| Web (React) | `useSyncExternalStore` on the JS StateFlow; or Compose for Web `collectAsState` |

**Why not LiveData?** LiveData is Android-only. `StateFlow` is multiplatform and integrates with coroutines — the 2025+ standard.

---

## 9. Scalability Strategy

### Codebase scalability
- **Feature-based modularization** — each feature is a self-contained package, promotable to its own Gradle module when it grows.
- **Clean Architecture** ensures features can be added/removed without touching unrelated code.
- **Version catalog** (`libs.versions.toml`) centralizes dependencies.
- **Contract-first** DTOs (OpenAPI) keep backend ↔ client in sync as teams scale.

### Runtime scalability
- **Stateless backend services** behind a load balancer; horizontal scaling.
- **Database:** read replicas + connection pooling; partition `attendance`/`logs` by `technicianId` & month.
- **Realtime:** WebSocket hub with sticky sessions or Firebase/Firestore's built-in fan-out.
- **Caching:** Redis for hot reads (e.g., active technicians).
- **Push:** FCM topic per region/team to avoid per-device fan-out costs.

### Delivery scalability
- **CI/CD pipelines** per module (shared, android, ios, web, backend).
- **Feature flags** (e.g., Firebase Remote Config) gate rollouts.
- **Observability** → SLOs per feature; alerts via Grafana/Sentry.

### Team scalability
- Clear module ownership (CODEOWNERS).
- ADRs in `/docs/adr/` record architectural decisions.

---

## 10. Platform Responsibilities — Shared vs Platform-Specific

| Concern | Shared (KMP) | Platform-specific |
|---------|--------------|-------------------|
| Domain models & rules | ✅ | — |
| Use cases | ✅ | — |
| ViewModels + UiState | ✅ | — |
| Networking (Ktor client) | ✅ | — |
| Serialization (kotlinx.serialization) | ✅ | — |
| Local DB (SQLDelight) | ✅ (queries) | Driver impl per platform |
| DI (Koin) | ✅ (modules) | Platform module composition |
| Logging | ✅ (interface) | `Logcat` / `os_log` / `console` |
| Secure storage | `expect` interface | Keystore / Keychain / IndexedDB |
| Location | `expect` interface | FusedLocation / CoreLocation / Geolocation API |
| Push notifications | `expect` interface | FCM / APNs / Web Push |
| Background sync | `expect` scheduler | WorkManager / BGTaskScheduler / Service Worker |
| Biometrics (future) | `expect` interface | BiometricPrompt / LocalAuthentication |
| UI (screens, animations) | ❌ | Compose / SwiftUI / React |
| Navigation host | ❌ (contracts only) | Platform nav (NavHost / NavigationStack / React Router) |
| Analytics | Event definitions | SDK wiring |

**Rule of thumb:** *If it touches the device, it's platform-specific; if it's logic or data, it's shared.*

---

## Summary

- **One KMP shared module** powers Android, iOS, and (optionally) the Web admin — maximizing reuse.
- **Clean Architecture + MVVM** keeps the system testable, modular, and team-friendly.
- **Feature-first structure** scales from 5 to 50 features without refactors.
- **Backend** (Firebase or custom) is the single source of truth; clients are offline-first with sync.
- **StateFlow-based** reactive state is the 2025+ multiplatform standard.
- **Platform-specific concerns** are isolated behind `expect`/`actual` so the core remains portable.

This architecture is ready for a real-world production deployment of UniWatt ElekTrik, supporting both the **Technician App** and the **Admin Panel**, with a clear path for scaling users, features, and teams.

