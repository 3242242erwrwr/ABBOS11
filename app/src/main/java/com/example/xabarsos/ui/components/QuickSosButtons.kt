package com.example.xabarsos.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.DoorFront
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.xabarsos.audio.VoiceEffect
import com.example.xabarsos.ui.theme.EmergencyPink
import com.example.xabarsos.ui.theme.EmergencyRed

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun QuickSosButtons(
    friendsList: List<String>,
    selectedRecipient: String = "BARCHAGA",
    onSelectRecipient: (String) -> Unit = {},
    isRecordingVoiceNote: Boolean = false,
    recordingDurationSeconds: Int = 0,
    selectedVoiceEffect: VoiceEffect = VoiceEffect.NORMAL,
    onSelectVoiceEffect: (VoiceEffect) -> Unit = {},
    onStartVoiceNoteRecording: () -> Unit = {},
    onStopVoiceNoteAndSend: () -> Unit = {},
    onCancelVoiceNoteRecording: () -> Unit = {},
    onSendTypingStatus: (String) -> Unit = {},
    onOpenAddFriendDialog: () -> Unit = {},
    onSendSos: (text: String, recipient: String) -> Unit,
    onStopAllAlerts: () -> Unit = {}
) {
    var customMessage by remember { mutableStateOf("") }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
                shape = RoundedCornerShape(20.dp)
            ),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF161B28).copy(alpha = 0.75f)
        ),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp)
        ) {
            // HEADER: DO'STLAR BILAN ULANISH
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "🤝 DO'STLAR BILAN ULANISH",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // DYNAMIC FRIENDS CHIPS & DO'STLARIM BUTTON
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Preset Option 1: BARCHAGA (Default)
                FilterChip(
                    selected = (selectedRecipient == "BARCHAGA"),
                    onClick = {
                        onSelectRecipient("BARCHAGA")
                    },
                    label = { Text("📢 Barchaga", fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = EmergencyRed,
                        selectedLabelColor = Color.White,
                        containerColor = Color(0xFF22222E).copy(alpha = 0.8f),
                        labelColor = Color.LightGray
                    )
                )

                // Option 2: DO'STLARIM SPISOK BUTTON
                FilterChip(
                    selected = false,
                    onClick = onOpenAddFriendDialog,
                    label = { Text("🤝 Do'stlarim (${friendsList.size}) ›", fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = Color(0xFF00B0FF).copy(alpha = 0.25f),
                        labelColor = Color(0xFF00B0FF)
                    )
                )

                // Option 3: SELECTED SPECIFIC FRIEND BADGE WITH CANCEL TAP
                if (selectedRecipient != "BARCHAGA") {
                    FilterChip(
                        selected = true,
                        onClick = {
                            onSelectRecipient("BARCHAGA")
                        },
                        label = { Text("🎯 $selectedRecipient ✕", fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF00B0FF),
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ROW 1: "SAIDBEK QARA" & "JASMINAHON QANI"
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Button(
                    onClick = { onSendSos("SAIDBEK QARA", selectedRecipient) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = EmergencyRed.copy(alpha = 0.9f),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "🚨 SAIDBEK QARA",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                }

                Button(
                    onClick = { onSendSos("JASMINAHON QANI", selectedRecipient) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = EmergencyPink.copy(alpha = 0.9f),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "🚨 JASMINAHON QANI",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // ROW 2: "NIMA GAP?" & "ESHIKNI OCH"
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Button(
                    onClick = { onSendSos("NIMA GAP?", selectedRecipient) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF00B0FF).copy(alpha = 0.88f),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Chat,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "💬 NIMA GAP?",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                }

                Button(
                    onClick = { onSendSos("ESHIKNI OCH", selectedRecipient) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFAB47BC).copy(alpha = 0.88f),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.DoorFront,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "🚪 ESHIKNI OCH",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // ROW 3: "REALDA NIMA GAP?" & "UYGA KETYABMAN"
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Button(
                    onClick = { onSendSos("REALDA NIMA GAP?", selectedRecipient) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF26A69A).copy(alpha = 0.88f),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ElectricBolt,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "⚡ REALDA NIMA GAP?",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                }

                Button(
                    onClick = { onSendSos("UYGA KETYABMAN", selectedRecipient) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF7E57C2).copy(alpha = 0.88f),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Home,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "🏠 UYGA KETYABMAN",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // ROW 4: "MASHINADA MUAMMO" & "TEL QIL YORDAM KERAK"
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Button(
                    onClick = { onSendSos("MASHINADA MUAMMO", selectedRecipient) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFF7043).copy(alpha = 0.88f),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.DirectionsCar,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "🚗 MASHINADA MUAMMO",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                }

                Button(
                    onClick = { onSendSos("TEL QIL YORDAM KERAK", selectedRecipient) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF66BB6A).copy(alpha = 0.88f),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Phone,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "📞 TEL QIL YORDAM KERAK",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // ROW 5: "YIG'ILYAPMIZ KELILAR" & "ISHGA CHIQTINGMI?"
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Button(
                    onClick = { onSendSos("YIG'ILYAPMIZ KELILAR", selectedRecipient) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFFA726).copy(alpha = 0.9f),
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Groups,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = Color.Black
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "👥 YIG'ILYAPMIZ KELILAR",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = Color.Black
                    )
                }

                Button(
                    onClick = { onSendSos("ISHGA CHIQTINGMI?", selectedRecipient) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF42A5F5).copy(alpha = 0.88f),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Work,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "💼 ISHGA CHIQTINGMI?",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // VOICE EFFECT SELECTOR CHIPS (🎭 OVOZ EFFEKTI)
            Text(
                text = "🎭 OVOZ EFFEKTI (GALASAVOY UCHUN):",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFFFD54F)
            )

            Spacer(modifier = Modifier.height(4.dp))

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                VoiceEffect.entries.forEach { effect ->
                    val isSelected = selectedVoiceEffect == effect
                    FilterChip(
                        selected = isSelected,
                        onClick = { onSelectVoiceEffect(effect) },
                        label = {
                            Text(
                                text = "${effect.emoji} ${effect.displayName}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF00E676),
                            selectedLabelColor = Color.Black,
                            containerColor = Color(0xFF1E2230),
                            labelColor = Color.LightGray
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Custom Message Section (PERFECT VERTICALLY CENTERED BasicTextField OR GALASAVOY RECORDING BAR)
            Text(
                text = "Boshqa maxsus xabar yozish:",
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = Color.LightGray
            )

            Spacer(modifier = Modifier.height(4.dp))

            if (isRecordingVoiceNote) {
                // Voice Recording Live Bar (Galasavoy)
                val formattedRecDuration = String.format(java.util.Locale.getDefault(), "%02d:%02d", recordingDurationSeconds / 60, recordingDurationSeconds % 60)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = EmergencyRed.copy(alpha = 0.25f)
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "🔴 $formattedRecDuration",
                                color = EmergencyRed,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Yozilmoqda...",
                                color = Color.White,
                                fontSize = 11.sp
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Button(
                                onClick = {
                                    onSendTypingStatus("idle")
                                    onCancelVoiceNoteRecording()
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF333344),
                                    contentColor = Color.LightGray
                                ),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                                modifier = Modifier.height(34.dp)
                            ) {
                                Text("Bekor qilish", fontSize = 11.sp)
                            }

                            Spacer(modifier = Modifier.width(6.dp))

                            Button(
                                onClick = {
                                    onSendTypingStatus("idle")
                                    onStopVoiceNoteAndSend()
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF00E676),
                                    contentColor = Color.Black
                                ),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                                modifier = Modifier.height(34.dp)
                            ) {
                                Text("YUBORISH 📤", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }
                        }
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BasicTextField(
                        value = customMessage,
                        onValueChange = {
                            customMessage = it
                            if (it.isNotBlank()) {
                                onSendTypingStatus("typing_text")
                            } else {
                                onSendTypingStatus("idle")
                            }
                        },
                        singleLine = true,
                        textStyle = TextStyle(
                            fontSize = 13.sp,
                            color = Color.White,
                            fontWeight = FontWeight.Medium
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .background(Color(0xFF121218).copy(alpha = 0.8f), RoundedCornerShape(10.dp))
                            .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(10.dp))
                            .padding(horizontal = 12.dp),
                        decorationBox = { innerTextField ->
                            Box(
                                contentAlignment = Alignment.CenterStart,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                if (customMessage.isEmpty()) {
                                    Text(
                                        text = "Xabaringizni yozing...",
                                        color = Color.Gray,
                                        fontSize = 13.sp
                                    )
                                }
                                innerTextField()
                            }
                        }
                    )

                    Spacer(modifier = Modifier.width(6.dp))

                    Button(
                        onClick = {
                            if (customMessage.isNotBlank()) {
                                onSendTypingStatus("idle")
                                onSendSos(customMessage, selectedRecipient)
                                customMessage = ""
                            }
                        },
                        enabled = customMessage.isNotBlank(),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(0.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = EmergencyRed,
                            disabledContainerColor = Color(0xFF333344)
                        ),
                        modifier = Modifier
                            .height(44.dp)
                            .width(44.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Yuborish",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    // GALASAVOY BUTTON (Ovozli Xabar)
                    Button(
                        onClick = {
                            onSendTypingStatus("typing_voice")
                            onStartVoiceNoteRecording()
                        },
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(0.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF00E676),
                            contentColor = Color.Black
                        ),
                        modifier = Modifier
                            .height(44.dp)
                            .width(44.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = "Galasavoy (Ovozli Xabar)",
                            tint = Color.Black,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}
