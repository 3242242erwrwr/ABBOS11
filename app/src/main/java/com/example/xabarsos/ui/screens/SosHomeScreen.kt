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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.BluetoothSearching
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
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
import com.example.xabarsos.ui.components.AddFriendDialog
import com.example.xabarsos.ui.components.AutoStartSetupDialog
import com.example.xabarsos.ui.components.MessagesHistoryDialog
import com.example.xabarsos.ui.components.QuickSosButtons
import com.example.xabarsos.ui.components.SettingsDialog
import com.example.xabarsos.ui.components.SosAlertBanner
import com.example.xabarsos.ui.theme.DarkBackground
import com.example.xabarsos.ui.theme.DarkCardContainer
import com.example.xabarsos.ui.theme.EmergencyRed
import com.example.xabarsos.ui.theme.NeonGreen
import com.example.xabarsos.ui.theme.NeonOrange
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
    val soundType by viewModel.soundType.collectAsState()
    val friendsList by viewModel.friendsList.collectAsState()
    val connectionStatus by viewModel.connectionStatus.collectAsState()
    val isScanningBluetooth by viewModel.isScanningBluetooth.collectAsState()

    val context = androidx.compose.ui.platform.LocalContext.current
    val prefs = remember { context.getSharedPreferences("xabar_sos_prefs", android.content.Context.MODE_PRIVATE) }
    var showFirstLaunchAutoStart by remember {
        mutableStateOf(!prefs.getBoolean("autostart_dialog_shown", false))
    }

    var showSettingsDialog by remember { mutableStateOf(false) }
    var showAddFriendDialog by remember { mutableStateOf(false) }
    var showMessagesMenuDialog by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = DarkBackground,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "🚨 XABAR SOS",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = Color.White
                        )
                        Text(
                            text = "Ismingiz: $userName",
                            fontSize = 12.sp,
                            color = Color.LightGray
                        )
                    }
                },
                actions = {
                    // Internet Status Indicator Badge
                    val statusColor = when (connectionStatus) {
                        is ConnectionStatus.Connected -> NeonGreen
                        is ConnectionStatus.Connecting -> NeonOrange
                        else -> EmergencyRed
                    }
                    Box(
                        modifier = Modifier
                            .padding(end = 4.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(statusColor.copy(alpha = 0.2f))
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
                            .background(Color(0xFF00B0FF).copy(alpha = 0.2f))
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (isScanningBluetooth) Icons.AutoMirrored.Filled.BluetoothSearching else Icons.Default.Bluetooth,
                                contentDescription = null,
                                tint = Color(0xFF00B0FF),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Blutuz",
                                color = Color(0xFF00B0FF),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Settings Button
                    IconButton(onClick = { showSettingsDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Sozlamalar",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkCardContainer
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding()
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
                contentPadding = PaddingValues(top = 12.dp, bottom = 24.dp)
            ) {
                // Preset Emergency SOS Buttons & Friends Connection
                item {
                    QuickSosButtons(
                        friendsList = friendsList,
                        onOpenAddFriendDialog = { showAddFriendDialog = true },
                        onSendSos = { sosText, targetRecipient ->
                            viewModel.sendSos(sosText, targetRecipient)
                        },
                        onStopAllAlerts = {
                            viewModel.dismissActiveAlert()
                        }
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                }

                // DEDICATED "XABARLAR MENYUSI" BUTTON CARD
                item {
                    Button(
                        onClick = { showMessagesMenuDialog = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF00B0FF),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = null,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (messages.isEmpty()) "📋 XABARLAR MENYUSI" else "📋 XABARLAR MENYUSI (${messages.size} TA XABAR)",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }

    if (showMessagesMenuDialog) {
        MessagesHistoryDialog(
            messages = messages,
            onClearHistory = { viewModel.clearHistory() },
            onDismiss = { showMessagesMenuDialog = false }
        )
    }

    if (showSettingsDialog) {
        SettingsDialog(
            currentName = userName,
            currentServerUrl = serverUrl,
            currentSoundType = soundType,
            onSave = { newName, newUrl, newSoundType ->
                viewModel.updateUserName(newName)
                viewModel.updateServerUrl(newUrl)
                viewModel.updateSoundType(newSoundType)
            },
            onTestSound = { soundToTest ->
                viewModel.testSound(soundToTest)
            },
            onStopTestSound = {
                viewModel.stopTestSound()
            },
            onDismiss = { showSettingsDialog = false }
        )
    }

    if (showAddFriendDialog) {
        AddFriendDialog(
            friendsList = friendsList,
            onAddFriend = { name -> viewModel.addFriend(name) },
            onRemoveFriend = { name -> viewModel.removeFriend(name) },
            onDismiss = { showAddFriendDialog = false }
        )
    }

    if (showFirstLaunchAutoStart) {
        AutoStartSetupDialog(
            onDismiss = {
                prefs.edit().putBoolean("autostart_dialog_shown", true).apply()
                showFirstLaunchAutoStart = false
            }
        )
    }
}

@Composable
fun SosMessageCard(message: SosMessage) {
    val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    val formattedTime = timeFormat.format(Date(message.timestamp))

    val backgroundColor = if (message.isIncoming) {
        Color(0xFF2C1418) // Dark deep red background for incoming emergency SOS
    } else {
        DarkCardContainer
    }

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
                    color = if (message.isIncoming) Color(0xFFFF5252) else Color(0xFF00B0FF)
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
                color = Color.White
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val channelText = when (message.channel) {
                    MessageChannel.INTERNET -> "🌐 Internet"
                    MessageChannel.BLUETOOTH -> "📡 Blutuz"
                    MessageChannel.LOCAL -> "📱 Mahalliy"
                }
                Text(
                    text = "Tizim: $channelText",
                    fontSize = 12.sp,
                    color = Color.LightGray
                )

                Text(
                    text = "🎯 Kimga: ${message.targetRecipient}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFFD54F)
                )
            }
        }
    }
}
