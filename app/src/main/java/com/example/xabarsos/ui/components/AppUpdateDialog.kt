package com.example.xabarsos.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.xabarsos.update.AppVersionInfo

@Composable
fun AppUpdateDialog(
    versionInfo: AppVersionInfo,
    isDownloading: Boolean = false,
    downloadProgress: Float = 0f,
    onStartUpdate: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF131826).copy(alpha = 0.90f),
        titleContentColor = Color.White,
        textContentColor = Color.White,
        modifier = Modifier.border(
            border = BorderStroke(
                width = 1.dp,
                brush = Brush.linearGradient(
                    colors = listOf(Color.White.copy(alpha = 0.35f), Color.White.copy(alpha = 0.05f))
                )
            ),
            shape = RoundedCornerShape(20.dp)
        ),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.SystemUpdate,
                    contentDescription = null,
                    tint = Color(0xFF00E676),
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "🚀 YANGI YANGILANISH MAVJUD!",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp
                )
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Versiya: v${versionInfo.versionName}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF00B0FF),
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF0A0C14).copy(alpha = 0.8f)
                    ),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "Yangi Imkoniyatlar:",
                            color = Color(0xFFFFD54F),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = versionInfo.releaseNotes,
                            color = Color.LightGray,
                            fontSize = 12.sp,
                            lineHeight = 18.sp
                        )
                    }
                }

                if (isDownloading) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "Yuklanmoqda... ${(downloadProgress * 100).toInt()}%",
                        color = Color(0xFF00E676),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { downloadProgress },
                        modifier = Modifier.fillMaxWidth(),
                        color = Color(0xFF00E676),
                        trackColor = Color(0xFF22222E)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onStartUpdate,
                enabled = !isDownloading,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF00E676),
                    contentColor = Color.Black
                ),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(
                    text = if (isDownloading) "YUKLANMOQDA..." else "⚡ ZUDLIK BILAN YANGILASH",
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            if (!isDownloading) {
                TextButton(onClick = onDismiss) {
                    Text("KEYINROQ", color = Color.Gray, fontWeight = FontWeight.Bold)
                }
            }
        }
    )
}
