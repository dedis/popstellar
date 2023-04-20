package com.github.dedis.popstellar.ui.home;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.*;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.github.dedis.popstellar.R;
import com.github.dedis.popstellar.databinding.SettingsFragmentBinding;
import com.takisoft.fix.support.v7.preference.PreferenceFragmentCompat;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class SettingsFragment extends PreferenceFragmentCompat {

  public static final String TAG = SettingsFragment.class.getSimpleName();

  public static SettingsFragment newInstance() {
    return new SettingsFragment();
  }

  private SettingsViewModel viewModel;

  @Override
  public void onCreatePreferencesFix(@Nullable Bundle savedInstanceState, String rootKey) {
    // Load the preferences from an XML resource
    setPreferencesFromResource(R.xml.settings_preferences, rootKey);
  }

  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater,
      @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState) {
    SettingsFragmentBinding binding = SettingsFragmentBinding.inflate(inflater, container, false);
    binding.setLifecycleOwner(getActivity());
    viewModel = HomeActivity.obtainSettingsViewModel(requireActivity());

    setDebuggingPreferences();

    return binding.getRoot();
  }

  /** Function that sets the callback for switching the preferences in the section "Debugging" */
  private void setDebuggingPreferences() {
    getPreferenceManager()
        .findPreference("enable_logging")
        .setOnPreferenceChangeListener(
            ((preference, newValue) -> {
              boolean selected = (boolean) newValue;
              if (selected) {
                final boolean[] confirmation = {false};
                // confirmation screen
                new AlertDialog.Builder(getContext())
                    .setTitle(R.string.confirm_title)
                    .setMessage(R.string.settings_enable_logging_confirmation)
                    .setPositiveButton(R.string.yes, (dialogInterface, i) -> confirmation[0] = true)
                    .setNegativeButton(R.string.no, null)
                    .show();
                if (confirmation[0]) {
                  viewModel.enableLogging();
                }
                return confirmation[0];
              } else {
                viewModel.disableLogging();
                return true;
              }
            }));
  }
}
