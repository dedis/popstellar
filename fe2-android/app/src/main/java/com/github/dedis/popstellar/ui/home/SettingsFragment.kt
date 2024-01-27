package com.github.dedis.popstellar.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.preference.Preference
import androidx.preference.SwitchPreference
import com.github.dedis.popstellar.R
import com.github.dedis.popstellar.utility.MessageValidator.verify
import com.github.dedis.popstellar.utility.NetworkLogger.Companion.setServerUrl
import com.takisoft.preferencex.EditTextPreference
import com.takisoft.preferencex.PreferenceFragmentCompat
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SettingsFragment : PreferenceFragmentCompat() {
  private lateinit var settingsViewModel: SettingsViewModel
  private lateinit var homeViewModel: HomeViewModel

  override fun onCreatePreferencesFix(savedInstanceState: Bundle?, rootKey: String?) {
    // Load the preferences from an XML resource
    setPreferencesFromResource(R.xml.settings_preferences, rootKey)

    // Set callbacks for debugging section
    setDebuggingPreferences()
  }

  override fun onCreateView(
      inflater: LayoutInflater,
      container: ViewGroup?,
      savedInstanceState: Bundle?
  ): View {
    homeViewModel = HomeActivity.obtainViewModel(requireActivity())
    settingsViewModel = HomeActivity.obtainSettingsViewModel(requireActivity())

    handleBackNav()

    return super.onCreateView(inflater, container, savedInstanceState)
  }

  override fun onResume() {
    super.onResume()
    homeViewModel.setPageTitle(R.string.settings)
    homeViewModel.setIsHome(false)
  }

  /** Function that sets the callback for switching the preferences in the section "Debugging" */
  private fun setDebuggingPreferences() {
    val serverUrl =
        preferenceManager.findPreference<EditTextPreference>(
            getString(R.string.settings_server_url_key))
    val enableLogging =
        preferenceManager.findPreference<SwitchPreference>(getString(R.string.settings_logging_key))

    // Initially the switch is disabled unless it's already on
    enableLogging!!.isEnabled = enableLogging.isChecked

    // Initially the edit text is enabled unless the logging is already on
    serverUrl!!.isEnabled = !enableLogging.isChecked

    // Set the callback for managing the logging
    enableLogging.onPreferenceChangeListener =
        Preference.OnPreferenceChangeListener { _: Preference?, newValue: Any ->
          // Save the URL preference if the switch is turned on
          val isLoggingEnabled = newValue as Boolean
          if (isLoggingEnabled) {
            settingsViewModel.enableLogging()
          } else {
            settingsViewModel.disableLogging()
          }
          // Enable the edit text if switch off, disable it if on
          // (i.e. the switch has to be off to change the server address)
          serverUrl.isEnabled = !isLoggingEnabled
          true
        }

    // Set the callback for managing the server url
    serverUrl.onPreferenceChangeListener =
        Preference.OnPreferenceChangeListener returnPoint@{ _: Preference?, newValue: Any ->
          val newUrl = newValue as String
          try {
            // Check if the new value is a valid URL
            verify().validUrl(newUrl)

            // Save the server url
            setServerUrl(newUrl)

            // Enable the switch preference based on URL validity
            enableLogging.isEnabled = true

            return@returnPoint true
          } catch (e: IllegalArgumentException) {
            enableLogging.isEnabled = false
            // Show an error message and prevent the preference from being updated
            Toast.makeText(context, R.string.error_settings_url_server, Toast.LENGTH_SHORT).show()
            return@returnPoint true
          }
        }
  }

  private fun handleBackNav() {
    HomeActivity.addBackNavigationCallbackToHome(requireActivity(), viewLifecycleOwner, TAG)
  }

  companion object {
    val TAG: String = SettingsFragment::class.java.simpleName

    @JvmStatic
    fun newInstance(): SettingsFragment {
      return SettingsFragment()
    }
  }
}
