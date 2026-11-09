package com.pulse.music.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.BitmapShader
import android.net.Uri
import android.widget.RemoteViews
import com.pulse.music.R
import com.pulse.music.ui.MainActivity

class PulseWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, appWidgetIds: IntArray) {
        val prefs = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
        val title = prefs.getString(KEY_TITLE, null) ?: "Nothing playing"
        val artist = prefs.getString(KEY_ARTIST, null) ?: "Open Pulse to choose a song"
        val playing = prefs.getBoolean(KEY_PLAYING, false)
        val artwork = prefs.getString(KEY_ARTWORK, null)
        val mediaId = prefs.getString(KEY_MEDIA_ID, null)
        val favorites = prefs.getStringSet(KEY_FAVORITES, emptySet()) ?: emptySet()
        appWidgetIds.forEach { id ->
            updateOne(context, manager, id, title, artist, playing, artwork, mediaId, mediaId != null && favorites.contains(mediaId))
        }
    }

    companion object {
        private const val PREFERENCES = "pulse_widget_state"
        private const val KEY_TITLE = "title"
        private const val KEY_ARTIST = "artist"
        private const val KEY_PLAYING = "playing"
        private const val KEY_ARTWORK = "artwork_uri"
        private const val KEY_MEDIA_ID = "media_id"
        private const val KEY_FAVORITES = "favorite_media_ids"

        fun refreshAll(context: Context) {
            val prefs = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
            updatePlaybackState(
                context,
                prefs.getString(KEY_TITLE, null) ?: "Nothing playing",
                prefs.getString(KEY_ARTIST, null) ?: "Open Pulse to choose a song",
                prefs.getBoolean(KEY_PLAYING, false),
                prefs.getString(KEY_ARTWORK, null)?.let(Uri::parse),
                prefs.getString(KEY_MEDIA_ID, null)
            )
        }

        fun updatePlaybackState(context: Context, title: String, artist: String, isPlaying: Boolean, artworkUri: Uri? = null, mediaId: String? = null) {
            val prefs = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
            prefs.edit().putString(KEY_TITLE, title).putString(KEY_ARTIST, artist).putBoolean(KEY_PLAYING, isPlaying)
                .putString(KEY_ARTWORK, artworkUri?.toString()).putString(KEY_MEDIA_ID, mediaId).apply()
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, PulseWidgetProvider::class.java))
            val favorites = prefs.getStringSet(KEY_FAVORITES, emptySet()) ?: emptySet()
            ids.forEach { id -> updateOne(context, manager, id, title, artist, isPlaying, artworkUri?.toString(), mediaId, mediaId != null && favorites.contains(mediaId)) }
        }

        fun toggleFavorite(context: Context) {
            val prefs = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
            val mediaId = prefs.getString(KEY_MEDIA_ID, null) ?: return
            val favorites = (prefs.getStringSet(KEY_FAVORITES, emptySet()) ?: emptySet()).toMutableSet()
            if (!favorites.add(mediaId)) favorites.remove(mediaId)
            prefs.edit().putStringSet(KEY_FAVORITES, favorites).apply()
            refreshAll(context)
        }

        private fun updateOne(context: Context, manager: AppWidgetManager, widgetId: Int, title: String, artist: String, isPlaying: Boolean, artworkUri: String?, mediaId: String?, isFavorite: Boolean) {
            val views = RemoteViews(context.packageName, R.layout.widget_pulse)
            val accent = accentFor(context)
            views.setTextViewText(R.id.widget_title, title)
            views.setTextViewText(R.id.widget_artist, artist)
            val artworkBitmap = artworkUri?.let { uri ->
                runCatching {
                    context.contentResolver.openInputStream(Uri.parse(uri))?.use { stream ->
                        val options = BitmapFactory.Options().apply { inSampleSize = 2 }
                        BitmapFactory.decodeStream(stream, null, options)
                    }
                }.getOrNull()
            }
            if (artworkBitmap == null) {
                views.setImageViewResource(R.id.widget_artwork, R.drawable.ic_pulse_equalizer)
                views.setImageViewResource(R.id.widget_backdrop, R.drawable.widget_pulse_background)
            } else {
                val cover = createEdgeToEdgeCover(artworkBitmap, 256)
                val backdrop = createBackdrop(artworkBitmap, 480, 120)
                views.setImageViewBitmap(R.id.widget_artwork, cover)
                views.setImageViewBitmap(R.id.widget_backdrop, backdrop)
                cover.recycle()
                backdrop.recycle()
                artworkBitmap.recycle()
            }
            views.setImageViewResource(R.id.widget_favorite, if (isFavorite) R.drawable.widget_ic_heart_filled else R.drawable.widget_ic_heart)
            views.setInt(R.id.widget_favorite, "setColorFilter", if (isFavorite) accent else Color.WHITE)
            views.setImageViewResource(R.id.widget_previous, R.drawable.widget_ic_previous)
            views.setImageViewResource(R.id.widget_play_pause, if (isPlaying) R.drawable.widget_ic_pause else R.drawable.widget_ic_play)
            views.setImageViewResource(R.id.widget_next, R.drawable.widget_ic_next)
            views.setImageViewResource(R.id.widget_open, R.drawable.widget_ic_queue)
            views.setInt(R.id.widget_title, "setTextColor", Color.WHITE)
            views.setInt(R.id.widget_artist, "setTextColor", Color.rgb(225, 211, 198))
            views.setInt(R.id.widget_play_pause, "setColorFilter", Color.WHITE)
            views.setInt(R.id.widget_previous, "setColorFilter", Color.WHITE)
            views.setInt(R.id.widget_next, "setColorFilter", Color.WHITE)
            views.setInt(R.id.widget_open, "setColorFilter", Color.WHITE)
            views.setInt(R.id.widget_play_pause, "setBackgroundResource", R.drawable.widget_play_button_bg)
            views.setOnClickPendingIntent(R.id.widget_favorite, actionPendingIntent(context, ACTION_FAVORITE, 8100))
            views.setOnClickPendingIntent(R.id.widget_previous, actionPendingIntent(context, ACTION_PREVIOUS, 8103))
            views.setOnClickPendingIntent(R.id.widget_play_pause, actionPendingIntent(context, ACTION_PLAY_PAUSE, 8101))
            views.setOnClickPendingIntent(R.id.widget_next, actionPendingIntent(context, ACTION_NEXT, 8102))
            views.setOnClickPendingIntent(R.id.widget_open, openPendingIntent(context))
            views.setOnClickPendingIntent(R.id.widget_artwork, openPendingIntent(context))
            views.setOnClickPendingIntent(R.id.widget_title, openPendingIntent(context))
            manager.updateAppWidget(widgetId, views)
        }

        private fun createEdgeToEdgeCover(source: Bitmap, size: Int): Bitmap {
            val side = minOf(source.width, source.height).coerceAtLeast(1)
            val cropped = Bitmap.createBitmap(source, (source.width - side) / 2, (source.height - side) / 2, side, side)
            val scaled = Bitmap.createScaledBitmap(cropped, size, size, true)
            if (cropped !== source) cropped.recycle()

            val output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(output)
            val shader = BitmapShader(scaled, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.shader = shader }
            val radius = size * 0.20f
            val radii = floatArrayOf(radius, radius, 0f, 0f, 0f, 0f, radius, radius)
            val path = Path().apply { addRoundRect(RectF(0f, 0f, size.toFloat(), size.toFloat()), radii, Path.Direction.CW) }
            canvas.drawPath(path, paint)
            scaled.recycle()
            return output
        }

        private fun createBackdrop(source: Bitmap, width: Int, height: Int): Bitmap {
            val tiny = Bitmap.createScaledBitmap(source, 28, 12, true)
            val backdrop = Bitmap.createScaledBitmap(tiny, width, height, true)
            if (tiny !== source) tiny.recycle()
            return backdrop
        }
        private fun accentFor(context: Context): Int {
            val prefs = context.getSharedPreferences("pulse_ui_preferences", Context.MODE_PRIVATE)
            val custom = prefs.getString("custom_accent_hex", null)
            if (!custom.isNullOrBlank()) runCatching { Color.parseColor(custom) }.getOrNull()?.let { return it }
            return when (prefs.getString("palette", "amber")) {
                "ocean" -> Color.rgb(105, 201, 194)
                "violet" -> Color.rgb(183, 160, 255)
                else -> Color.rgb(255, 179, 71)
            }
        }

        private fun openPendingIntent(context: Context): PendingIntent {
            val intent = Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            return PendingIntent.getActivity(context, 8104, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        }

        private fun actionPendingIntent(context: Context, action: String, requestCode: Int): PendingIntent {
            val intent = Intent(context, PulseWidgetActionReceiver::class.java)
                .setAction(action)
                .setData(Uri.parse("pulse-widget://action/$action"))
            return PendingIntent.getBroadcast(context, requestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        }

        const val ACTION_PLAY_PAUSE = "com.pulse.music.widget.PLAY_PAUSE"
        const val ACTION_NEXT = "com.pulse.music.widget.NEXT"
        const val ACTION_PREVIOUS = "com.pulse.music.widget.PREVIOUS"
        const val ACTION_FAVORITE = "com.pulse.music.widget.FAVORITE"
    }
}