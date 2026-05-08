import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

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
            // Link against the Firebase Apple SDK that the iosApp Xcode project
            // ships in via Swift Package Manager (FirebaseAuth product).
            linkerOpts("-framework", "FirebaseAuth")
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
            // Lightweight xlsx reader (~3 MB, no MethodHandle — works on minSdk 24).
            implementation("org.dhatim:fastexcel-reader:0.18.4")
            // Android does NOT ship javax.xml.stream (StAX API) — bundle it so
            // fastexcel-reader's transitive aalto-xml / stax2-api can resolve.
            implementation("stax:stax-api:1.0.1")
        }
        iosMain.dependencies {
            // Real Firebase Auth on iOS via the GitLive KMP wrapper. Requires
            // the iosApp Xcode project to add the Firebase Apple SDK (SPM)
            // and call FirebaseApp.configure() on launch.
            api(libs.gitlive.firebase.auth)
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
        versionCode = 1
        versionName = "1.0"
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
            )
        }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    debugImplementation(libs.compose.uiTooling)
}

