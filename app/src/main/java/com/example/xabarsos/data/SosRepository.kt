package com.example.xabarsos.data

import android.content.Context
import com.example.xabarsos.audio.SosAlertManager
import com.example.xabarsos.audio.SosSoundType
import com.example.xabarsos.audio.VoiceCallManager
import com.example.xabarsos.audio.WebRtcCallManager
import com.example.xabarsos.bluetooth.BluetoothSosManager
import com.example.xabarsos.model.MessageChannel
import com.example.xabarsos.model.SosMessage
import com.example.xabarsos.network.WebSocketSosManager
import com.example.xabarsos.notification.SosNotificationManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

class SosRepository(private val context: Context) {

    companion object {
        @Volatile
        private var INSTANCE: SosRepository? = null

        fun getInstance(context: Context): SosRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SosRepository(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    private val prefs = context.getSharedPreferences("xabar_sos_prefs", Context.MODE_PRIVATE)

    val alertManager = SosAlertManager(context)
    val notificationManager = SosNotificationManager(context)
    val webSocketManager = WebSocketSosManager(getServerUrl())
    val bluetoothManager = BluetoothSosManager(context)
    val webRtcCallManager = WebRtcCallManager(context)
    val voiceCallManager = VoiceCallManager(context)

    private val _messages = MutableStateFlow<List<SosMessage>>(emptyList())
    val messages: StateFlow<List<SosMessage>> = _messages.asStateFlow()

    private val _activeIncomingAlert = MutableStateFlow<SosMessage?>(null)
    val activeIncomingAlert: StateFlow<SosMessage?> = _activeIncomingAlert.asStateFlow()

    private val _userName = MutableStateFlow(getUserName())
    val userName: StateFlow<String> = _userName.asStateFlow()

    private val _serverUrl = MutableStateFlow(getServerUrl())
    val serverUrl: StateFlow<String> = _serverUrl.asStateFlow()

    private val _soundType = MutableStateFlow(getSoundType())
    val soundType: StateFlow<SosSoundType> = _soundType.asStateFlow()

    private val _friendsList = MutableStateFlow(getFriendsList())
    val friendsList: StateFlow<List<String>> = _friendsList.asStateFlow()

    private val processedMessageIds = HashSet<String>()
    @Volatile
    private var lastMutedTimestamp: Long = 0L
    @Volatile
    private var lastClearedTimestamp: Long = prefs.getLong("last_cleared_ts", 0L)

    init {
        // Setup Dual Audio Stream Listener
        voiceCallManager.setSendAudioChunkListener { base64Audio ->
            val session = voiceCallManager.currentSession.value ?: webRtcCallManager.currentSession.value
            if (session != null) {
                webSocketManager.sendCustomJson(
                    mapOf(
                        "type" to "voice_audio",
                        "callId" to session.callId,
                        "targetRecipient" to session.peerName,
                        "senderName" to getUserName(),
                        "senderDeviceId" to getDeviceId(),
                        "audioData" to base64Audio
                    )
                )
            }
        }

        // Setup WebRTC signaling listener
        webRtcCallManager.setSendSignalingListener { map ->
            val completeMap = map.toMutableMap()
            completeMap["senderName"] = getUserName()
            completeMap["senderDeviceId"] = getDeviceId()
            webSocketManager.sendCustomJson(completeMap)
        }

        // Setup listener for custom signaling (WebRTC + Cloud WebSocket Audio Relay)
        webSocketManager.setOnCustomJsonReceivedListener { jsonObj ->
            try {
                if (jsonObj.has("type") && !jsonObj.get("type").isJsonNull) {
                    val type = jsonObj.get("type").asString
                    val myName = getUserName().trim()
                    val target = if (jsonObj.has("targetRecipient") && !jsonObj.get("targetRecipient").isJsonNull) {
                        jsonObj.get("targetRecipient").asString.trim()
                    } else ""

                    val isForMe = target.equals("BARCHAGA", ignoreCase = true)
                            || target.equals("ALL", ignoreCase = true)
                            || target.isEmpty()
                            || target.equals(myName, ignoreCase = true)
                            || myName.contains(target, ignoreCase = true)

                    if (isForMe) {
                        when (type) {
                            "call_offer" -> {
                                val callId = if (jsonObj.has("callId") && !jsonObj.get("callId").isJsonNull) jsonObj.get("callId").asString else ""
                                val callerName = if (jsonObj.has("senderName") && !jsonObj.get("senderName").isJsonNull) jsonObj.get("senderName").asString else "Noma'lum"
                                val callerDeviceId = if (jsonObj.has("senderDeviceId") && !jsonObj.get("senderDeviceId").isJsonNull) jsonObj.get("senderDeviceId").asString else ""
                                if (callerDeviceId != getDeviceId()) {
                                    webRtcCallManager.receiveIncomingCall(callId, callerName, callerDeviceId)
                                    voiceCallManager.receiveIncomingCall(callId, callerName, callerDeviceId)
                                }
                            }
                            "call_answer" -> {
                                webRtcCallManager.onCallAcceptedByPeer()
                                voiceCallManager.onCallAcceptedByPeer()
                            }
                            "call_reject", "call_hangup" -> {
                                webRtcCallManager.rejectOrEndCall()
                                voiceCallManager.rejectOrEndCall()
                            }
                            "webrtc_offer" -> {
                                val sdp = if (jsonObj.has("sdp") && !jsonObj.get("sdp").isJsonNull) jsonObj.get("sdp").asString else ""
                                if (sdp.isNotEmpty()) {
                                    webRtcCallManager.onWebRtcOfferReceived(sdp)
                                }
                            }
                            "webrtc_answer" -> {
                                val sdp = if (jsonObj.has("sdp") && !jsonObj.get("sdp").isJsonNull) jsonObj.get("sdp").asString else ""
                                if (sdp.isNotEmpty()) {
                                    webRtcCallManager.onWebRtcAnswerReceived(sdp)
                                }
                            }
                            "webrtc_ice" -> {
                                val sdpMid = if (jsonObj.has("sdpMid") && !jsonObj.get("sdpMid").isJsonNull) jsonObj.get("sdpMid").asString else ""
                                val sdpMLineIndex = if (jsonObj.has("sdpMLineIndex") && !jsonObj.get("sdpMLineIndex").isJsonNull) jsonObj.get("sdpMLineIndex").asInt else 0
                                val candidate = if (jsonObj.has("candidate") && !jsonObj.get("candidate").isJsonNull) jsonObj.get("candidate").asString else ""
                                if (candidate.isNotEmpty()) {
                                    webRtcCallManager.onIceCandidateReceived(sdpMid, sdpMLineIndex, candidate)
                                }
                            }
                            "voice_audio" -> {
                                val audioData = if (jsonObj.has("audioData") && !jsonObj.get("audioData").isJsonNull) jsonObj.get("audioData").asString else ""
                                val callerDeviceId = if (jsonObj.has("senderDeviceId") && !jsonObj.get("senderDeviceId").isJsonNull) jsonObj.get("senderDeviceId").asString else ""
                                if (callerDeviceId != getDeviceId() && audioData.isNotEmpty()) {
                                    voiceCallManager.onAudioChunkReceived(audioData)
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("SosRepository", "Error handling WebRTC signaling: ${e.message}")
            }
        }

        // Setup listener for WebSocket messages
        webSocketManager.setOnMessageReceivedListener { sosMessage ->
            processIncomingSosMessage(sosMessage)
        }

        // Setup listener for Bluetooth messages
        bluetoothManager.setOnMessageReceivedListener { sosMessage ->
            processIncomingSosMessage(sosMessage)
        }

        // Connect WebSocket and Bluetooth Scan
        webSocketManager.connect()
        bluetoothManager.startListeningForNearbySos()
    }

    fun getDeviceId(): String {
        var devId = prefs.getString("device_id", null)
        if (devId.isNullOrEmpty()) {
            devId = UUID.randomUUID().toString()
            prefs.edit().putString("device_id", devId).apply()
        }
        return devId
    }

    @Synchronized
    fun processIncomingSosMessage(sosMessage: SosMessage) {
        // Ignore messages older than last cleared timestamp
        if (sosMessage.timestamp <= lastClearedTimestamp) {
            return
        }

        // Check if message ID was ALREADY processed
        if (processedMessageIds.contains(sosMessage.id)) {
            return
        }
        processedMessageIds.add(sosMessage.id)

        val currentList = _messages.value.toMutableList()
        val myDeviceId = getDeviceId()
        val myName = getUserName().trim()

        // Check if message is a STOP signal
        val isStopSignal = sosMessage.messageText.contains("STOP", ignoreCase = true)
                || sosMessage.messageText.contains("BEKOR QILINDI", ignoreCase = true)

        if (isStopSignal) {
            lastMutedTimestamp = maxOf(lastMutedTimestamp, sosMessage.timestamp, System.currentTimeMillis())
            dismissActiveAlert()

            if (currentList.none { it.id == sosMessage.id }) {
                currentList.add(0, sosMessage)
                _messages.value = currentList
            }
            return // DO NOT PLAY SOUND OR NOTIFICATION FOR STOP SIGNAL!
        }

        // 1. IF THIS MESSAGE WAS SENT FROM THIS EXACT PHYSICAL DEVICE ID, DO NOT PLAY ALARM OR NOTIFICATION!
        if (sosMessage.deviceId.isNotBlank() && sosMessage.deviceId == myDeviceId) {
            if (currentList.none { it.id == sosMessage.id }) {
                currentList.add(0, sosMessage.copy(isIncoming = false))
                _messages.value = currentList
            }
            return
        }

        // 2. INCOMING MESSAGE FROM ANOTHER DEVICE
        if (currentList.none { it.id == sosMessage.id }) {
            currentList.add(0, sosMessage)
            _messages.value = currentList

            // Ignore old messages created BEFORE last STOP/mute timestamp!
            if (sosMessage.timestamp <= lastMutedTimestamp) {
                return
            }

            val target = sosMessage.targetRecipient.trim()
            val isForMe = target.equals("BARCHAGA", ignoreCase = true)
                    || target.equals("ALL", ignoreCase = true)
                    || target.isEmpty()
                    || target.equals(myName, ignoreCase = true)
                    || myName.contains(target, ignoreCase = true)
                    || target.contains(myName, ignoreCase = true)

            if (isForMe) {
                _activeIncomingAlert.value = sosMessage
                alertManager.playAlertSoundAndVibrate(_soundType.value)
                notificationManager.showHeadsUpSosNotification(sosMessage)
            }
        }
    }

    fun dismissActiveAlert() {
        lastMutedTimestamp = System.currentTimeMillis()
        _activeIncomingAlert.value = null
        SosAlertManager.stopAllAlerts()
        notificationManager.cancelEmergencyNotification()
    }

    fun sendSos(messageText: String, targetRecipient: String = "BARCHAGA") {
        val currentSender = getUserName()
        val myDeviceId = getDeviceId()

        val isStop = messageText.contains("STOP", ignoreCase = true) || messageText.contains("BEKOR QILINDI", ignoreCase = true)
        val formattedTarget = if (isStop) "BARCHAGA" else targetRecipient.trim().ifEmpty { "BARCHAGA" }

        val sosMessage = SosMessage(
            senderName = currentSender,
            deviceId = myDeviceId,
            messageText = messageText,
            targetRecipient = formattedTarget,
            channel = MessageChannel.INTERNET,
            isIncoming = false
        )

        // Mark own message ID as processed
        processedMessageIds.add(sosMessage.id)

        // 1. Add to local list
        val currentList = _messages.value.toMutableList()
        currentList.add(0, sosMessage)
        _messages.value = currentList

        // If sending STOP message, update mute timestamp locally & stop all sound
        if (isStop) {
            dismissActiveAlert()
        }

        // 2. Broadcast via WebSocket (Wi-Fi / 4G Internet)
        webSocketManager.sendSosMessage(sosMessage)

        // 3. Broadcast via Bluetooth LE (Offline local mesh)
        bluetoothManager.broadcastSosOffline(currentSender, myDeviceId, formattedTarget, messageText)
    }

    fun saveUserName(name: String) {
        val trimmed = name.trim().ifEmpty { "Foydalanuvchi" }
        prefs.edit().putString("user_name", trimmed).apply()
        _userName.value = trimmed
    }

    fun getUserName(): String {
        return prefs.getString("user_name", "SAIDBEK") ?: "SAIDBEK"
    }

    fun saveServerUrl(url: String) {
        val trimmed = url.trim().ifEmpty { "https://xabar-sos.onrender.com" }
        prefs.edit().putString("server_url", trimmed).apply()
        _serverUrl.value = trimmed
        webSocketManager.updateServerUrl(trimmed)
    }

    fun getServerUrl(): String {
        return prefs.getString("server_url", "https://xabar-sos.onrender.com") ?: "https://xabar-sos.onrender.com"
    }

    fun saveSoundType(type: SosSoundType) {
        prefs.edit().putString("sound_type", type.name).apply()
        _soundType.value = type
    }

    fun getSoundType(): SosSoundType {
        val savedName = prefs.getString("sound_type", SosSoundType.ALARM.name) ?: SosSoundType.ALARM.name
        return try {
            SosSoundType.valueOf(savedName)
        } catch (e: Exception) {
            SosSoundType.ALARM
        }
    }

    fun getFriendsList(): List<String> {
        val savedSet = prefs.getStringSet("friends_set", emptySet()) ?: emptySet()
        return savedSet.toList().sorted()
    }

    fun addFriend(name: String) {
        val trimmed = name.trim()
        if (trimmed.isNotBlank()) {
            val currentList = getFriendsList().toMutableList()
            if (!currentList.contains(trimmed)) {
                currentList.add(trimmed)
            }
            // SharedPreferences requires a NEW Set instance
            val newSet = HashSet(currentList)
            prefs.edit().remove("friends_set").apply() // Clear existing reference first
            prefs.edit().putStringSet("friends_set", newSet).apply()
            _friendsList.value = currentList.sorted()
        }
    }

    fun removeFriend(name: String) {
        val currentList = getFriendsList().toMutableList()
        currentList.remove(name)
        val newSet = HashSet(currentList)
        prefs.edit().remove("friends_set").apply()
        prefs.edit().putStringSet("friends_set", newSet).apply()
        _friendsList.value = currentList.sorted()
    }

    fun deleteMessageById(id: String) {
        val currentList = _messages.value.toMutableList()
        currentList.removeAll { it.id == id }
        _messages.value = currentList
    }

    fun startVoiceCall(peerName: String, peerDeviceId: String = "") {
        val callId = UUID.randomUUID().toString()
        webRtcCallManager.startOutgoingCall(callId, peerName, peerDeviceId)
        webSocketManager.sendCustomJson(
            mapOf(
                "type" to "call_offer",
                "callId" to callId,
                "targetRecipient" to peerName,
                "senderName" to getUserName(),
                "senderDeviceId" to getDeviceId()
            )
        )
    }

    fun acceptVoiceCall() {
        webRtcCallManager.acceptIncomingCall()
    }

    fun rejectOrEndVoiceCall() {
        webRtcCallManager.rejectOrEndCall()
    }

    fun clearHistory() {
        lastClearedTimestamp = System.currentTimeMillis()
        prefs.edit().putLong("last_cleared_ts", lastClearedTimestamp).apply()
        _messages.value = emptyList()
        processedMessageIds.clear()
    }
}
