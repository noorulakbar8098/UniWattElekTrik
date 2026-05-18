package com.example.uniwattelektrik.core.notification

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.uniwattelektrik.core.AppLog
import com.example.uniwattelektrik.push.PushTokenRegistrar
import java.util.concurrent.TimeUnit

/**
 * Periodic safety-net background job.
 *
 * Purpose: on aggressive-killer OEMs (Xiaomi MIUI, Oppo ColorOS, Vivo
 * FunTouch, …) the FCM socket is sometimes silently severed when the app
 * is swiped from recents — Google Play Services then can't deliver pushes
 * until the app is opened again. WorkManager is the ONE background API
 * those OEMs reliably honour (because Play Store certification requires
 * it), so we use it as a heartbeat:
 *
 *  1. Re-fetch the FCM token and write it to Firestore (catches token
 *     rotations that happened while the app was killed and the
 *     FirebaseMessagingService never got a chance to run).
 *  2. Future: poll a "missed events" collection in Firestore and post
 *     local notifications for anything the device didn't receive via FCM
 *     in the last 15 min.
 *
 * Constraints:
 *  - Requires network — pointless to run offline.
 *  - Min repeat interval = 15 min (WorkManager hard floor).
 *  - Battery + Doze aware — WorkManager batches with system idle windows.
 *
 * NOT a replacement for FCM. FCM still does the heavy lifting; this just
 * smooths over OEM flakiness.
 */
class PushSyncWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        runCatching { PushTokenRegistrar.registerCurrentDevice() }
            .onFailure { AppLog.w(TAG, "token refresh failed", it) }

        // TODO (Phase 2): query firestore /users/{uid}/missed_pushes after
        // we add server-side mirroring; for now we just keep the token fresh.

        return Result.success()
    }

    companion object {
        private const val TAG       = "PushSyncWorker"
        const val UNIQUE_NAME       = "uw.push.sync"
        private const val INTERVAL_MIN = 30L   // WorkManager floor is 15 min

        /**
         * Enqueue (or refresh) the periodic worker. Safe to call on every
         * launch — `ExistingPeriodicWorkPolicy.KEEP` means we won't reset
         * the timer on already-scheduled work.
         */
        fun enqueue(context: Context) {
            val req = PeriodicWorkRequestBuilder<PushSyncWorker>(
                INTERVAL_MIN, TimeUnit.MINUTES,
                // FlexInterval — Android may run the worker any time inside
                // [intervalMillis - flexMillis, intervalMillis].
                5L, TimeUnit.MINUTES,
            )
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build(),
                )
                .addTag(UNIQUE_NAME)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                UNIQUE_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                req,
            )
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(UNIQUE_NAME)
        }
    }
}

