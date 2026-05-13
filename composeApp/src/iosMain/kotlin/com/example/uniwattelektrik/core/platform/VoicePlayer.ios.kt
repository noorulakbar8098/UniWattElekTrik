package com.example.uniwattelektrik.core.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

@Composable
actual fun rememberVoicePlayer(): VoicePlayer = remember { NoOpVoicePlayer() }

private class NoOpVoicePlayer : VoicePlayer {
    override val currentUrl: String? = null
    override val isPlaying: Boolean = false
    override val positionMs: Long = 0L
    override val durationMs: Long = 0L
    override fun play(url: String) = Unit
    override fun pause() = Unit
    override fun stop() = Unit
    override fun release() = Unit
}
