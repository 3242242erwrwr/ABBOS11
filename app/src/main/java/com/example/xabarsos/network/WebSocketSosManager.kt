package com.example.xabarsos.network

import android.util.Log
import com.example.xabarsos.model.MessageChannel
import com.example.xabarsos.model.SosMessage
import com.google.gson.Gson
import com.google.gson.JsonArray
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
    // Client for persistent WebSocket with proper 4G LTE timeouts
    private val client = OkHttpClient.Builder()
        .connectTimeout(6, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.SECONDS)
        .writeTimeout(6, TimeUnit.SECONDS)
        .pingInterval(4, TimeUnit.SECONDS) // Fast 4s Ping for 4G CGNAT NAT Keep-Alive
        .retryOnConnectionFailure(true)
        .build()

    // Client for HTTP REST requests & background polling on 4G
    private val fastHttpClient = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .writeTimeout(5, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    private val gson = Gson()
    private var webSocket: WebSocket? = null
    private val scope = CoroutineScope(Dispatchers.IO + Job())

    private val _connectionStatus = MutableStateFlow<ConnectionStatus>(ConnectionStatus.Disconnected)
    val connectionStatus: StateFlow<ConnectionStatus> = _connectionStatus.asStateFlow()

    private var onMessageReceivedListener: ((SosMessage) -> Unit)? = null

    init {
        startConnectionWatcher()
    }

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

    private fun startConnectionWatcher() {
        scope.launch {
            while (true) {
                delay(2000) // Every 2s verify connection is active
                if (_connectionStatus.value !is ConnectionStatus.Connected) {
                    connect()
                }
            }
        }
    }

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

                    // Skip server heartbeat ping frames
                    if (messageObj.has("type") && messageObj.get("type").asString == "ping") {
                        _connectionStatus.value = ConnectionStatus.Connected
                        return
                    }

                    if (messageObj.has("messageText") || messageObj.has("senderName")) {
                        val sosMessage = SosMessage(
                            id = messageObj.get("id")?.asString ?: java.util.UUID.randomUUID().toString(),
                            deviceId = messageObj.get("deviceId")?.asString ?: "",
                            senderName = messageObj.get("senderName")?.asString ?: "Noma'lum",
                            messageText = messageObj.get("messageText")?.asString ?: "SOS!",
                            targetRecipient = messageObj.get("targetRecipient")?.asString ?: "BARCHAGA",
                            timestamp = messageObj.get("timestamp")?.asLong ?: System.currentTimeMillis(),
                            channel = MessageChannel.INTERNET,
                            isIncoming = true
                        )
                        onMessageReceivedListener?.invoke(sosMessage)
                    }
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
            delay(1000) // Fast auto-reconnect after 1 second
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

    fun fetchRecentSosMessagesHttp(sinceTimestamp: Long, onResult: (List<SosMessage>) -> Unit) {
        scope.launch {
            try {
                val pollUrl = "$serverBaseUrl/api/sos/recent?since=$sinceTimestamp"
                val request = Request.Builder().url(pollUrl).get().build()
                fastHttpClient.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val bodyStr = response.body?.string() ?: ""
                        val jsonObj = gson.fromJson(bodyStr, JsonObject::class.java)
                        val messagesArray = jsonObj.getAsJsonArray("messages") ?: JsonArray()

                        val parsedList = mutableListOf<SosMessage>()
                        for (i in 0 until messagesArray.size()) {
                            val msgObj = messagesArray.get(i).asJsonObject
                            val sosMsg = SosMessage(
                                id = msgObj.get("id")?.asString ?: java.util.UUID.randomUUID().toString(),
                                deviceId = msgObj.get("deviceId")?.asString ?: "",
                                senderName = msgObj.get("senderName")?.asString ?: "Noma'lum",
                                messageText = msgObj.get("messageText")?.asString ?: "SOS!",
                                targetRecipient = msgObj.get("targetRecipient")?.asString ?: "BARCHAGA",
                                timestamp = msgObj.get("timestamp")?.asLong ?: System.currentTimeMillis(),
                                channel = MessageChannel.INTERNET,
                                isIncoming = true
                            )
                            parsedList.add(sosMsg)
                        }

                        if (parsedList.isNotEmpty()) {
                            _connectionStatus.value = ConnectionStatus.Connected
                            onResult(parsedList)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("WebSocketSosManager", "Error polling HTTP REST: ${e.message}")
            }
        }
    }

    fun sendSosMessage(message: SosMessage): Boolean {
        val messageJson = gson.toJson(
            mapOf(
                "id" to message.id,
                "deviceId" to message.deviceId,
                "senderName" to message.senderName,
                "messageText" to message.messageText,
                "targetRecipient" to message.targetRecipient,
                "timestamp" to message.timestamp
            )
        )

        // 1. Send via WebSocket
        val wsSent = webSocket?.send(messageJson) == true

        // 2. ALWAYS Send via HTTP REST with up to 3 Retries on 4G LTE
        scope.launch {
            var retries = 0
            var success = false
            while (retries < 3 && !success) {
                try {
                    val httpUrl = "$serverBaseUrl/api/sos"
                    val body = messageJson.toRequestBody("application/json; charset=utf-8".toMediaType())
                    val request = Request.Builder()
                        .url(httpUrl)
                        .post(body)
                        .build()
                    fastHttpClient.newCall(request).execute().use { response ->
                        if (response.isSuccessful) {
                            Log.d("WebSocketSosManager", "HTTP POST 4G broadcast succeeded on attempt ${retries + 1}")
                            _connectionStatus.value = ConnectionStatus.Connected
                            success = true
                        }
                    }
                } catch (e: Exception) {
                    Log.e("WebSocketSosManager", "HTTP POST 4G broadcast error attempt ${retries + 1}: ${e.message}")
                    retries++
                    delay(300) // Wait 300ms before retry
                }
            }
        }

        return wsSent
    }
}
