package com.github.dedis.popstellar.ui.settings;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.ViewModelProvider;

import com.github.dedis.popstellar.R;
import com.github.dedis.popstellar.ViewModelFactory;
import com.github.dedis.popstellar.utility.ActivityUtils;

public class SettingsActivity extends AppCompatActivity {

  private SettingsViewModel mViewModel;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.settings_activity);

    setupSettingsFragment();

    mViewModel = obtainViewModel(this);

    // Subscribe to "open settings" event
    mViewModel
        .getOpenSettingsEvent()
        .observe(
            this,
            booleanEvent -> {
              Boolean event = booleanEvent.getContentIfNotHandled();
              if (event != null) {
                setupSettingsFragment();
              }
            });
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    getMenuInflater().inflate(R.menu.options_menu, menu);
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    if (item.getItemId() == R.id.settings) {
      mViewModel.openSettings();
      return true;
    } else {
      return super.onOptionsItemSelected(item);
    }
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
      ActivityUtils.replaceFragmentInActivity(
          getSupportFragmentManager(), settingsFragment, R.id.fragment_container_settings);
    }
  }
}
