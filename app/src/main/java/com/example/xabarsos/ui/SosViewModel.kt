package com.example.xabarsos.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.xabarsos.data.SosRepository
import com.example.xabarsos.model.SosMessage
import com.example.xabarsos.network.ConnectionStatus
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class SosViewModel(application: Application) : AndroidViewModel(application) {

    val repository = SosRepository(application)

    val messages: StateFlow<List<SosMessage>> = repository.messages
    val activeIncomingAlert: StateFlow<SosMessage?> = repository.activeIncomingAlert
    val userName: StateFlow<String> = repository.userName
    val serverUrl: StateFlow<String> = repository.serverUrl
    val connectionStatus: StateFlow<ConnectionStatus> = repository.webSocketManager.connectionStatus
    val isScanningBluetooth: StateFlow<Boolean> = repository.bluetoothManager.isScanning

    fun sendSos(text: String) {
        if (text.isNotBlank()) {
            repository.sendSos(text)
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

    fun clearHistory() {
        repository.clearHistory()
    }
}
