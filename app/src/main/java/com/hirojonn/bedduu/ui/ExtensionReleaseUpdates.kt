package com.hirojonn.bedduu.ui

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.hirojonn.bedduu.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import java.io.IOException
import java.util.concurrent.TimeUnit

object ExtensionReleaseUpdates {
    private const val WORK_NAME = "keiyoushi_release_check"
    private const val CHANNEL_ID = "extension_releases"
    private const val NOTIFICATION_ID = 5310
    private const val RELEASES_URL =
        "https://api.github.com/repos/keiyoushi/extensions-source/releases?per_page=1"
    private const val PREFS_NAME = "bendduu_prefs"
    private const val KEY_RELEASE_ID = "keiyoushi_release_id"
    private const val KEY_TRACKING_INITIALIZED = "keiyoushi_tracking_initialized"

    private val httpClient = OkHttpClient.Builder()
        .callTimeout(20, TimeUnit.SECONDS)
        .build()

    data class ReleaseInfo(
        val id: Long,
        val tag: String,
        val title: String,
        val url: String
    )

    sealed class CheckResult {
        object NoReleases : CheckResult()
        data class TrackingStarted(val release: ReleaseInfo) : CheckResult()
        data class ReleaseAvailable(val release: ReleaseInfo) : CheckResult()
        data class UpToDate(val release: ReleaseInfo) : CheckResult()
    }

    suspend fun check(context: Context, notifyOnNewRelease: Boolean): CheckResult =
        withContext(Dispatchers.IO) {
            val request = Request.Builder()
                .url(RELEASES_URL)
                .header("Accept", "application/vnd.github+json")
                .header("X-GitHub-Api-Version", "2022-11-28")
                .header("User-Agent", "Bendduu-Android")
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw IOException("GitHub returned HTTP ${response.code}.")
                }
                val responseBody = response.body?.string()
                    ?: throw IOException("GitHub returned an empty response.")
                val releases = try {
                    JSONArray(responseBody)
                } catch (error: org.json.JSONException) {
                    throw IOException("GitHub returned invalid release data.", error)
                }

                val preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                if (releases.length() == 0) {
                    preferences.edit()
                        .putBoolean(KEY_TRACKING_INITIALIZED, true)
                        .apply()
                    return@withContext CheckResult.NoReleases
                }

                val json = releases.optJSONObject(0)
                    ?: throw IOException("GitHub's latest release is malformed.")
                val release = ReleaseInfo(
                    id = json.optLong("id", 0L),
                    tag = json.optString("tag_name"),
                    title = json.optString("name").ifBlank { json.optString("tag_name") },
                    url = json.optString("html_url")
                )
                if (release.id <= 0 || release.tag.isBlank() || release.url.isBlank()) {
                    throw IOException("GitHub's latest release is missing required details.")
                }

                val wasInitialized = preferences.getBoolean(KEY_TRACKING_INITIALIZED, false)
                val lastReleaseId = preferences.getLong(KEY_RELEASE_ID, 0L)
                if (!wasInitialized) {
                    preferences.edit()
                        .putBoolean(KEY_TRACKING_INITIALIZED, true)
                        .putLong(KEY_RELEASE_ID, release.id)
                        .apply()
                    return@withContext CheckResult.TrackingStarted(release)
                }

                if (release.id != lastReleaseId) {
                    if (notifyOnNewRelease) showReleaseNotification(context, release)
                    preferences.edit().putLong(KEY_RELEASE_ID, release.id).apply()
                    CheckResult.ReleaseAvailable(release)
                } else {
                    CheckResult.UpToDate(release)
                }
            }
        }

    fun schedule(context: Context, enabled: Boolean) {
        if (!enabled) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
            return
        }

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        val request = PeriodicWorkRequestBuilder<ExtensionReleaseWorker>(12, TimeUnit.HOURS)
            .setConstraints(constraints)
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }

    private fun showReleaseNotification(context: Context, release: ReleaseInfo) {
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            throw IOException("Notification permission is not granted.")
        }
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) {
            throw IOException("Notifications are disabled for Bendduu.")
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    "Extension releases",
                    NotificationManager.IMPORTANCE_DEFAULT
                )
            )
        }

        val openRelease = PendingIntent.getActivity(
            context,
            NOTIFICATION_ID,
            Intent(Intent.ACTION_VIEW, Uri.parse(release.url)),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher)
            .setContentTitle("New Keiyoushi release")
            .setContentText(release.title)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("${release.title} (${release.tag}) is now available.")
            )
            .setContentIntent(openRelease)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
    }
}

class ExtensionReleaseWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val preferences = applicationContext.getSharedPreferences(
            "bendduu_prefs",
            Context.MODE_PRIVATE
        )
        if (!preferences.getBoolean("release_notifications", false)) return Result.success()

        return try {
            ExtensionReleaseUpdates.check(applicationContext, notifyOnNewRelease = true)
            Result.success()
        } catch (error: IOException) {
            Log.w("ExtensionReleaseWorker", "Release check failed; it will be retried.", error)
            Result.retry()
        }
    }
}
