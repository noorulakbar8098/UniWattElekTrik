package com.example.uniwattelektrik.core.platform

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.os.Build
import android.os.SystemClock
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

private const val TAG = "VoiceRecorder"

@Composable
actual fun rememberVoiceRecorder(): VoiceRecorder {
    val context = LocalContext.current
    val recorder = remember { AndroidVoiceRecorder(context.applicationContext) }
    // Make sure we don't leak the MediaRecorder if the screen leaves comp
    // mid-recording.
    DisposableEffect(recorder) {
        onDispose { recorder.cancel() }
    }
    return recorder
}

private class AndroidVoiceRecorder(private val context: Context) : VoiceRecorder {
    private var recorder: MediaRecorder? = null
    private var outFile: File? = null
    private var startedAtMs: Long = 0L

    private var isRecordingState by mutableStateOf(false)
    private var elapsedState by mutableLongStateOf(0L)
    private var lastErrorState by mutableStateOf<String?>(null)

    override val isRecording: Boolean get() = isRecordingState
    override val elapsedMs: Long get() {
        // Recompute on access so callers polling the state get a live tick.
        return if (isRecordingState && startedAtMs > 0L)
            SystemClock.elapsedRealtime() - startedAtMs
        else elapsedState
    }
    override val lastError: String? get() = lastErrorState

    override suspend fun start(): Unit = withContext(Dispatchers.IO) {
        if (isRecordingState) return@withContext
        if (!hasMicPermission()) {
            lastErrorState = "Microphone permission denied. Enable it in Settings."
            Log.w(TAG, "start: mic permission missing")
            return@withContext
        }
        try {
            val dir = File(context.cacheDir, "voice-notes").apply { mkdirs() }
            val file = File(dir, "vn_${System.currentTimeMillis()}.m4a")
            outFile = file

            @Suppress("DEPRECATION")
            val mr = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
                MediaRecorder(context)
            else MediaRecorder()

            // Conservative encoder settings — broad device support. AAC LC
            // at 22.05 kHz / 64 kbps is more compatible than 44.1 kHz on
            // budget chipsets that ship a stripped-down audio HAL.
            mr.setAudioSource(MediaRecorder.AudioSource.MIC)
            mr.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            mr.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            mr.setAudioSamplingRate(22_050)
            mr.setAudioEncodingBitRate(64_000)
            mr.setAudioChannels(1)
            mr.setOutputFile(file.absolutePath)
            mr.setOnErrorListener { _, what, extra ->
                Log.e(TAG, "MediaRecorder onError what=$what extra=$extra")
            }
            mr.prepare()
            mr.start()
            recorder = mr
            startedAtMs = SystemClock.elapsedRealtime()
            isRecordingState = true
            elapsedState = 0L
            lastErrorState = null
            Log.d(TAG, "start: recording to ${file.absolutePath}")
        } catch (t: Throwable) {
            Log.e(TAG, "start failed", t)
            lastErrorState = "Recording failed to start: ${t.message ?: "unknown"}"
            cleanupFailedStart()
        }
    }

    override suspend fun stop(): VoiceRecording? = withContext(Dispatchers.IO) {
        if (!isRecordingState) {
            if (lastErrorState == null) lastErrorState = "No active recording."
            return@withContext null
        }
        val mr = recorder ?: run {
            lastErrorState = "No active recording."
            return@withContext null
        }
        val file = outFile ?: run {
            lastErrorState = "No active recording."
            return@withContext null
        }
        // MediaRecorder.stop() throws RuntimeException("stop failed") when
        // called too soon after start() — under ~600ms on most devices the
        // encoder hasn't emitted a usable frame yet.
        val elapsed = SystemClock.elapsedRealtime() - startedAtMs
        if (elapsed < 700L) {
            runCatching { mr.stop() }
            runCatching { mr.release() }
            recorder = null
            runCatching { file.delete() }
            outFile = null
            isRecordingState = false
            elapsedState = 0L
            lastErrorState = "Recording too short. Hold the mic for at least a second."
            return@withContext null
        }
        // File-size-as-truth: keep the audio if any bytes landed on disk
        // even when MediaRecorder.stop() throws (well-known Android quirk).
        var stopThrew: Throwable? = null
        try {
            mr.stop()
        } catch (t: Throwable) {
            stopThrew = t
            Log.w(TAG, "stop() threw, will check file size: ${t.message}")
        }
        runCatching { mr.release() }
        recorder = null
        isRecordingState = false
        val duration = elapsed.coerceAtLeast(0L)
        elapsedState = duration

        val fileSize = file.length()
        Log.d(TAG, "stop: file=${file.absolutePath} size=$fileSize durationMs=$duration")
        if (fileSize > 0L) {
            lastErrorState = null
            val authority = "${context.packageName}.fileprovider"
            val uri = FileProvider.getUriForFile(context, authority, file)
            VoiceRecording(contentUri = uri.toString(), durationMs = duration)
        } else {
            Log.e(TAG, "stop: empty audio file (size=0) — discarding", stopThrew)
            runCatching { file.delete() }
            outFile = null
            lastErrorState = if (stopThrew != null) {
                "Recording failed: ${stopThrew.message ?: "encoder error"}"
            } else {
                "Microphone returned no audio. Try again."
            }
            null
        }
    }

    override fun cancel() {
        if (!isRecordingState && recorder == null) return
        runCatching { recorder?.stop() }
        runCatching { recorder?.release() }
        recorder = null
        runCatching { outFile?.delete() }
        outFile = null
        isRecordingState = false
        elapsedState = 0L
        lastErrorState = null
    }

    private fun cleanupFailedStart() {
        runCatching { recorder?.release() }
        recorder = null
        runCatching { outFile?.delete() }
        outFile = null
        isRecordingState = false
        startedAtMs = 0L
    }

    private fun hasMicPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED
}
