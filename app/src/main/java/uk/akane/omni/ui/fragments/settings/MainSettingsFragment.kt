package uk.akane.omni.ui.fragments.settings

import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import androidx.preference.Preference
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import uk.akane.omni.R
import uk.akane.omni.logic.update.ApkInstaller
import uk.akane.omni.logic.update.UpdateChecker
import uk.akane.omni.ui.fragments.BasePreferenceFragment
import uk.akane.omni.ui.fragments.BaseSettingFragment

class MainSettingsFragment : BaseSettingFragment(
    R.string.settings,
    { MainSettingsTopFragment() })

class MainSettingsTopFragment : BasePreferenceFragment() {

    private var updatePreference: Preference? = null

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.settings_top, rootKey)

        val currentVersion = currentVersionName()
        findPreference<Preference>("version")?.summary = currentVersion

        updatePreference = findPreference<Preference>("update")?.apply {
            setOnPreferenceClickListener {
                checkForUpdate(currentVersion)
                true
            }
        }
    }

    private fun currentVersionName(): String =
        requireContext().packageManager
            .getPackageInfo(requireContext().packageName, 0).versionName ?: ""

    private fun checkForUpdate(currentVersion: String) {
        val preference = updatePreference ?: return
        preference.isEnabled = false
        preference.summary = getString(R.string.update_checking)

        viewLifecycleOwner.lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) { UpdateChecker.check(currentVersion) }
            if (!isAdded) return@launch
            preference.isEnabled = true
            when (result) {
                is UpdateChecker.Result.UpToDate ->
                    preference.summary = getString(R.string.update_up_to_date)
                is UpdateChecker.Result.Failed ->
                    preference.summary = getString(R.string.update_failed, result.reason)
                is UpdateChecker.Result.Available -> {
                    preference.summary =
                        getString(R.string.update_available, result.release.versionName)
                    offerInstall(result.release)
                }
            }
        }
    }

    private fun offerInstall(release: UpdateChecker.Release) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.update_available, release.versionName))
            .setMessage(release.notes.ifBlank { getString(R.string.settings_update_desc) })
            .setIcon(R.drawable.ic_omni)
            .setNegativeButton(R.string.dismiss, null)
            .setPositiveButton(R.string.update_dialog_install) { _, _ -> startInstall(release) }
            .show()
    }

    private fun startInstall(release: UpdateChecker.Release) {
        // Android will not let an app install packages until the user has granted it that
        // permission specifically, so send them there rather than failing silently.
        if (!ApkInstaller.canInstall(requireContext())) {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.update_permission_title)
                .setMessage(R.string.update_permission_text)
                .setNegativeButton(R.string.dismiss, null)
                .setPositiveButton(R.string.accept) { _, _ ->
                    ApkInstaller.requestInstallPermission(requireContext())
                }
                .show()
            return
        }

        val preference = updatePreference ?: return
        preference.isEnabled = false
        viewLifecycleOwner.lifecycleScope.launch {
            val error = withContext(Dispatchers.IO) {
                ApkInstaller.downloadAndInstall(requireContext().applicationContext, release) { progress ->
                    if (progress >= 0f) {
                        val percent = (progress * 100).toInt()
                        preference.summary = getString(R.string.update_downloading, percent)
                    }
                }
            }
            if (!isAdded) return@launch
            preference.isEnabled = true
            if (error != null) {
                preference.summary = getString(R.string.update_failed, error)
            }
        }
    }
}
