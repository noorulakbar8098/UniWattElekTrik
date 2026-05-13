package com.example.uniwattelektrik.core.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

@Composable
actual fun rememberVoiceRecorder(): VoiceRecorder = remember { NoOpVoiceRecorder() }

private class NoOpVoiceRecorder : VoiceRecorder {
    override val isRecording: Boolean = false
    override val elapsedMs: Long = 0L
    override val lastError: String? = "Voice recording is not yet implemented on iOS."
    override suspend fun start() = Unit
    override suspend fun stop(): VoiceRecording? = null
    override fun cancel() = Unit
}
