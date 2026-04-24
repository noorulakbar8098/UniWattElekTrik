# UniWatt ElekTrik — Kotlin Multiplatform (KMP) Setup Guide

A step-by-step guide to initialize the **UniWatt ElekTrik** KMP project targeting **Android** and **iOS** (Web optional, deferred). This guide focuses **only on project setup and configuration**, with a Clean Architecture-ready structure, Gradle Kotlin DSL, and best practices for **2025+ KMP development**.

---

## 1. Environment Setup

### Required Tools

| Tool | Recommended Version (2025+) | Purpose |
|------|------------------------------|---------|
| **JDK** | 17 (or 21) | Required for Gradle & Kotlin compilation |
| **Android Studio** | Ladybug (2024.2) or newer | Primary IDE for KMP |
| **Kotlin Plugin** | 2.0.21+ | Ships with Android Studio |
| **Kotlin Multiplatform Plugin** | Latest (bundled) | Enables KMP module wizard |
| **Gradle** | 8.9+ (via wrapper) | Build system |
| **Xcode** | 15.4+ | Required for iOS build/run (macOS only) |
| **CocoaPods** (optional) | 1.15+ | Only if using `cocoapods{}` integration |
| **Kotlin Multiplatform Mobile (KMM)** plugin | Built into AS | Run iOS from Android Studio |

### Installation Checklist

1. Install **JDK 17** — `brew install openjdk@17` and set `JAVA_HOME`.
2. Install **Android Studio** (latest stable).
3. Open **Settings → Plugins** and ensure:
   - ✅ Kotlin Multiplatform
   - ✅ Kotlin
   - ✅ Android
4. Install **Xcode** from the Mac App Store; run `sudo xcode-select --install` and `sudo xcodebuild -license accept`.
5. (Optional) `brew install cocoapods`.

Verify:
```bash
java -version
kotlin -version
xcodebuild -version
./gradlew --version
```

---

## 2. Project Creation

### Option A — Android Studio Wizard (Recommended)

1. **File → New → New Project → Kotlin Multiplatform App**.
2. Fill in:
   - **Name:** `UniWatt ElekTrik`
   - **Package:** `com.example.uniwattelektrik`
   - **Application ID:** `com.example.uniwattelektrik`
3. **Targets:** select
   - ✅ Android
   - ✅ iOS (choose *"Regular framework"* — avoid CocoaPods unless needed)
   - ⬜ Desktop / Web (skip for now)
4. **Share UI with Compose Multiplatform:** ✅ (optional; enables shared Compose).
5. Click **Finish** → wait for Gradle sync.

### Option B — Kotlin Multiplatform Wizard Web

Use https://kmp.jetbrains.com to generate the template, then open the unzipped folder in Android Studio.

---

## 3. Project Structure

After generation, your root looks like:

```
UniWattElekTrik/
├── build.gradle.kts              # Root build script
├── settings.gradle.kts           # Module registry
├── gradle.properties
├── gradle/
│   └── libs.versions.toml        # Version catalog
├── composeApp/                   # Shared + Android entry (when using Compose MPP template)
│   ├── build.gradle.kts
│   └── src/
│       ├── commonMain/           # Shared Kotlin code (all targets)
│       ├── androidMain/          # Android-specific actuals
│       ├── iosMain/              # iOS-specific actuals
│       └── commonTest/           # Shared tests
└── iosApp/                       # Native iOS (SwiftUI) entry
    ├── iosApp.xcodeproj/
    └── iosApp/
        ├── iOSApp.swift
        └── ContentView.swift
```

### Module Roles

| Module | Role |
|--------|------|
| **composeApp** (shared) | Houses **all shared code** (business logic, models, data, optional UI). Exposes a framework to iOS. |
| **commonMain** | Pure Kotlin code reused across Android & iOS. |
| **androidMain** | Android-only `actual` implementations (e.g., `Context`-based). |
| **iosMain** | iOS-only `actual` implementations (using Kotlin/Native + Apple APIs). |
| **iosApp** | Swift/SwiftUI host that consumes the shared framework. |

> 💡 In modern KMP templates, the Android app lives inside `composeApp` (no separate `androidApp` module). The iOS app remains a separate Xcode project in `iosApp/`.

---

## 4. Shared Module Configuration

### Add Dependencies (in `gradle/libs.versions.toml`)

```toml
[versions]
kotlin = "2.0.21"
agp = "8.5.2"
coroutines = "1.9.0"
serialization = "1.7.3"
compose-multiplatform = "1.7.0"

[libraries]
kotlinx-coroutines-core   = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-core",   version.ref = "coroutines" }
kotlinx-serialization-json = { module = "org.jetbrains.kotlinx:kotlinx-serialization-json", version.ref = "serialization" }

[plugins]
kotlinMultiplatform    = { id = "org.jetbrains.kotlin.multiplatform",    version.ref = "kotlin" }
kotlinSerialization    = { id = "org.jetbrains.kotlin.plugin.serialization", version.ref = "kotlin" }
androidApplication     = { id = "com.android.application", version.ref = "agp" }
composeMultiplatform   = { id = "org.jetbrains.compose",   version.ref = "compose-multiplatform" }
composeCompiler        = { id = "org.jetbrains.kotlin.plugin.compose", version.ref = "kotlin" }
```

---

## 5. Clean Architecture Preparation

Inside `composeApp/src/commonMain/kotlin/com/example/uniwattelektrik/`, create the following package skeleton:

```
com/example/uniwattelektrik/
├── core/           # Cross-cutting: Result wrappers, DI glue, utils, error types
├── data/           # Implementations: repositories, data sources, DTOs, mappers
│   ├── local/
│   ├── remote/
│   └── repository/
├── domain/         # Pure business logic: entities, repository interfaces, use cases
│   ├── model/
│   ├── repository/
│   └── usecase/
└── presentation/   # UI-layer abstractions: ViewModels, UI state, navigation contracts
    ├── state/
    └── viewmodel/
```

### Purpose (high level)

| Layer | Purpose |
|-------|---------|
| **core** | Shared helpers, `Result`/error sealed types, DI bootstrap, logging, dispatchers. |
| **domain** | Framework-agnostic. Entities, use cases, repository **interfaces**. No Android/iOS imports. |
| **data** | Concrete repositories, remote (Ktor) & local (SQLDelight) data sources, mappers. |
| **presentation** | UI-facing orchestration: multiplatform ViewModels (or equivalent), UI state holders. |

> Dependency rule: **presentation → domain ← data**, `core` is used by all.

Create them via Android Studio: right-click `commonMain/kotlin/com.example.uniwattelektrik` → **New → Package**.

---

## 6. Gradle Configuration — Sample `composeApp/build.gradle.kts`

```kotlin
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    androidTarget {
        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }

    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.components.resources)

            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)
        }

        androidMain.dependencies {
            implementation("androidx.activity:activity-compose:1.9.2")
        }

        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}

android {
    namespace = "com.example.uniwattelektrik"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.uniwattelektrik"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
```

### Root `settings.gradle.kts`

```kotlin
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "UniWattElekTrik"
include(":composeApp")
```

---

## 7. Running the Project

### Android
1. Select the **composeApp** run configuration.
2. Choose an emulator or connected device (API 24+).
3. Click **▶ Run**.

CLI alternative:
```bash
./gradlew :composeApp:installDebug
```

### iOS (via Xcode)
1. Open `iosApp/iosApp.xcodeproj` in **Xcode**.
2. Select a simulator (e.g., *iPhone 15*).
3. Click **▶ Run**. Xcode triggers the Gradle task `embedAndSignAppleFrameworkForXcode` to build the shared framework.

### iOS (via Android Studio / KMM plugin)
1. In Android Studio, switch to the **iosApp** run configuration.
2. Pick an iOS simulator.
3. Click **▶ Run**.

---

## 8. Verify Setup — Minimal Shared Class

### `commonMain` — `Greeting.kt`
```kotlin
package com.example.uniwattelektrik

class Greeting {
    fun greet(): String = "Hello from UniWatt ElekTrik on ${getPlatform().name}!"
}
```

### `commonMain` — `Platform.kt` (expect)
```kotlin
package com.example.uniwattelektrik

interface Platform { val name: String }
expect fun getPlatform(): Platform
```

### `androidMain` — `Platform.android.kt`
```kotlin
package com.example.uniwattelektrik
import android.os.Build

class AndroidPlatform : Platform {
    override val name = "Android ${Build.VERSION.SDK_INT}"
}
actual fun getPlatform(): Platform = AndroidPlatform()
```

### `iosMain` — `Platform.ios.kt`
```kotlin
package com.example.uniwattelektrik
import platform.UIKit.UIDevice

class IOSPlatform : Platform {
    override val name = UIDevice.currentDevice.systemName() +
                        " " + UIDevice.currentDevice.systemVersion
}
actual fun getPlatform(): Platform = IOSPlatform()
```

### Use in Android (`MainActivity.kt`)
```kotlin
Text(text = Greeting().greet())
```

### Use in iOS (`ContentView.swift`)
```swift
import SwiftUI
import ComposeApp

struct ContentView: View {
    var body: some View {
        Text(Greeting().greet())
    }
}
```

Run both apps → you should see:
- **Android:** `Hello from UniWatt ElekTrik on Android 34!`
- **iOS:** `Hello from UniWatt ElekTrik on iOS 17.5!`

✅ Setup verified.

---

## 9. Version Control Setup

### `.gitignore` (project root)
```gitignore
# Gradle
.gradle/
build/
!gradle-wrapper.jar
local.properties

# IntelliJ / Android Studio
.idea/
*.iml
.kotlin/

# macOS
.DS_Store

# Xcode
xcuserdata/
*.xcworkspace/xcuserdata/
DerivedData/
iosApp/Pods/
iosApp/Podfile.lock

# Kotlin/Native
*.klib
```

### Initialize Git
```bash
cd /Users/noorul/AndroidStudioProjects/UniWattElekTrik
git init
git branch -M main
git add .
git commit -m "chore: initial KMP setup for UniWatt ElekTrik (Android + iOS)"
```

### (Optional) Push to remote
```bash
git remote add origin git@github.com:<your-user>/UniWattElekTrik.git
git push -u origin main
```

---

## ✅ Final Checklist

- [ ] JDK 17, Android Studio, Xcode installed
- [ ] KMP project generated with Android + iOS targets
- [ ] `composeApp` builds & syncs without errors
- [ ] Clean Architecture packages (`core`, `data`, `domain`, `presentation`) created
- [ ] Coroutines + Serialization dependencies added
- [ ] Android app runs on emulator
- [ ] iOS app runs on simulator via Xcode
- [ ] `Greeting` displays on both platforms
- [ ] Git initialized with `.gitignore` and initial commit

You now have a **stable, modern, production-ready KMP foundation** for UniWatt ElekTrik. 🚀

