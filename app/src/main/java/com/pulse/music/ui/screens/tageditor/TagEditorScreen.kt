package com.pulse.music.ui.screens.tageditor

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TagEditorScreen(
    viewModel: TagEditorViewModel,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()

    val writePermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        viewModel.onWritePermissionResult(result.resultCode == Activity.RESULT_OK)
    }

    val artworkPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let(viewModel::onAlbumArtworkSelected)
    }

    val saveTags: () -> Unit = {
        viewModel.saveTags { intentSender ->
            writePermissionLauncher.launch(
                IntentSenderRequest.Builder(intentSender).build()
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit Metadata") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = saveTags, enabled = !state.isSaving) {
                        Icon(Icons.Default.Save, contentDescription = "Save")
                    }
                }
            )
        },
        modifier = modifier
    ) { padding ->
        if (state.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp)
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (state.saveSuccess == true) {
                    Text(
                        "Changes successfully saved!",
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.titleMedium
                    )
                }

                if (state.errorMessage != null) {
                    Text(
                        state.errorMessage ?: "",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                Text("Album artwork", style = MaterialTheme.typography.titleMedium)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (state.metadata.albumArt != null) {
                        AsyncImage(
                            model = state.metadata.albumArt,
                            contentDescription = "Current album artwork",
                            modifier = Modifier.size(112.dp).clip(RoundedCornerShape(14.dp)),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Surface(
                            modifier = Modifier.size(112.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.Image,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(42.dp)
                                )
                            }
                        }
                    }

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            "The selected cover is applied to all ${state.albumTrackCount} track(s) in this album.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        OutlinedButton(
                            onClick = { artworkPicker.launch("image/*") },
                            enabled = !state.isSaving
                        ) {
                            Icon(Icons.Default.AddPhotoAlternate, contentDescription = null)
                            Spacer(Modifier.width(7.dp))
                            Text(if (state.metadata.albumArt == null) "Choose cover" else "Change cover")
                        }
                    }
                }

                OutlinedTextField(
                    value = state.metadata.title,
                    onValueChange = { viewModel.onMetadataChange(state.metadata.copy(title = it)) },
                    label = { Text("Title") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = state.metadata.artist,
                    onValueChange = { viewModel.onMetadataChange(state.metadata.copy(artist = it)) },
                    label = { Text("Artist") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = state.metadata.album,
                    onValueChange = { viewModel.onMetadataChange(state.metadata.copy(album = it)) },
                    label = { Text("Album") },
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = state.metadata.year,
                        onValueChange = { viewModel.onMetadataChange(state.metadata.copy(year = it)) },
                        label = { Text("Year") },
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedTextField(
                        value = state.metadata.trackNumber,
                        onValueChange = { viewModel.onMetadataChange(state.metadata.copy(trackNumber = it)) },
                        label = { Text("Track Number") },
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = state.metadata.genre,
                    onValueChange = { viewModel.onMetadataChange(state.metadata.copy(genre = it)) },
                    label = { Text("Genre") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = state.metadata.lyrics ?: "",
                    onValueChange = { viewModel.onMetadataChange(state.metadata.copy(lyrics = it)) },
                    label = { Text("Lyrics") },
                    modifier = Modifier.fillMaxWidth().height(160.dp),
                    maxLines = 8
                )

                Button(
                    onClick = saveTags,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !state.isSaving
                ) {
                    if (state.isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text("Save Changes")
                    }
                }
            }
        }
    }
}
