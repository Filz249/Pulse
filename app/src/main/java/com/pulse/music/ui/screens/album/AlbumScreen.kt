package com.pulse.music.ui.screens.album

import android.graphics.BitmapFactory
import android.graphics.Color as AndroidColor
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.pulse.music.domain.model.Song
import com.pulse.music.ui.screens.home.HomeViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlbumScreen(viewModel: HomeViewModel, albumId: Long, onBackClick: () -> Unit, onPlaySongs: (List<Song>, Int) -> Unit, onEditSongTags: (Song) -> Unit) {
    val allSongs by viewModel.librarySongs.collectAsState()
    val tracks = remember(allSongs, albumId) { allSongs.filter { it.albumId == albumId }.sortedWith(compareBy<Song> { it.trackNumber.takeIf { n -> n > 0 } ?: Int.MAX_VALUE }.thenBy { it.title.lowercase() }) }
    val first = tracks.firstOrNull()
    val artworkAccent = rememberArtworkAccent(first?.albumArtUri)
    Scaffold(
        topBar = { TopAppBar(title = { Text("Album") }, navigationIcon = { IconButton(onClick = onBackClick) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") } }) },
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { padding ->
        if (first == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { Text("This album is no longer available in your library.") }
        } else {
            LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 12.dp, bottom = 32.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        color = artworkAccent.copy(alpha = 0.13f),
                        border = BorderStroke(1.dp, artworkAccent.copy(alpha = 0.38f))
                    ) {
                        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                            if (first.albumArtUri != null) {
                                AsyncImage(model = first.albumArtUri, contentDescription = "Album artwork", modifier = Modifier.size(112.dp).clip(RoundedCornerShape(14.dp)), contentScale = ContentScale.Crop)
                            } else {
                                Box(Modifier.size(112.dp).clip(RoundedCornerShape(14.dp)), contentAlignment = Alignment.Center) { Icon(Icons.Default.GraphicEq, contentDescription = null, tint = artworkAccent, modifier = Modifier.size(42.dp)) }
                            }
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                Text(first.album.ifBlank { "Unknown album" }, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                Text(first.artist, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                Text("${tracks.size} songs", style = MaterialTheme.typography.labelMedium, color = artworkAccent)
                            }
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Button(onClick = { onPlaySongs(tracks, 0) }, modifier = Modifier.weight(1f)) { Icon(Icons.Default.PlayArrow, contentDescription = null); Spacer(Modifier.width(6.dp)); Text("Play album") }
                        OutlinedButton(onClick = { onEditSongTags(first) }, modifier = Modifier.weight(1f)) { Icon(Icons.Default.Edit, contentDescription = null); Spacer(Modifier.width(6.dp)); Text("Edit album") }
                    }
                    Text("Tracks", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
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

@Composable
private fun rememberArtworkAccent(uri: Uri?): Color {
    val context = LocalContext.current
    val fallback = MaterialTheme.colorScheme.primary
    val accent by produceState(initialValue = fallback, key1 = uri) {
        if (uri != null) {
            val sampled = withContext(Dispatchers.IO) {
                runCatching {
                    context.contentResolver.openInputStream(uri)?.use { stream ->
                        val options = BitmapFactory.Options().apply { inSampleSize = 16 }
                        BitmapFactory.decodeStream(stream, null, options)?.let { bitmap ->
                            var red = 0L; var green = 0L; var blue = 0L; var count = 0L
                            val stepX = (bitmap.width / 12).coerceAtLeast(1)
                            val stepY = (bitmap.height / 12).coerceAtLeast(1)
                            for (y in 0 until bitmap.height step stepY) {
                                for (x in 0 until bitmap.width step stepX) {
                                    val pixel = bitmap.getPixel(x, y)
                                    red += AndroidColor.red(pixel); green += AndroidColor.green(pixel); blue += AndroidColor.blue(pixel); count++
                                }
                            }
                            bitmap.recycle()
                            if (count > 0) Color(red.toFloat() / count / 255f, green.toFloat() / count / 255f, blue.toFloat() / count / 255f) else null
                        }
                    }
                }.getOrNull()
            }
            if (sampled != null) value = sampled
        }
    }
    return accent
}