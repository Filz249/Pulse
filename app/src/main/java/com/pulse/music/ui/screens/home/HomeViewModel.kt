package com.pulse.music.ui.screens.home

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pulse.music.domain.model.Playlist
import com.pulse.music.domain.model.Song
import com.pulse.music.domain.repository.PlaylistRepository
import com.pulse.music.domain.usecase.GetSongsUseCase
import com.pulse.music.domain.usecase.RefreshSongsUseCase
import com.pulse.music.player.PlayerConnection
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class SortOrder { TITLE, ARTIST, DATE_ADDED, DURATION }

@HiltViewModel
class HomeViewModel @Inject constructor(
    getSongsUseCase: GetSongsUseCase,
    private val refreshSongsUseCase: RefreshSongsUseCase,
    private val playlistRepository: PlaylistRepository,
    val playerConnection: PlayerConnection,
    @ApplicationContext context: Context
) : ViewModel() {
    private val preferences = context.getSharedPreferences("pulse_library", Context.MODE_PRIVATE)
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()
    private val _sortOrder = MutableStateFlow(SortOrder.TITLE)
    val sortOrder: StateFlow<SortOrder> = _sortOrder.asStateFlow()

    val librarySongs: StateFlow<List<Song>> = getSongsUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val songs: StateFlow<List<Song>> = combine(librarySongs, _searchQuery, _sortOrder) { all, query, sort ->
        val matches = if (query.isBlank()) all else all.filter {
            it.title.contains(query, true) || it.artist.contains(query, true) ||
                it.album.contains(query, true) || it.genre.contains(query, true)
        }
        when (sort) {
            SortOrder.TITLE -> matches.sortedBy { it.title.lowercase() }
            SortOrder.ARTIST -> matches.sortedBy { it.artist.lowercase() }
            SortOrder.DATE_ADDED -> matches.sortedByDescending { it.dateAdded }
            SortOrder.DURATION -> matches.sortedByDescending { it.duration }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val playlists: StateFlow<List<Playlist>> = playlistRepository.getPlaylists()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _recentIds = MutableStateFlow(
        preferences.getString("recently_played_ids", "").orEmpty().split(",").mapNotNull { it.toLongOrNull() }
    )

    private val _playCounts = MutableStateFlow(
        preferences.getString("play_counts", "").orEmpty().split(",").mapNotNull { token ->
            token.split(":").takeIf { it.size == 2 }?.let { parts -> parts[0].toLongOrNull()?.let { id -> id to (parts[1].toIntOrNull() ?: 0) } }
        }.toMap()
    )
    val playCounts: StateFlow<Map<Long, Int>> = _playCounts.asStateFlow()

    val recentlyPlayed: StateFlow<List<Song>> = combine(librarySongs, _recentIds) { all, ids ->
        ids.mapNotNull { id -> all.find { it.id == id } }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _recentPlaylistIds = MutableStateFlow(
        preferences.getString("recently_played_playlist_ids", "").orEmpty().split(",").mapNotNull { it.toLongOrNull() }
    )
    val recentlyPlayedPlaylists: StateFlow<List<Playlist>> = combine(playlists, _recentPlaylistIds) { all, ids ->
        ids.mapNotNull { id -> all.find { it.id == id } }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        viewModelScope.launch {
            playerConnection.currentSong.collect { song ->
                if (song != null) {
                    val ids = (listOf(song.id) + _recentIds.value.filterNot { it == song.id }).take(12)
                    _recentIds.value = ids
                    val counts = _playCounts.value.toMutableMap().apply { put(song.id, (get(song.id) ?: 0) + 1) }
                    _playCounts.value = counts
                    preferences.edit()
                        .putString("recently_played_ids", ids.joinToString(","))
                        .putString("play_counts", counts.entries.joinToString(",") { "${it.key}:${it.value}" })
                        .apply()
                }
            }
        }
    }

    fun refresh() { viewModelScope.launch { refreshSongsUseCase() } }
    fun onSearchQueryChanged(query: String) { _searchQuery.value = query }
    fun onSortOrderChanged(sort: SortOrder) { _sortOrder.value = sort }
    fun playSong(songs: List<Song>, index: Int) { playerConnection.playSongs(songs, index) }

    fun createPlaylist(name: String, initialSong: Song? = null) {
        val clean = name.trim()
        if (clean.isEmpty()) return
        viewModelScope.launch {
            val id = playlistRepository.createPlaylist(clean)
            initialSong?.let { playlistRepository.addSongToPlaylist(id, it.id) }
        }
    }
    fun addSongToPlaylist(song: Song, playlistId: Long) {
        viewModelScope.launch { playlistRepository.addSongToPlaylist(playlistId, song.id) }
    }
    fun playPlaylist(playlistId: Long) {
        val ids = (listOf(playlistId) + _recentPlaylistIds.value.filterNot { it == playlistId }).take(8)
        _recentPlaylistIds.value = ids
        preferences.edit().putString("recently_played_playlist_ids", ids.joinToString(",")).apply()
        viewModelScope.launch {
            val tracks = playlistRepository.getSongsForPlaylist(playlistId)
            if (tracks.isNotEmpty()) playerConnection.playSongs(tracks)
        }
    }
}
