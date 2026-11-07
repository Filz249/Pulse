package com.pulse.music.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.pulse.music.ui.components.MiniPlayer
import com.pulse.music.ui.navigation.Screen
import com.pulse.music.ui.screens.home.HomeScreen
import com.pulse.music.ui.screens.home.HomeViewModel
import com.pulse.music.ui.screens.player.PlayerScreen
import com.pulse.music.ui.screens.player.PlayerViewModel
import com.pulse.music.ui.screens.tageditor.TagEditorScreen
import com.pulse.music.ui.screens.tageditor.TagEditorViewModel
import com.pulse.music.ui.theme.PulseTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            PulseTheme {
                val navController = rememberNavController()
                val homeViewModel: HomeViewModel = hiltViewModel()
                val currentSong by homeViewModel.playerConnection.currentSong.collectAsState()
                val isPlaying by homeViewModel.playerConnection.isPlaying.collectAsState()

                Scaffold(
                    bottomBar = {
                        if (currentSong != null) {
                            MiniPlayer(
                                song = currentSong,
                                isPlaying = isPlaying,
                                onTogglePlayPause = { homeViewModel.playerConnection.togglePlayPause() },
                                onSkipNext = { homeViewModel.playerConnection.skipNext() },
                                onExpandClick = { navController.navigate(Screen.Player.route) }
                            )
                        }
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        NavHost(
                            navController = navController,
                            startDestination = Screen.Home.route
                        ) {
                            composable(Screen.Home.route) {
                                HomeScreen(
                                    viewModel = homeViewModel,
                                    onSongClick = { songs, index ->
                                        homeViewModel.playSong(songs, index)
                                    },
                                    onEditSongTags = { song ->
                                        val route = Screen.TagEditor.createRoute(
                                            filePath = song.filePath,
                                            contentUri = song.uri.toString()
                                        )
                                        navController.navigate(route)
                                    }
                                )
                            }

                            composable(Screen.Player.route) {
                                val playerViewModel: PlayerViewModel = hiltViewModel()
                                PlayerScreen(
                                    viewModel = playerViewModel,
                                    onBackClick = { navController.popBackStack() }
                                )
                            }

                            composable(
                                route = Screen.TagEditor.route,
                                arguments = listOf(
                                    navArgument("filePath") { type = NavType.StringType },
                                    navArgument("contentUri") { type = NavType.StringType }
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
