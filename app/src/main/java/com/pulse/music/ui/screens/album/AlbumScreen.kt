package com.pulse.music.ui.screens.album

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.pulse.music.domain.model.Song
import com.pulse.music.ui.screens.home.HomeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlbumScreen(viewModel: HomeViewModel, albumId: Long, onBackClick: () -> Unit, onPlaySongs: (List<Song>, Int) -> Unit, onEditSongTags: (Song) -> Unit) {
    val allSongs by viewModel.librarySongs.collectAsState()
    val tracks = remember(allSongs, albumId) { allSongs.filter { it.albumId == albumId }.sortedWith(compareBy<Song> { it.trackNumber.takeIf { n -> n > 0 } ?: Int.MAX_VALUE }.thenBy { it.title.lowercase() }) }
    val first = tracks.firstOrNull()
    Scaffold(topBar = { TopAppBar(title = { Text("Album") }, navigationIcon = { IconButton(onClick = onBackClick) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") } }) }, contentWindowInsets = WindowInsets(0, 0, 0, 0)) { padding ->
        if (first == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { Text("This album is no longer available in your library.") }
        } else {
            LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 32.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        AsyncImage(model = first.albumArtUri, contentDescription = "Album artwork", modifier = Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(18.dp)), contentScale = ContentScale.Crop)
                        Text(first.album.ifBlank { "Unknown album" }, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        Text(first.artist, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("${tracks.size} songs", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Button(onClick = { onPlaySongs(tracks, 0) }, modifier = Modifier.weight(1f)) { Icon(Icons.Default.PlayArrow, contentDescription = null); Spacer(Modifier.width(6.dp)); Text("Play album") }
                            OutlinedButton(onClick = { onEditSongTags(first) }, modifier = Modifier.weight(1f)) { Icon(Icons.Default.Edit, contentDescription = null); Spacer(Modifier.width(6.dp)); Text("Edit album cover") }
                        }
                        Text("Tracks", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                    }
                }
                itemsIndexed(tracks, key = { _, song -> song.id }) { index, song ->
                    var menuOpen by remember(song.id) { mutableStateOf(false) }
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(if (song.trackNumber > 0) song.trackNumber.toString() else (index + 1).toString(), modifier = Modifier.width(30.dp), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                        Column(Modifier.weight(1f).clickable { onPlaySongs(tracks, index) }.padding(vertical = 10.dp)) {
                            Text(song.title, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(song.artist, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        Box {
                            IconButton(onClick = { menuOpen = true }) { Icon(Icons.Default.MoreVert, contentDescription = "Song options") }
                            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                                DropdownMenuItem(text = { Text("Edit song information") }, leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) }, onClick = { menuOpen = false; onEditSongTags(song) })
                                DropdownMenuItem(text = { Text("Play song") }, leadingIcon = { Icon(Icons.Default.PlayArrow, contentDescription = null) }, onClick = { menuOpen = false; onPlaySongs(tracks, index) })
                            }
                        }
                    }
                    if (index != tracks.lastIndex) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
                }
            }
        }
    }
}