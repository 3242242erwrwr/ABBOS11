package com.example.xabarsos.model

import java.util.UUID

enum class MessageChannel(val displayName: String) {
    INTERNET("Wi-Fi / 4G (Internet)"),
    BLUETOOTH("Bluetooth (Oflayn)"),
    LOCAL("Mahalliy")
}

data class SosMessage(
    val id: String = UUID.randomUUID().toString(),
    val senderName: String,
    val messageText: String,
    val targetRecipient: String = "BARCHAGA",
    val timestamp: Long = System.currentTimeMillis(),
    val channel: MessageChannel = MessageChannel.INTERNET,
    val isIncoming: Boolean = true,
    val isUrgent: Boolean = true
)
