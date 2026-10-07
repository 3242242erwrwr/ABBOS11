package com.example.xabarsos.ui.components

import android.content.Context
import android.media.AudioManager
import android.media.MediaPlayer
import android.util.Base64
import android.util.Log
import android.view.SurfaceHolder
import android.view.SurfaceView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.xabarsos.ui.theme.DarkCardContainer
import com.example.xabarsos.ui.theme.EmergencyRed
import java.io.File
import java.io.FileOutputStream

@Composable
fun VideoNotePlayerDialog(
    base64Video: String,
    senderName: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var mediaPlayerInstance by remember { mutableStateOf<MediaPlayer?>(null) }
    var tempVideoFile by remember { mutableStateOf<File?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            try {
                mediaPlayerInstance?.stop()
                mediaPlayerInstance?.release()
            } catch (e: Exception) {}
            mediaPlayerInstance = null
            try {
                tempVideoFile?.delete()
            } catch (e: Exception) {}
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkCardContainer,
        titleContentColor = Color.White,
        textContentColor = Color.White,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Videocam,
                    contentDescription = null,
                    tint = Color(0xFF00E676),
                    modifier = Modifier.size(26.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "📹 DOIRA VIDEO XABAR: $senderName",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // CIRCULAR VIDEO PLAYER
                Box(
                    modifier = Modifier
                        .size(200.dp)
                        .clip(CircleShape)
                        .border(3.dp, Color(0xFF00E676), CircleShape)
                        .background(Color.Black),
                    contentAlignment = Alignment.Center
                ) {
                    AndroidView(
                        factory = { ctx ->
                            SurfaceView(ctx).apply {
                                holder.addCallback(object : SurfaceHolder.Callback {
                                    override fun surfaceCreated(holder: SurfaceHolder) {
                                        try {
                                            val videoBytes = Base64.decode(base64Video, Base64.NO_WRAP)
                                            val file = File.createTempFile("play_video_", ".mp4", ctx.cacheDir)
                                            FileOutputStream(file).use { it.write(videoBytes) }
                                            tempVideoFile = file

                                            // Auto-boost speaker volume to 100% MAX
                                            val audioManager = ctx.getSystemService(Context.AUDIO_SERVICE) as AudioManager
                                            val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                                            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, maxVol, 0)

                                            mediaPlayerInstance = MediaPlayer().apply {
                                                setDataSource(file.absolutePath)
                                                setDisplay(holder)
                                                isLooping = true
                                                prepare()
                                                start()
                                            }
                                        } catch (e: Exception) {
                                            Log.e("VideoNotePlayer", "Error playing video note: ${e.message}")
                                        }
                                    }

                                    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {}

                                    override fun surfaceDestroyed(holder: SurfaceHolder) {
                                        try {
                                            mediaPlayerInstance?.stop()
                                            mediaPlayerInstance?.release()
                                        } catch (e: Exception) {}
                                        mediaPlayerInstance = null
                                    }
                                })
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "▶️ Video va Ovoz Tiniq Ijro Etilmoqda",
                    color = Color(0xFF00E676),
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(
                    containerColor = EmergencyRed,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("YOPISH / TAYYOR", fontWeight = FontWeight.Bold)
            }
        }
    )
}
