package com.example.xabarsos.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.example.xabarsos.audio.CallSession
import com.example.xabarsos.audio.CallState
import com.example.xabarsos.audio.SosSoundType
import com.example.xabarsos.data.SosRepository
import com.example.xabarsos.model.SosMessage
import com.example.xabarsos.network.ConnectionStatus
import kotlinx.coroutines.flow.StateFlow

class SosViewModel(application: Application) : AndroidViewModel(application) {

    val repository = SosRepository.getInstance(application)

    val messages: StateFlow<List<SosMessage>> = repository.messages
    val activeIncomingAlert: StateFlow<SosMessage?> = repository.activeIncomingAlert
    val userName: StateFlow<String> = repository.userName
    val serverUrl: StateFlow<String> = repository.serverUrl
    val soundType: StateFlow<SosSoundType> = repository.soundType
    val friendsList: StateFlow<List<String>> = repository.friendsList
    val connectionStatus: StateFlow<ConnectionStatus> = repository.webSocketManager.connectionStatus
    val isScanningBluetooth: StateFlow<Boolean> = repository.bluetoothManager.isScanning

    val callState: StateFlow<CallState> = repository.voiceCallManager.callState
    val currentCallSession: StateFlow<CallSession?> = repository.voiceCallManager.currentSession
    val isMuted: StateFlow<Boolean> = repository.voiceCallManager.isMuted
    val isSpeakerOn: StateFlow<Boolean> = repository.voiceCallManager.isSpeakerOn
    val callDurationSeconds: StateFlow<Int> = repository.voiceCallManager.callDurationSeconds

    fun startVoiceCall(peerName: String, peerDeviceId: String = "") {
        repository.startVoiceCall(peerName, peerDeviceId)
    }

    fun acceptVoiceCall() {
        repository.acceptVoiceCall()
    }

    fun rejectOrEndVoiceCall() {
        repository.rejectOrEndVoiceCall()
    }

    fun toggleMute() {
        repository.voiceCallManager.toggleMute()
    }

    fun toggleSpeaker() {
        repository.voiceCallManager.toggleSpeaker()
    }

    fun sendSos(text: String, targetRecipient: String = "BARCHAGA") {
        if (text.isNotBlank()) {
            repository.sendSos(text, targetRecipient)
        }
    }

    fun dismissActiveAlert() {
        repository.dismissActiveAlert()
    }

    fun updateUserName(name: String) {
        repository.saveUserName(name)
    }

    fun updateServerUrl(url: String) {
        repository.saveServerUrl(url)
    }

    fun updateSoundType(type: SosSoundType) {
        repository.saveSoundType(type)
    }

    fun testSound(type: SosSoundType) {
        repository.alertManager.testSound(type)
    }

    fun stopTestSound() {
        repository.alertManager.stopAlertSoundAndVibrate()
    }

    fun addFriend(name: String) {
        repository.addFriend(name)
    }

    fun removeFriend(name: String) {
        repository.removeFriend(name)
    }

    fun deleteMessageById(id: String) {
        repository.deleteMessageById(id)
    }

    fun clearHistory() {
        repository.clearHistory()
    }
}
