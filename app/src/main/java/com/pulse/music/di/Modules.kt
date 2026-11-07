package com.pulse.music.di

import android.content.Context
import androidx.room.Room
import com.pulse.music.data.local.db.AppDatabase
import com.pulse.music.data.local.db.dao.PlaylistDao
import com.pulse.music.data.local.db.dao.SongDao
import com.pulse.music.data.repository.PlaylistRepositoryImpl
import com.pulse.music.data.repository.SongRepositoryImpl
import com.pulse.music.data.repository.TagEditorRepositoryImpl
import com.pulse.music.domain.repository.PlaylistRepository
import com.pulse.music.domain.repository.SongRepository
import com.pulse.music.domain.repository.TagEditorRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "pulse_music.db"
        ).fallbackToDestructiveMigration().build()
    }

    @Provides
    fun provideSongDao(database: AppDatabase): SongDao = database.songDao()

    @Provides
    fun providePlaylistDao(database: AppDatabase): PlaylistDao = database.playlistDao()
}

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindSongRepository(impl: SongRepositoryImpl): SongRepository

    @Binds
    @Singleton
    abstract fun bindTagEditorRepository(impl: TagEditorRepositoryImpl): TagEditorRepository

    @Binds
    @Singleton
    abstract fun bindPlaylistRepository(impl: PlaylistRepositoryImpl): PlaylistRepository
}
