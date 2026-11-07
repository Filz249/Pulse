package com.pulse.music.ui.screens.tageditor

import android.app.Activity
import android.content.Context
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
                    errorMessage = "Greška kod učitavanja: ${e.message}"
                )
            }
        }
    }

    fun onMetadataChange(updated: SongMetadata) {
        _uiState.value = _uiState.value.copy(metadata = updated)
    }

    fun saveTags(activity: Activity?) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true)

            // Na Android 11+ (API 30+) za tuđe datoteke potrebno je zatražiti dozvolu
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && activity != null) {
                try {
                    val uri = Uri.parse(contentUri)
                    val pendingIntent = MediaStore.createWriteRequest(
                        context.contentResolver,
                        listOf(uri)
                    )
                    activity.startIntentSenderForResult(
                        pendingIntent.intentSender,
                        WRITE_REQUEST_CODE,
                        null, 0, 0, 0
                    )
                } catch (e: Exception) {
                    // Nastavi dalje ako je već dozvoljeno
                }
            }

            val success = updateSongTagsUseCase(filePath, _uiState.value.metadata)
            if (success) {
                refreshSongsUseCase()
            }
            _uiState.value = _uiState.value.copy(
                isSaving = false,
                saveSuccess = success,
                errorMessage = if (!success) "Neuspješno spremanje tagova." else null
            )
        }
    }

    companion object {
        const val WRITE_REQUEST_CODE = 4001
    }
}
