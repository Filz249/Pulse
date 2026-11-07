package com.pulse.music.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val PulseAmber = Color(0xFFFFB347)
val PulseAmberBright = Color(0xFFFF9F2E)
val DarkBackground = Color(0xFF111011)
val DarkSurface = Color(0xFF1B1818)
val DarkSurfaceVariant = Color(0xFF292322)
val LightText = Color(0xFFF6EEE5)
val MutedText = Color(0xFFB9AAA0)

private val PulseColorScheme = darkColorScheme(
    primary = PulseAmber, onPrimary = Color(0xFF1B120A),
    secondary = PulseAmberBright, onSecondary = Color(0xFF1B120A),
    background = DarkBackground, surface = DarkSurface, surfaceVariant = DarkSurfaceVariant,
    onBackground = LightText, onSurface = LightText, onSurfaceVariant = MutedText,
    outline = Color(0xFF6F5A4A), outlineVariant = Color(0xFF47372E)
)

@Composable
fun PulseTheme(darkTheme: Boolean = true, content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = PulseColorScheme, content = content)
}
