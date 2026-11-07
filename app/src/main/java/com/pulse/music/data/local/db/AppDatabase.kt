package com.pulse.music.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.pulse.music.data.local.db.dao.PlaylistDao
import com.pulse.music.data.local.db.dao.SongDao
import com.pulse.music.data.local.db.entity.PlaylistEntity
import com.pulse.music.data.local.db.entity.PlaylistSongCrossRef
import com.pulse.music.data.local.db.entity.SongEntity

@Database(
    entities = [
        SongEntity::class,
        PlaylistEntity::class,
        PlaylistSongCrossRef::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun songDao(): SongDao
    abstract fun playlistDao(): PlaylistDao
}
