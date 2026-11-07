package com.pulse.music.ui.screens.tageditor

import android.content.Context
import android.content.IntentSender
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pulse.music.domain.model.SongMetadata
import com.pulse.music.domain.usecase.RefreshSongsUseCase
import com.pulse.music.domain.usecase.UpdateSongTagsUseCase
import com.pulse.music.data.local.tageditor.TagLibDataSource
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import javax.inject.Inject

data class TagEditorUiState(
    val isLoading: Boolean = true,
    val metadata: SongMetadata = SongMetadata("", "", "", "", "", ""),
    val isSaving: Boolean = false,
    val saveSuccess: Boolean? = null,
    val errorMessage: String? = null
)

@HiltViewModel
class TagEditorViewModel @Inject constructor(
    private val tagLibDataSource: TagLibDataSource,
    private val updateSongTagsUseCase: UpdateSongTagsUseCase,
    private val refreshSongsUseCase: RefreshSongsUseCase,
    @ApplicationContext private val context: Context,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val rawPath: String = checkNotNull(savedStateHandle["filePath"])
    private val rawUri: String = checkNotNull(savedStateHandle["contentUri"])

    val filePath: String = URLDecoder.decode(rawPath, StandardCharsets.UTF_8.toString())
    val contentUri: String = URLDecoder.decode(rawUri, StandardCharsets.UTF_8.toString())

    private val _uiState = MutableStateFlow(TagEditorUiState())
    val uiState: StateFlow<TagEditorUiState> = _uiState.asStateFlow()

    init {
        loadMetadata()
    }

    private fun loadMetadata() {
        viewModelScope.launch {
            try {
                val metadata = tagLibDataSource.readMetadata(filePath)
                _uiState.value = TagEditorUiState(isLoading = false, metadata = metadata)
            } catch (e: Exception) {
                _uiState.value = TagEditorUiState(
                    isLoading = false,
                    errorMessage = "Error reading metadata: ${e.message}"
                )
            }
        }
    }

    fun onMetadataChange(updated: SongMetadata) {
        _uiState.value = _uiState.value.copy(metadata = updated)
    }

    /**
     * Android 11+ requires the user to approve writes to media owned by another app.
     * The UI launches the returned IntentSender and calls onWritePermissionResult
     * only after Android reports the user's decision.
     */
    fun saveTags(onWritePermissionRequired: (IntentSender) -> Unit) {
        if (_uiState.value.isSaving) return
        _uiState.value = _uiState.value.copy(
            isSaving = true,
            saveSuccess = null,
            errorMessage = null
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                val request = MediaStore.createWriteRequest(
                    context.contentResolver,
                    listOf(Uri.parse(contentUri))
                )
                onWritePermissionRequired(request.intentSender)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    errorMessage = "Could not request permission to edit this audio file: ${e.message}"
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
                errorMessage = "Permission denied. The audio tags were not changed."
            )
        }
    }

    private fun persistTags() {
        viewModelScope.launch {
            val success = updateSongTagsUseCase(filePath, _uiState.value.metadata)
            if (success) refreshSongsUseCase()
            _uiState.value = _uiState.value.copy(
                isSaving = false,
                saveSuccess = success,
                errorMessage = if (!success) "Failed to write metadata tags to file." else null
            )
        }
    }
}
