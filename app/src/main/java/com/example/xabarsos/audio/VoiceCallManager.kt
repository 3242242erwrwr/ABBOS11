package com.example.xabarsos.audio

import android.content.Context
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.media.Ringtone
import android.media.RingtoneManager
import android.util.Base64
import android.util.Log
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

    private var audioRecord: AudioRecord? = null
    private var audioTrack: AudioTrack? = null
    private val isRecording = AtomicBoolean(false)
    private val isPlaying = AtomicBoolean(false)

    private var incomingRingtone: Ringtone? = null
    private var durationTimerJob: Job? = null
    private var sendAudioChunkListener: ((String) -> Unit)? = null

    companion object {
        private const val SAMPLE_RATE = 16000 // 16kHz for clear voice
        private const val CHANNEL_IN = AudioFormat.CHANNEL_IN_MONO
        private const val CHANNEL_OUT = AudioFormat.CHANNEL_OUT_MONO
        private const val ENCODING = AudioFormat.ENCODING_PCM_16BIT
    }

    fun setSendAudioChunkListener(listener: (String) -> Unit) {
        sendAudioChunkListener = listener
    }

    fun startOutgoingCall(callId: String, friendName: String, friendDeviceId: String) {
        _currentSession.value = CallSession(
            callId = callId,
            peerName = friendName,
            peerDeviceId = friendDeviceId,
            isIncoming = false
        )
        _callState.value = CallState.OUTGOING_RINGING
    }

    fun receiveIncomingCall(callId: String, callerName: String, callerDeviceId: String) {
        if (_callState.value != CallState.IDLE) {
            // Already in a call
            return
        }
        _currentSession.value = CallSession(
            callId = callId,
            peerName = callerName,
            peerDeviceId = callerDeviceId,
            isIncoming = true
        )
        _callState.value = CallState.INCOMING_RINGING
        playRingtone()
    }

    fun acceptIncomingCall() {
        stopRingtone()
        _callState.value = CallState.CONNECTED
        startAudioStream()
        startCallTimer()
    }

    fun onCallAcceptedByPeer() {
        stopRingtone()
        _callState.value = CallState.CONNECTED
        startAudioStream()
        startCallTimer()
    }

    fun rejectOrEndCall() {
        stopRingtone()
        stopAudioStream()
        stopCallTimer()
        _callState.value = CallState.ENDED
        scope.launch {
            kotlinx.coroutines.delay(1000)
            _callState.value = CallState.IDLE
            _currentSession.value = null
            _callDurationSeconds.value = 0
            _isMuted.value = false
            _isSpeakerOn.value = true
        }
    }

    fun toggleMute() {
        _isMuted.value = !_isMuted.value
    }

    fun toggleSpeaker() {
        val newSpeakerState = !_isSpeakerOn.value
        _isSpeakerOn.value = newSpeakerState
        try {
            audioManager.isSpeakerphoneOn = newSpeakerState
            if (newSpeakerState) {
                audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
            }
        } catch (e: Exception) {
            Log.e("VoiceCallManager", "Error toggling speaker: ${e.message}")
        }
    }

    private fun playRingtone() {
        try {
            val ringtoneUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            incomingRingtone = RingtoneManager.getRingtone(context, ringtoneUri)
            incomingRingtone?.play()
        } catch (e: Exception) {
            Log.e("VoiceCallManager", "Error playing ringtone: ${e.message}")
        }
    }

    private fun stopRingtone() {
        try {
            incomingRingtone?.stop()
            incomingRingtone = null
        } catch (e: Exception) {}
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
        durationTimerJob?.cancel()
    }

    @Synchronized
    private fun startAudioStream() {
        try {
            audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
            audioManager.isSpeakerphoneOn = _isSpeakerOn.value

            val minRecSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_IN, ENCODING)
            try {
                audioRecord = AudioRecord(
                    MediaRecorder.AudioSource.VOICE_COMMUNICATION,
                    SAMPLE_RATE,
                    CHANNEL_IN,
                    ENCODING,
                    minRecSize * 2
                )
            } catch (se: SecurityException) {
                Log.e("VoiceCallManager", "RECORD_AUDIO permission missing: ${se.message}")
                return
            }

            val minTrackSize = AudioTrack.getMinBufferSize(SAMPLE_RATE, CHANNEL_OUT, ENCODING)
            audioTrack = AudioTrack(
                AudioManager.STREAM_VOICE_CALL,
                SAMPLE_RATE,
                CHANNEL_OUT,
                ENCODING,
                minTrackSize * 2,
                AudioTrack.MODE_STREAM
            )

            audioTrack?.play()
            isPlaying.set(true)

            audioRecord?.startRecording()
            isRecording.set(true)

            // Audio Record Thread
            scope.launch(Dispatchers.IO) {
                val buffer = ByteArray(640) // 20ms chunks
                while (isRecording.get() && _callState.value == CallState.CONNECTED) {
                    val read = audioRecord?.read(buffer, 0, buffer.size) ?: 0
                    if (read > 0 && !_isMuted.value) {
                        val base64Chunk = Base64.encodeToString(buffer, 0, read, Base64.NO_WRAP)
                        sendAudioChunkListener?.invoke(base64Chunk)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("VoiceCallManager", "Error starting audio stream: ${e.message}")
        }
    }

    fun onAudioChunkReceived(base64Data: String) {
        if (_callState.value == CallState.CONNECTED && isPlaying.get()) {
            try {
                val pcmData = Base64.decode(base64Data, Base64.NO_WRAP)
                audioTrack?.write(pcmData, 0, pcmData.size)
            } catch (e: Exception) {
                Log.e("VoiceCallManager", "Error playing audio chunk: ${e.message}")
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
        } catch (e: Exception) {}
        audioRecord = null

        try {
            audioTrack?.stop()
            audioTrack?.release()
        } catch (e: Exception) {}
        audioTrack = null

        try {
            audioManager.mode = AudioManager.MODE_NORMAL
            audioManager.isSpeakerphoneOn = false
        } catch (e: Exception) {}
    }
}
