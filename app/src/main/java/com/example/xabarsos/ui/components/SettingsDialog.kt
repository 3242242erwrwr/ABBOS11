package com.example.xabarsos.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun SettingsDialog(
    currentName: String,
    currentServerUrl: String,
    onSave: (String, String) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(currentName) }
    var serverUrl by remember { mutableStateOf(currentServerUrl) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "⚙️ SOZLAMALAR",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Sizning ismingiz (SOS xabarida ko'rinadi):",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    placeholder = { Text("Masa'lan: Saidbek yoki Jasmina") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Render Web Service Server Manzili:",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = serverUrl,
                    onValueChange = { serverUrl = it },
                    placeholder = { Text("https://xabar-sos.onrender.com") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "💡 Server Wi-Fi va 4G orqali barcha qurilmalarga SOS xabarini tarqatadi.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(name, serverUrl)
                    onDismiss()
                }
            ) {
                Text("SAQLASH")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("BEKOR QILISH")
            }
        }
    )
}
