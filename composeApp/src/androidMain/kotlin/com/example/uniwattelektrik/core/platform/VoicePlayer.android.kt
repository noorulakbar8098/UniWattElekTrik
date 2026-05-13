package com.example.uniwattelektrik.core.platform

import android.media.MediaPlayer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

@Composable
actual fun rememberVoicePlayer(): VoicePlayer {
    val player = remember { AndroidVoicePlayer() }
    DisposableEffect(player) { onDispose { player.release() } }
    return player
}

private class AndroidVoicePlayer : VoicePlayer {
    private var media: MediaPlayer? = null
    private var currentUrlState by mutableStateOf<String?>(null)
    private var isPlayingState by mutableStateOf(false)
    private var positionState by mutableLongStateOf(0L)
    private var durationState by mutableLongStateOf(0L)

    override val currentUrl: String? get() = currentUrlState
    override val isPlaying: Boolean get() = isPlayingState
    override val positionMs: Long get() = media?.currentPosition?.toLong() ?: positionState
    override val durationMs: Long get() = durationState

    override fun play(url: String) {
        if (currentUrlState == url && media != null) {
            // Resume or restart from current position.
            runCatching { media?.start() }
            isPlayingState = true
            return
        }
        // Different track — release previous and load fresh.
        release()
        try {
            val mp = MediaPlayer()
            mp.setDataSource(url)
            mp.setOnPreparedListener {
                durationState = it.duration.toLong()
                it.start()
                isPlayingState = true
            }
            mp.setOnCompletionListener {
                isPlayingState = false
                positionState = durationState
            }
            mp.setOnErrorListener { _, _, _ ->
                isPlayingState = false
                true
            }
            mp.prepareAsync()
            media = mp
            currentUrlState = url
        } catch (t: Throwable) {
            t.printStackTrace()
            release()
        }
    }

    override fun pause() {
        runCatching { media?.pause() }
        isPlayingState = false
    }

    override fun stop() {
        runCatching { media?.pause() }
        runCatching { media?.seekTo(0) }
        positionState = 0L
        isPlayingState = false
    }

    override fun release() {
        runCatching { media?.release() }
        media = null
        currentUrlState = null
        isPlayingState = false
        positionState = 0L
        durationState = 0L
    }
}
