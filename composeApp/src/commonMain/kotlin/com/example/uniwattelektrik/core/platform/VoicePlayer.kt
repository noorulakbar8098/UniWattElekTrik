package com.example.uniwattelektrik.core.platform

import androidx.compose.runtime.Composable

/**
 * Plays a previously-recorded voice note from a remote URL.
 *
 * One instance per screen — call [play] with a different URL to switch
 * playback (auto-stops the previous track). [release] must be called when
 * the player leaves composition.
 */
@Composable
expect fun rememberVoicePlayer(): VoicePlayer

interface VoicePlayer {
    /** Currently-loaded URL, or null when idle. */
    val currentUrl: String?

    /** True while audio is actively playing. */
    val isPlaying: Boolean

    /** Elapsed time in milliseconds since [play] started. 0 when idle. */
    val positionMs: Long

    /** Total duration in milliseconds once prepared (0 until then). */
    val durationMs: Long

    /** Start (or resume) playing [url]. Safe to call repeatedly. */
    fun play(url: String)

    /** Pause without releasing — [play] resumes. */
    fun pause()

    /** Stop and reset position to 0. */
    fun stop()

    /** Release native resources. Call from a DisposableEffect. */
    fun release()
}
