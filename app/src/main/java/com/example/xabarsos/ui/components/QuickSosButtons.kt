package com.example.xabarsos.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import com.example.xabarsos.ui.theme.DarkCardContainer
import com.example.xabarsos.ui.theme.EmergencyPink
import com.example.xabarsos.ui.theme.EmergencyRed

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun QuickSosButtons(
    friendsList: List<String>,
    onOpenAddFriendDialog: () -> Unit,
    onSendSos: (text: String, recipient: String) -> Unit,
    onStopAllAlerts: () -> Unit
) {
    var customMessage by remember { mutableStateOf("") }
    var selectedRecipient by remember { mutableStateOf("BARCHAGA") }
    var previousListSize by remember { mutableStateOf(friendsList.size) }

    // Auto-select newly added friend when friendsList grows
    if (friendsList.size > previousListSize) {
        friendsList.lastOrNull()?.let { newlyAdded ->
            selectedRecipient = newlyAdded
        }
        previousListSize = friendsList.size
    } else if (friendsList.size < previousListSize) {
        previousListSize = friendsList.size
        if (friendsList.none { it.equals(selectedRecipient, ignoreCase = true) }) {
            selectedRecipient = "BARCHAGA"
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = DarkCardContainer
        ),
        shape = RoundedCornerShape(16.dp)
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

            // DYNAMIC FRIENDS CHIPS
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Preset Option 1: BARCHAGA (Default)
                FilterChip(
                    selected = (selectedRecipient == "BARCHAGA"),
                    onClick = {
                        selectedRecipient = "BARCHAGA"
                    },
                    label = { Text("📢 Barchaga", fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = EmergencyRed,
                        selectedLabelColor = Color.White,
                        containerColor = Color(0xFF22222E),
                        labelColor = Color.LightGray
                    )
                )

                // DYNAMIC DEDICATED FRIEND CHIPS
                friendsList.forEach { friendName ->
                    FilterChip(
                        selected = (selectedRecipient.equals(friendName, ignoreCase = true)),
                        onClick = {
                            selectedRecipient = friendName
                        },
                        label = { Text("👤 $friendName", fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF00B0FF),
                            selectedLabelColor = Color.White,
                            containerColor = Color(0xFF22222E),
                            labelColor = Color.LightGray
                        )
                    )
                }

                // ADD FRIEND BUTTON CHIP ("+ Do'st qo'shish")
                FilterChip(
                    selected = false,
                    onClick = onOpenAddFriendDialog,
                    label = { Text("➕ Do'st qo'shish", fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.PersonAdd,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = Color(0xFFFF9100)
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = Color(0xFF2C251E),
                        labelColor = Color(0xFFFF9100)
                    )
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Main Requested Preset SOS Buttons: "SAIDBEK QARA" & "JASMINAHON QANI"
            Button(
                onClick = { onSendSos("SAIDBEK QARA", selectedRecipient) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = EmergencyRed,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    modifier = Modifier.size(26.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "🚨 SAIDBEK QARA",
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Black
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = { onSendSos("JASMINAHON QANI", selectedRecipient) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = EmergencyPink,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    modifier = Modifier.size(26.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "🚨 JASMINAHON QANI",
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Black
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 🛑 AUXILIARY EMERGENCY STOP BUTTON (YORDAMCHI STOP KNOPKASI)
            Button(
                onClick = {
                    onStopAllAlerts()
                    onSendSos("🛑 SOS BEKOR QILINDI (STOP)", selectedRecipient)
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFF9100),
                    contentColor = Color.Black
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Cancel,
                    contentDescription = "Stop",
                    modifier = Modifier.size(24.dp),
                    tint = Color.Black
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "🛑 STOP (SOS VA SIRENANI TO'XTATISH)",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Compact Custom Message Section (Kichikroq va ixcham)
            Text(
                text = "Boshqa maxsus xabar yozish:",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = customMessage,
                    onValueChange = { customMessage = it },
                    placeholder = {
                        Text(
                            text = "Xabaringizni yozing...",
                            color = Color.Gray,
                            fontSize = 13.sp
                        )
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp),
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true,
                    textStyle = TextStyle(fontSize = 14.sp, color = Color.White),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFF121218),
                        unfocusedContainerColor = Color(0xFF121218),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = EmergencyRed,
                        unfocusedBorderColor = Color(0xFF333344)
                    )
                )

                Spacer(modifier = Modifier.width(8.dp))

                Button(
                    onClick = {
                        if (customMessage.isNotBlank()) {
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
                        .height(46.dp)
                        .width(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Yuborish",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
