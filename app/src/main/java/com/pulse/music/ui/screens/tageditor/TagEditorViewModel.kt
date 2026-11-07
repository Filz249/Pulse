package com.pulse.music.ui.screens.tageditor

import android.content.Context
import android.content.IntentSender
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pulse.music.data.local.tageditor.TagLibDataSource
import com.pulse.music.domain.model.Song
import com.pulse.music.domain.model.SongMetadata
import com.pulse.music.domain.usecase.GetSongsUseCase
import com.pulse.music.domain.usecase.RefreshSongsUseCase
import com.pulse.music.domain.usecase.UpdateAlbumArtworkUseCase
import com.pulse.music.domain.usecase.UpdateSongTagsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.ByteArrayOutputStream
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import javax.inject.Inject

data class TagEditorUiState(
    val isLoading: Boolean = true,
    val metadata: SongMetadata = SongMetadata("", "", "", "", "", ""),
    val isSaving: Boolean = false,
    val saveSuccess: Boolean? = null,
    val errorMessage: String? = null,
    val albumArtworkChanged: Boolean = false,
    val albumTrackCount: Int = 1
)

@HiltViewModel
class TagEditorViewModel @Inject constructor(
    private val tagLibDataSource: TagLibDataSource,
    private val getSongsUseCase: GetSongsUseCase,
    private val updateSongTagsUseCase: UpdateSongTagsUseCase,
    private val updateAlbumArtworkUseCase: UpdateAlbumArtworkUseCase,
    private val refreshSongsUseCase: RefreshSongsUseCase,
    @ApplicationContext private val context: Context,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val rawPath: String = checkNotNull(savedStateHandle["filePath"])
    private val rawUri: String = checkNotNull(savedStateHandle["contentUri"])
    private val albumId: Long = savedStateHandle["albumId"] ?: -1L

    val filePath: String = URLDecoder.decode(rawPath, StandardCharsets.UTF_8.toString())
    val contentUri: String = URLDecoder.decode(rawUri, StandardCharsets.UTF_8.toString())

    private var albumSongs: List<Song> = emptyList()

    private val _uiState = MutableStateFlow(TagEditorUiState())
    val uiState: StateFlow<TagEditorUiState> = _uiState.asStateFlow()

    init {
        loadMetadata()
    }

    private fun loadMetadata() {
        viewModelScope.launch {
            try {
                val allSongs = getSongsUseCase().first()
                albumSongs = allSongs.filter { song ->
                    albumId >= 0L && song.albumId == albumId && song.filePath.isNotBlank()
                }
                if (albumSongs.none { it.filePath == filePath }) {
                    allSongs.firstOrNull { it.filePath == filePath }?.let { albumSongs = albumSongs + it }
                }

                val metadata = tagLibDataSource.readMetadata(filePath)
                _uiState.value = TagEditorUiState(
                    isLoading = false,
                    metadata = metadata,
                    albumTrackCount = albumSongs.size.coerceAtLeast(1)
                )
            } catch (e: Exception) {
                _uiState.value = TagEditorUiState(
                    isLoading = false,
                    errorMessage = "Error reading metadata: ${e.message}"
                )
            }
        }
    }

    fun onMetadataChange(updated: SongMetadata) {
        _uiState.value = _uiState.value.copy(
            metadata = updated,
            saveSuccess = null,
            errorMessage = null
        )
    }

    fun onAlbumArtworkSelected(uri: Uri) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true, errorMessage = null)
            try {
                val jpegBytes = withContext(Dispatchers.IO) { readAndNormalizeArtwork(uri) }
                _uiState.value = _uiState.value.copy(
                    metadata = _uiState.value.metadata.copy(albumArt = jpegBytes),
                    albumArtworkChanged = true,
                    isSaving = false,
                    saveSuccess = null,
                    errorMessage = null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    errorMessage = "Could not load this cover image: ${e.message}"
                )
            }
        }
    }

    private fun readAndNormalizeArtwork(uri: Uri): ByteArray {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, bounds)
        } ?: throw IllegalArgumentException("The selected image could not be opened.")

        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
            throw IllegalArgumentException("The selected file is not a supported image.")
        }

        var sampleSize = 1
        while (maxOf(bounds.outWidth / sampleSize, bounds.outHeight / sampleSize) > 1600) {
            sampleSize *= 2
        }

        val options = BitmapFactory.Options().apply { inSampleSize = sampleSize }
        val bitmap = context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, options)
        } ?: throw IllegalArgumentException("The selected image could not be decoded.")

        val output = ByteArrayOutputStream()
        val compressed = bitmap.compress(Bitmap.CompressFormat.JPEG, 92, output)
        bitmap.recycle()
        if (!compressed) throw IllegalStateException("Image conversion failed.")
        return output.toByteArray()
    }

    /**
     * Android 11+ requires approval for every media item that will be modified.
     * When the cover changes, request approval for all tracks in the current album.
     */
    fun saveTags(onWritePermissionRequired: (IntentSender) -> Unit) {
        val current = _uiState.value
        if (current.isSaving) return
        _uiState.value = current.copy(isSaving = true, saveSuccess = null, errorMessage = null)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                val targetUris = if (current.albumArtworkChanged) {
                    (albumSongs.map { it.uri } + Uri.parse(contentUri)).distinct()
                } else {
                    listOf(Uri.parse(contentUri))
                }
                val request = MediaStore.createWriteRequest(context.contentResolver, targetUris)
                onWritePermissionRequired(request.intentSender)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    errorMessage = "Could not request permission to edit these audio files: ${e.message}"
                )
            }
        } else {
            persistTags()
        }
    }

    fun onWritePermissionResult(granted: Boolean) {
        if (granted) {
            persistTags()
        } else {
            _uiState.value = _uiState.value.copy(
                isSaving = false,
                saveSuccess = false,
                errorMessage = "Permission denied. The audio tags and album artwork were not changed."
            )
        }
    }

    private fun persistTags() {
        viewModelScope.launch {
            val current = _uiState.value
            val tagsSaved = updateSongTagsUseCase(
                filePath,
                current.metadata.copy(albumArt = null)
            )

            if (!tagsSaved) {
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    saveSuccess = false,
                    errorMessage = "Failed to write metadata tags to the selected file."
                )
                return@launch
            }

            var failedArtworkPaths: List<String> = emptyList()
            if (current.albumArtworkChanged && current.metadata.albumArt != null) {
                val paths = (albumSongs.map { it.filePath } + filePath)
                    .filter { it.isNotBlank() }
                    .distinct()
                failedArtworkPaths = updateAlbumArtworkUseCase(paths, current.metadata.albumArt)
                rescanAlbumFiles(paths)
            }

            refreshSongsUseCase()
            val artworkSucceeded = failedArtworkPaths.isEmpty()
            _uiState.value = _uiState.value.copy(
                isSaving = false,
                saveSuccess = artworkSucceeded,
                albumArtworkChanged = !artworkSucceeded && current.albumArtworkChanged,
                errorMessage = when {
                    artworkSucceeded -> null
                    failedArtworkPaths.size == 1 ->
                        "Metadata was saved, but the album cover could not be updated for one track."
                    else ->
                        "Metadata was saved, but the album cover could not be updated for ${failedArtworkPaths.size} tracks."
                }
            )
        }
    }

    private suspend fun rescanAlbumFiles(paths: List<String>) = withContext(Dispatchers.IO) {
        if (paths.isEmpty()) return@withContext
        val latch = CountDownLatch(paths.size)
        MediaScannerConnection.scanFile(context, paths.toTypedArray(), null) { _, _ ->
            latch.countDown()
        }
        withTimeoutOrNull(10_000L) {
            latch.await(10, TimeUnit.SECONDS)
        }
    }
}
