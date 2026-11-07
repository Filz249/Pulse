package com.pulse.music.data.repository

import android.net.Uri
import com.pulse.music.data.local.db.dao.PlaylistDao
import com.pulse.music.data.local.db.dao.SongDao
import com.pulse.music.data.local.db.entity.PlaylistEntity
import com.pulse.music.data.local.db.entity.PlaylistSongCrossRef
import com.pulse.music.data.local.db.entity.SongEntity
import com.pulse.music.data.local.mediastore.MediaStoreDataSource
import com.pulse.music.data.local.tageditor.TagLibDataSource
import com.pulse.music.domain.model.Album
import com.pulse.music.domain.model.Artist
import com.pulse.music.domain.model.Playlist
import com.pulse.music.domain.model.Song
import com.pulse.music.domain.model.SongMetadata
import com.pulse.music.domain.repository.PlaylistRepository
import com.pulse.music.domain.repository.SongRepository
import com.pulse.music.domain.repository.TagEditorRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SongRepositoryImpl @Inject constructor(
    private val mediaStoreDataSource: MediaStoreDataSource,
    private val songDao: SongDao
) : SongRepository {

    override fun getSongs(): Flow<List<Song>> {
        return songDao.getAllSongs().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun refreshSongs() {
        val songs = mediaStoreDataSource.fetchAudioFiles()
        songDao.clearSongs()
        songDao.insertSongs(songs.map { it.toEntity() })
    }

    override fun getAlbums(): Flow<List<Album>> {
        return getSongs().map { songs ->
            songs.groupBy { it.albumId }
                .map { (_, albumSongs) ->
                    val first = albumSongs.first()
                    Album(
                        id = first.albumId,
                        title = first.album,
                        artist = first.artist,
                        songCount = albumSongs.size,
                        albumArtUri = first.albumArtUri
                    )
                }
        }
    }

    override fun getArtists(): Flow<List<Artist>> {
        return getSongs().map { songs ->
            songs.groupBy { it.artist }
                .entries
                .mapIndexed { index, entry ->
                    val artistName = entry.key
                    val artistSongs = entry.value
                    Artist(
                        id = index.toLong(),
                        name = artistName,
                        trackCount = artistSongs.size,
                        albumCount = artistSongs.map { it.albumId }.distinct().size
                    )
                }
        }
    }

    override suspend fun getSongById(id: Long): Song? {
        return songDao.getSongById(id)?.toDomain()
    }

    private fun Song.toEntity() = SongEntity(
        id = id,
        title = title,
        artist = artist,
        album = album,
        albumId = albumId,
        duration = duration,
        trackNumber = trackNumber,
        year = year,
        genre = genre,
        uriString = uri.toString(),
        filePath = filePath,
        dateAdded = dateAdded,
        albumArtUriString = albumArtUri?.toString()
    )

    private fun SongEntity.toDomain() = Song(
        id = id,
        title = title,
        artist = artist,
        album = album,
        albumId = albumId,
        duration = duration,
        trackNumber = trackNumber,
        year = year,
        genre = genre,
        uri = Uri.parse(uriString),
        filePath = filePath,
        dateAdded = dateAdded,
        albumArtUri = albumArtUriString?.let { Uri.parse(it) }
    )
}

@Singleton
class TagEditorRepositoryImpl @Inject constructor(
    private val tagLibDataSource: TagLibDataSource
) : TagEditorRepository {
    override suspend fun readMetadata(filePath: String): SongMetadata {
        return tagLibDataSource.readMetadata(filePath)
    }

    override suspend fun updateMetadata(filePath: String, metadata: SongMetadata): Boolean {
        return tagLibDataSource.writeMetadata(filePath, metadata)
    }
}

@Singleton
class PlaylistRepositoryImpl @Inject constructor(
    private val playlistDao: PlaylistDao
) : PlaylistRepository {
    override fun getPlaylists(): Flow<List<Playlist>> {
        return playlistDao.getAllPlaylists().map { entities ->
            entities.map {
                Playlist(
                    id = it.id,
                    name = it.name,
                    songCount = 0,
                    createdAt = it.createdAt
                )
            }
        }
    }

    override suspend fun createPlaylist(name: String): Long {
        return playlistDao.insertPlaylist(PlaylistEntity(name = name))
    }

    override suspend fun addSongToPlaylist(playlistId: Long, songId: Long) {
        playlistDao.insertSongToPlaylist(PlaylistSongCrossRef(playlistId, songId))
    }

    override suspend fun getSongsForPlaylist(playlistId: Long): List<Song> {
        return playlistDao.getSongsForPlaylist(playlistId).map {
            Song(
                id = it.id,
                title = it.title,
                artist = it.artist,
                album = it.album,
                albumId = it.albumId,
                duration = it.duration,
                trackNumber = it.trackNumber,
                year = it.year,
                genre = it.genre,
                uri = Uri.parse(it.uriString),
                filePath = it.filePath,
                dateAdded = it.dateAdded,
                albumArtUri = it.albumArtUriString?.let { uriStr -> Uri.parse(uriStr) }
            )
        }
    }

    override suspend fun deletePlaylist(playlistId: Long) {
        playlistDao.deletePlaylist(PlaylistEntity(id = playlistId, name = ""))
    }
}
