package com.pulse.music.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

enum class PulsePalette(val key: String, val label: String, val primary: Color, val secondary: Color) {
    AMBER("amber", "Amber", Color(0xFFFFAE42), Color(0xFFF2994A)),
    OCEAN("ocean", "Ocean", Color(0xFF69C9C2), Color(0xFF38AFA8)),
    VIOLET("violet", "Violet", Color(0xFFB7A0FF), Color(0xFF9879F0));

    companion object {
        fun fromKey(key: String?): PulsePalette =
            values().firstOrNull { it.key == key } ?: AMBER
    }
}

val PulseAmber = Color(0xFFFFAE42)
val PulseAmberBright = Color(0xFFF2994A)
val DarkBackground = Color(0xFF0F0F0F)
val DarkSurface = Color(0xFF1A1210)
val DarkSurfaceVariant = Color(0xFF2B2019)
val LightText = Color(0xFFF4EAE2)
val MutedText = Color(0xFFB8AFA8)

@Composable
fun PulseTheme(palette: PulsePalette = PulsePalette.AMBER, customPrimary: Color? = null, content: @Composable () -> Unit) {
    val primary = customPrimary ?: palette.primary
    val secondary = customPrimary?.copy(alpha = 0.82f) ?: palette.secondary
    val colors = darkColorScheme(
        primary = primary,
        onPrimary = Color(0xFF1B120A),
        secondary = secondary,
        onSecondary = Color(0xFF1B120A),
        background = DarkBackground,
        surface = DarkSurface,
        surfaceVariant = DarkSurfaceVariant,
        onBackground = LightText,
        onSurface = LightText,
        onSurfaceVariant = MutedText,
        outline = Color(0xFF6A4C36),
        outlineVariant = Color(0xFF4A3626)
    )
    MaterialTheme(colorScheme = colors, content = content)
}
