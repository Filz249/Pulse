package com.pulse.music.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pulse.music.BuildConfig
import com.pulse.music.update.UpdateChecker
import com.pulse.music.update.UpdateStatus
import com.pulse.music.update.UpdateDownloadState
import com.pulse.music.update.UpdateInstaller
import com.pulse.music.update.UpdateInstallResult
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBackClick: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var updateStatus by remember { mutableStateOf(UpdateChecker.cached(context)) }
    var downloadInfo by remember { mutableStateOf(UpdateInstaller.getDownloadInfo(context)) }
    var installMessage by remember { mutableStateOf<String?>(null) }
    var checking by remember { mutableStateOf(false) }

    suspend fun checkUpdates(force: Boolean) {
        checking = true
        updateStatus = if (force) UpdateChecker.checkNow(context) else UpdateChecker.checkIfDue(context)
        checking = false
    }

    LaunchedEffect(Unit) {
        checkUpdates(force = false)
    }

    LaunchedEffect(updateStatus.state, updateStatus.latestCommit, updateStatus.downloadUrl) {
        if (updateStatus.state == UpdateStatus.State.AVAILABLE) {
            val url = updateStatus.downloadUrl
            val commit = updateStatus.latestCommit
            if (url != null && commit != null) {
                UpdateInstaller.ensureDownload(context, url, commit)
                while (true) {
                    val latest = UpdateInstaller.getDownloadInfo(context)
                    downloadInfo = latest
                    if (latest.state !in setOf(
                            UpdateDownloadState.PENDING,
                            UpdateDownloadState.DOWNLOADING,
                            UpdateDownloadState.PAUSED
                        )
                    ) break
                    delay(900)
                }
            }
        } else {
            downloadInfo = UpdateInstaller.getDownloadInfo(context)
        }
    }

    fun beginUpdateDownload() {
        val url = updateStatus.downloadUrl ?: return
        val commit = updateStatus.latestCommit ?: return
        UpdateInstaller.ensureDownload(context, url, commit)
        downloadInfo = UpdateInstaller.getDownloadInfo(context)
        installMessage = null
    }

    fun launchUpdateInstaller() {
        installMessage = when (UpdateInstaller.installDownloadedUpdate(context)) {
            UpdateInstallResult.LAUNCHED -> "Opening Android's installer. Follow its confirmation steps to finish updating Pulse."
            UpdateInstallResult.PERMISSION_REQUIRED -> "Allow Pulse to install updates in Android settings, return here, then tap Install update again."
            UpdateInstallResult.NOT_READY -> "The update file is not ready yet. Check the download status and try again."
            UpdateInstallResult.ERROR -> "Android could not open the installer. You can retry from this screen."
        }
    }

    Scaffold(
        modifier = modifier,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Pulse", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Text("Your music. Your device. Your control.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(18.dp)
            ) {
                Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.SystemUpdate, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(9.dp))
                        Text("Software updates", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                    }
                    Text(updateStatus.message, style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)

                    when (updateStatus.state) {
                        UpdateStatus.State.AVAILABLE -> {
                            Text(updateStatus.latestRelease ?: "Latest release",
                                style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                            when (downloadInfo.state) {
                                UpdateDownloadState.DOWNLOADING -> {
                                    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                                        Text("Downloading update automatically… ${downloadInfo.progressPercent}%",
                                            style = MaterialTheme.typography.bodyMedium)
                                        LinearProgressIndicator(
                                            progress = downloadInfo.progressPercent / 100f,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                }
                                UpdateDownloadState.PENDING, UpdateDownloadState.PAUSED -> {
                                    Text(
                                        if (downloadInfo.state == UpdateDownloadState.PAUSED) "Update download is paused." else "Update download is queued…",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                                }
                                UpdateDownloadState.DOWNLOADED -> {
                                    Button(onClick = { launchUpdateInstaller() }, modifier = Modifier.fillMaxWidth()) {
                                        Icon(Icons.Default.SystemUpdate, contentDescription = null)
                                        Spacer(Modifier.width(8.dp))
                                        Text("Install update")
                                    }
                                }
                                UpdateDownloadState.FAILED -> {
                                    OutlinedButton(onClick = { beginUpdateDownload() }, modifier = Modifier.fillMaxWidth()) {
                                        Icon(Icons.Default.Refresh, contentDescription = null)
                                        Spacer(Modifier.width(8.dp))
                                        Text("Retry update download")
                                    }
                                }
                                UpdateDownloadState.NOT_STARTED -> {
                                    Button(onClick = { beginUpdateDownload() }, modifier = Modifier.fillMaxWidth()) {
                                        Icon(Icons.Default.Download, contentDescription = null)
                                        Spacer(Modifier.width(8.dp))
                                        Text("Download update")
                                    }
                                }
                            }
                            installMessage?.let {
                                Text(it, style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        UpdateStatus.State.UP_TO_DATE -> {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CheckCircle, null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.width(6.dp))
                                Text("Up to date", fontWeight = FontWeight.Medium)
                            }
                        }
                        else -> Unit
                    }

                    OutlinedButton(
                        onClick = { scope.launch { checkUpdates(force = true) } },
                        enabled = !checking,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(if (checking) "Checking…" else "Check for updates")
                    }
                    Text(
                        text = if (updateStatus.checkedAt > 0L) {
                            "Last checked: " + DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
                                .format(Date(updateStatus.checkedAt))
                        } else {
                            "Pulse checks for updates when opened and downloads new versions automatically when one is available."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "When the download is ready, tap Install update. Android will show its normal installation confirmation; Pulse cannot silently install apps.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            SettingsInfoCard(icon = { Icon(Icons.Default.Info, null, tint = MaterialTheme.colorScheme.primary) }, title = "About Pulse") {
                InfoLine("Version", BuildConfig.VERSION_NAME)
                InfoLine("Build", BuildConfig.GIT_COMMIT.take(7).let {
                    if (it == "local") "Local development build" else it
                })
                InfoLine("Platform", "Android")
                InfoLine("Playback", "Local audio with background playback and system media controls")
            }

            SettingsInfoCard(icon = { Icon(Icons.Default.Lock, null, tint = MaterialTheme.colorScheme.primary) }, title = "Privacy") {
                Text(
                    "Your music library, playlists and listening history are stored on this device. Pulse does not use advertising or analytics SDKs and does not require an account.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Divider(color = MaterialTheme.colorScheme.outlineVariant)
                Text(
                    "When update checking runs, Pulse contacts GitHub to read release information. It does not upload your music, playlists or personal library data.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            SettingsInfoCard(icon = { Icon(Icons.Default.MusicNote, null, tint = MaterialTheme.colorScheme.primary) }, title = "Audio and permissions") {
                Text(
                    "Pulse needs access to audio files on your device to build your library. Editing embedded tags may require Android to ask for write access to a selected audio file.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                "Pulse is an independent offline music player. Update downloads are hosted by the Pulse GitHub repository.",
                modifier = Modifier.padding(bottom = 18.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SettingsInfoCard(
    icon: @Composable () -> Unit,
    title: String,
    content: @Composable () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                icon()
                Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            }
            content()
        }
    }
}

@Composable
private fun InfoLine(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f, fill = false))
    }
}
