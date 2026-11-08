package com.pulse.music.widget

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.pulse.music.player.AudioPlayerService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.guava.await
import kotlinx.coroutines.launch

class PulseWidgetActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action != PulseWidgetProvider.ACTION_PLAY_PAUSE && action != PulseWidgetProvider.ACTION_NEXT) return

        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate).launch {
            var controllerFuture: com.google.common.util.concurrent.ListenableFuture<MediaController>? = null
            try {
                val token = SessionToken(context, ComponentName(context, AudioPlayerService::class.java))
                controllerFuture = MediaController.Builder(context, token).buildAsync()
                val controller = controllerFuture.await()
                when (action) {
                    PulseWidgetProvider.ACTION_PLAY_PAUSE -> {
                        if (controller.isPlaying) controller.pause() else controller.play()
                    }
                    PulseWidgetProvider.ACTION_NEXT -> controller.seekToNextMediaItem()
                }
            } catch (_: Exception) {
                // The widget stays usable when the playback service is not available yet.
            } finally {
                controllerFuture?.let { MediaController.releaseFuture(it) }
                pendingResult.finish()
            }
        }
    }
}
