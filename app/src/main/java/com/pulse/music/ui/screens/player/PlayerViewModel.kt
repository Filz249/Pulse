package com.pulse.music.ui.screens.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pulse.music.domain.model.Song
import com.pulse.music.player.PlayerConnection
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val playerConnection: PlayerConnection
) : ViewModel() {

    val currentSong: StateFlow<Song?> = playerConnection.currentSong
    val isPlaying: StateFlow<Boolean> = playerConnection.isPlaying
    val shuffleMode: StateFlow<Boolean> = playerConnection.shuffleMode
    val repeatMode: StateFlow<Int> = playerConnection.repeatMode

    private val _currentPosition = MutableStateFlow(0L)
    val currentPosition: StateFlow<Long> = _currentPosition.asStateFlow()

    val duration: Long get() = playerConnection.duration

    init {
        viewModelScope.launch {
            while (true) {
                _currentPosition.value = playerConnection.currentPosition
                delay(250)
            }
        }
    }

    fun togglePlayPause() = playerConnection.togglePlayPause()
    fun skipNext() = playerConnection.skipNext()
    fun skipPrevious() = playerConnection.skipPrevious()
    fun seekTo(positionMs: Long) = playerConnection.seekTo(positionMs)
    fun toggleShuffle() = playerConnection.toggleShuffle()
    fun toggleRepeatMode() = playerConnection.toggleRepeatMode()
}
