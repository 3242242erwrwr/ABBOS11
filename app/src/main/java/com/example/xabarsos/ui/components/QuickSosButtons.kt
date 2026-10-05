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
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Person
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
    onSendSos: (text: String, recipient: String) -> Unit
) {
    var customMessage by remember { mutableStateOf("") }
    var selectedRecipient by remember { mutableStateOf("BARCHAGA") }
    var customRecipientInput by remember { mutableStateOf("") }
    var showCustomRecipientField by remember { mutableStateOf(false) }

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
            Text(
                text = "⚡ TEZKOR SOS XABARI YUBORISH",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(10.dp))

            // RECIPIENT SELECTOR SECTION (KIMGA YUBORILSIN?)
            Text(
                text = "🎯 Qabul qiluvchini tanlang (Kimga yuborilsin?):",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFFFD54F)
            )

            Spacer(modifier = Modifier.height(6.dp))

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Preset Option 1: BARCHAGA (Default)
                FilterChip(
                    selected = (selectedRecipient == "BARCHAGA" && !showCustomRecipientField),
                    onClick = {
                        selectedRecipient = "BARCHAGA"
                        showCustomRecipientField = false
                    },
                    label = { Text("📢 Barchaga", fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = EmergencyRed,
                        selectedLabelColor = Color.White,
                        containerColor = Color(0xFF22222E),
                        labelColor = Color.LightGray
                    )
                )

                // Preset Option 2: SAIDBEK
                FilterChip(
                    selected = (selectedRecipient == "SAIDBEK" && !showCustomRecipientField),
                    onClick = {
                        selectedRecipient = "SAIDBEK"
                        showCustomRecipientField = false
                    },
                    label = { Text("👤 Saidbek", fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFF00B0FF),
                        selectedLabelColor = Color.White,
                        containerColor = Color(0xFF22222E),
                        labelColor = Color.LightGray
                    )
                )

                // Preset Option 3: JASMINAHON
                FilterChip(
                    selected = (selectedRecipient == "JASMINAHON" && !showCustomRecipientField),
                    onClick = {
                        selectedRecipient = "JASMINAHON"
                        showCustomRecipientField = false
                    },
                    label = { Text("👤 Jasminahon", fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = EmergencyPink,
                        selectedLabelColor = Color.White,
                        containerColor = Color(0xFF22222E),
                        labelColor = Color.LightGray
                    )
                )

                // Preset Option 4: Custom Name
                FilterChip(
                    selected = showCustomRecipientField,
                    onClick = {
                        showCustomRecipientField = true
                    },
                    label = { Text("➕ Maxsus ism...", fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFFFF9100),
                        selectedLabelColor = Color.White,
                        containerColor = Color(0xFF22222E),
                        labelColor = Color.LightGray
                    )
                )
            }

            if (showCustomRecipientField) {
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = customRecipientInput,
                    onValueChange = {
                        customRecipientInput = it
                        selectedRecipient = it.trim().ifEmpty { "BARCHAGA" }
                    },
                    placeholder = { Text("Masa'lan: Otabek, Oyazim...", color = Color.Gray, fontSize = 12.sp) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                    shape = RoundedCornerShape(8.dp),
                    textStyle = TextStyle(fontSize = 13.sp, color = Color.White),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFF121218),
                        unfocusedContainerColor = Color(0xFF121218),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFFFF9100),
                        unfocusedBorderColor = Color(0xFF333344)
                    )
                )
            }

            val activeRecipient = if (showCustomRecipientField && customRecipientInput.isNotBlank()) {
                customRecipientInput.trim()
            } else {
                selectedRecipient
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Main Requested Preset SOS Buttons: "SAIDBEK QARA" & "JASMINAHON QANI"
            Button(
                onClick = { onSendSos("SAIDBEK QARA", activeRecipient) },
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
                onClick = { onSendSos("JASMINAHON QANI", activeRecipient) },
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
                            onSendSos(customMessage, activeRecipient)
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
