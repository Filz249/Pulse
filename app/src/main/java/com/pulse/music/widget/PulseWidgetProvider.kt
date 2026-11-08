package com.pulse.music.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.pulse.music.R

class PulseWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, appWidgetIds: IntArray) {
        val preferences = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
        val title = preferences.getString(KEY_TITLE, null) ?: "Nothing playing"
        val artist = preferences.getString(KEY_ARTIST, null) ?: "Open Pulse to choose a song"
        val playing = preferences.getBoolean(KEY_PLAYING, false)
        appWidgetIds.forEach { updateOne(context, manager, it, title, artist, playing) }
    }

    companion object {
        private const val PREFERENCES = "pulse_widget_state"
        private const val KEY_TITLE = "title"
        private const val KEY_ARTIST = "artist"
        private const val KEY_PLAYING = "playing"

        fun refreshAll(context: Context) {
            val preferences = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
            updatePlaybackState(
                context,
                preferences.getString(KEY_TITLE, null) ?: "Nothing playing",
                preferences.getString(KEY_ARTIST, null) ?: "Open Pulse to choose a song",
                preferences.getBoolean(KEY_PLAYING, false)
            )
        }

        fun updatePlaybackState(context: Context, title: String, artist: String, isPlaying: Boolean) {
            context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE).edit()
                .putString(KEY_TITLE, title)
                .putString(KEY_ARTIST, artist)
                .putBoolean(KEY_PLAYING, isPlaying)
                .apply()

            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, PulseWidgetProvider::class.java))
            ids.forEach { updateOne(context, manager, it, title, artist, isPlaying) }
        }

        private fun updateOne(
            context: Context,
            manager: AppWidgetManager,
            widgetId: Int,
            title: String,
            artist: String,
            isPlaying: Boolean
        ) {
            val views = RemoteViews(context.packageName, R.layout.widget_pulse)
            views.setTextViewText(R.id.widget_title, title)
            views.setTextViewText(R.id.widget_artist, artist)
            val accent = when (
                context.getSharedPreferences("pulse_ui_preferences", Context.MODE_PRIVATE)
                    .getString("palette", "amber")
            ) {
                "ocean" -> android.graphics.Color.rgb(105, 201, 194)
                "violet" -> android.graphics.Color.rgb(183, 160, 255)
                else -> android.graphics.Color.rgb(255, 179, 71)
            }
            views.setTextColor(R.id.widget_brand, accent)
            views.setInt(R.id.widget_play_pause, "setColorFilter", accent)
            views.setInt(R.id.widget_next, "setColorFilter", accent)
            views.setImageViewResource(
                R.id.widget_play_pause,
                if (isPlaying) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play
            )
            views.setOnClickPendingIntent(
                R.id.widget_play_pause,
                actionPendingIntent(context, ACTION_PLAY_PAUSE, 8101)
            )
            views.setOnClickPendingIntent(
                R.id.widget_next,
                actionPendingIntent(context, ACTION_NEXT, 8102)
            )
            manager.updateAppWidget(widgetId, views)
        }

        private fun actionPendingIntent(context: Context, action: String, requestCode: Int): PendingIntent {
            val intent = Intent(context, PulseWidgetActionReceiver::class.java).setAction(action)
            return PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        const val ACTION_PLAY_PAUSE = "com.pulse.music.widget.PLAY_PAUSE"
        const val ACTION_NEXT = "com.pulse.music.widget.NEXT"
    }
}
