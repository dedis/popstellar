package com.github.dedis.popstellar.ui.home;

import android.os.Bundle;
import android.view.*;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.SwitchPreference;

import com.github.dedis.popstellar.R;
import com.github.dedis.popstellar.utility.MessageValidator;
import com.github.dedis.popstellar.utility.NetworkLogger;
import com.takisoft.preferencex.EditTextPreference;
import com.takisoft.preferencex.PreferenceFragmentCompat;

import java.util.Objects;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class SettingsFragment extends PreferenceFragmentCompat {

  public static final String TAG = SettingsFragment.class.getSimpleName();

  public static SettingsFragment newInstance() {
    return new SettingsFragment();
  }

  private SettingsViewModel settingsViewModel;
  private HomeViewModel homeViewModel;

  public SettingsFragment() {
    // Empty constructor
  }

  @Override
  public void onCreatePreferencesFix(@Nullable Bundle savedInstanceState, String rootKey) {
    // Load the preferences from an XML resource
    setPreferencesFromResource(R.xml.settings_preferences, rootKey);

    // Set callbacks for debugging section
    setDebuggingPreferences();
  }

  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater,
      @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState) {

    homeViewModel = HomeActivity.obtainViewModel(requireActivity());
    settingsViewModel = HomeActivity.obtainSettingsViewModel(requireActivity());

    return super.onCreateView(inflater, container, savedInstanceState);
  }

  @Override
  public void onResume() {
    super.onResume();
    homeViewModel.setPageTitle(R.string.settings);
    homeViewModel.setIsHome(false);
  }

  /** Function that sets the callback for switching the preferences in the section "Debugging" */
  private void setDebuggingPreferences() {
    EditTextPreference serverUrl =
        getPreferenceManager().findPreference(getString(R.string.settings_server_url_key));
    SwitchPreference enableLogging =
        getPreferenceManager().findPreference(getString(R.string.settings_logging_key));

    // Initially the switch is disabled unless it's already on
    Objects.requireNonNull(enableLogging).setEnabled(enableLogging.isChecked());

    // Initially the edit text is enabled unless the logging is already on
    Objects.requireNonNull(serverUrl).setEnabled(!enableLogging.isChecked());

    // Set the callback for managing the logging
    enableLogging.setOnPreferenceChangeListener(
        ((preference, newValue) -> {
          // Save the URL preference if the switch is turned on
          boolean isLoggingEnabled = (Boolean) newValue;

          if (isLoggingEnabled) {
            settingsViewModel.enableLogging();
          } else {
            settingsViewModel.disableLogging();
          }
          // Enable the edit text if switch off, disable it if on
          // (i.e. the switch has to be off to change the server address)
          serverUrl.setEnabled(!isLoggingEnabled);

          // Return true to allow the preference to be saved
          return true;
        }));

    // Set the callback for managing the server url
    serverUrl.setOnPreferenceChangeListener(
        (preference, newValue) -> {
          // Check if the new value is a valid URL
          String newUrl = (String) newValue;

          try {
            MessageValidator.verify().validUrl(newUrl);
            // Save the server url
            NetworkLogger.setServerUrl(newUrl);
            // Enable the switch preference based on URL validity
            enableLogging.setEnabled(true);
            return true;
          } catch (IllegalArgumentException e) {
            enableLogging.setEnabled(false);
            // Show an error message and prevent the preference from being updated
            Toast.makeText(getContext(), R.string.error_settings_url_server, Toast.LENGTH_SHORT)
                .show();
            return true;
          }
        });
  }
}
