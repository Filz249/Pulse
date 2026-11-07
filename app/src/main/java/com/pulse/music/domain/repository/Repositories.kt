package com.pulse.music.domain.repository

import com.pulse.music.domain.model.Album
import com.pulse.music.domain.model.Artist
import com.pulse.music.domain.model.Playlist
import com.pulse.music.domain.model.Song
import com.pulse.music.domain.model.SongMetadata
import kotlinx.coroutines.flow.Flow

interface SongRepository {
    fun getSongs(): Flow<List<Song>>
    suspend fun refreshSongs()
    fun getAlbums(): Flow<List<Album>>
    fun getArtists(): Flow<List<Artist>>
    suspend fun getSongById(id: Long): Song?
}

interface TagEditorRepository {
    suspend fun readMetadata(filePath: String): SongMetadata
    suspend fun updateMetadata(filePath: String, metadata: SongMetadata): Boolean
    suspend fun updateArtwork(filePaths: List<String>, artwork: ByteArray): List<String>
}

interface PlaylistRepository {
    fun getPlaylists(): Flow<List<Playlist>>
    suspend fun createPlaylist(name: String): Long
    suspend fun addSongToPlaylist(playlistId: Long, songId: Long)
    suspend fun getSongsForPlaylist(playlistId: Long): List<Song>
    suspend fun deletePlaylist(playlistId: Long)
}
