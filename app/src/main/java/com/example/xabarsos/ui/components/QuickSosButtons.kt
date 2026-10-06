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
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.DoorFront
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Phone
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
    onStopAllAlerts: () -> Unit = {}
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
            modifier = Modifier.padding(12.dp)
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

            Spacer(modifier = Modifier.height(8.dp))

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

            Spacer(modifier = Modifier.height(10.dp))

            // MAIN PRESET SOS BUTTONS (Compact Height: 48.dp)
            Button(
                onClick = { onSendSos("SAIDBEK QARA", selectedRecipient) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = EmergencyRed,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "🚨 SAIDBEK QARA",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Black
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Button(
                onClick = { onSendSos("JASMINAHON QANI", selectedRecipient) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = EmergencyPink,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "🚨 JASMINAHON QANI",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Black
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // PRESET BUTTONS (Compact 2-Column Grid Layout)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Button 1: "NIMA GAP?"
                Button(
                    onClick = { onSendSos("NIMA GAP?", selectedRecipient) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF00B0FF),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Chat,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "💬 NIMA GAP?",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Button 2: "ESHIKNI OCH"
                Button(
                    onClick = { onSendSos("ESHIKNI OCH", selectedRecipient) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFAB47BC),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.DoorFront,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "🚪 ESHIKNI OCH",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Button 3: "REALDA NIMA GAP?"
                Button(
                    onClick = { onSendSos("REALDA NIMA GAP?", selectedRecipient) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF26A69A),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ElectricBolt,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "⚡ REALDA NIMA GAP?",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Button 4: "UYGA KETYABMAN"
                Button(
                    onClick = { onSendSos("UYGA KETYABMAN", selectedRecipient) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF7E57C2),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Home,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "🏠 UYGA KETYABMAN",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // NEW REQUESTED PRESET BUTTONS
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Button 5: "MASHINADA MUAMMO"
                Button(
                    onClick = { onSendSos("MASHINADA MUAMMO", selectedRecipient) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFF7043),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.DirectionsCar,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "🚗 MASHINADA MUAMMO",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Button 6: "TEL QIL YORDAM KERAK"
                Button(
                    onClick = { onSendSos("TEL QIL YORDAM KERAK", selectedRecipient) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF66BB6A),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Phone,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "📞 TEL QIL YORDAM KERAK",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Button 7: "YIG'ILYAPMIZ KELILAR"
                Button(
                    onClick = { onSendSos("YIG'ILYAPMIZ KELILAR", selectedRecipient) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFFA726),
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Groups,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = Color.Black
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "👥 YIG'ILYAPMIZ KELILAR",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Button 8: "ISHGA CHIQTINGMI?"
                Button(
                    onClick = { onSendSos("ISHGA CHIQTINGMI?", selectedRecipient) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF42A5F5),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Work,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "💼 ISHGA CHIQTINGMI?",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Compact Custom Message Section (Kichikroq va ixcham)
            Text(
                text = "Boshqa maxsus xabar yozish:",
                fontSize = 11.sp,
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
                        .height(52.dp),
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

                Spacer(modifier = Modifier.width(6.dp))

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
                        .height(52.dp)
                        .width(52.dp)
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
