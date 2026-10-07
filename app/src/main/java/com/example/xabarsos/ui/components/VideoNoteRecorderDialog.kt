package com.example.xabarsos.ui.components

import android.hardware.Camera
import android.media.CamcorderProfile
import android.media.MediaRecorder
import android.net.Uri
import android.util.Base64
import android.util.Log
import android.view.SurfaceHolder
import android.view.SurfaceView
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
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
import androidx.core.content.FileProvider
import com.example.xabarsos.ui.theme.DarkCardContainer
import com.example.xabarsos.ui.theme.EmergencyRed
import kotlinx.coroutines.delay
import java.io.File
import java.io.FileInputStream

@Suppress("DEPRECATION")
@Composable
fun VideoNoteRecorderDialog(
    onSendVideoNote: (base64Video: String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var isRecording by remember { mutableStateOf(false) }
    var recDurationSeconds by remember { mutableStateOf(0) }
    var isFrontCamera by remember { mutableStateOf(true) }
    var statusMessage by remember { mutableStateOf("") }

    var cameraInstance by remember { mutableStateOf<Camera?>(null) }
    var mediaRecorderInstance by remember { mutableStateOf<MediaRecorder?>(null) }
    var recordedFile by remember { mutableStateOf<File?>(null) }
    var surfaceHolderInstance by remember { mutableStateOf<SurfaceHolder?>(null) }

    var systemVideoUri by remember { mutableStateOf<Uri?>(null) }
    var systemVideoFile by remember { mutableStateOf<File?>(null) }

    // Fallback System Camera Launcher
    val systemCameraVideoLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CaptureVideo()
    ) { success ->
        if (success) {
            val file = systemVideoFile
            if (file != null && file.exists() && file.length() > 0) {
                try {
                    val bytes = FileInputStream(file).use { it.readBytes() }
                    file.delete()
                    val base64Video = Base64.encodeToString(bytes, Base64.NO_WRAP)
                    onSendVideoNote(base64Video)
                    onDismiss()
                } catch (e: Exception) {
                    Log.e("VideoNoteRecorder", "Error reading system video file: ${e.message}")
                }
            }
        }
    }

    LaunchedEffect(isRecording) {
        if (isRecording) {
            recDurationSeconds = 0
            while (isRecording) {
                delay(1000)
                recDurationSeconds += 1
                if (recDurationSeconds >= 60) {
                    isRecording = false
                }
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            try {
                mediaRecorderInstance?.stop()
                mediaRecorderInstance?.release()
            } catch (e: Exception) {}
            try {
                cameraInstance?.stopPreview()
                cameraInstance?.release()
            } catch (e: Exception) {}
        }
    }

    val formattedRecDuration = String.format(java.util.Locale.getDefault(), "%02d:%02d", recDurationSeconds / 60, recDurationSeconds % 60)

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
                    text = "📹 DOIRA VIDEO XABAR YOZISH",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // CIRCULAR CAMERA PREVIEW
                Box(
                    modifier = Modifier
                        .size(190.dp)
                        .clip(CircleShape)
                        .border(3.dp, if (isRecording) EmergencyRed else Color(0xFF00E676), CircleShape)
                        .background(Color.Black),
                    contentAlignment = Alignment.Center
                ) {
                    AndroidView(
                        factory = { ctx ->
                            SurfaceView(ctx).apply {
                                holder.addCallback(object : SurfaceHolder.Callback {
                                    override fun surfaceCreated(holder: SurfaceHolder) {
                                        surfaceHolderInstance = holder
                                        try {
                                            val cameraId = if (isFrontCamera) getFrontCameraId() else Camera.CameraInfo.CAMERA_FACING_BACK
                                            cameraInstance = Camera.open(cameraId).apply {
                                                setDisplayOrientation(90)
                                                setPreviewDisplay(holder)
                                                startPreview()
                                            }
                                        } catch (e: Exception) {
                                            Log.e("VideoNoteRecorder", "Error opening camera: ${e.message}")
                                            statusMessage = "Tizim kamerasini ishlatish uchun pastdagi tugmani bosing"
                                        }
                                    }

                                    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {}

                                    override fun surfaceDestroyed(holder: SurfaceHolder) {
                                        try {
                                            cameraInstance?.stopPreview()
                                            cameraInstance?.release()
                                        } catch (e: Exception) {}
                                        cameraInstance = null
                                    }
                                })
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // TIMER & CAMERA FLIP ROW
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isRecording) "🔴 $formattedRecDuration (Yozilmoqda...)" else "Tayyor: Yozishni bosing",
                        color = if (isRecording) EmergencyRed else Color.LightGray,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )

                    IconButton(
                        onClick = {
                            if (!isRecording) {
                                isFrontCamera = !isFrontCamera
                                try {
                                    cameraInstance?.stopPreview()
                                    cameraInstance?.release()
                                    val cameraId = if (isFrontCamera) getFrontCameraId() else Camera.CameraInfo.CAMERA_FACING_BACK
                                    cameraInstance = Camera.open(cameraId).apply {
                                        setDisplayOrientation(90)
                                        surfaceHolderInstance?.let { setPreviewDisplay(it) }
                                        startPreview()
                                    }
                                } catch (e: Exception) {
                                    Log.e("VideoNoteRecorder", "Error flipping camera: ${e.message}")
                                }
                            }
                        },
                        enabled = !isRecording
                    ) {
                        Icon(
                            imageVector = Icons.Default.Cameraswitch,
                            contentDescription = "Kamera O'zgartirish",
                            tint = Color(0xFF00B0FF)
                        )
                    }
                }

                if (statusMessage.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = statusMessage,
                        color = Color(0xFFFFD54F),
                        fontSize = 11.sp
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // RECORD & SEND BUTTONS
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (!isRecording) {
                        Button(
                            onClick = {
                                try {
                                    val cam = cameraInstance
                                    if (cam != null) {
                                        val cameraId = if (isFrontCamera) getFrontCameraId() else Camera.CameraInfo.CAMERA_FACING_BACK
                                        cam.unlock()
                                        recordedFile = File.createTempFile("videonote_", ".mp4", context.cacheDir)

                                        val profile = if (CamcorderProfile.hasProfile(cameraId, CamcorderProfile.QUALITY_480P)) {
                                            CamcorderProfile.get(cameraId, CamcorderProfile.QUALITY_480P)
                                        } else {
                                            CamcorderProfile.get(cameraId, CamcorderProfile.QUALITY_LOW)
                                        }

                                        mediaRecorderInstance = MediaRecorder().apply {
                                            setCamera(cam)
                                            setAudioSource(MediaRecorder.AudioSource.MIC)
                                            setVideoSource(MediaRecorder.VideoSource.CAMERA)
                                            setProfile(profile)
                                            setOrientationHint(if (isFrontCamera) 270 else 90)
                                            setOutputFile(recordedFile?.absolutePath)
                                            surfaceHolderInstance?.let { setPreviewDisplay(it.surface) }
                                            prepare()
                                            start()
                                        }
                                        isRecording = true
                                        statusMessage = ""
                                    } else {
                                        // Fallback to System Camera App
                                        val sysFile = File.createTempFile("sys_videonote_", ".mp4", context.cacheDir)
                                        systemVideoFile = sysFile
                                        val sysUri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", sysFile)
                                        systemVideoUri = sysUri
                                        systemCameraVideoLauncher.launch(sysUri)
                                    }
                                } catch (e: Exception) {
                                    Log.e("VideoNoteRecorder", "Error starting video recording, launching system camera: ${e.message}")
                                    try {
                                        val sysFile = File.createTempFile("sys_videonote_", ".mp4", context.cacheDir)
                                        systemVideoFile = sysFile
                                        val sysUri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", sysFile)
                                        systemVideoUri = sysUri
                                        systemCameraVideoLauncher.launch(sysUri)
                                    } catch (e2: Exception) {
                                        statusMessage = "Kamera xatoligi: ${e2.message}"
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = EmergencyRed,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.FiberManualRecord,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("🔴 YOZISH", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }

                        // SYSTEM CAMERA BACKUP BUTTON
                        Button(
                            onClick = {
                                try {
                                    val sysFile = File.createTempFile("sys_videonote_", ".mp4", context.cacheDir)
                                    systemVideoFile = sysFile
                                    val sysUri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", sysFile)
                                    systemVideoUri = sysUri
                                    systemCameraVideoLauncher.launch(sysUri)
                                } catch (e: Exception) {
                                    statusMessage = "Kamera ochishda xatolik: ${e.message}"
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF00B0FF),
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("📷 KAMERA", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                    } else {
                        Button(
                            onClick = {
                                isRecording = false
                                try {
                                    mediaRecorderInstance?.stop()
                                    mediaRecorderInstance?.release()
                                } catch (e: Exception) {}
                                mediaRecorderInstance = null

                                val file = recordedFile
                                if (file != null && file.exists() && file.length() > 0) {
                                    try {
                                        val bytes = FileInputStream(file).use { it.readBytes() }
                                        file.delete()
                                        val base64Video = Base64.encodeToString(bytes, Base64.NO_WRAP)
                                        onSendVideoNote(base64Video)
                                        onDismiss()
                                    } catch (e: Exception) {
                                        Log.e("VideoNoteRecorder", "Error reading video note file: ${e.message}")
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF00E676),
                                contentColor = Color.Black
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Stop,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("YUBORISH 📤", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("BEKOR QILISH", color = Color.Gray, fontWeight = FontWeight.Bold)
            }
        }
    )
}

@Suppress("DEPRECATION")
private fun getFrontCameraId(): Int {
    val cameraInfo = Camera.CameraInfo()
    for (i in 0 until Camera.getNumberOfCameras()) {
        Camera.getCameraInfo(i, cameraInfo)
        if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            return i
        }
    }
    return Camera.CameraInfo.CAMERA_FACING_BACK
}
