package com.example.xabarsos.audio

import android.content.Context
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.util.Base64
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.atomic.AtomicBoolean

class VoiceNoteManager(private val context: Context) {

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val scope = CoroutineScope(Dispatchers.IO + Job())

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _recordingDurationSeconds = MutableStateFlow(0)
    val recordingDurationSeconds: StateFlow<Int> = _recordingDurationSeconds.asStateFlow()

    private val _selectedVoiceEffect = MutableStateFlow(VoiceEffect.NORMAL)
    val selectedVoiceEffect: StateFlow<VoiceEffect> = _selectedVoiceEffect.asStateFlow()

    private var audioRecord: AudioRecord? = null
    private var mediaPlayer: MediaPlayer? = null
    private var timerJob: Job? = null
    private val isRecordingActive = AtomicBoolean(false)
    private val recordedPcmStream = ByteArrayOutputStream()

    companion object {
        private const val SAMPLE_RATE = 16000
        private const val CHANNEL_IN = AudioFormat.CHANNEL_IN_MONO
        private const val ENCODING = AudioFormat.ENCODING_PCM_16BIT
    }

    fun setSelectedVoiceEffect(effect: VoiceEffect) {
        _selectedVoiceEffect.value = effect
    }

    @Synchronized
    fun startRecording(): Boolean {
        try {
            stopPlaying()
            recordedPcmStream.reset()

            val minBufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_IN, ENCODING)
            if (minBufferSize <= 0) return false

            try {
                audioRecord = AudioRecord(
                    MediaRecorder.AudioSource.MIC,
                    SAMPLE_RATE,
                    CHANNEL_IN,
                    ENCODING,
                    maxOf(minBufferSize * 2, 4096)
                )
            } catch (se: SecurityException) {
                Log.e("VoiceNoteManager", "RECORD_AUDIO permission missing: ${se.message}")
                return false
            }

            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                audioRecord?.release()
                audioRecord = null
                return false
            }

            audioRecord?.startRecording()
            isRecordingActive.set(true)
            _isRecording.value = true

            startRecordingTimer()

            // Recording Thread
            scope.launch(Dispatchers.IO) {
                val buffer = ByteArray(2048)
                while (isRecordingActive.get()) {
                    try {
                        val read = audioRecord?.read(buffer, 0, buffer.size) ?: 0
                        if (read > 0) {
                            recordedPcmStream.write(buffer, 0, read)
                        }
                    } catch (e: Exception) {
                        Log.e("VoiceNoteManager", "Error reading PCM: ${e.message}")
                    }
                }
            }
            return true
        } catch (e: Exception) {
            Log.e("VoiceNoteManager", "Error starting recording: ${e.message}")
            stopRecordingAndGetBase64()
            return false
        }
    }

    @Synchronized
    fun stopRecordingAndGetBase64(effect: VoiceEffect = _selectedVoiceEffect.value): String? {
        stopRecordingTimer()
        isRecordingActive.set(false)
        _isRecording.value = false

        try {
            audioRecord?.stop()
            audioRecord?.release()
        } catch (e: Exception) {}
        audioRecord = null

        val pcmBytes = recordedPcmStream.toByteArray()
        recordedPcmStream.reset()

        if (pcmBytes.isEmpty()) return null

        return try {
            // Convert PCM ByteArray to ShortArray
            val shortBuffer = ShortArray(pcmBytes.size / 2)
            ByteBuffer.wrap(pcmBytes).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(shortBuffer)

            // Apply selected Voice Effect (Robot, Horror, Baby, Old Man, Girl)
            val transformedShorts = VoiceEffectProcessor.applyEffect(shortBuffer, effect, SAMPLE_RATE)

            // Convert transformed Shorts back to WAV file bytes
            val wavFile = createWavFile(transformedShorts, SAMPLE_RATE)
            val wavBytes = FileInputStream(wavFile).use { it.readBytes() }
            wavFile.delete()

            Base64.encodeToString(wavBytes, Base64.NO_WRAP)
        } catch (e: Exception) {
            Log.e("VoiceNoteManager", "Error processing voice effect: ${e.message}")
            null
        }
    }

    @Synchronized
    fun playVoiceNote(base64Audio: String, onFinished: () -> Unit = {}) {
        try {
            stopPlaying()

            if (base64Audio.isBlank()) return

            val audioBytes = Base64.decode(base64Audio, Base64.NO_WRAP)
            val playFile = File.createTempFile("play_galasavoy_", ".wav", context.cacheDir)
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

    private fun createWavFile(pcmShorts: ShortArray, sampleRate: Int): File {
        val file = File.createTempFile("voice_effect_", ".wav", context.cacheDir)
        val rawData = ByteArray(pcmShorts.size * 2)
        ByteBuffer.wrap(rawData).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().put(pcmShorts)

        FileOutputStream(file).use { out ->
            val totalDataLen = rawData.size + 36
            val byteRate = sampleRate * 2

            val header = ByteArray(44)
            header[0] = 'R'.code.toByte()
            header[1] = 'I'.code.toByte()
            header[2] = 'F'.code.toByte()
            header[3] = 'F'.code.toByte()
            header[4] = (totalDataLen and 0xff).toByte()
            header[5] = ((totalDataLen shr 8) and 0xff).toByte()
            header[6] = ((totalDataLen shr 16) and 0xff).toByte()
            header[7] = ((totalDataLen shr 24) and 0xff).toByte()
            header[8] = 'W'.code.toByte()
            header[9] = 'A'.code.toByte()
            header[10] = 'V'.code.toByte()
            header[11] = 'E'.code.toByte()
            header[12] = 'f'.code.toByte()
            header[13] = 'm'.code.toByte()
            header[14] = 't'.code.toByte()
            header[15] = ' '.code.toByte()
            header[16] = 16 // 16 for PCM
            header[17] = 0
            header[18] = 0
            header[19] = 0
            header[20] = 1 // PCM = 1
            header[21] = 0
            header[22] = 1 // Mono = 1
            header[23] = 0
            header[24] = (sampleRate and 0xff).toByte()
            header[25] = ((sampleRate shr 8) and 0xff).toByte()
            header[26] = ((sampleRate shr 16) and 0xff).toByte()
            header[27] = ((sampleRate shr 24) and 0xff).toByte()
            header[28] = (byteRate and 0xff).toByte()
            header[29] = ((byteRate shr 8) and 0xff).toByte()
            header[30] = ((byteRate shr 16) and 0xff).toByte()
            header[31] = ((byteRate shr 24) and 0xff).toByte()
            header[32] = 2 // Block align
            header[33] = 0
            header[34] = 16 // Bits per sample
            header[35] = 0
            header[36] = 'd'.code.toByte()
            header[37] = 'a'.code.toByte()
            header[38] = 't'.code.toByte()
            header[39] = 'a'.code.toByte()
            header[40] = (rawData.size and 0xff).toByte()
            header[41] = ((rawData.size shr 8) and 0xff).toByte()
            header[42] = ((rawData.size shr 16) and 0xff).toByte()
            header[43] = ((rawData.size shr 24) and 0xff).toByte()

            out.write(header)
            out.write(rawData)
        }

        return file
    }
}
