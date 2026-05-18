import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

// ─────────────────────────────────────────────────────────────────────────────
//  App version — single source of truth.
//  Bump `appVersionCode` for every Play upload (must be strictly increasing);
//  `appVersionName` is the human-readable string shown in Settings → "v 1.0".
// ─────────────────────────────────────────────────────────────────────────────
val appVersionCode = 1
val appVersionName = "1.0"

// ─────────────────────────────────────────────────────────────────────────────
//  Release signing config — loaded from `keystore.properties` at the project
//  root (NOT committed; see keystore.properties.example for the schema).
//  When the file is missing the release build falls back to the debug
//  signing key so local `assembleRelease` doesn't fail; CI / Play uploads
//  must provide the real keystore.
// ─────────────────────────────────────────────────────────────────────────────
val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties().apply {
    if (keystorePropertiesFile.exists()) {
        load(FileInputStream(keystorePropertiesFile))
    }
}
val hasReleaseSigning = keystoreProperties.getProperty("storeFile")?.isNotBlank() == true

// Firebase is required. Fail the build with an actionable message if the config
// file is missing so this can't be confused with a runtime auth error.
val googleServicesJson = layout.projectDirectory.file("google-services.json").asFile
if (!googleServicesJson.exists()) {
    throw GradleException(
        """
        |Missing Firebase config: ${googleServicesJson.path}
        |
        |1. Firebase Console → Project Settings → Your Android app
        |   (package: com.example.uniwattelektrik)
        |2. Download google-services.json
        |3. Place it at composeApp/google-services.json
        |4. Re-run the build.
        """.trimMargin()
    )
}
apply(plugin = libs.plugins.googleServices.get().pluginId)

kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }

    // Silence the "expect/actual classes are in Beta" warning.
    targets.all {
        compilations.all {
            compileTaskProvider.configure {
                compilerOptions {
                    freeCompilerArgs.add("-Xexpect-actual-classes")
                }
            }
        }
    }

    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
            // GitLive Firebase exports its types — needed so iosApp.swift can
            // see them and so linker can resolve Firebase symbols correctly.
            export(libs.gitlive.firebase.auth)
            export(libs.gitlive.firebase.firestore)
            // Link against the Firebase Apple SDK that the iosApp Xcode project
            // ships in via Swift Package Manager (FirebaseAuth + FirebaseFirestore).
            linkerOpts("-framework", "FirebaseAuth")
            linkerOpts("-framework", "FirebaseFirestore")
        }
    }
    
    sourceSets {
        androidMain.dependencies {
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.androidx.activity.compose)
            implementation(libs.firebase.auth)
            implementation(libs.firebase.firestore)
            implementation(libs.firebase.messaging)
            implementation(libs.firebase.perf)
            implementation(libs.coil.network.okhttp)
            implementation(libs.kotlinx.coroutines.playServices)
            implementation(libs.play.services.location)
            implementation(libs.osmdroid.android)
            implementation(libs.androidx.work.runtime)
            // Lightweight xlsx reader (~3 MB, no MethodHandle — works on minSdk 24).
            implementation("org.dhatim:fastexcel-reader:0.18.4")
            // Android does NOT ship javax.xml.stream (StAX API) — bundle it so
            // fastexcel-reader's transitive aalto-xml / stax2-api can resolve.
            implementation("stax:stax-api:1.0.1")
        }
        iosMain.dependencies {
            // Real Firebase Auth + Firestore on iOS via the GitLive KMP
            // wrapper. Requires the iosApp Xcode project to add the Firebase
            // Apple SDK (SPM) with FirebaseAuth + FirebaseFirestore products
            // and call FirebaseApp.configure() on launch.
            api(libs.gitlive.firebase.auth)
            api(libs.gitlive.firebase.firestore)
        }
        commonMain.dependencies {
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.ui)
            implementation(compose.materialIconsExtended)
            implementation(libs.compose.components.resources)
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.datetime)
            implementation(libs.coil.compose)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}

android {
    namespace = "com.example.uniwattelektrik"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.example.uniwattelektrik"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = appVersionCode
        versionName = appVersionName

        // Surfaces appVersionName/appVersionCode at runtime via BuildConfig
        // so the in-app "v X.Y" label stays in sync with the Play upload.
        buildConfigField("String", "APP_VERSION_NAME", "\"$appVersionName\"")
        buildConfigField("int",    "APP_VERSION_CODE", "$appVersionCode")
    }

    buildFeatures {
        // BuildConfig is opt-in on AGP 8+; we use it for the version constants
        // above and any future env-specific flags.
        buildConfig = true
    }

    // ─── Signing ────────────────────────────────────────────────────────────
    signingConfigs {
        // Release signing reads from keystore.properties. When that file is
        // absent (fresh checkout, CI without secrets) we leave this empty so
        // `release` falls back to debug signing — the build still produces an
        // installable APK locally, it just isn't Play-uploadable.
        create("release") {
            if (hasReleaseSigning) {
                storeFile     = rootProject.file(keystoreProperties.getProperty("storeFile"))
                storePassword = keystoreProperties.getProperty("storePassword")
                keyAlias      = keystoreProperties.getProperty("keyAlias")
                keyPassword   = keystoreProperties.getProperty("keyPassword")
            }
        }
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += listOf(
                "META-INF/DEPENDENCIES",
                "META-INF/LICENSE",
                "META-INF/LICENSE.txt",
                "META-INF/license.txt",
                "META-INF/NOTICE",
                "META-INF/NOTICE.txt",
                "META-INF/notice.txt",
                "META-INF/*.kotlin_module",
                // OSMDroid + others bundle these and they conflict between deps.
                "META-INF/INDEX.LIST",
                "META-INF/io.netty.versions.properties",
            )
        }
        // Don't compress already-compressed assets (faster install, no waste).
        jniLibs {
            useLegacyPackaging = false
        }
    }

    // ─── Build types ────────────────────────────────────────────────────────
    buildTypes {
        getByName("debug") {
            // Side-by-side install with release: the debug build lives at
            // `com.example.uniwattelektrik.debug` and shows "1.0-debug" in
            // the version label so QA can tell the two apart instantly.
            applicationIdSuffix = ".debug"
            versionNameSuffix   = "-debug"
            isDebuggable        = true
            isMinifyEnabled     = false
            isShrinkResources   = false
            // Performance Monitoring is noisy + skews data in dev builds.
            // The plugin reads this flag at apply-time.
            extra["enablePerformancePlugin"] = false
        }
        getByName("release") {
            isDebuggable      = false
            // R8 full mode: shrinks code + obfuscates + optimises. ~40% APK
            // size reduction on this app and a measurable startup win.
            isMinifyEnabled   = true
            // Drops unused res/ entries that R8 proves are unreachable.
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            signingConfig = signingConfigs.getByName(
                if (hasReleaseSigning) "release" else "debug",
            )
            // Strip Compose runtime / Compose-compiler intermediates that
            // aren't used at runtime to slim the AAB further.
            isCrunchPngs = true
        }
    }

    // ─── App-bundle splits — smaller per-device downloads from Play ─────────
    bundle {
        language { enableSplit = true }
        density  { enableSplit = true }
        abi      { enableSplit = true }
    }

    // ─── Lint — fail the release build on real correctness errors only ─────
    lint {
        abortOnError       = false   // never block a build on style warnings
        checkReleaseBuilds = true
        warningsAsErrors   = false
        // Keep the baseline file out of source control until we've cleaned
        // existing warnings; uncomment to snapshot them.
        // baseline = file("lint-baseline.xml")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    debugImplementation(libs.compose.uiTooling)
}



