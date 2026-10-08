package com.example.xabarsos.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PlayArrow
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.xabarsos.model.MessageChannel
import com.example.xabarsos.model.SosMessage
import com.example.xabarsos.network.ConnectionStatus
import com.example.xabarsos.ui.SosViewModel
import com.example.xabarsos.ui.components.AddFriendDialog
import com.example.xabarsos.ui.components.AppUpdateDialog
import com.example.xabarsos.ui.components.AutoStartSetupDialog
import com.example.xabarsos.ui.components.JamuHabarLogo
import com.example.xabarsos.ui.components.MessagesHistoryDialog
import com.example.xabarsos.ui.components.MicrophonePermissionDialog
import com.example.xabarsos.ui.components.QuickSosButtons
import com.example.xabarsos.ui.components.SelectFriendPromptDialog
import com.example.xabarsos.ui.components.SettingsDialog
import com.example.xabarsos.ui.components.SosAlertBanner
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
    val broadcastToAllEnabled by viewModel.broadcastToAllEnabled.collectAsState()
    val friendsList by viewModel.friendsList.collectAsState()
    val connectionStatus by viewModel.connectionStatus.collectAsState()

    val isRecordingVoiceNote by viewModel.isRecordingVoiceNote.collectAsState()
    val isPlayingVoiceNote by viewModel.isPlayingVoiceNote.collectAsState()
    val recordingDurationSeconds by viewModel.recordingDurationSeconds.collectAsState()
    val selectedVoiceEffect by viewModel.selectedVoiceEffect.collectAsState()
    val peerTypingStatus by viewModel.peerTypingStatus.collectAsState()
    val typingSenderName by viewModel.typingSenderName.collectAsState()
    val typingStatusType by viewModel.typingStatusType.collectAsState()

    val availableAppUpdate by viewModel.availableAppUpdate.collectAsState()
    val isDownloadingUpdate by viewModel.isDownloadingUpdate.collectAsState()
    val updateDownloadProgress by viewModel.updateDownloadProgress.collectAsState()

    val context = androidx.compose.ui.platform.LocalContext.current
    val prefs = remember { context.getSharedPreferences("xabar_sos_prefs", Context.MODE_PRIVATE) }
    var showFirstLaunchAutoStart by remember {
        mutableStateOf(!prefs.getBoolean("autostart_dialog_shown", false))
    }

    var showMicrophonePermissionDialog by remember { mutableStateOf(false) }

    val recordAudioPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.startVoiceNoteRecording()
        } else {
            showMicrophonePermissionDialog = true
        }
    }

    var showSettingsDialog by remember { mutableStateOf(false) }
    var showAddFriendDialog by remember { mutableStateOf(false) }
    var showMessagesMenuDialog by remember { mutableStateOf(false) }
    var showSelectFriendPromptDialog by remember { mutableStateOf(false) }
    var selectedRecipient by remember { mutableStateOf("") }

    val backgroundGradient = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF0D101A),
            Color(0xFF151928),
            Color(0xFF090B12)
        )
    )

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        JamuHabarLogo(size = 34.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "XABAR SOS",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = Color.White
                            )
                            Text(
                                text = "Ismingiz: $userName",
                                fontSize = 11.sp,
                                color = Color.LightGray
                            )
                        }
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
                            .padding(end = 8.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(statusColor.copy(alpha = 0.2f))
                            .padding(horizontal = 6.dp, vertical = 3.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (connectionStatus is ConnectionStatus.Connected) Icons.Default.Wifi else Icons.Default.WifiOff,
                                contentDescription = null,
                                tint = statusColor,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = if (connectionStatus is ConnectionStatus.Connected) "Internet" else "Oflayn",
                                color = statusColor,
                                fontSize = 10.sp,
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
                    containerColor = Color.Transparent
                )
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundGradient)
        ) {
            // Ambient glowing lights background
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFF00E5FF).copy(alpha = 0.35f), Color.Transparent),
                        center = Offset(size.width * 0.85f, size.height * 0.20f),
                        radius = size.width * 0.85f
                    )
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFFFF1744).copy(alpha = 0.30f), Color.Transparent),
                        center = Offset(size.width * 0.15f, size.height * 0.75f),
                        radius = size.width * 0.90f
                    )
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFFAA00FF).copy(alpha = 0.25f), Color.Transparent),
                        center = Offset(size.width * 0.50f, size.height * 0.50f),
                        radius = size.width * 0.65f
                    )
                )
            }

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
                        isPlayingVoiceNote = isPlayingVoiceNote,
                        onPlayVoiceNote = { audioData -> viewModel.playVoiceNote(audioData) },
                        onDismiss = { viewModel.dismissActiveAlert() }
                    )
                }

                // Live Peer Typing / Voice Recording Status Badge (Calm & Clean without flashing)
                peerTypingStatus?.let { statusText ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 6.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFF00B0FF).copy(alpha = 0.25f)
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "💬 $statusText",
                                color = Color(0xFF00B0FF),
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 14.dp),
                    contentPadding = PaddingValues(top = 10.dp, bottom = 24.dp)
                ) {
                    // Preset Emergency SOS Buttons & Friends Connection
                    item {
                        QuickSosButtons(
                            friendsList = friendsList,
                            selectedRecipient = selectedRecipient,
                            typingSenderName = typingSenderName,
                            typingStatusType = typingStatusType,
                            onSelectRecipient = { selectedRecipient = it },
                            isRecordingVoiceNote = isRecordingVoiceNote,
                            recordingDurationSeconds = recordingDurationSeconds,
                            selectedVoiceEffect = selectedVoiceEffect,
                            onSelectVoiceEffect = { effect -> viewModel.setSelectedVoiceEffect(effect) },
                            onStartVoiceNoteRecording = {
                                if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                                    viewModel.startVoiceNoteRecording()
                                } else {
                                    recordAudioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                }
                            },
                            onStopVoiceNoteAndSend = {
                                viewModel.stopVoiceNoteAndSend(selectedRecipient)
                            },
                            onCancelVoiceNoteRecording = {
                                viewModel.cancelVoiceNoteRecording()
                            },
                            onSendTypingStatus = { status ->
                                viewModel.sendTypingStatus(status, selectedRecipient)
                            },
                            onOpenAddFriendDialog = { showAddFriendDialog = true },
                            onShowSelectFriendPrompt = { showSelectFriendPromptDialog = true },
                            onSendSos = { sosText, targetRecipient ->
                                viewModel.sendSos(sosText, targetRecipient)
                            },
                            onStopAllAlerts = {
                                viewModel.dismissActiveAlert()
                            }
                        )

                        Spacer(modifier = Modifier.height(14.dp))
                    }

                    // DEDICATED GLASSMORPHISM "XABARLAR MENYUSI" BUTTON CARD
                    item {
                        Button(
                            onClick = { showMessagesMenuDialog = true },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF00B0FF).copy(alpha = 0.9f),
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (messages.isEmpty()) "📋 XABARLAR MENYUSI" else "📋 XABARLAR MENYUSI (${messages.size})",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                softWrap = false,
                                modifier = Modifier.weight(1f)
                            )
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    if (showMessagesMenuDialog) {
        MessagesHistoryDialog(
            messages = messages,
            onPlayVoiceNote = { audioData -> viewModel.playVoiceNote(audioData) },
            onDeleteSingleMessage = { messageId -> viewModel.deleteMessageById(messageId) },
            onClearHistory = { viewModel.clearHistory() },
            onDismiss = { showMessagesMenuDialog = false }
        )
    }

    if (showSettingsDialog) {
        SettingsDialog(
            currentName = userName,
            currentServerUrl = serverUrl,
            currentSoundType = soundType,
            currentBroadcastToAll = broadcastToAllEnabled,
            onSave = { newName, newUrl, newSoundType, newBroadcastToAll ->
                viewModel.updateUserName(newName)
                viewModel.updateServerUrl(newUrl)
                viewModel.updateSoundType(newSoundType)
                viewModel.updateBroadcastToAllEnabled(newBroadcastToAll)
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
            selectedRecipient = selectedRecipient,
            onSelectRecipient = { selectedRecipient = it },
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

    if (showMicrophonePermissionDialog) {
        MicrophonePermissionDialog(
            onDismiss = { showMicrophonePermissionDialog = false }
        )
    }

    if (showSelectFriendPromptDialog) {
        SelectFriendPromptDialog(
            onDismiss = { showSelectFriendPromptDialog = false }
        )
    }

    availableAppUpdate?.let { info ->
        AppUpdateDialog(
            versionInfo = info,
            isDownloading = isDownloadingUpdate,
            downloadProgress = updateDownloadProgress,
            onStartUpdate = { viewModel.startAppUpdate(info.apkUrl) },
            onDismiss = { viewModel.dismissAppUpdate() }
        )
    }
}

@Composable
fun SosMessageCard(
    message: SosMessage,
    onPlayVoiceNote: ((String) -> Unit)? = null,
    onDeleteMessage: (() -> Unit)? = null
) {
    val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    val formattedTime = timeFormat.format(Date(message.timestamp))

    val backgroundColor = if (message.isIncoming) {
        Color(0xFF2C1418).copy(alpha = 0.85f)
    } else {
        DarkCardContainer.copy(alpha = 0.85f)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (message.isIncoming) "📥 KELGAN SOS: ${message.senderName}" else "📤 YUBORILGAN SOS: Siz",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = if (message.isIncoming) Color(0xFFFF5252) else Color(0xFF00B0FF)
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (!message.isIncoming) {
                        Text(
                            text = if (message.isDelivered) "✓✓ Yetib bordi" else "✓ Yuborildi",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (message.isDelivered) Color(0xFF00E676) else Color.Gray
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                    }

                    Text(
                        text = formattedTime,
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                    if (onDeleteMessage != null) {
                        Spacer(modifier = Modifier.width(4.dp))
                        IconButton(
                            onClick = onDeleteMessage,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "O'chirish",
                                tint = Color(0xFFFF5252),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = message.messageText,
                fontSize = 20.sp,
                fontWeight = FontWeight.Black,
                color = Color.White
            )

            if (!message.audioData.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = { onPlayVoiceNote?.invoke(message.audioData) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF00E676),
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Eshitish",
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "▶️ GALASAVOYNI ESHITISH (OVOZNI TINGLASH)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }

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
