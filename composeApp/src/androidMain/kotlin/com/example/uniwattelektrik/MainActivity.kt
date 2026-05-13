package com.example.uniwattelektrik

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.uniwattelektrik.core.notification.DeepLinkBus
import com.example.uniwattelektrik.di.AppContainer
import com.example.uniwattelektrik.push.PushTokenRegistrar
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    /**
     * Registered once at activity creation. Result is intentionally ignored —
     * [com.example.uniwattelektrik.platform.LocationProvider] re-checks the
     * permission at every call site and returns `null` if it's missing, so
     * the UI degrades gracefully whether or not the user grants it.
     */
    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { /* no-op */ }

    /** Android 13+ runtime POST_NOTIFICATIONS prompt. Result informational only. */
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { /* no-op — user can grant later in Settings */ }

    /** RECORD_AUDIO prompt for voice-note recording in task threads. Result
     *  informational only — VoiceRecorder rechecks at call time. */
    private val micPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { /* no-op — user can grant later in Settings */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Keep the Splash theme (Ink900 windowBackground) through Compose's
        // first frame — switching to Material.Light here used to be needed
        // when splash_bg was a bright brand blue, but it now causes a
        // sea-blue flash (Material.Light's default holo-blue accent shows
        // briefly before Compose paints over it).
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        // Initialise the shared AppContext used by `actual` platform classes
        // (e.g. SessionStorage, LocationProvider).
        AndroidAppContext.init(applicationContext)

        // Ask for location at startup. The system dialog is shown only the first
        // time; on subsequent launches this is a silent no-op if already granted.
        if (!hasLocationPermission()) {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                ),
            )
        }

        // Android 13+ requires runtime consent before any notifications are posted.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        // Voice-note recording — request the mic permission the first time the
        // user lands. Result is ignored: VoiceRecorder.start() checks the
        // permission again at call time and silently no-ops if it's denied,
        // so the UI degrades to a "nothing happens on tap" state which the
        // user can resolve via system Settings.
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED) {
            micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }

        // Register / unregister the device's FCM token in Firestore as the user
        // signs in and out. distinctUntilChangedBy{uid} avoids re-uploading on
        // every session emission (e.g. profile refreshes).
        lifecycleScope.launch {
            var lastUid: String? = null
            AppContainer.authRepository.observeSession()
                .distinctUntilChangedBy { it?.user?.id }
                .collect { session ->
                    val uid = session?.user?.id
                    when {
                        uid != null && uid != lastUid -> {
                            runCatching { PushTokenRegistrar.registerCurrentDevice() }
                        }
                        uid == null && lastUid != null -> {
                            runCatching { PushTokenRegistrar.unregisterCurrentDevice() }
                        }
                    }
                    lastUid = uid
                }
        }

        setContent {
            App()
        }

        // Handle deep-link from a "cold-start" notification tap.
        publishDeepLinkFromIntent(intent)
    }

    /** Handle taps that arrive while the activity is already alive. */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        publishDeepLinkFromIntent(intent)
    }

    /**
     * Translate FCM `data` extras into a [DeepLinkBus] route key. Keys are
     * consumed (cleared) by whichever shell handles them so the same intent
     * isn't replayed on rotation.
     */
    private fun publishDeepLinkFromIntent(intent: Intent?) {
        val type = intent?.getStringExtra("type") ?: return
        val key = when (type) {
            "task"     -> "tasks"
            "leave"    -> "leave"
            "approval" -> "approvals"
            else       -> type    // pass-through for future types
        }
        // Optional FCM data: when the notification is for a specific task
        // (e.g. a new note added), the shell will deep-link straight into
        // the matching task detail instead of just opening the tasks tab.
        val taskId = intent.getStringExtra("taskId")
            ?: intent.getStringExtra("relatedId")
        DeepLinkBus.publish(key, taskId)
        // Drop the extras so the same notification doesn't re-trigger on
        // configuration changes (rotation, dark-mode toggle, etc.).
        intent.removeExtra("type")
        intent.removeExtra("taskId")
        intent.removeExtra("relatedId")
    }

    private fun hasLocationPermission(): Boolean {
        val fine = ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_FINE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_COARSE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}
