package com.pulse.music.ui

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.pulse.music.BuildConfig
import com.pulse.music.R
import com.pulse.music.ui.components.MiniPlayer
import com.pulse.music.ui.navigation.Screen
import com.pulse.music.ui.screens.album.AlbumScreen
import com.pulse.music.ui.screens.equalizer.EqualizerScreen
import com.pulse.music.ui.screens.home.HomeNavigationTab
import com.pulse.music.ui.screens.home.HomeScreen
import com.pulse.music.ui.screens.home.HomeViewModel
import com.pulse.music.ui.screens.player.PlayerScreen
import com.pulse.music.ui.screens.player.PlayerViewModel
import com.pulse.music.ui.screens.settings.SettingsScreen
import com.pulse.music.ui.screens.tageditor.TagEditorScreen
import com.pulse.music.ui.screens.tageditor.TagEditorViewModel
import com.pulse.music.ui.theme.PulsePalette
import com.pulse.music.ui.theme.PulseTheme
import com.pulse.music.widget.PulseWidgetProvider
import com.pulse.music.update.UpdateChecker
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        lifecycleScope.launch { UpdateChecker.checkIfDue(applicationContext) }

        setContent {
            val context = LocalContext.current
            val themePreferences = remember {
                context.getSharedPreferences("pulse_ui_preferences", MODE_PRIVATE)
            }
            var selectedPalette by rememberSaveable {
                mutableStateOf(PulsePalette.fromKey(themePreferences.getString("palette", null)))
            }
            var showBrandSplash by rememberSaveable { mutableStateOf(true) }
            LaunchedEffect(Unit) {
                delay(850)
                showBrandSplash = false
            }

            PulseTheme(palette = selectedPalette) {
                if (showBrandSplash) {
                    BrandSplashScreen()
                } else {
                    val navController = rememberNavController()
                    val drawerState = rememberDrawerState(DrawerValue.Closed)
                    val scope = rememberCoroutineScope()
                    val currentEntry by navController.currentBackStackEntryAsState()
                    var selectedHomeTab by rememberSaveable { mutableStateOf(HomeNavigationTab.HOME) }
                    val isHomeDestination = currentEntry?.destination?.route == Screen.Home.route
                    val homeViewModel: HomeViewModel = hiltViewModel()
                    val currentSong by homeViewModel.playerConnection.currentSong.collectAsState()
                    val isPlaying by homeViewModel.playerConnection.isPlaying.collectAsState()
                    val showMiniPlayer = currentSong != null && isHomeDestination

                    ModalNavigationDrawer(
                        drawerState = drawerState,
                        gesturesEnabled = true,
                        drawerContent = {
                            PulseDrawerContent(
                                selectedPalette = selectedPalette,
                                onPaletteSelected = { palette ->
                                    selectedPalette = palette
                                    themePreferences.edit().putString("palette", palette.key).apply()
                                },
                                onEqualizer = {
                                    scope.launch {
                                        drawerState.close()
                                        navController.navigate(Screen.Equalizer.route) { launchSingleTop = true }
                                    }
                                },
                                onSettings = {
                                    scope.launch {
                                        drawerState.close()
                                        navController.navigate(Screen.Settings.route) { launchSingleTop = true }
                                    }
                                },
                                onAddWidget = {
                                    scope.launch {
                                        drawerState.close()
                                        val manager = AppWidgetManager.getInstance(context)
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && manager.isRequestPinAppWidgetSupported) {
                                            manager.requestPinAppWidget(
                                                ComponentName(context, PulseWidgetProvider::class.java),
                                                null,
                                                null
                                            )
                                        } else {
                                            Toast.makeText(context, "Your launcher does not support adding widgets from Pulse.", Toast.LENGTH_LONG).show()
                                        }
                                    }
                                }
                            )
                        }
                    ) {
                        Scaffold(
                            contentWindowInsets = WindowInsets(0, 0, 0, 0),
                            bottomBar = {
                                if (isHomeDestination) {
                                    Column {
                                        if (showMiniPlayer) {
                                            MiniPlayer(
                                                song = currentSong,
                                                isPlaying = isPlaying,
                                                onTogglePlayPause = { homeViewModel.playerConnection.togglePlayPause() },
                                                onSkipNext = { homeViewModel.playerConnection.skipNext() },
                                                onExpandClick = { navController.navigate(Screen.Player.route) }
                                            )
                                        }
                                        NavigationBar(
                                            windowInsets = WindowInsets.navigationBars,
                                            containerColor = MaterialTheme.colorScheme.surface,
                                            tonalElevation = 6.dp
                                        ) {
                                            val navColors = NavigationBarItemDefaults.colors(
                                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                                indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
                                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            NavigationBarItem(
                                                selected = selectedHomeTab == HomeNavigationTab.HOME,
                                                onClick = { selectedHomeTab = HomeNavigationTab.HOME },
                                                icon = { Icon(Icons.Default.Home, contentDescription = null) },
                                                label = { Text("Home") },
                                                colors = navColors
                                            )
                                            NavigationBarItem(
                                                selected = selectedHomeTab == HomeNavigationTab.SEARCH,
                                                onClick = { selectedHomeTab = HomeNavigationTab.SEARCH },
                                                icon = { Icon(Icons.Default.Search, contentDescription = null) },
                                                label = { Text("Search") },
                                                colors = navColors
                                            )
                                            NavigationBarItem(
                                                selected = selectedHomeTab == HomeNavigationTab.PLAYLISTS,
                                                onClick = { selectedHomeTab = HomeNavigationTab.PLAYLISTS },
                                                icon = { Icon(Icons.Default.QueueMusic, contentDescription = null) },
                                                label = { Text("Playlists") },
                                                colors = navColors
                                            )
                                            NavigationBarItem(
                                                selected = selectedHomeTab == HomeNavigationTab.CREATE,
                                                onClick = { selectedHomeTab = HomeNavigationTab.CREATE },
                                                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                                                label = { Text("Create") },
                                                colors = navColors
                                            )
                                        }
                                    }
                                }
                            }
                        ) { innerPadding ->
                            Box(
                                Modifier
                                    .fillMaxSize()
                                    .padding(innerPadding)
                                    .windowInsetsPadding(WindowInsets.statusBars)
                                    .then(if (isHomeDestination) Modifier else Modifier.windowInsetsPadding(WindowInsets.navigationBars))
                            ) {
                                NavHost(navController, startDestination = Screen.Home.route) {
                                    composable(Screen.Home.route) {
                                        HomeScreen(
                                            viewModel = homeViewModel,
                                            onSongClick = { list, index -> homeViewModel.playSong(list, index) },
                                            onAlbumClick = { albumId -> navController.navigate(Screen.Album.createRoute(albumId)) },
                                            onOpenSettings = { navController.navigate(Screen.Settings.route) },
                                            onOpenMenu = { scope.launch { drawerState.open() } },
                                            selectedBottomTab = selectedHomeTab,
                                            onBottomTabChange = { selectedHomeTab = it },
                                            onEditSongTags = { song ->
                                                navController.navigate(Screen.TagEditor.createRoute(song.filePath, song.uri.toString(), song.albumId))
                                            }
                                        )
                                    }
                                    composable(
                                        Screen.Album.route,
                                        arguments = listOf(navArgument("albumId") { type = NavType.LongType })
                                    ) { entry ->
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
                                    composable(Screen.Equalizer.route) {
                                        EqualizerScreen(onBackClick = { navController.popBackStack() })
                                    }
                                    composable(Screen.Player.route) {
                                        val playerViewModel: PlayerViewModel = hiltViewModel()
                                        PlayerScreen(
                                            viewModel = playerViewModel,
                                            onBackClick = { navController.popBackStack() },
                                            onEditSongTags = { song ->
                                                navController.navigate(Screen.TagEditor.createRoute(song.filePath, song.uri.toString(), song.albumId))
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
    }
}

@Composable
private fun BrandSplashScreen() {
    Box(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Image(
                painter = painterResource(R.drawable.ic_pulse_mark),
                contentDescription = "Pulse",
                modifier = Modifier.size(104.dp)
            )
            Text("PULSE", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold, letterSpacing = 4.sp)
            Text(
                "YOUR MUSIC. YOUR WAY.",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = 2.sp
            )
        }
    }
}

@Composable
private fun PulseDrawerContent(
    selectedPalette: PulsePalette,
    onPaletteSelected: (PulsePalette) -> Unit,
    onEqualizer: () -> Unit,
    onSettings: () -> Unit,
    onAddWidget: () -> Unit
) {
    ModalDrawerSheet(
        drawerContainerColor = MaterialTheme.colorScheme.surface,
        drawerContentColor = MaterialTheme.colorScheme.onSurface
    ) {
        Column(
            Modifier.fillMaxHeight().verticalScroll(rememberScrollState()).padding(vertical = 12.dp)
        ) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_pulse_mark),
                    contentDescription = "Pulse logo",
                    modifier = Modifier.size(48.dp)
                )
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("Pulse", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("Music library", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            NavigationDrawerItem(
                label = { Text("Equalizer") },
                selected = false,
                icon = { Icon(Icons.Default.GraphicEq, null) },
                onClick = onEqualizer,
                modifier = Modifier.padding(horizontal = 12.dp)
            )
            NavigationDrawerItem(
                label = { Text("Add home-screen widget") },
                selected = false,
                icon = { Icon(Icons.Default.Widgets, null) },
                onClick = onAddWidget,
                modifier = Modifier.padding(horizontal = 12.dp)
            )
            NavigationDrawerItem(
                label = { Text("Settings & updates") },
                selected = false,
                icon = { Icon(Icons.Default.Settings, null) },
                onClick = onSettings,
                modifier = Modifier.padding(horizontal = 12.dp)
            )
            HorizontalDivider(Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.outlineVariant)
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Palette, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(10.dp))
                Text("App themes", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            }
            PulsePalette.values().forEach { palette ->
                NavigationDrawerItem(
                    label = { Text(palette.label) },
                    selected = palette == selectedPalette,
                    icon = {
                        Box(
                            Modifier.size(18.dp).clip(CircleShape).background(palette.primary)
                        )
                    },
                    onClick = { onPaletteSelected(palette) },
                    modifier = Modifier.padding(horizontal = 12.dp),
                    colors = NavigationDrawerItemDefaults.colors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        selectedIconColor = MaterialTheme.colorScheme.primary
                    )
                )
            }
            HorizontalDivider(Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.outlineVariant)
            Column(Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Pulse for Android", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                }
                Text("Version ${BuildConfig.VERSION_NAME}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    "Build ${BuildConfig.GIT_COMMIT.take(7)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "Update checking and installation options are in Settings.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
