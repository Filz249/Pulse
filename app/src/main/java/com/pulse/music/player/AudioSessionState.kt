package com.pulse.music.player

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

object AudioSessionState {
    private val _audioSessionId = MutableStateFlow(0)
    val audioSessionId = _audioSessionId.asStateFlow()

    fun update(sessionId: Int) {
        _audioSessionId.value = sessionId
    }
}
