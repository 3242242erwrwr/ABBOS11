package com.example.xabarsos.audio

import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.media.Ringtone
import android.media.RingtoneManager
import android.os.Build
import android.util.Base64
import android.util.Log
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

enum class CallState {
    IDLE,
    OUTGOING_RINGING,
    INCOMING_RINGING,
    CONNECTED,
    ENDED
}

data class CallSession(
    val callId: String,
    val peerName: String,
    val peerDeviceId: String,
    val isIncoming: Boolean
)

object G711Codec {
    private const val BIAS = 0x84
    private const val CLIP = 32635

    private val uLawToPcmMap = ShortArray(256)

    init {
        for (i in 0 until 256) {
            uLawToPcmMap[i] = decodeULawSample(i.toByte())
        }
    }

    fun encodeULaw(pcm: ShortArray, count: Int): ByteArray {
        val uLaw = ByteArray(count)
        for (i in 0 until count) {
            uLaw[i] = encodeULawSample(pcm[i])
        }
        return uLaw
    }

    fun decodeULaw(uLaw: ByteArray, count: Int): ShortArray {
        val pcm = ShortArray(count)
        for (i in 0 until count) {
            pcm[i] = uLawToPcmMap[uLaw[i].toInt() and 0xFF]
        }
        return pcm
    }

    private fun encodeULawSample(sample: Short): Byte {
        var pcmSample = sample.toInt()
        val sign = (pcmSample shr 8) and 0x80
        if (sign != 0) pcmSample = -pcmSample
        if (pcmSample > CLIP) pcmSample = CLIP
        pcmSample += BIAS
        var exponent = 7
        var mask = 0x4000
        while ((pcmSample and mask) == 0 && exponent > 0) {
            exponent--
            mask = mask shr 1
        }
        val mantissa = (pcmSample shr (exponent + 3)) and 0x0F
        val uLaw = (sign or (exponent shl 4) or mantissa).inv()
        return uLaw.toByte()
    }

    private fun decodeULawSample(uLawByte: Byte): Short {
        val uLaw = uLawByte.toInt().inv()
        val sign = uLaw and 0x80
        val exponent = (uLaw shr 4) and 0x07
        val mantissa = uLaw and 0x0F
        var sample = ((mantissa shl 3) + 0x84) shl exponent
        sample -= BIAS
        return (if (sign != 0) -sample else sample).toShort()
    }
}

class VoiceCallManager(private val context: Context) {

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val scope = CoroutineScope(Dispatchers.IO + Job())

    private val _callState = MutableStateFlow(CallState.IDLE)
    val callState: StateFlow<CallState> = _callState.asStateFlow()

    private val _currentSession = MutableStateFlow<CallSession?>(null)
    val currentSession: StateFlow<CallSession?> = _currentSession.asStateFlow()

    private val _isMuted = MutableStateFlow(false)
    val isMuted: StateFlow<Boolean> = _isMuted.asStateFlow()

    private val _isSpeakerOn = MutableStateFlow(true)
    val isSpeakerOn: StateFlow<Boolean> = _isSpeakerOn.asStateFlow()

    private val _callDurationSeconds = MutableStateFlow(0)
    val callDurationSeconds: StateFlow<Int> = _callDurationSeconds.asStateFlow()

    @Volatile
    private var audioRecord: AudioRecord? = null
    @Volatile
    private var audioTrack: AudioTrack? = null
    private val isRecording = AtomicBoolean(false)
    private val isPlaying = AtomicBoolean(false)

    private var incomingRingtone: Ringtone? = null
    private var durationTimerJob: Job? = null
    private var sendAudioChunkListener: ((String) -> Unit)? = null

    companion object {
        private const val CHANNEL_IN = AudioFormat.CHANNEL_IN_MONO
        private const val CHANNEL_OUT = AudioFormat.CHANNEL_OUT_MONO
        private const val ENCODING = AudioFormat.ENCODING_PCM_16BIT
    }

    fun setSendAudioChunkListener(listener: (String) -> Unit) {
        sendAudioChunkListener = listener
    }

    fun startOutgoingCall(callId: String, friendName: String, friendDeviceId: String) {
        try {
            _currentSession.value = CallSession(
                callId = callId,
                peerName = friendName,
                peerDeviceId = friendDeviceId,
                isIncoming = false
            )
            _callState.value = CallState.OUTGOING_RINGING
        } catch (t: Throwable) {
            Log.e("VoiceCallManager", "Error starting outgoing call: ${t.message}")
        }
    }

    fun receiveIncomingCall(callId: String, callerName: String, callerDeviceId: String) {
        try {
            if (_callState.value != CallState.IDLE) return
            _currentSession.value = CallSession(
                callId = callId,
                peerName = callerName,
                peerDeviceId = callerDeviceId,
                isIncoming = true
            )
            _callState.value = CallState.INCOMING_RINGING
            playRingtone()
        } catch (t: Throwable) {
            Log.e("VoiceCallManager", "Error receiving incoming call: ${t.message}")
        }
    }

    fun acceptIncomingCall() {
        try {
            stopRingtone()
            _callState.value = CallState.CONNECTED
            startAudioStream()
            startCallTimer()
        } catch (t: Throwable) {
            Log.e("VoiceCallManager", "Error accepting incoming call: ${t.message}")
        }
    }

    fun onCallAcceptedByPeer() {
        try {
            stopRingtone()
            _callState.value = CallState.CONNECTED
            startAudioStream()
            startCallTimer()
        } catch (t: Throwable) {
            Log.e("VoiceCallManager", "Error handling peer call acceptance: ${t.message}")
        }
    }

    fun rejectOrEndCall() {
        try {
            stopRingtone()
            stopAudioStream()
            stopCallTimer()
            _callState.value = CallState.ENDED
            scope.launch {
                kotlinx.coroutines.delay(800)
                _callState.value = CallState.IDLE
                _currentSession.value = null
                _callDurationSeconds.value = 0
                _isMuted.value = false
                _isSpeakerOn.value = true
            }
        } catch (t: Throwable) {
            _callState.value = CallState.IDLE
            _currentSession.value = null
        }
    }

    fun toggleMute() {
        _isMuted.value = !_isMuted.value
    }

    fun toggleSpeaker() {
        try {
            val newSpeakerState = !_isSpeakerOn.value
            _isSpeakerOn.value = newSpeakerState
            audioManager.isSpeakerphoneOn = newSpeakerState
            if (newSpeakerState) {
                audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
            }
        } catch (t: Throwable) {
            Log.e("VoiceCallManager", "Error toggling speaker: ${t.message}")
        }
    }

    private fun playRingtone() {
        try {
            val ringtoneUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            if (ringtoneUri != null) {
                incomingRingtone = RingtoneManager.getRingtone(context, ringtoneUri)
                incomingRingtone?.play()
            }
        } catch (t: Throwable) {
            Log.e("VoiceCallManager", "Error playing ringtone: ${t.message}")
        }
    }

    private fun stopRingtone() {
        try {
            incomingRingtone?.stop()
            incomingRingtone = null
        } catch (t: Throwable) {}
    }

    private fun startCallTimer() {
        _callDurationSeconds.value = 0
        durationTimerJob?.cancel()
        durationTimerJob = scope.launch {
            while (_callState.value == CallState.CONNECTED) {
                kotlinx.coroutines.delay(1000)
                _callDurationSeconds.value += 1
            }
        }
    }

    private fun stopCallTimer() {
        try {
            durationTimerJob?.cancel()
        } catch (t: Throwable) {}
    }

    @Synchronized
    private fun startAudioStream() {
        try {
            // Auto Boost In-Call and Music Volumes to 100% Maximum
            try {
                val maxCallVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_VOICE_CALL)
                audioManager.setStreamVolume(AudioManager.STREAM_VOICE_CALL, maxCallVol, 0)

                val maxMusicVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, maxMusicVol, 0)

                audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
                audioManager.isSpeakerphoneOn = _isSpeakerOn.value
            } catch (t: Throwable) {
                Log.e("VoiceCallManager", "Error boosting volume: ${t.message}")
            }

            // Determine Hardware Supported Sample Rate (16000Hz or 8000Hz)
            var sampleRate = 16000
            var minRecSize = AudioRecord.getMinBufferSize(sampleRate, CHANNEL_IN, ENCODING)
            if (minRecSize <= 0) {
                sampleRate = 8000
                minRecSize = AudioRecord.getMinBufferSize(sampleRate, CHANNEL_IN, ENCODING)
            }
            val frameSizeShorts = if (sampleRate == 16000) 640 else 320

            // 1. Create AudioRecord (MIC / VOICE_RECOGNITION Primary)
            if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                if (minRecSize > 0) {
                    try {
                        audioRecord = AudioRecord(
                            MediaRecorder.AudioSource.MIC,
                            sampleRate,
                            CHANNEL_IN,
                            ENCODING,
                            maxOf(minRecSize * 2, frameSizeShorts * 4)
                        )
                    } catch (t: Throwable) {
                        try {
                            audioRecord = AudioRecord(
                                MediaRecorder.AudioSource.VOICE_COMMUNICATION,
                                sampleRate,
                                CHANNEL_IN,
                                ENCODING,
                                maxOf(minRecSize * 2, frameSizeShorts * 4)
                            )
                        } catch (t2: Throwable) {
                            Log.e("VoiceCallManager", "Error creating AudioRecord fallback: ${t2.message}")
                        }
                    }
                }
            }

            if (audioRecord?.state == AudioRecord.STATE_INITIALIZED) {
                try {
                    audioRecord?.startRecording()
                    isRecording.set(true)
                } catch (t: Throwable) {
                    Log.e("VoiceCallManager", "Error startRecording: ${t.message}")
                }
            }

            // 2. Create Low-Latency AudioTrack
            var minTrackSize = AudioTrack.getMinBufferSize(sampleRate, CHANNEL_OUT, ENCODING)
            if (minTrackSize <= 0) {
                minTrackSize = AudioTrack.getMinBufferSize(8000, CHANNEL_OUT, ENCODING)
            }

            if (minTrackSize > 0) {
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        audioTrack = AudioTrack.Builder()
                            .setAudioAttributes(
                                AudioAttributes.Builder()
                                    .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                                    .build()
                            )
                            .setAudioFormat(
                                AudioFormat.Builder()
                                    .setEncoding(ENCODING)
                                    .setSampleRate(sampleRate)
                                    .setChannelMask(CHANNEL_OUT)
                                    .build()
                            )
                            .setBufferSizeInBytes(maxOf(minTrackSize * 2, frameSizeShorts * 4))
                            .setPerformanceMode(AudioTrack.PERFORMANCE_MODE_LOW_LATENCY)
                            .setTransferMode(AudioTrack.MODE_STREAM)
                            .build()
                    } else {
                        @Suppress("DEPRECATION")
                        audioTrack = AudioTrack(
                            AudioManager.STREAM_VOICE_CALL,
                            sampleRate,
                            CHANNEL_OUT,
                            ENCODING,
                            maxOf(minTrackSize * 2, frameSizeShorts * 4),
                            AudioTrack.MODE_STREAM
                        )
                    }
                } catch (t: Throwable) {
                    try {
                        @Suppress("DEPRECATION")
                        audioTrack = AudioTrack(
                            AudioManager.STREAM_VOICE_CALL,
                            8000,
                            CHANNEL_OUT,
                            ENCODING,
                            maxOf(8000, frameSizeShorts * 4),
                            AudioTrack.MODE_STREAM
                        )
                    } catch (t2: Throwable) {
                        Log.e("VoiceCallManager", "Error creating AudioTrack fallback: ${t2.message}")
                    }
                }
            }

            if (audioTrack?.state == AudioTrack.STATE_INITIALIZED) {
                try {
                    audioTrack?.play()
                    isPlaying.set(true)
                } catch (t: Throwable) {
                    Log.e("VoiceCallManager", "Error playing AudioTrack: ${t.message}")
                }
            }

            // 3. Audio Recording Thread (With 2.5x Voice Gain Amplification)
            scope.launch(Dispatchers.IO) {
                val shortBuffer = ShortArray(frameSizeShorts)
                while (isRecording.get() && _callState.value == CallState.CONNECTED) {
                    try {
                        val currentRec = audioRecord
                        if (currentRec != null && currentRec.state == AudioRecord.STATE_INITIALIZED) {
                            val readShorts = currentRec.read(shortBuffer, 0, frameSizeShorts)
                            if (readShorts > 0 && !_isMuted.value) {
                                // Apply 2.5x Voice Gain Amplification for loud, crisp voice
                                for (i in 0 until readShorts) {
                                    val amp = (shortBuffer[i] * 2.5f).toInt()
                                    shortBuffer[i] = amp.coerceIn(-32768, 32767).toShort()
                                }
                                val g711Bytes = G711Codec.encodeULaw(shortBuffer, readShorts)
                                val base64Chunk = Base64.encodeToString(g711Bytes, Base64.NO_WRAP)
                                sendAudioChunkListener?.invoke(base64Chunk)
                            }
                        }
                    } catch (t: Throwable) {
                        Log.e("VoiceCallManager", "Error reading AudioRecord: ${t.message}")
                    }
                }
            }
        } catch (t: Throwable) {
            Log.e("VoiceCallManager", "Error starting audio stream: ${t.message}")
        }
    }

    fun onAudioChunkReceived(base64Data: String) {
        if (_callState.value == CallState.CONNECTED && isPlaying.get()) {
            try {
                val currentTrack = audioTrack
                if (currentTrack != null && currentTrack.state == AudioTrack.STATE_INITIALIZED && base64Data.isNotBlank()) {
                    val g711Bytes = Base64.decode(base64Data, Base64.NO_WRAP)
                    val pcmShorts = G711Codec.decodeULaw(g711Bytes, g711Bytes.size)
                    currentTrack.write(pcmShorts, 0, pcmShorts.size)
                }
            } catch (t: Throwable) {
                Log.e("VoiceCallManager", "Error playing audio chunk: ${t.message}")
            }
        }
    }

    @Synchronized
    private fun stopAudioStream() {
        isRecording.set(false)
        isPlaying.set(false)

        try {
            audioRecord?.stop()
            audioRecord?.release()
        } catch (t: Throwable) {}
        audioRecord = null

        try {
            audioTrack?.stop()
            audioTrack?.release()
        } catch (t: Throwable) {}
        audioTrack = null

        try {
            audioManager.mode = AudioManager.MODE_NORMAL
            audioManager.isSpeakerphoneOn = false
        } catch (t: Throwable) {}
    }
}
