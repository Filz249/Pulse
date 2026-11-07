package com.pulse.music.update

import android.content.Context
import com.pulse.music.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

data class UpdateStatus(
    val state: State = State.NOT_CHECKED,
    val message: String = "Update status has not been checked yet.",
    val latestRelease: String? = null,
    val downloadUrl: String? = null,
    val checkedAt: Long = 0L
) {
    enum class State { NOT_CHECKED, UP_TO_DATE, AVAILABLE, ERROR }
}

object UpdateChecker {
    private const val PREFS = "pulse_updates"
    private const val KEY_STATE = "state"
    private const val KEY_MESSAGE = "message"
    private const val KEY_RELEASE = "release"
    private const val KEY_URL = "download_url"
    private const val KEY_CHECKED_AT = "checked_at"
    private const val CHECK_INTERVAL_MS = 24L * 60L * 60L * 1000L
    private const val RELEASE_URL = "https://api.github.com/repos/Filz249/Pulse/releases/latest"

    private val mutex = Mutex()

    fun cached(context: Context): UpdateStatus {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val state = runCatching {
            UpdateStatus.State.valueOf(prefs.getString(KEY_STATE, UpdateStatus.State.NOT_CHECKED.name)!!)
        }.getOrDefault(UpdateStatus.State.NOT_CHECKED)

        return UpdateStatus(
            state = state,
            message = prefs.getString(KEY_MESSAGE, "Update status has not been checked yet.")
                ?: "Update status has not been checked yet.",
            latestRelease = prefs.getString(KEY_RELEASE, null),
            downloadUrl = prefs.getString(KEY_URL, null),
            checkedAt = prefs.getLong(KEY_CHECKED_AT, 0L)
        )
    }

    suspend fun checkIfDue(context: Context): UpdateStatus = mutex.withLock {
        val current = cached(context)
        val fresh = current.checkedAt > 0L &&
            System.currentTimeMillis() - current.checkedAt < CHECK_INTERVAL_MS
        if (fresh) current else fetchAndStore(context)
    }

    suspend fun checkNow(context: Context): UpdateStatus = mutex.withLock {
        fetchAndStore(context)
    }

    private suspend fun fetchAndStore(context: Context): UpdateStatus = withContext(Dispatchers.IO) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        var connection: HttpURLConnection? = null
        try {
            connection = (URL(RELEASE_URL).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 10_000
                readTimeout = 10_000
                setRequestProperty("Accept", "application/vnd.github+json")
                setRequestProperty("User-Agent", "Pulse-Android")
                setRequestProperty("X-GitHub-Api-Version", "2022-11-28")
            }

            if (connection.responseCode !in 200..299) {
                throw IOException("GitHub returned HTTP ${connection.responseCode}")
            }

            val response = connection.inputStream.bufferedReader().use { it.readText() }
            val release = JSONObject(response)
            val body = release.optString("body")
            val commit = Regex("(?i)Commit:[^0-9a-f]*([0-9a-f]{40})")
                .find(body)?.groupValues?.getOrNull(1)
            val assets = release.optJSONArray("assets")
            var downloadUrl: String? = null

            if (assets != null) {
                for (index in 0 until assets.length()) {
                    val asset = assets.optJSONObject(index) ?: continue
                    if (asset.optString("name") == "Pulse.apk") {
                        downloadUrl = asset.optString("browser_download_url").takeIf { it.isNotBlank() }
                        break
                    }
                }
            }

            if (commit.isNullOrBlank() || downloadUrl.isNullOrBlank()) {
                throw IOException("Latest release is missing its APK or build commit.")
            }

            val currentCommit = BuildConfig.GIT_COMMIT
            val result = when {
                currentCommit == "local" -> UpdateStatus(
                    state = UpdateStatus.State.ERROR,
                    message = "This development build cannot identify its GitHub commit. Install a GitHub release build to compare versions.",
                    latestRelease = release.optString("name").ifBlank { release.optString("tag_name") },
                    downloadUrl = downloadUrl
                )
                commit.equals(currentCommit, ignoreCase = true) -> UpdateStatus(
                    state = UpdateStatus.State.UP_TO_DATE,
                    message = "You're using the latest version of Pulse.",
                    latestRelease = release.optString("name").ifBlank { release.optString("tag_name") },
                    downloadUrl = downloadUrl
                )
                else -> UpdateStatus(
                    state = UpdateStatus.State.AVAILABLE,
                    message = "A newer version of Pulse is available.",
                    latestRelease = release.optString("name").ifBlank { release.optString("tag_name") },
                    downloadUrl = downloadUrl
                )
            }.copy(checkedAt = System.currentTimeMillis())

            prefs.edit()
                .putString(KEY_STATE, result.state.name)
                .putString(KEY_MESSAGE, result.message)
                .putString(KEY_RELEASE, result.latestRelease)
                .putString(KEY_URL, result.downloadUrl)
                .putLong(KEY_CHECKED_AT, result.checkedAt)
                .apply()
            result
        } catch (exception: Exception) {
            val failed = UpdateStatus(
                state = UpdateStatus.State.ERROR,
                message = "Couldn't check for updates. Check your connection and try again.",
                checkedAt = System.currentTimeMillis()
            )
            prefs.edit()
                .putString(KEY_STATE, failed.state.name)
                .putString(KEY_MESSAGE, failed.message)
                .remove(KEY_RELEASE)
                .remove(KEY_URL)
                .putLong(KEY_CHECKED_AT, failed.checkedAt)
                .apply()
            failed
        } finally {
            connection?.disconnect()
        }
    }
}
