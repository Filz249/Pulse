package com.pulse.music.ui.navigation

import java.net.URLEncoder
import java.nio.charset.StandardCharsets

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Settings : Screen("settings")
    object Equalizer : Screen("equalizer")
    object Player : Screen("player")
    object Album : Screen("album/{albumId}") { fun createRoute(albumId: Long) = "album/$albumId" }
    object TagEditor : Screen("tag_editor/{filePath}/{contentUri}/{albumId}") {
        fun createRoute(filePath: String, contentUri: String, albumId: Long): String {
            val encodedPath = URLEncoder.encode(filePath, StandardCharsets.UTF_8.toString())
            val encodedUri = URLEncoder.encode(contentUri, StandardCharsets.UTF_8.toString())
            return "tag_editor/$encodedPath/$encodedUri/$albumId"
        }
    }
}
