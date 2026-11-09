package com.pulse.music.widget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import com.pulse.music.player.PlayerConnection
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import javax.inject.Inject

@AndroidEntryPoint
class PulseWidgetActionReceiver : BroadcastReceiver() {
    @Inject lateinit var playerConnection: PlayerConnection

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action == PulseWidgetProvider.ACTION_FAVORITE) {
            PulseWidgetProvider.toggleFavorite(context)
            return
        }
        val playerAction = when (action) {
            PulseWidgetProvider.ACTION_PLAY_PAUSE -> "PLAY_PAUSE"
            PulseWidgetProvider.ACTION_NEXT -> "NEXT"
            PulseWidgetProvider.ACTION_PREVIOUS -> "PREVIOUS"
            else -> return
        }

        // Keep the broadcast alive while the MediaController binds to the playback service.
        val pendingResult = goAsync()
        val appContext = context.applicationContext
        CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate).launch {
            try {
                val completed = withTimeout(8_000L) { playerConnection.handleWidgetAction(playerAction) }
                if (!completed) {
                    Toast.makeText(appContext, "Open Pulse and start a song before using the widget controls.", Toast.LENGTH_SHORT).show()
                }
            } catch (error: Exception) {
                Log.e("PulseWidgetAction", "Broadcast action failed", error)
                Toast.makeText(appContext, "Pulse couldn't control playback. Open the app and try again.", Toast.LENGTH_SHORT).show()
            } finally {
                pendingResult.finish()
            }
        }
    }
}