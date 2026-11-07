package com.pulse.music.data.local.tageditor

import com.aman.taglib.TagLib
import com.pulse.music.domain.model.SongMetadata
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TagLibDataSource @Inject constructor() {

    suspend fun readMetadata(filePath: String): SongMetadata = withContext(Dispatchers.IO) {
        try {
            val metadata = TagLib.getMetadata(filePath)
            val artwork = TagLib.getArtwork(filePath)

            SongMetadata(
                title = metadata["TITLE"] ?: "",
                artist = metadata["ARTIST"] ?: "",
                album = metadata["ALBUM"] ?: "",
                year = metadata["DATE"] ?: metadata["YEAR"] ?: "",
                genre = metadata["GENRE"] ?: "",
                trackNumber = metadata["TRACKNUMBER"] ?: metadata["TRACK"] ?: "",
                lyrics = metadata["LYRICS"] ?: metadata["UNSYNCEDLYRICS"],
                albumArt = artwork
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

            val success = TagLib.setMetadata(filePath, tags)

            if (success && metadata.albumArt != null) {
                TagLib.setArtwork(filePath, metadata.albumArt)
            }

            success
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
