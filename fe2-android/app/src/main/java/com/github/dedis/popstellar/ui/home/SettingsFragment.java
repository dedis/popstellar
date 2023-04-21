package com.github.dedis.popstellar.ui.home;

import android.os.Bundle;
import android.view.*;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.github.dedis.popstellar.R;
import com.takisoft.fix.support.v7.preference.PreferenceFragmentCompat;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class SettingsFragment extends PreferenceFragmentCompat {

  public static SettingsFragment newInstance() {
    return new SettingsFragment();
  }

  private SettingsViewModel settingsViewModel;
  private HomeViewModel homeViewModel;

  public SettingsFragment() {}

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
    // Set the callback for managing the logging
    getPreferenceManager()
        .findPreference("enable_logging")
        .setOnPreferenceChangeListener(
            ((preference, newValue) -> {
              boolean selected = (boolean) newValue;
              if (selected) {
                settingsViewModel.enableServerLogging();
              } else {
                settingsViewModel.disableServerLogging();
              }
              return true;
            }));
  }
}
