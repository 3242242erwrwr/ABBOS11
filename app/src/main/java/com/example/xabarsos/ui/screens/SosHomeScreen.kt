package com.example.xabarsos.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.BluetoothSearching
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.xabarsos.model.MessageChannel
import com.example.xabarsos.model.SosMessage
import com.example.xabarsos.network.ConnectionStatus
import com.example.xabarsos.ui.SosViewModel
import com.example.xabarsos.ui.components.QuickSosButtons
import com.example.xabarsos.ui.components.SettingsDialog
import com.example.xabarsos.ui.components.SosAlertBanner
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SosHomeScreen(
    viewModel: SosViewModel
) {
    val messages by viewModel.messages.collectAsState()
    val activeAlert by viewModel.activeIncomingAlert.collectAsState()
    val userName by viewModel.userName.collectAsState()
    val serverUrl by viewModel.serverUrl.collectAsState()
    val connectionStatus by viewModel.connectionStatus.collectAsState()
    val isScanningBluetooth by viewModel.isScanningBluetooth.collectAsState()

    var showSettingsDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "XABAR SOS",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                        Text(
                            text = "Ismingiz: $userName",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                },
                actions = {
                    // Internet Status Indicator Badge
                    val statusColor = when (connectionStatus) {
                        is ConnectionStatus.Connected -> Color(0xFF4CAF50)
                        is ConnectionStatus.Connecting -> Color(0xFFFF9800)
                        else -> Color(0xFFE53935)
                    }
                    Box(
                        modifier = Modifier
                            .padding(end = 4.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(statusColor.copy(alpha = 0.15f))
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (connectionStatus is ConnectionStatus.Connected) Icons.Default.Wifi else Icons.Default.WifiOff,
                                contentDescription = null,
                                tint = statusColor,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (connectionStatus is ConnectionStatus.Connected) "Internet" else "Oflayn",
                                color = statusColor,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Bluetooth Badge
                    Box(
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF2196F3).copy(alpha = 0.15f))
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (isScanningBluetooth) Icons.AutoMirrored.Filled.BluetoothSearching else Icons.Default.Bluetooth,
                                contentDescription = null,
                                tint = Color(0xFF2196F3),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Blutuz",
                                color = Color(0xFF2196F3),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Settings Button
                    IconButton(onClick = { showSettingsDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Sozlamalar"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Active Flashing SOS Alarm Banner
            activeAlert?.let { alert ->
                SosAlertBanner(
                    sosMessage = alert,
                    onDismiss = { viewModel.dismissActiveAlert() }
                )
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(top = 16.dp, bottom = 24.dp)
            ) {
                // Sender Name Quick Input Card
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)
                        ),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Sizning ismingiz:",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                                OutlinedTextField(
                                    value = userName,
                                    onValueChange = { viewModel.updateUserName(it) },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Preset Emergency SOS Buttons: "SAIDBEK QARA" & "JASMINAHON QANI"
                item {
                    QuickSosButtons(
                        onSendSos = { sosText ->
                            viewModel.sendSos(sosText)
                        }
                    )

                    Spacer(modifier = Modifier.height(20.dp))
                }

                // Section Title: SOS Xabarlar Tarixi
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "📋 SOS XABARLAR TARIXI",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        if (messages.isNotEmpty()) {
                            IconButton(onClick = { viewModel.clearHistory() }) {
                                Icon(
                                    imageVector = Icons.Default.DeleteSweep,
                                    contentDescription = "Tarixni tozalash",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                }

                if (messages.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                            )
                        ) {
                            Text(
                                text = "Hozircha SOS xabarlar yo'q. Qizil tugmalardan birini bossangiz barcha qurilmalarga SOS boradi!",
                                modifier = Modifier.padding(16.dp),
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    items(messages, key = { it.id }) { message ->
                        SosMessageCard(message = message)
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }
    }

    if (showSettingsDialog) {
        SettingsDialog(
            currentName = userName,
            currentServerUrl = serverUrl,
            onSave = { newName, newUrl ->
                viewModel.updateUserName(newName)
                viewModel.updateServerUrl(newUrl)
            },
            onDismiss = { showSettingsDialog = false }
        )
    }
}

@Composable
fun SosMessageCard(message: SosMessage) {
    val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    val formattedTime = timeFormat.format(Date(message.timestamp))

    val backgroundColor = if (message.isIncoming) {
        Color(0xFFFFEBEE) // Soft red background for incoming SOS
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }

    val borderColor = if (message.isIncoming) Color(0xFFD32F2F) else Color.Gray

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (message.isIncoming) "📥 KELGAN SOS: ${message.senderName}" else "📤 YUBORILGAN SOS: Siz",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = if (message.isIncoming) Color(0xFFB71C1C) else MaterialTheme.colorScheme.primary
                )

                Text(
                    text = formattedTime,
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = message.messageText,
                fontSize = 20.sp,
                fontWeight = FontWeight.Black,
                color = Color.Black
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                val channelText = when (message.channel) {
                    MessageChannel.INTERNET -> "🌐 Internet (Wi-Fi/4G)"
                    MessageChannel.BLUETOOTH -> "📡 Blutuz (Oflayn Mesh)"
                    MessageChannel.LOCAL -> "📱 Mahalliy"
                }
                Text(
                    text = "Tizim: $channelText",
                    fontSize = 12.sp,
                    color = Color.DarkGray
                )
            }
        }
    }
}
