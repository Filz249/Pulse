package com.pulse.music.player

import android.content.ComponentName
import android.net.Uri
import com.pulse.music.R
import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.pulse.music.domain.model.Song
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.guava.await
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Singleton managing the connection to [AudioPlayerService] through [MediaController].
 * Acts as the bridge between UI ViewModels and the background playback session.
 */
@Singleton
class PlayerConnection @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var mediaController: MediaController? = null
    private var currentPlaylist: List<Song> = emptyList()

    private val _currentSong = MutableStateFlow<Song?>(null)
    val currentSong: StateFlow<Song?> = _currentSong.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _shuffleMode = MutableStateFlow(false)
    val shuffleMode: StateFlow<Boolean> = _shuffleMode.asStateFlow()

    private val _repeatMode = MutableStateFlow(Player.REPEAT_MODE_OFF)
    val repeatMode: StateFlow<Int> = _repeatMode.asStateFlow()

    init {
        scope.launch {
            connect()
        }
    }

    private suspend fun connect() {
        val sessionToken = SessionToken(
            context,
            ComponentName(context, AudioPlayerService::class.java)
        )

        mediaController = MediaController.Builder(context, sessionToken)
            .buildAsync()
            .await()

        mediaController?.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _isPlaying.value = isPlaying
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                val mediaId = mediaItem?.mediaId?.toLongOrNull()
                _currentSong.value = currentPlaylist.find { it.id == mediaId }
            }

            override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
                _shuffleMode.value = shuffleModeEnabled
            }

            override fun onRepeatModeChanged(mode: Int) {
                _repeatMode.value = mode
            }
        })
    }

    fun playSongs(songs: List<Song>, startIndex: Int = 0) {
        currentPlaylist = songs
        mediaController?.apply {
            val mediaItems = songs.map { song ->
                val meta = MediaMetadata.Builder()
                    .setTitle(song.title)
                    .setArtist(song.artist)
                    .setAlbumTitle(song.album)
                    .setArtworkUri(song.albumArtUri ?: Uri.parse("android.resource://${context.packageName}/${R.drawable.ic_pulse_mark}"))
                    .build()

                MediaItem.Builder()
                    .setMediaId(song.id.toString())
                    .setUri(song.uri)
                    .setMediaMetadata(meta)
                    .build()
            }
            setMediaItems(mediaItems, startIndex, 0L)
            prepare()
            play()
            _currentSong.value = songs.getOrNull(startIndex)
        }
    }

    fun togglePlayPause() {
        mediaController?.let {
            if (it.isPlaying) it.pause() else it.play()
        }
    }

    fun skipNext() = mediaController?.seekToNextMediaItem()
    fun skipPrevious() = mediaController?.seekToPreviousMediaItem()
    fun seekTo(positionMs: Long) = mediaController?.seekTo(positionMs)

    fun toggleShuffle() {
        mediaController?.let {
            it.shuffleModeEnabled = !it.shuffleModeEnabled
        }
    }

    fun toggleRepeatMode() {
        mediaController?.let {
            it.repeatMode = when (it.repeatMode) {
                Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
                Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
                else -> Player.REPEAT_MODE_OFF
            }
        }
    }

    val currentPosition: Long
        get() = mediaController?.currentPosition ?: 0L

    val duration: Long
        get() = mediaController?.duration ?: 0L
}
