package com.pulse.music.ui.screens.home

import android.Manifest
import android.os.Build
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.pulse.music.domain.model.Playlist
import com.pulse.music.domain.model.Song

enum class HomeNavigationTab { HOME, SEARCH, PLAYLISTS, CREATE }
private enum class LibraryCategory(val label: String) { SONGS("Songs"), ALBUMS("Albums"), ARTISTS("Artists") }
private data class RecentGridEntry(val title: String, val subtitle: String, val song: Song?, val icon: ImageVector, val onClick: () -> Unit)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onSongClick: (List<Song>, Int) -> Unit,
    onEditSongTags: (Song) -> Unit,
    onAlbumClick: (Long) -> Unit,
    onOpenSettings: () -> Unit,
    selectedBottomTab: HomeNavigationTab,
    onBottomTabChange: (HomeNavigationTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) Manifest.permission.READ_MEDIA_AUDIO else Manifest.permission.READ_EXTERNAL_STORAGE
    val permissionState = rememberPermissionState(permission)
    val librarySongs by viewModel.librarySongs.collectAsState()
    val songs by viewModel.songs.collectAsState()
    val recent by viewModel.recentlyPlayed.collectAsState()
    val playlists by viewModel.playlists.collectAsState()
    val recentPlaylists by viewModel.recentlyPlayedPlaylists.collectAsState()
    val query by viewModel.searchQuery.collectAsState()

    var category by remember { mutableStateOf(LibraryCategory.SONGS) }
    var sortOpen by remember { mutableStateOf(false) }
    var createOpen by remember { mutableStateOf(false) }
    var songForNewPlaylist by remember { mutableStateOf<Song?>(null) }
    val albums = remember(librarySongs) { librarySongs.groupBy { it.albumId }.values.filter { it.isNotEmpty() }.sortedBy { it.first().album.lowercase() } }
    val artists = remember(librarySongs) { librarySongs.groupBy { it.artist }.values.filter { it.isNotEmpty() }.sortedBy { it.first().artist.lowercase() } }

    LaunchedEffect(permissionState.status.isGranted) { if (permissionState.status.isGranted) viewModel.refresh() }
    LaunchedEffect(selectedBottomTab) {
        if (selectedBottomTab != HomeNavigationTab.SEARCH) viewModel.onSearchQueryChanged("")
        if (selectedBottomTab == HomeNavigationTab.CREATE) {
            songForNewPlaylist = null
            createOpen = true
            onBottomTabChange(HomeNavigationTab.HOME)
        }
    }
    if (createOpen) {
        NewPlaylistDialog(
            initialSong = songForNewPlaylist,
            onDismiss = { createOpen = false; songForNewPlaylist = null },
            onCreate = { name, song -> viewModel.createPlaylist(name, song); createOpen = false; songForNewPlaylist = null }
        )
    }

    val historySongs = recent.ifEmpty { librarySongs }
    val recentArtistGroups = historySongs.distinctBy { it.artist }.take(2)
    val recentAlbumGroups = historySongs.distinctBy { it.albumId }.take(2)
    val historyPlaylists = recentPlaylists.ifEmpty { playlists }.take(2)
    val recentEntries = buildList {
        repeat(2) { index ->
            val song = historySongs.getOrNull(index)
            add(RecentGridEntry(song?.title ?: "No recent song", if (recent.isEmpty()) "Songs · Your library" else "Recently played song", song, Icons.Default.MusicNote) {
                song?.let { selected -> val position = librarySongs.indexOfFirst { it.id == selected.id }; if (position >= 0) onSongClick(librarySongs, position) }
            })
        }
        repeat(2) { index ->
            val song = recentArtistGroups.getOrNull(index)
            add(RecentGridEntry(song?.artist ?: "No recent artist", "Artist", song, Icons.Default.Person) {
                song?.let { selected -> val tracks = librarySongs.filter { it.artist == selected.artist }; if (tracks.isNotEmpty()) onSongClick(tracks, 0) }
            })
        }
        repeat(2) { index ->
            val song = recentAlbumGroups.getOrNull(index)
            add(RecentGridEntry(song?.album ?: "No recent album", "Album", song, Icons.Default.Album) { song?.let { onAlbumClick(it.albumId) } })
        }
        repeat(2) { index ->
            val playlist = historyPlaylists.getOrNull(index)
            add(RecentGridEntry(playlist?.name ?: if (index == 0) "Create a playlist" else "No recent playlist", "Playlist", null, Icons.Default.GraphicEq) {
                if (playlist != null) viewModel.playPlaylist(playlist.id) else { songForNewPlaylist = null; createOpen = true }
            })
        }
    }

    Scaffold(modifier = modifier, contentWindowInsets = WindowInsets(0, 0, 0, 0)) { padding ->
        if (!permissionState.status.isGranted) {
            Box(Modifier.fillMaxSize().padding(padding).padding(24.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    PulseMark(Modifier.size(76.dp)); Spacer(Modifier.height(20.dp))
                    Text("Your music starts here", style = MaterialTheme.typography.headlineSmall)
                    Spacer(Modifier.height(8.dp))
                    Text("Allow Pulse to scan audio stored on this device. Your library stays on your phone.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(20.dp)); Button(onClick = { permissionState.launchPermissionRequest() }) { Text("Allow audio access") }
                    TextButton(onClick = onOpenSettings) { Text("Settings") }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 12.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        PulseMark(Modifier.size(42.dp)); Spacer(Modifier.width(11.dp))
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text("PULSE", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                            Text("YOUR MUSIC. YOUR WAY.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, letterSpacing = 1.3.sp)
                        }
                        IconButton(onClick = onOpenSettings) { Icon(Icons.Default.Settings, contentDescription = "Settings", tint = MaterialTheme.colorScheme.onSurfaceVariant) }
                        IconButton(onClick = { viewModel.refresh() }) { Icon(Icons.Default.Refresh, contentDescription = "Refresh library", tint = MaterialTheme.colorScheme.onSurfaceVariant) }
                        Box {
                            IconButton(onClick = { sortOpen = true }) { Icon(Icons.Default.Sort, contentDescription = "Sort music", tint = MaterialTheme.colorScheme.onSurfaceVariant) }
                            DropdownMenu(expanded = sortOpen, onDismissRequest = { sortOpen = false }) {
                                SortOrder.values().forEach { option -> DropdownMenuItem(text = { Text(option.displayName()) }, onClick = { viewModel.onSortOrderChanged(option); sortOpen = false }) }
                            }
                        }
                    }
                }
                if (selectedBottomTab == HomeNavigationTab.HOME) {
                    item {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            LibraryCategory.values().forEach { option ->
                                FilterChip(selected = category == option, onClick = { category = option }, label = { Text(option.label) }, modifier = Modifier.weight(1f))
                            }
                        }
                    }
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(verticalAlignment = Alignment.Bottom) {
                                Text("Recently played", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                                Spacer(Modifier.weight(1f))
                                Text("Your listening", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            recentEntries.chunked(2).forEach { row ->
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    row.forEach { entry -> RecentGridCard(entry, Modifier.weight(1f)) }
                                }
                            }
                        }
                    }
                    when (category) {
                        LibraryCategory.SONGS -> {
                            item { SectionHeading("Songs", "Your local collection") }
                            if (songs.isEmpty()) item { EmptyHint("Songs appear here when audio is added to your library.") }
                            itemsIndexed(songs, key = { _, song -> song.id }) { index, song ->
                                LibrarySongRow(song, playlists, { onSongClick(songs, index) }, { onEditSongTags(song) }, { track, id -> viewModel.addSongToPlaylist(track, id) }, { songForNewPlaylist = song; createOpen = true })
                            }
                        }
                        LibraryCategory.ALBUMS -> {
                            item { SectionHeading("Albums", "Browse your albums") }
                            if (albums.isEmpty()) item { EmptyHint("Albums appear here when audio is added to your library.") }
                            items(albums) { group -> CompactLibraryRow(group.first(), group.first().album, "${group.size} tracks") { onAlbumClick(group.first().albumId) } }
                        }
                        LibraryCategory.ARTISTS -> {
                            item { SectionHeading("Artists", "Browse by artist") }
                            if (artists.isEmpty()) item { EmptyHint("Artists appear here when audio is added to your library.") }
                            items(artists) { group -> CompactLibraryRow(group.first(), group.first().artist, "${group.size} tracks · ${group.map { it.albumId }.distinct().size} albums", true) { onSongClick(group, 0) } }
                        }
                    }
                } else if (selectedBottomTab == HomeNavigationTab.SEARCH) {
                    item {
                        OutlinedTextField(
                            value = query, onValueChange = viewModel::onSearchQueryChanged, modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("Search songs, albums or artists") }, leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                            singleLine = true, shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary, unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant)
                        )
                    }
                    item { SectionHeading("Search results", "${songs.size} matches in your library") }
                    if (songs.isEmpty()) item { EmptyHint(if (query.isBlank()) "Search your local music library." else "No matches found. Try another search.") }
                    itemsIndexed(songs, key = { _, song -> song.id }) { index, song ->
                        LibrarySongRow(song, playlists, { onSongClick(songs, index) }, { onEditSongTags(song) }, { track, id -> viewModel.addSongToPlaylist(track, id) }, { songForNewPlaylist = song; createOpen = true })
                    }
                } else if (selectedBottomTab == HomeNavigationTab.PLAYLISTS) {
                    item { SectionHeading("Playlists", "Your personal collections") }
                    item { PlaylistSection(playlists, { viewModel.playPlaylist(it) }, { createOpen = true }, true) }
                }
            }
        }
    }
}

@Composable
private fun RecentGridCard(entry: RecentGridEntry, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.height(70.dp).clickable(onClick = entry.onClick),
        shape = RoundedCornerShape(13.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.78f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
    ) {
        Row(Modifier.fillMaxSize().padding(horizontal = 9.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            if (entry.song?.albumArtUri != null) {
                AsyncImage(entry.song.albumArtUri, contentDescription = null, modifier = Modifier.size(48.dp).clip(RoundedCornerShape(9.dp)), contentScale = ContentScale.Crop)
            } else {
                Box(Modifier.size(48.dp).clip(RoundedCornerShape(9.dp)).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.13f)), contentAlignment = Alignment.Center) {
                    Icon(entry.icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                }
            }
            Spacer(Modifier.width(9.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(entry.title, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(entry.subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}
private fun SortOrder.displayName() = when (this) {
    SortOrder.TITLE -> "Title"
    SortOrder.ARTIST -> "Artist"
    SortOrder.DATE_ADDED -> "Recently added"
    SortOrder.DURATION -> "Duration"
}

@Composable
private fun PulseMark(modifier: Modifier = Modifier) {
    Surface(modifier, shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f))) {
        Box(contentAlignment = Alignment.Center) {
            Icon(painter = painterResource(com.pulse.music.R.drawable.ic_pulse_equalizer),
                contentDescription = "Pulse", tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.fillMaxSize(0.66f))
        }
    }
}

@Composable
private fun SectionHeading(title: String, subtitle: String? = null, modifier: Modifier = Modifier) {
    Column(modifier.padding(bottom = 10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        subtitle?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
}

@Composable
private fun EmptyHint(message: String) {
    Surface(Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f), shape = RoundedCornerShape(14.dp)) {
        Text(message, Modifier.padding(16.dp), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun CoverCard(song: Song, onClick: () -> Unit) {
    Column(Modifier.width(132.dp).clickable(onClick = onClick), verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Artwork(song, Modifier.size(132.dp))
        Text(song.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(song.artist, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun SquareCard(song: Song, title: String, onClick: () -> Unit) {
    Column(Modifier.width(142.dp).clickable(onClick = onClick), verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Artwork(song, Modifier.size(142.dp))
        Text(title.ifBlank { "Unknown album" }, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(song.artist, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun CircleCard(song: Song, title: String, subtitle: String, onClick: () -> Unit) {
    Column(Modifier.width(122.dp).clickable(onClick = onClick), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Artwork(song, Modifier.size(112.dp).clip(CircleShape), 56.dp)
        Text(title.ifBlank { "Unknown artist" }, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun Artwork(song: Song, modifier: Modifier = Modifier, corner: androidx.compose.ui.unit.Dp = 12.dp) {
    Surface(modifier.clip(RoundedCornerShape(corner)), color = MaterialTheme.colorScheme.surfaceVariant) {
        if (song.albumArtUri != null) AsyncImage(song.albumArtUri, "${song.album} artwork", Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        else Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) {
            Icon(Icons.Default.MusicNote, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(34.dp))
        }
    }
}

@Composable
private fun CompactLibraryRow(song: Song, title: String, subtitle: String, circular: Boolean = false, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
        Artwork(song, Modifier.size(58.dp).then(if (circular) Modifier.clip(CircleShape) else Modifier), if (circular) 29.dp else 9.dp)
        Spacer(Modifier.width(13.dp))
        Column(Modifier.weight(1f)) {
            Text(title.ifBlank { "Unknown" }, fontWeight = FontWeight.Medium, maxLines = 1)
            Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun LibrarySongRow(
    song: Song, playlists: List<Playlist>, onPlay: () -> Unit, onEdit: () -> Unit,
    onAdd: (Song, Long) -> Unit, onCreatePlaylist: () -> Unit
) {
    var menuOpen by remember(song.id) { mutableStateOf(false) }
    Row(Modifier.fillMaxWidth().clickable(onClick = onPlay).padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Artwork(song, Modifier.size(54.dp), 9.dp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(song.title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("${song.artist} · ${song.album}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Box {
            IconButton(onClick = { menuOpen = true }) { Icon(Icons.Default.MoreVert, contentDescription = "Song options", tint = MaterialTheme.colorScheme.onSurfaceVariant) }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                DropdownMenuItem(text = { Text("Edit metadata") }, onClick = { menuOpen = false; onEdit() })
                playlists.forEach { playlist ->
                    DropdownMenuItem(text = { Text("Add to ${playlist.name}") }, onClick = { menuOpen = false; onAdd(song, playlist.id) })
                }
                DropdownMenuItem(text = { Text("Create playlist with this song") }, onClick = { menuOpen = false; onCreatePlaylist() })
            }
        }
    }
}

@Composable
private fun PlaylistSection(playlists: List<Playlist>, onPlay: (Long) -> Unit, onCreate: () -> Unit, asList: Boolean = false) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (playlists.isEmpty()) EmptyHint("Create a playlist to start collecting tracks.")
        else if (asList) playlists.forEach { PlaylistCard(it, onPlay, Modifier.fillMaxWidth()) }
        else Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            playlists.forEach { PlaylistCard(it, onPlay, Modifier.width(210.dp)) }
        }
        OutlinedButton(onClick = onCreate, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.Add, null); Spacer(Modifier.width(8.dp)); Text("Create playlist")
        }
    }
}

@Composable
private fun PlaylistCard(playlist: Playlist, onPlay: (Long) -> Unit, modifier: Modifier = Modifier) {
    Surface(modifier.clickable { onPlay(playlist.id) }, shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(52.dp).clip(RoundedCornerShape(10.dp)).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.MusicNote, null, tint = MaterialTheme.colorScheme.primary)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(playlist.name, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("Personal collection", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
            }
            Icon(Icons.Default.GraphicEq, "Play playlist", tint = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun NewPlaylistDialog(initialSong: Song?, onDismiss: () -> Unit, onCreate: (String, Song?) -> Unit) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New playlist") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(if (initialSong != null) "This track will be added to your new playlist." else "Give your collection a name.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
                OutlinedTextField(name, { name = it }, label = { Text("Playlist name") }, singleLine = true)
            }
        },
        confirmButton = { Button(onClick = { onCreate(name, initialSong) }, enabled = name.isNotBlank()) { Text("Create") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
