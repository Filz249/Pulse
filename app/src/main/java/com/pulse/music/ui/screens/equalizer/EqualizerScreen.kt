package com.pulse.music.ui.screens.equalizer

import android.media.audiofx.Equalizer
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pulse.music.player.AudioSessionState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EqualizerScreen(onBackClick: () -> Unit, modifier: Modifier = Modifier) {
    val audioSessionId by AudioSessionState.audioSessionId.collectAsState()
    val equalizer = remember(audioSessionId) {
        if (audioSessionId > 0) runCatching {
            Equalizer(0, audioSessionId).apply { enabled = true }
        }.getOrNull() else null
    }
    DisposableEffect(equalizer) {
        onDispose { runCatching { equalizer?.release() } }
    }

    val bandCount = equalizer?.numberOfBands?.toInt() ?: 0
    val bandRange = equalizer?.bandLevelRange ?: shortArrayOf(-1500, 1500)
    val minLevel = bandRange[0].toFloat()
    val maxLevel = bandRange[1].toFloat()
    var levels by remember(equalizer) {
        mutableStateOf(List(bandCount) { index ->
            runCatching { equalizer?.getBandLevel(index.toShort())?.toFloat() ?: 0f }.getOrDefault(0f)
        })
    }
    var presetMenuOpen by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = { Text("Equalizer") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(Icons.Default.GraphicEq, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text("Shape your sound", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                    Text("Adjust the sound output while Pulse is playing music.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            when {
                audioSessionId <= 0 -> {
                    Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = MaterialTheme.shapes.large) {
                        Text(
                            "Start playing a song to connect the equalizer to Pulse's audio session.",
                            Modifier.padding(18.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                equalizer == null || bandCount == 0 -> {
                    Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = MaterialTheme.shapes.large) {
                        Text(
                            "The audio equalizer is not available on this device.",
                            Modifier.padding(18.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                else -> {
                    Box {
                        OutlinedButton(onClick = { presetMenuOpen = true }, modifier = Modifier.fillMaxWidth()) {
                            Text("Choose preset")
                        }
                        DropdownMenu(expanded = presetMenuOpen, onDismissRequest = { presetMenuOpen = false }) {
                            repeat(equalizer.numberOfPresets.toInt()) { preset ->
                                DropdownMenuItem(
                                    text = { Text(equalizer.getPresetName(preset.toShort()).toString()) },
                                    onClick = {
                                        equalizer.usePreset(preset.toShort())
                                        levels = List(bandCount) { band -> equalizer.getBandLevel(band.toShort()).toFloat() }
                                        presetMenuOpen = false
                                    }
                                )
                            }
                        }
                    }

                    Text("Frequency bands", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    levels.forEachIndexed { index, level ->
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                val frequencyHz = equalizer.getCenterFreq(index.toShort()) / 1000
                                Text(
                                    if (frequencyHz >= 1000) "${frequencyHz / 1000.0} kHz" else "${frequencyHz} Hz",
                                    style = MaterialTheme.typography.labelLarge
                                )
                                Text("${level.toInt()} dB/100", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Slider(
                                value = level,
                                onValueChange = { newValue ->
                                    equalizer.setBandLevel(index.toShort(), newValue.toInt().toShort())
                                    levels = levels.toMutableList().also { it[index] = newValue }
                                },
                                valueRange = minLevel..maxLevel
                            )
                        }
                    }
                    OutlinedButton(
                        onClick = {
                            levels = List(bandCount) { index ->
                                equalizer.setBandLevel(index.toShort(), 0)
                                0f
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Reset equalizer")
                    }
                }
            }
            Spacer(Modifier.padding(bottom = 8.dp))
        }
    }
}
