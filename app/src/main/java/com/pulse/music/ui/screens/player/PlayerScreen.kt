package com.pulse.music.ui.screens.player

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.pulse.music.domain.model.Song
import java.util.Locale
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(
    viewModel: PlayerViewModel,
    onBackClick: () -> Unit,
    onEditSongTags: (Song) -> Unit,
    modifier: Modifier = Modifier
) {
    val song by viewModel.currentSong.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val shuffleMode by viewModel.shuffleMode.collectAsState()
    val repeatMode by viewModel.repeatMode.collectAsState()
    val currentPosition by viewModel.currentPosition.collectAsState()
    val duration = if (viewModel.duration > 0) viewModel.duration else (song?.duration ?: 1L)
    var isUserSeeking by remember { mutableStateOf(false) }
    var seekPosition by remember { mutableFloatStateOf(0f) }
    var menuExpanded by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(1.dp)) {
                        Text("P U L S E", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                        Text("NOW PLAYING", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onSurface)
                    }
                },
                actions = {
                    Box {
                        IconButton(onClick = { menuExpanded = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "More options", tint = MaterialTheme.colorScheme.onSurface)
                        }
                        DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                            song?.let { currentSong ->
                                DropdownMenuItem(
                                    text = { Text("Edit song information") },
                                    leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                                    onClick = { menuExpanded = false; onEditSongTags(currentSong) }
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(Color(0xFF4D2B14), Color(0xFF1A1210), Color(0xFF0F0F0F))))
                .padding(padding)
        ) {
            val artworkSize = minOf(maxWidth - 48.dp, maxHeight * 0.41f, 342.dp).coerceAtLeast(160.dp)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = maxHeight)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 14.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                Surface(
                    modifier = Modifier.size(artworkSize),
                    shape = RoundedCornerShape(32.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shadowElevation = 14.dp
                ) {
                    if (song?.albumArtUri != null) {
                        AsyncImage(
                            model = song.albumArtUri,
                            contentDescription = "Album cover",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.MusicNote, contentDescription = null, modifier = Modifier.size(92.dp), tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }

                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.Start, verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(
                        song?.title ?: "No track playing",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFF4EAE2),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        song?.let { listOfNotNull(it.artist, it.year.takeIf { year -> year > 0 }?.toString()).joinToString(" · ") } ?: "-",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color(0xFFB8AFA8),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    val progress = if (isUserSeeking) seekPosition else (currentPosition.toFloat() / duration).coerceIn(0f, 1f)
                    Slider(
                        value = progress,
                        onValueChange = { isUserSeeking = true; seekPosition = it },
                        onValueChangeFinished = {
                            viewModel.seekTo((seekPosition * duration).toLong())
                            isUserSeeking = false
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = SliderDefaults.colors(
                            thumbColor = Color(0xFFFFAE42),
                            activeTrackColor = Color(0xFFFFAE42),
                            inactiveTrackColor = Color(0xFF5A493C)
                        )
                    )
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        val position = if (isUserSeeking) (seekPosition * duration).toLong() else currentPosition
                        Text(formatTime(position), color = Color(0xFFB8AFA8), style = MaterialTheme.typography.labelMedium)
                        Text(formatTime(duration), color = Color(0xFFB8AFA8), style = MaterialTheme.typography.labelMedium)
                    }
                }

                Row(
                    Modifier.fillMaxWidth().padding(top = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { viewModel.toggleShuffle() }) {
                        Icon(Icons.Default.Shuffle, contentDescription = "Shuffle", tint = if (shuffleMode) Color(0xFFFFAE42) else Color(0xFFB8AFA8))
                    }
                    IconButton(onClick = { viewModel.skipPrevious() }) {
                        Icon(Icons.Default.SkipPrevious, contentDescription = "Previous", modifier = Modifier.size(34.dp), tint = Color(0xFFF4EAE2))
                    }
                    FilledIconButton(
                        onClick = { viewModel.togglePlayPause() },
                        modifier = Modifier.size(76.dp),
                        shape = RoundedCornerShape(38.dp),
                        colors = IconButtonDefaults.filledIconButtonColors(containerColor = Color(0xFFFFAE42), contentColor = Color(0xFF1A0F08))
                    ) {
                        Icon(if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, contentDescription = if (isPlaying) "Pause" else "Play", modifier = Modifier.size(40.dp))
                    }
                    IconButton(onClick = { viewModel.skipNext() }) {
                        Icon(Icons.Default.SkipNext, contentDescription = "Next", modifier = Modifier.size(34.dp), tint = Color(0xFFF4EAE2))
                    }
                    IconButton(onClick = { viewModel.toggleRepeatMode() }) {
                        Icon(
                            if (repeatMode == 1) Icons.Default.RepeatOne else Icons.Default.Repeat,
                            contentDescription = "Repeat",
                            tint = if (repeatMode != 0) Color(0xFFFFAE42) else Color(0xFFB8AFA8)
                        )
                    }
                }
            }
        }
    }
}

private fun formatTime(millis: Long): String {
    val minutes = TimeUnit.MILLISECONDS.toMinutes(millis)
    val seconds = TimeUnit.MILLISECONDS.toSeconds(millis) - TimeUnit.MINUTES.toSeconds(minutes)
    return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
}
