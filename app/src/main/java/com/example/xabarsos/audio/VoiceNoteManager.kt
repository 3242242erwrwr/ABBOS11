package com.example.xabarsos.audio

import android.content.Context
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import android.util.Base64
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

class VoiceNoteManager(private val context: Context) {

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val scope = CoroutineScope(Dispatchers.IO + Job())

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _recordingDurationSeconds = MutableStateFlow(0)
    val recordingDurationSeconds: StateFlow<Int> = _recordingDurationSeconds.asStateFlow()

    private var mediaRecorder: MediaRecorder? = null
    private var mediaPlayer: MediaPlayer? = null
    private var tempAudioFile: File? = null
    private var timerJob: Job? = null

    @Synchronized
    fun startRecording(): Boolean {
        try {
            stopPlaying()

            tempAudioFile = File.createTempFile("galasavoy_", ".m4a", context.cacheDir)

            @Suppress("DEPRECATION")
            mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                MediaRecorder()
            }

            mediaRecorder?.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioSamplingRate(16000)
                setAudioEncodingBitRate(32000)
                setOutputFile(tempAudioFile?.absolutePath)
                prepare()
                start()
            }

            _isRecording.value = true
            startRecordingTimer()
            return true
        } catch (e: Exception) {
            Log.e("VoiceNoteManager", "Error starting recording: ${e.message}")
            stopRecordingAndGetBase64()
            return false
        }
    }

    @Synchronized
    fun stopRecordingAndGetBase64(): String? {
        stopRecordingTimer()
        _isRecording.value = false

        try {
            mediaRecorder?.stop()
            mediaRecorder?.release()
        } catch (e: Exception) {}
        mediaRecorder = null

        val file = tempAudioFile ?: return null
        if (!file.exists() || file.length() == 0L) return null

        return try {
            val bytes = FileInputStream(file).use { it.readBytes() }
            file.delete()
            tempAudioFile = null
            Base64.encodeToString(bytes, Base64.NO_WRAP)
        } catch (e: Exception) {
            Log.e("VoiceNoteManager", "Error reading recorded audio file: ${e.message}")
            null
        }
    }

    @Synchronized
    fun playVoiceNote(base64Audio: String, onFinished: () -> Unit = {}) {
        try {
            stopPlaying()

            if (base64Audio.isBlank()) return

            val audioBytes = Base64.decode(base64Audio, Base64.NO_WRAP)
            val playFile = File.createTempFile("play_galasavoy_", ".m4a", context.cacheDir)
            FileOutputStream(playFile).use { it.write(audioBytes) }

            // Auto-boost speaker volume to 100% MAX
            try {
                val maxMusicVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, maxMusicVol, 0)
            } catch (e: Exception) {}

            mediaPlayer = MediaPlayer().apply {
                setDataSource(playFile.absolutePath)
                setOnCompletionListener {
                    _isPlaying.value = false
                    try {
                        playFile.delete()
                    } catch (e: Exception) {}
                    onFinished()
                }
                prepare()
                start()
            }

            _isPlaying.value = true
        } catch (e: Exception) {
            Log.e("VoiceNoteManager", "Error playing voice note: ${e.message}")
            _isPlaying.value = false
        }
    }

    @Synchronized
    fun stopPlaying() {
        try {
            if (mediaPlayer?.isPlaying == true) {
                mediaPlayer?.stop()
            }
            mediaPlayer?.release()
        } catch (e: Exception) {}
        mediaPlayer = null
        _isPlaying.value = false
    }

    private fun startRecordingTimer() {
        _recordingDurationSeconds.value = 0
        timerJob?.cancel()
        timerJob = scope.launch {
            while (_isRecording.value) {
                kotlinx.coroutines.delay(1000)
                _recordingDurationSeconds.value += 1
            }
        }
    }

    private fun stopRecordingTimer() {
        timerJob?.cancel()
    }
}
