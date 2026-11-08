package com.pulse.music.widget

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.pulse.music.player.AudioPlayerService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.guava.await
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

class PulseWidgetActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action == PulseWidgetProvider.ACTION_FAVORITE) {
            PulseWidgetProvider.toggleFavorite(context)
            return
        }
        if (action != PulseWidgetProvider.ACTION_PLAY_PAUSE &&
            action != PulseWidgetProvider.ACTION_NEXT &&
            action != PulseWidgetProvider.ACTION_PREVIOUS
        ) return

        val pendingResult = goAsync()
        val appContext = context.applicationContext
        CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate).launch {
            var future: com.google.common.util.concurrent.ListenableFuture<MediaController>? = null
            try {
                val token = SessionToken(appContext, ComponentName(appContext, AudioPlayerService::class.java))
                future = MediaController.Builder(appContext, token).buildAsync()
                val controller = withTimeout(8_000L) { future.await() }
                when (action) {
                    PulseWidgetProvider.ACTION_PLAY_PAUSE -> {
                        if (controller.isPlaying || controller.playWhenReady) controller.pause() else controller.play()
                    }
                    PulseWidgetProvider.ACTION_NEXT -> {
                        if (controller.isCommandAvailable(Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM)) controller.seekToNextMediaItem()
                    }
                    PulseWidgetProvider.ACTION_PREVIOUS -> {
                        if (controller.mediaItemCount > 0) {
                            if (controller.currentPosition > 3_000L || !controller.isCommandAvailable(Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM)) {
                                controller.seekTo(0L)
                            } else {
                                controller.seekToPreviousMediaItem()
                            }
                        }
                    }
                }
            } catch (exception: Exception) {
                Log.e(TAG, "Widget playback action failed: $action", exception)
                Toast.makeText(appContext, "Pulse could not control playback. Open the app and try again.", Toast.LENGTH_SHORT).show()
            } finally {
                future?.let { MediaController.releaseFuture(it) }
                pendingResult.finish()
            }
        }
    }

    companion object {
        private const val TAG = "PulseWidgetAction"
    }
}