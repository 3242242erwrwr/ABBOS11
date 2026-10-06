package com.example.xabarsos.data

import android.content.Context
import com.example.xabarsos.audio.SosAlertManager
import com.example.xabarsos.audio.SosSoundType
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

    init {
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

    fun processIncomingSosMessage(sosMessage: SosMessage) {
        val currentList = _messages.value.toMutableList()
        val myDeviceId = getDeviceId()
        val myName = getUserName().trim()

        // Check if message is a STOP signal
        val isStopSignal = sosMessage.messageText.contains("STOP", ignoreCase = true)
                || sosMessage.messageText.contains("BEKOR QILINDI", ignoreCase = true)

        if (isStopSignal) {
            dismissActiveAlert()
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
        _activeIncomingAlert.value = null
        alertManager.stopAlertSoundAndVibrate()
        notificationManager.cancelEmergencyNotification()
    }

    fun sendSos(messageText: String, targetRecipient: String = "BARCHAGA") {
        val currentSender = getUserName()
        val myDeviceId = getDeviceId()
        val formattedTarget = targetRecipient.trim().ifEmpty { "BARCHAGA" }

        val sosMessage = SosMessage(
            senderName = currentSender,
            deviceId = myDeviceId,
            messageText = messageText,
            targetRecipient = formattedTarget,
            channel = MessageChannel.INTERNET,
            isIncoming = false
        )

        // 1. Add to local list
        val currentList = _messages.value.toMutableList()
        currentList.add(0, sosMessage)
        _messages.value = currentList

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

    fun clearHistory() {
        _messages.value = emptyList()
    }
}
