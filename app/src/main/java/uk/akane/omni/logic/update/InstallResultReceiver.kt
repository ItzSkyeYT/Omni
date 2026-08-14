package uk.akane.omni.logic.update

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.util.Log
import android.widget.Toast
import uk.akane.omni.R

/**
 * Receives the outcome of a PackageInstaller session.
 *
 * The interesting case is STATUS_PENDING_USER_ACTION: the system will not install silently, so it
 * hands back an intent that must be launched to show the confirmation dialog. Ignoring it makes
 * the install appear to hang forever with no error.
 */
class InstallResultReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (val status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, -1)) {
            PackageInstaller.STATUS_PENDING_USER_ACTION -> {
                @Suppress("DEPRECATION")
                val confirm = intent.getParcelableExtra<Intent>(Intent.EXTRA_INTENT)
                confirm?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                confirm?.let { context.startActivity(it) }
            }
            PackageInstaller.STATUS_SUCCESS -> {
                // Nothing to announce: the app is about to be replaced, and the new build shows
                // its release notes on first launch.
                Log.i("InstallResult", "update installed")
            }
            else -> {
                val message = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE)
                Log.w("InstallResult", "install status $status: $message")
                Toast.makeText(
                    context,
                    context.getString(R.string.update_install_failed),
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
}
