package com.example.xabarsos.network

import android.util.Log
import com.example.xabarsos.model.MessageChannel
import com.example.xabarsos.model.SosMessage
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.util.concurrent.TimeUnit

sealed class ConnectionStatus {
    object Disconnected : ConnectionStatus()
    object Connecting : ConnectionStatus()
    object Connected : ConnectionStatus()
    data class Error(val message: String) : ConnectionStatus()
}

class WebSocketSosManager(
    private var serverBaseUrl: String = "https://xabar-sos.onrender.com"
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.SECONDS) // WebSocket keeps connection alive
        .writeTimeout(10, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()
    private var webSocket: WebSocket? = null
    private val scope = CoroutineScope(Dispatchers.IO + Job())

    private val _connectionStatus = MutableStateFlow<ConnectionStatus>(ConnectionStatus.Disconnected)
    val connectionStatus: StateFlow<ConnectionStatus> = _connectionStatus.asStateFlow()

    private var onMessageReceivedListener: ((SosMessage) -> Unit)? = null

    fun setOnMessageReceivedListener(listener: (SosMessage) -> Unit) {
        onMessageReceivedListener = listener
    }

    fun updateServerUrl(url: String) {
        var formattedUrl = url.trim()
        if (!formattedUrl.startsWith("http://") && !formattedUrl.startsWith("https://")) {
            formattedUrl = "https://$formattedUrl"
        }
        if (formattedUrl.endsWith("/")) {
            formattedUrl = formattedUrl.dropLast(1)
        }
        serverBaseUrl = formattedUrl
        reconnect()
    }

    fun getServerUrl(): String = serverBaseUrl

    fun connect() {
        if (_connectionStatus.value == ConnectionStatus.Connected) return

        _connectionStatus.value = ConnectionStatus.Connecting

        val wsUrl = serverBaseUrl
            .replace("https://", "wss://")
            .replace("http://", "ws://") + "/ws"

        val request = Request.Builder()
            .url(wsUrl)
            .build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d("WebSocketSosManager", "WebSocket Connected to $wsUrl")
                _connectionStatus.value = ConnectionStatus.Connected
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                Log.d("WebSocketSosManager", "Message received: $text")
                try {
                    val messageObj = gson.fromJson(text, JsonObject::class.java)
                    val sosMessage = SosMessage(
                        id = messageObj.get("id")?.asString ?: java.util.UUID.randomUUID().toString(),
                        senderName = messageObj.get("senderName")?.asString ?: "Noma'lum",
                        messageText = messageObj.get("messageText")?.asString ?: "SOS!",
                        timestamp = messageObj.get("timestamp")?.asLong ?: System.currentTimeMillis(),
                        channel = MessageChannel.INTERNET,
                        isIncoming = true
                    )
                    onMessageReceivedListener?.invoke(sosMessage)
                } catch (e: Exception) {
                    Log.e("WebSocketSosManager", "Error parsing WebSocket message: ${e.message}")
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e("WebSocketSosManager", "WebSocket Failure: ${t.message}")
                _connectionStatus.value = ConnectionStatus.Error(t.message ?: "Ulanish xatosi")
                scheduleReconnect()
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Log.d("WebSocketSosManager", "WebSocket Closed: $reason")
                _connectionStatus.value = ConnectionStatus.Disconnected
                scheduleReconnect()
            }
        })
    }

    private fun scheduleReconnect() {
        scope.launch {
            delay(5000) // Reconnect after 5 seconds
            if (_connectionStatus.value !is ConnectionStatus.Connected) {
                connect()
            }
        }
    }

    fun reconnect() {
        disconnect()
        connect()
    }

    fun disconnect() {
        webSocket?.close(1000, "Normal closure")
        webSocket = null
        _connectionStatus.value = ConnectionStatus.Disconnected
    }

    fun sendSosMessage(message: SosMessage): Boolean {
        val messageJson = gson.toJson(
            mapOf(
                "id" to message.id,
                "senderName" to message.senderName,
                "messageText" to message.messageText,
                "timestamp" to message.timestamp
            )
        )

        // 1. Try WebSocket sending if connected
        val wsSent = webSocket?.send(messageJson) == true

        // 2. HTTP REST Broadcast fallback in background
        scope.launch {
            try {
                val httpUrl = "$serverBaseUrl/api/sos"
                val body = messageJson.toRequestBody("application/json; charset=utf-8".toMediaType())
                val request = Request.Builder()
                    .url(httpUrl)
                    .post(body)
                    .build()
                client.newCall(request).execute().use { response ->
                    Log.d("WebSocketSosManager", "HTTP POST fallback result: ${response.code}")
                }
            } catch (e: Exception) {
                Log.e("WebSocketSosManager", "HTTP POST broadcast error: ${e.message}")
            }
        }

        return wsSent
    }
}
