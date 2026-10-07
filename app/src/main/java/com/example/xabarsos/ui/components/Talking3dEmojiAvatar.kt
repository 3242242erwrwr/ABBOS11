package com.example.xabarsos.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.xabarsos.audio.VoiceEffect

@Composable
fun Talking3dEmojiAvatar(
    isPlaying: Boolean,
    effect: VoiceEffect = VoiceEffect.NORMAL,
    size: Dp = 120.dp
) {
    val infiniteTransition = rememberInfiniteTransition(label = "lipSync")

    // Lip sync mouth opening animation (0.1f to 1.0f)
    val mouthOpenScale by infiniteTransition.animateFloat(
        initialValue = 0.15f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 140, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "mouthOpen"
    )

    // Subtle 3D head sway animation
    val headSway by infiniteTransition.animateFloat(
        initialValue = -3f,
        targetValue = 3f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "headSway"
    )

    val currentMouthOpen = if (isPlaying) mouthOpenScale else 0.1f

    Box(
        modifier = Modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val w = this.size.width
            val h = this.size.height
            val centerX = w / 2f
            val centerY = h / 2f
            val radius = w * 0.44f

            // 1. 3D SPHERICAL SHADING BRUSH
            val (baseColor, highlightColor, rimColor) = when (effect) {
                VoiceEffect.ROBOT -> Triple(Color(0xFF37474F), Color(0xFF90A4AE), Color(0xFF00E5FF))
                VoiceEffect.HORROR -> Triple(Color(0xFF880E4F), Color(0xFFFF4081), Color(0xFFFF1744))
                VoiceEffect.BABY -> Triple(Color(0xFFF8BBD0), Color(0xFFFFF0F5), Color(0xFFFF4081))
                VoiceEffect.OLD_MAN -> Triple(Color(0xFF5D4037), Color(0xFFD7CCC8), Color(0xFFFFB300))
                VoiceEffect.GIRL -> Triple(Color(0xFFE91E63), Color(0xFFF8BBD0), Color(0xFFFF80AB))
                else -> Triple(Color(0xFFFFB300), Color(0xFFFFF9C4), Color(0xFFFFD54F))
            }

            val sphereBrush = Brush.radialGradient(
                colors = listOf(highlightColor, baseColor, Color(0xFF0A0A0E)),
                center = Offset(centerX - radius * 0.35f, centerY - radius * 0.35f),
                radius = radius * 1.35f
            )

            // Draw 3D Spherical Head
            drawCircle(
                brush = sphereBrush,
                radius = radius,
                center = Offset(centerX + (if (isPlaying) headSway else 0f), centerY)
            )

            // 3D Rim Light Glow
            drawCircle(
                color = rimColor.copy(alpha = 0.3f),
                radius = radius,
                center = Offset(centerX, centerY),
                style = Stroke(width = w * 0.04f)
            )

            // Glossy 3D Specular Highlight
            drawCircle(
                color = Color.White.copy(alpha = 0.55f),
                radius = radius * 0.18f,
                center = Offset(centerX - radius * 0.4f, centerY - radius * 0.4f)
            )

            // 2. 3D EYES (Left & Right)
            val eyeOffsetY = centerY - radius * 0.18f
            val leftEyeX = centerX - radius * 0.32f
            val rightEyeX = centerX + radius * 0.32f
            val eyeRadius = radius * 0.18f

            if (effect == VoiceEffect.ROBOT) {
                // Robot Cyan Visor Eyes
                drawRoundRect(
                    color = Color(0xFF00E5FF),
                    topLeft = Offset(centerX - radius * 0.5f, eyeOffsetY - eyeRadius * 0.6f),
                    size = Size(radius * 1.0f, eyeRadius * 1.2f),
                    cornerRadius = CornerRadius(8f, 8f)
                )
            } else {
                // Left 3D Eye
                drawCircle(color = Color.White, radius = eyeRadius, center = Offset(leftEyeX, eyeOffsetY))
                drawCircle(color = Color(0xFF1A237E), radius = eyeRadius * 0.55f, center = Offset(leftEyeX + 1f, eyeOffsetY + 1f))
                drawCircle(color = Color.White, radius = eyeRadius * 0.2f, center = Offset(leftEyeX - 2f, eyeOffsetY - 2f))

                // Right 3D Eye
                drawCircle(color = Color.White, radius = eyeRadius, center = Offset(rightEyeX, eyeOffsetY))
                drawCircle(color = Color(0xFF1A237E), radius = eyeRadius * 0.55f, center = Offset(rightEyeX + 1f, eyeOffsetY + 1f))
                drawCircle(color = Color.White, radius = eyeRadius * 0.2f, center = Offset(rightEyeX - 2f, eyeOffsetY - 2f))
            }

            // 3. DYNAMIC 3D TALKING MOUTH (REAL-TIME LIP SYNC)
            val mouthY = centerY + radius * 0.28f
            val mouthWidth = radius * 0.7f
            val mouthHeight = (radius * 0.65f) * currentMouthOpen

            val mouthPath = Path().apply {
                val left = centerX - mouthWidth / 2f
                val right = centerX + mouthWidth / 2f
                val top = mouthY - mouthHeight / 2f
                val bottom = mouthY + mouthHeight / 2f

                moveTo(left, mouthY)
                cubicTo(left, top, right, top, right, mouthY)
                cubicTo(right, bottom, left, bottom, left, mouthY)
                close()
            }

            // Dark Inner Mouth Cavity
            drawPath(
                path = mouthPath,
                color = Color(0xFF1A0000)
            )

            // Red Tongue inside mouth when open
            if (currentMouthOpen > 0.3f) {
                val tonguePath = Path().apply {
                    val left = centerX - mouthWidth * 0.3f
                    val right = centerX + mouthWidth * 0.3f
                    val top = mouthY + mouthHeight * 0.1f
                    val bottom = mouthY + mouthHeight * 0.45f

                    moveTo(left, top)
                    cubicTo(left, bottom, right, bottom, right, top)
                    close()
                }
                drawPath(path = tonguePath, color = Color(0xFFFF5252))
            }

            // White Top Teeth inside mouth when open
            if (currentMouthOpen > 0.4f) {
                drawRoundRect(
                    color = Color.White,
                    topLeft = Offset(centerX - mouthWidth * 0.25f, mouthY - mouthHeight * 0.4f),
                    size = Size(mouthWidth * 0.5f, mouthHeight * 0.25f),
                    cornerRadius = CornerRadius(2f, 2f)
                )
            }

            // Outer Lip Contour Border
            drawPath(
                path = mouthPath,
                color = Color(0xFF3E2723),
                style = Stroke(width = w * 0.035f)
            )
        }
    }
}
