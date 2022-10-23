package com.github.dedis.popstellar.ui.home;

import android.app.*;
import android.content.*;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.IdRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.*;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.ViewModelProvider;

import com.github.dedis.popstellar.R;
import com.github.dedis.popstellar.model.network.serializer.JsonUtils;
import com.github.dedis.popstellar.repository.local.PersistentData;
import com.github.dedis.popstellar.ui.wallet.SeedWalletFragment;
import com.github.dedis.popstellar.utility.ActivityUtils;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.security.GeneralSecurityException;
import java.util.function.Supplier;

import dagger.hilt.android.AndroidEntryPoint;

/** HomeActivity represents the entry point for the application. */
@AndroidEntryPoint
public class HomeActivity extends AppCompatActivity {

  private final String TAG = HomeActivity.class.getSimpleName();

  private HomeViewModel viewModel;
  private Menu menu;

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

  private void menuTitleUpdater() {
    viewModel
        .getIsWalletSetUpEvent()
        .observe(
            this,
            isSetUp ->
                menu.getItem(0)
                    .setTitle(
                        Boolean.TRUE.equals(isSetUp)
                            ? R.string.logout_title
                            : R.string.wallet_setup));
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    getMenuInflater().inflate(R.menu.options_menu, menu);
    this.menu = menu;
    menuTitleUpdater();
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    if (item.getItemId() == R.id.wallet_init_logout) {
      handleWalletSettings();
    } else if (item.getItemId() == R.id.clear_storage) {
      handleClearing();
    } else {
      return super.onOptionsItemSelected(item);
    }
    return true;
  }

  private void handleWalletSettings() {
    if (viewModel.isWalletSetUp()) {
      new MaterialAlertDialogBuilder(this)
          .setTitle(R.string.logout_title)
          .setMessage(R.string.logout_message)
          .setPositiveButton(R.string.confirm, (dialog, which) -> viewModel.logoutWallet())
          .setNegativeButton(R.string.cancel, (dialog, which) -> dialog.dismiss())
          .show();
    } else {
      setCurrentFragment(
          getSupportFragmentManager(), R.id.fragment_seed_wallet, SeedWalletFragment::new);
    }
  }

  private void handleClearing() {
    new AlertDialog.Builder(this)
        .setTitle(R.string.confirm_title)
        .setMessage(R.string.clear_confirmation_text)
        .setPositiveButton(
            R.string.yes,
            (dialogInterface, i) -> {
              boolean success = ActivityUtils.clearStorage(this);
              Toast.makeText(
                      this,
                      success ? R.string.clear_success : R.string.clear_failure,
                      Toast.LENGTH_LONG)
                  .show();

              // Restart activity
              recreate();
            })
        .setNegativeButton(R.string.no, null)
        .show();
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
