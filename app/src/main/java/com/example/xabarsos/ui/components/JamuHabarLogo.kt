package com.example.xabarsos.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun JamuHabarLogo(
    modifier: Modifier = Modifier,
    size: Dp = 180.dp
) {
    val neonGreen = Color(0xFF00E676)
    val darkBgGradient = Brush.radialGradient(
        colors = listOf(
            Color(0xFF0B331A),
            Color(0xFF04140B),
            Color(0xFF010603)
        )
    )

    val isSmall = size < 60.dp

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(darkBgGradient)
            .border(
                width = if (isSmall) 1.5.dp else (size.value * 0.035f).dp,
                color = neonGreen,
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        if (isSmall) {
            // Crisp, perfectly centered Chat Bubble + Signal Waves for TopAppBar
            Canvas(
                modifier = Modifier.size(size * 0.65f)
            ) {
                val w = this.size.width
                val h = this.size.height

                // Draw Chat Bubble
                val bubblePath = Path().apply {
                    moveTo(w * 0.15f, h * 0.3f)
                    cubicTo(w * 0.15f, h * 0.15f, w * 0.75f, h * 0.15f, w * 0.75f, h * 0.3f)
                    cubicTo(w * 0.75f, h * 0.6f, w * 0.5f, h * 0.7f, w * 0.3f, h * 0.7f)
                    lineTo(w * 0.12f, h * 0.88f)
                    lineTo(w * 0.18f, h * 0.6f)
                    cubicTo(w * 0.08f, h * 0.5f, w * 0.08f, h * 0.35f, w * 0.15f, h * 0.3f)
                    close()
                }

                drawPath(
                    path = bubblePath,
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0xFF69F0AE), Color(0xFF00C853))
                    )
                )

                // 3 White Dots
                val dotRadius = w * 0.05f
                val dotY = h * 0.42f
                drawCircle(color = Color.White, radius = dotRadius, center = Offset(w * 0.32f, dotY))
                drawCircle(color = Color.White, radius = dotRadius, center = Offset(w * 0.45f, dotY))
                drawCircle(color = Color.White, radius = dotRadius, center = Offset(w * 0.58f, dotY))

                // 2 Radio Signal Waves arcs
                val arcStroke = Stroke(width = w * 0.07f)
                drawArc(
                    color = neonGreen,
                    startAngle = -80f,
                    sweepAngle = 70f,
                    useCenter = false,
                    topLeft = Offset(w * 0.58f, h * 0.05f),
                    size = Size(w * 0.35f, h * 0.35f),
                    style = arcStroke
                )
                drawArc(
                    color = neonGreen,
                    startAngle = -85f,
                    sweepAngle = 75f,
                    useCenter = false,
                    topLeft = Offset(w * 0.68f, -h * 0.05f),
                    size = Size(w * 0.48f, h * 0.48f),
                    style = arcStroke
                )
            }
        } else {
            // Full Emblem for Splash Screen & Large displays
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = (size.value * 0.04f).dp)
            ) {
                Canvas(
                    modifier = Modifier.size((size.value * 0.4f).dp)
                ) {
                    val w = this.size.width
                    val h = this.size.height

                    val bubblePath = Path().apply {
                        moveTo(w * 0.15f, h * 0.25f)
                        cubicTo(w * 0.15f, h * 0.1f, w * 0.75f, h * 0.1f, w * 0.75f, h * 0.25f)
                        cubicTo(w * 0.75f, h * 0.55f, w * 0.5f, h * 0.65f, w * 0.3f, h * 0.65f)
                        lineTo(w * 0.1f, h * 0.8f)
                        lineTo(w * 0.15f, h * 0.55f)
                        cubicTo(w * 0.05f, h * 0.45f, w * 0.05f, h * 0.3f, w * 0.15f, h * 0.25f)
                        close()
                    }

                    drawPath(
                        path = bubblePath,
                        brush = Brush.verticalGradient(
                            colors = listOf(Color(0xFF69F0AE), Color(0xFF00C853))
                        )
                    )

                    val dotRadius = w * 0.045f
                    val dotY = h * 0.36f
                    drawCircle(color = Color.White, radius = dotRadius, center = Offset(w * 0.32f, dotY))
                    drawCircle(color = Color.White, radius = dotRadius, center = Offset(w * 0.45f, dotY))
                    drawCircle(color = Color.White, radius = dotRadius, center = Offset(w * 0.58f, dotY))

                    val arcStroke = Stroke(width = w * 0.06f)
                    drawArc(
                        color = neonGreen,
                        startAngle = -80f,
                        sweepAngle = 70f,
                        useCenter = false,
                        topLeft = Offset(w * 0.58f, h * 0.02f),
                        size = Size(w * 0.32f, h * 0.32f),
                        style = arcStroke
                    )
                    drawArc(
                        color = neonGreen,
                        startAngle = -85f,
                        sweepAngle = 75f,
                        useCenter = false,
                        topLeft = Offset(w * 0.68f, -h * 0.08f),
                        size = Size(w * 0.45f, h * 0.45f),
                        style = arcStroke
                    )
                }

                Spacer(modifier = Modifier.height((size.value * 0.02f).dp))

                Text(
                    text = "JAMU",
                    color = Color.White,
                    fontSize = (size.value * 0.16f).sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                )

                Box(
                    modifier = Modifier
                        .padding(top = (size.value * 0.005f).dp)
                        .border(
                            width = 1.dp,
                            color = neonGreen.copy(alpha = 0.8f),
                            shape = CircleShape
                        )
                        .padding(horizontal = (size.value * 0.07f).dp, vertical = (size.value * 0.005f).dp)
                ) {
                    Text(
                        text = "— HABAR —",
                        color = neonGreen,
                        fontSize = (size.value * 0.08f).sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp
                    )
                }
            }
        }
    }
}
