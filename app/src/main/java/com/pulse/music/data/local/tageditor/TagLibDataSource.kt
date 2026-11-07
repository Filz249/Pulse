package com.pulse.music.data.local.tageditor

import aman.taglib.TagLib
import com.pulse.music.domain.model.SongMetadata
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Reads and writes ID3/Vorbis/APE tags in audio files via native TagLib.
 * Supports MP3, FLAC, M4A, WAV, and OGG formats.
 */
@Singleton
class TagLibDataSource @Inject constructor() {

    suspend fun readMetadata(filePath: String): SongMetadata = withContext(Dispatchers.IO) {
        try {
            val metadata = TagLib.getMetadata(filePath) ?: emptyMap()
            val artworks = TagLib.getArtwork(filePath)
            val artworkBytes = artworks?.firstOrNull()?.data

            SongMetadata(
                title = metadata["TITLE"] ?: "",
                artist = metadata["ARTIST"] ?: "",
                album = metadata["ALBUM"] ?: "",
                year = metadata["DATE"] ?: metadata["YEAR"] ?: "",
                genre = metadata["GENRE"] ?: "",
                trackNumber = metadata["TRACKNUMBER"] ?: metadata["TRACK"] ?: "",
                lyrics = metadata["LYRICS"] ?: metadata["UNSYNCEDLYRICS"],
                albumArt = artworkBytes
            )
        } catch (e: Exception) {
            e.printStackTrace()
            SongMetadata("", "", "", "", "", "")
        }
    }

    suspend fun writeMetadata(filePath: String, metadata: SongMetadata): Boolean = withContext(Dispatchers.IO) {
        try {
            val tags = hashMapOf(
                "TITLE" to metadata.title,
                "ARTIST" to metadata.artist,
                "ALBUM" to metadata.album,
                "DATE" to metadata.year,
                "GENRE" to metadata.genre,
                "TRACKNUMBER" to metadata.trackNumber
            )

            metadata.lyrics?.let {
                tags["LYRICS"] = it
            }

            TagLib.setMetadata(filePath, tags)
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun writeArtwork(filePath: String, artwork: ByteArray): Boolean = withContext(Dispatchers.IO) {
        try {
            TagLib.setArtwork(filePath, artwork, "image/jpeg", "Cover")
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
