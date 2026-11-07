package com.pulse.music.ui.screens.home

import android.Manifest
import android.os.Build
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.pulse.music.domain.model.Song
import com.pulse.music.ui.components.SongListItem

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onSongClick: (List<Song>, Int) -> Unit,
    onEditSongTags: (Song) -> Unit,
    modifier: Modifier = Modifier
) {
    val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_AUDIO
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }
    val permissionState = rememberPermissionState(permission = permission)

    val songs by viewModel.songs.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    var showSortMenu by remember { mutableStateOf(false) }

    LaunchedEffect(permissionState.status.isGranted) {
        if (permissionState.status.isGranted) {
            viewModel.refresh()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Pulse") },
                actions = {
                    IconButton(onClick = { viewModel.refresh() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                    IconButton(onClick = { showSortMenu = true }) {
                        Icon(Icons.Default.Sort, contentDescription = "Sort")
                    }
                    DropdownMenu(
                        expanded = showSortMenu,
                        onDismissRequest = { showSortMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("By Title") },
                            onClick = {
                                viewModel.onSortOrderChanged(SortOrder.TITLE)
                                showSortMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("By Artist") },
                            onClick = {
                                viewModel.onSortOrderChanged(SortOrder.ARTIST)
                                showSortMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("By Date Added") },
                            onClick = {
                                viewModel.onSortOrderChanged(SortOrder.DATE_ADDED)
                                showSortMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("By Duration") },
                            onClick = {
                                viewModel.onSortOrderChanged(SortOrder.DURATION)
                                showSortMenu = false
                            }
                        )
                    }
                }
            )
        },
        modifier = modifier
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (!permissionState.status.isGranted) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Text(
                            text = "Permission is required to scan and play local audio files.",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { permissionState.launchPermissionRequest() }) {
                            Text("Grant Permission")
                        }
                    }
                }
            } else {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.onSearchQueryChanged(it) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    placeholder = { Text("Search songs, artists, albums...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true
                )

                if (songs.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No songs found on this device.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        itemsIndexed(songs, key = { _, song -> song.id }) { index, song ->
                            SongListItem(
                                song = song,
                                onClick = { onSongClick(songs, index) },
                                onMoreClick = { onEditSongTags(song) }
                            )
                        }
                    }
                }
            }
        }
    }
}
