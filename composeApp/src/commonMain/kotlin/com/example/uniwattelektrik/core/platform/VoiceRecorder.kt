package com.example.uniwattelektrik.core.platform

import androidx.compose.runtime.Composable

/**
 * Records short voice notes on the user's device for task-thread attachments.
 *
 *  • [start] kicks off recording. Hides the OS mic-permission prompt — call
 *    sites should request `RECORD_AUDIO` ahead of time on Android.
 *  • [stop] writes the buffered audio to a temp file and returns its
 *    platform-specific URI (Android: `content://` via FileProvider,
 *    iOS: `file://`). Caller uploads it via
 *    [com.example.uniwattelektrik.feature.workforce.data.remote.WorkforceDirectory.uploadVoiceNote].
 *  • [cancel] discards the in-progress recording.
 *
 * The recorder is a small state machine — one call site, no concurrency.
 * Re-entrant `start` is a no-op while a recording is already running.
 */
@Composable
expect fun rememberVoiceRecorder(): VoiceRecorder

interface VoiceRecorder {
    /** True while a recording is currently in progress. */
    val isRecording: Boolean

    /** Elapsed time in milliseconds since [start]. 0 when idle. */
    val elapsedMs: Long

    /**
     * Human-readable reason for the most recent [start] / [stop] failure.
     * Reset to null on each successful action so the UI doesn't show stale
     * errors. Typical values:
     *   - "Microphone permission denied. Enable it in Settings."
     *   - "Recording too short. Hold the mic for at least a second."
     *   - "Recording failed. Please try again."
     */
    val lastError: String?

    /**
     * Start recording. Suspends while MediaRecorder.prepare() / start() set
     * up the audio pipeline (50-300ms+ on low-end devices). Caller MUST run
     * this from a coroutine — never block on it from the main thread, since
     * the underlying native calls can ANR under system pressure.
     *
     * No-op if already recording or permission missing.
     */
    suspend fun start()

    /**
     * Stop recording and return the captured audio's URI + duration in ms.
     * Same suspend contract as [start] — runs file I/O + MediaRecorder.stop()
     * off the main thread.
     *
     * Returns null if nothing was recorded; check [lastError] for the reason.
     */
    suspend fun stop(): VoiceRecording?

    /** Discard the in-progress recording without producing an output. */
    fun cancel()
}

data class VoiceRecording(
    val contentUri: String,
    val durationMs: Long,
)
