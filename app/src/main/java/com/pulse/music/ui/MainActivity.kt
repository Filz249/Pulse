package com.pulse.music.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import com.pulse.music.update.UpdateChecker
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.pulse.music.ui.components.MiniPlayer
import com.pulse.music.ui.navigation.Screen
import com.pulse.music.ui.screens.home.HomeScreen
import com.pulse.music.ui.screens.home.HomeViewModel
import com.pulse.music.ui.screens.album.AlbumScreen
import com.pulse.music.ui.screens.player.PlayerScreen
import com.pulse.music.ui.screens.player.PlayerViewModel
import com.pulse.music.ui.screens.settings.SettingsScreen
import com.pulse.music.ui.screens.tageditor.TagEditorScreen
import com.pulse.music.ui.screens.tageditor.TagEditorViewModel
import com.pulse.music.ui.theme.PulseTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        lifecycleScope.launch { UpdateChecker.checkIfDue(applicationContext) }

        setContent {
            PulseTheme {
                val navController = rememberNavController()
                val currentEntry by navController.currentBackStackEntryAsState()
                val homeViewModel: HomeViewModel = hiltViewModel()
                val currentSong by homeViewModel.playerConnection.currentSong.collectAsState()
                val isPlaying by homeViewModel.playerConnection.isPlaying.collectAsState()
                val showMiniPlayer = currentSong != null &&
                    currentEntry?.destination?.route == Screen.Home.route

                Scaffold(
                    contentWindowInsets = WindowInsets(0, 0, 0, 0),
                    bottomBar = {
                        if (showMiniPlayer) {
                            MiniPlayer(
                                song = currentSong,
                                isPlaying = isPlaying,
                                onTogglePlayPause = { homeViewModel.playerConnection.togglePlayPause() },
                                onSkipNext = { homeViewModel.playerConnection.skipNext() },
                                onExpandClick = { navController.navigate(Screen.Player.route) },
                                modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
                            )
                        }
                    }
                ) { innerPadding ->
                    Box(
                        Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .windowInsetsPadding(WindowInsets.statusBars)
                            .then(if (showMiniPlayer) Modifier else Modifier.windowInsetsPadding(WindowInsets.navigationBars))
                    ) {
                        NavHost(navController, startDestination = Screen.Home.route) {
                            composable(Screen.Home.route) {
                                HomeScreen(
                                    homeViewModel,
                                    onSongClick = { list, index -> homeViewModel.playSong(list, index) },
                                    onAlbumClick = { albumId -> navController.navigate(Screen.Album.createRoute(albumId)) },
                                    onOpenSettings = { navController.navigate(Screen.Settings.route) },
                                    onEditSongTags = { song ->
                                        navController.navigate(
                                            Screen.TagEditor.createRoute(song.filePath, song.uri.toString(), song.albumId)
                                        )
                                    }
                                )
                            }
                            composable(Screen.Album.route, arguments = listOf(navArgument("albumId") { type = NavType.LongType })) { entry ->
                                val albumId = entry.arguments?.getLong("albumId") ?: -1L
                                AlbumScreen(
                                    viewModel = homeViewModel,
                                    albumId = albumId,
                                    onBackClick = { navController.popBackStack() },
                                    onPlaySongs = { tracks, index -> homeViewModel.playSong(tracks, index) },
                                    onEditSongTags = { song ->
                                        navController.navigate(Screen.TagEditor.createRoute(song.filePath, song.uri.toString(), song.albumId))
                                    }
                                )
                            }
                            composable(Screen.Settings.route) {
                                SettingsScreen(onBackClick = { navController.popBackStack() })
                            }
                            composable(Screen.Player.route) {
                                val playerViewModel: PlayerViewModel = hiltViewModel()
                                PlayerScreen(
                                    viewModel = playerViewModel,
                                    onBackClick = { navController.popBackStack() },
                                    onEditSongTags = { song ->
                                        navController.navigate(
                                            Screen.TagEditor.createRoute(song.filePath, song.uri.toString(), song.albumId)
                                        )
                                    }
                                )
                            }
                            composable(
                                Screen.TagEditor.route,
                                arguments = listOf(
                                    navArgument("filePath") { type = NavType.StringType },
                                    navArgument("contentUri") { type = NavType.StringType },
                                    navArgument("albumId") { type = NavType.LongType }
                                )
                            ) {
                                val tagEditorViewModel: TagEditorViewModel = hiltViewModel()
                                TagEditorScreen(
                                    viewModel = tagEditorViewModel,
                                    onBackClick = { navController.popBackStack() }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
