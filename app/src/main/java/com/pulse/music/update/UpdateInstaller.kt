package com.pulse.music.update

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings

enum class UpdateDownloadState {
    NOT_STARTED, PENDING, DOWNLOADING, PAUSED, DOWNLOADED, FAILED
}

data class UpdateDownloadInfo(
    val state: UpdateDownloadState = UpdateDownloadState.NOT_STARTED,
    val progressPercent: Int = 0
)

enum class UpdateInstallResult {
    LAUNCHED, PERMISSION_REQUIRED, NOT_READY, ERROR
}

/**
 * Downloads the latest APK with Android's DownloadManager.
 * Installation is always handed to Android's package installer for user approval.
 */
object UpdateInstaller {
    private const val PREFS = "pulse_updates"
    private const val KEY_DOWNLOAD_ID = "download_id"
    private const val KEY_DOWNLOAD_COMMIT = "download_commit"
    private const val APK_MIME_TYPE = "application/vnd.android.package-archive"

    private fun manager(context: Context): DownloadManager =
        context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

    fun ensureDownload(context: Context, downloadUrl: String, commit: String): Long {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val downloadManager = manager(context)
        val previousId = prefs.getLong(KEY_DOWNLOAD_ID, -1L)
        val previousCommit = prefs.getString(KEY_DOWNLOAD_COMMIT, null)

        if (previousId > 0L && previousCommit == commit) {
            val existing = getDownloadInfo(context)
            if (existing.state != UpdateDownloadState.FAILED &&
                existing.state != UpdateDownloadState.NOT_STARTED
            ) {
                return previousId
            }
            downloadManager.remove(previousId)
        } else if (previousId > 0L) {
            downloadManager.remove(previousId)
        }

        val request = DownloadManager.Request(Uri.parse(downloadUrl))
            .setTitle("Pulse update")
            .setDescription("Downloading the latest version of Pulse")
            .setMimeType(APK_MIME_TYPE)
            .setAllowedOverRoaming(false)
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalFilesDir(
                context,
                Environment.DIRECTORY_DOWNLOADS,
                "Pulse-update.apk"
            )

        val newId = downloadManager.enqueue(request)
        prefs.edit()
            .putLong(KEY_DOWNLOAD_ID, newId)
            .putString(KEY_DOWNLOAD_COMMIT, commit)
            .apply()
        return newId
    }

    fun getDownloadInfo(context: Context): UpdateDownloadInfo {
        val id = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getLong(KEY_DOWNLOAD_ID, -1L)
        if (id <= 0L) return UpdateDownloadInfo()

        val cursor = manager(context).query(DownloadManager.Query().setFilterById(id))
            ?: return UpdateDownloadInfo(UpdateDownloadState.FAILED)

        cursor.use {
            if (!it.moveToFirst()) return UpdateDownloadInfo(UpdateDownloadState.FAILED)
            val status = it.getInt(it.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
            val downloaded = it.getLong(
                it.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)
            )
            val total = it.getLong(
                it.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)
            )
            val progress = if (total > 0L) {
                ((downloaded * 100L) / total).toInt().coerceIn(0, 100)
            } else {
                0
            }

            val state = when (status) {
                DownloadManager.STATUS_PENDING -> UpdateDownloadState.PENDING
                DownloadManager.STATUS_RUNNING -> UpdateDownloadState.DOWNLOADING
                DownloadManager.STATUS_PAUSED -> UpdateDownloadState.PAUSED
                DownloadManager.STATUS_SUCCESSFUL -> UpdateDownloadState.DOWNLOADED
                DownloadManager.STATUS_FAILED -> UpdateDownloadState.FAILED
                else -> UpdateDownloadState.FAILED
            }
            return UpdateDownloadInfo(state, progress)
        }
    }

    fun installDownloadedUpdate(context: Context): UpdateInstallResult {
        val id = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getLong(KEY_DOWNLOAD_ID, -1L)
        if (id <= 0L || getDownloadInfo(context).state != UpdateDownloadState.DOWNLOADED) {
            return UpdateInstallResult.NOT_READY
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            !context.packageManager.canRequestPackageInstalls()
        ) {
            val permissionIntent = Intent(
                Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                Uri.parse("package:" + context.packageName)
            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            return runCatching {
                context.startActivity(permissionIntent)
                UpdateInstallResult.PERMISSION_REQUIRED
            }.getOrDefault(UpdateInstallResult.ERROR)
        }

        val apkUri = manager(context).getUriForDownloadedFile(id)
            ?: return UpdateInstallResult.NOT_READY
        val installIntent = Intent(Intent.ACTION_INSTALL_PACKAGE).apply {
            setData(apkUri)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        return runCatching {
            context.startActivity(installIntent)
            UpdateInstallResult.LAUNCHED
        }.getOrDefault(UpdateInstallResult.ERROR)
    }
}
