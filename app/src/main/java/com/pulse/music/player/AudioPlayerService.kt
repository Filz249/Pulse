package com.pulse.music.player

import android.content.Intent
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.DefaultMediaNotificationProvider
import com.pulse.music.R
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.Player
import com.pulse.music.widget.PulseWidgetProvider
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService

/**
 * Foreground service hosting the ExoPlayer instance and MediaSession.
 * Handles background audio playback, system notifications, lock-screen controls,
 * and audio focus transitions (such as incoming calls or unplugged headphones).
 */
@OptIn(UnstableApi::class)
class AudioPlayerService : MediaSessionService() {

    private var mediaSession: MediaSession? = null

    override fun onCreate() {
        super.onCreate()

        // Give Pulse its own playback notification channel on Android versions
        // where the Media3 notification provider controls the notification.
        setMediaNotificationProvider(
            DefaultMediaNotificationProvider.Builder(this)
                .setChannelId("pulse_playback")
                .setChannelName(R.string.media_notification_channel)
                .build()
        )

        val audioAttributes = AudioAttributes.Builder()
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .setUsage(C.USAGE_MEDIA)
            .build()

        val player = ExoPlayer.Builder(this)
            .setAudioAttributes(audioAttributes, true)
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_LOCAL)
            .build()

        player.addListener(object : Player.Listener {
            override fun onAudioSessionIdChanged(audioSessionId: Int) {
                AudioSessionState.update(audioSessionId)
            }

            override fun onMediaMetadataChanged(mediaMetadata: androidx.media3.common.MediaMetadata) {
                updateWidget(player)
            }

            override fun onMediaItemTransition(mediaItem: androidx.media3.common.MediaItem?, reason: Int) {
                updateWidget(player)
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                updateWidget(player)
            }
        })
        AudioSessionState.update(player.audioSessionId)
        mediaSession = MediaSession.Builder(this, player).build()
    }

    private fun updateWidget(player: ExoPlayer) {
        val title = player.mediaMetadata.title?.toString()?.takeIf { it.isNotBlank() } ?: "Nothing playing"
        val artist = player.mediaMetadata.artist?.toString()?.takeIf { it.isNotBlank() } ?: "Open Pulse to choose a song"
        PulseWidgetProvider.updatePlaybackState(this, title, artist, player.isPlaying)
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = mediaSession?.player
        if (player != null && !player.playWhenReady) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        mediaSession?.run {
            player.release()
            release()
        }
        mediaSession = null
        AudioSessionState.update(0)
        super.onDestroy()
    }
}
