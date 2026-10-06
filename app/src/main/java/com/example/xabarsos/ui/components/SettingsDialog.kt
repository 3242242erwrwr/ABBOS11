package com.example.xabarsos.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.xabarsos.audio.SosSoundType
import com.example.xabarsos.ui.theme.DarkCardContainer
import com.example.xabarsos.ui.theme.EmergencyRed
import com.example.xabarsos.utils.ManufacturerPowerUtil

@Composable
fun SettingsDialog(
    currentName: String,
    currentServerUrl: String,
    currentSoundType: SosSoundType,
    onSave: (String, String, SosSoundType) -> Unit,
    onTestSound: (SosSoundType) -> Unit,
    onStopTestSound: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var name by remember { mutableStateOf(currentName) }
    var serverUrl by remember { mutableStateOf(currentServerUrl) }
    var selectedSoundType by remember { mutableStateOf(currentSoundType) }
    var isTestingSound by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        onDispose {
            onStopTestSound()
        }
    }

    AlertDialog(
        onDismissRequest = {
            onStopTestSound()
            onDismiss()
        },
        containerColor = DarkCardContainer,
        titleContentColor = Color.White,
        textContentColor = Color.White,
        title = {
            Text(
                text = "⚙️ SOZLAMALAR",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // User Name Section
                Text(
                    text = "👤 Sizning ismingiz (SOS xabarida ko'rinadi):",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    placeholder = { Text("Masa'lan: Saidbek yoki Jasmina", color = Color.Gray) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFF121218),
                        unfocusedContainerColor = Color(0xFF121218),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = EmergencyRed,
                        unfocusedBorderColor = Color(0xFF333344)
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                // MANUFACTURER AUTOSTART & BACKGROUND PERMISSION HELPER
                Text(
                    text = "📱 HAR QANDAY TELEFONDA 100% ISHLASHI UCHUN SOZLASH:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFFFFD54F),
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))

                OutlinedButton(
                    onClick = {
                        ManufacturerPowerUtil.openAutoStartSettings(context)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFF00E676)
                    )
                ) {
                    Text("⚡ AUTO-START (AVTOZAPUSK) RUXSATINI BERISH", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(6.dp))

                OutlinedButton(
                    onClick = {
                        ManufacturerPowerUtil.openDisplayOverAppsSettings(context)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFF00B0FF)
                    )
                ) {
                    Text("📱 BOSHQA ILOVALAR USTIDA KO'RINISH", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Sound Selection Section
                Text(
                    text = "🔊 SOS OVOZI VA SIGNAL TANLASH:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))

                SosSoundType.entries.forEach { soundType ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                selectedSoundType = soundType
                            }
                            .padding(vertical = 4.dp)
                    ) {
                        RadioButton(
                            selected = (selectedSoundType == soundType),
                            onClick = { selectedSoundType = soundType },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = EmergencyRed,
                                unselectedColor = Color.Gray
                            )
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = soundType.displayName,
                            color = Color.White,
                            fontSize = 14.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Test Sound Button
                OutlinedButton(
                    onClick = {
                        if (isTestingSound) {
                            onStopTestSound()
                            isTestingSound = false
                        } else {
                            onTestSound(selectedSoundType)
                            isTestingSound = true
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = if (isTestingSound) EmergencyRed else Color(0xFF00B0FF)
                    )
                ) {
                    Icon(
                        imageVector = if (isTestingSound) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isTestingSound) "OVOZNI TO'XTATISH" else "🔊 OVOZNI TESHIRISH (MAX BALAND)",
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Server URL Section
                Text(
                    text = "🌐 Render Web Service Server Manzili:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = serverUrl,
                    onValueChange = { serverUrl = it },
                    placeholder = { Text("https://xabar-sos.onrender.com", color = Color.Gray) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFF121218),
                        unfocusedContainerColor = Color(0xFF121218),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = EmergencyRed,
                        unfocusedBorderColor = Color(0xFF333344)
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onStopTestSound()
                    onSave(name, serverUrl, selectedSoundType)
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = EmergencyRed,
                    contentColor = Color.White
                )
            ) {
                Text("SAQLASH", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    onStopTestSound()
                    onDismiss()
                }
            ) {
                Text("BEKOR QILISH", color = Color.Gray)
            }
        }
    )
}
