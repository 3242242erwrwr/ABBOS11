package com.example.xabarsos.data

import android.content.Context
import com.example.xabarsos.audio.SosAlertManager
import com.example.xabarsos.audio.SosSoundType
import com.example.xabarsos.bluetooth.BluetoothSosManager
import com.example.xabarsos.model.MessageChannel
import com.example.xabarsos.model.SosMessage
import com.example.xabarsos.network.WebSocketSosManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SosRepository(private val context: Context) {

    private val prefs = context.getSharedPreferences("xabar_sos_prefs", Context.MODE_PRIVATE)

    val alertManager = SosAlertManager(context)
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
            onNewSosReceived(sosMessage)
        }

        // Setup listener for Bluetooth messages
        bluetoothManager.setOnMessageReceivedListener { sosMessage ->
            onNewSosReceived(sosMessage)
        }

        // Connect WebSocket and Bluetooth Scan
        webSocketManager.connect()
        bluetoothManager.startListeningForNearbySos()
    }

    private fun onNewSosReceived(sosMessage: SosMessage) {
        val currentList = _messages.value.toMutableList()
        if (currentList.none { it.id == sosMessage.id }) {
            currentList.add(0, sosMessage)
            _messages.value = currentList

            // Check if this incoming SOS message is targeted to me or to EVERYONE
            if (sosMessage.isIncoming) {
                val myName = getUserName().trim()
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
                }
            }
        }
    }

    fun dismissActiveAlert() {
        _activeIncomingAlert.value = null
        alertManager.stopAlertSoundAndVibrate()
    }

    fun sendSos(messageText: String, targetRecipient: String = "BARCHAGA") {
        val currentSender = getUserName()
        val formattedTarget = targetRecipient.trim().ifEmpty { "BARCHAGA" }

        val sosMessage = SosMessage(
            senderName = currentSender,
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
        bluetoothManager.broadcastSosOffline(currentSender, formattedTarget, messageText)
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
        val savedSet = prefs.getStringSet("friends_set", null)
        return savedSet?.toList()?.sorted() ?: emptyList()
    }

    fun addFriend(name: String) {
        val trimmed = name.trim()
        if (trimmed.isNotBlank()) {
            val current = getFriendsList().toMutableSet()
            current.add(trimmed)
            prefs.edit().putStringSet("friends_set", current).apply()
            _friendsList.value = current.toList().sorted()
        }
    }

    fun removeFriend(name: String) {
        val current = getFriendsList().toMutableSet()
        current.remove(name)
        prefs.edit().putStringSet("friends_set", current).apply()
        _friendsList.value = current.toList().sorted()
    }

    fun clearHistory() {
        _messages.value = emptyList()
    }
}
