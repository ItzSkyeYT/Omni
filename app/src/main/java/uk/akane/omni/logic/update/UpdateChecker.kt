package uk.akane.omni.logic.update

import android.util.Log
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Checks the fork's own GitHub releases for a newer build.
 *
 * This fork is not on any store, so nothing else will ever tell a user an update exists. The
 * GitHub REST API needs no token for a public repository, and the unauthenticated allowance is
 * far more than a once-a-day check will ever use.
 */
object UpdateChecker {

    private const val TAG = "UpdateChecker"
    private const val LATEST_RELEASE_URL =
        "https://api.github.com/repos/ItzSkyeYT/Omni/releases/latest"
    private const val TIMEOUT_MS = 15_000

    data class Release(
        val versionName: String,
        val notes: String,
        val apkUrl: String,
        val sizeBytes: Long
    )

    sealed interface Result {
        data class Available(val release: Release) : Result
        data object UpToDate : Result
        data class Failed(val reason: String) : Result
    }

    /** Blocking; call from a background dispatcher. */
    fun check(currentVersionName: String): Result {
        val release = try {
            fetchLatest()
        } catch (e: Exception) {
            Log.w(TAG, "update check failed", e)
            return Result.Failed(e.message ?: "network error")
        } ?: return Result.UpToDate

        return if (isNewer(release.versionName, currentVersionName)) {
            Result.Available(release)
        } else {
            Result.UpToDate
        }
    }

    /** Returns null when the repository has no releases yet, which is not an error. */
    private fun fetchLatest(): Release? {
        val connection = (URL(LATEST_RELEASE_URL).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = TIMEOUT_MS
            readTimeout = TIMEOUT_MS
            setRequestProperty("Accept", "application/vnd.github+json")
            setRequestProperty("X-GitHub-Api-Version", "2022-11-28")
        }
        try {
            // A repository with tags but no published releases answers 404 here. That is the
            // normal state before the first release and must not surface as a failure.
            if (connection.responseCode == HttpURLConnection.HTTP_NOT_FOUND) return null
            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                throw IllegalStateException("HTTP ${connection.responseCode}")
            }
            val json = JSONObject(connection.inputStream.bufferedReader().use { it.readText() })
            if (json.optBoolean("draft") || json.optBoolean("prerelease")) return null

            val assets = json.getJSONArray("assets")
            for (i in 0 until assets.length()) {
                val asset = assets.getJSONObject(i)
                val name = asset.optString("name")
                if (name.endsWith(".apk", ignoreCase = true)) {
                    return Release(
                        versionName = normalise(json.optString("tag_name")),
                        notes = json.optString("body").trim(),
                        apkUrl = asset.getString("browser_download_url"),
                        sizeBytes = asset.optLong("size")
                    )
                }
            }
            // A release with notes but no apk cannot be installed, so it is not an update.
            return null
        } finally {
            connection.disconnect()
        }
    }

    /** Tags in this repo have been both "v1.5" and "1.3", so the prefix is optional. */
    internal fun normalise(tag: String): String = tag.trim().removePrefix("v").removePrefix("V")

    /**
     * Compares dotted version strings numerically, so 1.10 correctly beats 1.9. Anything
     * unparseable sorts as zero rather than throwing, since a malformed tag upstream should not
     * crash the app that reads it.
     */
    internal fun isNewer(candidate: String, current: String): Boolean {
        val a = normalise(candidate).split('.')
        val b = normalise(current).split('.')
        for (i in 0 until maxOf(a.size, b.size)) {
            val x = a.getOrNull(i)?.takeWhile { it.isDigit() }?.toIntOrNull() ?: 0
            val y = b.getOrNull(i)?.takeWhile { it.isDigit() }?.toIntOrNull() ?: 0
            if (x != y) return x > y
        }
        return false
    }
}
