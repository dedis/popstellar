package com.github.dedis.popstellar.ui.home;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.IdRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.*;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.ViewModelProvider;

import com.github.dedis.popstellar.R;
import com.github.dedis.popstellar.databinding.HomeActivityBinding;
import com.github.dedis.popstellar.model.network.serializer.JsonUtils;
import com.github.dedis.popstellar.ui.home.wallet.SeedWalletFragment;
import com.github.dedis.popstellar.ui.lao.witness.WitnessingViewModel;
import com.github.dedis.popstellar.utility.ActivityUtils;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.security.GeneralSecurityException;
import java.util.function.Supplier;

import dagger.hilt.android.AndroidEntryPoint;
import timber.log.Timber;

/** HomeActivity represents the entry point for the application. */
@AndroidEntryPoint
public class HomeActivity extends AppCompatActivity {

  private final String TAG = HomeActivity.class.getSimpleName();

  private HomeViewModel viewModel;
  private HomeActivityBinding binding;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    binding = HomeActivityBinding.inflate(getLayoutInflater());
    setContentView(binding.getRoot());

    viewModel = obtainViewModel(this);
    handleTopAppBar();

    // Load all the json schemas in background when the app is started.
    AsyncTask.execute(
        () -> {
          JsonUtils.loadSchema(JsonUtils.ROOT_SCHEMA);
          JsonUtils.loadSchema(JsonUtils.DATA_SCHEMA);
          JsonUtils.loadSchema(JsonUtils.GENERAL_MESSAGE_SCHEMA);
        });

    // At start of Activity we display home fragment
    setCurrentFragment(getSupportFragmentManager(), R.id.fragment_home, HomeFragment::newInstance);

    // Try to restore the wallet if persisted in the database
    if (!viewModel.restoreWallet()) {
      // If the state restore fails it means that no wallet is set up
      setCurrentFragment(
          getSupportFragmentManager(), R.id.fragment_seed_wallet, SeedWalletFragment::newInstance);

      new MaterialAlertDialogBuilder(this)
          .setMessage(R.string.wallet_init_message)
          .setNeutralButton(R.string.ok, (dialog, which) -> dialog.dismiss())
          .show();
    }
  }

  private void handleTopAppBar() {
    viewModel.getPageTitle().observe(this, binding.topAppBar::setTitle);

    setNavigation();
    setMenuItemListener();
    observeWallet();
  }

  private void setNavigation() {
    // Observe whether the home icon or back arrow should be displayed
    viewModel
        .isHome()
        .observe(
            this,
            isHome -> {
              if (Boolean.TRUE.equals(isHome)) {
                binding.topAppBar.setNavigationIcon(R.drawable.home_icon);
                binding.topAppBar.getMenu().setGroupVisible(0, true);
              } else {
                Fragment fragment =
                    getSupportFragmentManager().findFragmentById(R.id.fragment_container_home);
                // If the fragment is not the seed wallet then make the back arrow appear
                if (fragment instanceof SeedWalletFragment) {
                  binding.topAppBar.setNavigationIcon(null);
                } else {
                  binding.topAppBar.setNavigationIcon(R.drawable.back_arrow_icon);
                }
                // Disable the overflow menu
                binding.topAppBar.getMenu().setGroupVisible(0, false);
              }
            });

    // Listen to click on left icon of toolbar
    binding.topAppBar.setNavigationOnClickListener(
        view -> {
          if (Boolean.FALSE.equals(viewModel.isHome().getValue())) {
            Timber.tag(TAG).d("Going back to home");
            // Press back arrow
            onBackPressed();
          } else {
            // If the user presses on the home button display the general info about the app
            new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.app_name)
                .setMessage(R.string.app_info)
                .setNeutralButton(R.string.ok, (dialog, which) -> dialog.dismiss())
                .show();
          }
        });
  }

  private void setMenuItemListener() {
    // Set menu items behaviour
    binding.topAppBar.setOnMenuItemClickListener(
        item -> {
          if (item.getItemId() == R.id.wallet_init_logout) {
            handleWalletSettings();
          } else if (item.getItemId() == R.id.clear_storage) {
            handleClearing();
          } else if (item.getItemId() == R.id.home_settings) {
            handleSettings();
          }
          return true;
        });
  }

  private void observeWallet() {
    // Listen to wallet status to adapt the menu item title
    viewModel
        .getIsWalletSetUpEvent()
        .observe(
            this,
            isSetUp ->
                binding
                    .topAppBar
                    .getMenu()
                    .getItem(0)
                    .setTitle(
                        Boolean.TRUE.equals(isSetUp)
                            ? R.string.logout_title
                            : R.string.wallet_setup));
  }

  @Override
  public void onStop() {
    super.onStop();

    // On stop persist the wallet
    try {
      viewModel.saveWallet();
    } catch (GeneralSecurityException e) {
      // We do not display the security error to the user
      Timber.tag(TAG).d(e, "Storage was unsuccessful due to wallet error");
      Toast.makeText(this, R.string.error_storage_wallet, Toast.LENGTH_SHORT).show();
    }
  }

  private void handleWalletSettings() {
    if (viewModel.isWalletSetUp()) {
      new MaterialAlertDialogBuilder(this)
          .setTitle(R.string.logout_title)
          .setMessage(R.string.logout_message)
          .setPositiveButton(
              R.string.confirm,
              (dialog, which) -> {
                viewModel.logoutWallet();
                setCurrentFragment(
                    getSupportFragmentManager(),
                    R.id.fragment_seed_wallet,
                    SeedWalletFragment::newInstance);
              })
          .setNegativeButton(R.string.cancel, (dialog, which) -> dialog.dismiss())
          .show();
    } else {
      setCurrentFragment(
          getSupportFragmentManager(), R.id.fragment_seed_wallet, SeedWalletFragment::newInstance);
    }
  }

  private void handleClearing() {
    new AlertDialog.Builder(this)
        .setTitle(R.string.confirm_title)
        .setMessage(R.string.clear_confirmation_text)
        .setPositiveButton(
            R.string.yes,
            (dialogInterface, i) -> {
              viewModel.clearStorage();
              Toast.makeText(this, R.string.clear_success, Toast.LENGTH_LONG).show();

              // Restart activity
              Intent intent = HomeActivity.newIntent(this);

              // Flags to clear data structures and free memory
              intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
              intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

              startActivity(intent);
              finish();
            })
        .setNegativeButton(R.string.no, null)
        .show();
  }

  private void handleSettings() {
    ActivityUtils.setFragmentInContainer(
        getSupportFragmentManager(), R.id.fragment_container_home, SettingsFragment::newInstance);
  }

  public static HomeViewModel obtainViewModel(FragmentActivity activity) {
    return new ViewModelProvider(activity).get(HomeViewModel.class);
  }

  public static SettingsViewModel obtainSettingsViewModel(FragmentActivity activity) {
    return new ViewModelProvider(activity).get(SettingsViewModel.class);
  }

  public static WitnessingViewModel obtainWitnessingViewModel(FragmentActivity activity) {
    return new ViewModelProvider(activity).get(WitnessingViewModel.class);
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

  /** Adds a callback that describes the action to take the next time the back button is pressed */
  public static void addBackNavigationCallback(
      FragmentActivity activity, LifecycleOwner lifecycleOwner, OnBackPressedCallback callback) {
    activity.getOnBackPressedDispatcher().addCallback(lifecycleOwner, callback);
  }

  /** Adds a specific callback for the back button that opens the home fragment */
  public static void addBackNavigationCallbackToHome(
      FragmentActivity activity, LifecycleOwner lifecycleOwner, String tag) {
    addBackNavigationCallback(
        activity,
        lifecycleOwner,
        ActivityUtils.buildBackButtonCallback(
            tag,
            "home fragment",
            () ->
                setCurrentFragment(
                    activity.getSupportFragmentManager(), R.id.fragment_home, HomeFragment::new)));
  }
}
