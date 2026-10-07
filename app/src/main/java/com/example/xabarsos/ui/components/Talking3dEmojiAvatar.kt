package com.example.xabarsos.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
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
    size: Dp = 130.dp
) {
    val infiniteTransition = rememberInfiniteTransition(label = "lipSync3d")

    // 1. Fast Lip-Sync Mouth Opening (Vertical 0.15f to 1.0f)
    val mouthOpenScale by infiniteTransition.animateFloat(
        initialValue = 0.15f,
        targetValue = 0.98f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 130, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "mouthOpen3d"
    )

    // 2. Multi-Phoneme Mouth Width Variation (Horizontal 0.82f to 1.18f for 'A, O, E, U, M' speech)
    val mouthWidthScale by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.18f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 210, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "mouthWidth3d"
    )

    // 3. Eyebrow Raising / Speech Expression Peak
    val eyebrowRaise by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -6f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 280, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "eyebrowRaise"
    )

    // 4. Subtle 3D Head Motion / Chin Sways Downward on Loud Speech
    val headSwayX by infiniteTransition.animateFloat(
        initialValue = -2.5f,
        targetValue = 2.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "headSwayX"
    )

    val currentMouthOpen = if (isPlaying) mouthOpenScale else 0.08f
    val currentMouthWidth = if (isPlaying) mouthWidthScale else 0.9f
    val currentEyebrowOffset = if (isPlaying) eyebrowRaise else 0f
    val currentChinSwayY = if (isPlaying) currentMouthOpen * 4f else 0f

    Box(
        modifier = Modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val w = this.size.width
            val h = this.size.height
            val centerX = w / 2f + (if (isPlaying) headSwayX else 0f)
            val centerY = h / 2f + currentChinSwayY
            val radius = w * 0.42f

            // A. GROUND DROP SHADOW UNDER 3D AVATAR
            drawOval(
                brush = Brush.radialGradient(
                    colors = listOf(Color.Black.copy(alpha = 0.45f), Color.Transparent),
                    center = Offset(centerX, h * 0.94f),
                    radius = radius * 0.9f
                ),
                topLeft = Offset(centerX - radius * 0.85f, h * 0.88f),
                size = Size(radius * 1.7f, h * 0.12f)
            )

            // B. 3D PIXAR SPHERICAL LIGHTING BRUSH
            val (baseColor, highlightColor, rimGlowColor) = when (effect) {
                VoiceEffect.ROBOT -> Triple(Color(0xFF37474F), Color(0xFFCFD8DC), Color(0xFF00E5FF))
                VoiceEffect.HORROR -> Triple(Color(0xFF880E4F), Color(0xFFFF80AB), Color(0xFFFF1744))
                VoiceEffect.BABY -> Triple(Color(0xFFF8BBD0), Color(0xFFFFFFFF), Color(0xFFFF4081))
                VoiceEffect.OLD_MAN -> Triple(Color(0xFF5D4037), Color(0xFFE0D7D5), Color(0xFFFFB300))
                VoiceEffect.GIRL -> Triple(Color(0xFFD81B60), Color(0xFFFF80AB), Color(0xFFFF4081))
                else -> Triple(Color(0xFFFF8F00), Color(0xFFFFF59D), Color(0xFFFFD54F))
            }

            val sphereBrush = Brush.radialGradient(
                colors = listOf(highlightColor, baseColor, Color(0xFF0D0A02)),
                center = Offset(centerX - radius * 0.38f, centerY - radius * 0.38f),
                radius = radius * 1.4f
            )

            // Draw Primary 3D Sphere Head
            drawCircle(
                brush = sphereBrush,
                radius = radius,
                center = Offset(centerX, centerY)
            )

            // 3D Ambient Rim Light Glow
            drawCircle(
                color = rimGlowColor.copy(alpha = 0.35f),
                radius = radius,
                center = Offset(centerX, centerY),
                style = Stroke(width = w * 0.035f)
            )

            // Glossy Specular Reflection Ring (Top Left Glass Highlight)
            drawOval(
                brush = Brush.verticalGradient(
                    colors = listOf(Color.White.copy(alpha = 0.65f), Color.White.copy(alpha = 0.05f))
                ),
                topLeft = Offset(centerX - radius * 0.6f, centerY - radius * 0.75f),
                size = Size(radius * 0.55f, radius * 0.35f)
            )

            // C. DYNAMIC 3D EYEBROWS
            val eyebrowY = centerY - radius * 0.38f + currentEyebrowOffset
            val leftEyebrowX = centerX - radius * 0.35f
            val rightEyebrowX = centerX + radius * 0.35f

            if (effect != VoiceEffect.ROBOT) {
                val eyebrowPathLeft = Path().apply {
                    moveTo(leftEyebrowX - radius * 0.2f, eyebrowY + 2f)
                    quadraticTo(leftEyebrowX, eyebrowY - radius * 0.12f, leftEyebrowX + radius * 0.2f, eyebrowY)
                }
                val eyebrowPathRight = Path().apply {
                    moveTo(rightEyebrowX - radius * 0.2f, eyebrowY)
                    quadraticTo(rightEyebrowX, eyebrowY - radius * 0.12f, rightEyebrowX + radius * 0.2f, eyebrowY + 2f)
                }
                drawPath(path = eyebrowPathLeft, color = Color(0xFF3E2723), style = Stroke(width = w * 0.045f))
                drawPath(path = eyebrowPathRight, color = Color(0xFF3E2723), style = Stroke(width = w * 0.045f))
            }

            // D. REALISTIC 3D EYES
            val eyeOffsetY = centerY - radius * 0.15f
            val leftEyeX = centerX - radius * 0.30f
            val rightEyeX = centerX + radius * 0.30f
            val eyeRadius = radius * 0.20f

            if (effect == VoiceEffect.ROBOT) {
                // Cyberpunk Metallic Visor
                drawRoundRect(
                    brush = Brush.horizontalGradient(
                        colors = listOf(Color(0xFF00E5FF), Color(0xFF18FFFF), Color(0xFF00E5FF))
                    ),
                    topLeft = Offset(centerX - radius * 0.55f, eyeOffsetY - eyeRadius * 0.7f),
                    size = Size(radius * 1.10f, eyeRadius * 1.4f),
                    cornerRadius = CornerRadius(10f, 10f)
                )
            } else {
                // Left 3D Eye (Socket + White Sclera + Iris + Pupil + Glint)
                drawCircle(color = Color(0xFF261C14), radius = eyeRadius * 1.08f, center = Offset(leftEyeX, eyeOffsetY))
                drawCircle(color = Color.White, radius = eyeRadius, center = Offset(leftEyeX, eyeOffsetY))

                val irisBrush = Brush.radialGradient(
                    colors = listOf(Color(0xFF00E5FF), Color(0xFF0D47A1), Color(0xFF000051)),
                    center = Offset(leftEyeX, eyeOffsetY),
                    radius = eyeRadius * 0.65f
                )
                drawCircle(brush = irisBrush, radius = eyeRadius * 0.62f, center = Offset(leftEyeX, eyeOffsetY))
                drawCircle(color = Color.Black, radius = eyeRadius * 0.35f, center = Offset(leftEyeX, eyeOffsetY))
                drawCircle(color = Color.White, radius = eyeRadius * 0.22f, center = Offset(leftEyeX - eyeRadius * 0.2f, eyeOffsetY - eyeRadius * 0.2f))

                // Right 3D Eye
                drawCircle(color = Color(0xFF261C14), radius = eyeRadius * 1.08f, center = Offset(rightEyeX, eyeOffsetY))
                drawCircle(color = Color.White, radius = eyeRadius, center = Offset(rightEyeX, eyeOffsetY))

                val irisBrushRight = Brush.radialGradient(
                    colors = listOf(Color(0xFF00E5FF), Color(0xFF0D47A1), Color(0xFF000051)),
                    center = Offset(rightEyeX, eyeOffsetY),
                    radius = eyeRadius * 0.65f
                )
                drawCircle(brush = irisBrushRight, radius = eyeRadius * 0.62f, center = Offset(rightEyeX, eyeOffsetY))
                drawCircle(color = Color.Black, radius = eyeRadius * 0.35f, center = Offset(rightEyeX, eyeOffsetY))
                drawCircle(color = Color.White, radius = eyeRadius * 0.22f, center = Offset(rightEyeX - eyeRadius * 0.2f, eyeOffsetY - eyeRadius * 0.2f))
            }

            // E. HYPER-REALISTIC 3D MULTI-STATE TALKING MOUTH (REAL-TIME LIP SYNC)
            val mouthCenterY = centerY + radius * 0.32f
            val baseMouthWidth = radius * 0.72f * currentMouthWidth
            val maxMouthHeight = radius * 0.70f * currentMouthOpen

            val mouthLeftX = centerX - baseMouthWidth / 2f
            val mouthRightX = centerX + baseMouthWidth / 2f
            val mouthTopY = mouthCenterY - maxMouthHeight / 2f
            val mouthBottomY = mouthCenterY + maxMouthHeight / 2f

            val mouthPath = Path().apply {
                moveTo(mouthLeftX, mouthCenterY)
                cubicTo(
                    mouthLeftX + baseMouthWidth * 0.2f, mouthTopY - 4f,
                    mouthRightX - baseMouthWidth * 0.2f, mouthTopY - 4f,
                    mouthRightX, mouthCenterY
                )
                cubicTo(
                    mouthRightX - baseMouthWidth * 0.15f, mouthBottomY + 6f,
                    mouthLeftX + baseMouthWidth * 0.15f, mouthBottomY + 6f,
                    mouthLeftX, mouthCenterY
                )
                close()
            }

            // Dark 3D Inner Mouth Cavity
            drawPath(
                path = mouthPath,
                color = Color(0xFF140202)
            )

            // 3D Animated Pink Tongue
            if (currentMouthOpen > 0.20f) {
                val tongueY = mouthCenterY + maxMouthHeight * 0.12f
                val tonguePath = Path().apply {
                    val tLeft = centerX - baseMouthWidth * 0.32f
                    val tRight = centerX + baseMouthWidth * 0.32f
                    val tBottom = mouthBottomY - 2f

                    moveTo(tLeft, tongueY)
                    cubicTo(tLeft, tBottom + maxMouthHeight * 0.3f, tRight, tBottom + maxMouthHeight * 0.3f, tRight, tongueY)
                    close()
                }
                drawPath(
                    path = tonguePath,
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0xFFFF5252), Color(0xFFD50000))
                    )
                )
            }

            // 3D Top Teeth Arch
            if (currentMouthOpen > 0.30f) {
                val teethWidth = baseMouthWidth * 0.58f
                val teethHeight = (maxMouthHeight * 0.35f).coerceAtLeast(3f)

                drawRoundRect(
                    color = Color.White,
                    topLeft = Offset(centerX - teethWidth / 2f, mouthTopY + 2f),
                    size = Size(teethWidth, teethHeight),
                    cornerRadius = CornerRadius(3f, 3f)
                )

                // Individual Teeth Separator Lines for 3D realism
                val toothStep = teethWidth / 4f
                for (t in 1..3) {
                    val tx = (centerX - teethWidth / 2f) + (t * toothStep)
                    drawLine(
                        color = Color.LightGray.copy(alpha = 0.7f),
                        start = Offset(tx, mouthTopY + 2f),
                        end = Offset(tx, mouthTopY + 2f + teethHeight),
                        strokeWidth = 1.5f
                    )
                }
            }

            // 3D Outer Lip Highlight & Contour Frame
            val lipColor = when (effect) {
                VoiceEffect.GIRL -> Color(0xFFC2185B)
                VoiceEffect.HORROR -> Color(0xFF4A0007)
                else -> Color(0xFF4E260E)
            }

            drawPath(
                path = mouthPath,
                color = lipColor,
                style = Stroke(width = w * 0.038f)
            )
        }
    }
}
