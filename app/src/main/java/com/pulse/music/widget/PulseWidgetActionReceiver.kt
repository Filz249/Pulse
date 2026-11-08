package com.pulse.music.widget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.pulse.music.player.PlayerConnection
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class PulseWidgetActionReceiver : BroadcastReceiver() {
    @Inject lateinit var playerConnection: PlayerConnection

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            PulseWidgetProvider.ACTION_FAVORITE -> PulseWidgetProvider.toggleFavorite(context)
            PulseWidgetProvider.ACTION_PLAY_PAUSE -> playerConnection.handleWidgetAction("PLAY_PAUSE")
            PulseWidgetProvider.ACTION_NEXT -> playerConnection.handleWidgetAction("NEXT")
            PulseWidgetProvider.ACTION_PREVIOUS -> playerConnection.handleWidgetAction("PREVIOUS")
        }
    }
}