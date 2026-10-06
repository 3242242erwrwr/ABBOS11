package com.example.xabarsos.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.PhoneInTalk
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.xabarsos.audio.CallSession
import com.example.xabarsos.audio.CallState
import com.example.xabarsos.ui.theme.DarkCardContainer
import com.example.xabarsos.ui.theme.EmergencyRed

@Composable
fun VoiceCallDialog(
    callState: CallState,
    currentSession: CallSession?,
    isMuted: Boolean,
    isSpeakerOn: Boolean,
    durationSeconds: Int,
    onAcceptCall: () -> Unit,
    onRejectOrEndCall: () -> Unit,
    onToggleMute: () -> Unit,
    onToggleSpeaker: () -> Unit,
    onDismiss: () -> Unit
) {
    if (callState == CallState.IDLE || currentSession == null) return

    val infiniteTransition = rememberInfiniteTransition()
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    val formattedDuration = String.format(java.util.Locale.getDefault(), "%02d:%02d", durationSeconds / 60, durationSeconds % 60)

    AlertDialog(
        onDismissRequest = {
            if (callState == CallState.INCOMING_RINGING || callState == CallState.OUTGOING_RINGING) {
                onRejectOrEndCall()
            }
        },
        containerColor = DarkCardContainer,
        titleContentColor = Color.White,
        textContentColor = Color.White,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.PhoneInTalk,
                    contentDescription = null,
                    tint = Color(0xFF00E676),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "📞 OVOZLI QO'NG'IROQ",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // CALL AVATAR LOGO WITH PULSE
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .scale(if (callState == CallState.CONNECTED) 1f else pulseScale)
                        .clip(CircleShape)
                        .background(
                            when (callState) {
                                CallState.CONNECTED -> Color(0xFF00E676).copy(alpha = 0.2f)
                                CallState.INCOMING_RINGING -> Color(0xFFFF9100).copy(alpha = 0.2f)
                                else -> Color(0xFF00B0FF).copy(alpha = 0.2f)
                            }
                        )
                        .border(
                            2.dp,
                            when (callState) {
                                CallState.CONNECTED -> Color(0xFF00E676)
                                CallState.INCOMING_RINGING -> Color(0xFFFF9100)
                                else -> Color(0xFF00B0FF)
                            },
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = currentSession.peerName.take(1).uppercase(),
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // PEER NAME
                Text(
                    text = "👤 ${currentSession.peerName}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(4.dp))

                // STATUS TEXT
                val statusText = when (callState) {
                    CallState.OUTGOING_RINGING -> "Gudok borilmoqda..."
                    CallState.INCOMING_RINGING -> "📥 Ovozli qo'ng'iroq kelmoqda..."
                    CallState.CONNECTED -> "🟢 Muloqotda ($formattedDuration)"
                    CallState.ENDED -> "🔴 Qo'ng'iroq yakunlandi"
                    else -> ""
                }

                Text(
                    text = statusText,
                    color = when (callState) {
                        CallState.CONNECTED -> Color(0xFF00E676)
                        CallState.INCOMING_RINGING -> Color(0xFFFFD54F)
                        else -> Color.LightGray
                    },
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(16.dp))

                // INCOMING CALL ACCEPT / REJECT BUTTONS
                if (callState == CallState.INCOMING_RINGING) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // ACCEPT BUTTON
                        Button(
                            onClick = onAcceptCall,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF00E676),
                                contentColor = Color.Black
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.height(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Call,
                                contentDescription = "Javob berish",
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("JAVOB BERISH", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }

                        // REJECT BUTTON
                        Button(
                            onClick = onRejectOrEndCall,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = EmergencyRed,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.height(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CallEnd,
                                contentDescription = "Rad etish",
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("RAD ETISH", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                } else if (callState == CallState.CONNECTED || callState == CallState.OUTGOING_RINGING) {
                    // ACTIVE CALL CONTROL BUTTONS (Mute, Speaker, Hangup)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // MUTE TOGGLE
                        IconButton(
                            onClick = onToggleMute,
                            modifier = Modifier
                                .size(48.dp)
                                .background(
                                    if (isMuted) EmergencyRed else Color(0xFF22222E),
                                    CircleShape
                                )
                        ) {
                            Icon(
                                imageVector = if (isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                                contentDescription = "Mute",
                                tint = Color.White
                            )
                        }

                        // HANG UP BUTTON
                        IconButton(
                            onClick = onRejectOrEndCall,
                            modifier = Modifier
                                .size(56.dp)
                                .background(EmergencyRed, CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CallEnd,
                                contentDescription = "Tugatish",
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                        }

                        // SPEAKER TOGGLE
                        IconButton(
                            onClick = onToggleSpeaker,
                            modifier = Modifier
                                .size(48.dp)
                                .background(
                                    if (isSpeakerOn) Color(0xFF00B0FF) else Color(0xFF22222E),
                                    CircleShape
                                )
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                contentDescription = "Dinamik",
                                tint = Color.White
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {}
    )
}
