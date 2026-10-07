package com.example.xabarsos.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun LiveAudioWaveformVisualizer(
    modifier: Modifier = Modifier,
    isRecording: Boolean = true,
    width: Dp = 90.dp,
    height: Dp = 26.dp
) {
    val infiniteTransition = rememberInfiniteTransition(label = "waveform")

    val wave1 by infiniteTransition.animateFloat(
        initialValue = 0.2f, targetValue = 0.95f,
        animationSpec = infiniteRepeatable(tween(220, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "w1"
    )
    val wave2 by infiniteTransition.animateFloat(
        initialValue = 0.85f, targetValue = 0.25f,
        animationSpec = infiniteRepeatable(tween(280, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "w2"
    )
    val wave3 by infiniteTransition.animateFloat(
        initialValue = 0.35f, targetValue = 1.0f,
        animationSpec = infiniteRepeatable(tween(180, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "w3"
    )
    val wave4 by infiniteTransition.animateFloat(
        initialValue = 0.9f, targetValue = 0.3f,
        animationSpec = infiniteRepeatable(tween(240, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "w4"
    )

    val barScales = listOf(wave1, wave2, wave3, wave4, wave2, wave1, wave3, wave4, wave2, wave1)

    Box(
        modifier = modifier
            .width(width)
            .height(height),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.width(width).height(height)) {
            val canvasW = size.width
            val canvasH = size.height
            val barCount = barScales.size
            val barSpacing = 3f
            val totalSpacing = (barCount - 1) * barSpacing
            val barWidth = (canvasW - totalSpacing) / barCount

            val barBrush = Brush.verticalGradient(
                colors = listOf(
                    Color(0xFF00E676), // Neon Green Top
                    Color(0xFF00B0FF)  // Neon Cyan Bottom
                )
            )

            for (i in 0 until barCount) {
                val scale = if (isRecording) barScales[i] else 0.2f
                val barH = (canvasH * scale).coerceAtLeast(4f)
                val left = i * (barWidth + barSpacing)
                val top = (canvasH - barH) / 2f

                drawRoundRect(
                    brush = barBrush,
                    topLeft = Offset(left, top),
                    size = Size(barWidth, barH),
                    cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
                )
            }
        }
    }
}
