package com.github.dedis.popstellar.ui.settings;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.ViewModelProvider;

import com.github.dedis.popstellar.R;
import com.github.dedis.popstellar.ViewModelFactory;
import com.github.dedis.popstellar.utility.ActivityUtils;

public class SettingsActivity extends AppCompatActivity {

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.settings_activity);

    setupSettingsFragment();
  }

  public static SettingsViewModel obtainViewModel(FragmentActivity activity) {
    ViewModelFactory factory = ViewModelFactory.getInstance(activity.getApplication());
    return new ViewModelProvider(activity, factory).get(SettingsViewModel.class);
  }

  private void setupSettingsFragment() {
    SettingsFragment settingsFragment =
        (SettingsFragment) getSupportFragmentManager().findFragmentById(R.id.fragment_settings);
    if (settingsFragment == null) {
      settingsFragment = SettingsFragment.newInstance();
    }
    ActivityUtils.replaceFragmentInActivity(
        getSupportFragmentManager(), settingsFragment, R.id.fragment_container_settings);
  }
}
