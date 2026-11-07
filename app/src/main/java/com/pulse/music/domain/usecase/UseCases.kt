package com.pulse.music.domain.usecase

import com.pulse.music.domain.model.Song
import com.pulse.music.domain.model.SongMetadata
import com.pulse.music.domain.repository.SongRepository
import com.pulse.music.domain.repository.TagEditorRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetSongsUseCase @Inject constructor(
    private val repository: SongRepository
) {
    operator fun invoke(): Flow<List<Song>> = repository.getSongs()
}

class RefreshSongsUseCase @Inject constructor(
    private val repository: SongRepository
) {
    suspend operator fun invoke() = repository.refreshSongs()
}

class UpdateSongTagsUseCase @Inject constructor(
    private val repository: TagEditorRepository
) {
    suspend operator fun invoke(filePath: String, metadata: SongMetadata): Boolean {
        return repository.updateMetadata(filePath, metadata)
    }
}
