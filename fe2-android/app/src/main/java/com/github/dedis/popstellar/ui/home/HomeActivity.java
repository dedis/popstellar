package com.github.dedis.popstellar.ui.home;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.IdRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.*;
import androidx.lifecycle.ViewModelProvider;

import com.github.dedis.popstellar.R;
import com.github.dedis.popstellar.model.network.serializer.JsonUtils;
import com.github.dedis.popstellar.repository.local.PersistentData;
import com.github.dedis.popstellar.ui.settings.SettingsActivity;
import com.github.dedis.popstellar.utility.ActivityUtils;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.security.GeneralSecurityException;
import java.util.function.Supplier;

import dagger.hilt.android.AndroidEntryPoint;

/** HomeActivity represents the entry point for the application. */
@AndroidEntryPoint
public class HomeActivity extends AppCompatActivity {

  private final String TAG = HomeActivity.class.getSimpleName();

  private HomeViewModel viewModel;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.home_activity);

    viewModel = obtainViewModel(this);

    // Load all the json schemas in background when the app is started.
    AsyncTask.execute(
        () -> {
          JsonUtils.loadSchema(JsonUtils.ROOT_SCHEMA);
          JsonUtils.loadSchema(JsonUtils.DATA_SCHEMA);
          JsonUtils.loadSchema(JsonUtils.GENERAL_MESSAGE_SCHEMA);
        });

    // At start of Activity we display home fragment
    setCurrentFragment(getSupportFragmentManager(), R.id.fragment_home, HomeFragment::newInstance);

    restoreStoredState();
  }

  @Override
  public void onStop() {
    super.onStop();

    try {
      viewModel.savePersistentData();
    } catch (GeneralSecurityException e) {
      // We do not display the security error to the user
      Log.d(TAG, "Storage was unsuccessful du to wallet error " + e);
      Toast.makeText(this, R.string.error_storage_wallet, Toast.LENGTH_SHORT).show();
    }
  }

  /** Setup the listeners that changes the navigation bar menus */
  private void setupMenuAvailabilityListeners(BottomNavigationView navbar) {
    MenuItem connectItem = navbar.getMenu().getItem(HomeTab.CONNECT.ordinal());
    MenuItem launchItem = navbar.getMenu().getItem(HomeTab.LAUNCH.ordinal());
    MenuItem socialMediaItem = navbar.getMenu().getItem(HomeTab.SOCIAL.ordinal());

    // Gray out the launch and connect buttons depending on the wallet state
    viewModel
        .getIsWalletSetUpEvent()
        .observe(
            this,
            walletSetup -> {
              // We set the button icon depending on the livedata value
              boolean setup = Boolean.TRUE.equals(walletSetup);
              connectItem.setIcon(
                  setup ? R.drawable.ic_home_connect_enabled : R.drawable.ic_home_connect_disabled);
              launchItem.setIcon(
                  setup ? R.drawable.ic_home_launch_enabled : R.drawable.ic_home_launch_disabled);
            });

    // Gray out the social media button if no laos were created
    viewModel
        .isSocialMediaEnabled()
        .observe(
            this,
            enabled ->
                // We set the button icon depending on the livedata value
                socialMediaItem.setIcon(
                    Boolean.TRUE.equals(enabled)
                        ? R.drawable.ic_common_social_media_enabled
                        : R.drawable.ic_common_social_media_disabled));
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    getMenuInflater().inflate(R.menu.options_menu, menu);
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    if (item.getItemId() == R.id.settings) {
      Intent intent = new Intent(this, SettingsActivity.class);
      Log.d(HomeViewModel.TAG, "Trying to open settings");
      startActivity(intent);
      return true;
    } else {
      return super.onOptionsItemSelected(item);
    }
  }

  private void showWalletWarning() {
    Toast.makeText(this, R.string.uninitialized_wallet_exception, Toast.LENGTH_SHORT).show();
  }

  private void restoreStoredState() {
    PersistentData data = ActivityUtils.loadPersistentData(this);
    viewModel.restoreConnections(data);
  }

  public static HomeViewModel obtainViewModel(FragmentActivity activity) {
    return new ViewModelProvider(activity).get(HomeViewModel.class);
  }

  /** Factory method to create a fresh Intent that opens an HomeActivity */
  public static Intent newIntent(Context ctx) {
    return new Intent(ctx, HomeActivity.class);
  }

  /**
   * Set the current fragment in the container of the home activity
   *
   * @param manager the manager of the activity
   * @param id of the fragment
   * @param fragmentSupplier provides the fragment if it is missing
   */
  public static void setCurrentFragment(
      FragmentManager manager, @IdRes int id, Supplier<Fragment> fragmentSupplier) {
    ActivityUtils.setFragmentInContainer(
        manager, R.id.fragment_container_home, id, fragmentSupplier);
  }
}
