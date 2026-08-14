package uk.akane.omni.logic.update

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import java.net.HttpURLConnection
import java.net.URL

/**
 * Downloads a release apk and hands it to the system installer.
 *
 * Uses the PackageInstaller session API rather than the older ACTION_VIEW route: the bytes are
 * streamed straight into a session, so there is no file on shared storage, no FileProvider, no
 * authority string and no uri permission to grant.
 *
 * The install only succeeds if the new apk is signed with the same certificate as the installed
 * one. Android keys update identity on the signing certificate and there is no override.
 */
object ApkInstaller {

    private const val TAG = "ApkInstaller"
    const val ACTION_INSTALL_STATUS = "uk.akane.omni.INSTALL_STATUS"

    /** True when the user has granted this app permission to install packages. */
    fun canInstall(context: Context): Boolean =
        context.packageManager.canRequestPackageInstalls()

    /** Sends the user to the per-app "install unknown apps" toggle. */
    fun requestInstallPermission(context: Context) {
        val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, "package:${context.packageName}".toUri())
        } else {
            Intent(Settings.ACTION_SECURITY_SETTINGS)
        }
        context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    private fun String.toUri(): Uri = Uri.parse(this)

    /**
     * Blocking; call from a background dispatcher. [onProgress] receives 0..1, or -1 when the
     * server does not report a length.
     */
    fun downloadAndInstall(
        context: Context,
        release: UpdateChecker.Release,
        onProgress: (Float) -> Unit
    ): String? {
        val installer = context.packageManager.packageInstaller
        val params = PackageInstaller.SessionParams(
            PackageInstaller.SessionParams.MODE_FULL_INSTALL
        )
        val expected = release.sizeBytes
        if (expected > 0) params.setSize(expected)

        var sessionId = -1
        try {
            sessionId = installer.createSession(params)
            installer.openSession(sessionId).use { session ->
                val connection = (URL(release.apkUrl).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 20_000
                    readTimeout = 60_000
                    instanceFollowRedirects = true
                }
                try {
                    if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                        return "download failed: HTTP ${connection.responseCode}"
                    }
                    val total = if (expected > 0) expected else connection.contentLengthLong
                    session.openWrite("omni.apk", 0, total).use { output ->
                        connection.inputStream.use { input ->
                            val buffer = ByteArray(64 * 1024)
                            var written = 0L
                            while (true) {
                                val read = input.read(buffer)
                                if (read < 0) break
                                output.write(buffer, 0, read)
                                written += read
                                onProgress(if (total > 0) written.toFloat() / total else -1f)
                            }
                        }
                        session.fsync(output)
                    }
                } finally {
                    connection.disconnect()
                }

                val intent = Intent(ACTION_INSTALL_STATUS).setPackage(context.packageName)
                val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
                val pending = PendingIntent.getBroadcast(context, sessionId, intent, flags)
                session.commit(pending.intentSender)
            }
            return null
        } catch (e: Exception) {
            Log.w(TAG, "install failed", e)
            if (sessionId != -1) runCatching { installer.abandonSession(sessionId) }
            return e.message ?: "install failed"
        }
    }
}
