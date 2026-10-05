package com.example.xabarsos.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = EmergencyRed,
    onPrimary = Color.White,
    secondary = NeonBlue,
    onSecondary = Color.Black,
    tertiary = EmergencyPink,
    background = DarkBackground,
    onBackground = TextPrimary,
    surface = DarkSurface,
    onSurface = TextPrimary,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    surfaceContainer = DarkCardContainer,
    error = EmergencyRed,
    onError = Color.White
)

@Composable
fun XABARSOSTheme(
    darkTheme: Boolean = true, // Force Dark Theme for emergency SOS look & feel
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
