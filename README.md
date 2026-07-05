# UniWatt ElekTrik

**UniWatt ElekTrik** is a production-level mobile solution for Technical Workforce Management and Inventory tracking. It streamlines field operations by bridging the gap between administrators and on-site engineers with real-time data and a premium UI.

## 🚀 Key Features

### 🛠 Administrative Suite
- **Operations Dashboard**: Live KPI tracking, performance charts (7-day trends), and system-wide activity pulse.
- **Team Management**: Complete HR onboarding flow with role-based access control, shift management, and profile customization.
- **Smart Inventory**: A robust Master Spare List categorized by Department and Equipment, featuring specific technical specs for Cables and Components.
- **Task Orchestration**: Advanced task creation with SLA monitoring, location geofencing, and multi-assignee logic.

### 👷 Field Engineer Suite
- **Daily Workflow**: At-a-glance view of assigned tasks, priorities, and site locations.
- **Attendance**: Real-time check-in/out with location validation and shift status.
- **Work Completion**: Interactive task execution with work logs and status reporting.

## 🛠 Tech Stack
- **Framework**: [Kotlin Multiplatform (KMP)](https://kotlinlang.org/docs/multiplatform.html)
- **UI**: [Compose Multiplatform](https://www.jetbrains.com/lp/compose-multiplatform/) (Android & iOS)
- **Database**: Firebase Cloud Firestore (Real-time listeners)
- **Auth**: Firebase Authentication (Multi-role support)
- **Media**: Cloud Storage for photo uploads and attachments
- **Architecture**: Clean Architecture with MVVM (Shared business logic)

## 🎨 Design System
The app utilizes a **Premium Fintech-style UI** characterized by:
- Diagonal gradient headers with atmospheric vector rings.
- Soft-shadowed "Glassmorphism" cards.
- Staggered entrance animations for lists.
- A high-contrast typography hierarchy for readability in field conditions.

## 📂 Project Structure
- `commonMain`: Shared business logic, ViewModels, and UI Components.
- `androidMain`: Platform-specific Firestore, Storage, and System Bar implementations.
- `iosMain`: Native iOS bridges and UIViewController hosting.
- `feature/inventory`: Nested module for Departments, Equipment, and Spares management.

## 🏁 Getting Started

### Build and Run Android Application
To build and run the development version of the Android app, use the run configuration from the run widget in your IDE’s toolbar or build it directly from the terminal:
- on macOS/Linux: `./gradlew :composeApp:assembleDebug`
- on Windows: `.\gradlew.bat :composeApp:assembleDebug`

### Build and Run iOS Application
To build and run the development version of the iOS app, use the run configuration from the run widget in your IDE’s toolbar or open the [/iosApp](./iosApp) directory in Xcode and run it from there.

---
*Developed for UniWatt ElekTrik Operations — Empowering the Field.*
