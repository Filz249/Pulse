package com.pulse.music.ui

import android.appwidget.AppWidgetManager
import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import android.content.ComponentName
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.pulse.music.domain.model.Song
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
            var customHex by rememberSaveable { mutableStateOf(themePreferences.getString("custom_accent_hex", "#FFB347") ?: "#FFB347") }
            var customAccent by remember { mutableStateOf(themePreferences.getString("custom_accent_hex", null)?.let { hex -> runCatching { Color(android.graphics.Color.parseColor(hex)) }.getOrNull() }) }
            var showBrandSplash by rememberSaveable { mutableStateOf(true) }
            LaunchedEffect(Unit) {
                delay(850)
                showBrandSplash = false
            }

            PulseTheme(palette = selectedPalette, customPrimary = customAccent) {
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
                    val statsSongs by homeViewModel.librarySongs.collectAsState()
                    val statsPlaylists by homeViewModel.playlists.collectAsState()
                    val statsPlayCounts by homeViewModel.playCounts.collectAsState()
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
                                    customAccent = null
                                    themePreferences.edit().putString("palette", palette.key).remove("custom_accent_hex").apply()
                                    PulseWidgetProvider.refreshAll(context)
                                },
                                statsSongs = statsSongs,
                                statsPlaylistCount = statsPlaylists.size,
                                statsPlayCounts = statsPlayCounts,
                                customHex = customHex,
                                onCustomHexChange = { customHex = it },
                                onApplyCustomAccent = {
                                    val value = if (customHex.startsWith("#")) customHex else "#"+customHex
                                    runCatching { Color(android.graphics.Color.parseColor(value)) }.onSuccess { color ->
                                        customAccent = color
                                        customHex = value
                                        themePreferences.edit().putString("custom_accent_hex", value).apply()
                                        PulseWidgetProvider.refreshAll(context)
                                    }
                                },
                                onWheelColorSelected = { color ->
                                    customAccent = color
                                    customHex = "#%06X".format(color.toArgb() and 0xFFFFFF)
                                    themePreferences.edit().putString("custom_accent_hex", customHex).apply()
                                    PulseWidgetProvider.refreshAll(context)
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
            Icon(
                painter = painterResource(R.drawable.ic_pulse_equalizer),
                contentDescription = "Pulse",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(78.dp)
            )
            Text("PULSE", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold, letterSpacing = 4.sp, color = MaterialTheme.colorScheme.primary)
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
    onAddWidget: () -> Unit,
    customHex: String,
    onCustomHexChange: (String) -> Unit,
    onApplyCustomAccent: () -> Unit,
    onWheelColorSelected: (Color) -> Unit,
    statsSongs: List<Song>,
    statsPlaylistCount: Int,
    statsPlayCounts: Map<Long, Int>
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
                Icon(
                    painter = painterResource(R.drawable.ic_pulse_equalizer),
                    contentDescription = "Pulse logo",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(38.dp)
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
            DrawerListeningStats(
                songs = statsSongs,
                playlistCount = statsPlaylistCount,
                playCounts = statsPlayCounts,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp)
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
            var advancedExpanded by rememberSaveable { mutableStateOf(false) }
            var colorMode by rememberSaveable { mutableStateOf("wheel") }
            TextButton(
                onClick = { advancedExpanded = !advancedExpanded },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp)
            ) {
                Text(if (advancedExpanded) "Hide advanced color options" else "Advanced color options")
            }
            if (advancedExpanded) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = colorMode == "wheel", onClick = { colorMode = "wheel" }, label = { Text("Color wheel") })
                    FilterChip(selected = colorMode == "hex", onClick = { colorMode = "hex" }, label = { Text("HEX") })
                }
                if (colorMode == "wheel") {
                    PulseColorWheel(
                        modifier = Modifier.fillMaxWidth().height(210.dp).padding(horizontal = 38.dp),
                        onColorSelected = onWheelColorSelected
                    )
                    Text(customHex, modifier = Modifier.align(Alignment.CenterHorizontally), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelMedium)
                } else {
                    OutlinedTextField(
                        value = customHex,
                        onValueChange = onCustomHexChange,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        label = { Text("Custom accent (HEX)") },
                        placeholder = { Text("#FFB347") },
                        singleLine = true
                    )
                    Button(onClick = onApplyCustomAccent, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                        Text("Apply custom color")
                    }
                }
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


@Composable
private fun PulseColorWheel(modifier: Modifier = Modifier, onColorSelected: (Color) -> Unit) {
    val wheel = remember {
        val size = 256
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val pixels = IntArray(size * size)
        val center = (size - 1) / 2f
        val radius = center
        for (y in 0 until size) {
            for (x in 0 until size) {
                val dx = x - center
                val dy = y - center
                val distance = kotlin.math.sqrt(dx * dx + dy * dy)
                pixels[y * size + x] = if (distance <= radius) {
                    val hue = ((Math.toDegrees(kotlin.math.atan2(dy.toDouble(), dx.toDouble())) + 360.0) % 360.0).toFloat()
                    val saturation = (distance / radius).coerceIn(0f, 1f)
                    AndroidColor.HSVToColor(floatArrayOf(hue, saturation, 1f))
                } else AndroidColor.TRANSPARENT
            }
        }
        bitmap.setPixels(pixels, 0, size, 0, 0, size, size)
        bitmap.asImageBitmap()
    }
    Canvas(
        modifier = modifier.pointerInput(onColorSelected) {
            fun emitColor(position: Offset) {
                val center = Offset(size.width / 2f, size.height / 2f)
                val dx = position.x - center.x
                val dy = position.y - center.y
                val radius = minOf(size.width, size.height) / 2f
                val distance = kotlin.math.sqrt(dx * dx + dy * dy)
                if (radius > 0f && distance <= radius) {
                    val hue = ((Math.toDegrees(kotlin.math.atan2(dy.toDouble(), dx.toDouble())) + 360.0) % 360.0).toFloat()
                    val saturation = (distance / radius).coerceIn(0f, 1f)
                    onColorSelected(Color(AndroidColor.HSVToColor(floatArrayOf(hue, saturation, 1f))))
                }
            }
            detectDragGestures(
                onDragStart = { position -> emitColor(position) },
                onDrag = { change, _ ->
                    change.consume()
                    emitColor(change.position)
                }
            )
        }
    ) {
        val diameter = minOf(size.width, size.height).toInt()
        drawImage(wheel, dstSize = IntSize(diameter, diameter), dstOffset = androidx.compose.ui.unit.IntOffset(
            ((size.width - diameter) / 2f).toInt(), ((size.height - diameter) / 2f).toInt()
        ))
        drawCircle(
            color = Color.White.copy(alpha = 0.9f),
            radius = diameter / 2f - 1.dp.toPx(),
            style = Stroke(width = 2.dp.toPx())
        )
    }
}

@Composable
private fun DrawerListeningStats(
    songs: List<Song>,
    playlistCount: Int,
    playCounts: Map<Long, Int>,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val totalPlays = playCounts.values.sum()
    val playedSongs = songs.filter { (playCounts[it.id] ?: 0) > 0 }
    val topSongs = playedSongs.sortedByDescending { playCounts[it.id] ?: 0 }.take(5)
    val topArtists = songs.groupBy { it.artist }
        .mapValues { (_, tracks) -> tracks.sumOf { playCounts[it.id] ?: 0 } }
        .filterValues { it > 0 }
        .toList().sortedByDescending { it.second }.take(3)
    val topAlbums = songs.groupBy { it.albumId }
        .mapValues { (_, tracks) -> tracks.sumOf { playCounts[it.id] ?: 0 } to (tracks.firstOrNull()?.album ?: "Unknown album") }
        .filterValues { it.first > 0 }
        .values.sortedByDescending { it.first }.take(3)

    Card(
        modifier = modifier.clickable { expanded = !expanded },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.GraphicEq, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text("Listening statistics", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                Text(if (expanded) "Less" else "Details", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelSmall)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                StatValue("Plays", totalPlays.toString())
                StatValue("Tracks played", playedSongs.size.toString())
                StatValue("Playlists", playlistCount.toString())
            }
            if (expanded) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Text("Most played songs", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                if (topSongs.isEmpty()) Text("Your play history will build as you listen.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                topSongs.forEachIndexed { index, song ->
                    StatRankRow("${index + 1}. ${song.title}", "${song.artist} · ${playCounts[song.id] ?: 0} plays")
                }
                Text("Top artists", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                topArtists.forEachIndexed { index, entry -> StatRankRow("${index + 1}. ${entry.first}", "${entry.second} plays") }
                Text("Top albums", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                topAlbums.forEachIndexed { index, entry -> StatRankRow("${index + 1}. ${entry.second}", "${entry.first} plays") }
                Text("Library: ${songs.size} tracks · ${songs.map { it.albumId }.distinct().size} albums · ${songs.map { it.artist }.distinct().size} artists",
                    style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun StatValue(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun StatRankRow(title: String, detail: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(title, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, maxLines = 1)
        Spacer(Modifier.width(8.dp))
        Text(detail, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
    }
}
