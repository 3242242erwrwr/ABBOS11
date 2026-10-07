package com.example.xabarsos.audio

import android.content.Context
import android.media.AudioManager
import android.media.Ringtone
import android.media.RingtoneManager
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.webrtc.AudioSource
import org.webrtc.AudioTrack
import org.webrtc.DataChannel
import org.webrtc.IceCandidate
import org.webrtc.MediaConstraints
import org.webrtc.MediaStream
import org.webrtc.PeerConnection
import org.webrtc.PeerConnectionFactory
import org.webrtc.RtpReceiver
import org.webrtc.SdpObserver
import org.webrtc.SessionDescription

class WebRtcCallManager(private val context: Context) {

    private val scope = CoroutineScope(Dispatchers.IO + Job())
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

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

    private var factory: PeerConnectionFactory? = null
    private var peerConnection: PeerConnection? = null
    private var localAudioSource: AudioSource? = null
    private var localAudioTrack: AudioTrack? = null

    private var incomingRingtone: Ringtone? = null
    private var durationTimerJob: Job? = null

    private var sendSignalingListener: ((Map<String, Any>) -> Unit)? = null

    init {
        initWebRtcFactory()
    }

    fun setSendSignalingListener(listener: (Map<String, Any>) -> Unit) {
        sendSignalingListener = listener
    }

    @Synchronized
    private fun initWebRtcFactory() {
        if (factory != null) return
        try {
            val options = PeerConnectionFactory.InitializationOptions.builder(context.applicationContext)
                .setEnableInternalTracer(false)
                .createInitializationOptions()
            PeerConnectionFactory.initialize(options)

            factory = PeerConnectionFactory.builder()
                .setOptions(PeerConnectionFactory.Options())
                .createPeerConnectionFactory()
        } catch (t: Throwable) {
            Log.e("WebRtcCallManager", "Error initializing WebRTC Factory: ${t.message}")
        }
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

            createPeerConnection()
            createOffer()
        } catch (t: Throwable) {
            Log.e("WebRtcCallManager", "Error starting outgoing call: ${t.message}")
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
            Log.e("WebRtcCallManager", "Error receiving incoming call: ${t.message}")
        }
    }

    fun acceptIncomingCall() {
        try {
            stopRingtone()
            _callState.value = CallState.CONNECTED
            startCallTimer()

            createPeerConnection()

            sendSignalingListener?.invoke(
                mapOf(
                    "type" to "call_answer",
                    "callId" to (_currentSession.value?.callId ?: ""),
                    "targetRecipient" to (_currentSession.value?.peerName ?: "")
                )
            )
        } catch (t: Throwable) {
            Log.e("WebRtcCallManager", "Error accepting incoming call: ${t.message}")
        }
    }

    fun onCallAcceptedByPeer() {
        try {
            stopRingtone()
            _callState.value = CallState.CONNECTED
            startCallTimer()
        } catch (t: Throwable) {
            Log.e("WebRtcCallManager", "Error onCallAcceptedByPeer: ${t.message}")
        }
    }

    fun rejectOrEndCall() {
        try {
            stopRingtone()
            stopCallTimer()
            closePeerConnection()

            val session = _currentSession.value
            if (session != null) {
                sendSignalingListener?.invoke(
                    mapOf(
                        "type" to "call_hangup",
                        "callId" to session.callId,
                        "targetRecipient" to session.peerName
                    )
                )
            }

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
        localAudioTrack?.setEnabled(!_isMuted.value)
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
            Log.e("WebRtcCallManager", "Error toggling speaker: ${t.message}")
        }
    }

    private fun createPeerConnection() {
        if (peerConnection != null) return
        try {
            initWebRtcFactory()

            val iceServers = listOf<PeerConnection.IceServer>(
                PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer(),
                PeerConnection.IceServer.builder("stun:stun1.l.google.com:19302").createIceServer()
            )

            val rtcConfig = PeerConnection.RTCConfiguration(iceServers)

            peerConnection = factory?.createPeerConnection(rtcConfig, object : PeerConnection.Observer {
                override fun onSignalingChange(state: PeerConnection.SignalingState?) {}
                override fun onIceConnectionChange(state: PeerConnection.IceConnectionState?) {
                    Log.d("WebRtcCallManager", "ICE Connection State: $state")
                }
                override fun onIceConnectionReceivingChange(p0: Boolean) {}
                override fun onIceGatheringChange(state: PeerConnection.IceGatheringState?) {}
                override fun onIceCandidate(candidate: IceCandidate?) {
                    if (candidate != null) {
                        sendSignalingListener?.invoke(
                            mapOf(
                                "type" to "webrtc_ice",
                                "callId" to (_currentSession.value?.callId ?: ""),
                                "targetRecipient" to (_currentSession.value?.peerName ?: ""),
                                "sdpMid" to candidate.sdpMid,
                                "sdpMLineIndex" to candidate.sdpMLineIndex,
                                "candidate" to candidate.sdp
                            )
                        )
                    }
                }
                override fun onIceCandidatesRemoved(p0: Array<out IceCandidate>?) {}
                override fun onAddStream(stream: MediaStream?) {}
                override fun onRemoveStream(stream: MediaStream?) {}
                override fun onDataChannel(p0: DataChannel?) {}
                override fun onRenegotiationNeeded() {}
                override fun onAddTrack(receiver: RtpReceiver?, streams: Array<out MediaStream>?) {}
            })

            // Add local audio track
            val audioConstraints = MediaConstraints().apply {
                mandatory.add(MediaConstraints.KeyValuePair("googEchoCancellation", "true"))
                mandatory.add(MediaConstraints.KeyValuePair("googAutoGainControl", "true"))
                mandatory.add(MediaConstraints.KeyValuePair("googNoiseSuppression", "true"))
                mandatory.add(MediaConstraints.KeyValuePair("googHighpassFilter", "true"))
            }

            localAudioSource = factory?.createAudioSource(audioConstraints)
            localAudioTrack = factory?.createAudioTrack("ARDAM101", localAudioSource)
            localAudioTrack?.setEnabled(true)

            peerConnection?.addTrack(localAudioTrack, listOf("ARDAMS101"))

            // Audio Mode
            audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
            audioManager.isSpeakerphoneOn = _isSpeakerOn.value
        } catch (t: Throwable) {
            Log.e("WebRtcCallManager", "Error creating PeerConnection: ${t.message}")
        }
    }

    private fun createOffer() {
        val constraints = MediaConstraints().apply {
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
        }

        peerConnection?.createOffer(object : SdpObserver {
            override fun onCreateSuccess(desc: SessionDescription?) {
                if (desc != null) {
                    peerConnection?.setLocalDescription(object : SdpObserver {
                        override fun onCreateSuccess(p0: SessionDescription?) {}
                        override fun onSetSuccess() {
                            sendSignalingListener?.invoke(
                                mapOf(
                                    "type" to "webrtc_offer",
                                    "callId" to (_currentSession.value?.callId ?: ""),
                                    "targetRecipient" to (_currentSession.value?.peerName ?: ""),
                                    "sdp" to desc.description
                                )
                            )
                        }
                        override fun onCreateFailure(p0: String?) {}
                        override fun onSetFailure(p0: String?) {}
                    }, desc)
                }
            }
            override fun onSetSuccess() {}
            override fun onCreateFailure(p0: String?) {}
            override fun onSetFailure(p0: String?) {}
        }, constraints)
    }

    fun onWebRtcOfferReceived(sdp: String) {
        try {
            createPeerConnection()
            val desc = SessionDescription(SessionDescription.Type.OFFER, sdp)
            peerConnection?.setRemoteDescription(object : SdpObserver {
                override fun onCreateSuccess(p0: SessionDescription?) {}
                override fun onSetSuccess() {
                    createAnswer()
                }
                override fun onCreateFailure(p0: String?) {}
                override fun onSetFailure(p0: String?) {}
            }, desc)
        } catch (t: Throwable) {
            Log.e("WebRtcCallManager", "Error handling WebRTC offer: ${t.message}")
        }
    }

    private fun createAnswer() {
        val constraints = MediaConstraints().apply {
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
        }

        peerConnection?.createAnswer(object : SdpObserver {
            override fun onCreateSuccess(desc: SessionDescription?) {
                if (desc != null) {
                    peerConnection?.setLocalDescription(object : SdpObserver {
                        override fun onCreateSuccess(p0: SessionDescription?) {}
                        override fun onSetSuccess() {
                            sendSignalingListener?.invoke(
                                mapOf(
                                    "type" to "webrtc_answer",
                                    "callId" to (_currentSession.value?.callId ?: ""),
                                    "targetRecipient" to (_currentSession.value?.peerName ?: ""),
                                    "sdp" to desc.description
                                )
                            )
                        }
                        override fun onCreateFailure(p0: String?) {}
                        override fun onSetFailure(p0: String?) {}
                    }, desc)
                }
            }
            override fun onSetSuccess() {}
            override fun onCreateFailure(p0: String?) {}
            override fun onSetFailure(p0: String?) {}
        }, constraints)
    }

    fun onWebRtcAnswerReceived(sdp: String) {
        try {
            val desc = SessionDescription(SessionDescription.Type.ANSWER, sdp)
            peerConnection?.setRemoteDescription(object : SdpObserver {
                override fun onCreateSuccess(p0: SessionDescription?) {}
                override fun onSetSuccess() {}
                override fun onCreateFailure(p0: String?) {}
                override fun onSetFailure(p0: String?) {}
            }, desc)
        } catch (t: Throwable) {
            Log.e("WebRtcCallManager", "Error handling WebRTC answer: ${t.message}")
        }
    }

    fun onIceCandidateReceived(sdpMid: String, sdpMLineIndex: Int, candidateSdp: String) {
        try {
            val candidate = IceCandidate(sdpMid, sdpMLineIndex, candidateSdp)
            peerConnection?.addIceCandidate(candidate)
        } catch (t: Throwable) {
            Log.e("WebRtcCallManager", "Error adding IceCandidate: ${t.message}")
        }
    }

    private fun closePeerConnection() {
        try {
            peerConnection?.close()
            peerConnection = null

            localAudioTrack?.dispose()
            localAudioTrack = null

            localAudioSource?.dispose()
            localAudioSource = null

            audioManager.mode = AudioManager.MODE_NORMAL
            audioManager.isSpeakerphoneOn = false
        } catch (t: Throwable) {
            Log.e("WebRtcCallManager", "Error closing PeerConnection: ${t.message}")
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
            Log.e("WebRtcCallManager", "Error playing ringtone: ${t.message}")
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
}
