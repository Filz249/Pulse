package com.pulse.music.domain.model

import android.net.Uri

data class Song(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val albumId: Long,
    val duration: Long,
    val trackNumber: Int,
    val year: Int,
    val genre: String,
    val uri: Uri,
    val filePath: String,
    val dateAdded: Long,
    val albumArtUri: Uri?
)

data class SongMetadata(
    val title: String,
    val artist: String,
    val album: String,
    val year: String,
    val genre: String,
    val trackNumber: String,
    val lyrics: String? = null,
    val albumArt: ByteArray? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as SongMetadata
        return title == other.title &&
                artist == other.artist &&
                album == other.album &&
                year == other.year &&
                genre == other.genre &&
                trackNumber == other.trackNumber &&
                lyrics == other.lyrics &&
                albumArt.contentEquals(other.albumArt)
    }

    override fun hashCode(): Int {
        var result = title.hashCode()
        result = 31 * result + artist.hashCode()
        result = 31 * result + album.hashCode()
        result = 31 * result + year.hashCode()
        result = 31 * result + genre.hashCode()
        result = 31 * result + trackNumber.hashCode()
        result = 31 * result + (lyrics?.hashCode() ?: 0)
        result = 31 * result + (albumArt?.contentHashCode() ?: 0)
        return result
    }
}

data class Album(
    val id: Long,
    val title: String,
    val artist: String,
    val songCount: Int,
    val albumArtUri: Uri?
)

data class Artist(
    val id: Long,
    val name: String,
    val trackCount: Int,
    val albumCount: Int
)

data class Playlist(
    val id: Long,
    val name: String,
    val songCount: Int,
    val createdAt: Long
)
